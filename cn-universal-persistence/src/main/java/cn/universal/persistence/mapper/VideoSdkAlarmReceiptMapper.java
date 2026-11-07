package cn.universal.persistence.mapper;

import cn.universal.persistence.common.BaseMapper;
import cn.universal.persistence.entity.VideoSdkAlarmReceipt;
import java.util.List;
import org.apache.ibatis.annotations.*;

public interface VideoSdkAlarmReceiptMapper extends BaseMapper<VideoSdkAlarmReceipt> {
  @Update("UPDATE video_sdk_alarm_receipt SET lease_owner=#{owner},lease_until=#{until},attempts=attempts+1 WHERE id=#{id} AND status<>'done' AND lease_until<#{now}")
  int claim(@Param("id") String id, @Param("owner") String owner, @Param("now") long now, @Param("until") long until);
  @Update("UPDATE video_sdk_alarm_receipt SET log_persisted=1,updated_at=#{now} WHERE id=#{id} AND lease_owner=#{owner}")
  int logged(@Param("id") String id, @Param("owner") String owner, @Param("now") long now);
  @Update("UPDATE video_sdk_alarm_receipt SET status='done',flow_dispatched=#{dispatched},result_json=#{result},updated_at=#{now},lease_until=0,lease_owner='' WHERE id=#{id} AND lease_owner=#{owner}")
  int complete(@Param("id") String id, @Param("owner") String owner, @Param("dispatched") int dispatched, @Param("result") String result, @Param("now") long now);
  @Update("UPDATE video_sdk_alarm_receipt SET next_attempt_at=#{next},updated_at=#{now},lease_until=0,lease_owner='' WHERE id=#{id} AND lease_owner=#{owner}")
  int retry(@Param("id") String id, @Param("owner") String owner, @Param("next") long next, @Param("now") long now);
  @Select("SELECT * FROM video_sdk_alarm_receipt WHERE status<>'done' AND next_attempt_at<=#{now} AND lease_until<#{now} ORDER BY next_attempt_at,created_at LIMIT 16")
  List<VideoSdkAlarmReceipt> due(@Param("now") long now);
  @Select("<script>SELECT * FROM video_sdk_alarm_receipt WHERE tenant_id=#{tenant} AND instance_key=#{instance} AND device_id=#{device} AND event_kind='evidence_ready' AND created_at&gt;=#{from} AND created_at&lt;=#{to} <if test='channel != null and channel != &quot;&quot;'>AND channel_id=#{channel}</if> ORDER BY created_at DESC LIMIT 1000</script>")
  List<VideoSdkAlarmReceipt> evidence(@Param("tenant")String tenant,@Param("instance")String instance,@Param("device")String device,
      @Param("channel")String channel,@Param("from")long from,@Param("to")long to);
  @Select("SELECT COUNT(*) FROM video_sdk_alarm_receipt WHERE tenant_id=#{tenant} AND instance_key=#{instance} AND device_id=#{device} AND channel_id=#{channel} AND event_kind=#{kind} AND flow_dispatched=1 AND created_at>=#{from}")
  int recentDispatch(@Param("tenant") String tenant, @Param("instance") String instance, @Param("device") String device,
      @Param("channel") String channel, @Param("kind") String kind, @Param("from") long from);
  @Update("UPDATE video_sdk_alarm_receipt SET updated_at=#{now},lease_owner=#{receipt} WHERE id=#{guard} AND (updated_at<=#{from} OR lease_owner=#{receipt})")
  int reserveDispatch(@Param("guard") String guard, @Param("receipt") String receipt, @Param("now") long now, @Param("from") long from);
}
