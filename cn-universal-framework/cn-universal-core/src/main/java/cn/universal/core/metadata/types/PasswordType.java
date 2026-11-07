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
import cn.universal.core.metadata.ValidateResult;
import cn.universal.core.metadata.ValueType;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PasswordType extends AbstractType<PasswordType>
    implements ValueType, Converter<String> {

  public static final String ID = "password";
  public static final PasswordType GLOBAL = new PasswordType();

  @Override
  public String getId() {
    return ID;
  }

  @Override
  public String getName() {
    return "密码";
  }

  @Override
  public ValidateResult validate(Object value) {
    return ValidateResult.success(String.valueOf(value));
  }

  @Override
  public String format(Object value) {
    return String.valueOf(value);
  }

  @Override
  public String convert(Object value) {
    return value == null ? null : String.valueOf(value);
  }
}
