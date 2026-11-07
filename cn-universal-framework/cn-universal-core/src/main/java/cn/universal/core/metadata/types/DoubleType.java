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

import static java.math.BigDecimal.ROUND_HALF_UP;

import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@SuppressWarnings("all")
public class DoubleType extends NumberType<Double> {

  public static final String ID = "double";

  private Integer scale;

  public static final DoubleType GLOBAL = new DoubleType();

  @Override
  public Object format(Object value) {
    Number val = convertNumber(value);
    if (val == null) {
      return super.format(value);
    }
    int scale = this.scale == null ? 2 : this.scale;
    String scaled = new BigDecimal(val.toString()).setScale(scale, ROUND_HALF_UP).toString();
    return super.format(scaled);
  }

  public DoubleType scale(Integer scale) {
    this.scale = scale;
    return this;
  }

  @Override
  public Double convert(Object value) {
    return super.convertNumber(value, Number::doubleValue);
  }

  @Override
  public String getId() {
    return ID;
  }

  @Override
  public String getName() {
    return "双精度浮点数";
  }
}
