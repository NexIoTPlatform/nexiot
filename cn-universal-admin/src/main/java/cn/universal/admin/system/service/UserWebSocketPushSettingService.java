package cn.universal.admin.system.service;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.universal.common.constant.IoTConstant;
import cn.universal.common.exception.IoTException;
import cn.universal.persistence.entity.IoTUser;
import cn.universal.persistence.entity.admin.SysConfig;
import cn.universal.persistence.entity.bo.WebSocketPushProfileBO;
import cn.universal.persistence.entity.bo.WebSocketPushSettingsBO;
import cn.universal.security.service.IoTUserService;
import jakarta.annotation.Resource;
import java.util.Date;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class UserWebSocketPushSettingService {

  public static final String USER_CFG_KEY = "websocketPush";
  public static final String GLOBAL_CONFIG_KEY = "sys.websocket.push.enabled";
  public static final String DEFAULT_WEBSOCKET_PATH = "/ws/device-events";

  @Resource private IoTUserService ioTUserService;

  @Resource private ISysConfigService configService;

  public WebSocketPushProfileBO getProfile(String unionId) {
    IoTUser user = requireUser(unionId);
    return WebSocketPushProfileBO.builder()
        .globalEnabled(isGlobalEnabled())
        .adminEditable(user.isAdmin())
        .websocketPath(DEFAULT_WEBSOCKET_PATH)
        .settings(getSettings(user))
        .build();
  }

  public WebSocketPushSettingsBO getSettings(String unionId) {
    return getSettings(requireUser(unionId));
  }

  public WebSocketPushSettingsBO getSettings(IoTUser user) {
    JSONObject root = parseCfg(user == null ? null : user.getCfg());
    Object websocketPush = root.get(USER_CFG_KEY);
    if (websocketPush == null) {
      return WebSocketPushSettingsBO.defaults();
    }
    try {
      return JSONUtil.toBean(JSONUtil.toJsonStr(websocketPush), WebSocketPushSettingsBO.class)
          .normalized();
    } catch (Exception e) {
      return WebSocketPushSettingsBO.defaults();
    }
  }

  public WebSocketPushProfileBO saveProfile(String unionId, WebSocketPushProfileBO profile) {
    IoTUser user = requireUser(unionId);
    if (profile != null && profile.getSettings() != null) {
      saveSettings(user, profile.getSettings());
    }
    if (profile != null && profile.getGlobalEnabled() != null && user.isAdmin()) {
      saveGlobalEnabled(profile.getGlobalEnabled(), unionId);
    }
    return getProfile(unionId);
  }

  public boolean isGlobalEnabled() {
    String value = configService.selectConfigByKey(GLOBAL_CONFIG_KEY);
    if (StrUtil.isBlank(value)) {
      return false;
    }
    return Boolean.parseBoolean(value);
  }

  public JSONObject mergeProfileCfg(String originCfg, String latestCfg) {
    JSONObject origin = parseCfg(originCfg);
    JSONObject latest = parseCfg(latestCfg);
    if (latest.containsKey(IoTConstant.EXCLUSIVE_FIRST_LOGIN)) {
      origin.set(
          IoTConstant.EXCLUSIVE_FIRST_LOGIN,
          latest.getBool(IoTConstant.EXCLUSIVE_FIRST_LOGIN, false));
    }
    if (latest.containsKey(USER_CFG_KEY)) {
      origin.set(USER_CFG_KEY, latest.get(USER_CFG_KEY));
    }
    return origin;
  }

  private void saveSettings(IoTUser user, WebSocketPushSettingsBO settings) {
    JSONObject root = parseCfg(user.getCfg());
    root.set(USER_CFG_KEY, JSONUtil.parseObj(settings.normalized()));
    user.setCfg(root.toString());
    user.setUpdateBy(user.getUnionId());
    user.setUpdateDate(new Date());
    ioTUserService.updateUserById(user);
  }

  private void saveGlobalEnabled(Boolean enabled, String operatorUnionId) {
    SysConfig query = new SysConfig();
    query.setConfigKey(GLOBAL_CONFIG_KEY);
    List<SysConfig> configs = configService.selectConfigList(query);

    SysConfig config = configs.isEmpty() ? new SysConfig() : configs.get(0);
    config.setConfigName("WebSocket实时推送总开关");
    config.setConfigKey(GLOBAL_CONFIG_KEY);
    config.setConfigValue(Boolean.toString(Boolean.TRUE.equals(enabled)));
    config.setConfigType("N");
    config.setRemark("控制设备、告警和工单的实时 WebSocket 推送");

    if (config.getConfigId() == null) {
      config.setCreateBy(operatorUnionId);
      configService.insertConfig(config);
    } else {
      config.setUpdateBy(operatorUnionId);
      configService.updateConfig(config);
    }
  }

  private IoTUser requireUser(String unionId) {
    IoTUser user = ioTUserService.selectUserByUnionId(unionId);
    if (user == null) {
      throw new IoTException("当前用户不存在");
    }
    return user;
  }

  private JSONObject parseCfg(String cfg) {
    if (StrUtil.isBlank(cfg) || !JSONUtil.isTypeJSON(cfg)) {
      return new JSONObject();
    }
    try {
      return JSONUtil.parseObj(cfg);
    } catch (Exception e) {
      return new JSONObject();
    }
  }
}
