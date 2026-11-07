package cn.universal.admin.video.alarm;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import cn.universal.dm.video.VideoSdkEventCatalog;
import cn.universal.persistence.common.BaseMapper;
import cn.universal.persistence.entity.VideoPlatformInstance;
import cn.universal.persistence.mapper.VideoSdkAlarmReceiptMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.*;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.ObjectProvider;
import tk.mybatis.mapper.mapperhelper.MapperHelper;

class VideoSdkAlarmReceiptServiceTest {
  @Test void compactPayloadPreservesReplayFieldsAndRawOnlyCatalogMetadataWithoutMutatingCaller() throws Exception {
    var mapper = springMapper(database());
    var json = new ObjectMapper();
    var instance = VideoPlatformInstance.builder().tenantId("tenant").instanceKey("isup").build();
    var request = new SdkVideoAlarmCallbackRequest();
    request.setInstanceKey("isup"); request.setPlatformType("hik_isup"); request.setVendor("hikvision");
    request.setDeviceId("ipc"); request.setDeviceName("camera"); request.setDeviceIp("192.0.2.1");
    request.setChannelId("7"); request.setChannelName("entrance"); request.setAlarmType("unknown");
    request.setAlarmName("motion"); request.setAlarmState("start"); request.setAlarmLevel("1");
    request.setDescription("detected movement"); request.setAlarmTime(1792131225132L);
    request.setEventId("stable-event"); request.setSourceEventId("original-event"); request.setSourceEventType("motion");
    request.setImageStatus("ready"); request.setPictureCount(1); request.setActivePostCount(2);
    request.setImages(List.of(Map.of("url", "https://media.test/image.jpg", "status", "stored")));
    request.setEvidence(List.of(Map.of("url", "https://media.test/clip.mp4", "status", "ready", "kind", "video")));
    var data = new LinkedHashMap<String, Object>();
    data.put("score", 0.95); data.put("vendorAlarmType", null); data.put("emptyWins", "");
    data.put("analysis", Map.of("objectCount", 2, "password", "data-secret"));
    request.setData(data);
    var raw = new LinkedHashMap<String, Object>();
    raw.put("vendorEventCode", 13); raw.put("cameraEventType", "VMD"); raw.put("sdkEventFamily", "ISUP");
    raw.put("vendorAlarmType", 8); raw.put("score", 0.3); raw.put("emptyWins", "fallback");
    raw.put("images", List.of(Map.of("url", "https://media.test/raw-image.jpg", "status", "stored", "token", "image-secret")));
    raw.put("rawOnly", Map.of("ruleId", "zone-1", "token", "nested-secret")); raw.put("token", "callback-secret");
    request.setRaw(raw);
    var original = json.valueToTree(request);
    var provider = mock(ObjectProvider.class);
    var publisher = mock(SdkVideoAlarmUpService.class);
    when(provider.getObject()).thenReturn(publisher);
    when(publisher.publishReceipt(any(), any())).thenReturn(Map.of("ruleDispatched", true));
    String receiptId;
    var accepting = new VideoSdkAlarmReceiptService(mapper, json, provider);
    try {
      receiptId = (String) accepting.accept(instance, request).get("receiptId");
      var persisted = json.readTree(mapper.selectByPrimaryKey(receiptId).getPayloadJson());
      assertTrue(persisted.get("raw").isNull());
      assertEquals(13, persisted.at("/data/vendorEventCode").intValue());
      assertEquals("VMD", persisted.at("/data/cameraEventType").textValue());
      assertEquals(0.95, persisted.at("/data/score").doubleValue());
      assertTrue(persisted.at("/data/vendorAlarmType").isNull());
      assertEquals("", persisted.at("/data/emptyWins").textValue());
      assertFalse(persisted.get("data").has("token"));
      assertEquals(Map.of("ruleId", "zone-1"), json.convertValue(persisted.at("/data/rawOnly"), Map.class));
      assertEquals(Map.of("objectCount", 2), json.convertValue(persisted.at("/data/analysis"), Map.class));
      assertEquals("https://media.test/raw-image.jpg", persisted.at("/data/images/0/url").textValue());
      assertFalse(persisted.at("/data/images/0").has("token"));
      var originalFields = ((com.fasterxml.jackson.databind.node.ObjectNode) original).deepCopy();
      var persistedFields = ((com.fasterxml.jackson.databind.node.ObjectNode) persisted).deepCopy();
      originalFields.remove(List.of("data", "raw")); persistedFields.remove(List.of("data", "raw"));
      assertEquals(originalFields, persistedFields);
      assertSame(data, request.getData()); assertSame(raw, request.getRaw());
      assertEquals(original, json.valueToTree(request));
    } finally { accepting.stop(); }

    var restarted = new VideoSdkAlarmReceiptService(mapper, json, provider);
    try {
      restarted.process(receiptId);
      var replayed = ArgumentCaptor.forClass(SdkVideoAlarmCallbackRequest.class);
      verify(publisher).publishReceipt(replayed.capture(), argThat(receipt -> receiptId.equals(receipt.getId())));
      assertNull(replayed.getValue().getRaw());
      assertEquals(request.getImages(), replayed.getValue().getImages());
      assertEquals(request.getEvidence(), replayed.getValue().getEvidence());
      assertEquals("ISUP", replayed.getValue().getData().get("sdkEventFamily"));
      assertEquals(List.of(Map.of("url", "https://media.test/raw-image.jpg", "status", "stored")), replayed.getValue().getData().get("images"));
      assertEquals("motionDetection", isupCatalog(json).resolve("hik_isup", replayed.getValue().getAlarmType(), replayed.getValue().getData()).id());
      assertEquals("done", mapper.selectByPrimaryKey(receiptId).getStatus());
      assertEquals(1, mapper.selectByPrimaryKey(receiptId).getFlowDispatched());
    } finally { restarted.stop(); }
  }

  private VideoSdkEventCatalog isupCatalog(ObjectMapper json) {
    var types = mock(cn.universal.persistence.mapper.admin.SysDictTypeMapper.class);
    var data = mock(cn.universal.persistence.mapper.admin.SysDictDataMapper.class);
    var type = new cn.universal.persistence.entity.admin.SysDictType(); type.setStatus("0");
    when(types.selectOne(any())).thenReturn(type);
    when(data.selectDictDataByType("video_sdk_event_hik_sdk")).thenReturn(List.of(
        new cn.universal.persistence.entity.admin.SysDictData().setDictCode(1L).setDictSort(1)
            .setDictValue("0x4000:3").setDictLabel("移动侦测").setStatus("0")
            .setRemark("{\"type\":\"motion\",\"eventId\":\"motionDetection\"}")));
    return new VideoSdkEventCatalog(data, types, json);
  }

  private JdbcDataSource database() throws Exception {
    var db=new JdbcDataSource();db.setURL("jdbc:h2:mem:frame_"+UUID.randomUUID()+";DB_CLOSE_DELAY=-1;MODE=PostgreSQL;LOCK_TIMEOUT=10000");
    try(var connection=db.getConnection();var statement=connection.createStatement()) {
      statement.execute(Files.readString(Path.of("../cn-universal-db-migrator/src/main/resources/db/migration/postgresql/V2026101302__video_sdk_alarm_receipt.sql")));
    }
    return db;
  }
  private VideoSdkAlarmReceiptMapper springMapper(JdbcDataSource db) {
    var config=new Configuration(new Environment("test",new org.mybatis.spring.transaction.SpringManagedTransactionFactory(),db));
    config.setMapUnderscoreToCamelCase(true);var helper=new MapperHelper();helper.registerMapper(BaseMapper.class);
    config.addMapper(VideoSdkAlarmReceiptMapper.class);helper.processConfiguration(config);
    var template=new org.mybatis.spring.SqlSessionTemplate(new SqlSessionFactoryBuilder().build(config));
    return template.getMapper(VideoSdkAlarmReceiptMapper.class);
  }
  private cn.universal.persistence.entity.VideoSdkAlarmReceipt nativeReceipt(String id,String tenant,String instance,String device,String channel,String kind) {
    var receipt=new cn.universal.persistence.entity.VideoSdkAlarmReceipt();receipt.setId(id);receipt.setTenantId(tenant);receipt.setInstanceKey(instance);
    receipt.setDeviceId(device);receipt.setChannelId(channel);receipt.setEventKind(kind);return receipt;
  }
  @Test void independentImageBudgetHasPersistentScopeAndRetryOwnershipWithoutChangingSharedCooldown() throws Exception {
    var db=database();var mapper=springMapper(db);var service=new VideoSdkAlarmReceiptService(mapper,new ObjectMapper(),mock(ObjectProvider.class));
    try {
      var first=nativeReceipt("first","tenant","instance","ipc","1","motion");
      var next=nativeReceipt("next","tenant","instance","ipc","1","motion");
      assertFalse(service.cooledDown(first,60));assertFalse(service.frameCooledDown(first,60));
      assertTrue(service.cooledDown(next,60));assertTrue(service.frameCooledDown(next,60));
      assertFalse(service.frameCooledDown(first,60)); // retry after failed enqueue owns the existing frame slot
      var restarted=new VideoSdkAlarmReceiptService(mapper,new ObjectMapper(),mock(ObjectProvider.class));
      try {assertTrue(restarted.frameCooledDown(next,60));assertFalse(restarted.frameCooledDown(first,60));}finally {restarted.stop();}
      for(var scope:List.of(
          nativeReceipt("tenant-other","other","instance","ipc","1","motion"),
          nativeReceipt("instance-other","tenant","other","ipc","1","motion"),
          nativeReceipt("device-other","tenant","instance","other","1","motion"),
          nativeReceipt("channel-other","tenant","instance","ipc","2","motion"),
          nativeReceipt("kind-other","tenant","instance","ipc","1","tamper")))assertFalse(service.frameCooledDown(scope,60));
      try(var connection=db.getConnection();var statement=connection.createStatement();var rows=statement.executeQuery("SELECT id FROM video_sdk_alarm_receipt WHERE event_id='__frame_cooldown__'")) {
        int count=0;while(rows.next()){count++;assertTrue(rows.getString(1).matches("f:[a-f0-9]{62}"));}assertEquals(6,count);
      }
      var instance=VideoPlatformInstance.builder().tenantId("tenant").instanceKey("instance").build();
      var request=new SdkVideoAlarmCallbackRequest();request.setDeviceId("ipc");request.setChannelId("1");request.setAlarmType("motion");request.setEventId("__frame_cooldown__");
      String callback=(String)service.accept(instance,request).get("receiptId");assertTrue(callback.matches("[a-f0-9]{64}"));
      try(var connection=db.getConnection();var statement=connection.prepareStatement("UPDATE video_sdk_alarm_receipt SET updated_at=? WHERE event_id='__frame_cooldown__'")) {
        statement.setLong(1,System.currentTimeMillis()-61000);statement.executeUpdate();
      }
      assertFalse(service.frameCooledDown(next,60));assertTrue(service.cooledDown(next,60)); // independent guard expiry
      assertThrows(IllegalArgumentException.class,()->service.frameCooledDown(first,0));
      assertThrows(IllegalArgumentException.class,()->service.frameCooledDown(first,3601));
    }finally {service.stop();}
  }
  @Test void concurrentFirstImageReservationCreatesOneGuardAndOnlyOneOwner() throws Exception {
    var db=database();var mapper=springMapper(db);var service=new VideoSdkAlarmReceiptService(mapper,new ObjectMapper(),mock(ObjectProvider.class));
    var workers=Executors.newFixedThreadPool(12);var start=new CountDownLatch(1);
    try {
      var futures=new ArrayList<Future<Boolean>>();
      for(int i=0;i<12;i++) {
        var receipt=nativeReceipt("concurrent-"+i,"tenant","instance","ipc","1","motion");
        futures.add(workers.submit(()->{start.await();return service.frameCooledDown(receipt,60);}));
      }
      start.countDown();int allowed=0;for(var future:futures)if(!future.get(20,TimeUnit.SECONDS))allowed++;assertEquals(1,allowed);
      try(var connection=db.getConnection();var statement=connection.createStatement();var rows=statement.executeQuery("SELECT count(*) FROM video_sdk_alarm_receipt WHERE event_id='__frame_cooldown__'")) {
        assertTrue(rows.next());assertEquals(1,rows.getInt(1));
      }
    }finally {workers.shutdownNow();service.stop();}
  }
  @Test void persistedDuplicateIdentitySurvivesServiceRestartAndPreservesEvidenceVersions() throws Exception {
    JdbcDataSource db = new JdbcDataSource();
    db.setURL("jdbc:h2:mem:receipt_"+UUID.randomUUID()+";DB_CLOSE_DELAY=-1;MODE=PostgreSQL");
    try (var c=db.getConnection();var s=c.createStatement()) {
      s.execute(Files.readString(Path.of("../cn-universal-db-migrator/src/main/resources/db/migration/postgresql/V2026101302__video_sdk_alarm_receipt.sql")));
    }
    var config = new Configuration(new Environment("test", new JdbcTransactionFactory(), db));
    config.setMapUnderscoreToCamelCase(true);
    MapperHelper helper=new MapperHelper(); helper.registerMapper(BaseMapper.class);
    config.addMapper(VideoSdkAlarmReceiptMapper.class); helper.processConfiguration(config);
    SqlSessionFactory factory=new SqlSessionFactoryBuilder().build(config);
    var instance=VideoPlatformInstance.builder().tenantId("t").instanceKey("isup").build();
    var request=new SdkVideoAlarmCallbackRequest(); request.setInstanceKey("isup");request.setDeviceId("ipc");request.setChannelId("1");
    request.setEventId("stable");request.setAlarmType("motion");
    String receiptId;
    try(var session=factory.openSession(true)) {
      var service=new VideoSdkAlarmReceiptService(session.getMapper(VideoSdkAlarmReceiptMapper.class),new ObjectMapper(),mock(ObjectProvider.class));
      try {receiptId=(String)service.accept(instance,request).get("receiptId");assertEquals(true,service.accept(instance,request).get("duplicate"));}
      finally{service.stop();}
    }
    try(var session=factory.openSession(true)) {
      var mapper=session.getMapper(VideoSdkAlarmReceiptMapper.class);
      var restarted=new VideoSdkAlarmReceiptService(mapper,new ObjectMapper(),mock(ObjectProvider.class));
      try {
        assertEquals(receiptId,restarted.accept(instance,request).get("receiptId"));
        assertEquals(1,mapper.claim(receiptId,"a",1,1000));assertEquals(0,mapper.claim(receiptId,"b",2,1000));
        assertEquals(1,mapper.claim(receiptId,"b",1001,2000));
        var receipt=mapper.selectByPrimaryKey(receiptId);
        assertFalse(restarted.cooledDown(receipt,30));
        request.setEventId("second");var second=(String)restarted.accept(instance,request).get("receiptId");
        assertTrue(restarted.cooledDown(mapper.selectByPrimaryKey(second),30));
        assertFalse(restarted.cooledDown(receipt,30)); // retries retain their acquired dispatch slot
        request.setEventId("stable");request.setActivePostCount(2);
        String continuous=(String)restarted.accept(instance,request).get("receiptId");
        assertNotEquals(receiptId,continuous);assertEquals(true,restarted.accept(instance,request).get("duplicate"));
        request.setAlarmType("evidence_ready"); request.setSourceEventId("stable");request.setImages(List.of(Map.of("url","one")));
        String firstImage=(String)restarted.accept(instance,request).get("receiptId");
        assertNotEquals(receiptId,firstImage);
        request.setImages(List.of(Map.of("url","one"),Map.of("url","two")));
        assertNotEquals(firstImage,restarted.accept(instance,request).get("receiptId"));
      } finally{restarted.stop();}
    }
  }
}
