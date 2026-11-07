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
import java.util.Optional;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FileType extends AbstractType<FileType> implements ValueType, Converter<String> {

  public static final String ID = "file";

  private BodyType bodyType = BodyType.url;

  @Override
  public String getId() {
    return ID;
  }

  @Override
  public String getName() {
    return "文件";
  }

  public FileType bodyType(BodyType type) {
    this.bodyType = type;
    return this;
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

  public enum BodyType {
    url,
    base64,
    binary;

    public static Optional<BodyType> of(String name) {
      if (name == null) {
        return Optional.empty();
      }
      for (BodyType value : values()) {
        if (value.name().equalsIgnoreCase(name)) {
          return Optional.of(value);
        }
      }
      return Optional.empty();
    }
  }
}
