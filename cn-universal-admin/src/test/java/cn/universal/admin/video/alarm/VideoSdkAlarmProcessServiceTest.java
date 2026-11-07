package cn.universal.admin.video.alarm;

import static org.junit.jupiter.api.Assertions.*;

import cn.universal.common.exception.BaseException;
import cn.universal.persistence.common.BaseMapper;
import cn.universal.persistence.entity.*;
import cn.universal.persistence.mapper.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import javax.sql.DataSource;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.*;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.SqlSessionTemplate;
import org.mybatis.spring.transaction.SpringManagedTransactionFactory;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import tk.mybatis.mapper.mapperhelper.MapperHelper;

class VideoSdkAlarmProcessServiceTest {
  private record Fixture(
      VideoSdkAlarmProcessService service,
      VideoSdkAlarmProcessMapper processes,
      VideoSdkAlarmProcessMemberMapper members,
      DataSource db) {}

  private Fixture fixture() throws Exception {
    var db = new JdbcDataSource();
    db.setURL(
        "jdbc:h2:mem:process_"
            + UUID.randomUUID()
            + ";DB_CLOSE_DELAY=-1;MODE=PostgreSQL;LOCK_TIMEOUT=10000");
    try (var connection = db.getConnection();
        var statement = connection.createStatement()) {
      statement.execute(
          Files.readString(
              Path.of(
                  "../cn-universal-db-migrator/src/main/resources/db/migration/postgresql/V2026101304__video_alarm_processes.sql")));
    }
    var config =
        new Configuration(new Environment("test", new SpringManagedTransactionFactory(), db));
    config.setMapUnderscoreToCamelCase(true);
    var helper = new MapperHelper();
    helper.registerMapper(BaseMapper.class);
    config.addMapper(VideoSdkAlarmProcessMapper.class);
    config.addMapper(VideoSdkAlarmProcessMemberMapper.class);
    helper.processConfiguration(config);
    var session = new SqlSessionTemplate(new SqlSessionFactoryBuilder().build(config));
    var processes = session.getMapper(VideoSdkAlarmProcessMapper.class);
    var members = session.getMapper(VideoSdkAlarmProcessMemberMapper.class);
    return new Fixture(
        new VideoSdkAlarmProcessService(processes, members, new DataSourceTransactionManager(db)),
        processes,
        members,
        db);
  }

  private static VideoPlatformInstance instance(String tenant, String key) {
    return VideoPlatformInstance.builder()
        .tenantId(tenant)
        .instanceKey(key)
        .platformType("hik_isup")
        .build();
  }

  private static SdkVideoAlarmCallbackRequest request(
      String instance, String device, String channel, int ordinal, String state, long time) {
    var r = new SdkVideoAlarmCallbackRequest();
    r.setInstanceKey(instance);
    r.setDeviceId(device);
    r.setChannelId(channel);
    r.setAlarmType("motion");
    r.setEventId(new UUID(0, ordinal).toString());
    r.setAlarmState(state);
    r.setActivePostCount(1);
    r.setAlarmTime(time);
    return r;
  }

  private static VideoSdkAlarmReceipt receipt(
      VideoPlatformInstance i, SdkVideoAlarmCallbackRequest r, long received) {
    var v = new VideoSdkAlarmReceipt();
    v.setId(
        VideoSdkAlarmReceiptService.digest(
            i.getTenantId()
                + i.getInstanceKey()
                + r.getDeviceId()
                + r.getChannelId()
                + r.getEventId()));
    v.setTenantId(i.getTenantId());
    v.setInstanceKey(i.getInstanceKey());
    v.setDeviceId(r.getDeviceId());
    v.setChannelId(r.getChannelId());
    v.setEventId(r.getEventId());
    v.setEventKind(r.getAlarmType());
    v.setCreatedAt(received);
    return v;
  }

  @SuppressWarnings("unchecked")
  private static List<Map<String, Object>> rows(Map<String, Object> page) {
    return (List<Map<String, Object>>) page.get("rows");
  }

  private static Map<String, String> range(String device, long time) {
    return Map.of(
        "deviceId",
        device,
        "startTime",
        Long.toString(time - 1000),
        "endTime",
        Long.toString(time + 600000));
  }

  @Test
  void realCameraRepeatedStartsWithCounterOneAndNoVendorIdFormOneDurableProcess() throws Exception {
    var f = fixture();
    var instance = instance("tenant-a", "isup-a");
    long time = System.currentTimeMillis();
    String process = null;
    for (int n = 1; n <= 5; n++) {
      var request = request("isup-a", "ipc", "1", n, "start", time + n * 1000);
      var receipt = receipt(instance, request, time + n * 1000);
      String id = f.service.record(instance, receipt, request);
      if (process == null) process = id;
      assertEquals(process, id);
      assertEquals(id, f.service.record(instance, receipt, request));
    }
    var rows = rows(f.service.list(instance, range("ipc", time)));
    assertEquals(1, rows.size());
    assertEquals(5L, rows.getFirst().get("count"));
    assertEquals("active", rows.getFirst().get("state"));
    assertEquals(false, rows.getFirst().get("stopObserved"));
    var restarted =
        new VideoSdkAlarmProcessService(
            f.processes, f.members, new DataSourceTransactionManager(f.db));
    assertEquals(5L, restarted.events(instance, process, Map.of()).get("total"));
    assertTrue(
        rows(restarted.events(instance, process, Map.of())).stream()
            .allMatch(
                member ->
                    member.get("activePostCount").equals(1L) && member.get("evidenceId") != null));
  }

  @Test
  void genuineStopEndsTheProcessWhileInactivityOnlyMarksUnknownGap() throws Exception {
    var f = fixture();
    var instance = instance("t", "i");
    long time = System.currentTimeMillis() - 120000;
    var start = request("i", "ipc", "1", 1, "start", time);
    String first = f.service.record(instance, receipt(instance, start, time), start);
    var stop = request("i", "ipc", "1", 2, "stop", time + 1000);
    assertEquals(first, f.service.record(instance, receipt(instance, stop, time + 1000), stop));
    var next = request("i", "ipc", "1", 3, "start", time + 2000);
    String second = f.service.record(instance, receipt(instance, next, time + 2000), next);
    assertNotEquals(first, second);
    f.service.expire();
    var ended = f.processes.selectByPrimaryKey(first);
    assertEquals("ended", ended.getState());
    assertEquals(1, ended.getStopObserved());
    assertEquals("device_inactive", ended.getCloseReason());
    var unknown = f.processes.selectByPrimaryKey(second);
    assertEquals("unknown_gap", unknown.getState());
    assertEquals(0, unknown.getStopObserved());
    assertEquals("no_inactive_observed", unknown.getCloseReason());
    var delayed = request("i", "ipc", "1", 4, "start", time + 3000);
    assertEquals(
        second, f.service.record(instance, receipt(instance, delayed, time + 3000), delayed));
    var afterGap = request("i", "ipc", "1", 5, "start", time + 40000);
    assertNotEquals(
        second, f.service.record(instance, receipt(instance, afterGap, time + 40000), afterGap));
  }

  @Test
  void evidenceSupplementLinksByOriginalGatewayEventWithoutAddingMemberOrGuessingVendorId()
      throws Exception {
    var f = fixture();
    var instance = instance("t", "i");
    long time = System.currentTimeMillis();
    var original = request("i", "ipc", "1", 1, "start", time);
    original.setSourceEventId("vendor-motion");
    String process = f.service.record(instance, receipt(instance, original, time), original);
    var supplement = request("i", "ipc", "1", 2, "unknown", time);
    supplement.setAlarmType("evidence_ready");
    supplement.setSourceEventId(original.getEventId());
    assertEquals(
        process,
        f.service.record(instance, receipt(instance, supplement, time + 1000), supplement));
    assertEquals(1L, f.service.events(instance, process, Map.of()).get("total"));
    supplement.setSourceEventId("vendor-motion");
    assertNull(f.service.record(instance, receipt(instance, supplement, time + 1000), supplement));
    assertEquals(1L, f.processes.selectByPrimaryKey(process).getEventCount());
  }

  @Test
  void instanceTenantDeviceAndChannelRemainIsolatedAndSyntheticCallbacksDoNotBecomeMembers()
      throws Exception {
    var f = fixture();
    long time = System.currentTimeMillis();
    var a = instance("a", "i");
    var b = instance("b", "j");
    var request = request("i", "ipc", "1", 1, "start", time);
    String process = f.service.record(a, receipt(a, request, time), request);
    var another = request("j", "ipc", "1", 1, "start", time);
    assertNotEquals(process, f.service.record(b, receipt(b, another, time), another));
    another = request("i", "ipc", "2", 2, "start", time);
    assertNotEquals(process, f.service.record(a, receipt(a, another, time), another));
    assertThrows(BaseException.class, () -> f.service.events(b, process, Map.of()));
    assertThrows(
        BaseException.class, () -> f.service.record(b, receipt(a, request, time), request));
    var synthetic = request("i", "ipc", "1", 3, "start", time);
    synthetic.setSdkEventFamily("gateway");
    assertNull(f.service.record(a, receipt(a, synthetic, time), synthetic));
    synthetic.setSdkEventFamily(null);
    synthetic.setData(Map.of("acceptanceFixture", "probe"));
    assertNull(f.service.record(a, receipt(a, synthetic, time), synthetic));
    synthetic.setData(null);
    synthetic.setAlarmType("line_crossing");
    assertNull(f.service.record(a, receipt(a, synthetic, time), synthetic));
    assertEquals(1L, f.processes.selectByPrimaryKey(process).getEventCount());
    assertThrows(
        BaseException.class,
        () -> f.service.list(a, Map.of("deviceId", "ipc", "startTime", "invalid")));
  }

  @Test
  void concurrentWorkersAndRepeatedReceiptsCommitExactlyOneMemberAndCountPerIdentity()
      throws Exception {
    var f = fixture();
    var instance = instance("t", "i");
    long time = System.currentTimeMillis();
    var pool = Executors.newFixedThreadPool(8);
    try {
      var calls = new ArrayList<Callable<String>>();
      for (int n = 1; n <= 16; n++) {
        int ordinal = n;
        calls.add(
            () -> {
              var request = request("i", "ipc", "1", ordinal, "start", time + ordinal);
              return f.service.record(
                  instance, receipt(instance, request, time + ordinal), request);
            });
      }
      var first = new HashSet<String>();
      for (var future : pool.invokeAll(calls)) first.add(future.get());
      assertEquals(1, first.size());
      for (var future : pool.invokeAll(calls)) assertEquals(first.iterator().next(), future.get());
      assertEquals(16L, f.processes.selectByPrimaryKey(first.iterator().next()).getEventCount());
      assertEquals(16L, f.service.events(instance, first.iterator().next(), Map.of()).get("total"));
    } finally {
      pool.shutdownNow();
    }
  }
}
