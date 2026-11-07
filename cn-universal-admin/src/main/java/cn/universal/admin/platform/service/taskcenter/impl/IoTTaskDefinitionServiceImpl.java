package cn.universal.admin.platform.service.taskcenter.impl;

import cn.hutool.core.util.StrUtil;
import cn.universal.admin.platform.service.taskcenter.IIoTTaskDefinitionService;
import cn.universal.admin.platform.service.taskcenter.support.IoTTaskCenterConstants;
import cn.universal.persistence.entity.IoTTaskDefinition;
import cn.universal.persistence.entity.IoTUser;
import cn.universal.persistence.mapper.IoTTaskDefinitionMapper;
import cn.universal.security.service.IoTUserService;
import cn.universal.quartz.util.CronUtils;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class IoTTaskDefinitionServiceImpl implements IIoTTaskDefinitionService {

  private final IoTTaskDefinitionMapper taskDefinitionMapper;
  private final IoTUserService ioTUserService;

  @Override
  public List<IoTTaskDefinition> selectTaskDefinitionList(
      IoTTaskDefinition query, String unionId, boolean admin) {
    if (query == null) {
      query = new IoTTaskDefinition();
    }
    String scopedCreatorId = unionId;
    if (!admin) {
      scopedCreatorId = resolveTenantOwnerUnionId(unionId);
      query.setCreatorId(scopedCreatorId);
      query.setTenantId(resolveTenantId(unionId));
    }
    List<IoTTaskDefinition> list =
        taskDefinitionMapper.selectTaskDefinitionList(query, scopedCreatorId, admin);
    if (list == null || list.isEmpty()) {
      return new ArrayList<>();
    }
    list.forEach(this::fillNextExecuteTime);
    return list;
  }

  @Override
  public IoTTaskDefinition selectTaskDefinitionById(Long id, String unionId, boolean admin) {
    IoTTaskDefinition definition = taskDefinitionMapper.selectTaskDefinitionById(id);
    if (definition == null) {
      return null;
    }
    if (!admin && !canAccessTenantResource(definition.getTenantId(), definition.getCreatorId(), unionId)) {
      return null;
    }
    fillNextExecuteTime(definition);
    return definition;
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public Long createTaskDefinition(IoTTaskDefinition definition) {
    Date now = new Date();
    if (StrUtil.isBlank(definition.getTenantId())) {
      definition.setTenantId(resolveTenantId(definition.getCreatorId()));
    }
    definition.setCreateTime(now);
    definition.setUpdateTime(now);
    taskDefinitionMapper.insert(definition);
    return definition.getId();
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public void updateTaskDefinition(IoTTaskDefinition definition) {
    definition.setUpdateTime(new Date());
    taskDefinitionMapper.updateByPrimaryKeySelective(definition);
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public int deleteTaskDefinitionByIds(Long[] ids, String unionId, boolean admin) {
    if (ids == null || ids.length == 0) {
      return 0;
    }
    int rows = 0;
    for (Long id : ids) {
      IoTTaskDefinition definition = selectTaskDefinitionById(id, unionId, admin);
      if (definition == null) {
        continue;
      }
      rows += taskDefinitionMapper.deleteByPrimaryKey(id);
    }
    return rows;
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public void updateLastExecution(Long id, Long taskId, Date executeTime) {
    IoTTaskDefinition update =
        IoTTaskDefinition.builder()
            .id(id)
            .lastTaskId(taskId)
            .lastExecuteTime(executeTime)
            .updateTime(new Date())
            .build();
    taskDefinitionMapper.updateByPrimaryKeySelective(update);
  }

  private void fillNextExecuteTime(IoTTaskDefinition definition) {
    if (definition == null) {
      return;
    }
    if (!IoTTaskCenterConstants.DefinitionScheduleType.CRON.equals(definition.getScheduleType())) {
      definition.setNextExecuteTime(null);
      return;
    }
    if (!IoTTaskCenterConstants.DefinitionScheduleStatus.ENABLED.equals(
        definition.getScheduleStatus())) {
      definition.setNextExecuteTime(null);
      return;
    }
    if (definition.getCronExpression() == null || definition.getCronExpression().isBlank()) {
      definition.setNextExecuteTime(null);
      return;
    }
    definition.setNextExecuteTime(CronUtils.getNextExecution(definition.getCronExpression()));
  }

  private boolean canAccessTenantResource(String tenantId, String creatorId, String unionId) {
    if (StrUtil.isBlank(unionId)) {
      return false;
    }
    String currentTenantId = resolveTenantId(unionId);
    String resourceTenantId = StrUtil.blankToDefault(tenantId, resolveTenantId(creatorId));
    if (StrUtil.isBlank(resourceTenantId)) {
      return StrUtil.equals(unionId, creatorId);
    }
    return StrUtil.equals(currentTenantId, resourceTenantId)
        || StrUtil.equals(unionId, creatorId);
  }

  private String resolveTenantId(String creatorId) {
    if (StrUtil.isBlank(creatorId)) {
      return null;
    }
    IoTUser creator = ioTUserService.selectUserByUnionId(creatorId);
    if (creator == null) {
      return null;
    }
    return StrUtil.blankToDefault(creator.getTenantId(), creator.getUnionId());
  }

  private String resolveTenantOwnerUnionId(String unionId) {
    if (StrUtil.isBlank(unionId)) {
      return null;
    }
    IoTUser tenantOwner = ioTUserService.selectUserByUnionId(unionId);
    if (tenantOwner == null) {
      return unionId;
    }
    if (!tenantOwner.isAdmin() && !StrUtil.equalsIgnoreCase(tenantOwner.getTenantRole(), "ADMIN")) {
      String tenantId = resolveTenantId(tenantOwner.getUnionId());
      if (StrUtil.isNotBlank(tenantId)) {
        List<IoTUser> adminUsers =
            ioTUserService.selectUserList(
                IoTUser.builder().tenantId(tenantId).tenantRole("ADMIN").build());
        if (adminUsers != null && !adminUsers.isEmpty()) {
          tenantOwner = adminUsers.get(0);
        }
      }
    }
    return tenantOwner.getUnionId();
  }
}
