package cn.universal.admin.plugin;

import cn.universal.common.domain.R;
import cn.universal.common.plugin.PlatformPluginDescriptor;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 统一插件状态查询接口。 */
@RestController
@RequestMapping("/admin/plugin/status")
@Tag(name = "插件状态", description = "统一插件启用状态查询接口")
public class PluginStatusController {

  private final List<PlatformPluginDescriptor> pluginDescriptors;

  public PluginStatusController(ObjectProvider<List<PlatformPluginDescriptor>> pluginDescriptors) {
    this.pluginDescriptors = Optional.ofNullable(pluginDescriptors.getIfAvailable()).orElse(List.of());
  }

  @GetMapping("/list")
  @Operation(summary = "查询所有插件状态")
  public R list() {
    List<Map<String, Object>> plugins =
        pluginDescriptors.stream()
            .sorted(Comparator.comparing(PlatformPluginDescriptor::getPluginCode))
            .map(this::toStatus)
            .collect(Collectors.toList());
    return R.ok(plugins);
  }

  @GetMapping("/{pluginCode}")
  @Operation(summary = "按插件编码查询状态")
  public R getByCode(@PathVariable String pluginCode) {
    return pluginDescriptors.stream()
        .filter(descriptor -> descriptor.getPluginCode().equalsIgnoreCase(pluginCode))
        .findFirst()
        .map(descriptor -> R.ok(toStatus(descriptor)))
        .orElseGet(() -> R.ok(buildDisabledStatus(pluginCode)));
  }

  private Map<String, Object> toStatus(PlatformPluginDescriptor descriptor) {
    Map<String, Object> status = new LinkedHashMap<>();
    status.put("pluginCode", descriptor.getPluginCode());
    status.put("pluginName", descriptor.getPluginName());
    status.put("installed", descriptor.isInstalled());
    status.put("enabled", descriptor.isEnabled());
    status.put("metadata", descriptor.getMetadata());
    return status;
  }

  private Map<String, Object> buildDisabledStatus(String pluginCode) {
    Map<String, Object> status = new LinkedHashMap<>();
    status.put("pluginCode", pluginCode);
    status.put("pluginName", pluginCode == null ? "" : pluginCode.toLowerCase(Locale.ROOT));
    status.put("installed", false);
    status.put("enabled", false);
    status.put("metadata", Map.of());
    return status;
  }
}
