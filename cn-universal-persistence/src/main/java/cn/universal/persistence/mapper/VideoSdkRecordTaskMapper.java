package cn.universal.persistence.mapper;

import cn.universal.persistence.common.BaseMapper;
import cn.universal.persistence.entity.VideoSdkRecordTask;
import java.util.List;
import org.apache.ibatis.annotations.*;

public interface VideoSdkRecordTaskMapper extends BaseMapper<VideoSdkRecordTask> {
  @Select("<script>SELECT * FROM video_sdk_record_task WHERE tenant_id=#{tenant} AND instance_key=#{instance} <if test='status != null and status != &quot;&quot;'>AND status=#{status}</if> ORDER BY created_at DESC, id DESC LIMIT #{limit} OFFSET #{offset}</script>")
  List<VideoSdkRecordTask> page(@Param("tenant") String tenant, @Param("instance") String instance,
      @Param("status") String status, @Param("limit") int limit, @Param("offset") int offset);
  @Select("<script>SELECT COUNT(*) FROM video_sdk_record_task WHERE tenant_id=#{tenant} AND instance_key=#{instance} <if test='status != null and status != &quot;&quot;'>AND status=#{status}</if></script>")
  long count(@Param("tenant") String tenant, @Param("instance") String instance, @Param("status") String status);
  @Select("SELECT COUNT(*) FROM video_sdk_record_task WHERE tenant_id=#{tenant} AND instance_key=#{instance} AND status NOT IN ('ready','partial','failed','cancelled','expired','interrupted')")
  int active(@Param("tenant") String tenant, @Param("instance") String instance);
  @Select("SELECT * FROM video_sdk_record_task WHERE next_poll_at <= #{now} AND lease_until < #{now} AND (status NOT IN ('ready','partial','failed','cancelled','expired','interrupted') OR (status='ready' AND mode='instant')) ORDER BY next_poll_at,created_at LIMIT 16")
  List<VideoSdkRecordTask> due(@Param("now") long now);
  // Filter before LIMIT so tasks for another node's adapters cannot starve this worker's queue.
  @Select("<script>SELECT t.* FROM video_sdk_record_task t LEFT JOIN video_platform_instance i ON i.instance_key=t.instance_key "
      + "WHERE t.next_poll_at &lt;= #{now} AND t.lease_until &lt; #{now} "
      + "AND (t.status NOT IN ('ready','partial','failed','cancelled','expired','interrupted') OR (t.status='ready' AND t.mode='instant')) "
      + "AND (i.instance_key IS NULL <if test='platformTypes != null and !platformTypes.isEmpty()'>OR LOWER(i.platform_type) IN "
      + "<foreach collection='platformTypes' item='type' open='(' separator=',' close=')'>#{type}</foreach></if>) "
      + "ORDER BY t.next_poll_at,t.created_at LIMIT 16</script>")
  List<VideoSdkRecordTask> dueForPlatforms(@Param("now") long now, @Param("platformTypes") java.util.Collection<String> platformTypes);
  @Update("UPDATE video_sdk_record_task SET lease_owner=#{owner},lease_until=#{until} WHERE id=#{id} AND lease_until < #{now}")
  int claim(@Param("id") String id, @Param("owner") String owner, @Param("now") long now, @Param("until") long until);
  @Update("UPDATE video_sdk_record_task SET status=#{t.status},progress=#{t.progress},message=#{t.message},gateway_job_id=#{t.gatewayJobId},result_json=#{t.resultJson},updated_at=#{t.updatedAt},next_poll_at=#{t.nextPollAt},lease_until=0,lease_owner='' WHERE id=#{t.id} AND lease_owner=#{owner}")
  int finishPoll(@Param("t") VideoSdkRecordTask task, @Param("owner") String owner);
  @Update("UPDATE video_sdk_record_task SET gateway_job_id=#{id} WHERE id=#{id} AND lease_owner=#{owner}")
  int markDispatch(@Param("id") String id, @Param("owner") String owner);
  @Update("UPDATE video_sdk_record_task SET cancel_requested=1,next_poll_at=0 WHERE id=#{id} AND tenant_id=#{tenant} AND instance_key=#{instance} AND status NOT IN ('ready','partial','failed','cancelled','expired','interrupted')")
  int requestCancel(@Param("id") String id, @Param("tenant") String tenant, @Param("instance") String instance);
}
