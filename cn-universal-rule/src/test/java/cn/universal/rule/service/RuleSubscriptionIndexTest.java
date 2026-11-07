package cn.universal.rule.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import cn.universal.persistence.dto.IoTDeviceDTO;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.sql.ResultSet;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.*;

class RuleSubscriptionIndexTest {
  @Test
  void preloadsParsedRulesDeduplicatesAndPreservesCreatorIsolation() throws Exception {
    JdbcTemplate jdbc = mock(JdbcTemplate.class);
    doAnswer(
            call -> {
              RowCallbackHandler handler = call.getArgument(1);
              for (String relation : List.of("p", "pd", "g")) {
                ResultSet row = mock(ResultSet.class);
                when(row.getLong("id")).thenReturn(1L);
                when(row.getString("creator_id")).thenReturn("u");
                when(row.getString("rule_name")).thenReturn("r");
                when(row.getString("relation_id")).thenReturn(relation);
                when(row.getString("config"))
                    .thenReturn("{\"fields\":\"*\",\"appId\":\"app\",\"targets\":[]}");
                handler.processRow(row);
              }
              return null;
            })
        .when(jdbc)
        .query(anyString(), any(RowCallbackHandler.class));
    try (var index = new RuleSubscriptionIndex(jdbc, new SimpleMeterRegistry())) {
      index.refresh();
      var device =
          IoTDeviceDTO.builder()
              .userUnionId("u")
              .productKey("p")
              .iotId("pd")
              .devGroupId(List.of("g"))
              .build();
      for (int i = 0; i < 1000; i++) assertEquals(1, index.match(device).size());
      assertEquals("select * from app ", index.match(device).getFirst().sql());
      device.setUserUnionId("other");
      assertTrue(index.match(device).isEmpty());
      verify(jdbc, times(1)).query(anyString(), any(RowCallbackHandler.class));
    }
  }
}
