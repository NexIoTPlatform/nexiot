#!/usr/bin/env python3
"""Export a read-only, consistent NexIoT schema and explicitly allowed core seeds.

Requires PostgreSQL 18 client tools in PATH. Connection parameters are read from
Spring's PostgreSQL profile or its PG_DATASOURCE_* environment overrides.
The source database is never changed. No general table-data dump is performed.
"""
from __future__ import annotations
import argparse
import hashlib
import json
import os
from pathlib import Path
import re
import subprocess
import tempfile
import urllib.parse
import uuid
from open_source_policy import (OSS_ADMIN_PASSWORD_HASH, OSS_ADMIN_USERNAME,
                                OSS_MENU_MIGRATION, OSS_MENU_ROOT_IDS,
                                OSS_MENU_PERMISSION_PREFIXES, retired_menu_predicate,
                                retired_menu_query, retained_menu_filter)

ROOT = Path(__file__).resolve().parents[2]
SCHEMA = "unix_tenant_202605"
# An omitted table is always empty, including future tables discovered in source.
SEEDS = {
    "010-dictionaries.sql": {
        "sys_dict_type": "TRUE",
        "sys_dict_data": "dict_type <> 'whitelist'",
        "sys_menu": retained_menu_filter(),
        "iot_node_red_flow_template_category": "TRUE",
    },
    "020-platform-admin.sql": {
        "iot_user": "id = 1 AND tenant_role = 'PLATFORM_ADMIN' AND deleted = 0",
        "sys_role": "role_id = 1 AND role_key = 'admin'",
        "sys_role_menu": "role_id = 1 AND menu_id IN (SELECT menu_id FROM sys_menu WHERE " + retained_menu_filter() + ")",
        "sys_user_role": "role_id = 1 AND union_id IN (SELECT union_id FROM iot_user WHERE id = 1 AND tenant_role = 'PLATFORM_ADMIN' AND deleted = 0)",
        "iot_tenant_package": "package_id = 900000000000000001 AND deleted = 0",
        "iot_tenant_package_menu": "package_id = 900000000000000001 AND menu_id IN (SELECT menu_id FROM sys_menu WHERE " + retained_menu_filter() + ")",
    },
    "030-platform-config.sql": {"sys_config": "config_key IN (" + ",".join("'"+k+"'" for k in (
        "sys.site.name", "sys.site.logo", "sys.site.icon", "sys.site.description",
        "sys.site.copyright", "sys.site.icp", "sys.watermark.enabled", "sys.watermark.text",
        "sys.layout.mode", "sys.layout.color", "sys.login.welcome", "sys.login.intro",
        "sys.login.title", "sys.login.slogan", "sys.login.subtitle", "sys.websocket.push.enabled",
        "sys.oss.cloudStorageService", "videoai.vlm.compatibility", "videoai.vlm.modality",
        "videoai.vlm.baseUrl", "videoai.vlm.apiKey", "videoai.vlm.model", "videoai.vlm.timeoutSec",
        "videoai.vlm.maxTokens", "videoai.vlm.temperature", "videoai.vlm.prompt")) + ")"},
    "040-system-components.sql": {
        "iot_viz_widget_bundle": "scope_type = 'SYSTEM' AND coalesce(tenant_id, '') = ''",
        "iot_viz_widget_type": "scope_type = 'SYSTEM' AND coalesce(tenant_id, '') = '' AND bundle_id IN (SELECT id FROM iot_viz_widget_bundle WHERE scope_type = 'SYSTEM' AND coalesce(tenant_id, '') = '')",
        "mp_component_library": "TRUE",
        "mp_component_type_mapping": "component_key IN (SELECT component_key FROM mp_component_library)",
    },
}
VIDEO_RECEIPT_MIGRATION = 'V2026101302__video_sdk_alarm_receipt.sql'
VIDEO_PERMISSION_MIGRATION = 'V2026101303__video_operation_permission_families.sql'
VIDEO_READ_PERMISSION_MIGRATION = 'V2026101401__video_read_permission.sql'
INDUSTRIAL_CONSOLE_MIGRATION = 'V2026101403__retire_industrial_console.sql'
INDUSTRIAL_PROBE_MIGRATION = 'V2026101801__industrial_endpoint_probe_permission.sql'
RETIRED_INDUSTRIAL_MENU_IDS = tuple(2062600000000000000 + suffix
                                   for suffix in (1, 11, 13, 14, 15, 16, 21, 22, 23, 24))
VIDEO_PROCESS_MIGRATION = 'V2026101304__video_alarm_processes.sql'
UNIVIEW_SDK_MIGRATION = 'V2026102801__enable_uniview_sdk_video.sql'
UNIVIEW_NATIVE_STREAM_MIGRATION = 'V2026102802__uniview_native_stream_default.sql'
UNIVIEW_SMART_ALARM_MIGRATION = 'V2026102803__uniview_smart_alarm_dictionary.sql'
UNIVIEW_FIELDS = ('endpoint','token','defaultUsername','defaultPassword','sdkPortDefault',
                  'rtspPortDefault','streamMode','zlmMediaServerId')
UNIVIEW_SMART_EVENTS = {
    '0x000003EB': ('line_crossing', 'lineCrossing', 'NETDEV_ALARM_LINE_CROSS (1003)'),
    '0x000003EC': ('intrusion', 'regionIntrusion', 'NETDEV_ALARM_OBJECTS_INSIDE (1004)'),
    '0x000003F7': ('region_enter', 'regionEntrance', 'NETDEV_ALARM_ENTER_AREA (1015)'),
    '0x000003F8': ('region_exit', 'regionExit', 'NETDEV_ALARM_LEAVE_AREA (1016)'),
}
UNIVIEW_EVENT_KEYS = tuple(f'0x{i:08X}' for i in range(1, 9)) + tuple(UNIVIEW_SMART_EVENTS)
WVP_EVENT_MIGRATION = 'V2026101601__wvp_gb28181_event_dictionary.sql'
OTA_TRANSFER_MIGRATION = 'V2026101602__ota_transfer_state.sql'
HISTORY_HYPERTABLE_MIGRATION = 'V2026101901__history_hypertables.sql'
WECHAT_LOGIN_MIGRATION = 'V2026110301__wechat_scan_login.sql'
WECHAT_REPLY_MIGRATION = 'V2026110302__wechat_message_reply_dict.sql'
WECHAT_REPLY_KEYS = ('default', 'subscribe', 'scan', 'event', 'login_success', 'code_invalid',
                     'session_expired', 'account_mismatch', 'session_processed', 'config_changed',
                     'binding_disabled', 'binding_pending', 'unbound', 'account_unavailable')
WVP_EVENT_KEYS = ('1', '2', '3', '4', '7') + tuple(f'2:{i}' for i in range(1, 6)) + tuple(
    f'5:{i}' for i in range(1, 14)) + ('6:1', '6:2')
VIDEO_OPERATION_PERMISSIONS = tuple('video:device:' + action for action in ('play','control','config','maintain'))
POINT_TEMPLATE_MIGRATION = 'V2026101301__industrial_point_templates.sql'
POINT_TEMPLATE_COMPLETION_MIGRATION = 'V2026101402__complete_industrial_point_templates.sql'
POINT_TEMPLATE_COLUMNS = {'template_type': 'character varying(16)', 'protocol': 'character varying(32)',
                          'status': 'character varying(16)', 'draft_content': 'jsonb', 'draft_revision': 'bigint'}
POINT_TEMPLATE_PERMISSIONS = tuple('industrial:point-template:' + action
                                   for action in ('query', 'add', 'edit', 'remove', 'publish', 'apply'))


CONFIG_OVERRIDES = {
    "sys.site.name": "NexIoT", "sys.site.logo": "", "sys.site.icon": "",
    "sys.site.copyright": "Copyright © NexIoT", "sys.site.icp": "",
    "sys.watermark.enabled": "false", "sys.watermark.text": "",
    "sys.login.title": "NexIoT", "sys.login.subtitle": "",
    "sys.oss.cloudStorageService": "minio", "videoai.vlm.baseUrl": "",
    "videoai.vlm.apiKey": "", "videoai.vlm.prompt": "",
}


def resolve(value):
    for _ in range(20):
        if "${" not in value:
            return value
        replacement = re.sub(r"\$\{([^{}:]+):([^{}]*)\}", lambda m: os.environ.get(m[1], m[2]), value)
        if replacement == value:
            raise ValueError("Unresolved Spring property placeholder")
        value = replacement
    raise ValueError("Excessively nested Spring property")


def connection(profile):
    props = dict(line.split("=", 1) for line in profile.read_text().splitlines()
                 if line and not line.startswith("#") and "=" in line)
    url = urllib.parse.urlparse(resolve(props["spring.datasource.url"]).removeprefix("jdbc:"))
    configured_schema = urllib.parse.parse_qs(url.query).get("currentSchema", [SCHEMA])[0].split(",")[0]
    if configured_schema != SCHEMA:
        raise ValueError(f"Expected audited source schema {SCHEMA}, got {configured_schema}")
    return os.environ | {
        "PGHOST": url.hostname, "PGPORT": str(url.port or 5432), "PGDATABASE": url.path[1:],
        "PGUSER": resolve(props["spring.datasource.username"]),
        "PGPASSWORD": resolve(props["spring.datasource.password"]),
        "PGOPTIONS": f"-c default_transaction_read_only=on -c search_path={SCHEMA},public",
        "PGCONNECT_TIMEOUT": "15", "PGCLIENTENCODING": "UTF8",
    }


class Snapshot:
    def __init__(self, env):
        self.errors = tempfile.TemporaryFile(mode="w+")
        self.proc = subprocess.Popen(["psql", "-X", "-qAt", "-v", "ON_ERROR_STOP=1"],
                                     env=env, stdin=subprocess.PIPE, stdout=subprocess.PIPE,
                                     stderr=self.errors, text=True, bufsize=1)
        self.query("BEGIN ISOLATION LEVEL REPEATABLE READ READ ONLY;")
        self.snapshot = self.query("SELECT pg_export_snapshot();")[0]

    def query(self, sql):
        marker = "nexiot_export_" + uuid.uuid4().hex
        self.proc.stdin.write(sql + "\n\\echo " + marker + "\n")
        self.proc.stdin.flush()
        lines = []
        for line in self.proc.stdout:
            if line.rstrip("\n") == marker:
                return lines
            lines.append(line.rstrip("\n"))
        raise RuntimeError("Read-only source query failed; no source mutations were attempted")

    def close(self):
        if self.proc.poll() is None:
            self.proc.stdin.write("ROLLBACK;\n\\q\n")
            self.proc.stdin.flush()
        self.proc.communicate(timeout=20)
        self.errors.close()


def identifier(s):
    return '"' + s.replace('"', '""') + '"'


def literal(value):
    if value is None:
        return "NULL"
    if isinstance(value, bool):
        return "TRUE" if value else "FALSE"
    if isinstance(value, (int, float)):
        return str(value)
    if isinstance(value, (dict, list)):
        value = json.dumps(value, ensure_ascii=False, separators=(",", ":"))
    return "'" + str(value).replace("'", "''") + "'"


def sanitize(table, row):
    for key in ("create_by", "update_by", "creator_id"):
        if key in row and row[key] is not None:
            row[key] = OSS_ADMIN_USERNAME
    if table == "iot_user":
        for key in ("email", "mobile", "org_id", "avatar", "parent_union_id", "login_ip",
                    "login_date", "update_by", "update_date", "cfg", "remark", "dept_id"):
            row[key] = None
        row["alias"] = "NexIoT"
        row["username"] = OSS_ADMIN_USERNAME
        row["union_id"] = OSS_ADMIN_USERNAME
        # Never publish a source environment's password hash, salt or audit dates.
        row["password"] = OSS_ADMIN_PASSWORD_HASH
        row["salt"] = None
        row["create_date"] = "2026-11-07T00:00:00"
    if table == "sys_user_role":
        row["union_id"] = OSS_ADMIN_USERNAME
    if table == "iot_tenant_package":
        row["remark"] = "NexIoT 默认租户功能套餐（不含任何租户）"
    if table == "sys_config":
        row["config_value"] = CONFIG_OVERRIDES.get(row["config_key"], row["config_value"])
    if table == "sys_dict_data" and row.get("remark"):
        # Configuration-field hints are retained, with environment-specific examples removed.
        row["remark"] = row["remark"].replace("192.168.31.118", "127.0.0.1")
    return row


def verify_point_template_source(source):
    """Do not let re-exporting an older source erase reviewed point-template structure/seeds."""
    columns = json.loads(source.query(f"SELECT json_object_agg(a.attname, format_type(a.atttypid,a.atttypmod)) FROM pg_attribute a WHERE a.attrelid='{SCHEMA}.iot_industrial_config_template'::regclass AND a.attnum>0 AND NOT a.attisdropped;")[0])
    if any(columns.get(name) != type_name for name, type_name in POINT_TEMPLATE_COLUMNS.items()):
        raise ValueError(f"Source point-template columns are not current; apply {POINT_TEMPLATE_MIGRATION} before re-exporting")
    origin_columns = source.query(f"SELECT c.relname || ':' || a.attname || ':' || format_type(a.atttypid,a.atttypmod) FROM pg_attribute a JOIN pg_class c ON c.oid=a.attrelid JOIN pg_namespace n ON n.oid=c.relnamespace WHERE n.nspname='{SCHEMA}' AND c.relname IN ('iot_industrial_point','iot_industrial_collect_group') AND a.attname IN ('source_template_id','source_template_version') AND NOT a.attisdropped AND NOT a.attnotnull;")
    expected_origin = {table + ':' + name + ':' + kind for table in ('iot_industrial_point','iot_industrial_collect_group')
                       for name, kind in (('source_template_id','bigint'),('source_template_version','integer'))}
    if set(origin_columns) != expected_origin:
        raise ValueError(f"Source point-template provenance columns are not current; apply {POINT_TEMPLATE_MIGRATION}")
    names = source.query(f"SELECT conname FROM pg_constraint WHERE conrelid='{SCHEMA}.iot_industrial_config_template'::regclass AND convalidated AND conname LIKE 'ck_iot_ind_config_template_%';")
    expected = {'ck_iot_ind_config_template_type', 'ck_iot_ind_config_template_status',
                'ck_iot_ind_config_template_binding', 'ck_iot_ind_config_template_revision'}
    if not expected.issubset(names):
        raise ValueError(f"Source point-template constraints are not current; apply {POINT_TEMPLATE_MIGRATION}")
    index = source.query(f"SELECT count(*) FROM pg_index WHERE indexrelid=to_regclass('{SCHEMA}.idx_iot_ind_config_template_point_filter') AND indisvalid AND NOT indisunique;")[0]
    if index != '1':
        raise ValueError(f"Source point-template filter index is missing; apply {POINT_TEMPLATE_MIGRATION}")


def verify_wechat_login_source(source):
    """Keep future exports from silently removing reviewed authentication structure."""
    expected_columns = {
        'iot_wechat_login_config': {'tenant_id', 'account_name', 'app_id', 'app_secret',
            'callback_token', 'encoding_aes_key', 'account_qr_url', 'capability_mode',
            'login_mode', 'enabled', 'binding_enabled', 'demo_user_union_id',
            'expire_seconds', 'config_version', 'create_time', 'update_time'},
        'iot_user_wechat_binding': {'id', 'tenant_id', 'app_id', 'openid',
            'user_union_id', 'create_time', 'update_time'},
    }
    for table, expected in expected_columns.items():
        actual = source.query(f"SELECT column_name FROM information_schema.columns WHERE table_schema='{SCHEMA}' AND table_name='{table}';")
        if set(actual) != expected:
            raise ValueError(f"Source WeChat login structure is not current; apply {WECHAT_LOGIN_MIGRATION}")
    constraints = source.query(f"SELECT conname FROM pg_constraint c JOIN pg_namespace n ON n.oid=c.connamespace WHERE n.nspname='{SCHEMA}' AND convalidated AND conname IN ('iot_wechat_login_config_pkey','ck_wechat_login_capability','ck_wechat_login_mode','ck_wechat_login_expiry','ck_wechat_login_version','iot_user_wechat_binding_pkey','uk_wechat_binding_identity','uk_wechat_binding_account');")
    if len(constraints) != 8:
        raise ValueError(f"Source WeChat login constraints are missing; apply {WECHAT_LOGIN_MIGRATION}")
    index = source.query(f"SELECT count(*) FROM pg_index WHERE indexrelid=to_regclass('{SCHEMA}.idx_wechat_binding_unsubscribe') AND indisvalid AND NOT indisunique;")[0]
    if index != '1':
        raise ValueError(f"Source WeChat unsubscribe lookup is missing; apply {WECHAT_LOGIN_MIGRATION}")


def verify_wechat_reply_source(source):
    """Retain the reply dictionaries when rebuilding a new-installation seed."""
    types = source.query(f"SELECT dict_type FROM {SCHEMA}.sys_dict_type WHERE dict_type IN ('wechat_reply','wechat_keyword_reply');")
    keys = source.query(f"SELECT dict_value FROM {SCHEMA}.sys_dict_data WHERE dict_type='wechat_reply';")
    if len(types) != 2 or not set(WECHAT_REPLY_KEYS).issubset(keys):
        raise ValueError(f"Source WeChat reply dictionaries are incomplete; apply {WECHAT_REPLY_MIGRATION}")


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--profile", type=Path, default=ROOT / "cn-universal-web/src/main/resources/application-postgresql.properties")
    parser.add_argument("--output", type=Path, default=Path(__file__).parent / "postgres")
    args = parser.parse_args()
    env = connection(args.profile)
    source = Snapshot(env)
    generated = {}
    try:
        version = source.query("SHOW server_version;")[0]
        if int(version.split(".")[0]) != 18:
            raise ValueError("This snapshot targets PostgreSQL 18; audit compatibility before changing major version")
        verify_point_template_source(source)
        verify_wechat_login_source(source)
        verify_wechat_reply_source(source)
        ota_transfer = source.query(f"SELECT count(*) FROM information_schema.columns WHERE table_schema='{SCHEMA}' AND table_name='iot_ota_task_device' AND column_name='transfer_state' AND data_type='text' AND is_nullable='YES' AND column_default IS NULL;")[0]
        if ota_transfer != '1':
            raise ValueError(f"Source is missing compatible OTA transfer state; apply {OTA_TRANSFER_MIGRATION}")
        receipt = source.query(f"SELECT to_regclass('{SCHEMA}.video_sdk_alarm_receipt') IS NOT NULL;")[0]
        if receipt != 't':
            raise ValueError(f"Source is missing durable video alarm receipts; apply {VIDEO_RECEIPT_MIGRATION}")
        process_tables = source.query(f"SELECT count(*) FROM pg_tables WHERE schemaname='{SCHEMA}' AND tablename IN ('video_sdk_alarm_process','video_sdk_alarm_process_member');")[0]
        if process_tables != '2':
            raise ValueError(f"Source is missing video alarm process projection; apply {VIDEO_PROCESS_MIGRATION}")
        wvp_type = source.query(f"SELECT count(*) FROM {SCHEMA}.sys_dict_type WHERE dict_type='video_sdk_event_wvp';")[0]
        wvp_keys = source.query(f"SELECT dict_value FROM {SCHEMA}.sys_dict_data WHERE dict_type='video_sdk_event_wvp' AND dict_value IN ({','.join(literal(key) for key in WVP_EVENT_KEYS)});")
        if wvp_type != '1' or len(wvp_keys) != len(WVP_EVENT_KEYS) or set(wvp_keys) != set(WVP_EVENT_KEYS):
            raise ValueError(f"Source GB28181 event dictionary is missing/incomplete; apply {WVP_EVENT_MIGRATION}")
        uniview_types = source.query(f"SELECT dict_type FROM {SCHEMA}.sys_dict_type WHERE dict_type IN ('video_uniview_sdk_fields','video_sdk_event_uniview_sdk');")
        uniview_platform = source.query(f"SELECT count(*) FROM {SCHEMA}.sys_dict_data WHERE dict_type='video_platform_type' AND dict_value='uniview_sdk';")[0]
        uniview_fields = source.query(f"SELECT dict_value FROM {SCHEMA}.sys_dict_data WHERE dict_type='video_uniview_sdk_fields' AND dict_value IN ({','.join(literal(key) for key in UNIVIEW_FIELDS)});")
        uniview_events = source.query(f"SELECT dict_value FROM {SCHEMA}.sys_dict_data WHERE dict_type='video_sdk_event_uniview_sdk' AND dict_value IN ({','.join(literal(key) for key in UNIVIEW_EVENT_KEYS)});")
        if len(uniview_types) != 2 or uniview_platform != '1' or sorted(uniview_fields) != sorted(UNIVIEW_FIELDS) or sorted(uniview_events) != sorted(UNIVIEW_EVENT_KEYS):
            raise ValueError(f"Source Uniview SDK dictionaries are missing/incomplete; apply {UNIVIEW_SDK_MIGRATION} and {UNIVIEW_SMART_ALARM_MIGRATION}")
        uniview_native_default = source.query(f"SELECT count(*) FROM {SCHEMA}.sys_dict_data WHERE dict_type='video_uniview_sdk_fields' AND dict_value='streamMode' AND remark::jsonb->>'placeholder'='默认 sdk_only';")[0]
        if uniview_native_default != '1':
            raise ValueError(f"Source Uniview native stream default is missing; apply {UNIVIEW_NATIVE_STREAM_MIGRATION}")
        for key, (topic, event_id, sdk_code) in UNIVIEW_SMART_EVENTS.items():
            matches = source.query(f"SELECT count(*) FROM {SCHEMA}.sys_dict_data WHERE dict_type='video_sdk_event_uniview_sdk' AND dict_value={literal(key)} AND remark::jsonb->>'type'={literal(topic)} AND remark::jsonb->>'eventId'={literal(event_id)} AND remark::jsonb->>'sdkCode'={literal(sdk_code)};")[0]
            if matches != '1':
                raise ValueError(f"Source Uniview smart alarm semantics are incompatible; apply {UNIVIEW_SMART_ALARM_MIGRATION}: {key}")
        tables = source.query(f"SELECT tablename FROM pg_tables WHERE schemaname='{SCHEMA}' ORDER BY tablename;")
        extensions = source.query("SELECT extname || ':' || extversion FROM pg_extension ORDER BY extname;")
        objects = json.loads(source.query(f"SELECT json_build_object('tables',count(*) FILTER(WHERE relkind IN('r','p')),'partitioned_tables',count(*) FILTER(WHERE relkind='p'),'sequences',count(*) FILTER(WHERE relkind='S'),'indexes',count(*) FILTER(WHERE relkind IN('i','I'))) FROM pg_class c JOIN pg_namespace n ON n.oid=c.relnamespace WHERE n.nspname='{SCHEMA}';")[0])
        for section, name in (("pre-data", "001-schema.sql"), ("post-data", "090-constraints-indexes.sql")):
            result = subprocess.run(["pg_dump", "--schema-only", "--schema", SCHEMA,
                                     "--section", section, "--no-owner", "--no-privileges",
                                     "--no-security-labels", "--snapshot", source.snapshot],
                                    env=env, text=True, capture_output=True, check=True)
            text = re.sub(r"^\\(?:un)?restrict .*\n", "", result.stdout, flags=re.M)
            text = re.sub(r"^-- Dumped (?:from database|by pg_dump) version .*\n", "", text, flags=re.M)
            if section == "post-data":
                # Recreate empty hypertables through 097, never copy Timescale's
                # internal insert blocker into an ordinary PostgreSQL bootstrap.
                text = re.sub(r"^CREATE TRIGGER ts_insert_blocker .*;\n", "", text, flags=re.M)
                # Bootstrap includes reviewed schema fixes even before the source DB is upgraded.
                text = text.replace("-- Name: uk_instance_channel; Type: INDEX;", "-- Name: idx_instance_channel; Type: INDEX;")
                text = re.sub(
                    rf"CREATE UNIQUE INDEX uk_instance_channel ON {re.escape(SCHEMA)}\.video_platform_channel USING btree \(instance_key, channel_id(?:, parent_id)?\);",
                    f"CREATE INDEX idx_instance_channel ON {SCHEMA}.video_platform_channel USING btree (instance_key, channel_id, parent_id);",
                    text,
                )
            generated[name] = "-- NexIoT PostgreSQL 18; schema only; no source business rows.\n" + text
        counts, filters = {}, {}
        for filename, policies in SEEDS.items():
            statements = ["-- Explicit core seed allowlist. All other tables remain empty.",
                          "SET standard_conforming_strings = on;"]
            for table, predicate in policies.items():
                cols = source.query(f"SELECT column_name FROM information_schema.columns WHERE table_schema='{SCHEMA}' AND table_name='{table}' ORDER BY ordinal_position;")
                raw = source.query(f"SELECT row_to_json(seed) FROM (SELECT * FROM {identifier(SCHEMA)}.{identifier(table)} WHERE {predicate} ORDER BY 1) seed;")
                rows = [sanitize(table, json.loads(item)) for item in raw]
                counts[table] = len(rows)
                filters[table] = predicate
                statements.append(f"\n-- {table}: {len(rows)} rows; source filter: {predicate}")
                for row in rows:
                    statements.append(f"INSERT INTO {identifier(SCHEMA)}.{identifier(table)} ({', '.join(map(identifier, cols))}) VALUES ({', '.join(literal(row[c]) for c in cols)});")
            generated[filename] = "\n".join(statements) + "\n"
        for table in ("iot_user", "sys_role", "sys_user_role", "iot_tenant_package"):
            if counts[table] != 1:
                raise ValueError(f"Required singleton core seed {table} is missing or ambiguous")
        # Fail closed on environment identifiers or credential material in dictionaries/components.
        for filename, content in generated.items():
            if re.search(r"(?i)\bbony\b|192\.168\.31\.|125\.210\.49\.121|qiniu\.oss\.filecoin|outlookfil", content):
                raise ValueError(f"Environment-specific content remains in {filename}; audit it before exporting")
        manifest = {
            "format": 1, "source_server_version": version, "source_schema": SCHEMA,
            "reviewed_schema_changes": [POINT_TEMPLATE_MIGRATION, VIDEO_RECEIPT_MIGRATION, VIDEO_PROCESS_MIGRATION,
                                        POINT_TEMPLATE_COMPLETION_MIGRATION, OTA_TRANSFER_MIGRATION,
                                        HISTORY_HYPERTABLE_MIGRATION, WECHAT_LOGIN_MIGRATION],
            "reviewed_data_changes": [VIDEO_PERMISSION_MIGRATION, VIDEO_READ_PERMISSION_MIGRATION, INDUSTRIAL_CONSOLE_MIGRATION,
                                      WVP_EVENT_MIGRATION, INDUSTRIAL_PROBE_MIGRATION, UNIVIEW_SDK_MIGRATION, UNIVIEW_NATIVE_STREAM_MIGRATION, UNIVIEW_SMART_ALARM_MIGRATION, WECHAT_REPLY_MIGRATION, OSS_MENU_MIGRATION],
            "installed_migration_version": "2026110701",
            "open_source_scope": {
                "retired_menu_roots": list(OSS_MENU_ROOT_IDS),
                "retired_permission_prefixes": list(OSS_MENU_PERMISSION_PREFIXES),
                "removed_menu_rows": int(source.query(
                    f"SELECT count(*) FROM {SCHEMA}.sys_menu WHERE menu_id IN ({retired_menu_query(SCHEMA)});")[0]),
            },
            "source_extensions": extensions, "source_objects": objects,
            "source_transaction": "REPEATABLE READ READ ONLY; shared exported snapshot",
            "seed_counts": counts, "source_row_filters": filters,
            "empty_tables": [t for t in tables if not counts.get(t, 0)],
            "default_denied_tables": [t for t in tables if t not in counts],
            "policy": "Default deny. Keep core dictionaries, one public bootstrap administrator (independent installation password hash), retained module permissions, one default tenant package, sanitized site settings and SYSTEM component definitions. No tenants, devices, products, history, tokens or source environment credentials.",
        }
        generated["095-reset-sequences.sql"] = sequence_sql()
        generated["097-history-hypertables.sql"] = history_hypertable_sql()
        generated["099-verify-clean-seed.sql"] = verification_sql(tables, counts, objects)
        manifest["files"] = {k: hashlib.sha256(v.encode()).hexdigest() for k, v in sorted(generated.items())}
        args.output.mkdir(parents=True, exist_ok=True)
        for name, content in generated.items():
            (args.output / name).write_text(content)
        (args.output / "manifest.json").write_text(json.dumps(manifest, ensure_ascii=False, indent=2) + "\n")
        (args.output / "SHA256SUMS").write_text("".join(f"{digest}  {name}\n" for name, digest in manifest["files"].items()))
        print(json.dumps({"source_version": version, "objects": objects, "seed_counts": counts,
                          "empty_table_count": len(manifest["empty_tables"])}, ensure_ascii=False, indent=2))
    finally:
        source.close()


def sequence_sql():
    return f'''-- Derive sequences from retained rows, never from source business counters.
DO $nexiot$
DECLARE s record; maximum bigint;
BEGIN
  FOR s IN
    SELECT seq.oid AS sequence_oid, sn.nspname AS sequence_schema, seq.relname AS sequence_name,
           tn.nspname AS table_schema, t.relname AS table_name, a.attname AS column_name,
           ps.seqstart AS start_value
    FROM pg_class seq JOIN pg_namespace sn ON sn.oid=seq.relnamespace
    JOIN pg_sequence ps ON ps.seqrelid=seq.oid
    JOIN pg_depend d ON d.objid=seq.oid AND d.classid='pg_class'::regclass AND d.deptype IN ('a','i')
    JOIN pg_class t ON t.oid=d.refobjid JOIN pg_namespace tn ON tn.oid=t.relnamespace
    JOIN pg_attribute a ON a.attrelid=t.oid AND a.attnum=d.refobjsubid
    WHERE seq.relkind='S' AND sn.nspname='{SCHEMA}'
  LOOP
    EXECUTE format('SELECT max(%I) FROM %I.%I', s.column_name, s.table_schema, s.table_name) INTO maximum;
    PERFORM pg_catalog.setval(s.sequence_oid::regclass, greatest(coalesce(maximum,s.start_value),s.start_value), maximum IS NOT NULL);
  END LOOP;
END $nexiot$;
'''


def history_hypertable_sql():
    migration = (ROOT / 'cn-universal-db-migrator/src/main/resources/db/migration/postgresql'
                 / HISTORY_HYPERTABLE_MIGRATION).read_text()
    return ("-- New installation: use TimescaleDB when it is available in the database image.\n"
            "DO $$ BEGIN\n"
            "  IF EXISTS (SELECT 1 FROM pg_available_extensions WHERE name='timescaledb') THEN\n"
            "    CREATE EXTENSION IF NOT EXISTS timescaledb WITH SCHEMA public;\n"
            "  END IF;\nEND $$;\n"
            f"SET LOCAL search_path TO {SCHEMA},public;\n" + migration)


def verification_sql(tables, counts, objects):
    expected = json.dumps({t: counts.get(t, 0) for t in tables}, separators=(",", ":"))
    return f'''-- Run only during initial bootstrap; deliberately rejects any business rows.
DO $nexiot$
DECLARE item record; actual bigint; actual_objects bigint; maximum bigint; sequence_last bigint; sequence_called boolean; s record;
BEGIN
  IF EXISTS (SELECT 1 FROM pg_extension WHERE extname='timescaledb') THEN
    EXECUTE $query$SELECT count(*) FROM timescaledb_information.hypertables WHERE hypertable_schema='{SCHEMA}'
      AND hypertable_name ~ '^ts_iot_device_log(_metadata)?(_[0-9]+)?$'$query$ INTO actual_objects;
    SELECT count(*) INTO actual FROM pg_class c JOIN pg_namespace n ON n.oid=c.relnamespace
      WHERE n.nspname='{SCHEMA}' AND c.relkind='r'
      AND c.relname ~ '^ts_iot_device_log(_metadata)?(_[0-9]+)?$';
    IF actual_objects <> actual THEN RAISE EXCEPTION 'History hypertable count mismatch: % / %',actual_objects,actual; END IF;
  END IF;
  FOR item IN SELECT key AS table_name,value::bigint AS expected FROM jsonb_each_text('{expected}'::jsonb)
  LOOP
    EXECUTE format('SELECT count(*) FROM %I.%I','{SCHEMA}',item.table_name) INTO actual;
    IF actual <> item.expected THEN
      RAISE EXCEPTION 'Clean seed mismatch %.%: expected %, got %','{SCHEMA}',item.table_name,item.expected,actual;
    END IF;
  END LOOP;
  SELECT count(*) INTO actual_objects FROM pg_class c JOIN pg_namespace n ON n.oid=c.relnamespace
    WHERE n.nspname='{SCHEMA}' AND c.relkind IN ('r','p');
  IF actual_objects <> {objects['tables']} THEN RAISE EXCEPTION 'Schema table count mismatch: %',actual_objects; END IF;
  SELECT count(*) INTO actual_objects FROM pg_class c JOIN pg_namespace n ON n.oid=c.relnamespace
    WHERE n.nspname='{SCHEMA}' AND c.relkind='p';
  IF actual_objects <> {objects['partitioned_tables']} THEN RAISE EXCEPTION 'Partitioned-table count mismatch: %',actual_objects; END IF;
  SELECT count(*) INTO actual_objects FROM pg_class c JOIN pg_namespace n ON n.oid=c.relnamespace
    WHERE n.nspname='{SCHEMA}' AND c.relkind='S';
  IF actual_objects <> {objects['sequences']} THEN RAISE EXCEPTION 'Sequence count mismatch: %',actual_objects; END IF;
  SELECT count(*) INTO actual_objects FROM pg_class c JOIN pg_namespace n ON n.oid=c.relnamespace
    WHERE n.nspname='{SCHEMA}' AND c.relkind IN ('i','I');
  IF actual_objects <> {objects['indexes']} THEN RAISE EXCEPTION 'Index count mismatch: %',actual_objects; END IF;
  IF EXISTS (SELECT 1 FROM pg_constraint c JOIN pg_namespace n ON n.oid=c.connamespace WHERE n.nspname='{SCHEMA}' AND NOT c.convalidated)
    THEN RAISE EXCEPTION 'Unvalidated constraints remain'; END IF;
  IF EXISTS (SELECT 1 FROM pg_index i JOIN pg_class c ON c.oid=i.indexrelid JOIN pg_namespace n ON n.oid=c.relnamespace WHERE n.nspname='{SCHEMA}' AND NOT i.indisvalid)
    THEN RAISE EXCEPTION 'Invalid indexes remain'; END IF;
  IF (SELECT count(*) FROM pg_constraint c JOIN pg_namespace n ON n.oid=c.connamespace
      WHERE n.nspname='{SCHEMA}' AND c.convalidated AND c.conname IN
      ('iot_wechat_login_config_pkey','ck_wechat_login_capability','ck_wechat_login_mode',
       'ck_wechat_login_expiry','ck_wechat_login_version','iot_user_wechat_binding_pkey',
       'uk_wechat_binding_identity','uk_wechat_binding_account')) <> 8
    THEN RAISE EXCEPTION 'WeChat login constraints are missing'; END IF;
  IF NOT EXISTS (SELECT 1 FROM pg_index WHERE indexrelid=to_regclass('{SCHEMA}.idx_wechat_binding_unsubscribe') AND indisvalid AND NOT indisunique)
    THEN RAISE EXCEPTION 'WeChat unsubscribe lookup is missing'; END IF;
  IF NOT EXISTS (SELECT 1 FROM pg_index WHERE indexrelid=to_regclass('{SCHEMA}.uk_device_channel') AND indisunique)
    THEN RAISE EXCEPTION 'Video channels require device-scoped uniqueness'; END IF;
  IF to_regclass('{SCHEMA}.uk_instance_channel') IS NOT NULL
    THEN RAISE EXCEPTION 'Instance-wide video channel uniqueness must not be initialized'; END IF;
  IF NOT EXISTS (SELECT 1 FROM pg_index WHERE indexrelid=to_regclass('{SCHEMA}.idx_instance_channel') AND NOT indisunique)
    THEN RAISE EXCEPTION 'Video instance channel lookup requires a non-unique index'; END IF;
  IF (SELECT count(*) FROM information_schema.columns WHERE table_schema='{SCHEMA}'
      AND table_name='iot_ota_task_device' AND column_name='transfer_state'
      AND data_type='text' AND is_nullable='YES' AND column_default IS NULL) <> 1
    THEN RAISE EXCEPTION 'OTA transfer state column is missing/incompatible'; END IF;
  IF (SELECT count(*) FROM information_schema.columns WHERE table_schema='{SCHEMA}'
      AND table_name='iot_industrial_config_template'
      AND column_name IN ('template_type','protocol','status','draft_content','draft_revision')) <> 5
    THEN RAISE EXCEPTION 'Point-template columns are missing'; END IF;
  IF (SELECT count(*) FROM information_schema.columns WHERE table_schema='{SCHEMA}'
      AND table_name IN ('iot_industrial_point','iot_industrial_collect_group')
      AND column_name IN ('source_template_id','source_template_version') AND is_nullable='YES') <> 4
    THEN RAISE EXCEPTION 'Point-template provenance columns are missing'; END IF;
  IF (SELECT count(*) FROM pg_constraint WHERE conrelid='{SCHEMA}.iot_industrial_config_template'::regclass
      AND convalidated AND conname IN ('ck_iot_ind_config_template_type','ck_iot_ind_config_template_status',
      'ck_iot_ind_config_template_binding','ck_iot_ind_config_template_revision')) <> 4
    THEN RAISE EXCEPTION 'Point-template constraints are missing'; END IF;
  IF NOT EXISTS (SELECT 1 FROM pg_index WHERE indexrelid=to_regclass('{SCHEMA}.idx_iot_ind_config_template_point_filter') AND indisvalid AND NOT indisunique)
    THEN RAISE EXCEPTION 'Point-template filter index is missing'; END IF;
  IF EXISTS (SELECT 1 FROM {SCHEMA}.sys_dict_data d LEFT JOIN {SCHEMA}.sys_dict_type t ON t.dict_type=d.dict_type WHERE t.dict_id IS NULL)
    THEN RAISE EXCEPTION 'Orphan dictionary rows'; END IF;
  IF (SELECT count(*) FROM {SCHEMA}.sys_dict_type WHERE dict_type IN ('wechat_reply','wechat_keyword_reply')) <> 2
     OR (SELECT count(DISTINCT dict_value) FROM {SCHEMA}.sys_dict_data WHERE dict_type='wechat_reply'
         AND dict_value IN ({','.join(literal(key) for key in WECHAT_REPLY_KEYS)})) <> {len(WECHAT_REPLY_KEYS)}
    THEN RAISE EXCEPTION 'WeChat reply dictionaries are incomplete'; END IF;
  IF (SELECT count(*) FROM {SCHEMA}.sys_dict_type WHERE dict_type='video_sdk_event_wvp') <> 1
     OR (SELECT count(DISTINCT dict_value) FROM {SCHEMA}.sys_dict_data WHERE dict_type='video_sdk_event_wvp'
         AND dict_value IN ({','.join(literal(key) for key in WVP_EVENT_KEYS)})) <> {len(WVP_EVENT_KEYS)}
    THEN RAISE EXCEPTION 'GB28181 event dictionary is incomplete'; END IF;
  IF (SELECT count(*) FROM {SCHEMA}.sys_dict_type WHERE dict_type IN
        ('video_uniview_sdk_fields','video_sdk_event_uniview_sdk')) <> 2
     OR (SELECT count(*) FROM {SCHEMA}.sys_dict_data WHERE dict_type='video_platform_type'
         AND dict_value='uniview_sdk') <> 1
     OR (SELECT count(DISTINCT dict_value) FROM {SCHEMA}.sys_dict_data WHERE dict_type='video_uniview_sdk_fields'
         AND dict_value IN ('endpoint','token','defaultUsername','defaultPassword','sdkPortDefault',
                            'rtspPortDefault','streamMode','zlmMediaServerId')) <> 8
     OR (SELECT count(*) FROM {SCHEMA}.sys_dict_data WHERE dict_type='video_uniview_sdk_fields'
         AND dict_value='streamMode' AND remark::jsonb->>'placeholder'='默认 sdk_only') <> 1
     OR (SELECT count(DISTINCT dict_value) FROM {SCHEMA}.sys_dict_data WHERE dict_type='video_sdk_event_uniview_sdk'
         AND dict_value IN ({','.join(literal(key) for key in UNIVIEW_EVENT_KEYS)})) <> {len(UNIVIEW_EVENT_KEYS)}
    THEN RAISE EXCEPTION 'Uniview SDK dictionaries are incomplete'; END IF;
  IF EXISTS (SELECT 1 FROM (VALUES
       {','.join('(' + ','.join(literal(part) for part in (key, *values)) + ')' for key, values in UNIVIEW_SMART_EVENTS.items())}
       ) expected(code,topic,event_id,sdk_code)
       WHERE NOT EXISTS (SELECT 1 FROM {SCHEMA}.sys_dict_data actual
         WHERE actual.dict_type='video_sdk_event_uniview_sdk' AND actual.dict_value=expected.code
         AND actual.remark::jsonb->>'type'=expected.topic AND actual.remark::jsonb->>'eventId'=expected.event_id
         AND actual.remark::jsonb->>'sdkCode'=expected.sdk_code))
    THEN RAISE EXCEPTION 'Uniview smart alarm semantics are incomplete'; END IF;
  IF EXISTS (SELECT 1 FROM {SCHEMA}.sys_menu WHERE {retired_menu_predicate()})
    THEN RAISE EXCEPTION 'Menus outside the open-source module scope remain'; END IF;
  IF EXISTS (SELECT 1 FROM {SCHEMA}.sys_menu child LEFT JOIN {SCHEMA}.sys_menu parent
      ON parent.menu_id=child.parent_id WHERE child.parent_id<>0 AND parent.menu_id IS NULL)
    THEN RAISE EXCEPTION 'Orphan menu rows'; END IF;
  IF (SELECT count(*) FROM {SCHEMA}.iot_user WHERE id=1 AND username={literal(OSS_ADMIN_USERNAME)}
      AND union_id={literal(OSS_ADMIN_USERNAME)} AND password={literal(OSS_ADMIN_PASSWORD_HASH)}) <> 1
    THEN RAISE EXCEPTION 'Public bootstrap administrator is missing or incompatible'; END IF;
  IF EXISTS (SELECT 1 FROM {SCHEMA}.iot_tenant_package_menu r LEFT JOIN {SCHEMA}.sys_menu m
      ON m.menu_id=r.menu_id WHERE m.menu_id IS NULL)
    THEN RAISE EXCEPTION 'Orphan tenant-package/menu permissions'; END IF;
  IF EXISTS (SELECT 1 FROM {SCHEMA}.sys_role_menu r LEFT JOIN {SCHEMA}.sys_menu m ON m.menu_id=r.menu_id WHERE m.menu_id IS NULL)
    THEN RAISE EXCEPTION 'Orphan role/menu permissions'; END IF;
  IF EXISTS (SELECT 1 FROM {SCHEMA}.sys_user_role r LEFT JOIN {SCHEMA}.iot_user u ON u.union_id=r.union_id LEFT JOIN {SCHEMA}.sys_role role ON role.role_id=r.role_id WHERE u.id IS NULL OR role.role_id IS NULL)
    THEN RAISE EXCEPTION 'Orphan user/role permissions'; END IF;
  FOR s IN
    SELECT seq.oid AS sequence_oid, sn.nspname AS sequence_schema, seq.relname AS sequence_name,
           tn.nspname AS table_schema, t.relname AS table_name, a.attname AS column_name
    FROM pg_class seq JOIN pg_namespace sn ON sn.oid=seq.relnamespace
    JOIN pg_depend d ON d.objid=seq.oid AND d.classid='pg_class'::regclass AND d.deptype IN ('a','i')
    JOIN pg_class t ON t.oid=d.refobjid JOIN pg_namespace tn ON tn.oid=t.relnamespace
    JOIN pg_attribute a ON a.attrelid=t.oid AND a.attnum=d.refobjsubid
    WHERE seq.relkind='S' AND sn.nspname='{SCHEMA}'
  LOOP
    EXECUTE format('SELECT max(%I) FROM %I.%I',s.column_name,s.table_schema,s.table_name) INTO maximum;
    EXECUTE format('SELECT last_value,is_called FROM %I.%I',s.sequence_schema,s.sequence_name) INTO sequence_last,sequence_called;
    IF maximum IS NOT NULL AND (sequence_last < maximum OR (sequence_last = maximum AND NOT sequence_called))
      THEN RAISE EXCEPTION 'Sequence behind retained rows: %',s.sequence_name; END IF;
  END LOOP;
  RAISE NOTICE 'NexIoT clean seed verified: {objects['tables']} tables, {sum(counts.values())} retained core rows, {sum(1 for t in tables if not counts.get(t, 0))} completely empty tables';
END $nexiot$;
'''


if __name__ == "__main__":
    main()
