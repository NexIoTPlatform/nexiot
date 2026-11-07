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
import cn.universal.core.metadata.FormatSupport;
import cn.universal.core.metadata.ValidateResult;
import cn.universal.core.metadata.ValueType;
import java.util.HashMap;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GeoType extends AbstractType<GeoType>
    implements ValueType, FormatSupport, Converter<GeoPoint> {

  public static final String ID = "geoPoint";

  public static final GeoType GLOBAL = new GeoType();

  // 经度字段
  private String latProperty = "lat";

  // 纬度字段
  private String lonProperty = "lon";

  public GeoType latProperty(String property) {
    this.latProperty = property;
    return this;
  }

  public GeoType lonProperty(String property) {
    this.lonProperty = property;
    return this;
  }

  @Override
  public String getId() {
    return ID;
  }

  @Override
  public String getName() {
    return "地理位置";
  }

  public Map<String, Object> convertToMap(Object value) {
    Map<String, Object> mapGeoPoint = new HashMap<>();

    GeoPoint point = convert(value);
    if (point != null) {
      mapGeoPoint.put("lat", point.getLat());
      mapGeoPoint.put("lon", point.getLon());
    }
    return mapGeoPoint;
  }

  public GeoPoint convert(Object value) {
    return GeoPoint.of(value);
  }

  @Override
  public ValidateResult validate(Object value) {

    GeoPoint geoPoint = convert(value);

    return geoPoint == null
        ? ValidateResult.fail("不支持的Geo格式:" + value)
        : ValidateResult.success(geoPoint);
  }

  @Override
  public String format(Object value) {
    GeoPoint geoPoint = convert(value);

    return String.valueOf(geoPoint);
  }
}
