package cn.universal.admin.video.service;

import cn.hutool.core.util.IdUtil;
import cn.universal.common.exception.BaseException;
import cn.universal.dm.video.VideoPlatformAdapterRegistry;
import cn.universal.persistence.entity.*;
import cn.universal.persistence.mapper.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.*;
import java.util.concurrent.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Immutable configuration versions and resumable batch results use the shared task center. */
@Service
@Slf4j
@RequiredArgsConstructor
public class VideoSdkConfigurationTaskService {
  private static final Set<String> RESOURCES=Set.of("time","encoding","osd","image","motion","tamper","videoLoss","recordingPlan");
  private final VideoSdkConfigTaskMapper tasks;
  private final IoTTaskItemMapper items;
  private final VideoPlatformInstanceMapper instances;
  private final VideoPlatformDeviceMapper devices;
  private final VideoPlatformChannelMapper channels;
  private final VideoPlatformAdapterRegistry adapters;
  private final ObjectMapper json;
  private final String owner=UUID.randomUUID().toString();
  private final ExecutorService workers=new ThreadPoolExecutor(2,2,0,TimeUnit.SECONDS,new SynchronousQueue<>(),
      r->{Thread t=new Thread(r,"isup-config-batch");t.setDaemon(true);return t;});

  public Map<String,Object> templates(VideoPlatformInstance instance) {
    requireIsup(instance);
    return Map.of("records",tasks.templates(instance.getTenantId(),instance.getInstanceKey()).stream().map(this::templateView).toList());
  }
  @Transactional
  public Map<String,Object> createTemplate(VideoPlatformInstance instance,Map<String,Object> input,String creator) {
    requireIsup(instance);
    if(!Set.of("name","resource","changes").containsAll(input.keySet()))throw new BaseException("配置模板参数包含不支持字段");
    String resource=Objects.toString(input.get("resource"),"");
    if(!RESOURCES.contains(resource))throw new BaseException("该资源尚不支持可写配置模板");
    String name=Objects.toString(input.get("name"),"").trim();
    if(name.isBlank()||name.length()>100)throw new BaseException("模板名称需为1到100字");
    if(!(input.get("changes") instanceof Map<?,?> changes)||changes.isEmpty())throw new BaseException("配置模板必须包含至少一个更改字段");
    Map<String,Object> parameters=new LinkedHashMap<>(input);parameters.remove("name");
    parameters.put("deviceId","validation");parameters.put("expectedRevision","validation");parameters.put("requestId",UUID.randomUUID().toString());
    cn.universal.dm.video.VideoOperationRegistry.validate(cn.universal.dm.video.VideoOperationRegistry.requirePublic("deviceConfigApply"),parameters);
    var task=task(instance,"ISUP_CONFIG_TEMPLATE",name,creator);
    task.setStatus("SUCCESS");task.setRequestSnapshot(write(Map.of("resource",resource,"changes",input.get("changes"))));
    tasks.insert(task);return templateView(task);
  }
  @Transactional
  public Map<String,Object> createBatch(VideoPlatformInstance instance,Map<String,Object> input,String creator) {
    requireIsup(instance);
    if(!Integer.valueOf(1).equals(instance.getEnabled()))throw new BaseException("实例已停用");
    if(!Set.of("templateId","targets").containsAll(input.keySet()))throw new BaseException("批量配置参数包含不支持字段");
    var template=requireTask(instance,Objects.toString(input.get("templateId")),"ISUP_CONFIG_TEMPLATE");
    if(!(input.get("targets") instanceof List<?> targets)||targets.isEmpty()||targets.size()>200)throw new BaseException("每批需选择1到200个设备通道");
    Map<String,Object> configuration=read(template.getRequestSnapshot());
    var batch=task(instance,"ISUP_CONFIG_BATCH",template.getTaskName(),creator);
    batch.setStatus("PENDING");batch.setTotalCount(targets.size());batch.setRequestSnapshot(template.getRequestSnapshot());batch.setDefinitionId(template.getId());
    List<IoTTaskItem> entries=new ArrayList<>();Set<String> identities=new HashSet<>();
    for(Object value:targets){
      if(!(value instanceof Map<?,?> target)||!Set.of("deviceId","channelId").containsAll(target.keySet()))throw new BaseException("批量目标字段无效");
      String device=Objects.toString(target.get("deviceId"),""),channel=Objects.toString(target.get("channelId"),"");
      validateTarget(instance,device,channel,Objects.toString(configuration.get("resource")));
      if(!identities.add(device+"/"+channel))throw new BaseException("批量目标重复");
      IoTTaskItem item=new IoTTaskItem();item.setId(IdUtil.getSnowflakeNextId());item.setTaskId(batch.getId());item.setTenantId(instance.getTenantId());
      item.setItemNo(UUID.randomUUID().toString());item.setItemKey(device+"/"+channel);item.setItemName(device+"/"+channel);
      item.setStatus("PENDING");item.setRawData(write(Map.of("deviceId",device,"channelId",channel)));
      item.setCreateTime(new Date());item.setUpdateTime(item.getCreateTime());entries.add(item);
    }
    tasks.insert(batch);for(var item:entries)items.insert(item);return batchView(batch,entries);
  }
  public Map<String,Object> batch(VideoPlatformInstance instance,String id){var task=requireTask(instance,id,"ISUP_CONFIG_BATCH");return batchView(task,configurationItems(task));}
  public Map<String,Object> batches(VideoPlatformInstance instance){requireIsup(instance);return Map.of("records",tasks.batches(instance.getTenantId(),instance.getInstanceKey()).stream().map(t->batchView(t,configurationItems(t))).toList());}
  public Map<String,Object> cancel(VideoPlatformInstance instance,String id){var task=requireTask(instance,id,"ISUP_CONFIG_BATCH");tasks.cancel(task.getId(),instance.getTenantId(),instance.getInstanceKey());return batch(instance,id);}

  @Scheduled(initialDelay=5000,fixedDelay=2000)
  public void tick(){
    try{for(var task:tasks.due(new Date(System.currentTimeMillis()-120_000))){try{workers.execute(()->process(task.getId()));}catch(RejectedExecutionException full){break;}}}
    catch(Exception ex){log.warn("ISUP批量配置调度暂不可用：{}",ex.getClass().getSimpleName());}
  }
  void process(long id){
    Date now=new Date();if(tasks.claim(id,owner,now,new Date(now.getTime()-120_000))!=1)return;
    var batch=tasks.selectByPrimaryKey(id);List<IoTTaskItem> entries=configurationItems(batch);
    var probe=new VideoPlatformInstance();probe.setInstanceKey(batch.getBizKey());var instance=instances.selectOne(probe);
    Map<String,Object> template=read(batch.getRequestSnapshot());
    for(var item:entries){
      if(!Set.of("PENDING","DISPATCHED").contains(item.getStatus()))continue;
      try{
        if(tasks.renew(id,owner,new Date())!=1)return;
        var latest=tasks.selectByPrimaryKey(id);
        if("CANCEL_REQUESTED".equals(latest.getStatus())){item.setStatus("CANCELLED");item.setMessage("取消待执行目标");continue;}
        if(instance==null||!Objects.equals(instance.getTenantId(),batch.getTenantId())||!Integer.valueOf(1).equals(instance.getEnabled()))throw new BaseException("实例停用或租户变更");
        var target=read(item.getRawData());String device=Objects.toString(target.get("deviceId")),channel=Objects.toString(target.get("channelId"),"");
        validateTarget(instance,device,channel,Objects.toString(template.get("resource")));
        var adapter=adapters.getAdapter("hik_isup");Map<String,Object> request=new LinkedHashMap<>(target);if(channel.isBlank())request.remove("channelId");request.put("resource",template.get("resource"));
        if("DISPATCHED".equals(item.getStatus())){
          var observed=adapter.executeOperation(instance,"deviceConfigRead",request);
          item.setStatus("UNKNOWN");item.setMessage("服务中断后的配置结果需核对，已保留读取结果并停止自动重放");item.setResultData(write(observed));continue;
        }
        var capabilities=adapter.executeOperation(instance,"capabilities",target);
        if(capabilities.get("operations") instanceof List<?> rows&&rows.stream().anyMatch(row->row instanceof Map<?,?> m&&"deviceConfigApply".equals(m.get("id"))
            &&(Boolean.FALSE.equals(m.get("implemented"))||"unsupported".equals(m.get("deviceSupport"))))){item.setStatus("SKIPPED");item.setMessage("设备或网关明确不支持此配置能力");continue;}
        var observed=adapter.executeOperation(instance,"deviceConfigRead",request);
        String revision=Objects.toString(observed.get("observedRevision"),"");if(revision.isBlank())throw new BaseException("无法获得配置版本，已停止写入");
        request.put("expectedRevision",revision);request.put("changes",template.get("changes"));request.put("requestId",item.getItemNo());
        if("CANCEL_REQUESTED".equals(tasks.selectByPrimaryKey(id).getStatus())){item.setStatus("CANCELLED");item.setMessage("取消待执行目标");continue;}
        item.setStatus("DISPATCHED");item.setResultData(write(Map.of("beforeRevision",revision,"requestId",item.getItemNo())));item.setUpdateTime(new Date());
        if(tasks.updateItemOwned(item,owner)!=1||tasks.renew(id,owner,new Date())!=1)return;
        var result=adapter.executeOperation(instance,"deviceConfigApply",request);
        item.setResultData(write(result));String status=Objects.toString(result.get("status"),"unknown");
        item.setStatus("success".equals(status)&&Boolean.TRUE.equals(result.get("verified"))?"SUCCESS":"partial".equals(status)?"PARTIAL":"unknown".equals(status)?"UNKNOWN":"FAILED");
        item.setMessage("SUCCESS".equals(item.getStatus())?"配置已读取确认":"配置未全部确认，请查看逐项结果");
      }catch(Exception ex){
        boolean uncertain="DISPATCHED".equals(item.getStatus());item.setStatus(uncertain?"UNKNOWN":"FAILED");
        item.setMessage(uncertain?"配置下发结果未知，请先读取核对后再创建新任务":"目标执行失败，请检查能力、在线状态与配置版本");
      }finally{item.setUpdateTime(new Date());if(tasks.updateItemOwned(item,owner)!=1||tasks.renew(id,owner,new Date())!=1)return;}
    }
    int success=(int)entries.stream().filter(e->"SUCCESS".equals(e.getStatus())).count();
    int failed=(int)entries.stream().filter(e->!"SUCCESS".equals(e.getStatus())).count();
    String status=entries.stream().allMatch(e->"CANCELLED".equals(e.getStatus()))?"CANCELLED":failed==0?"SUCCESS":
        success>0||entries.stream().anyMatch(e->Set.of("PARTIAL","UNKNOWN").contains(e.getStatus()))?"PARTIAL_SUCCESS":"FAILED";
    tasks.finish(id,owner,status,success,failed,write(Map.of("completed",entries.size())),new Date());
  }
  private List<IoTTaskItem> configurationItems(IoTTaskCenter batch){return tasks.configurationItems(batch.getId(),batch.getTenantId(),batch.getBizKey());}
  private void validateTarget(VideoPlatformInstance instance,String device,String channel,String resource){
    var d=devices.selectOne(VideoPlatformDevice.builder().instanceKey(instance.getInstanceKey()).deviceId(device).build());
    if(d==null||!Objects.equals(d.getTenantId(),instance.getTenantId())||Integer.valueOf(0).equals(d.getEnabled()))throw new BaseException("设备不存在、已停用或无权访问");
    if(!"time".equals(resource)&&channel.isBlank())throw new BaseException("该配置模板必须指定通道");
    if(!channel.isBlank()&&channels.selectCount(VideoPlatformChannel.builder().instanceKey(instance.getInstanceKey()).deviceId(d.getId()).channelId(channel).build())==0)throw new BaseException("配置目标通道不存在");
  }
  private IoTTaskCenter requireTask(VideoPlatformInstance instance,String id,String type){
    requireIsup(instance);IoTTaskCenter task;try{task=tasks.selectByPrimaryKey(Long.parseLong(id));}catch(Exception ex){throw new BaseException("任务不存在");}
    if(task==null||!type.equals(task.getTaskType())||!Objects.equals(task.getTenantId(),instance.getTenantId())||!Objects.equals(task.getBizKey(),instance.getInstanceKey()))throw new BaseException("配置模板或任务不存在、或无权访问");return task;
  }
  private IoTTaskCenter task(VideoPlatformInstance instance,String type,String name,String creator){
    Date now=new Date();return IoTTaskCenter.builder().id(IdUtil.getSnowflakeNextId()).taskNo(UUID.randomUUID().toString()).taskType(type).taskName(name)
        .tenantId(instance.getTenantId()).bizModule("video").bizKey(instance.getInstanceKey()).productKey(instance.getInstanceKey())
        .creatorId(creator==null||creator.isBlank()?instance.getCreatorId():creator).totalCount(0).successCount(0).failCount(0).createTime(now).updateTime(now).build();
  }
  private Map<String,Object> templateView(IoTTaskCenter task){Map<String,Object> result=new LinkedHashMap<>(read(task.getRequestSnapshot()));result.put("id",String.valueOf(task.getId()));result.put("version",String.valueOf(task.getId()));result.put("name",task.getTaskName());result.put("createdAt",task.getCreateTime().getTime());return result;}
  private Map<String,Object> batchView(IoTTaskCenter task,List<IoTTaskItem> entries){
    List<Map<String,Object>> results=new ArrayList<>();for(var item:entries){Map<String,Object> result=new LinkedHashMap<>(read(item.getRawData()));result.put("status",item.getStatus().toLowerCase(Locale.ROOT));result.put("message",Objects.toString(item.getMessage(),""));result.put("result",read(item.getResultData()));results.add(result);}
    return Map.of("id",String.valueOf(task.getId()),"status",task.getStatus().toLowerCase(Locale.ROOT).replace("partial_success","partial"),"total",entries.size(),
        "completed",entries.stream().filter(e->!Set.of("PENDING","DISPATCHED").contains(e.getStatus())).count(),"results",results,"templateId",Objects.toString(task.getDefinitionId(),""),
        "taskName",task.getTaskName(),"createdAt",task.getCreateTime().getTime(),"updatedAt",task.getUpdateTime().getTime());
  }
  private static void requireIsup(VideoPlatformInstance instance){if(instance==null||!"hik_isup".equals(instance.getPlatformType())||instance.getTenantId()==null)throw new BaseException("仅有租户归属的 ISUP 平台支持配置模板任务");}
  private Map<String,Object> read(String value){try{return value==null?new LinkedHashMap<>():json.readValue(value,new TypeReference<LinkedHashMap<String,Object>>(){});}catch(Exception ex){throw new BaseException("配置任务数据无效");}}
  private String write(Object value){try{return json.writeValueAsString(value);}catch(Exception ex){throw new BaseException("配置任务数据不可序列化");}}
  @jakarta.annotation.PreDestroy public void stop(){workers.shutdownNow();}
}
