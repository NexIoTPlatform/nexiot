package cn.universal.persistence.mapper;

import cn.universal.persistence.common.BaseMapper;
import cn.universal.persistence.entity.IoTTaskDefinition;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface IoTTaskDefinitionMapper extends BaseMapper<IoTTaskDefinition> {

  List<IoTTaskDefinition> selectTaskDefinitionList(
      @Param("query") IoTTaskDefinition query,
      @Param("unionId") String unionId,
      @Param("admin") Boolean admin);

  IoTTaskDefinition selectTaskDefinitionById(@Param("id") Long id);
}
