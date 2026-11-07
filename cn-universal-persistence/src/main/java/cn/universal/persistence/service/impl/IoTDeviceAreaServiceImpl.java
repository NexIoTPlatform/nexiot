package cn.universal.persistence.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.universal.persistence.entity.IoTDevice;
import cn.universal.persistence.entity.SupportMapAreas;
import cn.universal.persistence.mapper.SupportMapAreasMapper;
import cn.universal.persistence.service.IoTDeviceAreaService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class IoTDeviceAreaServiceImpl implements IoTDeviceAreaService {

  @Resource private SupportMapAreasMapper supportMapAreasMapper;

  @Value("${iot.support-map-areas.lookup-enabled:false}")
  private boolean supportMapAreasLookupEnabled;

  @Override
  public void fillDeviceArea(IoTDevice ioTDevice, String longitude, String latitude) {
    if (ioTDevice == null || StrUtil.isBlank(longitude) || StrUtil.isBlank(latitude)) {
      return;
    }

    ioTDevice.setCoordinate(StrUtil.join(",", longitude, latitude));
    ioTDevice.setAreasId(null);

    if (!supportMapAreasLookupEnabled) {
      return;
    }

    try {
      SupportMapAreas supportMapAreas = supportMapAreasMapper.selectMapAreas(longitude, latitude);
      if (supportMapAreas == null) {
        log.info("查询区域id为空,lot={},lat={}", longitude, latitude);
        return;
      }
      ioTDevice.setAreasId(supportMapAreas.getId());
    } catch (Exception e) {
      log.warn("区域id维护失败，已按 fallback 返回空 areasId,lot={},lat={}", longitude, latitude, e);
    }
  }
}
