package cn.universal.manager.notice.service.channel;

import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/** 通知渠道工厂类 用于管理和获取各种通知渠道 */
@Component
public class NoticeChannelFactory {

  private final List<NoticeSendChannel> channels;

  @Autowired
  public NoticeChannelFactory(List<NoticeSendChannel> channels) {
    this.channels = channels;
  }

  /**
   * 根据渠道类型获取对应的通知渠道
   *
   * @param type 渠道类型
   * @return 通知渠道实例
   */
  public Optional<NoticeSendChannel> getChannel(String type) {
    return channels.stream().filter(channel -> channel.support(type)).findFirst();
  }

  /**
   * 检查是否支持指定的渠道类型
   *
   * @param type 渠道类型
   * @return 是否支持
   */
  public boolean supports(String type) {
    return channels.stream().anyMatch(channel -> channel.support(type));
  }

  /**
   * 获取所有支持的渠道类型
   *
   * @return 渠道类型列表
   */
  public List<String> getSupportedTypes() {
    return List.of(
        "dingTalk", // 钉钉
        "email", // 邮箱
        "feishu", // 飞书
        "sms_ali", // 阿里云短信
        "sms_tencent", // 腾讯云短信
        "audio_ali", // 阿里云语音
        "audio_tencent", // 腾讯云语音
        "wechat" // 微信服务号
        );
  }
}
