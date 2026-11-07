package cn.universal.admin.system.web;

import cn.universal.common.domain.R;
import cn.universal.common.exception.IoTException;
import cn.universal.persistence.entity.IoTUser;
import cn.universal.persistence.entity.WechatLoginConfig;
import cn.universal.persistence.entity.WechatUserBinding;
import cn.universal.security.BaseController;
import cn.universal.security.wechat.WechatAccountConfigService;
import cn.universal.security.wechat.WechatAuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/system/profile/wechat")
public class WechatProfileController extends BaseController {
  private final WechatAccountConfigService configs;
  private final WechatAuthService auth;

  public WechatProfileController(WechatAccountConfigService configs, WechatAuthService auth) {
    this.configs = configs;
    this.auth = auth;
  }

  @GetMapping
  public R<List<Map<String, Object>>> bindings(HttpServletResponse response) {
    response.setHeader("Cache-Control", "no-store");
    IoTUser user = requireUser();
    WechatLoginConfig config = configs.findByTenantId(user.getTenantId());
    List<Map<String, Object>> result = configs.bindings(user.getTenantId(), user.getUnionId()).stream()
        .map(binding -> bindingView(binding, config)).toList();
    return R.ok(result);
  }

  @PostMapping("/sessions")
  public R<Map<String, Object>> create(HttpServletRequest request, HttpServletResponse response) {
    response.setHeader("Cache-Control", "no-store");
    return R.ok(auth.createBinding(requireUser().getUnionId(), request.getRemoteAddr()));
  }

  @PostMapping("/sessions/{id}/confirm")
  public R<Void> confirm(@PathVariable String id, @RequestHeader("X-Wechat-Proof") String proof,
      HttpServletResponse response) {
    response.setHeader("Cache-Control", "no-store");
    auth.confirmBinding(id, proof, requireUser().getUnionId());
    return R.ok();
  }

  @DeleteMapping
  public R<Void> unbind() {
    IoTUser user = requireUser();
    configs.unbind(user.getTenantId(), user.getUnionId());
    return R.ok();
  }

  private IoTUser requireUser() {
    IoTUser user = currentTenantUser();
    if (user == null || user.isAdmin() || user.getTenantId() == null
        || !Integer.valueOf(0).equals(user.getDeleted()) || !Integer.valueOf(0).equals(user.getStatus())) {
      throw new IoTException("请使用正常启用的租户账号进行微信绑定");
    }
    return user;
  }

  private Map<String, Object> bindingView(WechatUserBinding binding, WechatLoginConfig config) {
    Map<String, Object> view = new LinkedHashMap<>();
    view.put("appId", binding.getAppId());
    view.put("accountName", config == null ? "微信公众号" : config.getAccountName());
    String openId = binding.getOpenId();
    view.put("openIdMasked", openId == null || openId.length() < 8 ? "***"
        : openId.substring(0, 3) + "***" + openId.substring(openId.length() - 4));
    view.put("createTime", binding.getCreateTime());
    view.put("boundAt", binding.getCreateTime());
    return view;
  }
}
