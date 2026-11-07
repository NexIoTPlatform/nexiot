package nexiot.web.ide.debug.plugins.core.model;

import static nexiot.web.ide.debug.plugins.core.config.Constants.HEADER_REQUEST_BREAKPOINTS;
import static nexiot.web.ide.debug.plugins.core.config.Constants.HEADER_REQUEST_CLIENT_ID;
import static nexiot.web.ide.debug.plugins.core.config.Constants.HEADER_REQUEST_SCRIPT_ID;
import static nexiot.web.ide.debug.plugins.core.config.MessageType.BREAKPOINT;

import cn.universal.core.engine.MagicScriptDebugContext;
import cn.universal.core.engine.functions.ObjectConvertExtension;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import nexiot.web.ide.debug.plugins.core.config.WebSocketSessionManager;
import nexiot.web.ide.debug.plugins.core.servlet.MagicHttpServletRequest;
import nexiot.web.ide.debug.plugins.utils.JsonUtils;

public class DebugRequest {

  private final MagicHttpServletRequest request;

  private DebugRequest(MagicHttpServletRequest request) {
    this.request = request;
  }

  public static DebugRequest create(MagicHttpServletRequest request) {
    return new DebugRequest(request);
  }

  /** 获得断点 */
  public List<Integer> getRequestedBreakpoints() {
    String breakpoints = request.getHeader(HEADER_REQUEST_BREAKPOINTS);
    if (breakpoints != null) {
      return Arrays.stream(breakpoints.split(","))
          .map(val -> ObjectConvertExtension.asInt(val, -1))
          .filter(it -> it > 0)
          .collect(Collectors.toList());
    }
    return Collections.emptyList();
  }

  /** 获取测试scriptId */
  public String getRequestedScriptId() {
    return request.getHeader(HEADER_REQUEST_SCRIPT_ID);
  }

  /** 获取测试clientId */
  public String getRequestedClientId() {
    return request.getHeader(HEADER_REQUEST_CLIENT_ID);
  }

  public MagicScriptDebugContext createMagicScriptContext(int debugTimeout) {
    MagicScriptDebugContext debugContext = new MagicScriptDebugContext(getRequestedBreakpoints());
    String scriptId = getRequestedScriptId();
    String clientId = getRequestedClientId();
    debugContext.setTimeout(debugTimeout);
    debugContext.setId(scriptId);
    debugContext.setCallback(
        variables -> {
          List<Map<String, Object>> varList =
              (List<Map<String, Object>>) variables.get("variables");
          varList.stream()
              .filter(it -> it.containsKey("value"))
              .forEach(
                  variable -> {
                    variable.put("value", JsonUtils.toJsonStringWithoutLog(variable.get("value")));
                  });
          WebSocketSessionManager.sendByClientId(clientId, BREAKPOINT, scriptId, variables);
        });
    return debugContext;
  }
}
