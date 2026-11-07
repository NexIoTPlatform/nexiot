package cn.universal.admin.system.notice;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.universal.manager.notice.dto.NoticeChannelDTO;
import cn.universal.manager.notice.model.NoticeChannel;
import java.util.Map;
import org.junit.jupiter.api.Test;

class NoticeChannelControllerTest {

  @Test
  void buildConfigJsonShouldKeepMaskedSecretAndApplyOtherUpdates() {
    NoticeChannelDTO dto = new NoticeChannelDTO();
    dto.setChannelType("sms_ali");
    dto.setConfig(
        Map.of(
            "accessKey", "new-ak",
            "secretKey", "***",
            "signName", "new-sign",
            "templateCode", "new-template"));

    NoticeChannel existingChannel = new NoticeChannel();
    existingChannel.setChannelType("sms_ali");
    existingChannel.setConfig(
        "{\"accessKey\":\"old-ak\",\"secretKey\":\"old-secret\",\"signName\":\"old-sign\",\"templateCode\":\"old-template\"}");

    JSONObject config =
        JSONUtil.parseObj(NoticeChannelController.buildConfigJson(dto, existingChannel));

    assertEquals("new-ak", config.getStr("accessKey"));
    assertEquals("old-secret", config.getStr("secretKey"));
    assertEquals("new-sign", config.getStr("signName"));
    assertEquals("new-template", config.getStr("templateCode"));
  }

  @Test
  void buildConfigJsonShouldPersistExplicitSecretUpdates() {
    NoticeChannelDTO dto = new NoticeChannelDTO();
    dto.setChannelType("sms_ali");
    dto.setConfig(
        Map.of(
            "accessKey", "new-ak",
            "secretKey", "new-secret",
            "signName", "new-sign",
            "templateCode", "new-template"));

    NoticeChannel existingChannel = new NoticeChannel();
    existingChannel.setChannelType("sms_ali");
    existingChannel.setConfig(
        "{\"accessKey\":\"old-ak\",\"secretKey\":\"old-secret\",\"signName\":\"old-sign\",\"templateCode\":\"old-template\"}");

    JSONObject config =
        JSONUtil.parseObj(NoticeChannelController.buildConfigJson(dto, existingChannel));

    assertEquals("new-secret", config.getStr("secretKey"));
    assertEquals("new-ak", config.getStr("accessKey"));
  }

  @Test
  void buildConfigJsonShouldReplaceConfigWhenChannelTypeChanges() {
    NoticeChannelDTO dto = new NoticeChannelDTO();
    dto.setChannelType("webhook");
    dto.setConfig(Map.of("webhook", "https://example.com/hook"));

    NoticeChannel existingChannel = new NoticeChannel();
    existingChannel.setChannelType("sms_ali");
    existingChannel.setConfig(
        "{\"accessKey\":\"old-ak\",\"secretKey\":\"old-secret\",\"signName\":\"old-sign\",\"templateCode\":\"old-template\"}");

    JSONObject config =
        JSONUtil.parseObj(NoticeChannelController.buildConfigJson(dto, existingChannel));

    assertEquals("https://example.com/hook", config.getStr("webhook"));
    assertFalse(config.containsKey("secretKey"));
    assertFalse(config.containsKey("accessKey"));
  }
  @Test
  void omittedChannelTypeStillPreservesMaskedSecrets() {
    NoticeChannelDTO dto = new NoticeChannelDTO();
    dto.setConfig(Map.of("webhook", "***"));
    NoticeChannel existing = new NoticeChannel(); existing.setChannelType("dingTalk");
    existing.setConfig("{\"webhook\":\"https://example.invalid/secret-hook\"}");
    assertEquals("https://example.invalid/secret-hook", JSONUtil.parseObj(NoticeChannelController.buildConfigJson(dto, existing)).getStr("webhook"));
    assertEquals("***", NoticeChannelController.maskConfig(existing.getConfig()).getStr("webhook"));
  }

  @Test
  void crudPermissionIsEnforcedEvenWithoutMethodSecurity() {
    var channels = org.mockito.Mockito.mock(cn.universal.manager.notice.service.NoticeChannelService.class);
    var controller = new NoticeChannelController(channels, new com.fasterxml.jackson.databind.ObjectMapper());
    var access = org.mockito.Mockito.mock(cn.universal.manager.notice.service.NoticeAccess.class);
    var permissions = org.mockito.Mockito.mock(cn.universal.admin.system.permission.SysPermissionService.class);
    org.springframework.test.util.ReflectionTestUtils.setField(controller, "noticeAccess", access);
    org.springframework.test.util.ReflectionTestUtils.setField(controller, "permissions", permissions);
    org.mockito.Mockito.when(access.requireUser()).thenReturn(new cn.universal.persistence.entity.IoTUser());
    org.junit.jupiter.api.Assertions.assertThrows(org.springframework.web.server.ResponseStatusException.class, () -> controller.save(new NoticeChannelDTO()));
    org.junit.jupiter.api.Assertions.assertThrows(org.springframework.web.server.ResponseStatusException.class, () -> controller.delete(java.util.List.of(1L)));
    org.mockito.Mockito.verifyNoInteractions(channels);
  }

}
