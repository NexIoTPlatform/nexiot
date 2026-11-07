package cn.universal.listener;

import cn.universal.persistence.support.SupportMapAreasSpatialColumns;
import cn.universal.persistence.support.SupportMapAreasSpatialCapability;
import jakarta.annotation.PostConstruct;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import javax.sql.DataSource;
import lombok.extern.slf4j.Slf4j;

/**
 * Retained for future reference.
 *
 * <p>Area lookup enablement is now controlled manually by
 * `iot.support-map-areas.lookup-enabled`, so this probe is intentionally not wired into Spring.
 */
@Slf4j
public class SpatialCapabilityStartupListener {

  private static final String TABLE_NAME = "support_map_areas";
  private static final String PG_SCRIPT =
      "docs/202603-202604-upgrade/20260329_pg_support_map_areas_postgis.sql";
  private static final String DM_SCRIPT =
      "docs/202603-202604-upgrade/20260329_dm_support_map_areas_dmgeo.sql";

  private final DataSource dataSource;

  public SpatialCapabilityStartupListener(DataSource dataSource) {
    this.dataSource = dataSource;
  }

  @PostConstruct
  public void inspectSpatialCapability() {
    try (Connection connection = dataSource.getConnection()) {
      String productName = connection.getMetaData().getDatabaseProductName();
      if ("PostgreSQL".equalsIgnoreCase(productName)) {
        checkPostgreSql(connection);
      } else if (productName != null
          && productName.toUpperCase(Locale.ROOT).contains("DM")) {
        checkDameng(connection);
      }
    } catch (SQLException e) {
      log.warn("Spatial capability check skipped: unable to inspect datasource", e);
    }
  }

  private void checkPostgreSql(Connection connection) throws SQLException {
    boolean postgisReady =
        queryBoolean(
            connection, "select exists(select 1 from pg_extension where extname = 'postgis')");
    Map<String, String> columnTypes = fetchColumnTypes(connection, TABLE_NAME);
    String locationColumn =
        firstGeometryColumn(columnTypes, "GEOMETRY", "LOCATION_GEOM", "LOCATION");
    String polygonColumn = firstGeometryColumn(columnTypes, "GEOMETRY", "POLYGON_GEOM", "POLYGON");
    boolean locationReady = locationColumn != null;
    boolean polygonReady = polygonColumn != null;

    SupportMapAreasSpatialColumns.configurePostgreSql(locationColumn, polygonColumn);
    SupportMapAreasSpatialCapability.configurePostgreSql(postgisReady && locationReady && polygonReady);

    if (postgisReady && locationReady && polygonReady) {
      log.info(
          "PostgreSQL spatial support ready for support_map_areas (location: {}, polygon: {}).",
          locationColumn.toLowerCase(Locale.ROOT),
          polygonColumn.toLowerCase(Locale.ROOT));
      return;
    }

    List<String> missing = new ArrayList<>();
    if (!postgisReady) {
      missing.add("PostGIS extension not installed");
    }
    if (!locationReady) {
      missing.add("no geometry column found in location/location_geom");
    }
    if (!polygonReady) {
      missing.add("no geometry column found in polygon/polygon_geom");
    }
    log.warn(
        "PostgreSQL spatial support is not ready for {}: {}. Run {}",
        TABLE_NAME,
        String.join("; ", missing),
        PG_SCRIPT);
  }

  private void checkDameng(Connection connection) throws SQLException {
    boolean dmgeoReady =
        queryLong(
                connection,
                "select count(*) from all_objects "
                    + "where owner = 'SYSGEO' and object_name = 'DMGEO' and object_type = 'PACKAGE'")
            > 0;
    Map<String, String> columnTypes = fetchColumnTypes(connection, TABLE_NAME);
    String locationColumn =
        firstColumnContainingType(columnTypes, "ST_GEOMETRY", "LOCATION_GEOM", "LOCATION");
    String polygonColumn =
        firstColumnContainingType(columnTypes, "ST_GEOMETRY", "POLYGON_GEOM", "POLYGON");
    boolean locationReady = locationColumn != null;
    boolean polygonReady = polygonColumn != null;

    SupportMapAreasSpatialColumns.configureDameng(locationColumn, polygonColumn);
    SupportMapAreasSpatialCapability.configureDameng(dmgeoReady && locationReady && polygonReady);

    if (dmgeoReady && locationReady && polygonReady) {
      log.info(
          "Dameng spatial support ready for support_map_areas (location: {}, polygon: {}).",
          locationColumn.toLowerCase(Locale.ROOT),
          polygonColumn.toLowerCase(Locale.ROOT));
      return;
    }

    List<String> missing = new ArrayList<>();
    if (!dmgeoReady) {
      missing.add("SYSGEO.DMGEO package missing");
    }
    if (!locationReady) {
      missing.add("no ST_GEOMETRY column found in location/location_geom");
    }
    if (!polygonReady) {
      missing.add("no ST_GEOMETRY column found in polygon/polygon_geom");
    }
    log.warn(
        "Dameng spatial support is not ready for {}: {}. Run {}",
        TABLE_NAME,
        String.join("; ", missing),
        DM_SCRIPT);
  }

  private Map<String, String> fetchColumnTypes(Connection connection, String tableName)
      throws SQLException {
    DatabaseMetaData metaData = connection.getMetaData();
    String catalog = connection.getCatalog();
    String schema = connection.getSchema();
    Set<String> schemaCandidates = new LinkedHashSet<>();
    addCandidate(schemaCandidates, schema);
    addCandidate(schemaCandidates, schema == null ? null : schema.toUpperCase(Locale.ROOT));
    addCandidate(schemaCandidates, schema == null ? null : schema.toLowerCase(Locale.ROOT));
    schemaCandidates.add(null);

    Set<String> tableCandidates = new LinkedHashSet<>();
    addCandidate(tableCandidates, tableName);
    addCandidate(tableCandidates, tableName.toUpperCase(Locale.ROOT));
    addCandidate(tableCandidates, tableName.toLowerCase(Locale.ROOT));

    Map<String, String> columns = new HashMap<>();
    for (String schemaCandidate : schemaCandidates) {
      for (String tableCandidate : tableCandidates) {
        try (ResultSet resultSet =
            metaData.getColumns(catalog, schemaCandidate, tableCandidate, null)) {
          while (resultSet.next()) {
            String columnName = resultSet.getString("COLUMN_NAME");
            String typeName = resultSet.getString("TYPE_NAME");
            if (columnName != null) {
              columns.putIfAbsent(
                  columnName.toUpperCase(Locale.ROOT),
                  typeName == null ? "" : typeName.toUpperCase(Locale.ROOT));
            }
          }
        }
        if (!columns.isEmpty()) {
          return columns;
        }
      }
    }
    return columns;
  }

  private boolean queryBoolean(Connection connection, String sql) throws SQLException {
    try (PreparedStatement preparedStatement = connection.prepareStatement(sql);
        ResultSet resultSet = preparedStatement.executeQuery()) {
      return resultSet.next() && resultSet.getBoolean(1);
    }
  }

  private long queryLong(Connection connection, String sql) throws SQLException {
    try (PreparedStatement preparedStatement = connection.prepareStatement(sql);
        ResultSet resultSet = preparedStatement.executeQuery()) {
      return resultSet.next() ? resultSet.getLong(1) : 0L;
    }
  }

  private boolean isType(String actualType, String expectedType) {
    return expectedType.equals(actualType);
  }

  private boolean containsType(String actualType, String expectedTypePart) {
    return actualType != null && actualType.contains(expectedTypePart);
  }

  private String firstGeometryColumn(
      Map<String, String> columnTypes, String expectedType, String... candidates) {
    for (String candidate : candidates) {
      if (isType(columnTypes.get(candidate), expectedType)) {
        return candidate;
      }
    }
    return null;
  }

  private String firstColumnContainingType(
      Map<String, String> columnTypes, String expectedTypePart, String... candidates) {
    for (String candidate : candidates) {
      if (containsType(columnTypes.get(candidate), expectedTypePart)) {
        return candidate;
      }
    }
    return null;
  }

  private void addCandidate(Set<String> candidates, String candidate) {
    if (candidate != null && !candidate.isBlank()) {
      candidates.add(candidate);
    }
  }
}
