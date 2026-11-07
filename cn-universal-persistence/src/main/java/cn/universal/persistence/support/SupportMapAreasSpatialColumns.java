package cn.universal.persistence.support;

import java.util.Set;

/** Stores the spatial column names selected at runtime for support_map_areas. */
public final class SupportMapAreasSpatialColumns {

  private static final Set<String> LOCATION_COLUMNS = Set.of("location", "location_geom");
  private static final Set<String> POLYGON_COLUMNS = Set.of("polygon", "polygon_geom");

  private static volatile String postgreSqlLocationColumn = "location_geom";
  private static volatile String postgreSqlPolygonColumn = "polygon_geom";
  private static volatile String damengLocationColumn = "location_geom";
  private static volatile String damengPolygonColumn = "polygon_geom";

  private SupportMapAreasSpatialColumns() {}

  public static void configurePostgreSql(String locationColumn, String polygonColumn) {
    postgreSqlLocationColumn = sanitize(locationColumn, "location_geom", LOCATION_COLUMNS);
    postgreSqlPolygonColumn = sanitize(polygonColumn, "polygon_geom", POLYGON_COLUMNS);
  }

  public static void configureDameng(String locationColumn, String polygonColumn) {
    damengLocationColumn = sanitize(locationColumn, "location_geom", LOCATION_COLUMNS);
    damengPolygonColumn = sanitize(polygonColumn, "polygon_geom", POLYGON_COLUMNS);
  }

  public static String getPostgreSqlLocationColumn() {
    return postgreSqlLocationColumn;
  }

  public static String getPostgreSqlPolygonColumn() {
    return postgreSqlPolygonColumn;
  }

  public static String getDamengLocationColumn() {
    return damengLocationColumn;
  }

  public static String getDamengPolygonColumn() {
    return damengPolygonColumn;
  }

  private static String sanitize(String actual, String fallback, Set<String> allowedColumns) {
    if (actual == null) {
      return fallback;
    }
    String normalized = actual.trim().toLowerCase();
    return allowedColumns.contains(normalized) ? normalized : fallback;
  }
}
