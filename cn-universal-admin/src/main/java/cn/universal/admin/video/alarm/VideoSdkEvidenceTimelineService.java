package cn.universal.admin.video.alarm;

import cn.universal.persistence.entity.VideoPlatformInstance;
import cn.universal.persistence.mapper.VideoSdkAlarmReceiptMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** Enrich gateway event pages with backend-generated evidence without changing gateway cursors. */
@Service
@RequiredArgsConstructor
public class VideoSdkEvidenceTimelineService {
  private final VideoSdkAlarmReceiptMapper receipts;
  private final ObjectMapper json;
  public Map<String,Object> enrich(VideoPlatformInstance instance,Map<String,Object> parameters,Map<String,Object> page) {
    if(!(page.get("events") instanceof List<?> events))return page;
    long from=time(parameters.get("startTime"),System.currentTimeMillis()-86400000),to=time(parameters.get("endTime"),System.currentTimeMillis());
    // Main and sub streams share one physical alarm channel, as in the gateway timeline.
    String channel=Objects.toString(parameters.get("channelId"),"").replaceFirst("_sub$", "");
    List<cn.universal.persistence.entity.VideoSdkAlarmReceipt> stored=receipts.evidence(instance.getTenantId(),instance.getInstanceKey(),
        Objects.toString(parameters.get("deviceId"),""),channel,from-86400000,Math.max(to+600000,System.currentTimeMillis()));
    Map<String,List<Map<String,Object>>> references=new HashMap<>();
    for(var receipt:stored)try{
      var request=json.readValue(receipt.getPayloadJson(),SdkVideoAlarmCallbackRequest.class);
      if(request.getSourceEventId()==null||request.getAlarmTime()==null||request.getAlarmTime()<from||request.getAlarmTime()>to)continue;
      if(request.getEvidence() instanceof List<?> list)for(Object value:list)if(value instanceof Map<?,?> raw){
        Map<String,Object> reference=new LinkedHashMap<>();
        for(String key:List.of("url","downloadUrl","mp4Url","hlsUrl","kind","status","taskId","imageSource","capturedAt","sizeBytes","contentType","durationSeconds","actualDurationSeconds","requestedDurationSeconds","coverageStatus"))
          if(raw.containsKey(key))reference.put(key,raw.get(key));
        if(reference.get("url") instanceof String url&&!url.isBlank())references.computeIfAbsent(request.getSourceEventId(),ignored->new ArrayList<>()).add(reference);
      }
    }catch(Exception ignored){/* An unreadable evidence row does not hide the original gateway event. */}
    List<Map<String,Object>> result=new ArrayList<>();
    for(Object value:events)if(value instanceof Map<?,?> raw){
      Map<String,Object> event=new LinkedHashMap<>();raw.forEach((k,v)->{if(k instanceof String key)event.put(key,v);});
      Map<?,?> data=event.get("data") instanceof Map<?,?> m?m:Map.of();
      Set<String> ids=new HashSet<>();for(Object id:Arrays.asList(event.get("eventId"),event.get("sourceEventId"),data.get("eventId"),data.get("sourceEventId")))if(id instanceof String s)ids.add(s);
      String evidenceId=Objects.toString(parameters.get("evidenceId"),"").trim();
      String sourceEventId=Objects.toString(parameters.get("sourceEventId"),"").trim();
      // Match the gateway's precise AND filters; never treat a source ID as an evidence ID.
      if(!evidenceId.isEmpty()&&!evidenceId.equals(event.get("evidenceId")))continue;
      if(!sourceEventId.isEmpty()&&!ids.contains(sourceEventId))continue;
      List<Map<String,Object>> images=new ArrayList<>();if(event.get("images") instanceof List<?> list)for(Object image:list)if(image instanceof Map<?,?> m){Map<String,Object> copy=new LinkedHashMap<>();m.forEach((k,v)->{if(k instanceof String key)copy.put(key,v);});images.add(copy);}
      Set<String> urls=new HashSet<>();for(var image:images)urls.add(Objects.toString(image.getOrDefault("snapshotUrl",image.get("url")),""));
      boolean recordingAdded=false;
      for(String id:ids)for(var reference:references.getOrDefault(id,List.of())) {
        String url=Objects.toString(reference.get("url"));
        if("video".equals(reference.get("kind"))) { if(!recordingAdded){event.put("recording",reference);recordingAdded=true;} }
        else if(urls.add(url)){var image=new LinkedHashMap<>(reference);image.put("snapshotUrl",url);images.add(image);}
      }
      if(!images.isEmpty()){event.put("images",images);event.put("imageStatus","stored");}
      if(!Boolean.TRUE.equals(parameters.get("onlyEvidence"))||!images.isEmpty()||event.get("recording") instanceof Map<?,?> m&&m.get("url")!=null)result.add(event);
    }
    Map<String,Object> response=new LinkedHashMap<>(page);response.put("events",result);response.put("backendEvidenceIncluded",true);
    response.put("backendEvidenceTruncated",stored.size()==1000);if(Boolean.TRUE.equals(parameters.get("onlyEvidence")))response.remove("total");return response;
  }
  private static long time(Object value,long fallback){if(value==null)return fallback;if(value instanceof Number n){long v=n.longValue();return v<100000000000L?v*1000:v;}try{return java.time.LocalDateTime.parse(value.toString(),java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")).atZone(java.time.ZoneId.of("Asia/Shanghai")).toInstant().toEpochMilli();}catch(Exception invalid){return fallback;}}
}
