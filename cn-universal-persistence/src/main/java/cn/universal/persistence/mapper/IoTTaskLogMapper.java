package cn.universal.persistence.mapper;

import cn.universal.persistence.common.BaseMapper;
import cn.universal.persistence.entity.IoTTaskLog;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface IoTTaskLogMapper extends BaseMapper<IoTTaskLog> {

  List<IoTTaskLog> selectTaskLogList(@Param("taskId") Long taskId);
}
