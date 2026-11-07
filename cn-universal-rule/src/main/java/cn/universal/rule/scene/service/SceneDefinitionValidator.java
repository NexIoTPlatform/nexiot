package cn.universal.rule.scene.service;

import cn.universal.persistence.entity.bo.SceneLinkageBO;
import cn.universal.persistence.entity.bo.TriggerBO;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.quartz.CronExpression;
import org.springframework.util.StringUtils;

/** 写入边界校验；部分更新中 null 表示保留原配置。 */
final class SceneDefinitionValidator {
  private static final Set<String> OPERATORS =
      Set.of("eq", "not", "ne", "gt", "lt", "gte", "lte", "like", "in", "nin");

  private SceneDefinitionValidator() {}

  static void validate(SceneLinkageBO scene, boolean create) {
    if (create || scene.getSceneName() != null)
      require(StringUtils.hasText(scene.getSceneName()), "场景名称不能为空");
    if (scene.getSleepCycle() != null)
      require(
          scene.getSleepCycle() >= 0 && scene.getSleepCycle() <= Integer.MAX_VALUE,
          "沉默周期必须为有效的非负秒数");
    if (scene.getStatus() != null)
      require(scene.getStatus() == 0 || scene.getStatus() == 1, "场景状态只能为启用或停用");
    if (create || scene.getTouch() != null)
      require(
          "one".equals(scene.getTouch()) || "all".equals(scene.getTouch()), "条件关系必须为 one 或 all");
    List<TriggerBO> triggers = scene.getTriggerCondition();
    if (create || triggers != null) {
      require(triggers != null && !triggers.isEmpty(), "至少配置一个触发器");
      for (TriggerBO t : triggers) {
        require(t != null, "触发器不能为空");
        switch (Objects.toString(t.getTrigger(), "")) {
          case "device" -> {
            require(
                StringUtils.hasText(t.getProductKey()) && StringUtils.hasText(t.getDeviceId()),
                "设备触发器必须指定产品和设备");
            require(
                "properties".equals(t.getType()) || "event".equals(t.getType()), "设备触发器只支持属性和事件");
            if ("event".equals(t.getType())) {
              require(StringUtils.hasText(t.getModelId()), "事件标识不能为空");
              validateFilters(t.getFilters());
            } else {
              require(t.getFilters() != null && !t.getFilters().isEmpty(), "属性触发器至少配置一个条件");
              validateFilters(t.getFilters());
            }
          }
          case "timer" ->
              require(
                  t.getCron() != null && CronExpression.isValidExpression(t.getCron()),
                  "定时触发器的 cron 表达式无效");
          case "manual" -> {}
          default -> throw new IllegalArgumentException("不支持的触发方式");
        }
      }
      if ("all".equals(scene.getTouch())) {
        TriggerBO first = triggers.getFirst();
        require(
            triggers.stream()
                .allMatch(
                    t ->
                        "device".equals(t.getTrigger())
                            && Objects.equals(first.getProductKey(), t.getProductKey())
                            && Objects.equals(first.getDeviceId(), t.getDeviceId())
                            && Objects.equals(first.getType(), t.getType())),
            "满足全部仅支持同一设备、同一消息类型的一次上报；多个设备请使用满足任意一个");
      }
    }
    if (create || scene.getExecAction() != null) {
      require(scene.getExecAction() != null && !scene.getExecAction().isEmpty(), "至少配置一个执行动作");
      for (TriggerBO action : scene.getExecAction()) {
        require(action != null, "执行动作不能为空");
        if ("notice".equals(action.getTrigger()))
          require(StringUtils.hasText(action.getNoticeTemplateId()), "通知动作必须选择通知模板");
        else {
          require(
              "device".equals(action.getTrigger())
                  && StringUtils.hasText(action.getProductKey())
                  && StringUtils.hasText(action.getDeviceId())
                  && StringUtils.hasText(action.getModelId()),
              "设备动作必须指定产品、设备和功能");
        }
      }
    }
  }

  private static void validateFilters(List<TriggerBO.ConditionFilter> filters) {
    if (filters == null) return;
    require(filters.size() <= 100, "触发条件数量超过限制");
    for (var filter : filters) {
      require(filter != null && StringUtils.hasText(filter.getKey())
          && filter.getKey().length() <= 512 && filter.getValue() != null
          && !filter.getValue().isEmpty(), "条件的字段标识和值不能为空");
      require(OPERATORS.contains(Objects.toString(filter.getOperator(), "")), "不支持的比较操作符");
      if ("ne".equals(filter.getOperator())) filter.setOperator("not");
    }
  }

  private static void require(boolean condition, String message) {
    if (!condition) throw new IllegalArgumentException(message);
  }
}
