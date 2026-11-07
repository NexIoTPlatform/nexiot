package cn.universal.persistence.mapper;

import cn.universal.persistence.common.BaseMapper;
import cn.universal.persistence.entity.IoTTaskItem;
import cn.universal.persistence.entity.vo.IoTTaskCenterStatisticsVO;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface IoTTaskItemMapper extends BaseMapper<IoTTaskItem> {

  List<IoTTaskItem> selectTaskItemList(@Param("query") IoTTaskItem query);

  List<IoTTaskItem> selectByTaskId(@Param("taskId") Long taskId);

  int insertTaskItemBatch(@Param("list") List<IoTTaskItem> list);

  IoTTaskCenterStatisticsVO selectTaskStatistics(@Param("taskId") Long taskId);
}
