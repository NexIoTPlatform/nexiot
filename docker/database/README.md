# PostgreSQL 初始化

`postgres/manifest.json` 描述初始化对象、系统种子数据、迁移基线和逐文件 SHA-256。`postgres/SHA256SUMS` 用于执行前校验。

当前结构保留 660 张表、48 张分区表、135 个序列和 2145 个索引。菜单种子按社区版范围收口，业务表保持为空。平台管理员是全新安装账号 `admin`，不沿用导出源环境的账号或密码哈希。

Docker Compose 在第一次创建数据库时执行 `docker/postgres-init.sh` 和 `postgres/apply.sh`。手动执行时，通过 `PGHOST`、`PGPORT`、`PGDATABASE`、`PGUSER`、`PGPASSWORD` 设置目标空数据库后运行：

```bash
sh docker/database/postgres/apply.sh
```

脚本使用事务、初始化记录和校验摘要防止覆盖既有数据库。相同种子重复执行不修改数据；种子发生变化时拒绝重放，已有环境必须使用显式增量迁移。

`export_postgres.py` 只读导出结构和白名单种子，强制使用公开安装账号，并以 `open_source_policy.py` 过滤不开放菜单。它不会导出源管理员的密码哈希。

```bash
python3 docker/database/validate_postgres.py
```

校验使用独立临时 Docker PostgreSQL，覆盖干净初始化、菜单升级一致性、数据保留、重复执行和失败回滚。PostgreSQL 18.4 的本轮结果见根目录 `validation/`。MySQL 和达梦对应增量文件已提供，尚未实机验收。
