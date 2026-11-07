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

package cn.universal.admin.system.web;

import cn.universal.admin.system.service.IndexQueryService;
import cn.universal.persistence.dto.IndexQueryDTO;
import cn.universal.persistence.query.AjaxResult;
import cn.universal.security.BaseController;
import cn.universal.security.utils.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 首页数据接口
 *
 * @since 2023/8/12 16:51
 */
@RestController
@Tag(name = "首页overview", description = "首页数据统计接口")
@RequestMapping("/admin/index")
@Slf4j
public class IndexController extends BaseController {

  @Resource private IndexQueryService indexQueryService;

  /**
   * 首页数据统计
   *
   * @return 首页统计数据
   */
  @Operation(summary = "首页数据统计", description = "获取首页统计数据，包括设备数量、产品数量等信息")
  @io.swagger.v3.oas.annotations.responses.ApiResponse(
      responseCode = "200",
      description = "成功获取首页统计数据",
      content =
          @io.swagger.v3.oas.annotations.media.Content(
              mediaType = "application/json",
              schema =
                  @io.swagger.v3.oas.annotations.media.Schema(implementation = AjaxResult.class)))
  @GetMapping("/overview")
  public AjaxResult<IndexQueryDTO> index() {
    boolean admin = loginIoTUnionUser(SecurityUtils.getUnionId()).isAdmin();
    IndexQueryDTO qtyDTO =
        indexQueryService.queryIndexQty(admin ? null : SecurityUtils.getUnionId());
    return AjaxResult.success(qtyDTO);
  }
}
