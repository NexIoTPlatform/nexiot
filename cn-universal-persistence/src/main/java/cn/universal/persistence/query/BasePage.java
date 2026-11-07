/*
 *
 * Copyright (c) 2025, NexIoT. All Rights Reserved.
 *
 * @Description: 本文件由 gitee.com/NexIoT 开发并拥有版权，未经授权严禁擅自商用、复制或传播。
 * @Author: gitee.com/NexIoT
 * @Email: wo8335224@gmail.com
 * @Wechat: outlookFil
 *
 *
 */

package cn.universal.persistence.query;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * @Author gitee.com/NexIoT
 *
 * @since 2018年12月17日 上午10:59
 */
@Schema(description = "分页查询基类")
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class BasePage {

  @Schema(description = "页码")
  @Builder.Default
  private Integer page = 1;

  @Schema(description = "每页大小")
  @Builder.Default
  private Integer size = 10;

  @Schema(description = "页码")
  @Builder.Default
  private Integer pageNum = 1;

  @Schema(description = "每页大小")
  @Builder.Default
  private Integer pageSize = 10;

  // 特色场景使用
  @Schema(description = "半页大小")
  private Integer halfSize;

  /**
   * 规范化分页参数
   * 统一page/pageNum和size/pageSize字段，确保两组字段同步
   * 优先使用page/size，如果为空则使用pageNum/pageSize
   * 如果两组都为空，设置默认值page=1, size=10
   */
  public void normalizePage() {
    // 同步page和pageNum
    if (page == null && pageNum != null) {
      page = pageNum;
    } else if (page != null && pageNum == null) {
      pageNum = page;
    } else if (page == null && pageNum == null) {
      page = 1;
      pageNum = 1;
    }
    
    // 同步size和pageSize
    if (size == null && pageSize != null) {
      size = pageSize;
    } else if (size != null && pageSize == null) {
      pageSize = size;
    } else if (size == null && pageSize == null) {
      size = 10;
      pageSize = 10;
    }
  }
  
  /**
   * 获取规范化后的页码（优先返回page，保证非空）
   */
  public Integer getPageOrDefault() {
    normalizePage();
    return page;
  }
  
  /**
   * 获取规范化后的每页大小（优先返回size，保证非空）
   */
  public Integer getSizeOrDefault() {
    normalizePage();
    return size;
  }

  public Integer getHalfSize() {
    if (pageSize != null) {
      return pageSize / 2;
    }
    if (size != null) {
      return size / 2;
    }
    return 5; // 默认值
  }
}
