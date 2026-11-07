package cn.universal.web.rule;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONUtil;
import cn.universal.common.constant.IoTConstant;
import cn.universal.core.message.UPRequest;
import cn.universal.persistence.entity.bo.TriggerBO;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

/**
 * @author gitee.com/NexIoT
 * @version 1.0
 * @since 2025/10/19 18:47
 */
@Slf4j
public class DevicePerpertiesUPTest {
  public static String trigger =
      """
    [{"trigger":"device","type":"properties","deviceId":"89861590112330001924-2","deviceName":"通道2子设备","productKey":"3QAz7HuahAGN","filters":[{"key":"illumination","value":"2400","operator":"gt"},{"key":"humidity","value":"30","operator":"gt"}]}]
    """;
  public static String uo =
"""
{ "sourcePayload": "020310032900CA00000000000000000000000B513E", "humidity": 80.9, "humidityDesc": "湿度较高，建议适当通风", "temperature": 20.2, "temperatureDesc": "温度舒适，体感宜人", "illumination": 11, "illuminationDesc": "光线昏暗，适合休息", "overallComfort": "环境一般，部分指标需优化" }
    """;

  public static void main(String[] args) {
    UPRequest upRequest = new UPRequest();
    upRequest.setMessageType(IoTConstant.MessageType.PROPERTIES);
    upRequest.setProperties(JSONUtil.parseObj(uo));
    JSONArray jsonArray = JSONUtil.parseArray(trigger);
    List<TriggerBO> triggers =
        jsonArray.stream()
            .map(o -> BeanUtil.toBean(o, TriggerBO.class))
            .filter(o -> "device".equals(o.getTrigger()))
            .collect(Collectors.toList());
    DevicePerpertiesUPTest up = new DevicePerpertiesUPTest();
    up.testAlarm(triggers, "&&", upRequest);
  }

  public boolean testAlarm(List<TriggerBO> triggers, String separator, UPRequest upRequest) {
    Map<String, Object> properties = upRequest.getProperties();
    String express =
        triggers.stream()
            .map(
                triggerBo -> {
                  String filterExpress =
                      triggerBo.getFilters().stream()
                          .filter(item -> properties.containsKey(item.getKey()))
                          .map(
                              filter ->
                                  String.format(
                                      "%s %s %s",
                                      filter.getKey(),
                                      TriggerBO.Operator.valueOf(filter.getOperator()).getSymbol(),
                                      NumberUtil.isNumber(filter.getValue())
                                          ? filter.getValue()
                                          : String.format("'%s'", filter.getValue())))
                          .collect(Collectors.joining(separator));
                  return StrUtil.isEmpty(filterExpress) ? "" : String.format("(%s)", filterExpress);
                })
            .filter(StrUtil::isNotEmpty)
            .collect(Collectors.joining(separator));
    if (StrUtil.isEmpty(express)) {
      return false;
    }
    log.info("执行场景联动条件,express={},properties={}", express, properties);
    return true;
  }
}
