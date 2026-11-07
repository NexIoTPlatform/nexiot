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

public class GeoShapeType extends AbstractType<GeoShapeType> implements Converter<GeoShape> {

  public static final String ID = "geoShape";
  public static final GeoShapeType GLOBAL = new GeoShapeType();

  @Override
  public ValidateResult validate(Object value) {
    GeoShape shape;
    if (null == (shape = convert(value))) {
      return ValidateResult.builder().success(false).errorMsg("不支持的GepShape格式:" + value).build();
    }
    return ValidateResult.success(shape);
  }

  @Override
  public GeoShape convert(Object value) {

    return GeoShape.of(value);
  }

  @Override
  public String getId() {
    return "geoShape";
  }

  @Override
  public String getName() {
    return "地理地形";
  }

  @Override
  public String getDescription() {
    return null;
  }

  @Override
  public Object format(Object value) {
    return value;
  }
}
