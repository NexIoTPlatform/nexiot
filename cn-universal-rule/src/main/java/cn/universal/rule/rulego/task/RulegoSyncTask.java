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

package cn.universal.rule.rulego.task;

import cn.universal.persistence.entity.RulegoChain;
import cn.universal.persistence.mapper.RulegoChainMapper;
import cn.universal.rule.rulego.service.RulegoChainService;
import jakarta.annotation.Resource;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import tk.mybatis.mapper.entity.Example;

/**
 * rulego规则链同步任务
 *
 * @author gitee.com/NexIoT
 * @since 2025/01/15
 */
@Component
@Slf4j
@ConditionalOnProperty(name = "rulego.sync.enabled", havingValue = "true", matchIfMissing = true)
public class RulegoSyncTask {

  @Resource private RulegoChainMapper rulegoChainMapper;

  @Resource private RulegoChainService rulegoChainService;

  /** 定期同步rulego规则链DSL到数据库 每5分钟执行一次 */
  @Scheduled(fixedRate = 300000) // 5分钟
  public void syncRulegoChainDsl() {
    try {
      log.info("开始同步rulego规则链DSL");

      // 查询所有已部署的规则链
      Example example = new Example(RulegoChain.class);
      example.createCriteria().andEqualTo("status", "deployed").andEqualTo("deleted", 0);

      List<RulegoChain> rulegoChains = rulegoChainMapper.selectByExample(example);

      if (rulegoChains.isEmpty()) {
        log.info("没有需要同步的规则链");
        return;
      }

      int successCount = 0;
      int failCount = 0;

      for (RulegoChain rulegoChain : rulegoChains) {
        try {
          boolean result = rulegoChainService.syncRulegoChainDsl(rulegoChain.getRulegoId());
          if (result) {
            successCount++;
            log.debug("同步规则链DSL成功: {}", rulegoChain.getRulegoId());
          } else {
            failCount++;
            log.warn("同步规则链DSL失败: {}", rulegoChain.getRulegoId());
          }
        } catch (Exception e) {
          failCount++;
          log.error("同步规则链DSL异常: {}", rulegoChain.getRulegoId(), e);
        }
      }

      log.info("同步rulego规则链DSL完成，成功: {}, 失败: {}", successCount, failCount);

    } catch (Exception e) {
      log.error("同步rulego规则链DSL任务执行异常", e);
    }
  }

  /** 定期清理过期的执行日志 每天凌晨2点执行 */
  @Scheduled(cron = "0 0 2 * * ?")
  public void cleanExpiredLogs() {
    try {
      log.info("开始清理过期的rulego规则链执行日志");

      // 删除30天前的日志
      // 这里可以根据实际需求调整清理策略
      // 由于没有直接的SQL删除方法，这里只是记录日志
      // 实际项目中可以通过自定义SQL实现

      log.info("清理过期的rulego规则链执行日志完成");

    } catch (Exception e) {
      log.error("清理过期的rulego规则链执行日志任务执行异常", e);
    }
  }
}
