package cn.universal.persistence.support;

import java.util.Locale;

/** Stores whether native spatial lookup is currently available for support_map_areas. */
public final class SupportMapAreasSpatialCapability {

  private static volatile boolean postgreSqlLookupEnabled = true;
  private static volatile boolean damengLookupEnabled = true;

  private SupportMapAreasSpatialCapability() {}

  public static void configurePostgreSql(boolean enabled) {
    postgreSqlLookupEnabled = enabled;
  }

  public static void configureDameng(boolean enabled) {
    damengLookupEnabled = enabled;
  }

  public static boolean isLookupEnabled(String databaseProductName) {
    if (databaseProductName == null) {
      return true;
    }
    if ("PostgreSQL".equalsIgnoreCase(databaseProductName)) {
      return postgreSqlLookupEnabled;
    }
    if (databaseProductName.toUpperCase(Locale.ROOT).contains("DM")) {
      return damengLookupEnabled;
    }
    return true;
  }

  public static void disable(String databaseProductName) {
    if (databaseProductName == null) {
      return;
    }
    if ("PostgreSQL".equalsIgnoreCase(databaseProductName)) {
      postgreSqlLookupEnabled = false;
      return;
    }
    if (databaseProductName.toUpperCase(Locale.ROOT).contains("DM")) {
      damengLookupEnabled = false;
    }
  }
}
