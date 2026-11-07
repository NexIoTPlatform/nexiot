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

/**
 * 功能元数据
 *
 * @author zhouhao
 * @since 1.1
 */
public interface AbstractFunctionMetadata extends Metadata, Jsonable {

  /**
   * @return 输入参数定义
   */
  List<AbstractPropertyMetadata> getInputs();

  /**
   * @return 输出类型，为null表示无输出
   */
  ValueType getOutput();

  /**
   * @return 是否异步
   */
  boolean isAsync();
}
