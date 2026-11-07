package cn.universal.admin.video.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import cn.universal.common.exception.BaseException;
import cn.universal.dm.video.VideoPlatformAdapter;
import cn.universal.dm.video.VideoPlatformAdapterRegistry;
import cn.universal.persistence.entity.*;
import cn.universal.persistence.mapper.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class VideoSdkRecordingTaskServiceTest {
  final VideoSdkRecordTaskMapper tasks = mock(VideoSdkRecordTaskMapper.class);
  final VideoPlatformInstanceMapper instances = mock(VideoPlatformInstanceMapper.class);
  final VideoPlatformDeviceMapper devices = mock(VideoPlatformDeviceMapper.class);
  final VideoPlatformChannelMapper channels = mock(VideoPlatformChannelMapper.class);
  final VideoPlatformAdapterRegistry adapters = mock(VideoPlatformAdapterRegistry.class);
  final VideoPlatformAdapter adapter = mock(VideoPlatformAdapter.class);
  final VideoSdkRecordingTaskService service = new VideoSdkRecordingTaskService(tasks, instances, devices, channels, adapters, new ObjectMapper());
  final String id = UUID.randomUUID().toString();
  VideoPlatformInstance instance() {
    var i = new VideoPlatformInstance(); i.setInstanceKey("sdk"); i.setTenantId("tenant"); i.setEnabled(1); i.setPlatformType("hik_sdk"); return i;
  }
  VideoSdkRecordTask task() {
    var t = new VideoSdkRecordTask(); t.setId(id); t.setTenantId("tenant"); t.setInstanceKey("sdk");
    t.setDeviceId("camera"); t.setChannelId("1"); t.setMode("export"); t.setStartTime(1700000000000L); t.setEndTime(1700000030000L);
    t.setStatus("pending"); t.setProgress(0); t.setCreatedAt(System.currentTimeMillis()); t.setCancelRequested(0); t.setNextPollAt(0L);
    return t;
  }
  Map<String, Object> input() { return Map.of("requestId", id, "deviceId", "camera", "channelId", "1", "startTime",1700000000000L,"endTime",1700000030000L); }
  void pollSetup(VideoSdkRecordTask t) {
    when(tasks.selectByPrimaryKey(id)).thenReturn(t);
    when(tasks.claim(eq(id), anyString(), anyLong(), anyLong())).thenReturn(1);
    when(instances.selectOne(any())).thenReturn(instance());
    when(adapters.getAdapter("hik_sdk")).thenReturn(adapter);
    when(adapters.isSupported("hik_sdk")).thenReturn(true);
  }
  @AfterEach void close() { service.stop(); }
  @Test void deduplicatesAndRejectsForeignScopeOrChangedRange() {
    var t = task(); when(tasks.selectByPrimaryKey(id)).thenReturn(t);
    assertEquals(id, service.create(instance(), input(), "u").get("id"));
    verify(tasks, never()).insert(any());
    t.setEndTime(t.getEndTime()+1000);
    assertThrows(BaseException.class, () -> service.create(instance(), input(), "u"));
    t.setTenantId("other");
    assertThrows(BaseException.class, () -> service.get(instance(), id));
    assertThrows(BaseException.class, () -> service.cancel(instance(), id));
  }
  @Test void checksRaceDuplicateParametersAgain() {
    var t=task(); t.setEndTime(t.getEndTime()+1000);
    when(tasks.selectByPrimaryKey(id)).thenReturn(null,t);
    when(devices.selectOne(any())).thenReturn(new VideoPlatformDevice()); when(channels.selectCount(any())).thenReturn(1);
    doThrow(new org.springframework.dao.DuplicateKeyException("duplicate")).when(tasks).insert(any());
    assertThrows(BaseException.class, () -> service.create(instance(), input(), "u"));
  }
  @Test void cancellationBeforeDispatchNeverTouchesDevice() {
    var t=task(); t.setCancelRequested(1); pollSetup(t);
    service.poll(id);
    assertEquals("cancelled",t.getStatus()); verifyNoInteractions(adapter);
    verify(tasks).finishPoll(eq(t),anyString());
  }
  @Test void anotherNodeHoldingLeasePreventsDeviceCall() {
    var t=task(); pollSetup(t);
    when(tasks.claim(eq(id), anyString(), anyLong(), anyLong())).thenReturn(0);
    service.poll(id); verifyNoInteractions(adapter);
    verify(tasks,never()).finishPoll(any(),anyString());
  }
  @Test void unsupportedNodeNeverClaimsOrChangesAnotherNodesTask() {
    var t=task(); t.setMessage("正在下载"); t.setGatewayJobId(id); pollSetup(t);
    var uniview=instance(); uniview.setPlatformType("uniview_sdk");
    when(instances.selectOne(any())).thenReturn(uniview);
    when(adapters.isSupported("uniview_sdk")).thenReturn(false);

    service.poll(id);

    assertEquals("pending",t.getStatus()); assertEquals("正在下载",t.getMessage());
    verify(tasks,never()).claim(anyString(),anyString(),anyLong(),anyLong());
    verify(tasks,never()).finishPoll(any(),anyString());
    verify(tasks,never()).markDispatch(anyString(),anyString()); verifyNoInteractions(adapter);
  }
  @Test void supportedUniviewNodeStartsPollsAndCancelsRecording() {
    var t=task(); pollSetup(t);
    var uniview=instance(); uniview.setPlatformType("uniview_sdk");
    when(instances.selectOne(any())).thenReturn(uniview);
    when(adapters.isSupported("uniview_sdk")).thenReturn(true);
    when(adapters.getAdapter("uniview_sdk")).thenReturn(adapter);
    when(adapter.executeOperation(any(),eq("recordingStart"),any())).thenReturn(Map.of("status","queued"));
    when(adapter.executeOperation(any(),eq("recordingStatus"),any())).thenReturn(Map.of("status","downloading","progress",21));
    when(adapter.executeOperation(any(),eq("recordingCancel"),any())).thenReturn(Map.of("status","cancelled"));

    service.poll(id); assertEquals("queued",t.getStatus()); assertEquals(id,t.getGatewayJobId());
    service.poll(id); assertEquals("downloading",t.getStatus()); assertEquals(21,t.getProgress());
    t.setCancelRequested(1); service.poll(id); assertEquals("cancelled",t.getStatus());
    verify(adapter).executeOperation(eq(uniview),eq("recordingStart"),argThat(p -> id.equals(p.get("requestId"))));
    verify(adapter).executeOperation(eq(uniview),eq("recordingStatus"),any());
    verify(adapter).executeOperation(eq(uniview),eq("recordingCancel"),any());
    verify(tasks,times(3)).finishPoll(eq(t),anyString());
  }
  @Test void uncertainSubmissionUsesSameRequestIdOnRetry() {
    var t=task(); t.setGatewayJobId(id); pollSetup(t);
    when(adapter.executeOperation(any(),eq("recordingStatus"),any())).thenThrow(new IllegalStateException("录像任务不存在"));
    when(adapter.executeOperation(any(),eq("recordingStart"),any())).thenReturn(Map.of("status","queued","progress",0));
    service.poll(id);
    verify(adapter).executeOperation(any(),eq("recordingStart"),argThat(p -> id.equals(p.get("requestId"))));
    assertEquals("queued",t.getStatus());
  }
  @Test void failedGatewayResultKeepsDiagnosticCodeAndUnknownCoverageWithoutRedispatch() throws Exception {
    var t=task(); t.setGatewayJobId(id); t.setStatus("downloading"); pollSetup(t);
    String message="设备录像连续 90 秒未收到新媒体，请检查设备或缩短录像区间后重试";
    when(adapter.executeOperation(any(),eq("recordingStatus"),any())).thenReturn(Map.of(
        "status","failed","progress",54,"code","RECORDING_EOF_TIMEOUT","message",message,
        "requestedDurationSeconds",30,"coverageStatus","unknown"));

    service.poll(id);

    assertEquals("failed",t.getStatus()); assertEquals(message,t.getMessage());
    var json=new ObjectMapper(); var stored=json.readTree(t.getResultJson());
    assertEquals("failed",stored.path("status").asText());
    assertEquals("RECORDING_EOF_TIMEOUT",stored.path("code").asText());
    assertEquals(message,stored.path("message").asText());
    assertEquals("unknown",stored.path("coverageStatus").asText());
    verify(tasks).finishPoll(eq(t),anyString());

    // Read a detached persisted row so the API contract cannot depend on the poll's object identity.
    var persisted=json.readValue(json.writeValueAsString(t),VideoSdkRecordTask.class);
    when(tasks.selectByPrimaryKey(id)).thenReturn(persisted);
    var view=service.get(instance(),id);
    assertEquals("failed",view.get("status")); assertEquals(54,view.get("progress"));
    assertEquals("RECORDING_EOF_TIMEOUT",view.get("code")); assertEquals(message,view.get("message"));
    assertEquals("unknown",view.get("coverageStatus")); assertEquals(30,view.get("requestedDurationSeconds"));
    assertFalse(view.containsKey("url"));

    service.poll(id);
    verify(adapter,times(1)).executeOperation(any(),eq("recordingStatus"),any());
    verifyNoMoreInteractions(adapter); verify(tasks,never()).markDispatch(anyString(),anyString());
  }
  @Test void deletedInstanceTerminatesInsteadOfRetryingForever() {
    var t=task(); pollSetup(t); when(instances.selectOne(any())).thenReturn(null);
    service.poll(id); assertEquals("failed",t.getStatus()); verifyNoInteractions(adapter);
  }
  @Test void validatesRangeAndDisabledInstance() {
    var data=new HashMap<>(input()); data.put("endTime",1700008000000L);
    assertThrows(BaseException.class, () -> service.create(instance(),data,"u"));
    var i=instance(); i.setEnabled(0);
    assertThrows(BaseException.class, () -> service.create(i,input(),"u"));
    verify(tasks, never()).insert(any());
  }
  @Test void platformRecordingUsesDurationAndDurableIdWithoutRecomputingStart() {
    var i=instance();i.setPlatformType("hik_isup");var t=task();t.setMode("platform");when(tasks.selectByPrimaryKey(id)).thenReturn(t);
    var request=Map.<String,Object>of("requestId",id,"deviceId","camera","channelId","1","mode","platform","durationSeconds",30);
    assertEquals(t.getStartTime(),service.create(i,request,"u").get("startTime"));
    assertThrows(BaseException.class,()->service.create(i,Map.of("requestId",id,"deviceId","camera","channelId","1","mode","platform","durationSeconds",31),"u"));
  }
  @Test void readyAlarmClipRemainsSchedulableUntilEvidenceReceiptIsDurable() throws Exception {
    var t=task();t.setGatewayJobId(id);t.setResultJson("{\"sourceEventId\":\"alarm\",\"alarmTime\":1700000000000}");pollSetup(t);
    var publisher=mock(cn.universal.admin.video.alarm.SdkVideoAlarmUpService.class);
    var provider=mock(org.springframework.beans.factory.ObjectProvider.class);when(provider.getIfAvailable()).thenReturn(publisher);
    org.springframework.test.util.ReflectionTestUtils.setField(service,"alarmPublisher",provider);
    when(adapter.executeOperation(any(),eq("recordingStatus"),any())).thenReturn(Map.of("status","ready","url","https://media.test/clip.mp4","progress",100));
    doThrow(new BaseException("inbox unavailable")).doNothing().when(publisher).publishRecordingEvidence(any(),any(),anyMap(),anyMap());
    service.poll(id);assertEquals("awaiting_evidence",t.getStatus());assertFalse(new ObjectMapper().readTree(t.getResultJson()).path("evidencePublished").asBoolean());
    service.poll(id);assertEquals("ready",t.getStatus());assertTrue(new ObjectMapper().readTree(t.getResultJson()).path("evidencePublished").asBoolean());
    verify(publisher,times(2)).publishRecordingEvidence(any(),any(),anyMap(),anyMap());verify(adapter,never()).executeOperation(any(),eq("recordingStart"),any());
  }
  @Test void shortAlarmClipKeepsPartialAfterDurableEvidenceAndDoesNotPollAgain() throws Exception {
    var t=task();t.setGatewayJobId(id);t.setResultJson("{\"sourceEventId\":\"alarm\",\"alarmTime\":1700000000000}");pollSetup(t);
    var publisher=mock(cn.universal.admin.video.alarm.SdkVideoAlarmUpService.class);
    var provider=mock(org.springframework.beans.factory.ObjectProvider.class);when(provider.getIfAvailable()).thenReturn(publisher);
    org.springframework.test.util.ReflectionTestUtils.setField(service,"alarmPublisher",provider);
    when(adapter.executeOperation(any(),eq("recordingStatus"),any())).thenReturn(Map.of("status","partial","url","https://media.test/short.mp4","progress",100,"actualDurationSeconds",6.842,"requestedDurationSeconds",8,"coverageStatus","partial"));
    doThrow(new BaseException("inbox unavailable")).doNothing().when(publisher).publishRecordingEvidence(any(),any(),anyMap(),anyMap());
    service.poll(id);assertEquals("awaiting_evidence",t.getStatus());
    service.poll(id);assertEquals("partial",t.getStatus());
    var view=service.get(instance(),id);assertEquals(6.842,view.get("actualDurationSeconds"));assertEquals("partial",view.get("coverageStatus"));
    service.poll(id);verify(adapter,times(2)).executeOperation(any(),eq("recordingStatus"),any());verify(publisher,times(2)).publishRecordingEvidence(any(),any(),anyMap(),anyMap());
  }
  @Test void partialRecordingIsTerminalInActualMapperAndCanBeRetried() throws Exception {
    var db=new org.h2.jdbcx.JdbcDataSource();db.setURL("jdbc:h2:mem:recording_"+UUID.randomUUID()+";MODE=PostgreSQL;DB_CLOSE_DELAY=-1");
    String seed=java.nio.file.Files.readString(java.nio.file.Path.of("../docker/database/postgres/001-schema.sql"));
    int start=seed.indexOf("CREATE TABLE unix_tenant_202605.video_sdk_record_task (");int end=seed.indexOf(";",start);
    try(var connection=db.getConnection();var statement=connection.createStatement()){statement.execute(seed.substring(start,end+1).replace("unix_tenant_202605.",""));}
    var config=new org.apache.ibatis.session.Configuration(new org.apache.ibatis.mapping.Environment("test",new org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory(),db));
    config.setMapUnderscoreToCamelCase(true);var helper=new tk.mybatis.mapper.mapperhelper.MapperHelper();helper.registerMapper(cn.universal.persistence.common.BaseMapper.class);config.addMapper(VideoSdkRecordTaskMapper.class);helper.processConfiguration(config);
    try(var session=new org.apache.ibatis.session.SqlSessionFactoryBuilder().build(config).openSession(true)){
      var mapper=session.getMapper(VideoSdkRecordTaskMapper.class);var t=task();t.setStatus("partial");t.setUpdatedAt(System.currentTimeMillis());t.setLeaseUntil(0L);t.setLeaseOwner("");t.setTaskName("short");mapper.insert(t);
      assertEquals(0,mapper.active("tenant","sdk"));assertTrue(mapper.due(System.currentTimeMillis()).isEmpty());assertEquals(0,mapper.requestCancel(id,"tenant","sdk"));
      when(tasks.selectByPrimaryKey(id)).thenReturn(t);when(devices.selectOne(any())).thenReturn(new VideoPlatformDevice());when(channels.selectCount(any())).thenReturn(1);
      var retried=service.retry(instance(),id,UUID.randomUUID().toString(),"u");assertNotEquals(id,retried.get("id"));verify(tasks).insert(any());
    }
  }
  @Test void capableQueueFiltersBeforeLimitAndStillTerminatesDeletedInstances() throws Exception {
    var db=new org.h2.jdbcx.JdbcDataSource();db.setURL("jdbc:h2:mem:recording_capability_"+UUID.randomUUID()+";MODE=PostgreSQL;DB_CLOSE_DELAY=-1");
    String seed=java.nio.file.Files.readString(java.nio.file.Path.of("../docker/database/postgres/001-schema.sql"));
    int start=seed.indexOf("CREATE TABLE unix_tenant_202605.video_sdk_record_task (");int end=seed.indexOf(";",start);
    try(var connection=db.getConnection();var statement=connection.createStatement()){
      statement.execute(seed.substring(start,end+1).replace("unix_tenant_202605.",""));
      statement.execute("CREATE TABLE video_platform_instance (instance_key VARCHAR(100),platform_type VARCHAR(40))");
      statement.execute("INSERT INTO video_platform_instance VALUES ('uniview','uniview_sdk'),('hik','hik_sdk')");
    }
    var config=new org.apache.ibatis.session.Configuration(new org.apache.ibatis.mapping.Environment("test",new org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory(),db));
    config.setMapUnderscoreToCamelCase(true);var helper=new tk.mybatis.mapper.mapperhelper.MapperHelper();helper.registerMapper(cn.universal.persistence.common.BaseMapper.class);config.addMapper(VideoSdkRecordTaskMapper.class);helper.processConfiguration(config);
    try(var session=new org.apache.ibatis.session.SqlSessionFactoryBuilder().build(config).openSession(true)){
      var mapper=session.getMapper(VideoSdkRecordTaskMapper.class);
      for(int n=0;n<22;n++){
        var t=task();t.setId(UUID.randomUUID().toString());t.setInstanceKey(n<20?"uniview":n==20?"hik":"deleted");
        t.setCreatedAt((long)n);t.setUpdatedAt(1L);t.setLeaseUntil(0L);t.setLeaseOwner("");t.setTaskName("recording");mapper.insert(t);
      }
      var hik=mapper.dueForPlatforms(System.currentTimeMillis(),Set.of("hik_sdk"));
      assertEquals(List.of("hik","deleted"),hik.stream().map(VideoSdkRecordTask::getInstanceKey).toList());
      assertEquals(List.of("deleted"),mapper.dueForPlatforms(System.currentTimeMillis(),Set.of()).stream().map(VideoSdkRecordTask::getInstanceKey).toList());
      var uniview=mapper.dueForPlatforms(System.currentTimeMillis(),Set.of("uniview_sdk"));
      assertEquals(16,uniview.size());assertTrue(uniview.stream().allMatch(t -> "uniview".equals(t.getInstanceKey())));
    }
  }

}
