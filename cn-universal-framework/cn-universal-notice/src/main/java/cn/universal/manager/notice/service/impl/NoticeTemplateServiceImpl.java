package cn.universal.manager.notice.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.universal.manager.notice.dto.NoticeTemplateDTO;
import cn.universal.manager.notice.mapper.NoticeTemplateMapper;
import cn.universal.manager.notice.model.NoticeChannel;
import cn.universal.manager.notice.model.NoticeTemplate;
import cn.universal.manager.notice.service.NoticeChannelService;
import cn.universal.manager.notice.service.NoticeTemplateService;
import cn.universal.manager.notice.util.NoticeTemplateUtil;
import java.util.Date;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import tk.mybatis.mapper.entity.Example;

@Service
@Slf4j
public class NoticeTemplateServiceImpl implements NoticeTemplateService {

  @org.springframework.beans.factory.annotation.Autowired private cn.universal.manager.notice.service.NoticeAccess access;
  private final NoticeTemplateMapper templateMapper;
  private final NoticeChannelService noticeChannelService;
  private final cn.universal.manager.notice.service.NoticeService noticeService;

  public NoticeTemplateServiceImpl(
      NoticeTemplateMapper templateMapper,
      NoticeChannelService noticeChannelService,
      cn.universal.manager.notice.service.NoticeService noticeService) {
    this.templateMapper = templateMapper;
    this.noticeChannelService = noticeChannelService;
    this.noticeService = noticeService;
  }

  @Override
  public List<NoticeTemplate> list() {
    return search(null, null, null);
  }

  @Override
  @org.springframework.transaction.annotation.Transactional
  public void save(NoticeTemplate template) {
    var actor = access.requireUser();
    NoticeTemplate existing = template.getId() == null ? null : getById(template.getId());
    if (template.getId() != null && existing == null) throw new IllegalArgumentException("模板不存在");
    if (existing == null) {
      access.initialize(template);
      template.setCreator(actor.getUnionId());
      template.setCreateTime(new Date());
      if (template.getStatus() == null) template.setStatus("1");
    } else {
      access.inherit(template, existing);
      template.setCreator(existing.getCreator());
      template.setCreateTime(existing.getCreateTime());
      if (template.getStatus() == null) template.setStatus(existing.getStatus());
    }
    NoticeChannel channel = noticeChannelService.getById(template.getChannelId());
    if (channel == null || !java.util.Objects.equals(channel.getChannelType(), template.getChannelType()))
      throw new IllegalArgumentException("请选择匹配的通知渠道");
    access.sameTenant(template, channel);
    if (template.getName() == null || template.getName().isBlank()) throw new IllegalArgumentException("模板名称不能为空");
    if (!"0".equals(template.getStatus()) && !"1".equals(template.getStatus())) throw new IllegalArgumentException("模板状态无效");
    if (cn.universal.manager.notice.util.WechatTemplateRenderer.isWechat(template.getChannelType()))
      cn.universal.manager.notice.util.WechatTemplateRenderer.validate(template);
    template.setUpdateTime(new Date());
    if (template.getId() == null) templateMapper.insertSelective(template);
    else templateMapper.updateByPrimaryKey(template);
  }

  @Override
  @org.springframework.transaction.annotation.Transactional
  public void saveTemplate(NoticeTemplateDTO templateDTO, String currentUser) {
    Date now = new Date();
    NoticeTemplate template = new NoticeTemplate();

    // 设置基本属性
    template.setId(templateDTO.getId());
    template.setName(templateDTO.getName());
    template.setChannelType(templateDTO.getChannelType());
    template.setChannelId(templateDTO.getChannelId());
    template.setContent(templateDTO.getContent());
    template.setWechat(cn.universal.manager.notice.util.WechatTemplateRenderer.isWechat(templateDTO.getChannelType()) ? templateDTO.getWechat() : null);
    template.setReceivers(templateDTO.getReceivers());
    template.setStatus(templateDTO.getStatus());
    template.setRemark(templateDTO.getRemark());
    template.setUpdateTime(now);

    save(template);
  }

  @Override
  @org.springframework.transaction.annotation.Transactional
  public void delete(Long id) {
    deleteBatch(java.util.List.of(id));
  }

  @Override
  public NoticeTemplate getById(Long id) {
    if (id == null) return null;
    NoticeTemplate template = templateMapper.selectByPrimaryKey(id);
    if (template != null) access.check(template, template.getCreator());
    return template;
  }

  @Override
  public List<NoticeTemplate> search(String name, String channelType, String status) {
    Example example = new Example(NoticeTemplate.class);
    Example.Criteria c = example.createCriteria();
    if (StrUtil.isNotEmpty(name)) {
      c.andLike("name", "%" + name + "%");
    }
    if (StrUtil.isNotEmpty(channelType)) {
      c.andEqualTo("channelType", channelType);
    }
    if (StrUtil.isNotEmpty(status)) {
      c.andEqualTo("status", status);
    }
    example.orderBy("createTime").desc();
    access.scope(example);
    return templateMapper.selectByExample(example);
  }

  @Override
  @org.springframework.transaction.annotation.Transactional
  public void deleteBatch(List<Long> ids) {
    validateBatch(ids);
    Example example = new Example(NoticeTemplate.class);
    example.createCriteria().andIn("id", ids);
    access.scope(example);
    templateMapper.deleteByExample(example);
  }

  @Override
  @org.springframework.transaction.annotation.Transactional
  public void enableBatch(List<Long> ids) {
    validateBatch(ids);
    if (ids == null || ids.isEmpty()) {
      return;
    }
    Example example = new Example(NoticeTemplate.class);
    example.createCriteria().andIn("id", ids);
    NoticeTemplate update = new NoticeTemplate();
    update.setStatus("1"); // 启用
    update.setUpdateTime(new Date());
    access.scope(example);
    templateMapper.updateByExampleSelective(update, example);
  }

  @Override
  @org.springframework.transaction.annotation.Transactional
  public void disableBatch(List<Long> ids) {
    validateBatch(ids);
    if (ids == null || ids.isEmpty()) {
      return;
    }
    Example example = new Example(NoticeTemplate.class);
    example.createCriteria().andIn("id", ids);
    NoticeTemplate update = new NoticeTemplate();
    update.setStatus("0"); // 停用
    update.setUpdateTime(new Date());
    access.scope(example);
    templateMapper.updateByExampleSelective(update, example);
  }

  private void validateBatch(List<Long> ids) {
    access.requireUser();
    if (ids == null || ids.isEmpty() || ids.stream().anyMatch(java.util.Objects::isNull)) throw new IllegalArgumentException("模板 ID 不能为空");
    for (Long id : ids) if (getById(id) == null) throw new IllegalArgumentException("模板不存在");
  }

  @Override
  public void testTemplate(Long templateId, String receivers, Object params) {
    cn.universal.manager.notice.model.NoticeSendRequest request = new cn.universal.manager.notice.model.NoticeSendRequest();
    request.setTemplateId(templateId);
    request.setReceivers(receivers);
    request.setParams(NoticeTemplateUtil.parseJson(NoticeTemplateUtil.toJson(params)));
    cn.universal.manager.notice.service.channel.NoticeSendResult result = noticeService.test(request);
    if (!result.isSuccess()) throw new IllegalStateException(result.getErrorMessage());
  }
}
