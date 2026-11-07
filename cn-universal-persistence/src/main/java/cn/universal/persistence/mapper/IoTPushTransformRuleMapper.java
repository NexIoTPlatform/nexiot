package cn.universal.persistence.mapper;

import cn.universal.persistence.common.BaseMapper;
import cn.universal.persistence.entity.IoTPushTransformRule;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface IoTPushTransformRuleMapper extends BaseMapper<IoTPushTransformRule> {

  List<IoTPushTransformRule> selectByAppUniqueId(@Param("appUniqueId") String appUniqueId);

  List<IoTPushTransformRule> selectPublishedByAppUniqueId(@Param("appUniqueId") String appUniqueId);

  IoTPushTransformRule selectRuleById(@Param("id") Long id);

  int disablePublishedRulesForScope(
      @Param("appUniqueId") String appUniqueId,
      @Param("scopeType") String scopeType,
      @Param("productKey") String productKey,
      @Param("excludeId") Long excludeId,
      @Param("updateBy") String updateBy);

  int disablePublishedRulesForApp(
      @Param("appUniqueId") String appUniqueId,
      @Param("excludeId") Long excludeId,
      @Param("updateBy") String updateBy);

  int softDeleteOtherRulesForApp(
      @Param("appUniqueId") String appUniqueId,
      @Param("keepId") Long keepId,
      @Param("updateBy") String updateBy);
}
