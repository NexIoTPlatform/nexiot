package cn.universal.admin.platform.web;

import cn.universal.admin.platform.service.IProductOperationService;
import cn.universal.core.product.operation.ProductOperationRequest;
import cn.universal.persistence.entity.IoTUser;
import cn.universal.persistence.query.AjaxResult;
import cn.universal.security.BaseController;
import cn.universal.security.utils.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 产品级操作控制器。 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/v1/product/operations")
@Tag(name = "产品级操作", description = "产品级调试、同步、运维操作接口")
public class IoTProductOperationController extends BaseController {

  private final IProductOperationService productOperationService;

  @GetMapping("/{id}")
  @Operation(summary = "获取产品级操作列表")
  public AjaxResult list(
      @Parameter(name = "id", description = "产品ID", required = true)
          @PathVariable("id")
          Long id) {
    IoTUser user = loginIoTUnionUser(SecurityUtils.getUnionId());
    return AjaxResult.success(
        productOperationService.listOperations(id, user.getUnionId(), user.isAdmin()));
  }

  @PostMapping("/{id}/{operationCode}")
  @Operation(summary = "执行产品级操作")
  public AjaxResult execute(
      @Parameter(name = "id", description = "产品ID", required = true)
          @PathVariable("id")
          Long id,
      @Parameter(name = "operationCode", description = "操作编码", required = true)
          @PathVariable("operationCode")
          String operationCode,
      @RequestBody(required = false) ProductOperationRequest request) {
    IoTUser user = loginIoTUnionUser(SecurityUtils.getUnionId());
    ProductOperationRequest finalRequest = request == null ? new ProductOperationRequest() : request;
    finalRequest.setOperationCode(operationCode);
    return AjaxResult.success(
        productOperationService.execute(id, finalRequest, user.getUnionId(), user.isAdmin()));
  }
}
