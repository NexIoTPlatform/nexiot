package cn.universal.ossm.util;

import cn.universal.persistence.entity.IoTDevice;
import cn.universal.persistence.entity.IoTProduct;

/** Vendor implementations can decode an archived cloud picture without replaying its event. */
public interface VideoCloudImageDecoder {
  boolean supports(String platform);
  byte[] decodeArchived(String sourceUrl, byte[] bytes, IoTDevice device, IoTProduct product);
}
