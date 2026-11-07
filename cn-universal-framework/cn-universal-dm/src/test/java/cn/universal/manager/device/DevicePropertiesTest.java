/*
 *
 * Copyright (c) 2025, NexIoT. All Rights Reserved.
 *
 * @Description: 本文件由 gitee.com/NexIoT 开发并拥有版权，未经授权严禁擅自商用、复制或传播。
 * @Author: gitee.com/NexIoT
 * @Email: wo8335224@gmail.com
 * @Wechat: outlookFil
 *
 *
 */

package cn.universal.manager.device;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.universal.core.metadata.AbstractFunctionMetadata;
import cn.universal.core.metadata.DeviceMetadata;
import cn.universal.dm.device.entity.IoTDevicePropertiesBO;
import java.util.Optional;
import org.junit.Test;

/**
 * @author gitee.com/NexIoT
 * @version 1.0
 * @since 2025/11/19
 */
public class DevicePropertiesTest {

  @Test
  public void metaProperties() {
    String str =
        "{\"events\":[{\"id\":\"offline\",\"name\":\"下线\",\"expands\":{\"level\":\"warn\"},"
            + "\"valueType\":{\"type\":\"string\",\"expands\":{\"maxLength\":\"50\"}}}],"
            + "\"properties\":[{\"id\":\"smokeConcentration\",\"name\":\"烟雾浓度\",\"valueType\":{\"type\":\"int\","
            + "\"unit\":\"%\"}},{\"id\":\"greateValue\",\"name\":\"是否超标\",\"valueType\":{\"type\":\"boolean\"},"
            + "\"description\":\"是或者否\"},{\"id\":\"onlineStatus\",\"name\":\"开启状态\",\"valueType\":{\"type\":\"enum\","
            + "\"elements\":[{\"id\":\"open\",\"value\":\"0\",\"text\":\"开启\"},{\"id\":\"close\",\"value\":\"1\","
            + "\"text\":\"关闭\"}]}}],\"functions\":[{\"id\":\"ConfigurationFunction\",\"name\":\"下发配置信息\",\"output\":{},"
            + "\"inputs\":[{\"id\":\"deviceRestart\",\"name\":\"设备模式\",\"valueType\":{\"type\":\"enum\","
            + "\"elements\":[{\"text\":\"制冷\",\"value\":\"0\",\"id\":0},{\"id\":2,\"value\":\"1\",\"text\":\"制热\"},"
            + "{\"id\":3,\"value\":\"2\",\"text\":\"通风\"}]},\"description\":\"设备重启为1有效\"}]}],\"tags\":[]}";
    JSONObject json = JSONUtil.parseObj(str);
    DeviceMetadata data = new DeviceMetadata(json);
    Optional<AbstractFunctionMetadata> deviceIOTFunction =
        data.getFunction("ConfigurationFunction");
    System.out.println(JSONUtil.toJsonStr(deviceIOTFunction.get().getInputs()));
    IoTDevicePropertiesBO entity = new IoTDevicePropertiesBO();
    entity.withValue(data.getPropertyOrNull("onlineStatus"), 1);
    System.out.println(JSONUtil.toJsonStr(entity));

    IoTDevicePropertiesBO greateValue = new IoTDevicePropertiesBO();
    greateValue.withValue(data.getPropertyOrNull("greateValue"), "true");
    System.out.println(JSONUtil.toJsonStr(greateValue));
  }

  @Test
  public void rwMode() {
    String metadata =
        """
    {"events":[{"id":"online","name":"上线","valueType":{"type":"string"}},{"id":"offline","name":"下线","valueType":{"type":"string"}},{"config":false,"expands":{"level":"urgent"},"id":"higher_temp","name":"高温预警","valueType":{"expands":{"maxLength":"50"},"type":"string"}}],"functions":[{"config":false,"description":"水冷降温","id":"coolDown","inputs":[{"id":"waterLevel","name":"浇水量","valueType":{"precision":"2","type":"float","unit":"cm³"}}],"name":"降温","output":{}},{"config":false,"id":"setDeviceDesiredProperties","inputs":[{"id":"code","name":"编号","valueType":{"expands":{"maxLength":"50"},"type":"string"}},{"id":"humidity","name":"湿度","valueType":{"expands":{"maxLength":"50"},"type":"string"}},{"id":"temperture","name":"温度","valueType":{"precision":"2","type":"float"}}],"name":"设置期望值","output":{}}],"properties":[{"config":false,"description":"设备外壳温度","id":"temperture","mode":"r","name":"外壳温度","source":"device","valueType":{"type":"float","unit":"℃"}},{"config":false,"id":"code","mode":"rw","name":"编号","source":"device","valueType":{"type":"string"}},{"config":false,"id":"humidity","mode":"rw","name":"湿度","source":"device","valueType":{"type":"int"}}],"tags":[]}
    """;
    DeviceMetadata deviceMetadata = new DeviceMetadata(new JSONObject(metadata));
    System.out.println(JSONUtil.toJsonStr(deviceMetadata.getProperties()));
  }
}
