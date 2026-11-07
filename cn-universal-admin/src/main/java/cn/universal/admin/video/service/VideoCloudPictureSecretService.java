package cn.universal.admin.video.service;

import cn.hutool.json.JSONUtil;
import cn.universal.common.exception.BaseException;
import cn.universal.dm.cache.IoTCacheInvalidationPublisher;
import cn.universal.persistence.entity.IoTDevice;
import cn.universal.persistence.entity.IoTProduct;
import cn.universal.persistence.mapper.IoTDeviceMapper;
import java.util.Map;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/** Write-only camera password setting; responses contain only the configured flag. */
@Service
@RequiredArgsConstructor
public class VideoCloudPictureSecretService {
  private final IoTDeviceMapper devices;
  private final IoTCacheInvalidationPublisher invalidation;
  private final cn.universal.ossm.util.VideoCloudEvidenceService cloudEvidence;
  private final VideoCloudDeviceDetailService details;
  private final PlatformTransactionManager transactionManager;

  @Transactional
  public Map<String, Object> save(IoTDevice authorized, String password) {
    if (password.length() > 256) throw new BaseException("图片解密密码不能超过256个字符");
    IoTDevice current = devices.selectForUpdateByProductKeyAndDeviceId(authorized.getProductKey(), authorized.getDeviceId());
    if (current == null || !Objects.equals(current.getTenantId(), authorized.getTenantId()))
      throw new BaseException("设备不存在或归属已变化");
    var config = JSONUtil.parseObj(current.getConfiguration());
    if (!password.isBlank() && !cn.universal.common.utils.JsonDesensitizationUtil.isMaskedValue(password)) {
      config.set("pictureSecret", password);
      config.set("pictureSecretVerification", Map.of("status", "unverified", "message", "密码已更新，尚未验证"));
      var patch = new IoTDevice();
      patch.setId(current.getId());
      patch.setConfiguration(config.toString());
      devices.updateByPrimaryKeySelective(patch);
      current.setConfiguration(config.toString());
      cloudEvidence.retryAfterSecretChange();
      details.publishDeviceChange(current, authorized.getThirdPlatform());
    }
    return Map.of("pictureSecretConfigured", !config.getStr("pictureSecret", "").isBlank(),
        "pictureSecretVerification", VideoCloudDeviceDetailService.verification(config));
  }

  public Map<String, Object> verify(IoTDevice authorized, IoTProduct product, String candidate) {
    boolean stored = candidate == null || candidate.isBlank();
    IoTDevice latest = devices.selectIoTDevice(authorized.getProductKey(), authorized.getDeviceId());
    if (latest == null || !Objects.equals(latest.getTenantId(), authorized.getTenantId()))
      throw new BaseException("设备不存在或归属已变化");
    var config = VideoCloudDeviceDetailService.config(latest);
    String password = stored ? config.getStr("pictureSecret", "") : candidate;
    if (password.isBlank()) throw new BaseException("请先填写或保存图片解密密码");
    if (password.length() > 256 || cn.universal.common.utils.JsonDesensitizationUtil.isMaskedValue(password))
      throw new BaseException("图片解密密码无效");
    // No transaction or row lock while contacting the vendor.
    Map<String, Object> result = new java.util.LinkedHashMap<>();
    try {
      var verification = details.provider(product).verifyPictureSecret(VideoCloudDeviceDetailService.info(product),
          VideoCloudDeviceDetailService.hosted(latest, config), password);
      result.put("status", verification.supported() ? (verification.valid() ? "verified" : "invalid") : "unsupported");
      result.put("verificationMode", Objects.toString(verification.verificationMode(), ""));
      result.put("message", Objects.toString(verification.message(), ""));
    } catch (RuntimeException unavailable) {
      // Account permissions and transport errors do not prove that a device password is wrong.
      result.put("status", "error");
      result.put("verificationMode", "VENDOR_API");
      result.put("message", Objects.toString(unavailable.getMessage(), "").contains("OP1009")
          ? "乐橙密码验证接口权限不足（OP1009），可保存密码并通过真实告警图片验证解密"
          : "厂家密码验证未完成，请检查接口权限、设备状态或稍后重试；可通过真实告警图片验证解密");
    }
    result.put("checkedAt", System.currentTimeMillis());
    if (stored) new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
      var current = devices.selectForUpdateByProductKeyAndDeviceId(authorized.getProductKey(), authorized.getDeviceId());
      if (current == null || !Objects.equals(current.getTenantId(), authorized.getTenantId()))
        throw new BaseException("设备不存在或归属已变化");
      var currentConfig = VideoCloudDeviceDetailService.config(current);
      if (!password.equals(currentConfig.getStr("pictureSecret", "")))
        throw new BaseException("密码已被更新，请重新验证");
      currentConfig.set("pictureSecretVerification", result);
      var patch = new IoTDevice();
      patch.setId(current.getId());
      patch.setConfiguration(currentConfig.toString());
      devices.updateByPrimaryKeySelective(patch);
      current.setConfiguration(currentConfig.toString());
      details.publishDeviceChange(current, product.getThirdPlatform());
    });
    return result;
  }
}
