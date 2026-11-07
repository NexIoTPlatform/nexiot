package cn.universal.admin.video.service;

import cn.universal.dm.video.VideoThingModelSynchronizer;
import cn.universal.persistence.entity.IoTProduct;
import cn.universal.persistence.mapper.IoTProductMapper;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/** 直连及 WVP 产品在收到下次告警前即可选择具体事件配置规则。 */
@Slf4j
@Component
@RequiredArgsConstructor
public class VideoThingModelBootstrap {
  private final IoTProductMapper products;
  private final VideoSdkThingModelTemplate template;
  private final VideoThingModelSynchronizer synchronizer;

  @EventListener(ApplicationReadyEvent.class)
  public void prepare() {
    for (String platform : Set.of("hik_sdk", "dahua_sdk", "tiandy_sdk", "uniview_sdk", "hik_isup", "wvp")) {
      try {
        IoTProduct probe = new IoTProduct(); probe.setThirdPlatform(platform);
        for (IoTProduct product : products.select(probe)) {
          if (Integer.valueOf(1).equals(product.getIsDeleted())) continue;
          try {
            synchronizer.ensureMetadata(product, template.merge(product.getMetadata(), platform, false));
          } catch (Exception e) {
            log.warn("[视频物模型] 初始化失败 productKey={}, reason={}", product.getProductKey(),
                e.getClass().getSimpleName());
          }
        }
      } catch (Exception e) {
        log.warn("[视频物模型] 厂商初始化稍后重试 platform={}, reason={}", platform, e.getClass().getSimpleName());
      }
    }
  }
}
