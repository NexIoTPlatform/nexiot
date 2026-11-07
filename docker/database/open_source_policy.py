"""Shared database seed and incremental upgrade policy for NexIoT OSS."""

OSS_MENU_MIGRATION = "V2026110701__open_source_menu_scope.sql"
OSS_MENU_ROOT_IDS = (
    1700, 1283, 1440, 2043977798387777600, 2071107100000000009,
    1230, 2058515756470165504, 2042241329026695168, 2064800000000001000,
    2071107100000000016, 1196, 2043977798387777537,
)
OSS_MENU_PERMISSION_PREFIXES = (
    "industrial:", "video:", "videoai:", "scada:", "flow:", "alarm:",
    "node-red:", "nodered:", "analysis:", "visualization:", "ai:", "iot-card:",
    "tool:tcp:", "tool:udp:",
)
OSS_MENU_ROOT_PATHS = (
    "device-template", "videoCenter", "scada-iot", "videoai-factory",
    "nodered", "edge", "analysis", "platform-ai", "alarm",
)


def retired_menu_predicate(alias=""):
    """Match reviewed roots and plugin permissions, including alternative IDs."""
    prefix = alias + "." if alias else ""
    ids = ",".join(map(str, OSS_MENU_ROOT_IDS))
    paths = ",".join("'" + value + "'" for value in OSS_MENU_ROOT_PATHS)
    permissions = " OR ".join(
        f"COALESCE({prefix}perms,'') LIKE '{value}%'"
        for value in OSS_MENU_PERMISSION_PREFIXES
    )
    return (f"{prefix}menu_id IN ({ids}) OR {prefix}path IN ({paths}) "
            f"OR {prefix}component='flow/index' OR ({permissions})")


def retired_menu_query(schema=None):
    """Resolve descendants recursively without assuming a fixed nesting depth."""
    table = f"{schema}.sys_menu" if schema else "sys_menu"
    return (f"WITH RECURSIVE retired(menu_id) AS (SELECT menu_id FROM {table} "
            f"WHERE {retired_menu_predicate()} UNION SELECT child.menu_id FROM {table} child "
            "JOIN retired parent ON child.parent_id=parent.menu_id) SELECT menu_id FROM retired")


def retained_menu_filter():
    return ("COALESCE(path,'') !~* '^https?://' AND menu_id NOT IN ("
            + retired_menu_query() + ")")

# Public local-installation credentials. Change this password after first login.
# Username: admin; password: NexIoT@2026-ChangeMe
OSS_ADMIN_USERNAME = "admin"
OSS_ADMIN_PASSWORD_HASH = '$2a$12$P6NdJRJChU.hHkTArW5sI.AM.NSb4/8Ote8qvVXemm9y73ZP09e4W'
