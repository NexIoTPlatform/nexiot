package cn.universal.admin.video.alarm;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import cn.universal.persistence.entity.*;
import cn.universal.persistence.mapper.VideoSdkAlarmReceiptMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.*;
import org.junit.jupiter.api.Test;
class VideoSdkEvidenceTimelineServiceTest {
  @Test void subStreamIncludesEvidenceFromItsPhysicalAlarmChannel() throws Exception {
    var mapper=mock(VideoSdkAlarmReceiptMapper.class);var json=new ObjectMapper();
    var service=new VideoSdkEvidenceTimelineService(mapper,json);
    var instance=VideoPlatformInstance.builder().tenantId("tenant").instanceKey("isup").build();
    var request=new SdkVideoAlarmCallbackRequest();request.setSourceEventId("original");request.setAlarmTime(1700000000000L);
    request.setEvidence(List.of(Map.of("url","https://media.test/frame.jpg","kind","image","imageSource","platform_frame")));
    var receipt=new VideoSdkAlarmReceipt();receipt.setPayloadJson(json.writeValueAsString(request));
    when(mapper.evidence(eq("tenant"),eq("isup"),eq("nvr"),eq("7"),anyLong(),anyLong())).thenReturn(List.of(receipt));
    var page=Map.<String,Object>of("events",List.of(Map.of("eventId","original","deviceId","nvr","channelId","7")),"hasMore",false);

    var output=service.enrich(instance,Map.of("deviceId","nvr","channelId","7_sub","startTime",1699999990000L,"endTime",1700000010000L,"onlyEvidence",true),page);

    var events=(List<Map<String,Object>>)output.get("events");
    assertEquals(1,events.size());
    assertEquals("https://media.test/frame.jpg",((Map<?,?>)((List<?>)events.getFirst().get("images")).getFirst()).get("snapshotUrl"));
    verify(mapper).evidence(eq("tenant"),eq("isup"),eq("nvr"),eq("7"),anyLong(),anyLong());
  }
  @Test void mergesOnlyAssociatedEvidencePreservesCursorAndFiltersAfterEnrichment() throws Exception {
    var mapper=mock(VideoSdkAlarmReceiptMapper.class);var json=new ObjectMapper();var service=new VideoSdkEvidenceTimelineService(mapper,json);
    var instance=VideoPlatformInstance.builder().tenantId("tenant").instanceKey("isup").build();
    var request=new SdkVideoAlarmCallbackRequest();request.setSourceEventId("original");request.setAlarmTime(1700000000000L);
    request.setEvidence(List.of(Map.of("url","https://media.test/frame.jpg","kind","image","imageSource","platform_frame","callbackToken","hidden"),Map.ofEntries(Map.entry("url","https://media.test/clip.mp4"),Map.entry("downloadUrl","https://media.test/download/clip.mp4"),Map.entry("hlsUrl","https://media.test/clip.m3u8"),Map.entry("kind","video"),Map.entry("status","ready"),Map.entry("sizeBytes",12345),Map.entry("durationSeconds",12.03),Map.entry("actualDurationSeconds",12.03),Map.entry("requestedDurationSeconds",12),Map.entry("coverageStatus","complete"),Map.entry("taskId","real-task"),Map.entry("callbackToken","hidden"))));
    var receipt=new VideoSdkAlarmReceipt();receipt.setPayloadJson(json.writeValueAsString(request));
    when(mapper.evidence(eq("tenant"),eq("isup"),eq("nvr"),eq("7"),anyLong(),anyLong())).thenReturn(List.of(receipt));
    var page=Map.<String,Object>of("events",List.of(Map.of("eventId","original","deviceId","nvr","channelId","7"),Map.of("eventId","other")),"nextCursor","opaque","hasMore",true,"total",2);
    var output=service.enrich(instance,Map.of("deviceId","nvr","channelId","7","startTime",1699999990000L,"endTime",1700000010000L,"onlyEvidence",true),page);
    var events=(List<Map<String,Object>>)output.get("events");assertEquals(1,events.size());assertEquals("opaque",output.get("nextCursor"));assertEquals(true,output.get("hasMore"));assertFalse(output.containsKey("total"));
    assertEquals("https://media.test/frame.jpg",((Map<?,?>)((List<?>)events.getFirst().get("images")).getFirst()).get("snapshotUrl"));
    assertEquals("https://media.test/clip.mp4",((Map<?,?>)events.getFirst().get("recording")).get("url"));
    assertEquals("https://media.test/download/clip.mp4",((Map<?,?>)events.getFirst().get("recording")).get("downloadUrl"));
    assertEquals("https://media.test/clip.m3u8",((Map<?,?>)events.getFirst().get("recording")).get("hlsUrl"));
    var recording=(Map<?,?>)events.getFirst().get("recording");assertEquals(12.03,recording.get("actualDurationSeconds"));assertEquals(12,recording.get("requestedDurationSeconds"));assertEquals("complete",recording.get("coverageStatus"));assertFalse(recording.containsKey("callbackToken"));
    assertFalse(((Map<?,?>)((List<?>)events.getFirst().get("images")).getFirst()).containsKey("callbackToken"));
    verify(mapper).evidence(eq("tenant"),eq("isup"),eq("nvr"),eq("7"),anyLong(),anyLong());
  }
  @Test void preciseEvidenceAndSourceFiltersUseAndAndNeverMixTheIdentifiers() {
    var mapper=mock(VideoSdkAlarmReceiptMapper.class);var service=new VideoSdkEvidenceTimelineService(mapper,new ObjectMapper());
    var instance=VideoPlatformInstance.builder().tenantId("tenant").instanceKey("isup").build();
    when(mapper.evidence(any(),any(),any(),any(),anyLong(),anyLong())).thenReturn(List.of());
    var page=Map.<String,Object>of("events",List.of(Map.of("evidenceId","evidence-1","eventId","gateway-1","data",Map.of("sourceEventId","native-1")),Map.of("evidenceId","evidence-2","eventId","gateway-2")),"nextCursor","opaque","hasMore",true);
    var matches=service.enrich(instance,Map.of("evidenceId","evidence-1","sourceEventId","native-1"),page);
    assertEquals(1,((List<?>)matches.get("events")).size());assertEquals("opaque",matches.get("nextCursor"));
    assertEquals(0,((List<?>)service.enrich(instance,Map.of("evidenceId","native-1"),page).get("events")).size());
    assertEquals(0,((List<?>)service.enrich(instance,Map.of("evidenceId","evidence-1","sourceEventId","gateway-2"),page).get("events")).size());
  }
}
