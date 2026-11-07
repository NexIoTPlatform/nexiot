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

package cn.universal.core.metadata.unit;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

public class ValueUnits {

  private static final List<ValueUnitSupplier> suppliers = new CopyOnWriteArrayList<>();

  static {
    ValueUnits.register(
        new ValueUnitSupplier() {
          @Override
          public Optional<ValueUnit> getById(String id) {
            return Optional.ofNullable(UnifyUnit.of(id));
          }

          @Override
          public List<ValueUnit> getAll() {
            return Arrays.asList(UnifyUnit.values());
          }
        });
  }

  public static void register(ValueUnitSupplier supplier) {
    suppliers.add(supplier);
  }

  public static Optional<ValueUnit> lookup(String id) {
    for (ValueUnitSupplier supplier : suppliers) {
      Optional<ValueUnit> unit = supplier.getById(id);
      if (unit.isPresent()) {
        return unit;
      }
    }
    // json ?
    if (id.startsWith("{")) {
      return Optional.ofNullable(JsonValueUnit.of(id));
    }
    return Optional.of(SymbolValueUnit.of(id));
  }

  public static List<ValueUnit> getAllUnit() {
    return suppliers.stream()
        .map(ValueUnitSupplier::getAll)
        .flatMap(Collection::stream)
        .collect(Collectors.toList());
  }
}
