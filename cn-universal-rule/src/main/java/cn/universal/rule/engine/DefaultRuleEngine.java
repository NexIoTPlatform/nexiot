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

package cn.universal.rule.engine;

import cn.hutool.json.JSONObject;
import cn.universal.common.exception.IoTException;
import cn.universal.rule.enums.ParserFormat;
import cn.universal.rule.express.ExpressTemplate;
import cn.universal.rule.function.RuleFunctionTemplate;
import cn.universal.rule.model.RuleParserResult;
import cn.universal.rule.model.RuleParserResult.RuleField;
import cn.universal.rule.parser.RuleParser;
import cn.universal.rule.utils.JsonParserUtil;
import jakarta.annotation.Resource;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

/**
 * 默认规则引擎 @Author gitee.com/NexIoT
 *
 * @since 2025/12/3 9:36
 */
@Component
@Slf4j
public class DefaultRuleEngine implements RuleEngine {

  @Resource private RuleParser ruleParser;

  @Resource private ExpressTemplate expressTemplate;

  @Resource private RuleFunctionTemplate ruleFunctionTemplate;

  @Override
  public JSONObject executeRule(JSONObject param, String modelDefineString, String appId) {
    RuleParserResult model = ruleParser.parse(ParserFormat.sql, modelDefineString);

    if (CollectionUtils.isEmpty(model.getTopics())) {
      throw new IoTException("SQL解析错误：from不能为空", 10021);
    }
    String topic = model.getTopics().get(0);
    if ("*".equals(topic) || topic.equals(appId)) {
      return doExecuteRule(param, modelDefineString);
    }
    return null;
  }

  public JSONObject doExecuteRule(JSONObject param, String modelDefineString) {

    try {
      RuleParserResult model = ruleParser.parse(ParserFormat.sql, modelDefineString);
      if (StringUtils.isEmpty(model.getCondition())) {
        return convertParam(param, model.getFields());
      }
      Boolean executeResult = expressTemplate.executeTest(model.getCondition(), param);
      if (executeResult) {
        return convertParam(param, model.getFields());
      }
    } catch (Exception exception) {
      throw new IoTException("SQL解析错误:" + exception.getMessage());
    }
    return null;
  }

  private JSONObject convertParam(JSONObject param, List<RuleField> fields) {
    if ("*".equals(fields.get(0).getName().trim())) {
      return param;
    }
    JSONObject result = new JSONObject();
    fields.forEach(
        field -> {
          Object value;
          if (isConstant(field.getName())) {
            value = getConstant(field.getName());
            setConvertParamValue(
                result,
                Objects.nonNull(field.getAlias()) ? field.getAlias() : value.toString(),
                value);
            return;
          } else if (isFunction(field.getName())) {
            String functionName = parseFunctionName(field.getName());

            Object[] params =
                Stream.of(parseFunctionValue(field.getName()))
                    .map(v -> isConstant(v) ? getConstant(v) : JsonParserUtil.getValue(param, v))
                    .toArray();

            value = ruleFunctionTemplate.executeFunction(functionName, params);
          } else {
            value = JsonParserUtil.getValue(param, field.getName());
          }
          setConvertParamValue(
              result,
              Objects.nonNull(field.getAlias()) ? field.getAlias() : field.getName(),
              value);
        });

    return result;
  }

  public String getConstant(String filedName) {
    return filedName.substring(1, filedName.length() - 1);
  }

  public boolean isConstant(String filedName) {
    return filedName.startsWith("\"") && filedName.endsWith("\"");
  }

  public boolean isFunction(String filedName) {
    String pattern = ".*\\(.*\\)";
    return Pattern.matches(pattern, filedName);
  }

  public String parseFunctionName(String filedName) {
    return filedName.split("\\(")[0];
  }

  public String[] parseFunctionValue(String filedName) {
    return filedName.replaceAll("\\)", "").split("\\(")[1].split(",");
  }

  public static void main(String[] args) {
    //    JSONObject jsonObject = new JSONObject();
    //    jsonObject.set("a", "2");
    //    List<RuleField> fields = new ArrayList<>();
    //    fields.add(new RuleField("a", "a.b.c"));
    //    JSONObject jsonObject1 = new DefaultRuleEngine().convertParam(jsonObject, fields);
    //    System.out.println(jsonObject1);

    String[] strings = new DefaultRuleEngine().parseFunctionValue("adsadad(12312321)");

    System.out.println(strings[0]);
  }

  private void setConvertParamValue(JSONObject result, String name, Object value) {
    if (name.contains(".")) {
      String keyName = name.replaceAll("\"", "").replaceAll("'", "");
      String[] names = keyName.split("\\.");
      dealKey(result, names, 0, value);
    } else {
      result.set(name, value);
    }
  }

  private void dealKey(JSONObject tempObj, String[] names, int index, Object value) {
    if (index == names.length) {
      return;
    }
    if (tempObj.get(names[index]) != null) {
      if (index == names.length - 1) {
        tempObj.set(names[index], value);
        ++index;
      } else {
        ++index;
        dealKey(tempObj.getJSONObject(names[index - 1]), names, index, value);
      }
    } else {
      if (index == names.length - 1) {
        tempObj.set(names[index], value);
        ++index;
        return;
      } else {
        tempObj.set(names[index], new JSONObject());
        ++index;
      }
      dealKey(tempObj.getJSONObject(names[index - 1]), names, index, value);
    }
  }
}
