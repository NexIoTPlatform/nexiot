package cn.universal.persistence.mapper;

import cn.universal.persistence.common.BaseMapper;
import cn.universal.persistence.entity.IoTTaskCenter;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface IoTTaskCenterMapper extends BaseMapper<IoTTaskCenter> {

  List<IoTTaskCenter> selectTaskCenterList(
      @Param("query") IoTTaskCenter query,
      @Param("unionId") String unionId,
      @Param("admin") Boolean admin);

  IoTTaskCenter selectTaskCenterById(@Param("id") Long id);

  int markTaskRunningIfPending(@Param("id") Long id);

  int countActiveTasksByDefinitionId(@Param("definitionId") Long definitionId);

  IoTTaskCenter selectLatestActiveTaskByDefinitionId(@Param("definitionId") Long definitionId);
}
