-- Point templates package the product model, points and acquisition groups together.
-- Legacy CONFIG rows and immutable version contents are preserved. Run in the target
-- industrial schema; this is an incremental migration, never a bootstrap replay.
SET LOCAL lock_timeout = '10s';

ALTER TABLE iot_industrial_config_template
    ADD COLUMN IF NOT EXISTS template_type VARCHAR(16) NOT NULL DEFAULT 'CONFIG',
    ADD COLUMN IF NOT EXISTS protocol VARCHAR(32),
    ADD COLUMN IF NOT EXISTS status VARCHAR(16) NOT NULL DEFAULT 'PUBLISHED',
    ADD COLUMN IF NOT EXISTS draft_content JSONB,
    ADD COLUMN IF NOT EXISTS draft_revision BIGINT NOT NULL DEFAULT 0;

-- Application provenance stays on copied device configuration; archive keeps history
-- and no FK ties a live device configuration to template lifecycle operations.
ALTER TABLE iot_industrial_point
    ADD COLUMN IF NOT EXISTS source_template_id BIGINT,
    ADD COLUMN IF NOT EXISTS source_template_version INTEGER;
ALTER TABLE iot_industrial_collect_group
    ADD COLUMN IF NOT EXISTS source_template_id BIGINT,
    ADD COLUMN IF NOT EXISTS source_template_version INTEGER;
COMMENT ON COLUMN iot_industrial_point.source_template_id IS '来源点位模板ID，手工配置为空；归档模板仍保留来源。';
COMMENT ON COLUMN iot_industrial_point.source_template_version IS '应用时的已发布模板版本，现场配置不随模板升级自动变化。';
COMMENT ON COLUMN iot_industrial_collect_group.source_template_id IS '来源点位模板ID，手工配置为空；归档模板仍保留来源。';
COMMENT ON COLUMN iot_industrial_collect_group.source_template_version IS '应用时的已发布模板版本，现场配置不随模板升级自动变化。';
DO $point_template$
DECLARE actual record;
BEGIN
    FOR actual IN
        SELECT expected.table_name, expected.name, expected.type_name,
               format_type(a.atttypid,a.atttypmod) AS actual_type, a.attnotnull,
               pg_get_expr(d.adbin,d.adrelid) AS actual_default
        FROM (VALUES ('iot_industrial_point','source_template_id','bigint'),
                     ('iot_industrial_point','source_template_version','integer'),
                     ('iot_industrial_collect_group','source_template_id','bigint'),
                     ('iot_industrial_collect_group','source_template_version','integer')) expected(table_name,name,type_name)
        LEFT JOIN pg_attribute a ON a.attrelid = to_regclass(expected.table_name)
             AND a.attname = expected.name AND NOT a.attisdropped
        LEFT JOIN pg_attrdef d ON d.adrelid=a.attrelid AND d.adnum=a.attnum
    LOOP
        IF actual.actual_type IS DISTINCT FROM actual.type_name
           OR actual.attnotnull IS DISTINCT FROM false OR actual.actual_default IS NOT NULL THEN
            RAISE EXCEPTION 'Incompatible point-template provenance column %.%', actual.table_name,actual.name;
        END IF;
    END LOOP;
END $point_template$;

-- Reject a partially prepared but incompatible schema instead of accepting it silently.
DO $point_template$
DECLARE actual record;
BEGIN
    FOR actual IN
        SELECT expected.name, expected.type_name, expected.required, expected.default_value,
               format_type(a.atttypid, a.atttypmod) AS actual_type, a.attnotnull,
               pg_get_expr(d.adbin,d.adrelid) AS actual_default
        FROM (VALUES ('template_type', 'character varying(16)', true, '''CONFIG''::character varying'),
                     ('protocol', 'character varying(32)', false, NULL),
                     ('status', 'character varying(16)', true, '''PUBLISHED''::character varying'),
                     ('draft_content', 'jsonb', false, NULL),
                     ('draft_revision', 'bigint', true, '0')) expected(name, type_name, required, default_value)
        LEFT JOIN pg_attribute a ON a.attrelid = 'iot_industrial_config_template'::regclass
             AND a.attname = expected.name AND NOT a.attisdropped
        LEFT JOIN pg_attrdef d ON d.adrelid=a.attrelid AND d.adnum=a.attnum
    LOOP
        IF actual.actual_type IS DISTINCT FROM actual.type_name
           OR actual.attnotnull IS DISTINCT FROM actual.required
           OR actual.actual_default IS DISTINCT FROM actual.default_value THEN
            RAISE EXCEPTION 'Incompatible point-template column %', actual.name;
        END IF;
    END LOOP;
END $point_template$;

COMMENT ON COLUMN iot_industrial_config_template.template_type IS 'CONFIG完整配置模板；POINTS物模型、点位和采集组模板。';
COMMENT ON COLUMN iot_industrial_config_template.protocol IS '点位模板适用的规范化工业协议。';
COMMENT ON COLUMN iot_industrial_config_template.status IS 'DRAFT草稿、PUBLISHED已发布、ARCHIVED已归档。';
COMMENT ON COLUMN iot_industrial_config_template.draft_content IS '可编辑草稿快照，包含物模型属性、点位、采集组及关联。';
COMMENT ON COLUMN iot_industrial_config_template.draft_revision IS '草稿乐观锁修订号。';

DO $point_template$
DECLARE definition record;
BEGIN
    FOR definition IN SELECT * FROM (VALUES
        ('ck_iot_ind_config_template_type', 'template_type IN (''CONFIG'', ''POINTS'')'),
        ('ck_iot_ind_config_template_status', 'status IN (''DRAFT'', ''PUBLISHED'', ''ARCHIVED'')'),
        ('ck_iot_ind_config_template_binding', 'template_type <> ''POINTS'' OR (source_product_key IS NOT NULL AND btrim(source_product_key) <> '''' AND protocol IS NOT NULL AND btrim(protocol) <> '''')'),
        ('ck_iot_ind_config_template_revision', 'draft_revision >= 0 AND latest_version >= 0')
    ) expected(name, expression)
    LOOP
        IF NOT EXISTS (SELECT 1 FROM pg_constraint
                       WHERE conrelid = 'iot_industrial_config_template'::regclass
                         AND conname = definition.name) THEN
            EXECUTE format('ALTER TABLE iot_industrial_config_template ADD CONSTRAINT %I CHECK (%s)',
                           definition.name, definition.expression);
        END IF;
    END LOOP;
END $point_template$;

CREATE INDEX IF NOT EXISTS idx_iot_ind_config_template_point_filter
    ON iot_industrial_config_template (tenant_id, template_type, status, protocol, source_product_key);

-- Compare canonical constraint definitions to a temporary expected shape, so re-running
-- after a manual partial preparation is safe without ignoring a different CHECK/index.
DO $point_template$
DECLARE expected record; actual_definition text;
BEGIN
    CREATE TEMP TABLE nexiot_point_template_expected (
        tenant_id VARCHAR(64), template_type VARCHAR(16), status VARCHAR(16),
        protocol VARCHAR(32), source_product_key VARCHAR(64), draft_revision BIGINT, latest_version INTEGER,
        CONSTRAINT ck_iot_ind_config_template_type CHECK (template_type IN ('CONFIG', 'POINTS')),
        CONSTRAINT ck_iot_ind_config_template_status CHECK (status IN ('DRAFT', 'PUBLISHED', 'ARCHIVED')),
        CONSTRAINT ck_iot_ind_config_template_binding CHECK (template_type <> 'POINTS' OR (source_product_key IS NOT NULL AND btrim(source_product_key) <> '' AND protocol IS NOT NULL AND btrim(protocol) <> '')),
        CONSTRAINT ck_iot_ind_config_template_revision CHECK (draft_revision >= 0 AND latest_version >= 0)
    ) ON COMMIT DROP;
    FOR expected IN SELECT conname, pg_get_constraintdef(oid) AS definition
                    FROM pg_constraint WHERE conrelid = 'pg_temp.nexiot_point_template_expected'::regclass
    LOOP
        SELECT pg_get_constraintdef(oid) INTO actual_definition FROM pg_constraint
        WHERE conrelid = 'iot_industrial_config_template'::regclass
          AND conname = expected.conname AND convalidated AND contype = 'c';
        IF actual_definition IS DISTINCT FROM expected.definition THEN
            RAISE EXCEPTION 'Incompatible point-template constraint %', expected.conname;
        END IF;
    END LOOP;
    IF NOT EXISTS (
        SELECT 1 FROM pg_index i JOIN pg_class c ON c.oid = i.indexrelid
        WHERE i.indexrelid = to_regclass('idx_iot_ind_config_template_point_filter')
          AND i.indrelid = 'iot_industrial_config_template'::regclass
          AND i.indisvalid AND NOT i.indisunique AND i.indpred IS NULL
          AND pg_get_indexdef(i.indexrelid) LIKE '% USING btree (tenant_id, template_type, status, protocol, source_product_key)'
    ) THEN
        RAISE EXCEPTION 'Incompatible point-template filter index';
    END IF;
END $point_template$;

-- Lock the small permission tables while allocating IDs. Existing menu customizations
-- are retained; a collision in a route or permission definition fails for review.
LOCK TABLE sys_menu, sys_role_menu, iot_tenant_package_menu IN SHARE ROW EXCLUSIVE MODE;
DO $point_template$
DECLARE root_id BIGINT; page_id BIGINT; candidate_id BIGINT; item record;
BEGIN
    IF (SELECT count(*) FROM sys_menu WHERE parent_id = 0 AND path = 'device-template') > 1 THEN
        RAISE EXCEPTION 'Duplicate device-template root menus';
    END IF;
    SELECT menu_id INTO root_id FROM sys_menu WHERE parent_id = 0 AND path = 'device-template';
    IF root_id IS NULL THEN
        SELECT CASE WHEN EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 1700)
                    THEN COALESCE(MAX(menu_id), 0) + 1 ELSE 1700 END INTO root_id FROM sys_menu;
        INSERT INTO sys_menu (menu_id,menu_name,parent_id,order_num,path,component,is_frame,is_cache,menu_type,visible,status,perms,icon,create_by,create_time,remark)
        VALUES (root_id,'设备模板',0,25,'device-template','',1,0,'M','0','0','','custom:menu-template','migration',CURRENT_TIMESTAMP,'设备接入配置复用');
    ELSIF NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = root_id AND menu_type = 'M' AND is_frame = 1) THEN
        RAISE EXCEPTION 'Incompatible device-template root menu';
    END IF;

    FOR item IN SELECT * FROM (VALUES
        (1701, '点位模板', 1, 'point', 'iot/point-template/index', 'C', 'industrial:point-template:query', 'custom:iot-model'),
        (1702, '新增点位模板', 1, '#', '', 'F', 'industrial:point-template:add', '#'),
        (1703, '编辑点位模板', 2, '#', '', 'F', 'industrial:point-template:edit', '#'),
        (1704, '删除点位模板', 3, '#', '', 'F', 'industrial:point-template:remove', '#'),
        (1705, '发布点位模板', 4, '#', '', 'F', 'industrial:point-template:publish', '#'),
        (1706, '应用点位模板', 5, '#', '', 'F', 'industrial:point-template:apply', '#')
    ) expected(seed_id, name, order_num, path, component, menu_type, perms, icon)
    LOOP
        IF (SELECT count(*) FROM sys_menu WHERE perms = item.perms) > 1 THEN
            RAISE EXCEPTION 'Duplicate point-template permission %', item.perms;
        END IF;
        SELECT menu_id INTO candidate_id FROM sys_menu WHERE perms = item.perms;
        IF candidate_id IS NULL THEN
            SELECT CASE WHEN EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = item.seed_id)
                        THEN COALESCE(MAX(menu_id), 0) + 1 ELSE item.seed_id END INTO candidate_id FROM sys_menu;
            INSERT INTO sys_menu (menu_id,menu_name,parent_id,order_num,path,component,is_frame,is_cache,menu_type,visible,status,perms,icon,create_by,create_time,remark)
            VALUES (candidate_id,item.name,CASE WHEN item.menu_type = 'C' THEN root_id ELSE page_id END,
                    item.order_num,item.path,item.component,1,0,item.menu_type,'0','0',item.perms,item.icon,
                    'migration',CURRENT_TIMESTAMP,'物模型、采集组和点位共同复用');
        ELSIF NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = candidate_id
                         AND parent_id = CASE WHEN item.menu_type = 'C' THEN root_id ELSE page_id END
                         AND path = item.path AND component = item.component AND menu_type = item.menu_type
                         AND is_frame = 1) THEN
            RAISE EXCEPTION 'Incompatible point-template menu %', item.perms;
        END IF;
        IF item.menu_type = 'C' THEN page_id := candidate_id; END IF;
    END LOOP;
END $point_template$;

-- Preserve the prior permission boundary: old template authors gain draft/publish,
-- old import/point-create operators gain apply; equivalent point editors/removers gain
-- draft/publish/archive. Every new operation also gains the query/ancestor.
WITH candidates AS (
    SELECT DISTINCT old_grant.role_id, target.menu_id
    FROM sys_role_menu old_grant JOIN sys_menu old_menu ON old_menu.menu_id = old_grant.menu_id
    CROSS JOIN sys_menu target
    WHERE old_menu.perms IN ('industrial:template:query','industrial:template:add','industrial:template:remove','industrial:config:import','industrial:point:add','industrial:point:edit','industrial:point:remove')
      AND (target.path = 'device-template' AND target.parent_id = 0
           OR target.perms = 'industrial:point-template:query'
           OR old_menu.perms = 'industrial:template:add' AND target.perms IN ('industrial:point-template:add','industrial:point-template:edit','industrial:point-template:publish')
           OR old_menu.perms = 'industrial:point:add' AND target.perms IN ('industrial:point-template:add','industrial:point-template:apply')
           OR old_menu.perms = 'industrial:point:edit' AND target.perms IN ('industrial:point-template:edit','industrial:point-template:publish')
           OR old_menu.perms = 'industrial:point:remove' AND target.perms = 'industrial:point-template:remove'
           OR old_menu.perms = 'industrial:template:remove' AND target.perms = 'industrial:point-template:remove'
           OR old_menu.perms = 'industrial:config:import' AND target.perms = 'industrial:point-template:apply')
    UNION
    SELECT role.role_id, target.menu_id FROM sys_role role CROSS JOIN sys_menu target
    WHERE (role.role_type = 'PLATFORM_ADMIN' OR role.role_id = 1 AND role.role_key = 'admin')
      AND (target.path = 'device-template' AND target.parent_id = 0 OR target.perms LIKE 'industrial:point-template:%')
)
INSERT INTO sys_role_menu (role_id,menu_id,uuid)
SELECT candidate.role_id,candidate.menu_id,ids.max_id + ROW_NUMBER() OVER (ORDER BY candidate.role_id,candidate.menu_id)
FROM candidates candidate CROSS JOIN (SELECT COALESCE(MAX(uuid),0) AS max_id FROM sys_role_menu) ids
WHERE NOT EXISTS (SELECT 1 FROM sys_role_menu existing WHERE existing.role_id = candidate.role_id AND existing.menu_id = candidate.menu_id);

WITH candidates AS (
    SELECT DISTINCT old_grant.package_id, target.menu_id
    FROM iot_tenant_package_menu old_grant JOIN sys_menu old_menu ON old_menu.menu_id = old_grant.menu_id
    CROSS JOIN sys_menu target
    WHERE old_menu.perms IN ('industrial:template:query','industrial:template:add','industrial:template:remove','industrial:config:import','industrial:point:add','industrial:point:edit','industrial:point:remove')
      AND (target.path = 'device-template' AND target.parent_id = 0
           OR target.perms = 'industrial:point-template:query'
           OR old_menu.perms = 'industrial:template:add' AND target.perms IN ('industrial:point-template:add','industrial:point-template:edit','industrial:point-template:publish')
           OR old_menu.perms = 'industrial:point:add' AND target.perms IN ('industrial:point-template:add','industrial:point-template:apply')
           OR old_menu.perms = 'industrial:point:edit' AND target.perms IN ('industrial:point-template:edit','industrial:point-template:publish')
           OR old_menu.perms = 'industrial:point:remove' AND target.perms = 'industrial:point-template:remove'
           OR old_menu.perms = 'industrial:template:remove' AND target.perms = 'industrial:point-template:remove'
           OR old_menu.perms = 'industrial:config:import' AND target.perms = 'industrial:point-template:apply')
)
INSERT INTO iot_tenant_package_menu (id,package_id,menu_id,create_time)
SELECT ids.max_id + ROW_NUMBER() OVER (ORDER BY candidate.package_id,candidate.menu_id),candidate.package_id,candidate.menu_id,CURRENT_TIMESTAMP
FROM candidates candidate CROSS JOIN (SELECT COALESCE(MAX(id),0) AS max_id FROM iot_tenant_package_menu) ids
WHERE NOT EXISTS (SELECT 1 FROM iot_tenant_package_menu existing WHERE existing.package_id = candidate.package_id AND existing.menu_id = candidate.menu_id);

-- MAX allocation must not leave identity-backed menu/role sequences behind.
DO $point_template$
DECLARE sequence_name text; maximum BIGINT;
BEGIN
    sequence_name := pg_get_serial_sequence('sys_menu','menu_id');
    IF sequence_name IS NOT NULL THEN
        SELECT MAX(menu_id) INTO maximum FROM sys_menu;
        PERFORM setval(sequence_name::regclass, greatest(maximum, (SELECT last_value FROM pg_sequences WHERE schemaname = current_schema() AND sequencename = split_part(sequence_name,'.',2))), true);
    END IF;
    sequence_name := pg_get_serial_sequence('sys_role_menu','uuid');
    IF sequence_name IS NOT NULL THEN
        SELECT MAX(uuid) INTO maximum FROM sys_role_menu;
        PERFORM setval(sequence_name::regclass, greatest(maximum, (SELECT last_value FROM pg_sequences WHERE schemaname = current_schema() AND sequencename = split_part(sequence_name,'.',2))), true);
    END IF;
END $point_template$;
