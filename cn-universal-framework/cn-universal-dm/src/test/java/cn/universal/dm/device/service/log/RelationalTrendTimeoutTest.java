package cn.universal.dm.device.service.log;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import cn.universal.common.exception.IoTException;
import cn.universal.persistence.consistent.DeviceLogMetaHashSharding;
import cn.universal.persistence.mapper.IoTDeviceLogMetadataShardMapper;
import cn.universal.persistence.query.PropertyTrendQuery;
import java.sql.SQLException;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;

class RelationalTrendTimeoutTest {
  @Test
  void postgresQueryCancelledSqlStateDoesNotBecomeGenericDatabase500() throws Exception {
    var service = new MysqlDeviceLogService();
    var mapper = mock(IoTDeviceLogMetadataShardMapper.class);
    var shard = mock(DeviceLogMetaHashSharding.class);
    when(shard.generateTableName(anyString(), any())).thenReturn("metadata_6");
    for (var entry :
        java.util.Map.of(
                "ioTDeviceLogMetadataShardMapper", mapper, "deviceLogMetaHashSharding", shard)
            .entrySet()) {
      var field = MysqlDeviceLogService.class.getDeclaredField(entry.getKey());
      field.setAccessible(true);
      field.set(service, entry.getValue());
    }
    var query = new PropertyTrendQuery();
    query.setProductKey("p");
    query.setIotId("i");
    query.setProperty("speed");
    query.setBeginCreateTime(1700000000L);
    query.setEndCreateTime(1700086400L);
    var cancelled =
        new DataAccessResourceFailureException(
            "cancelled", new SQLException("canceling statement due to user request", "57014"));
    when(mapper.selectPropertyTrendSamples(any())).thenThrow(cancelled);
    assertEquals(
        503, assertThrows(IoTException.class, () -> service.queryPropertyTrend(query)).getCode());
    var unavailable =
        new DataAccessResourceFailureException(
            "connection unavailable", new SQLException("connection closed", "08006"));
    doThrow(unavailable).when(mapper).selectPropertyTrendSamples(any());
    assertSame(
        unavailable,
        assertThrows(
            DataAccessResourceFailureException.class, () -> service.queryPropertyTrend(query)));
  }
}
