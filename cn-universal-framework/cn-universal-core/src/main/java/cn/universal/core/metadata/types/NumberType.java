/*
 * Copyright 2019-2024 JetLinks Community
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cn.universal.core.metadata.types;

import cn.universal.core.metadata.Converter;
import cn.universal.core.metadata.UnitSupported;
import cn.universal.core.metadata.ValidateResult;
import cn.universal.core.metadata.ValueType;
import cn.universal.core.metadata.unit.ValueUnit;
import java.math.BigDecimal;
import java.util.Date;
import java.util.Optional;
import java.util.function.Function;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public abstract class NumberType<N extends Number> extends AbstractType<NumberType<N>>
    implements UnitSupported, ValueType, Converter<N> {

  // 最大值
  private Number max;

  // 最小值
  private Number min;

  // 单位
  private ValueUnit unit;

  public NumberType<N> unit(ValueUnit unit) {
    this.unit = unit;
    return this;
  }

  public NumberType<N> max(Number max) {
    this.max = max;
    return this;
  }

  public NumberType<N> min(Number min) {
    this.min = min;
    return this;
  }

  public Object format(Object value) {
    if (value == null) {
      return null;
    }
    ValueUnit unit = getUnit();
    if (unit == null) {
      return value;
    }
    return unit.format(value);
  }

  @Override
  public ValidateResult validate(Object value) {
    try {
      N numberValue = convert(value);
      if (numberValue == null) {
        return ValidateResult.fail("数字格式错误:" + value);
      }
      if (max != null && numberValue.doubleValue() > max.doubleValue()) {
        return ValidateResult.fail("超过最大值:" + max);
      }
      if (min != null && numberValue.doubleValue() < min.doubleValue()) {
        return ValidateResult.fail("小于最小值:" + min);
      }
      return ValidateResult.success(numberValue);
    } catch (NumberFormatException e) {
      return ValidateResult.fail(e.getMessage());
    }
  }

  public N convertNumber(Object value, Function<Number, N> mapper) {
    return Optional.ofNullable(convertNumber(value)).map(mapper).orElse(null);
  }

  public Number convertNumber(Object value) {
    if (value instanceof Number) {
      return ((Number) value);
    }
    if (value instanceof String) {
      try {
        return new BigDecimal(((String) value));
      } catch (NumberFormatException e) {
        return null;
      }
    }
    if (value instanceof Date) {
      return ((Date) value).getTime();
    }
    return null;
  }

  public abstract N convert(Object value);

  public long getMax(long defaultVal) {
    return Optional.ofNullable(getMax()).map(Number::longValue).orElse(defaultVal);
  }

  public long getMin(long defaultVal) {
    return Optional.ofNullable(getMin()).map(Number::longValue).orElse(defaultVal);
  }

  public double getMax(double defaultVal) {
    return Optional.ofNullable(getMax()).map(Number::doubleValue).orElse(defaultVal);
  }

  public double getMin(double defaultVal) {
    return Optional.ofNullable(getMin()).map(Number::doubleValue).orElse(defaultVal);
  }
}
