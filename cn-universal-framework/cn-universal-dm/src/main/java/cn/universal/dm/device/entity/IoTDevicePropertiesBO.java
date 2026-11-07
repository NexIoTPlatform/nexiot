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

package cn.universal.dm.device.entity;

import static java.util.Optional.ofNullable;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.universal.core.metadata.AbstractPropertyMetadata;
import cn.universal.core.metadata.ValueType;
import cn.universal.core.metadata.types.ArrayType;
import cn.universal.core.metadata.types.BooleanType;
import cn.universal.core.metadata.types.DateTimeType;
import cn.universal.core.metadata.types.EnumType;
import cn.universal.core.metadata.types.GeoPoint;
import cn.universal.core.metadata.types.GeoType;
import cn.universal.core.metadata.types.NumberType;
import cn.universal.core.metadata.types.ObjectType;
import cn.universal.core.metadata.types.StringType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.Date;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Slf4j
@Schema(description = "IoT设备属性业务对象")
public class IoTDevicePropertiesBO {

  //  private String id;

  @Schema(description = "IoT ID")
  private String iotId;

  @Schema(description = "设备ID")
  private String deviceId;

  @Schema(description = "属性标识")
  private String property;

  @Schema(description = "属性名称")
  private String propertyName;

  @Schema(description = "字符串值")
  private String stringValue;

  @Schema(description = "期望值")
  private Object desireValue;

  @Schema(description = "格式化值")
  private String formatValue;

  @Schema(description = "期望格式化值")
  private String desireFormatValue;

  @Schema(description = "数值")
  private BigDecimal numberValue;

  @Schema(description = "地理位置值")
  private GeoPoint geoValue;

  /** 最后更新时间 */
  @Schema(description = "时间戳")
  private long timestamp;

  @Schema(description = "对象值")
  private Object objectValue;

  @Schema(description = "原值")
  private String value;

  @Schema(description = "时间值")
  private Date timeValue;

  @Schema(description = "类型")
  private String type;

  @Schema(description = "单位符号")
  private String symbol;

  @Schema(description = "自定义字段")
  private String customized;

  // 是否设置存储策略
  @Schema(description = "是否设置存储策略")
  private boolean storagePolicy;

  public Map<String, Object> toMap() {
    return new JSONObject(this);
  }

  public IoTDevicePropertiesBO withValue(AbstractPropertyMetadata metadata, Object value) {
    if (metadata == null) {
      setValue(valueText(value));
      if (value instanceof Number) {
        numberValue = new BigDecimal(value.toString());
      } else if (value instanceof Date) {
        timeValue = ((Date) value);
      }
      return this;
    }
    setProperty(metadata.getId());
    setPropertyName(metadata.getName());
    return withValue(metadata.getValueType(), value);
  }

  public IoTDevicePropertiesBO withValue(ValueType type, Object value) {
    if (value == null) {
      return this;
    }
    setType(type.getType());
    String convertedValue;

    if (type instanceof NumberType) {
      NumberType<?> numberType = (NumberType<?>) type;
      Number number = numberType.convertNumber(value);
      if (number == null) {
        log.error("无法将" + value + "转为" + type.getId());
        //        throw new UnsupportedOperationException("无法将" + value + "转为" + type.getId());
        return this;
      }
      setSymbol(numberType.getUnit() != null ? numberType.getUnit().getSymbol() : "");
      convertedValue = valueText(number);
      BigDecimal numberVal;
      if (number instanceof BigDecimal) {
        numberVal = ((BigDecimal) number);
      } else if (number instanceof Integer) {
        numberVal = BigDecimal.valueOf(number.intValue());
      } else if (number instanceof Long) {
        numberVal = BigDecimal.valueOf(number.longValue());
      } else {
        numberVal = BigDecimal.valueOf(number.doubleValue());
      }
      setNumberValue(numberVal);
    } else if (type instanceof DateTimeType) {
      DateTimeType dateTimeType = (DateTimeType) type;
      convertedValue = valueText(value);
      setTimeValue(dateTimeType.convert(value));
    } else if (type instanceof ObjectType) {
      ObjectType objectType = (ObjectType) type;
      Object val = objectType.convert(value);
      convertedValue = JSONUtil.toJsonStr(val);
      setObjectValue(val);
    } else if (type instanceof ArrayType) {
      ArrayType objectType = (ArrayType) type;
      Object val = objectType.convert(value);
      convertedValue = JSONUtil.toJsonStr(val);
      setObjectValue(val);
    } else if (type instanceof GeoType) {
      GeoType geoType = (GeoType) type;
      GeoPoint val = geoType.convert(value);
      convertedValue = valueText(val);
      setGeoValue(val);
    } else if (type instanceof StringType) {
      StringType stringType = ((StringType) type);
      setStringValue(convertedValue = valueText(value));
      setSymbol(stringType.getUnit() != null ? stringType.getUnit().getSymbol() : "");
    } else if (type instanceof EnumType) {
      EnumType enumType = (EnumType) type;
      setFormatValue(enumType.format(value));
      setStringValue(convertedValue = valueText(value));
    } else if (type instanceof BooleanType) {
      BooleanType booleanType = (BooleanType) type;
      setFormatValue(booleanType.format(value));
      setStringValue(convertedValue = valueText(value));
    } else {
      setStringValue(convertedValue = valueText(value));
    }
    setValue(convertedValue);
    ofNullable(type.format(value)).map(IoTDevicePropertiesBO::valueText).ifPresent(this::setFormatValue);

    return this;
  }

  public IoTDevicePropertiesBO withDesire(ValueType type, Object value) {
    if (value == null) {
      return this;
    }
    ofNullable(type.format(value)).map(IoTDevicePropertiesBO::valueText).ifPresent(this::setDesireFormatValue);
    return this;
  }

  private static String valueText(Object value) {
    if (value instanceof BigDecimal decimal) {
      return decimal.toPlainString();
    }
    return String.valueOf(value);
  }
}
