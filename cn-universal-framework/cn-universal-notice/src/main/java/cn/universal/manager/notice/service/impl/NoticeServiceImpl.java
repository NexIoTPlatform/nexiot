package cn.universal.manager.notice.service.impl;

import cn.hutool.json.JSONUtil;
import cn.universal.manager.notice.mapper.NoticeTemplateMapper;
import cn.universal.manager.notice.model.*;
import cn.universal.manager.notice.service.*;
import cn.universal.manager.notice.service.channel.*;
import cn.universal.manager.notice.util.NoticeTemplateUtil;
import cn.universal.manager.notice.util.WechatTemplateRenderer;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class NoticeServiceImpl implements NoticeService {
  @org.springframework.beans.factory.annotation.Autowired private NoticeAccess access;
  private final NoticeTemplateMapper templates;
  private final NoticeRecordService records;
  private final NoticeChannelService channels;
  private final List<NoticeSendChannel> senders;

  public NoticeServiceImpl(NoticeTemplateMapper templates, NoticeRecordService records,
      NoticeChannelService channels, List<NoticeSendChannel> senders) {
    this.templates = templates;
    this.records = records;
    this.channels = channels;
    this.senders = senders;
  }

  private NoticeTemplate template(NoticeSendRequest request) {
    if (request == null || request.getTemplateId() == null) throw new IllegalArgumentException("模板 ID 不能为空");
    NoticeTemplate template = templates.selectByPrimaryKey(request.getTemplateId());
    if (template == null) throw new IllegalArgumentException("通知模板不存在");
    access.check(template, template.getCreator());
    if (access.currentUser() == null && StringUtils.hasText(request.getCreator()))
      access.checkCreator(request.getCreator(), template, template.getCreator());
    return template;
  }

  private NoticeChannel channel(NoticeTemplate template) {
    NoticeChannel channel = channels.getById(template.getChannelId());
    if (channel == null) throw new IllegalArgumentException("渠道配置不存在");
    access.sameTenant(template, channel);
    if (!Objects.equals(channel.getChannelType(), template.getChannelType()))
      throw new IllegalArgumentException("模板与渠道类型不匹配");
    if ("0".equals(template.getStatus())) throw new IllegalArgumentException("通知模板已停用");
    if ("1".equals(channel.getStatus())) throw new IllegalArgumentException("通知渠道已停用");
    return channel;
  }

  private String render(NoticeTemplate template, NoticeChannel channel, Map<String, Object> params) {
    if (WechatTemplateRenderer.isWechat(template.getChannelType()))
      return WechatTemplateRenderer.render(template, channel, params).toString();
    if (!StringUtils.hasText(template.getContent())) throw new IllegalArgumentException("模板内容不能为空");
    return NoticeTemplateUtil.replaceNestedParams(template.getContent(), params);
  }

  @Override public Map<String, Object> preview(NoticeSendRequest request) {
    NoticeTemplate template = template(request);
    NoticeChannel channel = channel(template);
    String content = render(template, channel, request.getParams());
    return WechatTemplateRenderer.isWechat(template.getChannelType())
        ? Map.of("channelType", template.getChannelType(), "payload", JSONUtil.parseObj(content))
        : Map.of("channelType", template.getChannelType(), "content", content);
  }

  private String receivers(NoticeTemplate template, NoticeSendRequest request, boolean test) {
    Set<String> values = new LinkedHashSet<>();
    for (String source : new String[] {test ? "" : template.getReceivers(), request.getReceivers()}) {
      String resolved = NoticeTemplateUtil.replaceParams(NoticeTemplateUtil.replaceNestedParams(source, request.getParams()), request.getParams());
      if (StringUtils.hasText(resolved)) for (String value : resolved.split(","))
        if (StringUtils.hasText(value)) values.add(value.trim());
    }
    return String.join(",", values);
  }

  @Override public void send(NoticeSendRequest request) {
    NoticeSendResult result = sendR(request);
    if (!result.isSuccess()) throw new IllegalStateException(result.getErrorMessage());
  }
  @Override public NoticeSendResult sendR(NoticeSendRequest request) { return execute(request, false); }
  @Override public NoticeSendResult test(NoticeSendRequest request) { return execute(request, true); }

  private NoticeSendResult execute(NoticeSendRequest request, boolean test) {
    if (access.currentUser() == null && request != null && StringUtils.hasText(request.getCreator())) {
      return access.runAs(request.getCreator(), () -> execute(request, test));
    }
    NoticeTemplate template;
    try { template = template(request); }
    catch (Exception e) { return NoticeSendResult.builder().success(false).errorMessage(e.getMessage()).build(); }
    NoticeSendRecord record = new NoticeSendRecord();
    record.setTemplateId(template.getId());
    record.setConfigId(template.getChannelId());
    record.setParams(JSONUtil.toJsonStr(request.getParams()));
    var actor = access.currentUser();
    record.setCreator(actor == null ? (StringUtils.hasText(request.getCreator()) ? request.getCreator() : template.getCreator()) : actor.getUnionId());
    access.inherit(record, template);
    record.setChannelType(template.getChannelType());
    record.setSendTime(new Date());
    try {
      NoticeChannel channel = channel(template);
      if (actor == null && StringUtils.hasText(request.getCreator()))
        access.checkCreator(request.getCreator(), channel, channel.getCreator());
      String recipients = receivers(template, request, test);
      record.setReceivers(recipients);
      if (test && !StringUtils.hasText(request.getReceivers())) throw new IllegalArgumentException("测试接收人不能为空");
      String content = render(template, channel, request.getParams());
      NoticeSendChannel sender = senders.stream().filter(c -> c.support(channel.getChannelType()))
          .findFirst().orElseThrow(() -> new IllegalArgumentException("不支持的通知类型: " + channel.getChannelType()));
      NoticeSendResult result = sender.send(content, recipients, channel, request.getParams());
      record.setStatus(result.isSuccess() ? "SUCCESS" : "FAIL");
      record.setResult(result.isSuccess()
          ? (WechatTemplateRenderer.isWechat(template.getChannelType()) ? "微信已受理，最终送达以微信回执为准; msgids=" + result.getMessageIds() : "OK")
          : result.getErrorMessage());
      return result;
    } catch (Exception e) {
      record.setStatus("FAIL");
      record.setResult(e.getMessage());
      return NoticeSendResult.builder().success(false).errorMessage(e.getMessage()).build();
    } finally { records.save(record); }
  }
}
