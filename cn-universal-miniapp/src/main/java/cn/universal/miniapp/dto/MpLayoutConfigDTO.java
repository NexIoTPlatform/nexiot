/*
 * Copyright (c) 2025, NexIoT. All Rights Reserved.
 */
package cn.universal.miniapp.dto;

import java.util.List;
import java.util.Map;
import lombok.Data;

/**
 * 布局配置DTO
 *
 * @author NexIoT
 * @since 2025/12/15
 */
@Data
public class MpLayoutConfigDTO {

  /** 小程序APPID */
  private String appId;

  /** 产品Key */
  private String productKey;

  /** 布局名称 */
  private String layoutName;

  /** 布局配置 */
  private List<LayoutComponent> layoutConfig;

  /** 布局组件 */
  @Data
  public static class LayoutComponent {
    /** 组件类型 */
    private String componentType;

    /** 组件ID */
    private String componentId;

    /** 行索引 */
    private Integer rowIndex;

    /** 列索引 */
    private Integer colIndex;

    /** 宽度占比 */
    private Integer width;

    /** 高度 */
    private Integer height;

    /** 组件配置 */
    private Map<String, Object> config;
  }
}
