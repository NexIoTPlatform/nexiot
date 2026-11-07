package cn.universal.persistence.mapper;

import cn.universal.persistence.common.BaseMapper;
import cn.universal.persistence.entity.VideoSdkAlarmProcess;
import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.*;

public interface VideoSdkAlarmProcessMapper extends BaseMapper<VideoSdkAlarmProcess> {
  @Select("SELECT * FROM video_sdk_alarm_process WHERE id=#{id} FOR UPDATE")
  VideoSdkAlarmProcess lock(@Param("id") String id);

  @Select(
      "SELECT * FROM video_sdk_alarm_process WHERE row_kind='process' AND state IN"
          + " ('active','unknown') AND last_received_at<#{before} ORDER BY last_received_at,id"
          + " LIMIT 100")
  List<VideoSdkAlarmProcess> expired(@Param("before") long before);

  String FILTER =
      " FROM video_sdk_alarm_process WHERE row_kind='process' AND tenant_id=#{scope.tenant} AND"
          + " instance_key=#{scope.instance} AND device_id=#{scope.device} AND"
          + " first_time&lt;=#{scope.end} AND last_time&gt;=#{scope.start} <if test='scope.channel"
          + " != null and scope.channel != &quot;&quot;'>AND channel_id=#{scope.channel}</if> <if"
          + " test='scope.state != &quot;all&quot;'>AND state=#{scope.state}</if>";

  @Select(
      "<script>SELECT *"
          + FILTER
          + " ORDER BY last_time DESC,id DESC LIMIT #{limit} OFFSET #{offset}</script>")
  List<VideoSdkAlarmProcess> page(
      @Param("scope") Map<String, Object> scope,
      @Param("limit") int limit,
      @Param("offset") long offset);

  @Select("<script>SELECT COUNT(*)" + FILTER + "</script>")
  long count(@Param("scope") Map<String, Object> scope);
}
