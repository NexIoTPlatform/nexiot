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

import cn.universal.core.metadata.ValueType;
import java.util.HashMap;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;
import org.springframework.util.CollectionUtils;

@Getter
@Setter
@SuppressWarnings("all")
public abstract class AbstractType<R> implements ValueType {

  private Map<String, Object> expands;

  private String description;

  public R expands(Map<String, Object> expands) {
    if (CollectionUtils.isEmpty(expands)) {
      return (R) this;
    }
    if (this.expands == null) {
      this.expands = new HashMap<>();
    }
    this.expands.putAll(expands);
    return (R) this;
  }

  public R expand(String configKey, Object value) {

    if (value == null) {
      return (R) this;
    }
    if (expands == null) {
      expands = new HashMap<>();
    }
    expands.put(configKey, value);
    return (R) this;
  }

  public R description(String description) {
    this.description = description;
    return (R) this;
  }
}
