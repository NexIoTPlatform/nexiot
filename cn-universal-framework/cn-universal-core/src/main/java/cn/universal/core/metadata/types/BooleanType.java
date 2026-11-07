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
import cn.universal.core.metadata.FormatSupport;
import cn.universal.core.metadata.ValidateResult;
import cn.universal.core.metadata.ValueType;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BooleanType extends AbstractType<BooleanType>
    implements ValueType, FormatSupport, Converter<Boolean> {

  public static final String ID = "boolean";

  public static final BooleanType GLOBAL = new BooleanType();

  private String trueText = "是";

  private String falseText = "否";

  private String trueValue = "true";

  private String falseValue = "false";

  public BooleanType trueText(String trueText) {
    this.trueText = trueText;
    return this;
  }

  public BooleanType falseText(String falseText) {
    this.falseText = falseText;
    return this;
  }

  public BooleanType trueValue(String trueValue) {
    this.trueValue = trueValue;
    return this;
  }

  public BooleanType falseValue(String falseValue) {
    this.falseText = falseValue;
    return this;
  }

  @Override
  public String getId() {
    return ID;
  }

  @Override
  public String getName() {
    return "布尔值";
  }

  public Boolean convert(Object value) {
    if (value instanceof Boolean) {
      return ((Boolean) value);
    }

    String stringVal = String.valueOf(value).trim();
    if (stringVal.equals(trueValue) || stringVal.equals(trueText)) {
      return true;
    }

    if (stringVal.equals(falseValue) || stringVal.equals(falseText)) {
      return false;
    }
    return stringVal.equals("1")
        || stringVal.equals("true")
        || stringVal.equals("y")
        || stringVal.equals("yes")
        || stringVal.equals("ok")
        || stringVal.equals("是")
        || stringVal.equals("正常");
  }

  @Override
  public ValidateResult validate(Object value) {

    Boolean trueOrFalse = convert(value);

    return trueOrFalse == null
        ? ValidateResult.fail("不支持的值:" + value)
        : ValidateResult.success(trueOrFalse);
  }

  @Override
  public String format(Object value) {
    Boolean trueOrFalse = convert(value);

    if (Boolean.TRUE.equals(trueOrFalse)) {
      return trueText;
    }
    if (Boolean.FALSE.equals(trueOrFalse)) {
      return falseText;
    }
    return "未知:" + value;
  }
}
