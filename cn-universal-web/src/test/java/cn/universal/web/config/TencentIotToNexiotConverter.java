package cn.universal.web.config;

import cn.hutool.core.io.FileUtil;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import org.junit.Test;

public class TencentIotToNexiotConverter {

  // 输入文件路径（替换为你的a.txt路径）
  private static final String INPUT_FILE_PATH = "/Users/liulihai/Desktop/a.txt";
  // 输出文件路径
  private static final String OUTPUT_FILE_PATH = "nexiot_model.json";
  // Icon保存根文件夹（自动创建）
  private static final String ICON_SAVE_DIR = "iot_icons/";

  @Test
  public void convertWithHierarchy() {
    // 初始化Icon保存文件夹
    File iconDir = new File(ICON_SAVE_DIR);
    if (!iconDir.exists()) {
      iconDir.mkdirs();
    }

    // 1. 读取输入文件
    String jsonStr = FileUtil.readString(new File(INPUT_FILE_PATH), "UTF-8");
    JSONObject root = JSONUtil.parseObj(jsonStr);
    JSONArray allDevices =
        root.getJSONObject("data")
            .getJSONObject("data")
            .getJSONObject("Response")
            .getJSONArray("Data");

    // 2. 构建分类ID到设备的映射
    Map<Integer, JSONObject> deviceMap = new HashMap<>();
    for (Object obj : allDevices) {
      JSONObject device = JSONUtil.parseObj(obj);
      deviceMap.put(device.getInt("Id"), device);
    }

    // 3. 构建三级树形结构（仅最后一级渲染物模型）
    JSONArray hierarchyRoot = new JSONArray();
    for (JSONObject device : deviceMap.values()) {
      if (device.getInt("ParentId") == 0) {
        // 一级分类（ParentId=0）
        JSONObject level1 = buildCategoryNode(device, deviceMap, iconDir);
        hierarchyRoot.add(level1);
      }
    }

    // 4. 输出到文件
    JSONObject finalResult = new JSONObject();
    finalResult.put("categoryHierarchy", hierarchyRoot);
    FileUtil.writeString(
        JSONUtil.toJsonPrettyStr(finalResult), new File(OUTPUT_FILE_PATH), "UTF-8");
    System.out.println("带三级结构的物模型转换完成，输出文件：" + OUTPUT_FILE_PATH);
    System.out.println("Icon保存路径：" + iconDir.getAbsolutePath());
  }

  /** 递归构建分类节点（仅最后一级渲染物模型，同时下载icon） */
  private JSONObject buildCategoryNode(
      JSONObject currentDevice, Map<Integer, JSONObject> deviceMap, File iconDir) {
    JSONObject node = new JSONObject();
    String categoryKey = currentDevice.getStr("CategoryKey");
    // 分类基础信息
    node.put("categoryId", currentDevice.getInt("Id"));
    node.put("categoryKey", categoryKey);
    node.put("categoryName", currentDevice.getStr("CategoryName"));
    node.put("parentId", currentDevice.getInt("ParentId"));
    node.put("listOrder", currentDevice.getInt("ListOrder"));

    // 下载Icon（IconUrl和IconUrlGrid）
    downloadIcon(currentDevice.getStr("IconUrl"), categoryKey + "_icon.png", iconDir);
    downloadIcon(currentDevice.getStr("IconUrlGrid"), categoryKey + "_grid.png", iconDir);

    // 递归添加子节点（二级/三级分类）
    JSONArray children = new JSONArray();
    for (JSONObject device : deviceMap.values()) {
      if (device.getInt("ParentId") == currentDevice.getInt("Id")) {
        children.add(buildCategoryNode(device, deviceMap, iconDir));
      }
    }
    node.put("children", children);

    // 仅最后一级分类（无子节点）渲染物模型
    if (children.isEmpty()) {
      String modelTemplate = currentDevice.getStr("ModelTemplate");
      if (!JSONUtil.isNull(modelTemplate) && !modelTemplate.isEmpty()) {
        JSONObject tencentModel = JSONUtil.parseObj(modelTemplate);
        JSONArray properties =
            convertProperties(
                tencentModel.getJSONArray("properties"), currentDevice.getStr("CategoryName"));
        JSONArray events =
            convertEvents(tencentModel.getJSONArray("events"), currentDevice.getStr("CategoryName"));
        JSONArray functions =
            convertActions(
                tencentModel.getJSONArray("actions"), currentDevice.getStr("CategoryName"));

        // 补充通用事件和功能
        addCommonEvents(events);
        addCommonFunction(functions);
        node.put("properties", properties);
        node.put("events", events);
        node.put("functions", functions);
      } else {
        node.put("properties", new JSONArray());
        node.put("events", new JSONArray());
        node.put("functions", new JSONArray());
      }
    } else {
      // 上级分类物模型字段为空
      node.put("properties", new JSONArray());
      node.put("events", new JSONArray());
      node.put("functions", new JSONArray());
    }

    return node;
  }

  /** 下载icon到指定文件夹 */
  private void downloadIcon(String iconUrl, String fileName, File saveDir) {
    if (JSONUtil.isNull(iconUrl) || iconUrl.isEmpty()) {
      return;
    }
    try {
      File iconFile = new File(saveDir, fileName);
      // 下载图片并保存
      HttpUtil.downloadFile(iconUrl, iconFile);
      System.out.println("已下载icon：" + fileName);
    } catch (Exception e) {
      System.err.println("icon下载失败：" + iconUrl + "，原因：" + e.getMessage());
    }
  }

  // 以下为原有转换逻辑（未修改）
  private JSONArray convertProperties(JSONArray tencentProps, String categoryName) {
    JSONArray nexiotProps = new JSONArray();
    if (JSONUtil.isNull(tencentProps)) return nexiotProps;

    for (Object propObj : tencentProps) {
      JSONObject tencentProp = JSONUtil.parseObj(propObj);
      JSONObject nexiotProp = new JSONObject();
      nexiotProp.put("id", tencentProp.getStr("id"));
      nexiotProp.put("name", tencentProp.getStr("name"));
      nexiotProp.put("mode", tencentProp.getStr("mode", "r"));
      nexiotProp.put("config", false);
      nexiotProp.put("source", "device");
      nexiotProp.put("description", tencentProp.getStr("desc", ""));

      JSONObject tencentDefine = tencentProp.getJSONObject("define");
      JSONObject valueType = new JSONObject();
      valueType.put("type", mapValueType(tencentDefine.getStr("type")));

      if ("enum".equals(tencentDefine.getStr("type")) && tencentDefine.containsKey("mapping")) {
        JSONArray elements = new JSONArray();
        JSONObject mapping = tencentDefine.getJSONObject("mapping");
        mapping.forEach(
            (key, val) -> {
              JSONObject element = new JSONObject();
              element.put("value", key);
              element.put("text", val.toString());
              elements.add(element);
            });
        valueType.put("elements", elements);
      }

      JSONObject expands = new JSONObject();
      if (tencentDefine.containsKey("unit")) valueType.put("unit", tencentDefine.getStr("unit"));
      if (tencentDefine.containsKey("step")) expands.put("precision", tencentDefine.getStr("step"));
      if (tencentDefine.containsKey("min")) expands.put("min", tencentDefine.getStr("min"));
      if (tencentDefine.containsKey("max")) expands.put("max", tencentDefine.getStr("max"));
      if (!expands.isEmpty()) valueType.put("expands", expands);

      nexiotProp.put("valueType", valueType);
      nexiotProps.add(nexiotProp);
    }
    return nexiotProps;
  }

  private JSONArray convertEvents(JSONArray tencentEvents, String categoryName) {
    JSONArray nexiotEvents = new JSONArray();
    if (JSONUtil.isNull(tencentEvents)) return nexiotEvents;

    for (Object eventObj : tencentEvents) {
      JSONObject tencentEvent = JSONUtil.parseObj(eventObj);
      JSONObject nexiotEvent = new JSONObject();
      nexiotEvent.put("id", tencentEvent.getStr("id"));
      nexiotEvent.put("name", tencentEvent.getStr("name"));
      nexiotEvent.put("config", false);
      nexiotEvent.put("description", tencentEvent.getStr("desc", ""));

      JSONObject expands = new JSONObject();
      String eventType = tencentEvent.getStr("type");
      switch (eventType) {
        case "alert":
          expands.put("level", "warn");
          break;
        case "fault":
          expands.put("level", "urgent");
          break;
        default:
          expands.put("level", "info");
      }
      nexiotEvent.put("expands", expands);

      JSONObject valueType = new JSONObject();
      JSONArray params = tencentEvent.getJSONArray("params");
      if (!JSONUtil.isNull(params) && params.size() > 0) {
        JSONObject firstParam = JSONUtil.parseObj(params.get(0));
        JSONObject paramDefine = firstParam.getJSONObject("define");
        valueType.put("type", mapValueType(paramDefine.getStr("type")));
      } else {
        valueType.put("type", "string");
      }
      nexiotEvent.put("valueType", valueType);
      nexiotEvents.add(nexiotEvent);
    }
    return nexiotEvents;
  }

  private JSONArray convertActions(JSONArray tencentActions, String categoryName) {
    JSONArray nexiotFunctions = new JSONArray();
    if (JSONUtil.isNull(tencentActions)) return nexiotFunctions;

    for (Object actionObj : tencentActions) {
      JSONObject tencentAction = JSONUtil.parseObj(actionObj);
      JSONObject nexiotFunction = new JSONObject();
      nexiotFunction.put("id", tencentAction.getStr("id"));
      nexiotFunction.put("name", tencentAction.getStr("name"));
      nexiotFunction.put("config", false);
      nexiotFunction.put("description", tencentAction.getStr("desc", ""));

      JSONArray inputs = new JSONArray();
      JSONArray tencentInputs = tencentAction.getJSONArray("input");
      if (!JSONUtil.isNull(tencentInputs)) {
        for (Object inputObj : tencentInputs) {
          JSONObject tencentInput = JSONUtil.parseObj(inputObj);
          JSONObject input = new JSONObject();
          input.put("id", tencentInput.getStr("id"));
          input.put("name", tencentInput.getStr("name"));
          input.put("valueType", buildValueType(tencentInput.getJSONObject("define")));
          inputs.add(input);
        }
      }
      nexiotFunction.put("inputs", inputs);

      JSONObject output = new JSONObject();
      JSONArray tencentOutputs = tencentAction.getJSONArray("output");
      if (!JSONUtil.isNull(tencentOutputs) && tencentOutputs.size() > 0) {
        JSONObject tencentOutput = JSONUtil.parseObj(tencentOutputs.get(0));
        output.put("id", tencentOutput.getStr("id"));
        output.put("name", tencentOutput.getStr("name"));
        output.put("valueType", buildValueType(tencentOutput.getJSONObject("define")));
      }
      nexiotFunction.put("output", output);

      nexiotFunctions.add(nexiotFunction);
    }
    return nexiotFunctions;
  }

  private JSONObject buildValueType(JSONObject define) {
    JSONObject valueType = new JSONObject();
    if (JSONUtil.isNull(define)) {
      valueType.put("type", "string");
      return valueType;
    }
    String type = define.getStr("type");
    valueType.put("type", mapValueType(type));
    if ("enum".equals(type) && define.containsKey("mapping")) {
      JSONArray elements = new JSONArray();
      JSONObject mapping = define.getJSONObject("mapping");
      mapping.forEach(
          (key, val) -> {
            JSONObject element = new JSONObject();
            element.put("value", key);
            element.put("text", val.toString());
            elements.add(element);
          });
      valueType.put("elements", elements);
    }
    if (define.containsKey("unit")) {
      valueType.put("unit", define.getStr("unit"));
    }
    return valueType;
  }

  private String mapValueType(String tencentType) {
    switch (tencentType) {
      case "bool":
        return "boolean";
      case "int":
        return "int";
      case "float":
        return "float";
      case "double":
        return "double";
      case "timestamp":
        return "string";
      case "struct":
        return "object";
      case "array":
        return "array";
      default:
        return tencentType;
    }
  }

  private void addCommonEvents(JSONArray nexiotEvents) {
    // 上线事件
    JSONObject onlineEvent = new JSONObject();
    onlineEvent.put("id", "online");
    onlineEvent.put("name", "上线");
    onlineEvent.put("config", false);
    JSONObject onlineExpands = new JSONObject();
    onlineExpands.put("level", "urgent");
    onlineEvent.put("expands", onlineExpands);
    JSONObject onlineValueType = new JSONObject();
    onlineValueType.put("type", "string");
    onlineEvent.put("valueType", onlineValueType);
    nexiotEvents.add(onlineEvent);

    // 下线事件
    JSONObject offlineEvent = new JSONObject();
    offlineEvent.put("id", "offline");
    offlineEvent.put("name", "下线");
    offlineEvent.put("config", false);
    JSONObject offlineValueType = new JSONObject();
    offlineValueType.put("type", "string");
    offlineEvent.put("valueType", offlineValueType);
    nexiotEvents.add(offlineEvent);
  }

  private void addCommonFunction(JSONArray nexiotFunctions) {
    // 公共功能：设置期望值
    JSONObject setDesiredPropFunc = new JSONObject();
    setDesiredPropFunc.put("id", "setDeviceDesiredProperties");
    setDesiredPropFunc.put("name", "设置期望值");
    setDesiredPropFunc.put("config", true);
    setDesiredPropFunc.put("description", "对所有可写的属性设置期望值，可用于标签、位置、个性化扩展使用");
    setDesiredPropFunc.put("inputs", new JSONArray());
    JSONObject output = new JSONObject();
    setDesiredPropFunc.put("output", output);
    nexiotFunctions.add(setDesiredPropFunc);
  }
}