package cn.universal.admin.video.hosting;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.universal.admin.platform.service.IIoTProductService;
import cn.universal.admin.system.permission.SysPermissionService;
import cn.universal.common.domain.R;
import cn.universal.core.video.hosting.VideoCloudHostingProvider.DeviceRef;
import cn.universal.persistence.entity.IoTProduct;
import cn.universal.persistence.entity.IoTUser;
import cn.universal.persistence.entity.vo.IoTProductVO;
import cn.universal.persistence.mapper.IoTProductMapper;
import cn.universal.persistence.query.IoTProductQuery;
import cn.universal.security.BaseController;
import cn.universal.security.service.IoTUserService;
import cn.universal.security.utils.SecurityUtils;
import java.util.List;
import java.util.Map;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.*;

/** Credentials are write-only; every cloud operation is bound to an authorized local product. */
@RestController
@RequestMapping("/admin/v1/video/dahua-cloud")
public class DahuaCloudController extends BaseController {
  @org.springframework.beans.factory.annotation.Autowired(required = false)
  private List<cn.universal.core.product.metadata.ProductMetadataProvider> metadataProviders = List.of();
  private final IIoTProductService products;
  private final SysPermissionService permissions;
  private final IoTProductMapper productMapper;
  private final IoTUserService users;
  private final VideoCloudHostingService hosting;

  public DahuaCloudController(
      IIoTProductService products,
      IoTProductMapper productMapper,
      IoTUserService users,
      VideoCloudHostingService hosting,
      SysPermissionService permissions) {
    this.permissions = permissions;
    this.products = products;
    this.productMapper = productMapper;
    this.users = users;
    this.hosting = hosting;
  }

  private void requirePermission(String permission) {
    if (!permissions.hasPermission(permission)) throw new AccessDeniedException("无权执行该操作");
  }

  private IoTUser user() {
    requirePermission("video:dahua_cloud:list");
    IoTUser user = users.selectUserByUnionId(SecurityUtils.getUnionId());
    if (user == null || StrUtil.isBlank(user.getTenantId())) throw new SecurityException("请选择租户账号");
    return user;
  }

  private List<IoTProduct> accessibleProducts() {
    IoTProductQuery query = new IoTProductQuery();
    query.setThirdPlatform("dahua_cloud");
    query.setTenantId(user().getTenantId());
    return products.selectDevProductV2List(query);
  }

  private IoTProduct product(String key) {
    if (accessibleProducts().stream().noneMatch(p -> key.equals(p.getProductKey())))
      throw new SecurityException("无权访问该产品");
    IoTProduct result = productMapper.getProductByProductKey(key);
    if (result == null || !user().getTenantId().equals(result.getTenantId()))
      throw new SecurityException("无权访问该产品");
    return result;
  }

  private Map<String, Object> view(IoTProduct product) {
    JSONObject config =
        StrUtil.isBlank(product.getConfiguration())
            ? new JSONObject()
            : JSONUtil.parseObj(product.getConfiguration());
    JSONObject nested = config.getJSONObject("dahua_cloud");
    if (nested != null) config = nested;
    // 响应只告知密钥是否已配置，不向浏览器回显 secretAccessKey。
    return Map.of(
        "productKey",
        product.getProductKey(),
        "name",
        product.getName(),
        "cloudProductId",
        config.getStr("cloudProductId", ""),
        "accessKey",
        config.getStr("accessKey", ""),
        "secretConfigured",
        StrUtil.isNotBlank(config.getStr("secretAccessKey")));
  }

  @GetMapping("/products")
  public R<?> products() {
    return R.ok(accessibleProducts().stream().map(this::view).toList());
  }

  @PostMapping("/products")
  public R<?> create(@RequestBody ProductRequest request) {
    requirePermission("system:product:add");
    rejectSuperAdminCreate("产品");
    JSONObject config = config(request, null);
    IoTProduct product =
        IoTProduct.builder()
            .name(required(request.name(), "产品名称"))
            .thirdPlatform("dahua_cloud")
            .transportProtocol("dahua_cloud")
            .deviceNode("VIDEO_DEVICE")
            .state((byte) 0)
            .configuration(config.toString())
            .creatorId(user().getUnionId())
            .tenantId(user().getTenantId())
            .build();
    products.insertDevProduct(product);
    return R.ok(view(product));
  }

  @PostMapping("/products/{key}/metadata")
  public R<?> syncMetadata(@PathVariable String key) {
    requirePermission("system:product:edit");
    product(key);
    var provider = metadataProviders.stream().filter(p -> "dahua_cloud".equals(p.getProtocolCode()))
        .findFirst().orElseThrow(() -> new IllegalStateException("大华云协议模块未启用"));
    JSONObject model = JSONUtil.parseObj(provider.getMetadata());
    var update = new cn.universal.persistence.entity.bo.IoTProductMetadataBatchBO();
    update.setProductKey(key);
    update.setMode("merge");
    update.setProperties(model.getJSONArray("properties").toList(cn.universal.persistence.entity.bo.IoTProductMetadataBO.class));
    update.setEvents(model.getJSONArray("events").toList(cn.universal.persistence.entity.bo.IoTProductMetadataBO.class));
    update.setFunctions(model.getJSONArray("functions").toList(cn.universal.persistence.entity.bo.IoTProductMetadataBO.class));
    if (products.batchUpdateMetadata(update) <= 0) throw new IllegalStateException("产品物模型未更新，请重试");
    return R.ok(Map.of("events", update.getEvents().size(), "functions", update.getFunctions().size()));
  }

  @PutMapping("/products/{key}")
  public R<?> update(@PathVariable String key, @RequestBody ProductRequest request) {
    requirePermission("system:product:edit");
    IoTProduct product = product(key);
    IoTProductVO value = new IoTProductVO();
    value.setId(product.getId());
    value.setProductKey(key);
    value.setConfiguration(config(request, product).toString());
    value.setType("privateConfiguration");
    products.updateDevProductConfig(value);
    return R.ok(view(productMapper.getProductByProductKey(key)));
  }

  private JSONObject config(ProductRequest request, IoTProduct old) {
    JSONObject config =
        old == null || StrUtil.isBlank(old.getConfiguration())
            ? new JSONObject()
            : JSONUtil.parseObj(old.getConfiguration());
    JSONObject nested = config.getJSONObject("dahua_cloud");
    if (nested != null) {
      config.putAll(nested);
      config.remove("dahua_cloud");
    }
    config.set("cloudProductId", required(request.cloudProductId(), "大华云 ProductId"));
    config.set("accessKey", required(request.accessKey(), "Access Key"));
    if (!config.getStr("cloudProductId").matches("[0-9]+")
        || !config.getStr("accessKey").matches("[A-Za-z0-9]+"))
      throw new IllegalArgumentException("ProductId 或 Access Key 格式无效");
    if (StrUtil.isNotBlank(request.secretAccessKey()))
      config.set("secretAccessKey", request.secretAccessKey().trim());
    required(config.getStr("secretAccessKey"), "Secret Access Key");
    config.set("videoHosting", Map.of("enabled", true));
    return config;
  }

  @GetMapping("/{key}/authorization")
  public R<?> authorization(@PathVariable String key) {
    product(key);
    return R.ok(hosting.authorization("dahua_cloud", key));
  }

  @GetMapping("/{key}/devices")
  public R<?> devices(
      @PathVariable String key,
      @RequestParam(defaultValue = "1") int page,
      @RequestParam(defaultValue = "20") int pageSize) {
    product(key);
    return R.ok(hosting.authorizedPage("dahua_cloud", key, page, pageSize));
  }

  @PostMapping("/{key}/import")
  public R<?> importDevices(@PathVariable String key, @RequestBody List<DeviceRef> devices) {
    requirePermission("system:instance:add");
    product(key);
    return R.ok(hosting.acceptHosted("dahua_cloud", key, devices));
  }

  @PostMapping("/{key}/sync")
  public R<?> sync(@PathVariable String key) {
    product(key);
    return R.ok(hosting.syncAuthorized("dahua_cloud", key));
  }

  @PostMapping("/{key}/bind")
  public R<?> bind(@PathVariable String key, @RequestBody BindRequest request) {
    requirePermission("system:instance:add");
    product(key);
    return R.ok(hosting.bindDevice("dahua_cloud", key, request.deviceId(), request.password()));
  }

  private static String required(String value, String label) {
    if (StrUtil.isBlank(value)) throw new IllegalArgumentException(label + "不能为空");
    return value.trim();
  }

  public record ProductRequest(
      String name, String cloudProductId, String accessKey, String secretAccessKey) {}

  public record BindRequest(String deviceId, String password) {}
}
