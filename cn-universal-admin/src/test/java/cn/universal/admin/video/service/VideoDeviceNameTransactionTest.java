package cn.universal.admin.video.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import cn.universal.admin.video.alarm.SdkVideoAlarmContextCache;
import cn.universal.persistence.common.BaseMapper;
import cn.universal.persistence.entity.IoTDevice;
import cn.universal.persistence.entity.VideoPlatformChannel;
import cn.universal.persistence.entity.VideoPlatformDevice;
import cn.universal.persistence.entity.VideoPlatformInstance;
import cn.universal.persistence.mapper.IoTDeviceMapper;
import cn.universal.persistence.mapper.VideoPlatformChannelMapper;
import cn.universal.persistence.mapper.VideoPlatformDeviceMapper;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.Test;
import org.mockito.AdditionalAnswers;
import org.mybatis.spring.SqlSessionTemplate;
import org.mybatis.spring.transaction.SpringManagedTransactionFactory;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionInterceptor;
import tk.mybatis.mapper.entity.EntityColumn;
import tk.mybatis.mapper.mapperhelper.EntityHelper;
import tk.mybatis.mapper.mapperhelper.MapperHelper;

class VideoDeviceNameTransactionTest {
  @Test
  void iotWriteFailureRollsBackBothVideoTablesThenSuccessfulRetryCommitsTogether()
      throws Exception {
    var source = new JdbcDataSource();
    source.setURL(
        "jdbc:h2:mem:video_names_" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1;MODE=PostgreSQL");
    var configuration =
        new Configuration(new Environment("test", new SpringManagedTransactionFactory(), source));
    configuration.setMapUnderscoreToCamelCase(true);
    var helper = new MapperHelper();
    helper.registerMapper(BaseMapper.class);
    for (Class<?> mapper :
        List.of(
            VideoPlatformDeviceMapper.class,
            VideoPlatformChannelMapper.class,
            IoTDeviceMapper.class)) configuration.addMapper(mapper);
    helper.processConfiguration(configuration);
    try (var connection = source.getConnection();
        var statement = connection.createStatement()) {
      for (Class<?> entity :
          List.of(VideoPlatformDevice.class, VideoPlatformChannel.class, IoTDevice.class)) {
        String columns =
            EntityHelper.getColumns(entity).stream()
                .map(
                    column ->
                        column.getColumn()
                            + " "
                            + type(column)
                            + (column.isId() ? " PRIMARY KEY" : ""))
                .collect(Collectors.joining(", "));
        statement.execute(
            "CREATE TABLE " + EntityHelper.getEntityTable(entity).getName() + " (" + columns + ")");
      }
    }
    var session = new SqlSessionTemplate(new SqlSessionFactoryBuilder().build(configuration));
    var devices = session.getMapper(VideoPlatformDeviceMapper.class);
    var channels = session.getMapper(VideoPlatformChannelMapper.class);
    var realIot = session.getMapper(IoTDeviceMapper.class);
    var iot = mock(IoTDeviceMapper.class, AdditionalAnswers.delegatesTo(realIot));
    var instance = VideoPlatformInstance.builder().instanceKey("dh").tenantId("tenant").build();
    devices.insertSelective(
        VideoPlatformDevice.builder()
            .id(10L)
            .instanceKey("dh")
            .deviceId("camera")
            .tenantId("tenant")
            .configuration("{\"sourceChannelNames\":{\"1\":\"露台\"}}")
            .build());
    channels.insertSelective(
        VideoPlatformChannel.builder()
            .id(20L)
            .instanceKey("dh")
            .deviceId(10L)
            .tenantId("tenant")
            .channelId("1")
            .channelName("露台")
            .streamConfig("{\"fusion\":true}")
            .build());
    realIot.insertSelective(
        IoTDevice.builder()
            .id(30L)
            .deviceId("camera")
            .productKey("custom")
            .tenantId("tenant")
            .configuration(
                "{\"instanceKey\":\"dh\",\"channelList\":[{\"channelId\":\"1\",\"channelName\":\"露台\"}]}")
            .build());
    var target =
        new VideoDeviceNameService(devices, channels, iot, mock(SdkVideoAlarmContextCache.class));
    var proxy = new ProxyFactory(target);
    proxy.addAdvice(
        new TransactionInterceptor(
            new DataSourceTransactionManager(source), new AnnotationTransactionAttributeSource()));
    var service = (VideoDeviceNameService) proxy.getProxy();
    doThrow(new IllegalStateException("simulated IoT failure"))
        .when(iot)
        .updateByPrimaryKeySelective(any());
    assertThrows(
        IllegalStateException.class,
        () -> service.renameChannel(instance, "camera", "1", Map.of("channelName", "门口")));
    assertEquals("露台", channels.selectByPrimaryKey(20L).getChannelName());
    assertFalse(devices.selectByPrimaryKey(10L).getConfiguration().contains("门口"));
    assertFalse(realIot.selectByPrimaryKey(30L).getConfiguration().contains("门口"));
    doAnswer(call -> realIot.updateByPrimaryKeySelective(call.getArgument(0)))
        .when(iot)
        .updateByPrimaryKeySelective(any());
    service.renameChannel(instance, "camera", "1", Map.of("channelName", "门口"));
    assertEquals("门口", channels.selectByPrimaryKey(20L).getChannelName());
    assertEquals("{\"fusion\":true}", channels.selectByPrimaryKey(20L).getStreamConfig());
    assertTrue(devices.selectByPrimaryKey(10L).getConfiguration().contains("门口"));
    assertTrue(realIot.selectByPrimaryKey(30L).getConfiguration().contains("门口"));
    try (var connection = source.getConnection();
        var statement = connection.createStatement()) {
      statement.execute("SHUTDOWN");
    }
  }

  private static String type(EntityColumn column) {
    if (column.getJavaType() == Long.class || column.getJavaType() == long.class) return "BIGINT";
    if (column.getJavaType() == Integer.class || column.getJavaType() == int.class)
      return "INTEGER";
    if (column.getJavaType() == Date.class) return "TIMESTAMP";
    if (column.getJavaType() == Boolean.class || column.getJavaType() == boolean.class)
      return "BOOLEAN";
    return "VARCHAR(4000)";
  }
}
