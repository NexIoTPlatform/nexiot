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
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public class ValueTypes {

  private static final Map<String, Supplier<ValueType>> supports = new ConcurrentHashMap<>();

  static {
    supports.put(ArrayType.ID, ArrayType::new);
    supports.put(BooleanType.ID, BooleanType::new);
    supports.put(DateTimeType.ID, DateTimeType::new);
    supports.put(DoubleType.ID, DoubleType::new);
    supports.put(EnumType.ID, EnumType::new);
    supports.put(FloatType.ID, FloatType::new);
    supports.put(IntType.ID, IntType::new);
    supports.put(LongType.ID, LongType::new);
    supports.put(ObjectType.ID, ObjectType::new);

    supports.put(StringType.ID, StringType::new);
    supports.put("text", StringType::new);

    supports.put(GeoType.ID, GeoType::new);
    supports.put(FileType.ID, FileType::new);
    supports.put(PasswordType.ID, PasswordType::new);
    supports.put(GeoShapeType.ID, GeoShapeType::new);
  }

  public static void register(String id, Supplier<ValueType> supplier) {
    supports.put(id, supplier);
  }

  public static Supplier<ValueType> lookup(String id) {
    if (id == null) {
      return null;
    }
    Supplier<ValueType> value = supports.get(id);
    if (value != null) {
      return value;
    }
    return supports.get(id);
  }
}
