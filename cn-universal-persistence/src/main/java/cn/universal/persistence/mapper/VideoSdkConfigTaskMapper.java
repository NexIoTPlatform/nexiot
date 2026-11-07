package cn.universal.persistence.mapper;

import cn.universal.persistence.common.BaseMapper;
import cn.universal.persistence.entity.IoTTaskCenter;
import cn.universal.persistence.entity.IoTTaskItem;
import java.util.Date;
import java.util.List;
import org.apache.ibatis.annotations.*;

/** Reuses durable task-center rows without changing its general executor contracts. */
public interface VideoSdkConfigTaskMapper extends BaseMapper<IoTTaskCenter> {
  @Select("SELECT i.* FROM iot_task_item i JOIN iot_task_center c ON c.id=i.task_id WHERE i.task_id=#{id} AND i.tenant_id=#{tenant} AND c.tenant_id=#{tenant} AND c.biz_key=#{instance} AND c.task_type='ISUP_CONFIG_BATCH' ORDER BY i.row_no,i.id")
  List<IoTTaskItem> configurationItems(@Param("id") long id,@Param("tenant") String tenant,@Param("instance") String instance);
  @Select("SELECT * FROM iot_task_center WHERE task_type='ISUP_CONFIG_TEMPLATE' AND tenant_id=#{tenant} AND biz_key=#{instance} ORDER BY create_time DESC,id DESC LIMIT 200")
  List<IoTTaskCenter> templates(@Param("tenant") String tenant, @Param("instance") String instance);
  @Select("SELECT * FROM iot_task_center WHERE task_type='ISUP_CONFIG_BATCH' AND tenant_id=#{tenant} AND biz_key=#{instance} ORDER BY create_time DESC,id DESC LIMIT 100")
  List<IoTTaskCenter> batches(@Param("tenant") String tenant,@Param("instance") String instance);
  @Select("SELECT * FROM iot_task_center WHERE task_type='ISUP_CONFIG_BATCH' AND (status='PENDING' OR (status='CANCEL_REQUESTED' AND ext_json IS NULL) OR (status IN ('RUNNING','CANCEL_REQUESTED') AND update_time<#{expired})) ORDER BY create_time LIMIT 8")
  List<IoTTaskCenter> due(@Param("expired") Date expired);
  @Update("UPDATE iot_task_center SET status=CASE WHEN status='CANCEL_REQUESTED' THEN 'CANCEL_REQUESTED' ELSE 'RUNNING' END,ext_json=#{owner},update_time=#{now} WHERE id=#{id} AND (status='PENDING' OR (status='CANCEL_REQUESTED' AND ext_json IS NULL) OR (status IN ('RUNNING','CANCEL_REQUESTED') AND update_time<#{expired}))")
  int claim(@Param("id") long id, @Param("owner") String owner, @Param("now") Date now, @Param("expired") Date expired);
  @Update("UPDATE iot_task_center SET update_time=#{now} WHERE id=#{id} AND ext_json=#{owner} AND status IN ('RUNNING','CANCEL_REQUESTED')")
  int renew(@Param("id") long id, @Param("owner") String owner, @Param("now") Date now);
  @Update("UPDATE iot_task_item SET status=#{item.status},message=#{item.message},result_data=#{item.resultData},update_time=#{item.updateTime} WHERE id=#{item.id} AND tenant_id=#{item.tenantId} AND EXISTS (SELECT 1 FROM iot_task_center c WHERE c.id=#{item.taskId} AND c.ext_json=#{owner} AND c.status IN ('RUNNING','CANCEL_REQUESTED'))")
  int updateItemOwned(@Param("item") IoTTaskItem item,@Param("owner") String owner);
  @Update("UPDATE iot_task_center SET status=#{status},success_count=#{success},fail_count=#{failed},result_snapshot=#{result},end_time=#{now},update_time=#{now} WHERE id=#{id} AND ext_json=#{owner}")
  int finish(@Param("id") long id, @Param("owner") String owner, @Param("status") String status,
      @Param("success") int success, @Param("failed") int failed, @Param("result") String result, @Param("now") Date now);
  @Update("UPDATE iot_task_center SET status='CANCEL_REQUESTED' WHERE id=#{id} AND tenant_id=#{tenant} AND biz_key=#{instance} AND task_type='ISUP_CONFIG_BATCH' AND status IN ('PENDING','RUNNING')")
  int cancel(@Param("id") long id, @Param("tenant") String tenant, @Param("instance") String instance);
}
