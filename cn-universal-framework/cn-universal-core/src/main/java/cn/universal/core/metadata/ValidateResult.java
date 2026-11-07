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

import java.util.function.Consumer;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 参数合法性校验
 *
 * @author zhouhao
 * @since 1.1
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ValidateResult {

  private boolean success;

  private Object value;

  private String errorMsg;

  public static ValidateResult success(Object value) {
    ValidateResult result = new ValidateResult();
    result.setSuccess(true);
    result.setValue(value);
    return result;
  }

  public static ValidateResult success() {
    ValidateResult result = new ValidateResult();
    result.setSuccess(true);
    return result;
  }

  public static ValidateResult fail(String message) {
    ValidateResult result = new ValidateResult();
    result.setSuccess(false);
    result.setErrorMsg(message);
    return result;
  }

  public Object assertSuccess() {
    if (!success) {
      throw new IllegalArgumentException(errorMsg);
    }
    return value;
  }

  public void ifFail(Consumer<ValidateResult> resultConsumer) {
    if (!success) {
      resultConsumer.accept(this);
    }
  }
}
