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

package cn.universal.persistence.mapper;

import cn.universal.persistence.common.BaseMapper;
import cn.universal.persistence.dto.IoTDeviceDTO;
import cn.universal.persistence.dto.IoTDeviceExportDTO;
import cn.universal.persistence.dto.IoTDeviceMetadataBO;
import cn.universal.persistence.entity.IoTDevice;
import cn.universal.persistence.entity.IoTProduct;
import cn.universal.persistence.entity.bo.IoTDeviceBO;
import cn.universal.persistence.entity.bo.IoTDeviceHistoryBO;
import cn.universal.persistence.entity.bo.IoTGwDeviceBO;
import cn.universal.persistence.entity.bo.NetworkBO;
import cn.universal.persistence.entity.vo.GwIoTDeviceVo;
import cn.universal.persistence.entity.vo.DashboardMapDeviceVO;
import cn.universal.persistence.entity.vo.IoTDeviceCompanyVO;
import cn.universal.persistence.entity.vo.IoTDeviceCountVO;
import cn.universal.persistence.entity.vo.IoTDeviceDetailVO;
import cn.universal.persistence.entity.vo.IoTDeviceModelVO;
import cn.universal.persistence.entity.vo.IoTDeviceVO;
import cn.universal.persistence.entity.vo.VideoCatalogDeviceRow;
import cn.universal.persistence.entity.vo.VideoCatalogChannelCount;
import cn.universal.persistence.entity.vo.VideoCatalogConfigurationChannel;
import cn.universal.persistence.entity.VideoPlatformChannel;
import cn.universal.persistence.query.IoTAPIQuery;
import cn.universal.persistence.query.IoTDeviceQuery;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.ibatis.annotations.Param;

public interface IoTDeviceMapper extends BaseMapper<IoTDevice> {

  /**
   * 查询设备导出数据(包含产品信息)
   *
   * @param ioTDevice 设备查询条件
   * @param unionId 用户ID
   * @return 设备导出DTO列表
   */
  List<IoTDeviceExportDTO> selectDeviceForExport(
      @Param("ioTDevice") IoTDevice ioTDevice, @Param("unionId") String unionId);

  /**
   * deviceId 设备编号,extDeviceId 扩展设备编号,iotId 设备统一编号
   *
   * @param map
   * @return IoTDeviceDTO
   */
  IoTDeviceDTO selectIoTDeviceBO(Map<String, Object> map);

  /**
   * 查询设备信息。分页查询
   *
   * @param map deviceId 设备编号,extDeviceId 扩展设备编号,iotId 设备统一编号
   * @return IoTDeviceDTO
   */
  List<IoTDeviceDTO> selectDevInstanceBOV2List(Map<String, Object> map);

  /** 计算应用设备数量 */
  List<IoTDeviceCountVO> countDevByApplication(String appUniqueId);

  /** 查询设备元数据 */
  IoTDeviceMetadataBO selectDevMetadataBo(String iotId);

  List<IoTDevice> selectList(
      @Param("productKey") String productKey, @Param("start") int start, @Param("limit") int limit);

  List<IoTDevice> selectListByProductKey(@Param("productKey") String productKey);

  IoTDeviceVO apiDeviceInfo(IoTAPIQuery apiQuery);

  List<IoTDeviceVO> apiDeviceList(IoTAPIQuery apiQuery);
  
    /**
     * V2版本设备列表查询 - 返回完整字段
     * 用于SaaS层设备导入获取完整设备信息
     *
     * @param apiQuery 查询条件
     * @return 设备详情列表（包含密钥、配置、标签等完整字段）
     */
    List<IoTDeviceDetailVO> apiDeviceListV2(IoTAPIQuery apiQuery);

  /** 查询设备 */
  IoTDevice selectDevInstanceById(String id);

  /** 查询设备列表 */
  List<IoTDevice> selectDevInstanceList(
      @Param("ioTDevice") IoTDevice ioTDevice, @Param("unionId") String unionId);

  /** 按第三方平台过滤的设备列表（新接口专用） */
  List<IoTDevice> selectDevInstanceListByPlatform(
      @Param("ioTDevice") IoTDevice ioTDevice,
      @Param("thirdPlatform") String thirdPlatform,
      @Param("unionId") String unionId);

  List<IoTDevice> selectDevInstanceUnList(
      @Param("ioTDevice") IoTDevice ioTDevice, @Param("unionId") String unionId);

  List<IoTDevice> selectDevInstanceForExport(
      @Param("ioTDevice") IoTDevice ioTDevice, @Param("unionId") String unionId);

  /** 新增设备 */
  int insertDevInstance(IoTDevice ioTDevice);

  /** 修改设备 */
  int updateDevInstance(IoTDevice ioTDevice);

  int bindApp(@Param("id") String[] id, @Param("appUniqueId") String appUniqueId);

  /** 设备绑定应用 */
  int apiBindApp(@Param("appUniqueId") String appUniqueId, @Param("iotId") String iotId);

  /** 设备应用解绑 */
  int apiUnBindApp(@Param("iotId") String iotId);

  /** 删除设备 */
  int deleteDevInstanceById(String id);

  /** 批量删除设备 */
  int deleteDevInstanceByIds(String[] ids);

  /** 根据分组id获取设备集合 */
  List<IoTDevice> selectDevListByIds(String groupId);

  int selectDevByAppUniqueId(String[] id);

  /** 查询所以设备型号 */
  List<IoTDeviceCompanyVO> selectAllBySupportChild(
      @Param("supportChild") Boolean supportChild, @Param("apps") String apps);

  IoTDevice getOneByDeviceId(@Param("query") IoTDeviceQuery query);

  IoTDevice selectIoTDevice(
      @Param("productKey") String productKey, @Param("deviceId") String deviceId);

  /** Lock a device identity while creating, moving, or deleting device-owned configuration. */
  IoTDevice selectForUpdateByProductKeyAndDeviceId(
      @Param("productKey") String productKey, @Param("deviceId") String deviceId);

  IoTDevice getOneByIotId(@Param("query") IoTDeviceQuery query);

  /** 根据产品key查型号配置 */
  IoTDeviceModelVO getModelByProductKey(String productKey);

  List<IoTDevice> selectNotBindDevInstanceList(
      @Param("groupId") String groupId,
      @Param("dev") IoTDevice ioTDevice,
      @Param("unionId") String unionId);

  /**
   * 查询满足离线标准的deviceId
   *
   * @param productKey 产品标识
   * @param difference 时间差额
   */
  List<String> selectOfflineThresholdIotIds(
      @Param("productKey") String productKey, @Param("difference") Long difference);

  /** 摄像头 */
  List<IoTDevice> selectCamera(
      @Param("productKeyList") List<String> productKeyList,
      @Param("discardValue") Integer discardValue,
      @Param("status") Integer status,
      @Param("start") Integer start,
      @Param("limit") Integer limit);

  /** 查询设备历史总数 */
  public int selectIoTDeviceHistory(IoTDeviceHistoryBO ioTDeviceHistoryBO);

  public int insertIoTDeviceHistory(IoTDeviceHistoryBO ioTDeviceHistoryBO);

  /** 修改设备配置字段 */
  public int updateDevConfiguration(Map<String, String> map);

  List<IoTDeviceVO> selectFenceDevice(IoTDeviceBO ioTDeviceBO);

  List<IoTDevice> hikDeviceList(IoTAPIQuery apiQuery);

  void clearDevBindInfo(IoTDevice instance);

  List<IoTDevice> queryMileSightList(@Param("bo") NetworkBO bo);

  List<GwIoTDeviceVo> getGatewayDeviceList(@Param("bo") IoTGwDeviceBO bo);

  List<IoTDevice> selectDevInstanceListWithTags(
      @Param("ioTDevice") IoTDeviceBO ioTDevice, @Param("unionId") String unionId);

  List<IoTDevice> listByIds(String[] ids);

  int checkIotIdUnique(@Param("iotId") String iotId);

  IoTDevice getOneByExtId(@Param("extId") String extId);

  /** 根据productKey统计设备数量 */
  int countByProductKey(String productKey);

  /** 批量刷新设备日志，更新online_time和state，初始化registry_time（如为空） */
  void batchFlushLog(@Param("iotIds") Set<String> iotIds);

  /** 统计所有设备总数 */
  long countAllDevices();

  /** 统计在线设备总数 */
  long countOnlineDevices();

  /** 按统一部门数据权限范围统计设备总数 */
  long countDevicesByScope(
      @Param("tenantId") String tenantId,
      @Param("scopeDeptIds") List<Long> scopeDeptIds,
      @Param("scopeUnionIds") List<String> scopeUnionIds,
      @Param("scopeDenied") boolean scopeDenied);

  /** 按统一部门数据权限范围统计在线设备总数 */
  long countOnlineDevicesByScope(
      @Param("tenantId") String tenantId,
      @Param("scopeDeptIds") List<Long> scopeDeptIds,
      @Param("scopeUnionIds") List<String> scopeUnionIds,
      @Param("scopeDenied") boolean scopeDenied);

  /** 统计已定位设备总数 */
  long countDashboardLocatedDevices(
      @Param("tenantId") String tenantId,
      @Param("scopeDeptIds") List<Long> scopeDeptIds,
      @Param("scopeUnionIds") List<String> scopeUnionIds,
      @Param("scopeDenied") boolean scopeDenied);

  /** 统计在线且已定位设备总数 */
  long countDashboardOnlineLocatedDevices(
      @Param("tenantId") String tenantId,
      @Param("scopeDeptIds") List<Long> scopeDeptIds,
      @Param("scopeUnionIds") List<String> scopeUnionIds,
      @Param("scopeDenied") boolean scopeDenied);

  /** 统计当前地图查询命中的设备数 */
  long countDashboardMapDevices(
      @Param("tenantId") String tenantId,
      @Param("scopeDeptIds") List<Long> scopeDeptIds,
      @Param("scopeUnionIds") List<String> scopeUnionIds,
      @Param("scopeDenied") boolean scopeDenied,
      @Param("keyword") String keyword,
      @Param("productKey") String productKey,
      @Param("state") Integer state,
      @Param("westLng") Double westLng,
      @Param("southLat") Double southLat,
      @Param("eastLng") Double eastLng,
      @Param("northLat") Double northLat);

  /** 查询当前地图设备列表 */
  List<DashboardMapDeviceVO> selectDashboardMapDevices(
      @Param("tenantId") String tenantId,
      @Param("scopeDeptIds") List<Long> scopeDeptIds,
      @Param("scopeUnionIds") List<String> scopeUnionIds,
      @Param("scopeDenied") boolean scopeDenied,
      @Param("keyword") String keyword,
      @Param("productKey") String productKey,
      @Param("state") Integer state,
      @Param("westLng") Double westLng,
      @Param("southLat") Double southLat,
      @Param("eastLng") Double eastLng,
      @Param("northLat") Double northLat,
      @Param("limit") Integer limit);

  /** 根据创建者统计设备总数 */
  long countDevicesByCreator(@Param("creatorId") String creatorId);

  /** 根据创建者统计在线设备总数 */
  long countOnlineDevicesByCreator(@Param("creatorId") String creatorId);

  List<IoTDevice> selectSubDeviceByGw(
      @Param("gwProductKey") String gwProductKey, @Param("gwDeviceId") String gwDeviceId);

  /** 根据网关设备查询子设备关系 */
  List<GwIoTDeviceVo> selectSubDeviceRelationByGw(
      @Param("gwProductKey") String gwProductKey, @Param("gwDeviceId") String gwDeviceId);

  /**
   * 查询视频设备列表（强制过滤 deviceNode=VIDEO_DEVICE）
   * 支持设备名称和设备序列号模糊搜索
   */
  List<IoTDevice> selectVideoDeviceList(
      @Param("ioTDevice") IoTDevice ioTDevice, @Param("unionId") String unionId);

  List<VideoCatalogDeviceRow> selectVideoCatalogDevices(
      @Param("ioTDevice") IoTDevice ioTDevice,
      @Param("keyword") String keyword,
      @Param("groupId") String groupId,
      @Param("targetIotId") String targetIotId);

  List<VideoCatalogChannelCount> selectVideoCatalogChannelCounts(
      @Param("devices") List<VideoCatalogDeviceRow> devices, @Param("tenantId") String tenantId);

  List<VideoPlatformChannel> selectVideoCatalogNativeChannels(
      @Param("instanceKey") String instanceKey, @Param("deviceId") String deviceId,
      @Param("tenantId") String tenantId, @Param("channelId") String channelId);

  List<VideoCatalogConfigurationChannel> selectVideoCatalogConfigurationChannels(
      @Param("channelsJson") String channelsJson);

  // ==================== 新增仪表盘统计方法 ====================

  /**
   * 获取设备类型分布（管理员）
   * 按设备节点类型统计设备数量
   *
   * @return 设备类型分布列表，包含 type 和 count 字段
   */
  List<Map<String, Object>> getDeviceTypeDistribution();

  /**
   * 获取设备类型分布（按创建者）
   * 按设备节点类型统计指定用户的设备数量
   *
   * @param creatorId 创建者ID
   * @return 设备类型分布列表，包含 type 和 count 字段
   */
  List<Map<String, Object>> getDeviceTypeDistributionByCreator(@Param("creatorId") String creatorId);

  /** 按统一部门数据权限范围获取设备类型分布 */
  List<Map<String, Object>> getDeviceTypeDistributionByScope(
      @Param("tenantId") String tenantId,
      @Param("scopeDeptIds") List<Long> scopeDeptIds,
      @Param("scopeUnionIds") List<String> scopeUnionIds,
      @Param("scopeDenied") boolean scopeDenied);

  /**
   * 获取产品接入方式分布（管理员）
   * 按第三方平台统计产品数量
   *
   * @return 产品接入方式分布列表，包含 platform 和 count 字段
   */
  List<Map<String, Object>> getProductPlatformDistribution();

  /**
   * 获取产品接入方式分布（按创建者）
   * 按第三方平台统计指定用户的产品数量
   *
   * @param creatorId 创建者ID
   * @return 产品接入方式分布列表，包含 platform 和 count 字段
   */
  List<Map<String, Object>> getProductPlatformDistributionByCreator(@Param("creatorId") String creatorId);

  /** 按统一部门数据权限范围获取产品接入方式分布 */
  List<Map<String, Object>> getProductPlatformDistributionByScope(
      @Param("tenantId") String tenantId,
      @Param("scopeDeptIds") List<Long> scopeDeptIds,
      @Param("scopeUnionIds") List<String> scopeUnionIds,
      @Param("scopeDenied") boolean scopeDenied);

  /**
   * 获取传输协议分布（管理员）
   * 按传输协议统计设备数量
   *
   * @return 传输协议分布列表，包含 protocol 和 count 字段
   */
  List<Map<String, Object>> getTransportProtocolDistribution();

  /**
   * 获取传输协议分布（按创建者）
   * 按传输协议统计指定用户的设备数量
   *
   * @param creatorId 创建者ID
   * @return 传输协议分布列表，包含 protocol 和 count 字段
   */
  List<Map<String, Object>> getTransportProtocolDistributionByCreator(@Param("creatorId") String creatorId);

  /** 按统一部门数据权限范围获取传输协议分布 */
  List<Map<String, Object>> getTransportProtocolDistributionByScope(
      @Param("tenantId") String tenantId,
      @Param("scopeDeptIds") List<Long> scopeDeptIds,
      @Param("scopeUnionIds") List<String> scopeUnionIds,
      @Param("scopeDenied") boolean scopeDenied);

  /**
   * 获取消息协议分布（管理员）
   * 按消息协议统计设备数量
   *
   * @return 消息协议分布列表，包含 protocol 和 count 字段
   */
  List<Map<String, Object>> getMessageProtocolDistribution();

  /**
   * 获取消息协议分布（按创建者）
   * 按消息协议统计指定用户的设备数量
   *
   * @param creatorId 创建者ID
   * @return 消息协议分布列表，包含 protocol 和 count 字段
   */
  List<Map<String, Object>> getMessageProtocolDistributionByCreator(@Param("creatorId") String creatorId);

  /** 按统一部门数据权限范围获取消息协议分布 */
  List<Map<String, Object>> getMessageProtocolDistributionByScope(
      @Param("tenantId") String tenantId,
      @Param("scopeDeptIds") List<Long> scopeDeptIds,
      @Param("scopeUnionIds") List<String> scopeUnionIds,
      @Param("scopeDenied") boolean scopeDenied);

  /**
   * 获取产品活跃度排行（管理员）
   * 按产品统计设备数量、在线率、今日消息数等
   *
   * @param limit 返回数量限制
   * @return 产品排行列表，包含 productName、productKey、deviceCount、onlineRate、todayMessages 等字段
   */
  List<Map<String, Object>> getProductRanking(@Param("limit") Integer limit);

  /**
   * 获取产品活跃度排行（按创建者）
   * 按产品统计指定用户的设备数量、在线率、今日消息数等
   *
   * @param creatorId 创建者ID
   * @param limit 返回数量限制
   * @return 产品排行列表，包含 productName、productKey、deviceCount、onlineRate、todayMessages 等字段
   */
  List<Map<String, Object>> getProductRankingByCreator(
      @Param("creatorId") String creatorId, @Param("limit") Integer limit);

  /** 按统一部门数据权限范围获取产品活跃度排行 */
  List<Map<String, Object>> getProductRankingByScope(
      @Param("limit") Integer limit,
      @Param("tenantId") String tenantId,
      @Param("scopeDeptIds") List<Long> scopeDeptIds,
      @Param("scopeUnionIds") List<String> scopeUnionIds,
      @Param("scopeDenied") boolean scopeDenied);

  IoTDeviceDetailVO queryIoTDetail(IoTAPIQuery apiQuery);
}
