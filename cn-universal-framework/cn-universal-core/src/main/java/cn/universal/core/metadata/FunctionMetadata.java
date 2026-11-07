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

import cn.hutool.json.JSONObject;
import cn.universal.core.metadata.types.UnknownType;
import cn.universal.core.metadata.types.ValueTypes;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.Setter;

/**
 * @since 1.0.0
 */
public class FunctionMetadata implements AbstractFunctionMetadata {

  @JsonIgnore private transient JSONObject jsonObject;

  @JsonIgnore private transient AbstractFunctionMetadata another;

  @Setter private List<AbstractPropertyMetadata> inputs;

  @Setter private ValueType output;

  @Getter @Setter private String id;

  @Getter @Setter private String name;

  @Getter @Setter private String description;

  @Getter @Setter private boolean async;

  @Getter @Setter private Map<String, Object> expands;

  public FunctionMetadata() {}

  public FunctionMetadata(JSONObject jsonObject) {
    fromJson(jsonObject);
  }

  public FunctionMetadata(AbstractFunctionMetadata another) {
    this.id = another.getId();
    this.name = another.getName();
    this.description = another.getDescription();
    this.expands = another.getExpands() == null ? null : new HashMap<>(another.getExpands());
    this.another = another;
    this.async = another.isAsync();
  }

  @Override
  public List<AbstractPropertyMetadata> getInputs() {
    if (inputs == null) {
      if (jsonObject != null) {
        inputs =
            Optional.ofNullable(jsonObject.getJSONArray("inputs"))
                .map(Collection::stream)
                .map(
                    stream ->
                        stream
                            .map(JSONObject.class::cast)
                            .map(PropertyMetadata::new)
                            .map(AbstractPropertyMetadata.class::cast)
                            .collect(Collectors.toList()))
                .orElse(Collections.emptyList());
      } else if (another != null) {
        inputs =
            another.getInputs().stream().map(PropertyMetadata::new).collect(Collectors.toList());
      } else {
        inputs = Collections.emptyList();
      }
    }
    return inputs;
  }

  @Override
  public ValueType getOutput() {
    if (output == null) {
      if (jsonObject != null) {
        output =
            Optional.ofNullable(jsonObject.getJSONObject("output"))
                .flatMap(
                    conf ->
                        Optional.ofNullable(ValueTypes.lookup(conf.getStr("type")))
                            .map(Supplier::get)
                            .map(type -> ValueTypeCodecs.decode(type, conf)))
                .orElseGet(UnknownType::new);
      } else if (another != null) {
        output = another.getOutput();
      } else {
        output = new UnknownType();
      }
    }
    return output;
  }

  @Override
  public String toString() {
    // /*获取系统信息*/ getSysInfo(Type name,)

    return String.join(
        "",
        new String[] {
          "/* ",
          getName(),
          " */",
          getId(),
          "(",
          String.join(
              ",",
              getInputs().stream().map(AbstractPropertyMetadata::toString).toArray(String[]::new)),
          ")"
        });
  }

  @Override
  public JSONObject toJson() {
    JSONObject json = new JSONObject();
    json.put("id", id);
    json.put("name", name);
    json.put("description", description);
    json.put("async", async);
    json.put("inputs", getInputs().stream().map(Jsonable::toJson).collect(Collectors.toList()));
    ValueTypeCodecs.encode(getOutput()).ifPresent(ot -> json.set("output", ot));
    json.put("expands", expands);

    return json;
  }

  @Override
  public void fromJson(JSONObject json) {
    if (json != null) {
      this.jsonObject = json;
      this.inputs = null;
      this.output = null;
      this.id = json.getStr("id");
      this.name = json.getStr("name");
      this.description = json.getStr("description", "");
      this.async = json.getBool("async", false);
      this.expands = json.getJSONObject("expands");
    }
  }
}
