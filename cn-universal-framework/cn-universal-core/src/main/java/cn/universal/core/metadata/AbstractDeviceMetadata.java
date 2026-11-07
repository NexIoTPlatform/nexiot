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

package cn.universal.core.metadata;

import java.util.List;
import java.util.Optional;

/**
 * 基础设备物模型定义
 *
 * @author zhouhao
 * @since 1.1
 */
public interface AbstractDeviceMetadata extends Metadata, Jsonable {

  /**
   * @return 所有属性定义
   */
  List<AbstractPropertyMetadata> getProperties();

  /**
   * @return 所有功能定义
   */
  List<AbstractFunctionMetadata> getFunctions();

  /**
   * @return 事件定义
   */
  List<AbstractEventMetadata> getEvents();

  /**
   * @return 标签定义
   */
  List<AbstractPropertyMetadata> getTags();

  default Optional<AbstractEventMetadata> getEvent(String id) {
    return Optional.ofNullable(getEventOrNull(id));
  }

  AbstractEventMetadata getEventOrNull(String id);

  default Optional<AbstractPropertyMetadata> getProperty(String id) {
    return Optional.ofNullable(getPropertyOrNull(id));
  }

  AbstractPropertyMetadata getPropertyOrNull(String id);

  default Optional<AbstractFunctionMetadata> getFunction(String id) {
    return Optional.ofNullable(getFunctionOrNull(id));
  }

  AbstractFunctionMetadata getFunctionOrNull(String id);

  default Optional<AbstractPropertyMetadata> getTag(String id) {
    return Optional.ofNullable(getTagOrNull(id));
  }

  AbstractPropertyMetadata getTagOrNull(String id);
}
