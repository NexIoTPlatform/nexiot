/*
 *
 * Copyright (c) 2025, NexIoT. All Rights Reserved.
 *
 * @Description: 本文件由 gitee.com/NexIoT 开发并拥有版权，未经授权严禁擅自商用、复制或传播。
 * @Author: gitee.com/NexIoT
 * @Email: wo8335224@gmail.com
 * @Wechat: outlookFil
 *
 *
 */

package cn.universal.admin.platform.service;

import cn.universal.common.domain.R;
import cn.universal.common.poi.ExcelTemplate;
import cn.universal.persistence.dto.IoTDeviceDeletePreview;
import cn.universal.persistence.dto.IoTDeviceExportDTO;
import cn.universal.persistence.entity.IoTDevice;
import cn.universal.persistence.entity.IoTDeviceShadow;
import cn.universal.persistence.entity.IoTProduct;
import cn.universal.persistence.entity.IoTTaskItem;
import cn.universal.persistence.entity.IoTUser;
import cn.universal.persistence.entity.bo.IoTDeviceBO;
import cn.universal.persistence.entity.bo.IoTDeviceOwnerBO;
import cn.universal.persistence.entity.bo.IoTGwDeviceBO;
import cn.universal.persistence.entity.bo.IoTSubDeviceGatewayBindBO;
import cn.universal.persistence.entity.vo.GwIoTDeviceVo;
import cn.universal.persistence.entity.vo.IoTDeviceCompanyVO;
import cn.universal.persistence.entity.vo.IoTDeviceCountVO;
import cn.universal.persistence.entity.vo.IoTDeviceModelVO;
import cn.universal.persistence.entity.vo.IoTDeviceVO;
import com.github.pagehelper.Page;
import jakarta.servlet.http.HttpServletResponse;
import java.io.InputStream;
import java.util.List;

/**
 * 设备Service接口
 *
 * @since 2025-12-24
 */
public interface IIoTDeviceService {

  /**
   * 查询设备分组
   *
   * @param iotId iotId
   */
  List<String> selectDevInstanceGroupName(String iotId);

  /**
   * 查询设备
   *
   * @param id 设备主键
   * @return 设备
   */
  public IoTDevice selectDevInstanceById(String id);

  IoTDevice selectDevInstanceById(String id, IoTUser scopeUser);

  List<IoTDeviceCountVO> countDevByApplication(String appUniqueId);

  /**
   * 查询设备列表
   *
   * @param ioTDevice 设备
   * @return 设备集合
   */
  public List<IoTDevice> selectDevInstanceList(IoTDevice ioTDevice, IoTUser iotUser);

  public List<IoTDevice> selectDevInstanceUnList(IoTDevice ioTDevice, IoTUser iotUser);

  /** 导出(旧版-仅设备表字段) */
  @Deprecated
  List<IoTDevice> selectDevInstanceForExport(IoTDevice ioTDevice, IoTUser iotUser);

  /** 导出设备数据(包含产品信息) */
  List<IoTDeviceExportDTO> selectDeviceForExport(IoTDevice ioTDevice, IoTUser iotUser);

  /** 设备批量导入 */
  public int importDevice(List<IoTDevice> devicesImport, IoTProduct ioTProduct, String deviceId);

  // 设备应用绑定
  int bindApp(String ids, String appUniqueId);

  /**
   * 新增设备
   *
   * @param devInstancebo 设备
   * @return 结果
   */
  public R insertDevInstance(IoTDeviceBO devInstancebo);

  /**
   * 修改设备
   *
   * @param devInstancebo 设备
   * @return 结果
   */
  public R updateDevInstance(IoTDeviceBO devInstancebo);

  /**
   * 修改设备归属账号
   *
   * @param id 设备主键
   * @param ownerBO 目标归属
   * @return 结果
   */
  R updateDevInstanceOwner(String id, IoTDeviceOwnerBO ownerBO);

  /** 修改子设备所属网关 */
  R updateSubDeviceGateway(String id, IoTSubDeviceGatewayBindBO bindBO);

  /** 查询子设备可换绑的网关候选 */
  List<GwIoTDeviceVo> selectSubDeviceGatewayCandidates(String id);

  /**
   * 批量删除设备
   *
   * @param ids 需要删除的设备主键集合
   * @return 结果
   */
  public R deleteDevInstanceByIds(String[] ids);

  /** Preview resources and gateway relations affected by deleting devices. */
  List<IoTDeviceDeletePreview> previewDeleteDevInstances(String[] ids);

  /**
   * 删除设备信息
   *
   * @param id 设备主键
   * @return 结果
   */
  public R deleteDevInstanceById(String id);

  /** 根据应用id查找设备 */
  public int selectDevByAppUniqueId(String[] ids);

  /**
   * 查询所以设备型号
   *
   * @param supportChild 等于true 查询支持子设备的型号
   * @return 设备型号
   */
  List<IoTDeviceCompanyVO> queryAllBySupportChild(Boolean supportChild, String apps);

  /**
   * 查询所以设备型号
   *
   * @return 设备影子
   */
  IoTDeviceShadow getDeviceShadow(String id);

  /**
   * 批量查询设备影子
   *
   * @param ids 设备 IoT ID 列表
   * @return 设备影子列表
   */
  List<IoTDeviceShadow> getDeviceShadows(List<String> ids);

  /**
   * 测试 Modbus 设备端点连通性。
   *
   * @param id 设备主键
   * @return 测试结果
   */
  R probeModbusEndpoint(String id);

  /**
   * 设备下发
   *
   * @param productKey 产品key
   * @param downRequest 下发内容
   * @return 结果
   */
  public R iotFunctionsDown(String productKey, String downRequest);

  /**
   * 根据产品key查型号配置
   *
   * @param productKey 产品key
   * @return 结果
   */
  public IoTDeviceModelVO getModelByProductKey(String productKey);

  List<IoTDevice> selectNotBindDevInstanceList(
      String groupId, IoTDevice ioTDevice, IoTUser iotUser);

  Page<IoTDeviceVO> selectFenceDevice(IoTDeviceBO ioTDeviceBO, int page, int size);

  ExcelTemplate importTemplateExcel(String productKey);

  ExcelTemplate importTemplateExcel(String productKey, boolean includeIndustrialDifferences);

  List<IoTDevice> parseImportExcel(IoTProduct ioTProduct, InputStream inputStream) throws Exception;

  void exportFailedImportTemplate(
      String productKey,
      String taskName,
      List<IoTTaskItem> failedItems,
      HttpServletResponse response)
      throws Exception;

  List<GwIoTDeviceVo> selectGwIotDevices(IoTGwDeviceBO bo);

  IoTDevice selectIoTDevice(String productKey, String deviceId);

  List<IoTDevice> selectDevInstanceListWithTags(IoTDeviceBO ioTDeviceBO, IoTUser iotUser);

  /** 新：按第三方平台过滤设备列表（不影响旧接口） */
  List<IoTDevice> selectDevInstanceListByPlatform(
      IoTDevice ioTDevice, IoTUser iotUser, String thirdPlatform);

  /** 根据网关设备查询子设备关系 */
  List<GwIoTDeviceVo> selectSubDeviceRelationByGw(String gwProductKey, String gwDeviceId);

  /**
   * 查询视频设备列表（强制过滤 deviceNode=VIDEO_DEVICE） 用于视频墙等需要只展示监控设备的场景
   *
   * @param keyword 关键词（设备名称/设备序列号模糊搜索）
   * @param iotUser 当前用户
   * @return 视频设备列表
   */
  List<IoTDevice> selectVideoDeviceList(String keyword, IoTUser iotUser);
}
