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

package cn.universal.core.metadata.types.codec;

import cn.universal.core.metadata.types.FileType;
import java.util.Map;
import java.util.Optional;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FileCodec extends AbstractValueTypeCodec<FileType> {

  @Override
  public String getTypeId() {
    return FileType.ID;
  }

  @Override
  public FileType decode(FileType type, Map<String, Object> config) {
    super.decode(type, config);

    Optional.ofNullable(config.get("bodyType"))
        .map(String::valueOf)
        .flatMap(FileType.BodyType::of)
        .ifPresent(type::setBodyType);

    return type;
  }

  @Override
  protected void doEncode(Map<String, Object> encoded, FileType type) {
    super.doEncode(encoded, type);
    encoded.put("bodyType", type.getBodyType().name());
  }
}
