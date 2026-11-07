/*
 *
 * Copyright (c) 2025, NexIoT. All Rights Reserved.
 *
 * @Description: OTA管理服务实现
 * @Author: gitee.com/NexIoT
 *
 */

package cn.universal.admin.platform.service.impl;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.universal.admin.platform.service.IIoTDeviceService;
import cn.universal.admin.platform.service.IIoTOtaAdminService;
import cn.universal.common.constant.IoTConstant;
import cn.universal.common.exception.IoTException;
import cn.universal.dm.device.service.ota.api.IoTOtaRuntimeService;
import cn.universal.dm.device.service.ota.api.OtaService.OtaServiceResult;
import cn.universal.dm.device.service.ota.support.OtaConstants;
import cn.universal.dm.device.service.ota.support.OtaTransferProfile;
import cn.universal.dm.device.service.ota.support.OtaVersionUtils;
import cn.universal.persistence.entity.IoTDevice;
import cn.universal.persistence.entity.IoTOtaDeviceVersion;
import cn.universal.persistence.entity.IoTOtaPackage;
import cn.universal.persistence.entity.IoTOtaTag;
import cn.universal.persistence.entity.IoTOtaTask;
import cn.universal.persistence.entity.IoTOtaTaskDevice;
import cn.universal.persistence.entity.IoTProduct;
import cn.universal.persistence.entity.IoTUser;
import cn.universal.persistence.mapper.IoTDeviceMapper;
import cn.universal.persistence.mapper.IoTOtaDeviceVersionMapper;
import cn.universal.persistence.mapper.IoTOtaPackageMapper;
import cn.universal.persistence.mapper.IoTOtaPackageTagRelMapper;
import cn.universal.persistence.mapper.IoTOtaTagMapper;
import cn.universal.persistence.mapper.IoTOtaTaskDeviceMapper;
import cn.universal.persistence.mapper.IoTOtaTaskMapper;
import cn.universal.persistence.mapper.IoTProductMapper;
import cn.universal.persistence.page.PageContextRunner;
import cn.universal.security.service.IoTUserService;
import cn.universal.security.utils.SecurityUtils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** OTA管理服务实现 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IoTOtaAdminServiceImpl implements IIoTOtaAdminService {

  private static final int ACTIVE_TASK_CONFLICT_QUERY_BATCH_SIZE = 300;
  private static final List<String> STRONG_CHECKSUM_ALGORITHMS =
      List.of("SHA256", "SHA-256", "SHA512", "SHA-512");
  private static final List<String> PACKAGE_STATUSES =
      List.of(
          OtaConstants.PackageStatus.DRAFT,
          OtaConstants.PackageStatus.PUBLISHED,
          OtaConstants.PackageStatus.DISABLED);

  private final IoTOtaTagMapper otaTagMapper;
  private final IoTOtaPackageMapper otaPackageMapper;
  private final IoTOtaPackageTagRelMapper otaPackageTagRelMapper;
  private final IoTOtaTaskMapper otaTaskMapper;
  private final IoTOtaTaskDeviceMapper otaTaskDeviceMapper;
  private final IoTOtaDeviceVersionMapper otaDeviceVersionMapper;
  private final IoTProductMapper productMapper;
  private final IoTDeviceMapper deviceMapper;
  private final IoTOtaRuntimeService otaRuntimeService;
  private final IoTUserService ioTUserService;
  private final IIoTDeviceService ioTDeviceService;

  @Override
  public Map<String, Object> getTransferProfile(String productKey) {
    IoTProduct product = requireAccessibleProduct(productKey);
    return readTransferProfile(product).toMap();
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public Map<String, Object> saveTransferProfile(String productKey, Map<String, Object> profile) {
    requireAccessibleProduct(productKey);
    IoTProduct product = productMapper.selectProductForUpdate(productKey);
    if (product == null) {
      throw new IoTException("产品不存在");
    }
    OtaTransferProfile transferProfile = OtaTransferProfile.from(profile);
    transferProfile.validate();
    JSONObject configuration = parseProductConfiguration(product);
    configuration.set("otaTransfer", transferProfile.toMap());
    // 只更新此产品的配置，保留连接参数和其他产品信息。
    IoTProduct update = new IoTProduct();
    update.setId(product.getId());
    update.setConfiguration(configuration.toString());
    if (productMapper.updateDevProduct(update) != 1) {
      throw new IoTException("厂家适配保存失败");
    }
    return transferProfile.toMap();
  }

  private IoTProduct requireAccessibleProduct(String productKey) {
    if (StrUtil.isBlank(productKey)) {
      throw new IoTException("请选择产品");
    }
    IoTProduct product = productMapper.getProductByProductKey(productKey);
    if (product == null) {
      throw new IoTException("产品不存在");
    }
    assertOtaAccess(product.getTenantId(), product.getCreatorId(), "无产品权限");
    return product;
  }

  private JSONObject parseProductConfiguration(IoTProduct product) {
    if (StrUtil.isBlank(product.getConfiguration())) {
      return new JSONObject();
    }
    if (!JSONUtil.isTypeJSONObject(product.getConfiguration())) {
      throw new IoTException("产品配置格式错误，请先修复产品配置");
    }
    return JSONUtil.parseObj(product.getConfiguration());
  }

  private OtaTransferProfile readTransferProfile(IoTProduct product) {
    OtaTransferProfile profile =
        OtaTransferProfile.from(parseProductConfiguration(product).get("otaTransfer"));
    profile.validate();
    return profile;
  }

  @Override
  public List<IoTOtaTag> selectTagList(IoTOtaTag query, String creatorId, boolean admin) {
    if (query == null) {
      query = new IoTOtaTag();
    }
    if (!admin) {
      query.setCreatorId(creatorId);
      query.setTenantId(resolveTenantId(creatorId, null));
    }
    return otaTagMapper.selectTagList(query);
  }

  @Override
  public IoTOtaTag selectTagById(Long id) {
    IoTOtaTag tag = otaTagMapper.selectTagById(id);
    assertOtaAccess(
        tag == null ? null : tag.getTenantId(),
        tag == null ? null : tag.getCreatorId(),
        "OTA标签不存在或无权访问");
    return tag;
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public Long saveTag(IoTOtaTag tag, String creatorId) {
    if (StrUtil.isBlank(tag.getTagName())) {
      throw new IoTException("标签名称不能为空");
    }
    if (StrUtil.isBlank(tag.getTagCode())) {
      tag.setTagCode("ota_" + IdUtil.fastSimpleUUID().substring(0, 8));
    }
    if (tag.getId() != null) {
      IoTOtaTag stored = otaTagMapper.selectTagById(tag.getId());
      assertOtaAccess(
          stored == null ? null : stored.getTenantId(),
          stored == null ? null : stored.getCreatorId(),
          "OTA标签不存在或无权访问");
      if (stored != null && StrUtil.isNotBlank(stored.getTenantId())) {
        tag.setTenantId(stored.getTenantId());
      }
    }
    tag.setTenantId(
        StrUtil.blankToDefault(tag.getTenantId(), resolveTenantId(creatorId, null)));
    if (tag.getId() == null) {
      tag.setCreatorId(creatorId);
      otaTagMapper.insertTag(tag);
    } else {
      otaTagMapper.updateTag(tag);
    }
    return tag.getId();
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public int deleteTagById(Long id) {
    IoTOtaTag tag = otaTagMapper.selectTagById(id);
    assertOtaAccess(
        tag == null ? null : tag.getTenantId(),
        tag == null ? null : tag.getCreatorId(),
        "OTA标签不存在或无权访问");
    otaPackageTagRelMapper.deleteByTagId(id);
    return otaTagMapper.deleteTagById(id);
  }

  @Override
  public List<IoTOtaPackage> selectPackageList(
      IoTOtaPackage query, String creatorId, boolean admin) {
    if (query == null) {
      query = new IoTOtaPackage();
    }
    if (!admin) {
      query.setCreatorId(creatorId);
      query.setTenantId(resolveTenantId(creatorId, null));
    }
    List<IoTOtaPackage> list = otaPackageMapper.selectPackageList(query);
    list.forEach(this::fillPackageTags);
    return list;
  }

  @Override
  public IoTOtaPackage selectPackageById(Long id) {
    IoTOtaPackage otaPackage = otaPackageMapper.selectPackageById(id);
    assertOtaAccess(
        otaPackage == null ? null : otaPackage.getTenantId(),
        otaPackage == null ? null : otaPackage.getCreatorId(),
        "OTA升级包不存在或无权访问");
    fillPackageTags(otaPackage);
    return otaPackage;
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public Long savePackage(IoTOtaPackage otaPackage, String creatorId) {
    IoTProduct product = resolveProduct(otaPackage.getProductId(), otaPackage.getProductKey());
    if (product == null) {
      throw new IoTException("产品不存在");
    }
    assertOtaAccess(product.getTenantId(), product.getCreatorId(), "无产品权限");
    if (StrUtil.isBlank(otaPackage.getPackageName())) {
      throw new IoTException("升级包名称不能为空");
    }
    if (StrUtil.isBlank(otaPackage.getVersion())) {
      throw new IoTException("升级包版本不能为空");
    }
    if (StrUtil.isBlank(otaPackage.getPackageType())) {
      otaPackage.setPackageType(OtaConstants.DEFAULT_PACKAGE_TYPE);
    }
    if (StrUtil.isBlank(otaPackage.getModule())) {
      otaPackage.setModule(OtaConstants.DEFAULT_MODULE);
    }
    if (StrUtil.isBlank(otaPackage.getFileUrl())) {
      throw new IoTException("升级包文件地址不能为空");
    }
    normalizePackageChecksum(otaPackage);
    validatePackageBeforePublish(otaPackage);

    otaPackage.setProductId(product.getId());
    otaPackage.setProductKey(product.getProductKey());
    otaPackage.setTenantId(resolveTenantId(product.getCreatorId(), product.getTenantId()));
    if (StrUtil.isBlank(otaPackage.getStatus())) {
      otaPackage.setStatus(OtaConstants.PackageStatus.DRAFT);
    }
    validatePackageStatus(otaPackage.getStatus());

    if (otaPackage.getId() == null) {
      otaPackage.setCreatorId(creatorId);
      otaPackageMapper.insertPackage(otaPackage);
    } else {
      IoTOtaPackage stored = otaPackageMapper.selectPackageById(otaPackage.getId());
      assertOtaAccess(
          stored == null ? null : stored.getTenantId(),
          stored == null ? null : stored.getCreatorId(),
          "OTA升级包不存在或无权访问");
      assertPackageEditable(stored, otaPackage);
      otaPackageMapper.updatePackage(otaPackage);
      otaPackageTagRelMapper.deleteByPackageId(otaPackage.getId());
    }
    validateTagAccess(otaPackage.getTagIds(), otaPackage.getTenantId());
    savePackageTags(otaPackage.getId(), otaPackage.getTagIds());
    return otaPackage.getId();
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public int updatePackageStatus(Long id, String status) {
    IoTOtaPackage otaPackage = otaPackageMapper.selectPackageById(id);
    if (otaPackage == null) {
      throw new IoTException("升级包不存在");
    }
    assertOtaAccess(otaPackage.getTenantId(), otaPackage.getCreatorId(), "OTA升级包不存在或无权访问");
    validatePackageStatus(status);
    otaPackage.setStatus(status);
    validatePackageBeforePublish(otaPackage);
    return otaPackageMapper.updatePackage(otaPackage);
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public int deletePackageById(Long id) {
    IoTOtaPackage otaPackage = otaPackageMapper.selectPackageById(id);
    assertOtaAccess(
        otaPackage == null ? null : otaPackage.getTenantId(),
        otaPackage == null ? null : otaPackage.getCreatorId(),
        "OTA升级包不存在或无权访问");
    if (otaPackageMapper.countTaskByPackageId(id) > 0) {
      throw new IoTException("该升级包已关联升级任务，不能删除");
    }
    otaPackageTagRelMapper.deleteByPackageId(id);
    return otaPackageMapper.deletePackageById(id);
  }

  @Override
  public List<IoTOtaTask> selectTaskList(IoTOtaTask query, String creatorId, boolean admin) {
    if (query == null) {
      query = new IoTOtaTask();
    }
    if (!admin) {
      query.setCreatorId(creatorId);
      query.setTenantId(resolveTenantId(creatorId, null));
    }
    return otaTaskMapper.selectTaskList(query);
  }

  @Override
  public IoTOtaTask selectTaskById(Long id) {
    IoTOtaTask task = otaTaskMapper.selectTaskById(id);
    assertOtaAccess(
        task == null ? null : task.getTenantId(),
        task == null ? null : task.getCreatorId(),
        "OTA任务不存在或无权访问");
    return task;
  }

  @Override
  public List<IoTOtaTaskDevice> selectTaskDeviceList(IoTOtaTaskDevice query) {
    if (query != null && query.getTaskId() != null) {
      PageContextRunner.runWithoutPaging(() -> selectTaskById(query.getTaskId()));
    }
    return otaTaskDeviceMapper.selectTaskDeviceList(query);
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public Long createTask(IoTOtaTask task, String creatorId) {
    if (task.getPackageId() == null) {
      throw new IoTException("请选择升级包");
    }
    IoTOtaPackage otaPackage = otaPackageMapper.selectPackageById(task.getPackageId());
    if (otaPackage == null) {
      throw new IoTException("升级包不存在");
    }
    assertOtaAccess(otaPackage.getTenantId(), otaPackage.getCreatorId(), "升级包不存在或无权访问");
    if (!OtaConstants.PackageStatus.PUBLISHED.equals(otaPackage.getStatus())) {
      throw new IoTException("仅已发布的升级包可创建升级任务");
    }
    validateDispatchablePackage(otaPackage);
    IoTProduct product =
        resolveProduct(
            task.getProductId(),
            StrUtil.blankToDefault(task.getProductKey(), otaPackage.getProductKey()));
    if (product == null) {
      throw new IoTException("产品不存在");
    }
    assertOtaAccess(product.getTenantId(), product.getCreatorId(), "无产品权限");
    if (!StrUtil.equals(product.getProductKey(), otaPackage.getProductKey())) {
      throw new IoTException("升级任务所属产品必须与升级包所属产品一致");
    }
    if (!usesProductDeviceCandidates(task.getRolloutMode())
        && (task.getDeviceIds() == null || task.getDeviceIds().isEmpty())) {
      throw new IoTException("指定设备发布时请选择目标设备");
    }
    if (task.isRollbackOnFailureFlag() && task.getRollbackPackageId() == null) {
      throw new IoTException("开启失败回滚时请选择回滚升级包");
    }
    if (task.getGrayPercent() != null
        && (task.getGrayPercent() < 1 || task.getGrayPercent() > 100)) {
      throw new IoTException("灰度比例必须在1-100之间");
    }
    if (task.getGrayDeviceLimit() != null && task.getGrayDeviceLimit() < 1) {
      throw new IoTException("灰度设备上限必须大于0");
    }
    if (task.getBatchSize() != null && task.getBatchSize() < 1) {
      throw new IoTException("单批设备数必须大于0");
    }
    if (task.getBatchIntervalSeconds() != null && task.getBatchIntervalSeconds() < 0) {
      throw new IoTException("批次间隔不能小于0");
    }
    if (task.getFailureThresholdPercent() != null
        && (task.getFailureThresholdPercent() < 1 || task.getFailureThresholdPercent() > 100)) {
      throw new IoTException("失败阈值必须在1-100之间");
    }
    if (task.isRollbackOnFailureFlag() && task.getFailureThresholdPercent() == null) {
      throw new IoTException("开启失败回滚时请设置失败阈值");
    }
    if (task.getRollbackPackageId() != null) {
      if (Objects.equals(task.getRollbackPackageId(), task.getPackageId())) {
        throw new IoTException("回滚升级包不能与当前升级包相同");
      }
      IoTOtaPackage rollbackPackage =
          otaPackageMapper.selectPackageById(task.getRollbackPackageId());
      if (rollbackPackage == null) {
        throw new IoTException("回滚升级包不存在");
      }
      assertOtaAccess(
          rollbackPackage.getTenantId(),
          rollbackPackage.getCreatorId(),
          "回滚升级包不存在或无权访问");
      if (!StrUtil.equals(rollbackPackage.getProductKey(), product.getProductKey())) {
        throw new IoTException("回滚升级包必须属于同一产品");
      }
      if (!OtaConstants.PackageStatus.PUBLISHED.equals(rollbackPackage.getStatus())) {
        throw new IoTException("仅已发布的升级包可用于回滚");
      }
    }
    if (!task.isAutoDispatchFlag()) {
      task.setPlannedStartTime(null);
    }
    if (task.getBatchSize() == null || task.getBatchSize() <= 0) {
      task.setBatchIntervalSeconds(0);
    }

    JSONObject strategy = buildStrategy(task);
    // 任务固定使用创建时的适配参数，之后修改产品配置不会改变正在传输的会话。
    strategy.set("transferProfile", readTransferProfile(product).toMap());
    List<IoTDevice> devices =
        applyGrayStrategy(
            resolveTaskDevices(product.getProductKey(), task.getDeviceIds(), task.getRolloutMode()),
            strategy);
    if (devices.isEmpty()) {
      throw new IoTException("未找到可升级设备");
    }
    String normalizedModule =
        StrUtil.blankToDefault(otaPackage.getModule(), OtaConstants.DEFAULT_MODULE);
    validateNoActiveTaskConflicts(product.getProductKey(), normalizedModule, devices);

    String snowflakeId = String.valueOf(IdUtil.getSnowflake().nextId());
    String taskSuffix =
        snowflakeId.length() > 4 ? snowflakeId.substring(snowflakeId.length() - 4) : snowflakeId;
    task.setTaskNo("OTA" + System.currentTimeMillis() + taskSuffix);
    task.setProductId(product.getId());
    task.setProductKey(product.getProductKey());
    task.setPackageId(otaPackage.getId());
    task.setTargetVersion(otaPackage.getVersion());
    task.setPackageType(otaPackage.getPackageType());
    task.setCustomType(otaPackage.getCustomType());
    task.setModule(normalizedModule);
    task.setCreatorId(creatorId);
    task.setTenantId(resolveTenantId(product.getCreatorId(), product.getTenantId()));
    if (StrUtil.isBlank(task.getTaskName())) {
      task.setTaskName(otaPackage.getPackageName() + " " + otaPackage.getVersion());
    }
    if (StrUtil.isBlank(task.getRolloutMode())) {
      task.setRolloutMode("SPECIFIED");
    }
    if (StrUtil.isBlank(task.getStatus())) {
      task.setStatus(OtaConstants.TaskStatus.PENDING);
    }

    task.setStrategyJson(strategy.toString());
    task.setDeviceCount(devices.size());
    task.setWaitingCount(devices.size());
    task.setInProgressCount(0);
    task.setSuccessCount(0);
    task.setFailedCount(0);
    task.setCanceledCount(0);
    task.setSkippedCount(0);
    otaTaskMapper.insertTask(task);

    int maxRetryCount = strategy.getInt("maxRetryCount", 3);
    List<IoTOtaTaskDevice> taskDevices = new ArrayList<>();
    for (IoTDevice device : devices) {
      IoTOtaDeviceVersion version =
          otaDeviceVersionMapper.selectLatestVersionByDevice(
              product.getProductKey(), device.getDeviceId());
      taskDevices.add(
          IoTOtaTaskDevice.builder()
              .taskId(task.getId())
              .tenantId(task.getTenantId())
              .packageId(otaPackage.getId())
              .productKey(product.getProductKey())
              .deviceId(device.getDeviceId())
              .iotId(device.getIotId())
              .currentVersion(version != null ? version.getCurrentVersion() : null)
              .targetVersion(otaPackage.getVersion())
              .status(OtaConstants.DeviceStatus.WAITING_PUSH)
              .progress(0)
              .retryCount(0)
              .maxRetryCount(maxRetryCount)
              .statusDetail("待下发升级任务")
              .build());
    }
    otaTaskDeviceMapper.insertTaskDeviceBatch(taskDevices);

    if (isFlagEnabled(strategy.get(OtaConstants.StrategyKey.AUTO_DISPATCH))
        && !isPlannedForFuture(strategy)) {
      otaRuntimeService.dispatchTask(task.getId());
    }
    return task.getId();
  }

  private String resolveTenantId(String creatorId, String fallbackTenantId) {
    if (StrUtil.isNotBlank(fallbackTenantId)) {
      return fallbackTenantId;
    }
    if (StrUtil.isBlank(creatorId)) {
      return null;
    }
    IoTUser creator = ioTUserService.selectUserByUnionId(creatorId);
    if (creator == null) {
      return null;
    }
    return StrUtil.blankToDefault(creator.getTenantId(), creator.getUnionId());
  }

  private void normalizePackageChecksum(IoTOtaPackage otaPackage) {
    if (otaPackage == null) {
      return;
    }
    if (StrUtil.isNotBlank(otaPackage.getChecksumAlgorithm())) {
      otaPackage.setChecksumAlgorithm(otaPackage.getChecksumAlgorithm().trim().toUpperCase());
    }
    if (StrUtil.isNotBlank(otaPackage.getChecksum())) {
      otaPackage.setChecksum(otaPackage.getChecksum().trim().toLowerCase());
    }
  }

  private void validatePackageBeforePublish(IoTOtaPackage otaPackage) {
    if (otaPackage == null || !OtaConstants.PackageStatus.PUBLISHED.equals(otaPackage.getStatus())) {
      return;
    }
    validateDispatchablePackage(otaPackage);
  }

  private void validatePackageStatus(String status) {
    if (PACKAGE_STATUSES.stream().noneMatch(item -> StrUtil.equals(item, status))) {
      throw new IoTException("升级包状态不合法");
    }
  }

  private void validateDispatchablePackage(IoTOtaPackage otaPackage) {
    if (otaPackage == null) {
      throw new IoTException("升级包不存在");
    }
    if (StrUtil.isBlank(otaPackage.getFileUrl())) {
      throw new IoTException("发布升级包必须配置文件地址");
    }
    if (otaPackage.getFileSize() == null || otaPackage.getFileSize() <= 0) {
      throw new IoTException("发布升级包必须配置有效文件大小");
    }
    if (StrUtil.isBlank(otaPackage.getChecksum())) {
      throw new IoTException("发布升级包必须配置校验值");
    }
    if (!isStrongChecksumAlgorithm(otaPackage.getChecksumAlgorithm())) {
      throw new IoTException("发布升级包必须使用 SHA256 或 SHA512 校验算法");
    }
    if (!isChecksumFormatValid(otaPackage.getChecksum(), otaPackage.getChecksumAlgorithm())) {
      throw new IoTException("升级包校验值格式与校验算法不匹配");
    }
  }

  private boolean isStrongChecksumAlgorithm(String algorithm) {
    return STRONG_CHECKSUM_ALGORITHMS.stream()
        .anyMatch(item -> StrUtil.equalsIgnoreCase(item, algorithm));
  }

  private boolean isChecksumFormatValid(String checksum, String algorithm) {
    if (StrUtil.isBlank(checksum) || StrUtil.isBlank(algorithm)) {
      return false;
    }
    int expectedLength = StrUtil.containsIgnoreCase(algorithm, "512") ? 128 : 64;
    return checksum.matches("(?i)^[0-9a-f]{" + expectedLength + "}$");
  }

  private void assertPackageEditable(IoTOtaPackage stored, IoTOtaPackage incoming) {
    if (stored == null || incoming == null) {
      throw new IoTException("OTA升级包不存在或无权访问");
    }
    boolean hasTask = otaPackageMapper.countTaskByPackageId(stored.getId()) > 0;
    boolean published = OtaConstants.PackageStatus.PUBLISHED.equals(stored.getStatus());
    if (!hasTask && !published) {
      return;
    }
    // 已发布或已建任务的包必须保持下发内容不可变，避免历史任务拿到被替换的固件。
    if (!StrUtil.equals(stored.getProductKey(), incoming.getProductKey())
        || !StrUtil.equals(stored.getVersion(), incoming.getVersion())
        || !StrUtil.equals(stored.getPackageType(), incoming.getPackageType())
        || !StrUtil.equals(normalizeModule(stored.getModule()), normalizeModule(incoming.getModule()))
        || !StrUtil.equals(stored.getFileUrl(), incoming.getFileUrl())
        || !Objects.equals(stored.getFileSize(), incoming.getFileSize())
        || !StrUtil.equalsIgnoreCase(stored.getChecksumAlgorithm(), incoming.getChecksumAlgorithm())
        || !StrUtil.equalsIgnoreCase(stored.getChecksum(), incoming.getChecksum())
        || !StrUtil.equals(stored.getSignatureUrl(), incoming.getSignatureUrl())) {
      throw new IoTException("已发布或已关联任务的升级包不能修改版本、文件或校验信息");
    }
  }

  @Override
  public OtaServiceResult dispatchTask(Long taskId) {
    selectTaskById(taskId);
    return otaRuntimeService.dispatchTask(taskId);
  }

  @Override
  public OtaServiceResult cancelTask(Long taskId) {
    selectTaskById(taskId);
    return otaRuntimeService.cancelTask(taskId);
  }

  @Override
  public OtaServiceResult retryTaskDevice(Long taskDeviceId) {
    IoTOtaTaskDevice taskDevice = otaTaskDeviceMapper.selectTaskDeviceById(taskDeviceId);
    if (taskDevice == null) {
      throw new IoTException("OTA任务设备不存在");
    }
    selectTaskById(taskDevice.getTaskId());
    return otaRuntimeService.retryTaskDevice(taskDeviceId);
  }

  @Override
  public Map<String, Object> getProductSummary(String productKey) {
    assertProductAccess(productKey);
    Map<String, Object> summary = new LinkedHashMap<>();
    List<IoTOtaPackage> recentPackages =
        otaPackageMapper.selectRecentPackagesByProductKey(productKey);
    recentPackages.forEach(this::fillPackageTags);
    List<IoTOtaTask> recentTasks = otaTaskMapper.selectRecentTasksByProductKey(productKey);
    List<Map<String, Object>> versionDistribution =
        otaDeviceVersionMapper.selectVersionDistributionByProductKey(productKey);

    IoTOtaPackage packageQuery = IoTOtaPackage.builder().productKey(productKey).build();
    List<IoTOtaPackage> allPackages = otaPackageMapper.selectPackageList(packageQuery);
    IoTOtaTask taskQuery = IoTOtaTask.builder().productKey(productKey).build();
    List<IoTOtaTask> allTasks = otaTaskMapper.selectTaskList(taskQuery);

    long publishedCount =
        allPackages.stream()
            .filter(item -> OtaConstants.PackageStatus.PUBLISHED.equals(item.getStatus()))
            .count();
    long runningCount =
        allTasks.stream()
            .filter(
                item ->
                    OtaConstants.TaskStatus.RUNNING.equals(item.getStatus())
                        || OtaConstants.TaskStatus.PENDING.equals(item.getStatus()))
            .count();

    summary.put("packageTotal", allPackages.size());
    summary.put("publishedPackageTotal", publishedCount);
    summary.put("taskTotal", allTasks.size());
    summary.put("runningTaskTotal", runningCount);
    summary.put("recentPackages", recentPackages);
    summary.put("recentTasks", recentTasks);
    summary.put("versionDistribution", versionDistribution);
    return summary;
  }

  @Override
  public Map<String, Object> getDeviceSummary(String productKey, String deviceId) {
    assertProductAccess(productKey);
    assertDeviceAccess(productKey, deviceId);
    IoTOtaDeviceVersion currentVersion =
        otaDeviceVersionMapper.selectLatestVersionByDevice(productKey, deviceId);
    IoTOtaTaskDevice activeTask =
        otaTaskDeviceMapper.selectLatestActiveByDevice(productKey, deviceId);
    List<IoTOtaTaskDevice> recentRecords =
        Objects.requireNonNullElse(
            otaTaskDeviceMapper.selectRecentByDevice(productKey, deviceId), Collections.emptyList());
    List<IoTOtaPackage> publishedPackages = listPublishedPackagesByProductKey(productKey);
    List<IoTOtaPackage> candidatePackages =
        resolveDeviceCandidatePackages(currentVersion, activeTask, publishedPackages);
    IoTOtaPackage recommendedPackage = resolveRecommendedPackage(currentVersion, candidatePackages);
    List<IoTOtaPackage> recentPublishedPackages =
        publishedPackages.stream().limit(5).collect(Collectors.toList());

    candidatePackages.forEach(this::fillPackageTags);
    recentPublishedPackages.forEach(this::fillPackageTags);

    Map<String, Object> summary = new LinkedHashMap<>();
    summary.put("currentVersion", currentVersion);
    summary.put("activeTask", activeTask);
    summary.put("latestRecord", recentRecords.isEmpty() ? activeTask : recentRecords.get(0));
    summary.put("recentRecords", recentRecords);
    summary.put("recommendedPackage", recommendedPackage);
    summary.put("candidatePackages", candidatePackages);
    summary.put("availablePackages", recentPublishedPackages);
    return summary;
  }

  @Override
  public IoTOtaDeviceVersion getLatestDeviceVersion(String productKey, String deviceId) {
    assertProductAccess(productKey);
    assertDeviceAccess(productKey, deviceId);
    return otaDeviceVersionMapper.selectLatestVersionByDevice(productKey, deviceId);
  }

  private void assertProductAccess(String productKey) {
    if (StrUtil.isBlank(productKey)) {
      return;
    }
    IoTProduct product = productMapper.getProductByProductKey(productKey);
    if (product == null) {
      throw new IoTException("产品不存在");
    }
    assertOtaAccess(product.getTenantId(), product.getCreatorId(), "无产品权限");
  }

  private void assertOtaAccess(String tenantId, String creatorId, String message) {
    IoTUser currentUser = currentTenantUser();
    if (currentUser == null || currentUser.isAdmin()) {
      return;
    }
    String currentTenantId = resolveTenantId(currentUser.getUnionId(), currentUser.getTenantId());
    String resourceTenantId = StrUtil.blankToDefault(tenantId, resolveTenantId(creatorId, null));
    if (StrUtil.isBlank(resourceTenantId)) {
      if (!StrUtil.equals(currentUser.getUnionId(), creatorId)) {
        throw new IoTException(message);
      }
      return;
    }
    if (!StrUtil.equals(currentTenantId, resourceTenantId)) {
      throw new IoTException(message);
    }
  }

  private IoTUser currentTenantUser() {
    String unionId = SecurityUtils.getUnionIdQuietly();
    if (StrUtil.isBlank(unionId)) {
      return null;
    }
    IoTUser currentUser = ioTUserService.selectUserByUnionId(unionId);
    if (currentUser == null) {
      return null;
    }
    return currentUser;
  }

  private void fillPackageTags(IoTOtaPackage otaPackage) {
    if (otaPackage == null) {
      return;
    }
    if (StrUtil.isNotBlank(otaPackage.getTagIdsText())) {
      otaPackage.setTagIds(
          Arrays.stream(otaPackage.getTagIdsText().split(","))
              .filter(StrUtil::isNotBlank)
              .map(Long::valueOf)
              .collect(Collectors.toList()));
    } else if (otaPackage.getId() != null) {
      otaPackage.setTagIds(otaPackageTagRelMapper.selectTagIdsByPackageId(otaPackage.getId()));
    } else {
      otaPackage.setTagIds(Collections.emptyList());
    }
    if (StrUtil.isNotBlank(otaPackage.getTagNamesText())) {
      otaPackage.setTagNames(
          Arrays.stream(otaPackage.getTagNamesText().split(","))
              .filter(StrUtil::isNotBlank)
              .collect(Collectors.toList()));
    } else {
      otaPackage.setTagNames(Collections.emptyList());
    }
  }

  private void savePackageTags(Long packageId, List<Long> tagIds) {
    if (packageId == null || tagIds == null || tagIds.isEmpty()) {
      return;
    }
    List<Long> uniqueTagIds =
        tagIds.stream().filter(Objects::nonNull).distinct().collect(Collectors.toList());
    if (!uniqueTagIds.isEmpty()) {
      otaPackageTagRelMapper.insertBatch(packageId, uniqueTagIds);
    }
  }

  private IoTProduct resolveProduct(Long productId, String productKey) {
    if (productId != null) {
      return productMapper.selectDevProductById(String.valueOf(productId));
    }
    if (StrUtil.isNotBlank(productKey)) {
      return productMapper.getProductByProductKey(productKey);
    }
    return null;
  }

  private List<IoTDevice> resolveTaskDevices(
      String productKey, List<String> deviceIds, String rolloutMode) {
    List<IoTDevice> devices =
        ioTDeviceService.selectDevInstanceList(
            IoTDevice.builder().productKey(productKey).build(), null);
    if (devices == null || devices.isEmpty()) {
      return Collections.emptyList();
    }
    if (usesProductDeviceCandidates(rolloutMode)) {
      return devices;
    }
    if (deviceIds == null || deviceIds.isEmpty()) {
      return Collections.emptyList();
    }
    List<String> filterIds =
        deviceIds.stream().filter(StrUtil::isNotBlank).distinct().collect(Collectors.toList());
    if (filterIds.isEmpty()) {
      return Collections.emptyList();
    }
    List<IoTDevice> filteredDevices =
        devices.stream()
        .filter(device -> filterIds.contains(device.getDeviceId()))
        .collect(Collectors.toList());
    if (filteredDevices.size() != filterIds.size()) {
      throw new IoTException("存在无权限或不存在的设备");
    }
    return filteredDevices;
  }

  private boolean usesProductDeviceCandidates(String rolloutMode) {
    return "ALL".equalsIgnoreCase(rolloutMode) || "GRAY".equalsIgnoreCase(rolloutMode);
  }

  private void validateTagAccess(List<Long> tagIds, String tenantId) {
    if (tagIds == null || tagIds.isEmpty()) {
      return;
    }
    for (Long tagId : tagIds) {
      if (tagId == null) {
        continue;
      }
      IoTOtaTag tag = otaTagMapper.selectTagById(tagId);
      assertOtaAccess(
          tag == null ? null : tag.getTenantId(),
          tag == null ? null : tag.getCreatorId(),
          "OTA标签不存在或无权访问");
      if (tag == null) {
        throw new IoTException("OTA标签不存在或无权访问");
      }
      String tagTenantId =
          StrUtil.blankToDefault(tag.getTenantId(), resolveTenantId(tag.getCreatorId(), null));
      if (StrUtil.isNotBlank(tenantId)
          && StrUtil.isNotBlank(tagTenantId)
          && !StrUtil.equals(tenantId, tagTenantId)) {
        throw new IoTException("升级包标签归属租户不一致");
      }
    }
  }

  private void assertDeviceAccess(String productKey, String deviceId) {
    IoTDevice device = ioTDeviceService.selectIoTDevice(productKey, deviceId);
    if (device == null) {
      throw new IoTException("设备不存在");
    }
  }

  private List<IoTOtaPackage> listPublishedPackagesByProductKey(String productKey) {
    if (StrUtil.isBlank(productKey)) {
      return Collections.emptyList();
    }
    IoTOtaPackage query = new IoTOtaPackage();
    query.setProductKey(productKey);
    query.setStatus(OtaConstants.PackageStatus.PUBLISHED);
    List<IoTOtaPackage> packages = otaPackageMapper.selectPackageList(query);
    return packages == null ? Collections.emptyList() : packages;
  }

  private List<IoTOtaPackage> resolveDeviceCandidatePackages(
      IoTOtaDeviceVersion currentVersion,
      IoTOtaTaskDevice activeTask,
      List<IoTOtaPackage> publishedPackages) {
    if (publishedPackages == null || publishedPackages.isEmpty()) {
      return Collections.emptyList();
    }
    String module =
        normalizeModule(
            currentVersion != null ? currentVersion.getModule() : activeTask == null ? null : activeTask.getModule());
    String packageType =
        normalizePackageType(
            currentVersion != null
                ? currentVersion.getPackageType()
                : activeTask == null ? null : activeTask.getPackageType());
    String customType =
        trimToNull(
            currentVersion != null && StrUtil.isNotBlank(currentVersion.getCustomType())
                ? currentVersion.getCustomType()
                : activeTask == null ? null : activeTask.getCustomType());
    return publishedPackages.stream()
        .filter(Objects::nonNull)
        .filter(item -> matchesDeviceContext(item, module, packageType, customType))
        .filter(item -> matchesDeviceConstraints(item, currentVersion))
        .sorted(this::comparePackagePriority)
        .limit(4)
        .collect(Collectors.toList());
  }

  private IoTOtaPackage resolveRecommendedPackage(
      IoTOtaDeviceVersion currentVersion, List<IoTOtaPackage> candidatePackages) {
    if (currentVersion == null
        || StrUtil.isBlank(currentVersion.getCurrentVersion())
        || candidatePackages == null
        || candidatePackages.isEmpty()) {
      return null;
    }
    return candidatePackages.stream()
        .filter(Objects::nonNull)
        .filter(item -> OtaVersionUtils.isTargetNewer(currentVersion.getCurrentVersion(), item.getVersion()))
        .findFirst()
        .orElse(null);
  }

  private boolean matchesDeviceContext(
      IoTOtaPackage otaPackage, String module, String packageType, String customType) {
    if (!StrUtil.equals(normalizeModule(otaPackage.getModule()), module)) {
      return false;
    }
    if (!StrUtil.equals(normalizePackageType(otaPackage.getPackageType()), packageType)) {
      return false;
    }
    String packageCustomType = trimToNull(otaPackage.getCustomType());
    if (StrUtil.isBlank(packageCustomType)) {
      return true;
    }
    return StrUtil.isNotBlank(customType) && StrUtil.equals(packageCustomType, customType);
  }

  private boolean matchesDeviceConstraints(
      IoTOtaPackage otaPackage, IoTOtaDeviceVersion currentVersion) {
    if (otaPackage == null) {
      return false;
    }
    String currentSoftwareVersion =
        trimToNull(currentVersion == null ? null : currentVersion.getCurrentVersion());
    String hardwareVersion =
        trimToNull(currentVersion == null ? null : currentVersion.getHardwareVersion());
    if (StrUtil.isNotBlank(otaPackage.getMinSoftwareVersion())) {
      if (StrUtil.isBlank(currentSoftwareVersion)
          || OtaVersionUtils.compareVersion(
                  currentSoftwareVersion, otaPackage.getMinSoftwareVersion())
              < 0) {
        return false;
      }
    }
    if (StrUtil.isNotBlank(otaPackage.getMinHardwareVersion())) {
      if (StrUtil.isBlank(hardwareVersion)
          || OtaVersionUtils.compareVersion(hardwareVersion, otaPackage.getMinHardwareVersion())
              < 0) {
        return false;
      }
    }
    if (StrUtil.isNotBlank(otaPackage.getMaxHardwareVersion())) {
      if (StrUtil.isBlank(hardwareVersion)
          || OtaVersionUtils.compareVersion(hardwareVersion, otaPackage.getMaxHardwareVersion())
              > 0) {
        return false;
      }
    }
    return true;
  }

  private int comparePackagePriority(IoTOtaPackage left, IoTOtaPackage right) {
    if (left == right) {
      return 0;
    }
    if (left == null) {
      return 1;
    }
    if (right == null) {
      return -1;
    }
    int versionCompare =
        OtaVersionUtils.compareVersion(
            StrUtil.blankToDefault(right.getVersion(), ""),
            StrUtil.blankToDefault(left.getVersion(), ""));
    if (versionCompare != 0) {
      return versionCompare;
    }
    int forceCompare = Boolean.compare(right.isForceUpgradeFlag(), left.isForceUpgradeFlag());
    if (forceCompare != 0) {
      return forceCompare;
    }
    long rightTime = right.getCreateTime() == null ? 0L : right.getCreateTime().getTime();
    long leftTime = left.getCreateTime() == null ? 0L : left.getCreateTime().getTime();
    if (rightTime != leftTime) {
      return Long.compare(rightTime, leftTime);
    }
    return Comparator.nullsLast(Long::compareTo).compare(right.getId(), left.getId());
  }

  private String normalizeModule(String module) {
    return StrUtil.blankToDefault(trimToNull(module), OtaConstants.DEFAULT_MODULE);
  }

  private String normalizePackageType(String packageType) {
    return StrUtil.blankToDefault(trimToNull(packageType), OtaConstants.DEFAULT_PACKAGE_TYPE);
  }

  private String trimToNull(String value) {
    if (StrUtil.isBlank(value)) {
      return null;
    }
    return value.trim();
  }

  private JSONObject buildStrategy(IoTOtaTask task) {
    JSONObject strategy =
        StrUtil.isNotBlank(task.getStrategyJson())
                && JSONUtil.isTypeJSONObject(task.getStrategyJson())
            ? JSONUtil.parseObj(task.getStrategyJson())
            : new JSONObject();
    Long plannedStartTime =
        task.getPlannedStartTime() != null
            ? Long.valueOf(task.getPlannedStartTime().getTime())
            : strategy.getLong(OtaConstants.StrategyKey.PLANNED_START_TIME);
    Long rollbackPackageId =
        task.getRollbackPackageId() != null
            ? task.getRollbackPackageId()
            : strategy.getLong(OtaConstants.StrategyKey.ROLLBACK_PACKAGE_ID);
    strategy.set(
        OtaConstants.StrategyKey.MAX_RETRY_COUNT,
        task.getMaxRetryCount() != null
            ? task.getMaxRetryCount()
            : strategy.getInt(OtaConstants.StrategyKey.MAX_RETRY_COUNT, 3));
    strategy.set(
        OtaConstants.StrategyKey.TIMEOUT_SECONDS,
        task.getTimeoutSeconds() != null
            ? task.getTimeoutSeconds()
            : strategy.getInt(OtaConstants.StrategyKey.TIMEOUT_SECONDS, 3600));
    strategy.set(
        OtaConstants.StrategyKey.REQUIRE_VERSION_CHECK,
        task.getRequireVersionCheck() != null
            ? task.getRequireVersionCheck()
            : strategy.getBool(OtaConstants.StrategyKey.REQUIRE_VERSION_CHECK, true));
    strategy.set(
        OtaConstants.StrategyKey.AUTO_DISPATCH,
        normalizeFlagValue(
            task.getAutoDispatch(),
            normalizeFlagValue(
                strategy.get(OtaConstants.StrategyKey.AUTO_DISPATCH), IoTConstant.ZERO_FALSE)));
    strategy.set(OtaConstants.StrategyKey.PLANNED_START_TIME, plannedStartTime);
    strategy.set(
        OtaConstants.StrategyKey.BATCH_SIZE,
        task.getBatchSize() != null
            ? task.getBatchSize()
            : strategy.getInt(OtaConstants.StrategyKey.BATCH_SIZE, 0));
    strategy.set(
        OtaConstants.StrategyKey.BATCH_INTERVAL_SECONDS,
        task.getBatchIntervalSeconds() != null
            ? task.getBatchIntervalSeconds()
            : strategy.getInt(OtaConstants.StrategyKey.BATCH_INTERVAL_SECONDS, 0));
    strategy.set(
        OtaConstants.StrategyKey.GRAY_PERCENT,
        task.getGrayPercent() != null
            ? task.getGrayPercent()
            : strategy.getInt(OtaConstants.StrategyKey.GRAY_PERCENT, 100));
    strategy.set(
        OtaConstants.StrategyKey.GRAY_DEVICE_LIMIT,
        task.getGrayDeviceLimit() != null
            ? task.getGrayDeviceLimit()
            : strategy.getInt(OtaConstants.StrategyKey.GRAY_DEVICE_LIMIT, 0));
    strategy.set(
        OtaConstants.StrategyKey.FAILURE_THRESHOLD_PERCENT,
        task.getFailureThresholdPercent() != null
            ? task.getFailureThresholdPercent()
            : strategy.getInt(OtaConstants.StrategyKey.FAILURE_THRESHOLD_PERCENT, 0));
    strategy.set(
        OtaConstants.StrategyKey.ROLLBACK_ON_FAILURE,
        normalizeFlagValue(
            task.getRollbackOnFailure(),
            normalizeFlagValue(
                strategy.get(OtaConstants.StrategyKey.ROLLBACK_ON_FAILURE),
                IoTConstant.ZERO_FALSE)));
    strategy.set(OtaConstants.StrategyKey.ROLLBACK_PACKAGE_ID, rollbackPackageId);
    return strategy;
  }

  private Integer normalizeFlagValue(Object value, Integer defaultValue) {
    if (value == null) {
      return defaultValue;
    }
    if (value instanceof Boolean booleanValue) {
      return booleanValue ? IoTConstant.ONE_TRUE : IoTConstant.ZERO_FALSE;
    }
    Integer normalized = Convert.toInt(value, defaultValue);
    if (normalized == null) {
      return defaultValue;
    }
    return Objects.equals(normalized, IoTConstant.ONE_TRUE)
        ? IoTConstant.ONE_TRUE
        : IoTConstant.ZERO_FALSE;
  }

  private boolean isFlagEnabled(Object value) {
    return Objects.equals(normalizeFlagValue(value, IoTConstant.ZERO_FALSE), IoTConstant.ONE_TRUE);
  }

  private List<IoTDevice> applyGrayStrategy(List<IoTDevice> devices, JSONObject strategy) {
    if (devices == null || devices.isEmpty()) {
      return Collections.emptyList();
    }
    List<IoTDevice> sortedDevices =
        devices.stream()
            .filter(Objects::nonNull)
            .sorted((left, right) -> StrUtil.compare(left.getDeviceId(), right.getDeviceId(), true))
            .collect(Collectors.toList());
    int total = sortedDevices.size();
    int grayPercent = strategy.getInt(OtaConstants.StrategyKey.GRAY_PERCENT, 100);
    int grayDeviceLimit = strategy.getInt(OtaConstants.StrategyKey.GRAY_DEVICE_LIMIT, 0);
    int selectedCount = total;
    boolean grayConfigured = (grayPercent > 0 && grayPercent < 100) || grayDeviceLimit > 0;
    if (grayPercent > 0 && grayPercent < 100) {
      selectedCount = Math.max(1, (int) Math.ceil(total * (grayPercent / 100D)));
    }
    if (grayDeviceLimit > 0) {
      selectedCount = Math.min(selectedCount, grayDeviceLimit);
    }
    if (grayConfigured) {
      selectedCount = Math.min(selectedCount, total);
    } else {
      selectedCount = total;
    }
    strategy.set(OtaConstants.StrategyKey.CANDIDATE_DEVICE_COUNT, total);
    strategy.set(OtaConstants.StrategyKey.SELECTED_DEVICE_COUNT, selectedCount);
    return new ArrayList<>(sortedDevices.subList(0, selectedCount));
  }

  private boolean isPlannedForFuture(JSONObject strategy) {
    Long plannedStartTime = strategy.getLong(OtaConstants.StrategyKey.PLANNED_START_TIME);
    return plannedStartTime != null && plannedStartTime > System.currentTimeMillis();
  }

  private void validateNoActiveTaskConflicts(
      String productKey, String module, List<IoTDevice> devices) {
    if (StrUtil.isBlank(productKey)
        || StrUtil.isBlank(module)
        || devices == null
        || devices.isEmpty()) {
      return;
    }
    List<String> deviceIds =
        devices.stream()
            .map(IoTDevice::getDeviceId)
            .filter(StrUtil::isNotBlank)
            .distinct()
            .collect(Collectors.toList());
    if (deviceIds.isEmpty()) {
      return;
    }

    LinkedHashMap<String, IoTOtaTaskDevice> conflictMap = new LinkedHashMap<>();
    for (int start = 0; start < deviceIds.size(); start += ACTIVE_TASK_CONFLICT_QUERY_BATCH_SIZE) {
      int end = Math.min(start + ACTIVE_TASK_CONFLICT_QUERY_BATCH_SIZE, deviceIds.size());
      List<IoTOtaTaskDevice> conflicts =
          otaTaskDeviceMapper.selectActiveConflictsByDeviceIds(
              productKey, module, deviceIds.subList(start, end));
      if (conflicts == null || conflicts.isEmpty()) {
        continue;
      }
      for (IoTOtaTaskDevice conflict : conflicts) {
        if (conflict != null && StrUtil.isNotBlank(conflict.getDeviceId())) {
          conflictMap.putIfAbsent(conflict.getDeviceId(), conflict);
        }
      }
      if (conflictMap.size() >= 10) {
        break;
      }
    }

    if (conflictMap.isEmpty()) {
      return;
    }

    String preview =
        conflictMap.values().stream()
            .limit(5)
            .map(
                conflict ->
                    conflict.getDeviceId()
                        + "(任务:"
                        + StrUtil.blankToDefault(
                            conflict.getTaskName(),
                            StrUtil.blankToDefault(
                                conflict.getTaskNo(), String.valueOf(conflict.getTaskId())))
                        + ")")
            .collect(Collectors.joining("，"));
    throw new IoTException("以下设备在模块[" + module + "]已有进行中的 OTA 任务，请先完成或取消后再创建：" + preview);
  }
}
