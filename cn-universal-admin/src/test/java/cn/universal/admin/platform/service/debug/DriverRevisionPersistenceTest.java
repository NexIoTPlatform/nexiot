package cn.universal.admin.platform.service.debug;

import static org.junit.jupiter.api.Assertions.*;

import cn.universal.persistence.entity.IoTDeviceProtocol;
import cn.universal.persistence.mapper.IoTProtocolRevisionMapper;
import java.nio.file.Files;
import java.nio.file.Path;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.*;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class DriverRevisionPersistenceTest {
  @ParameterizedTest
  @ValueSource(strings = {"postgresql", "mysql"})
  void persistsInOrderAndRollsBackWithSaveTransaction(String database) throws Exception {
    JdbcDataSource dataSource = new JdbcDataSource();
    dataSource.setURL(
        "jdbc:h2:mem:revision_"
            + database
            + ";DB_CLOSE_DELAY=-1;MODE="
            + (database.equals("postgresql") ? "PostgreSQL" : "MySQL"));
    try (var connection = dataSource.getConnection();
        var statement = connection.createStatement()) {
      statement.execute(
          Files.readString(
              Path.of(
                  "../cn-universal-db-migrator/src/main/resources/db/migration/"
                      + database
                      + "/V2026092401__driver_debug_history.sql")));
    }
    Configuration config =
        new Configuration(new Environment("test", new JdbcTransactionFactory(), dataSource));
    config.addMapper(IoTProtocolRevisionMapper.class);
    SqlSessionFactory factory = new SqlSessionFactoryBuilder().build(config);
    IoTDeviceProtocol protocol =
        IoTDeviceProtocol.builder()
            .id("driver")
            .version("1")
            .configuration("{\"location\":\"original\"}")
            .build();
    try (SqlSession session = factory.openSession(false)) {
      DriverRevisionService service =
          new DriverRevisionService(session.getMapper(IoTProtocolRevisionMapper.class));
      service.recordChange(protocol, "draft", "2", "user");
      assertEquals("draft", service.list("driver").getFirst().getSource());
      session.rollback();
      assertTrue(service.list("driver").isEmpty());
      service.recordChange(protocol, "saved", "2", "user");
      session.commit();
    }
    try (SqlSession session = factory.openSession()) {
      var rows = session.getMapper(IoTProtocolRevisionMapper.class).list("driver");
      assertEquals(2, rows.size());
      assertEquals("saved", rows.getFirst().getSource());
      assertEquals("original", rows.getLast().getSource());
      assertTrue(session.getMapper(IoTProtocolRevisionMapper.class).list("other-driver").isEmpty());
    }
  }
}
