package cn.universal.admin.video.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import cn.hutool.json.JSONUtil;
import cn.universal.core.video.hosting.VideoCloudHostingProvider;
import cn.universal.dm.cache.IoTCacheInvalidationPublisher;
import cn.universal.ossm.util.VideoCloudEvidenceService;
import cn.universal.persistence.entity.IoTDevice;
import cn.universal.persistence.entity.IoTProduct;
import cn.universal.persistence.dto.IoTDeviceDTO;
import cn.universal.persistence.base.IoTDevicePostProcessor;
import cn.universal.persistence.mapper.IoTDeviceMapper;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;

class VideoCloudDeviceDetailServiceTest {
  private IoTDevice camera() {
    return IoTDevice.builder().id(1L).productKey("p").deviceId("camera").tenantId("t")
        .configuration("{\"pictureSecret\":\"private\",\"hosting\":{\"status\":\"active\"},\"custom\":123}").build();
  }
  private PlatformTransactionManager transactions() {
    var manager = mock(PlatformTransactionManager.class);
    when(manager.getTransaction(any())).thenReturn(new SimpleTransactionStatus());
    return manager;
  }

  @Test void refreshMergesLockedCurrentConfigurationAndWhitelistsTheHttpResponse() {
    var devices = mock(IoTDeviceMapper.class);
    var provider = mock(VideoCloudHostingProvider.class);
    when(provider.platformType()).thenReturn("imoulife");
    var remote = new VideoCloudHostingProvider.HostedDevice("camera", "0", "camera", null, "model", "camera",
        null, null, null, null, 1, Map.of("deviceAbility", "TCM,Capture", "devicePasswordV2", "vendor-private",
            "channelList", List.of(Map.of("channelId", 0, "movable", true, "playToken", "private-token"))));
    when(provider.refreshDeviceDetails(any(), any())).thenReturn(remote);
    var current = camera();
    when(devices.selectForUpdateByProductKeyAndDeviceId("p", "camera")).thenReturn(current);
    var dto = IoTDeviceDTO.builder().productKey("p").deviceId("camera").thirdPlatform("imoulife").build();
    when(devices.selectIoTDeviceBO(Map.of("productKey", "p", "deviceId", "camera"))).thenAnswer(call -> {
      dto.setDevConfiguration(current.getConfiguration()); return dto;
    });
    var invalidation = mock(IoTCacheInvalidationPublisher.class);
    var service = new VideoCloudDeviceDetailService(devices, invalidation, List.of(provider), transactions());
    var result = service.refresh(camera(), IoTProduct.builder().productKey("p").thirdPlatform("imoulife").build());
    var config = JSONUtil.parseObj(current.getConfiguration());
    assertEquals("private", config.getStr("pictureSecret"));
    assertEquals(123, config.getInt("custom"));
    assertEquals("active", config.getJSONObject("hosting").getStr("status"));
    assertTrue(config.getBool("isTcmDevice"));
    assertFalse(config.toString().contains("vendor-private"));
    assertFalse(result.toString().contains("private"));
    assertEquals(true, result.get("pictureSecretConfigured"));
    assertEquals("model", ((Map<?, ?>) result.get("snapshot")).get("deviceModel"));
    verify(invalidation).publishDeviceChange(IoTDevicePostProcessor.Operation.UPDATE, dto);
    assertEquals("imoulife", dto.getThirdPlatform());
    assertTrue(JSONUtil.parseObj(dto.getDevConfiguration()).getBool("isTcmDevice"));
    assertEquals("private", JSONUtil.parseObj(dto.getDevConfiguration()).getStr("pictureSecret"));
  }

  @Test void savingPicturePasswordInvalidatesTheCallbackAliasUsingJoinedProductIdentity() {
    var devices = mock(IoTDeviceMapper.class);
    var current = camera();
    when(devices.selectForUpdateByProductKeyAndDeviceId("p", "camera")).thenReturn(current);
    var dto = IoTDeviceDTO.builder().productKey("p").deviceId("camera").thirdPlatform("imoulife").build();
    when(devices.selectIoTDeviceBO(Map.of("productKey", "p", "deviceId", "camera"))).thenAnswer(call -> {
      dto.setDevConfiguration(current.getConfiguration()); return dto;
    });
    var invalidation = mock(IoTCacheInvalidationPublisher.class);
    var details = new VideoCloudDeviceDetailService(devices, invalidation, List.of(), transactions());
    var service = new VideoCloudPictureSecretService(devices, invalidation,
        mock(VideoCloudEvidenceService.class), details, transactions());
    var result = service.save(current, "updated-private");
    verify(invalidation).publishDeviceChange(IoTDevicePostProcessor.Operation.UPDATE, dto);
    assertEquals("imoulife", dto.getThirdPlatform());
    assertEquals("updated-private", JSONUtil.parseObj(dto.getDevConfiguration()).getStr("pictureSecret"));
    assertEquals(true, result.get("pictureSecretConfigured"));
    assertFalse(result.toString().contains("updated-private"));
  }

  @Test void storedPasswordVerificationPublishesJoinedConfigurationAndPlatform() {
    var devices = mock(IoTDeviceMapper.class);
    var current = camera();
    when(devices.selectIoTDevice("p", "camera")).thenReturn(current);
    when(devices.selectForUpdateByProductKeyAndDeviceId("p", "camera")).thenReturn(current);
    var dto = IoTDeviceDTO.builder().productKey("p").deviceId("camera").thirdPlatform("imoulife").build();
    when(devices.selectIoTDeviceBO(Map.of("productKey", "p", "deviceId", "camera"))).thenAnswer(call -> {
      dto.setDevConfiguration(current.getConfiguration()); return dto;
    });
    var provider = mock(VideoCloudHostingProvider.class);
    when(provider.platformType()).thenReturn("imoulife");
    when(provider.verifyPictureSecret(any(), any(), eq("private")))
        .thenReturn(new VideoCloudHostingProvider.PictureSecretVerification(true, true, "vendor", "验证通过"));
    var invalidation = mock(IoTCacheInvalidationPublisher.class);
    var details = new VideoCloudDeviceDetailService(devices, invalidation, List.of(provider), transactions());
    var service = new VideoCloudPictureSecretService(devices, invalidation,
        mock(VideoCloudEvidenceService.class), details, transactions());
    service.verify(current, IoTProduct.builder().productKey("p").thirdPlatform("imoulife").build(), "");
    verify(invalidation).publishDeviceChange(IoTDevicePostProcessor.Operation.UPDATE, dto);
    assertEquals("imoulife", dto.getThirdPlatform());
    assertEquals("private", JSONUtil.parseObj(dto.getDevConfiguration()).getStr("pictureSecret"));
    assertEquals("verified", JSONUtil.parseObj(dto.getDevConfiguration())
        .getJSONObject("pictureSecretVerification").getStr("status"));
  }

  @Test void missingJoinStillInvalidatesKnownDeviceAliases() {
    var devices = mock(IoTDeviceMapper.class);
    var invalidation = mock(IoTCacheInvalidationPublisher.class);
    var service = new VideoCloudDeviceDetailService(devices, invalidation, List.of(), transactions());
    var current = camera();
    service.publishDeviceChange(current, "imoulife");
    verify(invalidation).publishDeviceChange(eq(IoTDevicePostProcessor.Operation.UPDATE), argThat(dto ->
        "p".equals(dto.getProductKey()) && "camera".equals(dto.getDeviceId())
            && "imoulife".equals(dto.getThirdPlatform())
            && current.getConfiguration().equals(dto.getDevConfiguration())));
  }

  @Test void storedPasswordVerificationDoesNotOverwriteAConcurrentPasswordChange() {
    var devices = mock(IoTDeviceMapper.class);
    when(devices.selectIoTDevice("p", "camera")).thenReturn(camera());
    var changed = camera(); changed.setConfiguration("{\"pictureSecret\":\"new-private\"}");
    when(devices.selectForUpdateByProductKeyAndDeviceId("p", "camera")).thenReturn(changed);
    var details = mock(VideoCloudDeviceDetailService.class);
    var provider = mock(VideoCloudHostingProvider.class);
    when(details.provider(any())).thenReturn(provider);
    when(provider.verifyPictureSecret(any(), any(), eq("private")))
        .thenReturn(new VideoCloudHostingProvider.PictureSecretVerification(true, true, "vendor", "验证通过"));
    var service = new VideoCloudPictureSecretService(devices, mock(IoTCacheInvalidationPublisher.class),
        mock(VideoCloudEvidenceService.class), details, transactions());
    assertThrows(cn.universal.common.exception.BaseException.class,
        () -> service.verify(camera(), IoTProduct.builder().productKey("p").thirdPlatform("imoulife").build(), ""));
    verify(devices, never()).updateByPrimaryKeySelective(any());
  }

  @Test void candidateVerificationNeverPersistsOrReturnsTheCandidate() {
    var devices = mock(IoTDeviceMapper.class);
    when(devices.selectIoTDevice("p", "camera")).thenReturn(camera());
    var details = mock(VideoCloudDeviceDetailService.class);
    var provider = mock(VideoCloudHostingProvider.class);
    when(details.provider(any())).thenReturn(provider);
    when(provider.verifyPictureSecret(any(), any(), eq("candidate")))
        .thenReturn(new VideoCloudHostingProvider.PictureSecretVerification(true, false, "vendor", "密码无效"));
    var service = new VideoCloudPictureSecretService(devices, mock(IoTCacheInvalidationPublisher.class),
        mock(VideoCloudEvidenceService.class), details, transactions());
    var result = service.verify(camera(), IoTProduct.builder().productKey("p").thirdPlatform("imoulife").build(), "candidate");
    assertEquals("invalid", result.get("status"));
    assertFalse(result.toString().contains("candidate"));
    verify(devices, never()).selectForUpdateByProductKeyAndDeviceId(anyString(), anyString());
    verify(devices, never()).updateByPrimaryKeySelective(any());
  }

  @Test void vendorPermissionFailureIsAnOperationalErrorAndKeepsTheStoredPassword() {
    var devices = mock(IoTDeviceMapper.class);
    var camera = camera();
    when(devices.selectIoTDevice("p", "camera")).thenReturn(camera);
    when(devices.selectForUpdateByProductKeyAndDeviceId("p", "camera")).thenReturn(camera);
    var details = mock(VideoCloudDeviceDetailService.class);
    var provider = mock(VideoCloudHostingProvider.class);
    when(details.provider(any())).thenReturn(provider);
    when(provider.verifyPictureSecret(any(), any(), anyString()))
        .thenThrow(new IllegalStateException("接口权限不足（OP1009）"));
    var service = new VideoCloudPictureSecretService(devices, mock(IoTCacheInvalidationPublisher.class),
        mock(VideoCloudEvidenceService.class), details, transactions());
    var result = service.verify(camera, IoTProduct.builder().productKey("p").thirdPlatform("imoulife").build(), "");
    assertEquals("error", result.get("status"));
    assertTrue(result.get("message").toString().contains("OP1009"));
    assertEquals("private", JSONUtil.parseObj(camera.getConfiguration()).getStr("pictureSecret"));
    assertEquals("error", JSONUtil.parseObj(camera.getConfiguration()).getJSONObject("pictureSecretVerification").getStr("status"));
  }
}
