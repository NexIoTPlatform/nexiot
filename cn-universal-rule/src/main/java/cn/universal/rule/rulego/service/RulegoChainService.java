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

package cn.universal.rule.rulego.service;

import cn.hutool.core.util.RandomUtil;
import cn.universal.common.exception.IoTException;
import cn.universal.persistence.entity.RulegoChain;
import cn.universal.persistence.entity.bo.RulegoChainBO;
import cn.universal.persistence.entity.vo.RulegoChainVO;
import cn.universal.persistence.mapper.RulegoChainMapper;
import cn.universal.rule.rulego.client.RulegoApiClient;
import cn.universal.rule.rulego.model.RulegoApiResponse;
import cn.universal.rule.rulego.model.RulegoChainInfo;
import cn.universal.rule.rulego.model.RulegoSaveRequest;
import cn.universal.rule.rulego.model.RulegoSuccessResponse;
import jakarta.annotation.Resource;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * rulego规则链服务
 *
 * @author gitee.com/NexIoT
 * @since 2025/01/15
 */
@Service
@Slf4j
public class RulegoChainService {

  @Resource private RulegoChainMapper rulegoChainMapper;

  @Resource private RulegoApiClient rulegoApiClient;

  /**
   * 查询规则链列表
   *
   * @param bo 查询条件
   * @return 规则链列表
   */
  public List<RulegoChainVO> queryRulegoChainList(RulegoChainBO bo) {
    return rulegoChainMapper.selectRulegoChainListByBo(bo);
  }

  /**
   * 根据ID查询规则链详情
   *
   * @param id 规则链ID
   * @param creatorId 创建人ID
   * @return 规则链详情
   */
  public RulegoChainVO queryRulegoChainById(Long id, String creatorId, String tenantId) {
    RulegoChain rulegoChain = rulegoChainMapper.selectByPrimaryKey(id);
    assertRulegoAccess(rulegoChain, creatorId, tenantId);
    return convertToVO(rulegoChain);
  }

  public RulegoChainVO queryRulegoChainById(Long id, String creatorId) {
    return queryRulegoChainById(id, creatorId, null);
  }

  /**
   * 根据rulegoId查询规则链详情
   *
   * @param rulegoId rulego规则链ID
   * @return 规则链详情
   */
  public RulegoChainVO queryRulegoChainByRulegoId(String rulegoId, String creatorId, String tenantId) {
    RulegoChain rulegoChain = rulegoChainMapper.selectByRulegoId(rulegoId);
    assertRulegoAccess(rulegoChain, creatorId, tenantId);
    return convertToVO(rulegoChain);
  }

  public RulegoChainVO queryRulegoChainByRulegoId(String rulegoId) {
    return queryRulegoChainByRulegoId(rulegoId, null, null);
  }

  /**
   * 保存规则链基础信息
   *
   * @param bo 规则链信息
   * @return 规则链ID
   */
  @Transactional(rollbackFor = Exception.class)
  public Long saveRulegoChain(RulegoChainBO bo) {
    // 生成rulego规则链ID
    String rulegoId = generateRulegoId(bo.getCreatorId());

    // 调用rulego API保存基础信息
    RulegoSaveRequest saveRequest = new RulegoSaveRequest();
    saveRequest.setId(rulegoId);
    saveRequest.setName(bo.getChainName());
    saveRequest.setRoot(true);
    saveRequest.setDescription(bo.getDescription());

    try {
      RulegoApiResponse<RulegoSuccessResponse> response =
          rulegoApiClient.saveChainInfo(saveRequest);
      if (response != null && !response.getSuccess()) {
        throw new IoTException("保存规则链基础信息失败: " + response.getMessage());
      }
    } catch (Exception e) {
      log.error("调用rulego API保存基础信息失败", e);
      throw new IoTException("保存规则链基础信息失败: " + e.getMessage());
    }

    // 保存到数据库
    RulegoChain rulegoChain = new RulegoChain();
    // 手动复制字段，避免String到Date的类型转换问题
    rulegoChain.setId(bo.getId());
    rulegoChain.setRulegoId(rulegoId);
    rulegoChain.setChainName(bo.getChainName());
    rulegoChain.setDescription(bo.getDescription());
    rulegoChain.setCreatorId(bo.getCreatorId());
    rulegoChain.setTenantId(resolveTenantId(bo.getCreatorId(), bo.getTenantId()));
    rulegoChain.setCreatorName(bo.getCreatorName());
    rulegoChain.setStatus("draft");
    rulegoChain.setDslContent(bo.getDslContent());
    rulegoChain.setCreateTime(new Date());
    rulegoChain.setUpdateTime(new Date());
    rulegoChain.setDeleted(0);

    rulegoChainMapper.insert(rulegoChain);

    log.info("保存规则链基础信息成功，rulegoId: {}, chainName: {}", rulegoId, bo.getChainName());
    return rulegoChain.getId();
  }

  /**
   * 更新规则链信息
   *
   * @param bo 规则链信息
   * @return 是否成功
   */
  @Transactional(rollbackFor = Exception.class)
  public boolean updateRulegoChain(RulegoChainBO bo) {
    RulegoChain existingChain = rulegoChainMapper.selectByPrimaryKey(bo.getId());
    assertRulegoAccess(existingChain, bo.getCreatorId(), bo.getTenantId());

    // 更新数据库
    RulegoChain updateChain = new RulegoChain();
    // 手动复制字段，避免String到Date的类型转换问题
    updateChain.setId(bo.getId());
    updateChain.setRulegoId(existingChain.getRulegoId());
    updateChain.setChainName(bo.getChainName());
    updateChain.setDescription(bo.getDescription());
    updateChain.setCreatorId(existingChain.getCreatorId());
    updateChain.setTenantId(
        resolveTenantId(
            existingChain.getCreatorId(),
            StringUtils.hasText(existingChain.getTenantId())
                ? existingChain.getTenantId()
                : bo.getTenantId()));
    updateChain.setCreatorName(
        StringUtils.hasText(bo.getCreatorName())
            ? bo.getCreatorName()
            : existingChain.getCreatorName());
    updateChain.setStatus(bo.getStatus());
    updateChain.setDslContent(bo.getDslContent());
    updateChain.setUpdateTime(new Date());

    int result = rulegoChainMapper.updateByPrimaryKeySelective(updateChain);
    return result > 0;
  }

  /**
   * 删除规则链
   *
   * @param id 规则链ID
   * @param creatorId 创建人ID
   * @return 是否成功
   */
  @Transactional(rollbackFor = Exception.class)
  public boolean deleteRulegoChain(Long id, String creatorId, String tenantId) {
    RulegoChain rulegoChain = rulegoChainMapper.selectByPrimaryKey(id);
    assertRulegoAccess(rulegoChain, creatorId, tenantId);

    // 调用rulego API删除规则链
    try {
      RulegoApiResponse<RulegoSuccessResponse> response =
          rulegoApiClient.deleteChain(rulegoChain.getRulegoId());
      if (response != null && !response.getSuccess()) {
        log.warn("调用rulego API删除规则链失败: {}", response.getMessage());
      }
    } catch (Exception e) {
      log.error("调用rulego API删除规则链失败", e);
    }

    // 软删除数据库记录
    RulegoChain updateChain = new RulegoChain();
    updateChain.setId(id);
    updateChain.setDeleted(1);
    updateChain.setUpdateTime(new Date());

    int result = rulegoChainMapper.updateByPrimaryKeySelective(updateChain);
    return result > 0;
  }

  /**
   * 部署规则链
   *
   * @param id 规则链ID
   * @param creatorId 创建人ID
   * @return 是否成功
   */
  @Transactional(rollbackFor = Exception.class)
  public boolean deployRulegoChain(Long id, String creatorId, String tenantId) {
    RulegoChain rulegoChain = rulegoChainMapper.selectByPrimaryKey(id);
    assertRulegoAccess(rulegoChain, creatorId, tenantId);

    // 调用rulego API部署规则链
    try {
      RulegoApiResponse<RulegoSuccessResponse> response =
          rulegoApiClient.deployChain(rulegoChain.getRulegoId());
      if (response != null && !response.getSuccess()) {
        throw new IoTException("部署规则链失败: " + response.getMessage());
      }
    } catch (Exception e) {
      log.error("调用rulego API部署规则链失败", e);
      throw new IoTException("部署规则链失败: " + e.getMessage());
    }

    // 更新状态
    RulegoChain updateChain = new RulegoChain();
    updateChain.setId(id);
    updateChain.setStatus("deployed");
    updateChain.setUpdateTime(new Date());

    int result = rulegoChainMapper.updateByPrimaryKeySelective(updateChain);
    return result > 0;
  }

  /**
   * 停止规则链
   *
   * @param id 规则链ID
   * @param creatorId 创建人ID
   * @return 是否成功
   */
  @Transactional(rollbackFor = Exception.class)
  public boolean stopRulegoChain(Long id, String creatorId, String tenantId) {
    RulegoChain rulegoChain = rulegoChainMapper.selectByPrimaryKey(id);
    assertRulegoAccess(rulegoChain, creatorId, tenantId);

    // 调用rulego API停止规则链
    try {
      RulegoApiResponse<RulegoSuccessResponse> response =
          rulegoApiClient.stopChain(rulegoChain.getRulegoId());
      if (response != null && !response.getSuccess()) {
        throw new IoTException("停止规则链失败: " + response.getMessage());
      }
    } catch (Exception e) {
      log.error("调用rulego API停止规则链失败", e);
      throw new IoTException("停止规则链失败: " + e.getMessage());
    }

    // 更新状态
    RulegoChain updateChain = new RulegoChain();
    updateChain.setId(id);
    updateChain.setStatus("stopped");
    updateChain.setUpdateTime(new Date());

    int result = rulegoChainMapper.updateByPrimaryKeySelective(updateChain);
    return result > 0;
  }

  /**
   * 同步规则链DSL
   *
   * @param rulegoId rulego规则链ID
   * @return 是否成功
   */
  @Transactional(rollbackFor = Exception.class)
  public boolean syncRulegoChainDsl(String rulegoId) {
    try {
      // 调用rulego API获取规则链详情
      RulegoApiResponse<RulegoChainInfo> response = rulegoApiClient.getChainDetail(rulegoId);
      if (response == null || !response.getSuccess() || response.getData() == null) {
        throw new IoTException("规则链详情为空: " + (response != null ? response.getMessage() : "响应为空"));
      }

      RulegoChainInfo chainInfo = response.getData();

      // 更新数据库中的DSL内容
      int result = rulegoChainMapper.updateDslContent(rulegoId, chainInfo.getDsl());
      return result > 0;
    } catch (Exception e) {
      log.error("同步规则链DSL失败，rulegoId: {}", rulegoId, e);
      throw new IoTException("同步规则链DSL失败: " + e.getMessage());
    }
  }

  /**
   * 获取设计器URL
   *
   * @param rulegoId rulego规则链ID
   * @return 设计器URL
   */
  public String getDesignerUrl(String rulegoId) {
    return rulegoApiClient.getDesignerUrl(rulegoId);
  }

  /**
   * 生成rulego规则链ID
   *
   * @return 规则链ID
   */
  private String generateRulegoId(String userId) {
    return userId + "_" + RandomUtil.randomString(6);
  }

  private void assertRulegoAccess(RulegoChain rulegoChain, String creatorId, String tenantId) {
    if (rulegoChain == null) {
      throw new IoTException("规则链不存在或无权限访问");
    }
    boolean creatorMatched =
        StringUtils.hasText(creatorId) && creatorId.equals(rulegoChain.getCreatorId());
    if (StringUtils.hasText(tenantId)) {
      String resourceTenantId =
          StringUtils.hasText(rulegoChain.getTenantId())
              ? rulegoChain.getTenantId()
              : rulegoChain.getCreatorId();
      if (tenantId.equals(resourceTenantId) || creatorMatched) {
        return;
      }
      throw new IoTException("规则链不存在或无权限访问");
    }
    if (StringUtils.hasText(creatorId) && !creatorMatched) {
      throw new IoTException("规则链不存在或无权限访问");
    }
  }

  private String resolveTenantId(String creatorId, String tenantId) {
    if (StringUtils.hasText(tenantId)) {
      return tenantId;
    }
    return creatorId;
  }

  /**
   * 转换为VO
   *
   * @param rulegoChain 实体对象
   * @return VO对象
   */
  private RulegoChainVO convertToVO(RulegoChain rulegoChain) {
    RulegoChainVO vo = new RulegoChainVO();

    // 手动复制字段，避免Date到String的类型转换问题
    vo.setId(rulegoChain.getId());
    vo.setRulegoId(rulegoChain.getRulegoId());
    vo.setChainName(rulegoChain.getChainName());
    vo.setDescription(rulegoChain.getDescription());
    vo.setCreatorId(rulegoChain.getCreatorId());
    vo.setCreatorName(rulegoChain.getCreatorName());
    vo.setStatus(rulegoChain.getStatus());
    vo.setDslContent(rulegoChain.getDslContent());

    // 格式化日期字段
    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    if (rulegoChain.getCreateTime() != null) {
      vo.setCreateTime(sdf.format(rulegoChain.getCreateTime()));
    }
    if (rulegoChain.getUpdateTime() != null) {
      vo.setUpdateTime(sdf.format(rulegoChain.getUpdateTime()));
    }
    if (rulegoChain.getLastSyncTime() != null) {
      vo.setLastSyncTime(sdf.format(rulegoChain.getLastSyncTime()));
    }

    // 设置状态描述
    switch (rulegoChain.getStatus()) {
      case "draft":
        vo.setStatusDesc("草稿");
        break;
      case "deployed":
        vo.setStatusDesc("已部署");
        break;
      case "stopped":
        vo.setStatusDesc("已停止");
        break;
      default:
        vo.setStatusDesc(rulegoChain.getStatus());
    }

    // 设置设计器URL
    vo.setDesignerUrl(getDesignerUrl(rulegoChain.getRulegoId()));

    return vo;
  }
}
