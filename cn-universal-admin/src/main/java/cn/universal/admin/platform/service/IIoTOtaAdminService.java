/*
 *
 * Copyright (c) 2025, NexIoT. All Rights Reserved.
 *
 * @Description: OTA管理服务接口
 * @Author: gitee.com/NexIoT
 *
 */

package cn.universal.admin.platform.service;

import cn.universal.dm.device.service.ota.api.OtaService.OtaServiceResult;
import cn.universal.persistence.entity.IoTOtaDeviceVersion;
import cn.universal.persistence.entity.IoTOtaPackage;
import cn.universal.persistence.entity.IoTOtaTag;
import cn.universal.persistence.entity.IoTOtaTask;
import cn.universal.persistence.entity.IoTOtaTaskDevice;
import java.util.List;
import java.util.Map;

/** OTA管理服务接口 */
public interface IIoTOtaAdminService {

  Map<String, Object> getTransferProfile(String productKey);

  Map<String, Object> saveTransferProfile(String productKey, Map<String, Object> profile);

  List<IoTOtaTag> selectTagList(IoTOtaTag query, String creatorId, boolean admin);

  IoTOtaTag selectTagById(Long id);

  Long saveTag(IoTOtaTag tag, String creatorId);

  int deleteTagById(Long id);

  List<IoTOtaPackage> selectPackageList(IoTOtaPackage query, String creatorId, boolean admin);

  IoTOtaPackage selectPackageById(Long id);

  Long savePackage(IoTOtaPackage otaPackage, String creatorId);

  int updatePackageStatus(Long id, String status);

  int deletePackageById(Long id);

  List<IoTOtaTask> selectTaskList(IoTOtaTask query, String creatorId, boolean admin);

  IoTOtaTask selectTaskById(Long id);

  List<IoTOtaTaskDevice> selectTaskDeviceList(IoTOtaTaskDevice query);

  Long createTask(IoTOtaTask task, String creatorId);

  OtaServiceResult dispatchTask(Long taskId);

  OtaServiceResult cancelTask(Long taskId);

  OtaServiceResult retryTaskDevice(Long taskDeviceId);

  Map<String, Object> getProductSummary(String productKey);

  Map<String, Object> getDeviceSummary(String productKey, String deviceId);

  IoTOtaDeviceVersion getLatestDeviceVersion(String productKey, String deviceId);
}
