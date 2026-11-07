#!/usr/bin/env python3
"""Run real clean-seed acceptance in a disposable Docker PostgreSQL 18 container."""
from __future__ import annotations
import argparse
from datetime import datetime, timezone
import hashlib
import json
import os
from pathlib import Path
import secrets
import subprocess
import tempfile
import time
from open_source_policy import OSS_MENU_MIGRATION, retired_menu_predicate

HERE = Path(__file__).resolve().parent
SCHEMA = "unix_tenant_202605"


def run(args, **kwargs):
    return subprocess.run(args, text=True, capture_output=True, **kwargs)


def validate_open_source_menu_upgrade(execute, sql, checks):
    """Existing installations lose excluded menus while business rows remain intact."""
    migration = (HERE.parents[1] / 'cn-universal-db-migrator/src/main/resources/db/migration/postgresql'
                 / OSS_MENU_MIGRATION).read_text()
    shape = f"""SELECT md5(jsonb_build_object(
      'menus',(SELECT jsonb_agg(to_jsonb(m) ORDER BY menu_id) FROM {SCHEMA}.sys_menu m),
      'roles',(SELECT jsonb_agg(to_jsonb(r) ORDER BY uuid) FROM {SCHEMA}.sys_role_menu r),
      'packages',(SELECT jsonb_agg(to_jsonb(p) ORDER BY id) FROM {SCHEMA}.iot_tenant_package_menu p),
      'administrator',(SELECT to_jsonb(u) FROM {SCHEMA}.iot_user u WHERE id=1))::text);"""
    expected = sql(shape).splitlines()[-1]
    fixture = """INSERT INTO sys_menu(menu_id,menu_name,parent_id,path,component,menu_type,status,perms) VALUES
      (1700,'Excluded template root',0,'device-template','','M','0',''),
      (987654001,'Nested excluded page',1700,'nested','unsupported/page','C','0','fixture:page'),
      (987654002,'Nested excluded button',987654001,'','','F','0','fixture:button'),
      (987654003,'Alternative industrial permission',1098,'','','F','0','industrial:fixture:read'),
      (987654004,'Alternative engine route',1081,'custom-flow','flow/index','C','0','fixture:flow');
      INSERT INTO sys_role_menu(role_id,menu_id,uuid) VALUES
        (1,987654002,987654001),(1,987654003,987654002),(1,987654004,987654003);
      INSERT INTO iot_tenant_package_menu(package_id,menu_id,id) VALUES
        (900000000000000001,987654002,987654001),
        (900000000000000001,987654003,987654002),
        (900000000000000001,987654004,987654003);
      INSERT INTO sys_config(config_name,config_key,config_value)
        VALUES ('OSS preservation fixture','oss.migration.fixture','preserved');
    """
    preservation = "SELECT config_value FROM sys_config WHERE config_key='oss.migration.fixture';"
    prefix = f'BEGIN; SET search_path TO {SCHEMA},public;\n'
    result = execute(['psql', '-X', '-qAt', '-v', 'ON_ERROR_STOP=1'], input=(
        prefix + fixture + migration + shape + preservation + migration + shape + preservation + 'ROLLBACK;'))
    assert result.stdout.strip().splitlines() == [expected, 'preserved', expected, 'preserved']
    assert sql(f"SELECT count(*) FROM {SCHEMA}.sys_menu WHERE {retired_menu_predicate()};") == '0'
    checks['oss_menu_bootstrap_upgrade_equivalence_recursive_cleanup_repeat_preserves_business_and_admin'] = 'passed'


def validate_ota_transfer_upgrade(execute, sql, checks):
    migration = (HERE.parents[1] / 'cn-universal-db-migrator/src/main/resources/db/migration/postgresql/'
                 'V2026101602__ota_transfer_state.sql').read_text()
    shape = f"""SELECT jsonb_agg(jsonb_build_array(a.attname,format_type(a.atttypid,a.atttypmod),
      a.attnotnull,pg_get_expr(d.adbin,d.adrelid),col_description(a.attrelid,a.attnum)) ORDER BY a.attnum)::text
      FROM pg_attribute a LEFT JOIN pg_attrdef d ON d.adrelid=a.attrelid AND d.adnum=a.attnum
      WHERE a.attrelid='{SCHEMA}.iot_ota_task_device'::regclass AND a.attnum>0 AND NOT a.attisdropped;"""
    expected = sql(shape).splitlines()[-1]
    # Only this disposable acceptance database is changed; the transaction restores its clean seed.
    fixture = """INSERT INTO iot_ota_task_device
      (id,task_id,product_key,device_id,tenant_id,status,progress,command_payload,raw_report)
      VALUES (987654321,987654321,'ota-acceptance','whole-package-device','test','PUSHED',17,
        '{"url":"existing-firmware.bin"}','{"status":"downloading"}');
      CREATE TEMP TABLE ota_preserved_row AS SELECT to_jsonb(d)-'transfer_state' AS original
        FROM iot_ota_task_device d WHERE id=987654321;
      ALTER TABLE iot_ota_task_device DROP COLUMN transfer_state;
    """
    preservation = """SELECT (to_jsonb(d)-'transfer_state'=p.original) AND transfer_state IS NULL
      FROM iot_ota_task_device d CROSS JOIN ota_preserved_row p WHERE d.id=987654321;"""
    result = execute(['psql','-X','-qAt','-v','ON_ERROR_STOP=1'], input=(
        f'BEGIN; SET search_path TO {SCHEMA},public;\n' + fixture + migration + shape + preservation
        + migration + shape + preservation + 'ROLLBACK;'))
    assert result.stdout.strip().splitlines() == [expected, 't', expected, 't']
    checks['ota_transfer_bootstrap_upgrade_repeat_and_existing_tasks_preserved'] = 'passed'

    payload = """'{"nextChunkIndex":7,"sessionToken":"isolated-session","acknowledged":"'
      || repeat('01',65536) || '","detail":"中文🙂"}'"""
    session = execute(['psql','-X','-qAt','-v','ON_ERROR_STOP=1'], input=(
        f'BEGIN; SET search_path TO {SCHEMA},public;\n'
        + "INSERT INTO iot_ota_task_device(id,task_id,product_key,device_id,transfer_state) "
        + f"VALUES (987654322,987654321,'ota-acceptance','chunked-device',{payload});"
        + "SELECT transfer_state::jsonb->>'sessionToken', length(transfer_state::jsonb->>'acknowledged') "
        + "FROM iot_ota_task_device WHERE id=987654322;ROLLBACK;"))
    assert session.stdout.strip() == 'isolated-session|131072'
    assert sql(shape).splitlines()[-1] == expected
    checks['ota_transfer_large_session_unicode_round_trip'] = 'passed'


def validate_uniview_sdk_dictionary_upgrade(execute, sql, checks):
    migration = (HERE.parents[1] / 'cn-universal-db-migrator/src/main/resources/db/migration/postgresql/'
                 'V2026102801__enable_uniview_sdk_video.sql').read_text()
    migration += (HERE.parents[1] / 'cn-universal-db-migrator/src/main/resources/db/migration/postgresql/'
                  'V2026102802__uniview_native_stream_default.sql').read_text()
    smart_migration = (HERE.parents[1] / 'cn-universal-db-migrator/src/main/resources/db/migration/postgresql/'
                       'V2026102803__uniview_smart_alarm_dictionary.sql').read_text()
    migration += smart_migration
    scope = "dict_type IN ('video_uniview_sdk_fields','video_sdk_event_uniview_sdk') OR (dict_type='video_platform_type' AND dict_value='uniview_sdk')"
    shape = f"""SELECT jsonb_build_object(
      'types',(SELECT jsonb_agg(jsonb_build_array(dict_name,dict_type,status,remark) ORDER BY dict_type)
        FROM {SCHEMA}.sys_dict_type WHERE dict_type IN ('video_uniview_sdk_fields','video_sdk_event_uniview_sdk')),
      'data',(SELECT jsonb_agg(jsonb_build_array(dict_sort,dict_label,dict_value,dict_type,css_class,list_class,
        is_default,status,CASE WHEN dict_type='video_platform_type' THEN to_jsonb(remark) ELSE remark::jsonb END)
        ORDER BY dict_type,dict_value) FROM {SCHEMA}.sys_dict_data WHERE {scope}))::text;"""
    expected = sql(shape).splitlines()[-1]
    remove = f"""DELETE FROM {SCHEMA}.sys_dict_data WHERE {scope};
      DELETE FROM {SCHEMA}.sys_dict_type WHERE dict_type IN ('video_uniview_sdk_fields','video_sdk_event_uniview_sdk');"""
    prefix = f'BEGIN; SET search_path TO {SCHEMA},public;\n'
    result = execute(['psql','-X','-qAt','-v','ON_ERROR_STOP=1'], input=(
      prefix + remove + migration + shape + migration + shape + 'ROLLBACK;'))
    assert result.stdout.strip().splitlines() == [expected, expected]
    checks['uniview_sdk_bootstrap_upgrade_equivalence_and_idempotence'] = 'passed'
    # An installation already at V2026102802 applies only the new smart-alarm migration.
    smart_remove = f"""DELETE FROM {SCHEMA}.sys_dict_data WHERE dict_type='video_sdk_event_uniview_sdk'
      AND dict_value IN ('0x000003EB','0x000003EC','0x000003F7','0x000003F8');"""
    result = execute(['psql','-X','-qAt','-v','ON_ERROR_STOP=1'], input=(
      prefix + smart_remove + smart_migration + shape + smart_migration + shape + 'ROLLBACK;'))
    assert result.stdout.strip().splitlines() == [expected, expected]
    checks['uniview_smart_alarm_upgrade_from_2802_equivalence_and_idempotence'] = 'passed'
    fixture = f"""UPDATE {SCHEMA}.sys_dict_type SET dict_name='用户宇视字典',status='1'
        WHERE dict_type='video_sdk_event_uniview_sdk';
      UPDATE {SCHEMA}.sys_dict_data SET dict_label='用户移动告警',status='1',
        remark='{{"type":"custom_motion","eventId":"customMotion"}}'
        WHERE dict_type='video_sdk_event_uniview_sdk' AND dict_value='0x00000001';
      UPDATE {SCHEMA}.sys_dict_data SET dict_label='用户区域告警',status='1',
        remark='{{"type":"custom_intrusion","eventId":"customIntrusion"}}'
        WHERE dict_type='video_sdk_event_uniview_sdk' AND dict_value='0x000003EC';
      DELETE FROM {SCHEMA}.sys_dict_data WHERE dict_type='video_uniview_sdk_fields' AND dict_value='rtspPortDefault';"""
    query = f"""SELECT jsonb_build_object(
      'type',(SELECT dict_name='用户宇视字典' AND status='1' FROM {SCHEMA}.sys_dict_type WHERE dict_type='video_sdk_event_uniview_sdk'),
      'custom',(SELECT dict_label='用户移动告警' AND status='1' AND remark::jsonb->>'eventId'='customMotion'
        FROM {SCHEMA}.sys_dict_data WHERE dict_type='video_sdk_event_uniview_sdk' AND dict_value='0x00000001'),
      'smartCustom',(SELECT dict_label='用户区域告警' AND status='1' AND remark::jsonb->>'eventId'='customIntrusion'
        FROM {SCHEMA}.sys_dict_data WHERE dict_type='video_sdk_event_uniview_sdk' AND dict_value='0x000003EC'),
      'fields',(SELECT count(*) FROM {SCHEMA}.sys_dict_data WHERE dict_type='video_uniview_sdk_fields'))::text;"""
    result = execute(['psql','-X','-qAt','-v','ON_ERROR_STOP=1'], input=(
      prefix + fixture + migration + query + migration + query + 'ROLLBACK;'))
    assert [json.loads(line) for line in result.stdout.strip().splitlines()] == [
      {'type': True, 'custom': True, 'smartCustom': True, 'fields': 8}] * 2
    assert sql(shape).splitlines()[-1] == expected
    checks['uniview_sdk_partial_upgrade_preserves_user_configuration'] = 'passed'


def validate_wvp_event_dictionary_upgrade(execute, sql, checks):
    migration = (HERE.parents[1] / 'cn-universal-db-migrator/src/main/resources/db/migration/postgresql/'
                 'V2026101601__wvp_gb28181_event_dictionary.sql').read_text()
    shape = f"""SELECT jsonb_build_object(
      'type',(SELECT jsonb_agg(jsonb_build_array(dict_name,dict_type,status,remark) ORDER BY dict_type)
              FROM {SCHEMA}.sys_dict_type WHERE dict_type='video_sdk_event_wvp'),
      'data',(SELECT jsonb_agg(jsonb_build_array(dict_sort,dict_label,dict_value,is_default,status,remark::jsonb)
                              ORDER BY dict_value)
              FROM {SCHEMA}.sys_dict_data WHERE dict_type='video_sdk_event_wvp'))::text;"""
    expected = sql(shape).splitlines()[-1]
    remove = f"""DELETE FROM {SCHEMA}.sys_dict_data WHERE dict_type='video_sdk_event_wvp';
                 DELETE FROM {SCHEMA}.sys_dict_type WHERE dict_type='video_sdk_event_wvp';"""
    prefix = f'BEGIN; SET search_path TO {SCHEMA},public;\n'
    migrated = execute(['psql', '-X', '-qAt', '-v', 'ON_ERROR_STOP=1'], input=(
        prefix + remove + migration + shape + migration + shape + 'ROLLBACK;'))
    assert migrated.returncode == 0 and migrated.stdout.strip().splitlines() == [expected, expected]
    checks['wvp_event_dictionary_bootstrap_upgrade_and_idempotence'] = 'passed'

    # Partial upgrades preserve a disabled/customized dictionary and additional vendor extensions.
    fixtures = f"""
      DELETE FROM {SCHEMA}.sys_dict_data WHERE dict_type='video_sdk_event_wvp' AND dict_value <> '5:6';
      UPDATE {SCHEMA}.sys_dict_type SET dict_name='用户国标字典', status='1' WHERE dict_type='video_sdk_event_wvp';
      UPDATE {SCHEMA}.sys_dict_data SET dict_label='用户入侵', status='1',
        remark='{{"type":"custom_intrusion","eventId":"customIntrusion"}}'
        WHERE dict_type='video_sdk_event_wvp' AND dict_value='5:6';
      INSERT INTO {SCHEMA}.sys_dict_data(dict_code,dict_sort,dict_label,dict_value,dict_type,remark)
        SELECT MAX(dict_code)+1,99,'扩展事件','5:99','video_sdk_event_wvp',
          '{{"type":"custom_event","eventId":"customEvent"}}' FROM {SCHEMA}.sys_dict_data;
    """
    query = f"""SELECT jsonb_build_object(
      'type',(SELECT dict_name='用户国标字典' AND status='1' FROM {SCHEMA}.sys_dict_type WHERE dict_type='video_sdk_event_wvp'),
      'custom',(SELECT dict_label='用户入侵' AND status='1' AND remark::jsonb->>'eventId'='customIntrusion'
                FROM {SCHEMA}.sys_dict_data WHERE dict_type='video_sdk_event_wvp' AND dict_value='5:6'),
      'count',(SELECT count(*) FROM {SCHEMA}.sys_dict_data WHERE dict_type='video_sdk_event_wvp'))::text;"""
    partial = execute(['psql', '-X', '-qAt', '-v', 'ON_ERROR_STOP=1'], input=(
        prefix + fixtures + migration + query + migration + query + 'ROLLBACK;'))
    assert partial.returncode == 0
    assert [json.loads(line) for line in partial.stdout.strip().splitlines()] == [
        {'type': True, 'custom': True, 'count': 26}] * 2
    assert sql(shape).splitlines()[-1] == expected
    checks['wvp_event_dictionary_partial_upgrade_preserves_customization'] = 'passed'


def validate_video_receipt_upgrade(execute, sql, checks):
    migration = (HERE.parents[1] / "cn-universal-db-migrator/src/main/resources/db/migration/postgresql/"
                 "V2026101302__video_sdk_alarm_receipt.sql").read_text()
    shape = f"""SELECT jsonb_build_object(
        'columns',(SELECT jsonb_agg(to_jsonb(c) ORDER BY ordinal_position) FROM
          (SELECT column_name,data_type,character_maximum_length,is_nullable,column_default,ordinal_position
           FROM information_schema.columns WHERE table_schema='{SCHEMA}' AND table_name='video_sdk_alarm_receipt') c),
        'indexes',(SELECT jsonb_agg(jsonb_build_array(indexname,indexdef) ORDER BY indexname)
          FROM pg_indexes WHERE schemaname='{SCHEMA}' AND tablename='video_sdk_alarm_receipt'))::text;"""
    expected = sql(shape)
    # Disposable acceptance DB only: simulate the old installation without this new table.
    sql(f"DROP TABLE {SCHEMA}.video_sdk_alarm_receipt;")
    sql(f"SET search_path TO {SCHEMA},public;\n" + migration)
    assert sql(shape) == expected
    duplicate = execute(["psql", "-X", "-q", "-At", "-v", "ON_ERROR_STOP=1"], input=f"""
      BEGIN;
      INSERT INTO {SCHEMA}.video_sdk_alarm_receipt(id,tenant_id,instance_key,device_id,event_id,event_kind,payload_json,created_at,updated_at)
      VALUES(repeat('a',64),'test','isup','ipc','e1','motion','{{}}',1,1);
      INSERT INTO {SCHEMA}.video_sdk_alarm_receipt(id,tenant_id,instance_key,device_id,event_id,event_kind,payload_json,created_at,updated_at)
      VALUES(repeat('b',64),'test','isup','ipc','e1','motion','{{}}',1,1);
      ROLLBACK;
    """, check=False)
    assert duplicate.returncode != 0 and 'uk_video_alarm_receipt_identity' in duplicate.stderr
    checks['video_alarm_receipt_bootstrap_upgrade_identity'] = 'passed'


def validate_wechat_reply_upgrade(execute, sql, checks):
    migration = (HERE.parents[1] / 'cn-universal-db-migrator/src/main/resources/db/migration/postgresql/'
                 'V2026110302__wechat_message_reply_dict.sql').read_text()
    types = "'wechat_reply','wechat_keyword_reply'"
    contents = f"""SELECT jsonb_build_object(
      'types',(SELECT jsonb_agg(jsonb_build_array(dict_id,dict_name,dict_type,status,remark) ORDER BY dict_type)
        FROM {SCHEMA}.sys_dict_type WHERE dict_type IN ({types})),
      'data',(SELECT jsonb_agg(jsonb_build_array(dict_code,dict_sort,dict_label,dict_value,dict_type,status,remark)
        ORDER BY dict_type,dict_sort,dict_code) FROM {SCHEMA}.sys_dict_data WHERE dict_type IN ({types})))::text;"""
    expected = sql(contents)
    sql(f'DELETE FROM {SCHEMA}.sys_dict_data WHERE dict_type IN ({types});'
        f'DELETE FROM {SCHEMA}.sys_dict_type WHERE dict_type IN ({types});')
    prefix = f'SET search_path TO {SCHEMA},public;\n'
    for _ in range(2):
        sql(prefix + migration)
        assert sql(contents).splitlines()[-1] == expected
    checks['wechat_reply_bootstrap_upgrade_and_repeat_equivalence'] = 'passed'
    preserved = sql(prefix + """BEGIN;
      UPDATE sys_dict_data SET remark='自定义 🎉' || chr(10) || '保留正文',status='1'
        WHERE dict_type='wechat_reply' AND dict_value='default';
      """ + migration + """
      SELECT status='1' AND remark='自定义 🎉' || chr(10) || '保留正文'
        FROM sys_dict_data WHERE dict_type='wechat_reply' AND dict_value='default';
      ROLLBACK;""")
    assert 't' in preserved.splitlines()
    checks['wechat_reply_upgrade_preserves_custom_text_newlines_emoji_and_disabled_status'] = 'passed'


def validate_wechat_login_upgrade(execute, sql, checks):
    migration = (HERE.parents[1] / 'cn-universal-db-migrator/src/main/resources/db/migration/postgresql/'
                 'V2026110301__wechat_scan_login.sql').read_text()
    tables = "'iot_wechat_login_config','iot_user_wechat_binding'"
    shape = f"""SELECT jsonb_build_object(
      'columns',(SELECT jsonb_agg(jsonb_build_array(table_name,column_name,data_type,
        character_maximum_length,is_nullable,column_default,collation_name) ORDER BY table_name,ordinal_position)
        FROM information_schema.columns WHERE table_schema='{SCHEMA}' AND table_name IN ({tables})),
      'indexes',(SELECT jsonb_agg(jsonb_build_array(indexname,indexdef) ORDER BY indexname)
        FROM pg_indexes WHERE schemaname='{SCHEMA}' AND tablename IN ({tables})),
      'constraints',(SELECT jsonb_agg(jsonb_build_array(c.conname,pg_get_constraintdef(c.oid)) ORDER BY c.conname)
        FROM pg_constraint c JOIN pg_class t ON t.oid=c.conrelid JOIN pg_namespace n ON n.oid=t.relnamespace
        WHERE n.nspname='{SCHEMA}' AND t.relname IN ({tables})))::text;"""
    expected = sql(shape).splitlines()[-1]
    # Only a disposable acceptance database is modified; compare a genuinely old installation.
    sql(f'DROP TABLE {SCHEMA}.iot_user_wechat_binding, {SCHEMA}.iot_wechat_login_config;')
    sql(f'SET search_path TO {SCHEMA},public;\n' + migration)
    assert sql(shape).splitlines()[-1] == expected
    sql(f'SET search_path TO {SCHEMA},public;\n' + migration)
    assert sql(shape).splitlines()[-1] == expected
    checks['wechat_login_bootstrap_upgrade_and_repeat_equivalence'] = 'passed'
    fixture = f"""BEGIN;
      INSERT INTO {SCHEMA}.iot_wechat_login_config(tenant_id) VALUES('wx-tenant-a');
      SELECT enabled=false AND binding_enabled=true AND capability_mode='MESSAGE_CODE'
        AND login_mode='BOUND_USER' AND expire_seconds=180 AND config_version=1
        AND app_secret IS NULL AND callback_token IS NULL AND encoding_aes_key IS NULL
        FROM {SCHEMA}.iot_wechat_login_config WHERE tenant_id='wx-tenant-a';
      INSERT INTO {SCHEMA}.iot_user_wechat_binding(id,tenant_id,app_id,openid,user_union_id) VALUES
        ('wx-a','wx-tenant-a','wx0000000000000000','openid-a','user-a'),
        ('wx-b','wx-tenant-b','wx0000000000000000','openid-a','user-b'),
        ('wx-c','wx-tenant-a','wx0000000000000001','openid-a','user-a'),
        ('wx-d','wx-tenant-a','wx0000000000000000','Openid-a','user-c');
      SELECT count(*) FROM {SCHEMA}.iot_user_wechat_binding WHERE app_id='wx0000000000000000' AND openid='openid-a';
      ROLLBACK;"""
    assert execute(['psql','-X','-qAt','-v','ON_ERROR_STOP=1'], input=fixture).stdout.strip().splitlines() == ['t', '2']
    checks['wechat_login_empty_secrets_safe_defaults_and_tenant_app_case_isolation'] = 'passed'
    for duplicate, constraint in (
        ("('wx-e','wx-tenant-a','wx0000000000000000','openid-a','user-b')", 'uk_wechat_binding_identity'),
        ("('wx-e','wx-tenant-a','wx0000000000000000','openid-b','user-a')", 'uk_wechat_binding_account')):
        result = execute(['psql','-X','-qAt','-v','ON_ERROR_STOP=1'], input=f"""BEGIN;
          INSERT INTO {SCHEMA}.iot_user_wechat_binding(id,tenant_id,app_id,openid,user_union_id)
            VALUES('wx-a','wx-tenant-a','wx0000000000000000','openid-a','user-a'),{duplicate};
          ROLLBACK;""", check=False)
        assert result.returncode != 0 and constraint in result.stderr
    for field, value, constraint in (('expire_seconds','59','ck_wechat_login_expiry'),
            ('config_version','0','ck_wechat_login_version'),
            ('login_mode',"'UNKNOWN'",'ck_wechat_login_mode')):
        result = execute(['psql','-X','-qAt','-v','ON_ERROR_STOP=1'], input=f"""BEGIN;
          INSERT INTO {SCHEMA}.iot_wechat_login_config(tenant_id,{field}) VALUES('wx-invalid',{value});
          ROLLBACK;""", check=False)
        assert result.returncode != 0 and constraint in result.stderr
    checks['wechat_login_duplicate_identity_account_and_invalid_configuration_rejected'] = 'passed'


def validate_video_permission_upgrade(execute, sql, checks):
    migration = (HERE.parents[1] / 'cn-universal-db-migrator/src/main/resources/db/migration/postgresql/'
                 'V2026101303__video_operation_permission_families.sql').read_text()
    perms = "'video:device:play','video:device:control','video:device:config','video:device:maintain'"
    shape = f"""SELECT jsonb_build_object(
      'menus',(SELECT jsonb_agg(jsonb_build_array(menu_id,menu_name,parent_id,order_num,menu_type,status,perms) ORDER BY perms)
        FROM {SCHEMA}.sys_menu WHERE perms IN ({perms})),
      'roles',(SELECT jsonb_agg(jsonb_build_array(r.role_id,m.perms) ORDER BY r.role_id,m.perms)
        FROM {SCHEMA}.sys_role_menu r JOIN {SCHEMA}.sys_menu m ON m.menu_id=r.menu_id WHERE m.perms IN ({perms})),
      'packages',(SELECT jsonb_agg(jsonb_build_array(r.package_id,m.perms) ORDER BY r.package_id,m.perms)
        FROM {SCHEMA}.iot_tenant_package_menu r JOIN {SCHEMA}.sys_menu m ON m.menu_id=r.menu_id WHERE m.perms IN ({perms})))::text;"""
    before = sql(shape)
    # Reversible transaction in the disposable DB: simulate an old seed and prove equivalence.
    remove = f"""DELETE FROM {SCHEMA}.sys_role_menu WHERE menu_id IN (SELECT menu_id FROM {SCHEMA}.sys_menu WHERE perms IN ({perms}));
      DELETE FROM {SCHEMA}.iot_tenant_package_menu WHERE menu_id IN (SELECT menu_id FROM {SCHEMA}.sys_menu WHERE perms IN ({perms}));
      DELETE FROM {SCHEMA}.sys_menu WHERE perms IN ({perms});"""
    result = execute(['psql','-X','-q','-At','-v','ON_ERROR_STOP=1'], input=
      f'BEGIN; SET search_path TO {SCHEMA},public;\n' + remove + '\n' + migration + '\n' + shape + '\n' + migration + '\n' + shape + '\nROLLBACK;')
    assert result.stdout.strip().splitlines() == [before,before]
    checks['video_permission_bootstrap_upgrade_equivalence_and_repeat'] = 'passed'
    # Existing editor gets all families; a read-only role/package receives none.
    fixtures = f"""INSERT INTO sys_role(role_id,role_name,role_key,role_sort,status) VALUES
      (987651,'legacy video editor','video_accept_editor',1,'0'),(987652,'video reader','video_accept_reader',1,'0');
      INSERT INTO sys_role_menu(role_id,menu_id,uuid)
      SELECT 987651,menu_id,987651 FROM sys_menu WHERE perms='video:platform:edit' LIMIT 1;
      INSERT INTO sys_role_menu(role_id,menu_id,uuid)
      SELECT 987652,menu_id,987652 FROM sys_menu WHERE perms='video:platform:view' LIMIT 1;
      INSERT INTO iot_tenant_package(package_id,package_name) VALUES
      (987651,'legacy video editor'),(987652,'video reader');
      INSERT INTO iot_tenant_package_menu(package_id,menu_id,id)
      SELECT 987651,menu_id,987651 FROM sys_menu WHERE perms='video:platform:edit' LIMIT 1;
      INSERT INTO iot_tenant_package_menu(package_id,menu_id,id)
      SELECT 987652,menu_id,987652 FROM sys_menu WHERE perms='video:platform:view' LIMIT 1;"""
    query = f"""SELECT count(*) FROM sys_role_menu r JOIN sys_menu m ON m.menu_id=r.menu_id
      WHERE r.role_id=987651 AND m.perms IN ({perms});
      SELECT count(*) FROM sys_role_menu r JOIN sys_menu m ON m.menu_id=r.menu_id
      WHERE r.role_id=987652 AND m.perms IN ({perms});
      SELECT count(*) FROM iot_tenant_package_menu r JOIN sys_menu m ON m.menu_id=r.menu_id
      WHERE r.package_id=987651 AND m.perms IN ({perms});
      SELECT count(*) FROM iot_tenant_package_menu r JOIN sys_menu m ON m.menu_id=r.menu_id
      WHERE r.package_id=987652 AND m.perms IN ({perms});"""
    result = execute(['psql','-X','-q','-At','-v','ON_ERROR_STOP=1'], input=
      f'BEGIN; SET search_path TO {SCHEMA},public;\n' + fixtures + migration + query + migration + query + '\nROLLBACK;')
    assert result.stdout.strip().splitlines() == ['4','0','4','0','4','0','4','0']
    checks['video_permission_legacy_editor_preserved_and_reader_not_elevated'] = 'passed'
    for assignment in ("menu_type='C'", "status='1'", "parent_id=0"):
        rejected = execute(['psql','-X','-q','-At','-v','ON_ERROR_STOP=1'], input=
            f"BEGIN; SET search_path TO {SCHEMA},public; UPDATE sys_menu SET {assignment} "
            "WHERE perms='video:device:play';\n" + migration + '\nROLLBACK;', check=False)
        assert rejected.returncode != 0 and 'incompatible' in rejected.stderr.lower()
    checks['video_permission_incompatible_existing_menu_refused'] = 'passed'


def validate_video_read_permission_upgrade(execute, sql, checks):
    migration = (HERE.parents[1] / 'cn-universal-db-migrator/src/main/resources/db/migration/postgresql/'
                 'V2026101401__video_read_permission.sql').read_text()
    shape = f"""SELECT jsonb_build_object(
      'menus',(SELECT jsonb_agg(jsonb_build_array(menu_id,menu_name,parent_id,order_num,menu_type,status,perms) ORDER BY menu_id)
        FROM {SCHEMA}.sys_menu WHERE perms='video:platform:query'),
      'roles',(SELECT jsonb_agg(r.role_id ORDER BY r.role_id) FROM {SCHEMA}.sys_role_menu r
        JOIN {SCHEMA}.sys_menu m ON m.menu_id=r.menu_id WHERE m.perms='video:platform:query'),
      'packages',(SELECT jsonb_agg(r.package_id ORDER BY r.package_id) FROM {SCHEMA}.iot_tenant_package_menu r
        JOIN {SCHEMA}.sys_menu m ON m.menu_id=r.menu_id WHERE m.perms='video:platform:query'))::text;"""
    expected = sql(shape)
    remove = """DELETE FROM sys_role_menu WHERE menu_id IN (SELECT menu_id FROM sys_menu WHERE perms='video:platform:query');
      DELETE FROM iot_tenant_package_menu WHERE menu_id IN (SELECT menu_id FROM sys_menu WHERE perms='video:platform:query');
      DELETE FROM sys_menu WHERE perms='video:platform:query';"""
    result = execute(['psql','-X','-q','-At','-v','ON_ERROR_STOP=1'], input=
      f'BEGIN; SET search_path TO {SCHEMA},public;\n'+remove+migration+shape+migration+shape+'\nROLLBACK;')
    assert result.stdout.strip().splitlines() == [expected, expected]
    checks['video_read_permission_bootstrap_upgrade_and_repeat_equivalence'] = 'passed'
    partial = """INSERT INTO sys_menu(menu_id,menu_name,parent_id,order_num,menu_type,status,perms)
      VALUES(987669,'Compatible pre-existing READ',1282,34,'F','0','video:platform:query');"""
    partial_query = """SELECT menu_id FROM sys_menu WHERE perms='video:platform:query';
      SELECT count(*) FROM sys_menu WHERE menu_id=2071401000000000001;
      SELECT count(*) FROM sys_role_menu r JOIN sys_menu m ON m.menu_id=r.menu_id
      WHERE r.role_id=1 AND m.perms='video:platform:query';
      SELECT count(*) FROM iot_tenant_package_menu r JOIN sys_menu m ON m.menu_id=r.menu_id
      WHERE r.package_id=900000000000000001 AND m.perms='video:platform:query';"""
    result = execute(['psql','-X','-q','-At','-v','ON_ERROR_STOP=1'], input=
      f'BEGIN; SET search_path TO {SCHEMA},public;\n'+remove+partial+migration+partial_query+migration+partial_query+'\nROLLBACK;')
    assert result.stdout.strip().splitlines() == ['987669','0','1','1'] * 2
    checks['video_read_permission_partial_alternate_id_keeps_identity_recovers_grants'] = 'passed'
    fixtures = """INSERT INTO sys_role(role_id,role_name,role_key,role_sort,status) VALUES
      (987661,'video reader','video_reader_fixture',1,'0'),(987662,'video editor','video_editor_fixture',2,'0'),
      (987663,'unrelated','unrelated_fixture',3,'0');
      INSERT INTO sys_role_menu(role_id,menu_id,uuid)
      SELECT 987661,menu_id,987661 FROM sys_menu WHERE perms='video:platform:view' LIMIT 1;
      INSERT INTO sys_role_menu(role_id,menu_id,uuid)
      SELECT 987662,menu_id,987662 FROM sys_menu WHERE perms='video:platform:edit' LIMIT 1;
      INSERT INTO iot_tenant_package(package_id,package_name) VALUES
      (987661,'legacy video reader'),(987662,'legacy video editor'),(987663,'unrelated');
      INSERT INTO iot_tenant_package_menu(package_id,menu_id,id)
      SELECT 987661,menu_id,987661 FROM sys_menu WHERE perms='video:platform:view' LIMIT 1;
      INSERT INTO iot_tenant_package_menu(package_id,menu_id,id)
      SELECT 987662,menu_id,987662 FROM sys_menu WHERE perms='video:platform:edit' LIMIT 1;"""
    query = """SELECT count(*) FROM sys_role_menu r JOIN sys_menu m ON m.menu_id=r.menu_id
      WHERE r.role_id=987661 AND m.perms='video:platform:query';
      SELECT count(*) FROM sys_role_menu r JOIN sys_menu m ON m.menu_id=r.menu_id
      WHERE r.role_id=987662 AND m.perms='video:platform:query';
      SELECT count(*) FROM sys_role_menu r JOIN sys_menu m ON m.menu_id=r.menu_id
      WHERE r.role_id=987663 AND m.perms='video:platform:query';
      SELECT count(*) FROM iot_tenant_package_menu r JOIN sys_menu m ON m.menu_id=r.menu_id
      WHERE r.package_id=987661 AND m.perms='video:platform:query';
      SELECT count(*) FROM iot_tenant_package_menu r JOIN sys_menu m ON m.menu_id=r.menu_id
      WHERE r.package_id=987662 AND m.perms='video:platform:query';
      SELECT count(*) FROM iot_tenant_package_menu r JOIN sys_menu m ON m.menu_id=r.menu_id
      WHERE r.package_id=987663 AND m.perms='video:platform:query';
      SELECT count(*) FROM sys_role_menu r JOIN sys_menu m ON m.menu_id=r.menu_id
      WHERE r.role_id IN (987661,987662,987663) AND m.perms IN
        ('video:device:play','video:device:control','video:device:config','video:device:maintain','video:platform:subscribe');
      SELECT count(*) FROM iot_tenant_package_menu r JOIN sys_menu m ON m.menu_id=r.menu_id
      WHERE r.package_id IN (987661,987662,987663) AND m.perms IN
        ('video:device:play','video:device:control','video:device:config','video:device:maintain','video:platform:subscribe');"""
    result = execute(['psql','-X','-q','-At','-v','ON_ERROR_STOP=1'], input=
      f'BEGIN; SET search_path TO {SCHEMA},public;\n'+fixtures+migration+query+migration+query+'\nROLLBACK;')
    assert result.stdout.strip().splitlines() == ['1','1','0','1','1','0','0','0'] * 2
    checks['video_read_permission_inherits_view_edit_only_without_elevating_other_families'] = 'passed'
    cases = [
      ("UPDATE sys_menu SET menu_type='C' WHERE perms='video:platform:query';", 'incompatible'),
      ("UPDATE sys_menu SET status='1' WHERE perms='video:platform:query';", 'incompatible'),
      ("UPDATE sys_menu SET parent_id=0 WHERE perms='video:platform:query';", 'incompatible'),
      ("UPDATE sys_menu SET perms='unrelated:fixture' WHERE menu_id=2071401000000000001;", 'collides'),
      ("UPDATE sys_menu SET perms='unrelated:fixture' WHERE perms IN ('video:platform:view','video:platform:list');", 'parent menu'),
      ("INSERT INTO sys_menu(menu_id,menu_name,parent_id,menu_type,status,perms) VALUES(987669,'duplicate',1282,'F','0','video:platform:query');", 'duplicate')]
    for preparation, reason in cases:
        result = execute(['psql','-X','-q','-At','-v','ON_ERROR_STOP=1'], input=
          f'BEGIN; SET search_path TO {SCHEMA},public;\n'+preparation+migration+'\nROLLBACK;', check=False)
        assert result.returncode != 0 and reason in result.stderr.lower()
    checks['video_read_permission_refuses_missing_parent_collision_duplicate_incompatible'] = 'passed'


def validate_video_process_upgrade(execute, sql, checks):
    migration = (HERE.parents[1] / 'cn-universal-db-migrator/src/main/resources/db/migration/postgresql/'
                 'V2026101304__video_alarm_processes.sql').read_text()
    shape = f"""SELECT jsonb_build_object(
      'columns',(SELECT jsonb_agg(to_jsonb(c) ORDER BY table_name,ordinal_position) FROM
        (SELECT table_name,column_name,data_type,character_maximum_length,is_nullable,column_default,ordinal_position
         FROM information_schema.columns WHERE table_schema='{SCHEMA}' AND table_name IN ('video_sdk_alarm_process','video_sdk_alarm_process_member'))c),
      'constraints',(SELECT jsonb_agg(jsonb_build_array(t.relname,c.conname,pg_get_constraintdef(c.oid)) ORDER BY t.relname,c.conname)
        FROM pg_constraint c JOIN pg_class t ON t.oid=c.conrelid JOIN pg_namespace n ON n.oid=t.relnamespace
        WHERE n.nspname='{SCHEMA}' AND t.relname IN ('video_sdk_alarm_process','video_sdk_alarm_process_member')),
      'indexes',(SELECT jsonb_agg(jsonb_build_array(tablename,indexname,indexdef) ORDER BY tablename,indexname)
        FROM pg_indexes WHERE schemaname='{SCHEMA}' AND tablename IN ('video_sdk_alarm_process','video_sdk_alarm_process_member')))::text;"""
    expected = sql(f'SET search_path TO {SCHEMA},public;\n'+shape).splitlines()[-1]
    remove = f'DROP TABLE {SCHEMA}.video_sdk_alarm_process_member; DROP TABLE {SCHEMA}.video_sdk_alarm_process;'
    result = execute(['psql','-X','-q','-At','-v','ON_ERROR_STOP=1'], input=
        f'BEGIN; SET search_path TO {SCHEMA},public;\n'+remove+'\n'+migration+'\n'+shape+'\nROLLBACK;')
    if result.stdout.strip() != expected:
        Path('/tmp/nexiot-process-schema-expected.json').write_text(expected)
        Path('/tmp/nexiot-process-schema-actual.json').write_text(result.stdout.strip())
        raise AssertionError('Video process bootstrap/upgrade schema differs; diagnostic shapes saved')
    checks['video_process_bootstrap_upgrade_schema_equivalence'] = 'passed'
    process = """INSERT INTO video_sdk_alarm_process(id,tenant_id,instance_key,device_id,channel_id,row_kind,state,created_at,updated_at)
      VALUES('process','tenant','isup','nvr','33','process','unknown',1,1);"""
    member = """INSERT INTO video_sdk_alarm_process_member(receipt_id,process_id,tenant_id,instance_key,device_id,channel_id,event_id,alarm_state,alarm_time,received_at)
      VALUES('receipt','process','tenant','isup','nvr','33','native-event','unknown',1,1);"""
    duplicate = execute(['psql','-X','-q','-At','-v','ON_ERROR_STOP=1'], input=
        f'BEGIN; SET search_path TO {SCHEMA},public;\n'+process+member+member+'ROLLBACK;', check=False)
    assert duplicate.returncode != 0 and 'video_alarm_process_member_pkey' in duplicate.stderr
    cross = execute(['psql','-X','-q','-At','-v','ON_ERROR_STOP=1'], input=
        f'BEGIN; SET search_path TO {SCHEMA},public;\n'+process+member.replace("'tenant'","'foreign'")+'ROLLBACK;', check=False)
    assert cross.returncode != 0 and 'fk_video_alarm_member_scope' in cross.stderr
    checks['video_process_member_identity_and_scope_enforced'] = 'passed'


def validate_point_template_upgrade(execute, sql, checks, *,
                                    migration_name="V2026101301__industrial_point_templates.sql",
                                    check_prefix="point_template"):
    """Compare latest bootstrap against an old-template schema upgraded in a separate DB."""
    migration = (HERE.parents[1] / "cn-universal-db-migrator/src/main/resources/db/migration/postgresql"
                 / migration_name).read_text()
    database = "nexiot_point_upgrade"
    sql("CREATE DATABASE " + database + ";")
    for filename in ("001-schema.sql", "010-dictionaries.sql", "020-platform-admin.sql",
                     "030-platform-config.sql", "040-system-components.sql", "090-constraints-indexes.sql",
                     "095-reset-sequences.sql", "099-verify-clean-seed.sql"):
        sql((HERE / "postgres" / filename).read_text(), database=database)
    shape = f"""
      SELECT jsonb_build_object(
        'columns',(SELECT jsonb_agg(to_jsonb(c) ORDER BY column_name) FROM
          (SELECT column_name,data_type,character_maximum_length,is_nullable,column_default
           FROM information_schema.columns WHERE table_schema='{SCHEMA}'
             AND table_name='iot_industrial_config_template') c),
        'provenance',(SELECT jsonb_agg(to_jsonb(c) ORDER BY table_name,column_name) FROM
          (SELECT table_name,column_name,data_type,is_nullable,column_default FROM information_schema.columns
           WHERE table_schema='{SCHEMA}' AND table_name IN ('iot_industrial_point','iot_industrial_collect_group')
             AND column_name IN ('source_template_id','source_template_version')) c),
        'constraints',(SELECT jsonb_agg(jsonb_build_array(conname,pg_get_constraintdef(oid),convalidated) ORDER BY conname)
           FROM pg_constraint WHERE conrelid='{SCHEMA}.iot_industrial_config_template'::regclass),
        'indexes',(SELECT jsonb_agg(jsonb_build_array(indexname,indexdef) ORDER BY indexname)
           FROM pg_indexes WHERE schemaname='{SCHEMA}' AND tablename='iot_industrial_config_template'),
        'menus',(SELECT jsonb_agg(jsonb_build_array(menu_name,parent_id,order_num,path,component,menu_type,perms,icon) ORDER BY menu_id)
           FROM {SCHEMA}.sys_menu WHERE perms LIKE 'industrial:point-template:%' OR parent_id=0 AND path='device-template'),
        'roles',(SELECT jsonb_agg(jsonb_build_array(r.role_id,m.perms,m.path) ORDER BY r.role_id,m.menu_id)
           FROM {SCHEMA}.sys_role_menu r JOIN {SCHEMA}.sys_menu m ON m.menu_id=r.menu_id
           WHERE m.perms LIKE 'industrial:point-template:%' OR m.parent_id=0 AND m.path='device-template'),
        'packages',(SELECT jsonb_agg(jsonb_build_array(r.package_id,m.perms,m.path) ORDER BY r.package_id,m.menu_id)
           FROM {SCHEMA}.iot_tenant_package_menu r JOIN {SCHEMA}.sys_menu m ON m.menu_id=r.menu_id
           WHERE m.perms LIKE 'industrial:point-template:%' OR m.parent_id=0 AND m.path='device-template')
      )::text;
    """
    expected = sql(shape, database=database)
    prefix = f"SET search_path TO {SCHEMA},public;\n"
    downgrade = prefix + """
      DELETE FROM sys_role_menu WHERE menu_id IN (SELECT menu_id FROM sys_menu WHERE perms LIKE 'industrial:point-template:%' OR parent_id=0 AND path='device-template');
      DELETE FROM iot_tenant_package_menu WHERE menu_id IN (SELECT menu_id FROM sys_menu WHERE perms LIKE 'industrial:point-template:%' OR parent_id=0 AND path='device-template');
      DELETE FROM sys_menu WHERE perms LIKE 'industrial:point-template:%' OR parent_id=0 AND path='device-template';
      ALTER TABLE iot_industrial_point DROP COLUMN source_template_id,DROP COLUMN source_template_version;
      ALTER TABLE iot_industrial_collect_group DROP COLUMN source_template_id,DROP COLUMN source_template_version;
      DROP INDEX idx_iot_ind_config_template_point_filter;
      ALTER TABLE iot_industrial_config_template
        DROP CONSTRAINT ck_iot_ind_config_template_type,
        DROP CONSTRAINT ck_iot_ind_config_template_status,
        DROP CONSTRAINT ck_iot_ind_config_template_binding,
        DROP CONSTRAINT ck_iot_ind_config_template_revision,
        DROP COLUMN template_type,DROP COLUMN protocol,DROP COLUMN status,DROP COLUMN draft_content,DROP COLUMN draft_revision;
    """
    sql(downgrade, database=database)
    sql(prefix + """
      INSERT INTO iot_industrial_config_template(id,tenant_id,template_key,name,create_by,update_by)
        VALUES(987654321,'acceptance','legacy-config','Preserved legacy configuration','acceptance','acceptance');
      INSERT INTO iot_industrial_config_template_version(id,template_id,version,schema_version,content,content_hash,create_by)
        VALUES(987654321,987654321,1,1,'{"scope":"CONFIG","points":[],"groups":[]}',repeat('a',64),'acceptance');
    """, database=database)
    legacy = sql(f"SELECT md5(row_to_json(t)::text) FROM {SCHEMA}.iot_industrial_config_template t WHERE id=987654321;", database=database)
    version = sql(f"SELECT md5(row_to_json(t)::text) FROM {SCHEMA}.iot_industrial_config_template_version t WHERE id=987654321;", database=database)
    def migrate(*, check=True):
        return execute(["psql", "-X", "-q", "-At", "-1", "-v", "ON_ERROR_STOP=1"],
                       database=database, input=prefix + migration, check=check)
    migrate()
    assert sql(shape, database=database) == expected
    # Compare original row JSON with newly appended fields removed without JSON key ordering differences.
    original = sql(f"SELECT md5(row_to_json(t)::text) FROM (SELECT id,tenant_id,template_key,name,description,source_product_key,source_device_id,endpoint_mode,latest_version,create_by,update_by,create_time,update_time FROM {SCHEMA}.iot_industrial_config_template WHERE id=987654321) t;", database=database)
    assert original == legacy
    assert sql(f"SELECT md5(row_to_json(t)::text) FROM {SCHEMA}.iot_industrial_config_template_version t WHERE id=987654321;", database=database) == version
    checks[check_prefix + "_bootstrap_matches_incremental_schema_and_permissions"] = "passed"
    checks[check_prefix + "_migration_preserves_legacy_templates_and_versions"] = "passed"
    migrate()
    assert sql(shape, database=database) == expected
    checks[check_prefix + "_migration_repeated_without_duplicate_permissions"] = "passed"
    invalid = execute(["psql", "-X", "-q", "-At", "-1", "-v", "ON_ERROR_STOP=1"], database=database,
                      input=prefix + "INSERT INTO iot_industrial_config_template(tenant_id,template_key,name,template_type,create_by,update_by) VALUES('acceptance','invalid','invalid','POINTS','acceptance','acceptance');", check=False)
    assert invalid.returncode != 0 and "ck_iot_ind_config_template_binding" in invalid.stderr
    sql(prefix + """
      INSERT INTO iot_industrial_config_template(tenant_id,template_key,name,template_type,source_product_key,protocol,status,latest_version,draft_content,create_by,update_by)
      VALUES('acceptance','points','Bound points and groups','POINTS','product-a','S7','DRAFT',0,
             '{"modelProperties":[{"id":"running"}],"groups":[{"key":"slow"}],"points":[{"identifier":"running"}]}','acceptance','acceptance');
    """, database=database)
    checks[check_prefix + "_requires_product_and_protocol_and_keeps_model_group_draft"] = "passed"
    # A separately committed partial preparation can be retried; a wrong definition is rejected.
    sql(prefix + "ALTER TABLE iot_industrial_config_template DROP CONSTRAINT ck_iot_ind_config_template_status;", database=database)
    migrate()
    assert sql(shape, database=database) == expected
    checks[check_prefix + "_partial_preparation_safe_retry"] = "passed"
    sql(prefix + "ALTER TABLE iot_industrial_config_template DROP CONSTRAINT ck_iot_ind_config_template_status; ALTER TABLE iot_industrial_config_template ADD CONSTRAINT ck_iot_ind_config_template_status CHECK(true);", database=database)
    incompatible = migrate(check=False)
    assert incompatible.returncode != 0 and "Incompatible point-template constraint" in incompatible.stderr
    checks[check_prefix + "_incompatible_constraint_refused"] = "passed"
    sql("DROP DATABASE " + database + ";")


def validate_migrator_baseline(execute, sql, checks, *, jar, host_port, password):
    """Exercise the real runner against Docker's empty default history metadata."""
    database = "nexiot_runner_baseline"
    sql("CREATE DATABASE " + database + ";")
    for filename in ("001-schema.sql", "010-dictionaries.sql", "020-platform-admin.sql",
                     "030-platform-config.sql", "040-system-components.sql", "090-constraints-indexes.sql",
                     "095-reset-sequences.sql", "099-verify-clean-seed.sql"):
        sql((HERE / "postgres" / filename).read_text(), database=database)
    prefix = f"SET search_path TO {SCHEMA},public;\n"
    sql(prefix + """
      DELETE FROM sys_role_menu WHERE menu_id BETWEEN 1700 AND 1706;
      DELETE FROM iot_tenant_package_menu WHERE menu_id BETWEEN 1700 AND 1706;
      DELETE FROM sys_menu WHERE menu_id BETWEEN 1700 AND 1706;
      DROP INDEX idx_iot_ind_config_template_point_filter;
      ALTER TABLE iot_industrial_point DROP COLUMN source_template_id,DROP COLUMN source_template_version;
      ALTER TABLE iot_industrial_collect_group DROP COLUMN source_template_id,DROP COLUMN source_template_version;
      ALTER TABLE iot_industrial_config_template
        DROP CONSTRAINT ck_iot_ind_config_template_type,DROP CONSTRAINT ck_iot_ind_config_template_status,
        DROP CONSTRAINT ck_iot_ind_config_template_binding,DROP CONSTRAINT ck_iot_ind_config_template_revision,
        DROP COLUMN template_type,DROP COLUMN protocol,DROP COLUMN status,DROP COLUMN draft_content,DROP COLUMN draft_revision;
    """, database=database)
    env = os.environ | {"DB_MIGRATOR_URL": f"jdbc:postgresql://127.0.0.1:{host_port}/{database}?currentSchema={SCHEMA}",
                        "DB_MIGRATOR_USERNAME": "nexiot", "DB_MIGRATOR_PASSWORD": password,
                        "DB_MIGRATOR_BASELINE_VERSION": "2026101001", "DB_MIGRATOR_TARGET": "2026101301",
                        "DB_MIGRATOR_HISTORY_TABLE": "flyway_schema_history"}
    def runner(action, *, confirmed=True, expected=0, history="flyway_schema_history",
               baseline_version="2026101001", schema_name=SCHEMA, target="2026101301"):
        current = env | {"DB_MIGRATOR_CONFIRM_BASELINE": "YES" if confirmed else "NO",
                         "DB_MIGRATOR_HISTORY_TABLE": history,
                         "DB_MIGRATOR_URL": f"jdbc:postgresql://127.0.0.1:{host_port}/{database}?currentSchema={schema_name}",
                         "DB_MIGRATOR_TARGET": target}
        if baseline_version is None:
            current.pop("DB_MIGRATOR_BASELINE_VERSION", None)
        else:
            current["DB_MIGRATOR_BASELINE_VERSION"] = baseline_version
        result = run(["java", "-jar", str(jar), action], env=current)
        if (result.returncode == 0) != (expected == 0):
            raise RuntimeError("Migrator acceptance failed: " + result.stderr[-3000:])
        return result
    runner("migrate", expected=1)
    assert sql(f"SELECT count(*) FROM {SCHEMA}.flyway_schema_history;", database=database) == "0"
    assert sql(f"SELECT count(*) FROM information_schema.columns WHERE table_schema='{SCHEMA}' AND table_name='iot_industrial_config_template' AND column_name='template_type';", database=database) == "0"
    checks["migrator_refuses_empty_history_without_historical_schema_changes"] = "passed"
    sql("CREATE SCHEMA baseline_missing; CREATE TABLE baseline_missing.acceptance_marker(value TEXT); INSERT INTO baseline_missing.acceptance_marker VALUES('preserved');", database=database)
    runner("migrate", expected=1, schema_name="baseline_missing")
    assert sql("SELECT to_regclass('baseline_missing.flyway_schema_history') IS NULL;", database=database) == "t"
    assert sql("SELECT value FROM baseline_missing.acceptance_marker;", database=database) == "preserved"
    checks["migrator_refuses_missing_history_without_creating_metadata"] = "passed"
    no_version = runner("baseline", expected=1, baseline_version=None)
    assert "DB_MIGRATOR_BASELINE_VERSION must be set" in no_version.stderr
    assert sql(f"SELECT count(*) FROM {SCHEMA}.flyway_schema_history;", database=database) == "0"
    checks["migrator_baseline_requires_explicit_installed_version"] = "passed"
    runner("baseline", confirmed=False, expected=1)
    assert sql(f"SELECT count(*) FROM {SCHEMA}.flyway_schema_history;", database=database) == "0"
    checks["migrator_baseline_requires_explicit_confirmation_and_keeps_empty_history"] = "passed"
    runner("baseline")
    for action in ("validate", "migrate", "validate", "migrate"):
        runner(action)
    assert sql(f"SELECT count(*) FROM {SCHEMA}.flyway_schema_history WHERE version='2026101301' AND success;", database=database) == "1"
    checks["migrator_default_empty_history_baseline_validate_migrate_repeat"] = "passed"
    history_hash = f"SELECT md5(jsonb_agg(to_jsonb(h) ORDER BY installed_rank)::text) FROM {SCHEMA}.flyway_schema_history h;"
    before = sql(history_hash, database=database)
    runner("baseline")
    assert sql(history_hash, database=database) == before
    checks["migrator_baseline_preserves_nonempty_applied_history"] = "passed"
    sql(prefix + "CREATE TABLE acceptance_empty_history (LIKE flyway_schema_history INCLUDING ALL);", database=database)
    rejected = runner("baseline", expected=1, history="acceptance_empty_history")
    assert "not the official bootstrap shape" in rejected.stderr
    assert sql(f"SELECT count(*) FROM {SCHEMA}.acceptance_empty_history;", database=database) == "0"
    checks["migrator_baseline_refuses_custom_empty_history"] = "passed"
    sql(prefix + "CREATE SCHEMA baseline_incompatible; CREATE TABLE baseline_incompatible.flyway_schema_history (LIKE flyway_schema_history INCLUDING ALL); ALTER TABLE baseline_incompatible.flyway_schema_history ADD COLUMN custom_value TEXT;", database=database)
    incompatible_env = env | {"DB_MIGRATOR_URL": f"jdbc:postgresql://127.0.0.1:{host_port}/{database}?currentSchema=baseline_incompatible",
                              "DB_MIGRATOR_CONFIRM_BASELINE": "YES"}
    rejected = run(["java", "-jar", str(jar), "baseline"], env=incompatible_env)
    assert rejected.returncode != 0 and "not the official bootstrap shape" in rejected.stderr
    assert sql("SELECT count(*) FROM baseline_incompatible.flyway_schema_history;", database=database) == "0"
    checks["migrator_baseline_refuses_modified_empty_default_history"] = "passed"
    # Clone the exact official empty metadata shape in separate schemas, then customize
    # only ACL or indexes so the guard's rejection has a specific, isolated cause.
    for scenario in ("acl", "column_acl", "index", "default", "identity"):
        current_schema = "baseline_" + scenario
        sql(f"CREATE SCHEMA {current_schema}; CREATE TABLE {current_schema}.flyway_schema_history (LIKE {SCHEMA}.flyway_schema_history INCLUDING DEFAULTS); ALTER TABLE {current_schema}.flyway_schema_history ADD CONSTRAINT flyway_schema_history_pk PRIMARY KEY(installed_rank); CREATE INDEX flyway_schema_history_s_idx ON {current_schema}.flyway_schema_history(success);", database=database)
        if scenario == "acl":
            sql(f"GRANT SELECT ON {current_schema}.flyway_schema_history TO PUBLIC;", database=database)
        elif scenario == "column_acl":
            sql(f"GRANT SELECT(installed_rank) ON {current_schema}.flyway_schema_history TO PUBLIC;", database=database)
        elif scenario == "index":
            sql(f"CREATE INDEX customized_history_version ON {current_schema}.flyway_schema_history(version);", database=database)
        elif scenario == "default":
            sql(f"ALTER TABLE {current_schema}.flyway_schema_history ALTER COLUMN installed_on SET DEFAULT timezone('UTC', now());", database=database)
        else:
            sql(f"ALTER TABLE {current_schema}.flyway_schema_history ALTER COLUMN installed_rank ADD GENERATED BY DEFAULT AS IDENTITY;", database=database)
        before = sql(f"SELECT md5(jsonb_agg(jsonb_build_array(c.relname,c.relacl::text,pg_get_indexdef(c.oid)) ORDER BY c.relname)::text) FROM pg_class c JOIN pg_namespace n ON n.oid=c.relnamespace WHERE n.nspname='{current_schema}';", database=database)
        rejected = runner("baseline", expected=1, schema_name=current_schema)
        assert "not the official bootstrap shape" in rejected.stderr
        after = sql(f"SELECT md5(jsonb_agg(jsonb_build_array(c.relname,c.relacl::text,pg_get_indexdef(c.oid)) ORDER BY c.relname)::text) FROM pg_class c JOIN pg_namespace n ON n.oid=c.relnamespace WHERE n.nspname='{current_schema}';", database=database)
        assert before == after
        checks["migrator_baseline_refuses_custom_empty_history_" + scenario] = "passed"
    # A newly initialized latest seed must baseline at its installed version so historical
    # DDL stays below the baseline. This is deliberately separate from the old-schema path.
    for filename in ("001-schema.sql", "010-dictionaries.sql", "020-platform-admin.sql",
                     "030-platform-config.sql", "040-system-components.sql", "090-constraints-indexes.sql",
                     "095-reset-sequences.sql", "099-verify-clean-seed.sql"):
        content = (HERE / "postgres" / filename).read_text().replace(SCHEMA, "baseline_latest")
        sql(content, database=database)
    installed_version = json.loads((HERE / "postgres/manifest.json").read_text())["installed_migration_version"]
    for action in ("baseline", "migrate", "validate"):
        runner(action, baseline_version=installed_version, schema_name="baseline_latest", target=installed_version)
    assert sql("SELECT count(*) FROM baseline_latest.flyway_schema_history;", database=database) == "1"
    assert sql("SELECT type || ':' || version FROM baseline_latest.flyway_schema_history;", database=database) == "BASELINE:" + installed_version
    checks["migrator_latest_seed_baseline_does_not_replay_historical_sql"] = "passed"
    sql("CREATE SCHEMA oss_scope_upgrade;", database=database)
    for filename in ("001-schema.sql", "010-dictionaries.sql", "020-platform-admin.sql",
                     "030-platform-config.sql", "040-system-components.sql", "090-constraints-indexes.sql",
                     "095-reset-sequences.sql"):
        content = (HERE / "postgres" / filename).read_text().replace(SCHEMA, "oss_scope_upgrade")
        # 001 creates the schema itself.
        if filename == "001-schema.sql":
            sql("DROP SCHEMA oss_scope_upgrade;", database=database)
        sql(content, database=database)
    sql("INSERT INTO oss_scope_upgrade.sys_menu(menu_id,menu_name,parent_id,path,menu_type,status,perms) "
        "VALUES (1700,'Legacy excluded menu',0,'device-template','M','0','');", database=database)
    for action in ("baseline", "migrate", "validate", "migrate", "validate"):
        runner(action, baseline_version="2026110302", schema_name="oss_scope_upgrade", target="2026110701")
    assert sql("SELECT count(*) FROM oss_scope_upgrade.sys_menu WHERE menu_id=1700;", database=database) == '0'
    assert sql("SELECT count(*) FROM oss_scope_upgrade.flyway_schema_history WHERE version='2026110701' AND success;", database=database) == '1'
    checks['oss_scope_real_runner_upgrade_once_repeat_zero_validate'] = 'passed'
    sql("DROP DATABASE " + database + ";")


def validate_industrial_console_upgrade(execute, sql, checks):
    migration = (HERE.parents[1] / 'cn-universal-db-migrator/src/main/resources/db/migration/postgresql/'
                 'V2026101403__retire_industrial_console.sql').read_text()
    shape = f"""SELECT jsonb_build_object(
      'menus',(SELECT jsonb_agg(to_jsonb(m) ORDER BY menu_id) FROM {SCHEMA}.sys_menu m),
      'roles',(SELECT jsonb_agg(to_jsonb(r) ORDER BY uuid) FROM {SCHEMA}.sys_role_menu r),
      'packages',(SELECT jsonb_agg(to_jsonb(p) ORDER BY id) FROM {SCHEMA}.iot_tenant_package_menu p))::text;"""
    expected = sql(shape)
    # Reconstruct the removed subtree without changing retained IDs or grants.
    fixture = """INSERT INTO sys_menu(menu_id,menu_name,parent_id,path,menu_type,status,perms) VALUES
      (2062600000000000001,'legacy console',1097,'industrial','C','0','industrial:runtime:query'),
      (2062600000000000011,'legacy endpoint add',2062600000000000001,'#','F','0','industrial:endpoint:add'),
      (987670,'nested legacy button',2062600000000000011,'#','F','0','industrial:legacy:fixture');
      UPDATE sys_menu SET parent_id=2062600000000000001 WHERE parent_id=1098 AND perms LIKE 'industrial:%'
        AND perms<>'industrial:endpoint:probe';
      INSERT INTO sys_role_menu(role_id,menu_id,uuid) VALUES (1,2062600000000000001,987670),(1,987670,987671);
      INSERT INTO iot_tenant_package_menu(package_id,menu_id,id) VALUES
        (900000000000000001,2062600000000000001,987670),(900000000000000001,987670,987671);
    """
    prefix = f'BEGIN; SET search_path TO {SCHEMA},public;\n'
    result = execute(['psql','-X','-qAt','-v','ON_ERROR_STOP=1'],
                     input=prefix+fixture+migration+shape+migration+shape+'ROLLBACK;')
    assert result.stdout.strip().splitlines() == [expected, expected]
    checks['industrial_console_bootstrap_upgrade_repeat_and_recursive_cleanup'] = 'passed'
    partial = "UPDATE sys_menu SET parent_id=1098 WHERE menu_id=2062600000000000051;"
    result = execute(['psql','-X','-qAt','-v','ON_ERROR_STOP=1'],
                     input=prefix+fixture+partial+migration+shape+'ROLLBACK;')
    assert result.stdout.strip() == expected
    checks['industrial_console_partial_reparent_retry_keeps_grants'] = 'passed'
    root_only = """INSERT INTO sys_role(role_id,role_name,role_key,role_sort,status) VALUES
      (987670,'legacy reader','legacy_reader_fixture',1,'0'),(987671,'unrelated','unrelated_fixture',1,'0');
      INSERT INTO iot_tenant_package(package_id,package_name) VALUES (987670,'legacy reader'),(987671,'unrelated');
      INSERT INTO sys_role_menu(role_id,menu_id,uuid) VALUES (987670,2062600000000000001,987672);
      INSERT INTO iot_tenant_package_menu(package_id,menu_id,id) VALUES (987670,2062600000000000001,987672);
    """
    query = """SELECT count(*) FROM sys_role_menu WHERE role_id=987670 AND menu_id=2062600000000000051;
      SELECT count(*) FROM iot_tenant_package_menu WHERE package_id=987670 AND menu_id=2062600000000000051;
      SELECT count(*) FROM sys_role_menu WHERE role_id=987671;
      SELECT count(*) FROM iot_tenant_package_menu WHERE package_id=987671;"""
    result = execute(['psql','-X','-qAt','-v','ON_ERROR_STOP=1'],
                     input=prefix+fixture+root_only+migration+query+migration+query+'ROLLBACK;')
    assert result.stdout.strip().splitlines() == ['1','1','0','0'] * 2
    checks['industrial_console_root_only_read_preserved_without_unrelated_grants'] = 'passed'
    for invalid in ("UPDATE sys_menu SET perms='wrong' WHERE menu_id=1098;",
                    "UPDATE sys_menu SET path='wrong' WHERE menu_id=2062600000000000001;",
                    "UPDATE sys_menu SET status='1' WHERE menu_id=2062600000000000051;"):
        result = execute(['psql','-X','-qAt','-v','ON_ERROR_STOP=1'],
                         input=prefix+fixture+invalid+migration+'COMMIT;', check=False)
        assert result.returncode != 0 and 'incompatible' in result.stderr.lower()
    assert sql(shape) == expected
    checks['industrial_console_incompatible_menu_aborts_without_changes'] = 'passed'


def validate_industrial_probe_permission_upgrade(execute, sql, checks):
    migration = (HERE.parents[1] / 'cn-universal-db-migrator/src/main/resources/db/migration/postgresql/'
                 'V2026101801__industrial_endpoint_probe_permission.sql').read_text()
    shape = f"""SELECT jsonb_build_object(
      'menus',(SELECT jsonb_agg(jsonb_build_array(menu_id,menu_name,parent_id,order_num,menu_type,status,perms) ORDER BY menu_id)
        FROM {SCHEMA}.sys_menu WHERE perms='industrial:endpoint:probe'),
      'roles',(SELECT jsonb_agg(r.role_id ORDER BY r.role_id) FROM {SCHEMA}.sys_role_menu r
        JOIN {SCHEMA}.sys_menu m ON m.menu_id=r.menu_id WHERE m.perms='industrial:endpoint:probe'),
      'packages',(SELECT jsonb_agg(r.package_id ORDER BY r.package_id) FROM {SCHEMA}.iot_tenant_package_menu r
        JOIN {SCHEMA}.sys_menu m ON m.menu_id=r.menu_id WHERE m.perms='industrial:endpoint:probe'))::text;"""
    expected = sql(shape)
    remove = """DELETE FROM sys_role_menu WHERE menu_id IN (SELECT menu_id FROM sys_menu WHERE perms='industrial:endpoint:probe');
      DELETE FROM iot_tenant_package_menu WHERE menu_id IN (SELECT menu_id FROM sys_menu WHERE perms='industrial:endpoint:probe');
      DELETE FROM sys_menu WHERE perms='industrial:endpoint:probe';"""
    prefix = f'BEGIN; SET search_path TO {SCHEMA},public;\n'
    result = execute(['psql','-X','-qAt','-v','ON_ERROR_STOP=1'],
                     input=prefix+remove+migration+shape+migration+shape+'ROLLBACK;')
    assert result.stdout.strip().splitlines() == [expected, expected]
    checks['industrial_probe_permission_bootstrap_upgrade_and_repeat_equivalence'] = 'passed'

    fixtures = """INSERT INTO sys_role(role_id,role_name,role_key,role_sort,status) VALUES
      (987681,'industrial editor','industrial_editor_fixture',1,'0'),
      (987682,'industrial reader','industrial_reader_fixture',2,'0'),
      (987683,'unrelated','unrelated_fixture',3,'0');
      INSERT INTO sys_role_menu(role_id,menu_id,uuid)
      SELECT 987681,menu_id,987681 FROM sys_menu WHERE perms='industrial:device:edit';
      INSERT INTO sys_role_menu(role_id,menu_id,uuid)
      SELECT 987682,menu_id,987682 FROM sys_menu WHERE perms='industrial:runtime:query' AND menu_type='F' LIMIT 1;
      INSERT INTO iot_tenant_package(package_id,package_name) VALUES
      (987681,'industrial editor'),(987682,'industrial reader'),(987683,'unrelated');
      INSERT INTO iot_tenant_package_menu(package_id,menu_id,id)
      SELECT 987681,menu_id,987681 FROM sys_menu WHERE perms='industrial:device:edit';
      INSERT INTO iot_tenant_package_menu(package_id,menu_id,id)
      SELECT 987682,menu_id,987682 FROM sys_menu WHERE perms='industrial:runtime:query' AND menu_type='F' LIMIT 1;
      DELETE FROM sys_role_menu WHERE role_id=1 AND menu_id IN (SELECT menu_id FROM sys_menu WHERE perms='industrial:device:edit');"""
    grants = """SELECT count(*) FROM sys_role_menu r JOIN sys_menu m ON m.menu_id=r.menu_id
      WHERE r.role_id=987681 AND m.perms='industrial:endpoint:probe';
      SELECT count(*) FROM sys_role_menu r JOIN sys_menu m ON m.menu_id=r.menu_id
      WHERE r.role_id IN (1,987682,987683) AND m.perms='industrial:endpoint:probe';
      SELECT count(*) FROM iot_tenant_package_menu r JOIN sys_menu m ON m.menu_id=r.menu_id
      WHERE r.package_id=987681 AND m.perms='industrial:endpoint:probe';
      SELECT count(*) FROM iot_tenant_package_menu r JOIN sys_menu m ON m.menu_id=r.menu_id
      WHERE r.package_id IN (987682,987683) AND m.perms='industrial:endpoint:probe';
      SELECT count(*) FROM sys_role_menu r JOIN sys_menu m ON m.menu_id=r.menu_id
      WHERE r.role_id IN (987681,987682,987683) AND m.perms IN
        ('industrial:endpoint:start','industrial:endpoint:stop','industrial:runtime:write');
      SELECT count(*) FROM iot_tenant_package_menu r JOIN sys_menu m ON m.menu_id=r.menu_id
      WHERE r.package_id IN (987681,987682,987683) AND m.perms IN
        ('industrial:endpoint:start','industrial:endpoint:stop','industrial:runtime:write');"""
    result = execute(['psql','-X','-qAt','-v','ON_ERROR_STOP=1'],
                     input=prefix+remove+fixtures+migration+grants+migration+grants+'ROLLBACK;')
    assert result.stdout.strip().splitlines() == ['1','0','1','0','0','0'] * 2
    checks['industrial_probe_permission_inherits_device_edit_only_without_admin_or_runtime_grants'] = 'passed'

    partial = """INSERT INTO sys_menu(menu_id,menu_name,parent_id,order_num,menu_type,status,perms)
      VALUES(987689,'Existing connection probe',1098,120,'F','0','industrial:endpoint:probe');"""
    identity = """SELECT menu_id FROM sys_menu WHERE perms='industrial:endpoint:probe';
      SELECT count(*) FROM sys_menu WHERE menu_id=2071801000000000001;"""
    result = execute(['psql','-X','-qAt','-v','ON_ERROR_STOP=1'],
                     input=prefix+remove+fixtures+partial+migration+identity+grants+migration+identity+grants+'ROLLBACK;')
    assert result.stdout.strip().splitlines() == ['987689','0','1','0','1','0','0','0'] * 2
    checks['industrial_probe_permission_partial_alternate_id_keeps_identity_recovers_grants'] = 'passed'

    for preparation in ("UPDATE sys_menu SET status='1' WHERE perms='industrial:device:edit';",
                        "UPDATE sys_menu SET menu_type='C' WHERE perms='industrial:device:edit';",
                        "UPDATE sys_menu SET parent_id=0 WHERE perms='industrial:device:edit';",
                        "UPDATE sys_menu SET perms='unrelated:fixture' WHERE perms='industrial:device:edit';"):
        result = execute(['psql','-X','-qAt','-v','ON_ERROR_STOP=1'],
                         input=prefix+remove+fixtures+preparation+migration+grants+migration+grants+'ROLLBACK;')
        assert result.stdout.strip().splitlines() == ['0'] * 12
    checks['industrial_probe_permission_ignores_inactive_misplaced_nonbutton_or_missing_edit_source'] = 'passed'

    cases = [
      "UPDATE sys_menu SET menu_type='C' WHERE perms='industrial:endpoint:probe';",
      "UPDATE sys_menu SET status='1' WHERE perms='industrial:endpoint:probe';",
      "UPDATE sys_menu SET parent_id=0 WHERE perms='industrial:endpoint:probe';",
      "UPDATE sys_menu SET perms='unrelated:fixture' WHERE menu_id=2071801000000000001;",
      "UPDATE sys_menu SET perms='unrelated:fixture' WHERE menu_id=1098;",
      "UPDATE sys_menu SET status='1' WHERE menu_id=1098;",
      "UPDATE sys_menu SET menu_type='F' WHERE menu_id=1098;",
      "INSERT INTO sys_menu(menu_id,menu_name,parent_id,menu_type,status,perms) VALUES(987689,'duplicate',1098,'F','0','industrial:endpoint:probe');"]
    for preparation in cases:
        result = execute(['psql','-X','-qAt','-v','ON_ERROR_STOP=1'],
                         input=prefix+preparation+migration+'COMMIT;', check=False)
        assert result.returncode != 0
    assert sql(shape) == expected
    checks['industrial_probe_permission_refuses_incompatible_parent_collision_duplicate_or_menu'] = 'passed'


def validate_history_hypertables(execute, sql, checks):
    migration = (HERE.parents[1] / 'cn-universal-db-migrator/src/main/resources/db/migration/postgresql/'
                 'V2026101901__history_hypertables.sql').read_text()
    has_extension = sql("SELECT EXISTS(SELECT 1 FROM pg_extension WHERE extname='timescaledb');").splitlines()[-1] == 't'
    if not has_extension:
        result = sql(f'BEGIN; SET LOCAL search_path TO {SCHEMA},public;\n' + migration + migration + 'ROLLBACK;')
        checks['history_plain_postgres_optional_extension_repeat'] = 'passed'
        return
    initial = sql(f"SELECT count(*) FROM timescaledb_information.hypertables WHERE hypertable_schema='{SCHEMA}' AND hypertable_name ~ '^ts_iot_device_log(_metadata)?(_[0-9]+)?$';").splitlines()[-1]
    assert initial == '50', initial
    checks['history_bootstrap_all_50_native_hypertables'] = 'passed'
    prefix = f'''BEGIN; CREATE SCHEMA history_upgrade_acceptance;
      CREATE TEMP TABLE history_expected(table_name name, fingerprint jsonb);
      DO $fixture$ DECLARE item record; BEGIN
        FOR item IN SELECT c.relname FROM pg_class c JOIN pg_namespace n ON n.oid=c.relnamespace
          WHERE n.nspname='{SCHEMA}' AND c.relkind='r'
          AND c.relname ~ '^ts_iot_device_log(_metadata)?(_[0-9]+)?$'
        LOOP
          EXECUTE format('CREATE TABLE history_upgrade_acceptance.%I (LIKE {SCHEMA}.%I INCLUDING ALL)',item.relname,item.relname);
          EXECUTE format('INSERT INTO history_upgrade_acceptance.%I(id,iot_id,content,create_time) '
            'VALUES (7,''fixture'',''中文🙂'',''2026-09-30 23:59:59.999999''),'
            '(7,''fixture'',''boundary'',''2026-10-01 00:00:00''),'
            '(8,''fixture'',''latest'',''2026-10-19 12:30:00'')',item.relname);
          EXECUTE format('INSERT INTO history_expected SELECT %L, jsonb_agg(to_jsonb(t) ORDER BY create_time,id) '
            'FROM history_upgrade_acceptance.%I t',item.relname,item.relname);
        END LOOP;
      END $fixture$;
      SET LOCAL search_path TO history_upgrade_acceptance,public;
    '''
    verification = '''DO $verify$ DECLARE item record; actual jsonb; BEGIN
      FOR item IN SELECT * FROM history_expected LOOP
        EXECUTE format('SELECT jsonb_agg(to_jsonb(t) ORDER BY create_time,id) FROM history_upgrade_acceptance.%I t',item.table_name) INTO actual;
        IF actual IS DISTINCT FROM item.fingerprint THEN RAISE EXCEPTION 'History changed: %',item.table_name; END IF;
      END LOOP;
      IF (SELECT count(*) FROM timescaledb_information.hypertables WHERE hypertable_schema='history_upgrade_acceptance') <> 50
        THEN RAISE EXCEPTION 'Expected 50 upgraded hypertables'; END IF;
    END $verify$;'''
    sql(prefix + migration + verification + migration + verification + 'ROLLBACK;')
    checks['history_upgrade_all_50_repeat_preserves_every_fixture_column_and_timestamp'] = 'passed'
    for incompatible in (
        'ALTER TABLE history_upgrade_acceptance.ts_iot_device_log_0 ALTER COLUMN create_time DROP NOT NULL;',
        'CREATE UNIQUE INDEX fixture_bad_unique ON history_upgrade_acceptance.ts_iot_device_log_0(iot_id,id);',
    ):
        # Empty the fixture for the unrelated uniqueness constraint test.
        setup = prefix + 'TRUNCATE history_upgrade_acceptance.ts_iot_device_log_0;' + incompatible
        result = execute(['psql','-X','-qAt','-v','ON_ERROR_STOP=1'], input=setup + migration, check=False)
        assert result.returncode != 0, 'Incompatible history schema was accepted'
    checks['history_refuses_nullable_time_and_unique_key_without_time'] = 'passed'


def main():
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument("--image", default="postgres:18.1-alpine")
    p.add_argument("--migrator-jar", type=Path, help="Also verify real confirmed baseline/migration against this built runner Jar")
    p.add_argument("--report", type=Path, default=HERE.parent / "dist/database-validation.json")
    args = p.parse_args()
    if args.migrator_jar and not args.migrator_jar.is_file():
        p.error("--migrator-jar must point to the built all.jar")
    name = "nexiot-seed-acceptance-" + secrets.token_hex(4)
    checks = {}
    report = {"started_at": datetime.now(timezone.utc).isoformat(), "image": args.image, "checks": checks}
    start = time.monotonic()
    with tempfile.TemporaryDirectory(prefix="nexiot-seed-") as temporary:
        envfile = Path(temporary) / "postgres.env"
        password = secrets.token_urlsafe(32)
        envfile.write_text("POSTGRES_USER=nexiot\nPOSTGRES_DB=nexiot\nPOSTGRES_PASSWORD=" + password + "\n")
        envfile.chmod(0o600)
        created = False
        def execute(command, *, database="nexiot", check=True, input=None):
            result = run(["docker", "exec", "-i", "-e", "PGUSER=nexiot", "-e", "PGDATABASE=" + database, name] + command, input=input)
            if check and result.returncode:
                raise RuntimeError("Acceptance command failed: " + result.stderr[-3000:])
            return result
        def sql(query, *, database="nexiot"):
            return execute(["psql", "-X", "-At", "-v", "ON_ERROR_STOP=1"], database=database, input=query).stdout.strip()
        try:
            publish = ["-p", "127.0.0.1::5432"] if args.migrator_jar else []
            result = run(["docker", "run", "-d", "--name", name, "--env-file", str(envfile)] + publish + [
                          "-v", str(HERE) + ":/opt/nexiot-database:ro", args.image])
            if result.returncode:
                raise RuntimeError(result.stderr)
            created = True
            deadline = time.monotonic() + 120
            while time.monotonic() < deadline:
                ready = execute(["pg_isready", "-h", "127.0.0.1", "-U", "nexiot", "-d", "nexiot"], check=False)
                if ready.returncode == 0:
                    break
                state = run(["docker", "inspect", "--format", "{{.State.Status}}", name]).stdout.strip()
                if state in ("exited", "dead"):
                    raise RuntimeError(run(["docker", "logs", "--tail", "60", name]).stdout)
                time.sleep(1)
            else:
                raise RuntimeError("PostgreSQL bootstrap did not become ready within 120 seconds")
            execute(["sh", "/opt/nexiot-database/postgres/apply.sh"])
            manifest = json.loads((HERE / "postgres/manifest.json").read_text())
            sql((HERE / "postgres/099-verify-clean-seed.sql").read_text())
            checks["ordered_import_and_all_table_counts"] = "passed"
            report["server_version"] = sql("SHOW server_version;")
            report["platform"] = run(["docker", "image", "inspect", "--format", "{{.Os}}/{{.Architecture}}", args.image]).stdout.strip()
            report["image_id"] = run(["docker", "image", "inspect", "--format", "{{.Id}}", args.image]).stdout.strip()
            report["source_objects"] = manifest["source_objects"]
            report["seed_counts"] = manifest["seed_counts"]
            report["empty_tables"] = len(manifest["empty_tables"])
            report["total_core_rows"] = sum(manifest["seed_counts"].values())
            checks["constraints_indexes_and_owned_sequences"] = "passed"
            validate_history_hypertables(execute, sql, checks)
            validate_ota_transfer_upgrade(execute, sql, checks)
            validate_open_source_menu_upgrade(execute, sql, checks)
            validate_video_receipt_upgrade(execute, sql, checks)
            validate_wechat_login_upgrade(execute, sql, checks)
            validate_wechat_reply_upgrade(execute, sql, checks)
            validate_video_process_upgrade(execute, sql, checks)
            validate_wvp_event_dictionary_upgrade(execute, sql, checks)
            validate_uniview_sdk_dictionary_upgrade(execute, sql, checks)
            if args.migrator_jar:
                host_port = run(["docker", "port", name, "5432/tcp"]).stdout.strip().rsplit(":", 1)[1]
                validate_migrator_baseline(execute, sql, checks, jar=args.migrator_jar.resolve(), host_port=host_port, password=password)
            channel_check = execute(["psql", "-X", "-q", "-At", "-v", "ON_ERROR_STOP=1"], input=(
                f"BEGIN; INSERT INTO {SCHEMA}.video_platform_device(id,instance_key,device_id) "
                "VALUES (987654321,'channel-acceptance','device-a'),(987654322,'channel-acceptance','device-b'); "
                f"INSERT INTO {SCHEMA}.video_platform_channel "
                "(id,device_id,instance_key,platform_device_id,channel_id,parent_id) VALUES "
                "(987654321,987654321,'channel-acceptance','device-a','shared-channel','shared-parent'),"
                "(987654322,987654322,'channel-acceptance','device-b','shared-channel','shared-parent'); "
                f"INSERT INTO {SCHEMA}.video_platform_channel "
                "(id,device_id,instance_key,platform_device_id,channel_id,parent_id) VALUES "
                "(987654323,987654321,'channel-acceptance','device-a','shared-channel','other-parent') "
                "ON CONFLICT (device_id,channel_id) DO NOTHING; "
                f"SELECT count(*) FROM {SCHEMA}.video_platform_channel WHERE instance_key='channel-acceptance'; "
                "ROLLBACK;"))
            assert channel_check.stdout.strip() == "2"
            checks["shared_channel_ids_kept_per_device_without_migration"] = "passed"
            assert sql(f"SELECT data_type FROM information_schema.columns WHERE table_schema='{SCHEMA}' "
                       "AND table_name='video_ai_algorithm_task' AND column_name='error_message';") == "text"
            checks["algorithm_error_message_initialized_as_text"] = "passed"
            sql(f"INSERT INTO {SCHEMA}.video_ai_algorithm_task "
                "(id,task_code,task_name,task_type,model_id,status,error_message) "
                "VALUES (987654321,'seed-acceptance','preserved task','STREAM',1,'STARTING','original error');")
            sql(f"UPDATE {SCHEMA}.video_ai_algorithm_task SET status='FAILED', "
                "error_message=repeat('错误🙂',2000) WHERE id=987654321;")
            task_query = (f"SELECT status || ':' || length(error_message) || ':' || "
                          f"(error_message=repeat('错误🙂',2000)) FROM {SCHEMA}.video_ai_algorithm_task "
                          "WHERE id=987654321;")
            assert sql(task_query) == "FAILED:6000:true"
            checks["long_unicode_error_and_failed_status_saved_together"] = "passed"
            sql(f"UPDATE {SCHEMA}.iot_user SET alias='seed acceptance changed' WHERE id=1;")
            user_query = f"SELECT md5(row_to_json(u)::text) FROM {SCHEMA}.iot_user u WHERE id=1;"
            user_before = sql(user_query)
            sql("CREATE TABLE public.acceptance_marker(value text); INSERT INTO public.acceptance_marker VALUES('preserved');")
            for _ in range(2):
                repeat = execute(["sh", "/opt/nexiot-database/postgres/apply.sh"])
                assert "no data changed" in repeat.stdout
                assert sql("SELECT value FROM public.acceptance_marker;") == "preserved"
                assert sql(task_query) == "FAILED:6000:true"
                assert sql(user_query) == user_before
                assert sql(f"SELECT count(*) FROM {SCHEMA}.flyway_schema_history;") == "0"
            checks["repeated_bootstrap_preserves_existing_data"] = "passed"
            checks["repeated_bootstrap_preserves_task_modified_administrator_and_empty_migration_history"] = "passed"
            # Validate the sequence actually yields an unused ID, then roll back the test row.
            sql(f"BEGIN; INSERT INTO {SCHEMA}.sys_config(config_name,config_key,config_value) VALUES ('acceptance','nexiot.seed.sequence.acceptance','ok'); ROLLBACK;")
            checks["insert_with_generated_sequence_key"] = "passed"
            original_digest = sql("SELECT content_sha256 FROM public.nexiot_bootstrap_history WHERE bootstrap_key='clean-seed-v1';")
            sql("UPDATE public.nexiot_bootstrap_history SET content_sha256='changed-for-negative-test';")
            mismatch = execute(["sh", "/opt/nexiot-database/postgres/apply.sh"], check=False)
            assert mismatch.returncode != 0 and "Seed version changed" in mismatch.stderr
            assert sql(task_query) == "FAILED:6000:true"
            assert sql(user_query) == user_before
            sql("UPDATE public.nexiot_bootstrap_history SET content_sha256='" + original_digest + "';")
            checks["changed_seed_refuses_replay"] = "passed"
            sql("CREATE DATABASE nexiot_untracked;")
            sql(f"CREATE SCHEMA {SCHEMA};", database="nexiot_untracked")
            protected = execute(["sh", "/opt/nexiot-database/postgres/apply.sh"], database="nexiot_untracked", check=False)
            assert protected.returncode != 0 and "Existing untracked" in protected.stderr
            assert sql("SELECT to_regclass('public.nexiot_bootstrap_history') IS NULL;", database="nexiot_untracked") == "t"
            checks["untracked_schema_refused_and_transaction_rolled_back"] = "passed"
            sql("CREATE DATABASE nexiot_atomicity;")
            failing = (HERE / "postgres/001-schema.sql").read_text() + "\nSELECT nex_iot_deliberately_missing_function();\n"
            rollback = execute(["psql", "-X", "-q", "-1", "-v", "ON_ERROR_STOP=1"], database="nexiot_atomicity", input=failing, check=False)
            assert rollback.returncode != 0
            assert sql(f"SELECT count(*) FROM pg_namespace WHERE nspname='{SCHEMA}';", database="nexiot_atomicity") == "0"
            checks["failed_import_rolls_back_entire_schema"] = "passed"
            report["seed_sha256"] = hashlib.sha256((HERE / "postgres/SHA256SUMS").read_bytes()).hexdigest()
            report["status"] = "passed"
        except Exception as error:
            report["status"] = "failed"
            report["error"] = str(error)
            raise
        finally:
            if created:
                run(["docker", "rm", "-f", "-v", name])
            report["elapsed_seconds"] = round(time.monotonic() - start, 2)
            report["finished_at"] = datetime.now(timezone.utc).isoformat()
            args.report.parent.mkdir(parents=True, exist_ok=True)
            args.report.write_text(json.dumps(report, ensure_ascii=False, indent=2) + "\n")
    print(json.dumps(report, ensure_ascii=False, indent=2))


if __name__ == "__main__":
    main()
