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

import cn.universal.core.metadata.ValidateResult;
import cn.universal.core.metadata.ValueType;

/**
 * @since 1.0.0
 */
public class UnknownType implements ValueType {

  @Override
  public ValidateResult validate(Object value) {
    return ValidateResult.success();
  }

  @Override
  public String getId() {
    return "unknown";
  }

  @Override
  public String getName() {
    return "未知类型";
  }

  @Override
  public String getDescription() {
    return "未知类型";
  }

  @Override
  public String format(Object value) {
    return String.valueOf(value);
  }
}
