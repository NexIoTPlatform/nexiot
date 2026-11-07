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
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.Setter;

/**
 * @since 1.0.0
 */
public class DeviceMetadata implements AbstractDeviceMetadata {

  @JsonIgnore private JSONObject jsonObject;

  private volatile Map<String, AbstractPropertyMetadata> properties;

  private volatile Map<String, AbstractFunctionMetadata> functions;

  private volatile Map<String, AbstractEventMetadata> events;

  private volatile Map<String, AbstractPropertyMetadata> tags;

  @Getter @Setter private String id;

  @Getter @Setter private String name;

  @Getter @Setter private String description;

  @Setter private Map<String, Object> expands;

  /** 默认构造函数，用于Jackson反序列化 */
  public DeviceMetadata() {
    // 初始化空对象，用于Jackson反序列化
  }

  public DeviceMetadata(JSONObject jsonObject) {
    this.jsonObject = jsonObject;
    getProperties();
    getEvents();
    getFunctions();
    getTags();
  }

  public DeviceMetadata(AbstractDeviceMetadata another) {
    this.id = another.getId();
    this.name = another.getName();
    this.description = another.getDescription();
    this.expands = another.getExpands();
    this.properties =
        another.getProperties().stream()
            .map(PropertyMetadata::new)
            .filter(
                property ->
                    property.getId() != null && !property.getId().trim().isEmpty()) // 过滤掉id为null的项
            .collect(
                Collectors.toMap(
                    PropertyMetadata::getId, Function.identity(), (a, b) -> a, LinkedHashMap::new));

    this.functions =
        another.getFunctions().stream()
            .map(FunctionMetadata::new)
            .filter(
                function ->
                    function.getId() != null && !function.getId().trim().isEmpty()) // 过滤掉id为null的项
            .collect(
                Collectors.toMap(
                    FunctionMetadata::getId, Function.identity(), (a, b) -> a, LinkedHashMap::new));

    this.events =
        another.getEvents().stream()
            .map(EventMetadata::new)
            .filter(
                event -> event.getId() != null && !event.getId().trim().isEmpty()) // 过滤掉id为null的项
            .collect(
                Collectors.toMap(
                    EventMetadata::getId, Function.identity(), (a, b) -> a, LinkedHashMap::new));
  }

  // Jackson序列化用的getter方法
  @JsonProperty("properties")
  public Map<String, AbstractPropertyMetadata> getPropertiesMap() {
    if (properties == null) {
      getProperties(); // 初始化properties
    }
    return properties;
  }

  // Jackson反序列化用的setter方法
  @JsonProperty("properties")
  public void setPropertiesMap(Map<String, AbstractPropertyMetadata> properties) {
    this.properties = properties;
  }

  @Override
  public List<AbstractPropertyMetadata> getProperties() {
    if (properties == null && jsonObject != null) {
      properties =
          Optional.ofNullable(jsonObject.getJSONArray("properties"))
              .map(Collection::stream)
              .<Map<String, AbstractPropertyMetadata>>map(
                  stream ->
                      stream
                          .map(JSONObject.class::cast)
                          .map(PropertyMetadata::new)
                          .map(AbstractPropertyMetadata.class::cast)
                          .filter(
                              property ->
                                  property.getId() != null
                                      && !property.getId().trim().isEmpty()) // 过滤掉id为null的项
                          .collect(
                              Collectors.toMap(
                                  AbstractPropertyMetadata::getId,
                                  Function.identity(),
                                  (a, b) -> a,
                                  LinkedHashMap::new)))
              .orElse(Collections.emptyMap());
    }
    if (properties == null) {
      this.properties = new HashMap<>();
    }
    return new ArrayList<>(properties.values());
  }

  // Jackson序列化用的getter方法
  @JsonProperty("functions")
  public Map<String, AbstractFunctionMetadata> getFunctionsMap() {
    if (functions == null) {
      getFunctions(); // 初始化functions
    }
    return functions;
  }

  // Jackson反序列化用的setter方法
  @JsonProperty("functions")
  public void setFunctionsMap(Map<String, AbstractFunctionMetadata> functions) {
    this.functions = functions;
  }

  @Override
  public List<AbstractFunctionMetadata> getFunctions() {
    if (functions == null && jsonObject != null) {
      functions =
          Optional.ofNullable(jsonObject.getJSONArray("functions"))
              .map(Collection::stream)
              .<Map<String, AbstractFunctionMetadata>>map(
                  stream ->
                      stream
                          .map(JSONObject.class::cast)
                          .map(FunctionMetadata::new)
                          .map(AbstractFunctionMetadata.class::cast)
                          .filter(
                              function ->
                                  function.getId() != null
                                      && !function.getId().trim().isEmpty()) // 过滤掉id为null的项
                          .collect(
                              Collectors.toMap(
                                  AbstractFunctionMetadata::getId,
                                  Function.identity(),
                                  (a, b) -> a,
                                  LinkedHashMap::new)))
              .orElse(Collections.emptyMap());
    }
    if (functions == null) {
      this.functions = new HashMap<>();
    }
    return new ArrayList<>(functions.values());
  }

  // Jackson序列化用的getter方法
  @JsonProperty("tags")
  public Map<String, AbstractPropertyMetadata> getTagsMap() {
    if (tags == null) {
      getTags(); // 初始化tags
    }
    return tags;
  }

  // Jackson反序列化用的setter方法
  @JsonProperty("tags")
  public void setTagsMap(Map<String, AbstractPropertyMetadata> tags) {
    this.tags = tags;
  }

  @Override
  public List<AbstractPropertyMetadata> getTags() {
    if (tags == null && jsonObject != null) {
      tags =
          Optional.ofNullable(jsonObject.getJSONArray("tags"))
              .map(Collection::stream)
              .<Map<String, AbstractPropertyMetadata>>map(
                  stream ->
                      stream
                          .map(JSONObject.class::cast)
                          .map(PropertyMetadata::new)
                          .map(AbstractPropertyMetadata.class::cast)
                          .filter(
                              tag ->
                                  tag.getId() != null
                                      && !tag.getId().trim().isEmpty()) // 过滤掉id为null的项
                          .collect(
                              Collectors.toMap(
                                  AbstractPropertyMetadata::getId,
                                  Function.identity(),
                                  (a, b) -> a,
                                  LinkedHashMap::new)))
              .orElse(Collections.emptyMap());
    }
    if (tags == null) {
      this.tags = new HashMap<>();
    }
    return new ArrayList<>(tags.values());
  }

  // Jackson序列化用的getter方法
  @JsonProperty("events")
  public Map<String, AbstractEventMetadata> getEventsMap() {
    if (events == null) {
      getEvents(); // 初始化events
    }
    return events;
  }

  // Jackson反序列化用的setter方法
  @JsonProperty("events")
  public void setEventsMap(Map<String, AbstractEventMetadata> events) {
    this.events = events;
  }

  @Override
  public List<AbstractEventMetadata> getEvents() {
    if (events == null && jsonObject != null) {
      events =
          Optional.ofNullable(jsonObject.getJSONArray("events"))
              .map(Collection::stream)
              .<Map<String, AbstractEventMetadata>>map(
                  stream ->
                      stream
                          .map(JSONObject.class::cast)
                          .map(EventMetadata::new)
                          .map(AbstractEventMetadata.class::cast)
                          .filter(
                              event ->
                                  event.getId() != null
                                      && !event.getId().trim().isEmpty()) // 过滤掉id为null的项
                          .collect(
                              Collectors.toMap(
                                  AbstractEventMetadata::getId,
                                  Function.identity(),
                                  (a, b) -> a,
                                  LinkedHashMap::new)))
              .orElse(Collections.emptyMap());
    }
    if (events == null) {
      this.events = new HashMap<>();
    }
    return new ArrayList<>(events.values());
  }

  @Override
  public AbstractEventMetadata getEventOrNull(String id) {
    if (events == null) {
      getEvents();
    }
    return events.get(id);
  }

  @Override
  public AbstractPropertyMetadata getPropertyOrNull(String id) {
    if (properties == null) {
      getProperties();
    }
    return properties.get(id);
  }

  @Override
  public AbstractFunctionMetadata getFunctionOrNull(String id) {
    if (functions == null) {
      getFunctions();
    }
    return functions.get(id);
  }

  @Override
  public AbstractPropertyMetadata getTagOrNull(String id) {
    if (tags == null) {
      getTags();
    }
    return tags.get(id);
  }

  public Map<String, Object> getExpands() {
    if (this.expands == null) {
      if (jsonObject != null) {
        this.expands = jsonObject.getJSONObject("expands");
      }
    }
    return this.expands;
  }

  @Override
  @JsonIgnore
  public JSONObject toJson() {
    JSONObject json = new JSONObject();
    json.put("id", id);
    json.put("name", name);
    json.put("description", description);
    json.put(
        "properties", getProperties().stream().map(Jsonable::toJson).collect(Collectors.toList()));
    json.put(
        "functions", getFunctions().stream().map(Jsonable::toJson).collect(Collectors.toList()));
    json.put("events", getEvents().stream().map(Jsonable::toJson).collect(Collectors.toList()));
    json.put("expands", expands);
    return json;
  }

  @Override
  @JsonIgnore
  public void fromJson(JSONObject json) {
    this.jsonObject = json;
    this.properties = null;
    this.events = null;
    this.functions = null;
    if (json != null) {
      this.id = json.getStr("id");
      this.name = json.getStr("name");
      this.description = json.getStr("description");
      this.expands = json.getJSONObject("expands");
    }
  }
}
