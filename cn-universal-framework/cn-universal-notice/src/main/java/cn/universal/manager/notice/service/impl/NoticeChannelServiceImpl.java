package cn.universal.manager.notice.service.impl;

import cn.universal.manager.notice.mapper.NoticeChannelMapper;
import cn.universal.manager.notice.model.NoticeChannel;
import cn.universal.manager.notice.service.NoticeChannelService;
import java.util.Date;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import tk.mybatis.mapper.entity.Example;

@Service
public class NoticeChannelServiceImpl implements NoticeChannelService {

  @Autowired private NoticeChannelMapper noticeChannelMapper;
  @Autowired private cn.universal.manager.notice.service.NoticeAccess access;
  @Autowired private cn.universal.manager.notice.mapper.NoticeTemplateMapper templates;
  @Autowired private cn.universal.manager.notice.mapper.WechatOpenIdBindingMapper bindings;

  @Override
  public List<NoticeChannel> list() {
    return search(null, null, null);
  }

  @Override
  public List<NoticeChannel> search(String name, String channelType, String status) {
    Example example = new Example(NoticeChannel.class);
    Example.Criteria criteria = example.createCriteria();

    if (name != null && !name.trim().isEmpty()) {
      criteria.andLike("name", "%" + name + "%");
    }

    if (channelType != null && !channelType.trim().isEmpty()) {
      criteria.andEqualTo("channelType", channelType);
    }

    if (status != null && !status.trim().isEmpty()) {
      criteria.andEqualTo("status", status);
    }

    example.orderBy("createTime").desc();
    access.scope(example);
    return noticeChannelMapper.selectByExample(example);
  }

  @Override
  @org.springframework.transaction.annotation.Transactional
  public void save(NoticeChannel channel) {
    var actor = access.requireUser();
    NoticeChannel existing = channel.getId() == null ? null : getById(channel.getId());
    if (channel.getId() != null && existing == null) throw new IllegalArgumentException("渠道不存在");
    if (existing == null) {
      access.initialize(channel);
      channel.setCreator(actor.getUnionId());
      if (channel.getStatus() == null) channel.setStatus("0");
    } else {
      access.inherit(channel, existing);
      channel.setCreator(existing.getCreator());
      channel.setCreateTime(existing.getCreateTime());
      if (!java.util.Objects.equals(existing.getChannelType(), channel.getChannelType())) {
        requireUnused(java.util.List.of(channel.getId()));
      }
      if ("wechat".equals(existing.getChannelType())) {
        String previous = cn.hutool.json.JSONUtil.parseObj(existing.getConfig()).getStr("appId");
        String next = cn.hutool.json.JSONUtil.parseObj(channel.getConfig()).getStr("appId");
        if (!java.util.Objects.equals(previous, next))
          throw new IllegalArgumentException("已有微信渠道不可更换 AppID，请新建渠道");
      }
    }
    if (!"0".equals(channel.getStatus()) && !"1".equals(channel.getStatus())) throw new IllegalArgumentException("渠道状态无效");
    if (channel.getName() == null || channel.getName().isBlank() || channel.getChannelType() == null || channel.getChannelType().isBlank())
      throw new IllegalArgumentException("渠道名称和类型不能为空");
    Date now = new Date();
    if (channel.getId() == null) {
      // 新增
      channel.setCreateTime(now);
      channel.setUpdateTime(now);
      // 让 PG/达梦使用数据库默认 identity/auto_increment 生成主键，避免显式插入 null。
      noticeChannelMapper.insertSelective(channel);
    } else {
      // 更新，执行全量更新以支持将字段显式清空为null
      channel.setUpdateTime(now);
      noticeChannelMapper.updateByPrimaryKey(channel);
    }
  }

  @Override
  @org.springframework.transaction.annotation.Transactional
  public void delete(Long id) {
    deleteBatch(java.util.List.of(id));
  }

  @Override
  @org.springframework.transaction.annotation.Transactional
  public void deleteBatch(List<Long> ids) {
    access.requireUser();
    if (ids == null || ids.isEmpty() || ids.stream().anyMatch(java.util.Objects::isNull)) throw new IllegalArgumentException("渠道 ID 不能为空");
    for (Long id : ids) if (getById(id) == null) throw new IllegalArgumentException("渠道不存在");
    requireUnused(ids);
    Example example = new Example(NoticeChannel.class);
    example.createCriteria().andIn("id", ids);
    access.scope(example);
    noticeChannelMapper.deleteByExample(example);
  }

  @Override
  public NoticeChannel getById(Long id) {
    if (id == null) return null;
    NoticeChannel channel = noticeChannelMapper.selectByPrimaryKey(id);
    if (channel != null) access.check(channel, channel.getCreator());
    return channel;
  }
  private void requireUnused(List<Long> ids) {
    Example example = new Example(cn.universal.manager.notice.model.NoticeTemplate.class);
    example.createCriteria().andIn("channelId", ids);
    if (cn.universal.manager.notice.service.NoticeAccess.unscoped(() -> templates.selectCountByExample(example)) > 0)
      throw new IllegalArgumentException("渠道已被通知模板引用，请先解除引用");
    Example binding = new Example(cn.universal.manager.notice.model.WechatOpenIdBinding.class);
    binding.createCriteria().andIn("channelId", ids);
    if (cn.universal.manager.notice.service.NoticeAccess.unscoped(() -> bindings.selectCountByExample(binding)) > 0)
      throw new IllegalArgumentException("渠道已有微信用户绑定，不能删除或更改类型");
  }
}
