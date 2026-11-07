package cn.universal.admin.video.downlink;

import org.springframework.stereotype.Service;

/** ISUP 与直连视频沿用同一物模型函数及租户下行拦截链。 */
@Service("hikIsupVideoDownService")
public class HikIsupVideoDownService extends AbstractSdkVideoDownService {
  @Override public String name() { return "Hikvision ISUP"; }
  @Override public String code() { return "hik_isup"; }
}
