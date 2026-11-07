package cn.universal.persistence.mapper;

import cn.universal.persistence.common.BaseMapper;
import cn.universal.persistence.entity.VideoSdkAlarmProcessMember;
import java.util.List;
import org.apache.ibatis.annotations.*;

public interface VideoSdkAlarmProcessMemberMapper extends BaseMapper<VideoSdkAlarmProcessMember> {
  @Select(
      "SELECT * FROM video_sdk_alarm_process_member WHERE tenant_id=#{tenant} AND"
          + " instance_key=#{instance} AND device_id=#{device} AND channel_id=#{channel} AND"
          + " event_id=#{event} ORDER BY alarm_time DESC,receipt_id DESC LIMIT 2")
  List<VideoSdkAlarmProcessMember> original(
      @Param("tenant") String tenant,
      @Param("instance") String instance,
      @Param("device") String device,
      @Param("channel") String channel,
      @Param("event") String event);

  @Select(
      "SELECT * FROM video_sdk_alarm_process_member WHERE process_id=#{process} AND"
          + " tenant_id=#{tenant} AND instance_key=#{instance} ORDER BY alarm_time,receipt_id LIMIT"
          + " #{limit} OFFSET #{offset}")
  List<VideoSdkAlarmProcessMember> page(
      @Param("tenant") String tenant,
      @Param("instance") String instance,
      @Param("process") String process,
      @Param("limit") int limit,
      @Param("offset") long offset);

  @Select(
      "SELECT COUNT(*) FROM video_sdk_alarm_process_member WHERE process_id=#{process} AND"
          + " tenant_id=#{tenant} AND instance_key=#{instance}")
  long count(
      @Param("tenant") String tenant,
      @Param("instance") String instance,
      @Param("process") String process);
}
