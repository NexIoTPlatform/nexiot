#!/usr/bin/env python3
"""Extract callback event dictionaries from the vendor Java SDK (stdlib only).

Usage: python3 generate_dahua_sdk_event_dict.py /path/to/NetSDKLib.java /path/to/NEW.sql
Applied Flyway migrations must never be regenerated; choose a new version for an SDK upgrade.
"""

import argparse
import hashlib
import json
import re
from collections import Counter
from pathlib import Path


EXCLUDED = {
    "NET_CONFIG_RESULT_EVENT_EX": "配置操作返回通知，不是设备告警",
    "NET_START_LISTEN_FINISH_EVENT": "订阅完成通知，不是设备告警",
    "NET_AUTO_RECONNECT_FAILD": "SDK 自动重连失败通知，不是设备告警",
    "EVENT_IVS_ALL": "订阅全部事件的通配符，不是事件编号",
}
# Preserve existing subscription types and thing-model identifiers. Only documented
# variants of these existing business events are consolidated across callbacks.
BUSINESS_EVENTS = [
    ("alarm_input", "alarmInput", "报警输入", [
        "NET_ALARM_ALARM_EX", "NET_ALARM_ALARM_EX2", "EVENT_ALARM_LOCALALARM"]),
    ("motion", "motionDetection", "移动侦测", [
        "NET_MOTION_ALARM_EX", "NET_EVENT_MOTIONDETECT"]),
    ("video_lost", "videoLoss", "视频丢失", [
        "NET_VIDEOLOST_ALARM_EX", "NET_ALARM_VIDEO_LOSS"]),
    ("shelter", "videoTamper", "视频遮挡", [
        "NET_SHELTER_ALARM_EX", "NET_ALARM_VIDEOBLIND", "EVENT_ALARM_VIDEOBLIND"]),
    ("disk_full", "diskFull", "硬盘满", ["NET_DISKFULL_ALARM_EX"]),
    ("disk_error", "diskError", "硬盘故障", ["NET_DISKERROR_ALARM_EX"]),
    ("line_crossing", "lineCrossing", "越界侦测", [
        "EVENT_IVS_CROSSLINEDETECTION", "EVENT_IVS_CROSSLINEDETECTION_EX",
        "NET_EVENT_CROSSLINE_DETECTION"]),
    ("intrusion", "regionIntrusion", "区域入侵", [
        "EVENT_IVS_CROSSREGIONDETECTION", "NET_EVENT_CROSSREGION_DETECTION"]),
]


def extract(sdk_path):
    source = sdk_path.read_text(encoding="utf-8-sig")
    constants = re.compile(
        r"^\s*public static final int\s+(\w+)\s*=\s*(0[xX][0-9a-fA-F]+|\d+);\s*//+([^\n]*)",
        re.M,
    )
    rows = {}
    names = {}
    excluded = []
    excluded_names = set()

    def add(family, name, value, description, path, line):
        code = int(value, 0)
        reason = EXCLUDED.get(name)
        if re.search(r"(?:只|仅)用于.*规则", description):
            reason = "仅用于规则配置，不是独立回调事件"
        elif "只用于订阅手机推送" in description:
            reason = "仅用于手机推送，不是本项目 IVS 回调事件"
        elif "暂时不支持单独使用" in description:
            reason = "仅作为开放智能事件内部规则，不可独立订阅"
        if reason:
            excluded.append((name, code, reason))
            excluded_names.add(name)
            rows.pop((family, code), None)
            return
        if name in excluded_names:
            return
        key = (family, code)
        if name in names and names[name] != key:
            raise ValueError(f"Conflicting SDK values for {name}")
        names[name] = key
        if key in rows:
            if rows[key]["sdkCode"] != name:
                raise ValueError(f"Conflicting SDK constants at {key}")
            return  # NetSDKLib has priority over companion enum descriptions.
        rows[key] = dict(family=family, code=code, sdkCode=name,
                         description=description.strip(), source=path, line=line)

    for family, start, end in [
        ("alarm", "// CLIENT_StartListenEx报警事件", "// 内部常量定义"),
        ("ivs", "// CLIENT_RealLoadPictureEx 智能抓图事件",
         "// CLIENT_GetNewDevConfig / CLIENT_SetNewDevConfig 配置项"),
    ]:
        offset = source.index(start)
        section = source[offset:source.index(end, offset)]
        for match in constants.finditer(section):
            line = source.count("\n", 0, offset + match.start(1)) + 1
            add(family, *match.groups(), sdk_path.name, line)

    # Documented alarm constant located just outside the StartListenEx section.
    wifi = next(m for m in constants.finditer(source) if m[1] == "NET_ALARM_WIFI_SEARCH_EX")
    add("alarm", *wifi.groups(), sdk_path.name, source.count("\n", 0, wifi.start(1)) + 1)

    enum_path = sdk_path.parent / "enumeration/EM_EVENT_IVS_TYPE.java"
    enum_source = enum_path.read_text(encoding="utf-8-sig")
    pattern = r'^\s*(EVENT_\w+)\(\s*(0[xX][0-9a-fA-F]+|\d+)\s*,\s*"([^"\n]*)"\s*\)'
    matches = list(re.finditer(pattern, enum_source, re.M))
    if len(matches) != len(re.findall(r"^\s*EVENT_\w+\(", enum_source, re.M)):
        raise ValueError("Unparsed IVS enum entry; review the new SDK syntax")
    for match in matches:
        line_end = enum_source.find("\n", match.end())
        tail = enum_source[match.end():line_end].split("//", 1)
        description = match[3] + (" " + tail[1].strip() if len(tail) == 2 else "")
        add("ivs", match[1], match[2], description, "enumeration/" + enum_path.name,
            enum_source.count("\n", 0, match.start(1)) + 1)

    overrides = {name: (typ, event_id, label)
                 for typ, event_id, label, constants_ in BUSINESS_EVENTS for name in constants_}
    for row in rows.values():
        name = row["sdkCode"]
        # Vendor names distinguish different events that happen to share a Chinese label.
        typ = "dahua_" + name.lower()
        event_id = "dahua" + "".join(part.title() for part in name.split("_"))
        label = re.split(r"[（(,，;；。]", row["description"], maxsplit=1)[0].strip()
        typ, event_id, label = overrides.get(name, (typ, event_id, label))
        remark = dict(type=typ, eventId=event_id, sdkCode=name)
        # Compatible with older gateway payloads which carry only alarmType.
        remark["aliases"] = [f"dahua_{'ivs' if row['family'] == 'ivs' else 'command'}_{row['code']}"]
        row.update(label=label, remark=remark, status="1" if "保留" in row["description"] else "0")
        row["value"] = (f"ivs:0x{row['code']:08X}" if row["family"] == "ivs"
                        else f"0x{row['code']:04X}")
        assert label and len(label) <= 100, row
        assert re.fullmatch(r"[A-Za-z0-9_.:-]{1,100}", typ), row
        assert re.fullmatch(r"[A-Za-z][A-Za-z0-9_]{0,99}", event_id), row
        assert len(json.dumps(remark, ensure_ascii=False)) <= 500, row
    identities = {}
    for row in rows.values():
        identity = (row["remark"]["type"], row["label"])
        previous = identities.setdefault(row["remark"]["eventId"], identity)
        if previous != identity:
            raise ValueError(f"Conflicting business event: {row}")
    return sorted(rows.values(), key=lambda row: (row["family"], row["code"])), sorted(set(excluded)), enum_path


SQL_START = """-- PostgreSQL only. Run in one transaction (Flyway default / psql --single-transaction).
-- No hard-coded schema or credentials. Explicit IDs also support tables without sequences.
-- Existing rows (including custom labels, status, and business IDs) are never overwritten.
SET LOCAL lock_timeout = '5s';
LOCK TABLE sys_dict_type, sys_dict_data IN SHARE ROW EXCLUSIVE MODE;

CREATE OR REPLACE FUNCTION pg_temp.dahua_sdk_key(value text) RETURNS text
LANGUAGE plpgsql IMMUTABLE AS $$
DECLARE
    v text := lower(btrim(value));
    family text := 'alarm';
    code bigint;
BEGIN
    IF v LIKE 'ivs:%' THEN family := 'ivs'; v := substr(v, 5);
    ELSIF v LIKE 'alarm:%' THEN v := substr(v, 7);
    END IF;
    IF v ~ '^0x[0-9a-f]{1,8}$' THEN
        code := ('x' || lpad(substr(v, 3), 8, '0'))::bit(32)::bigint;
    ELSIF v ~ '^-?[0-9]{1,10}$' THEN
        code := v::bigint;
        IF code < -2147483648 OR code > 4294967295 THEN RETURN NULL; END IF;
        IF code < 0 THEN code := code + 4294967296; END IF;
    ELSE RETURN NULL;
    END IF;
    RETURN family || ':' || code;
END $$;

CREATE OR REPLACE FUNCTION pg_temp.dahua_sdk_config(value text) RETURNS jsonb
LANGUAGE plpgsql IMMUTABLE AS $$
BEGIN
    IF jsonb_typeof(value::jsonb) = 'object' THEN RETURN value::jsonb; END IF;
    RETURN '{}'::jsonb;
EXCEPTION WHEN invalid_text_representation THEN RETURN '{}'::jsonb;
END $$;

CREATE TEMP TABLE dahua_sdk_seed (
    sort integer, label varchar(100), value varchar(100) PRIMARY KEY,
    status char(1), remark jsonb
) ON COMMIT DROP;

INSERT INTO dahua_sdk_seed (sort, label, value, status, remark) VALUES
"""

SQL_END = """

INSERT INTO sys_dict_type (dict_id, dict_name, dict_type, status, create_by, create_time, remark)
SELECT (SELECT COALESCE(MAX(dict_id), 0) + 1 FROM sys_dict_type),
       '大华SDK告警事件', 'video_sdk_event_dahua_sdk', '0', 'nexiot', CURRENT_TIMESTAMP,
       '字典值：普通告警 0x编号 / 智能抓图 ivs:0x编号；备注 JSON：type、eventId、sdkCode、aliases'
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_type WHERE dict_type = 'video_sdk_event_dahua_sdk');

-- When adding another code for an existing business event, inherit its customization.
-- Bad JSON is isolated, as it is in VideoSdkEventCatalog; it cannot abort the migration.
WITH anchors AS (
    SELECT DISTINCT ON (s.remark->>'eventId')
           s.remark->>'eventId' AS event_id, d.dict_label, d.status,
           pg_temp.dahua_sdk_config(d.remark)->>'type' AS business_type,
           pg_temp.dahua_sdk_config(d.remark)->>'eventId' AS business_id
    FROM dahua_sdk_seed s
    JOIN sys_dict_data d ON d.dict_type = 'video_sdk_event_dahua_sdk'
                       AND pg_temp.dahua_sdk_key(d.dict_value) = pg_temp.dahua_sdk_key(s.value)
    WHERE pg_temp.dahua_sdk_config(d.remark)->>'type' ~ '^[A-Za-z0-9_.:-]{1,100}$'
      AND pg_temp.dahua_sdk_config(d.remark)->>'type' <> 'vendor_raw'
      AND pg_temp.dahua_sdk_config(d.remark)->>'eventId' ~ '^[A-Za-z][A-Za-z0-9_]{0,99}$'
      AND pg_temp.dahua_sdk_config(d.remark)->>'eventId' NOT IN ('dahuaSdkEvent', 'hikSdkEvent')
      AND length(btrim(d.dict_label)) > 0 AND d.status IN ('0', '1')
    ORDER BY s.remark->>'eventId', d.dict_sort, d.dict_code
)
UPDATE dahua_sdk_seed s
SET label = a.dict_label, status = a.status,
    remark = s.remark || jsonb_build_object('type', a.business_type, 'eventId', a.business_id)
FROM anchors a WHERE s.remark->>'eventId' = a.event_id;

INSERT INTO sys_dict_data
    (dict_code, dict_sort, dict_label, dict_value, dict_type, is_default, status,
     create_by, create_time, remark)
SELECT ids.base + ROW_NUMBER() OVER (ORDER BY s.sort), s.sort, s.label, s.value,
       'video_sdk_event_dahua_sdk', 'N', s.status, 'nexiot', CURRENT_TIMESTAMP, s.remark::text
FROM dahua_sdk_seed s
CROSS JOIN (SELECT COALESCE(MAX(dict_code), 0) AS base FROM sys_dict_data) ids
WHERE NOT EXISTS (
    SELECT 1 FROM sys_dict_data d WHERE d.dict_type = 'video_sdk_event_dahua_sdk'
      AND pg_temp.dahua_sdk_key(d.dict_value) = pg_temp.dahua_sdk_key(s.value)
);

-- Assert full numeric coverage before committing; 0x3015 is intentionally not invented.
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM dahua_sdk_seed s WHERE NOT EXISTS (
            SELECT 1 FROM sys_dict_data d WHERE d.dict_type = 'video_sdk_event_dahua_sdk'
              AND pg_temp.dahua_sdk_key(d.dict_value) = pg_temp.dahua_sdk_key(s.value)
        )
    ) THEN RAISE EXCEPTION 'Incomplete Dahua SDK event dictionary'; END IF;
END $$;
"""


def sql_quote(value):
    return "'" + value.replace("'", "''") + "'"


def generate(sdk_path):
    rows, excluded, enum_path = extract(sdk_path)
    counts = Counter(row["family"] for row in rows)
    header = [
        "-- 大华 Java NetSDK V3.061.0000001.0.R.260826 完整回调事件字典",
        "-- Generated by cn-universal-db-migrator/scripts/generate_dahua_sdk_event_dict.py",
        f"-- {len(rows)} mappings: {counts['alarm']} ALARM, {counts['ivs']} IVS.",
        "-- Reserved IVS definitions are retained with status=1; existing status is preserved.",
        "-- Event code recognition only; vendor binary payload fields require separate decoders.",
        "-- 0x3015 (12309) is absent from this SDK; it remains a vendor_raw event.",
    ]
    for path in [sdk_path, enum_path]:
        header.append(f"-- {path.name} SHA-256: {hashlib.sha256(path.read_bytes()).hexdigest()}")
    for name, code, reason in excluded:
        header.append(f"-- Excluded {name} (0x{code:X}): {reason}")
    header += [
        "-- NET_UPLOAD_* belongs to CLIENT_StartService; NET_ALARM_BUS_PASSENGER_CARD_CHECK",
        "-- belongs to CLIENT_AttachBusState. These are not ALARM/IVS callback codes.",
    ]
    values = []
    for index, row in enumerate(rows, 1001):
        comment = f"-- {row['source']}:{row['line']} {row['description']}"
        remark = json.dumps(row["remark"], ensure_ascii=False, separators=(",", ":"))
        values.append(comment + "\n(" + ", ".join([
            str(index), sql_quote(row["label"]), sql_quote(row["value"]),
            sql_quote(row["status"]), sql_quote(remark),
        ]) + ")")
    return "\n".join(header) + "\n" + SQL_START + ",\n".join(values) + ";" + SQL_END, rows


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("sdk", type=Path)
    parser.add_argument("output", type=Path)
    args = parser.parse_args()
    sql, rows = generate(args.sdk)
    # Refuse to rewrite an existing migration, including an already-applied version.
    with args.output.open("x", encoding="utf-8") as output:
        output.write(sql)
    print(f"Generated {len(rows)} mappings: {dict(Counter(r['family'] for r in rows))}")
    print(f"Business events: {len({r['remark']['eventId'] for r in rows})}; "
          f"reserved/disabled: {sum(r['status'] == '1' for r in rows)}")


if __name__ == "__main__":
    main()
