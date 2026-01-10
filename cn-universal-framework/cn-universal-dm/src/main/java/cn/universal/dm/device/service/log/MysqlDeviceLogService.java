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

package cn.universal.dm.device.service.log;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.CharsetUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.universal.common.constant.IoTConstant.MessageType;
import cn.universal.core.message.UPRequest;
import cn.universal.core.metadata.AbstractPropertyMetadata;
import cn.universal.dm.device.entity.IoTDevicePropertiesBO;
import cn.universal.dm.device.service.impl.IoTProductDeviceService;
import cn.universal.persistence.base.BaseUPRequest;
import cn.universal.persistence.dto.IoTDeviceDTO;
import cn.universal.persistence.dto.LogStorePolicyDTO;
import cn.universal.persistence.entity.IoTDeviceEvents;
import cn.universal.persistence.entity.IoTDeviceLog;
import cn.universal.persistence.entity.IoTDeviceLogMetadata.IoTDeviceLogMetadataBuilder;
import cn.universal.persistence.entity.IoTProduct;
import cn.universal.persistence.entity.vo.IoTDeviceLogMetadataVO;
import cn.universal.persistence.entity.vo.IoTDeviceLogVO;
import cn.universal.persistence.mapper.IoTDeviceLogMapper;
import cn.universal.persistence.mapper.IoTDeviceLogMetadataMapper;
import cn.universal.persistence.mapper.IoTDeviceLogMetadataShardMapper;
import cn.universal.persistence.mapper.IoTDeviceLogShardMapper;
import cn.universal.persistence.query.LogQuery;
import cn.universal.persistence.query.PageBean;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import jakarta.annotation.Resource;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * 设备日志
 *
 * @author gitee.com/NexIoT
 * @version 1.0
 * @since 2025/9/22 16:10
 */
@Component
@Slf4j
public class MysqlDeviceLogService extends AbstractIoTDeviceLogService {
  private String storePolicy = "mysql";
  @Resource private IoTDeviceLogMapper ioTDeviceLogMapper;
  @Resource private IoTDeviceLogShardMapper ioTDeviceLogShardMapper;
  @Resource private IoTDeviceLogMetadataMapper ioTDeviceLogMetadataMapper;
  @Resource private IoTDeviceLogMetadataShardMapper ioTDeviceLogMetadataShardMapper;
  @Resource private IoTProductDeviceService iotProductDeviceService;

  /** 日志分表是否开启 */
  @Value("${shard.log.enable:true}")
  private Boolean enable;

  /** 日志meta分表是否开启 */
  @Value("${shard.logMeta.enable:true}")
  private Boolean metaEnable;

  @Override
  public String getPolicy() {
    return storePolicy;
  }

  @Override
  @Async
  public void saveDeviceLog(
      BaseUPRequest upRequest, IoTDeviceDTO ioTDeviceDTO, IoTProduct ioTProduct) {
    /** 产品数据存储策略，不为空则保存日志 */
    if (StrUtil.isNotBlank(ioTProduct.getStorePolicy())) {
      try {
        IoTDeviceLog log = build(upRequest, ioTDeviceDTO);
        if (enable) {
          ioTDeviceLogShardMapper.insertSelective(log);
        }
      } catch (Exception e) {
        log.error("保存设备日志报错={}", e);
      }
      String storePolicyConfiguration = ioTProduct.getStorePolicyConfiguration();
      try {
        if (StrUtil.isNotBlank(storePolicyConfiguration)
            || MessageType.EVENT.equals(upRequest.getMessageType())) {
          LogStorePolicyDTO productLogStorePolicy =
              iotProductDeviceService.getProductLogStorePolicy(ioTProduct.getProductKey());
          saveLogStorePolicy(productLogStorePolicy, upRequest, ioTProduct);
        }
      } catch (Exception e) {
        log.error("保存设备属性扩展日志报错={}", e);
      }
    }
  }

  @Override
  public void saveDeviceLog(
      IoTDeviceLog ioTDeviceLog, IoTDeviceDTO ioTDeviceDTO, IoTProduct ioTProduct) {
    /** 产品数据存储策略，不为空则保存日志 */
    if (StrUtil.isNotBlank(ioTProduct.getStorePolicy())) {
      try {
        if (enable) {
          ioTDeviceLogShardMapper.insertSelective(ioTDeviceLog);
        }
      } catch (Exception e) {
        log.error("保存设备日志报错={}", e);
      }
    }
  }

  private void saveLogStorePolicy(
      LogStorePolicyDTO logStorePolicyDTO, UPRequest up, IoTProduct ioTProduct) {
    if (MessageType.PROPERTIES.equals(up.getMessageType())
        && up.getProperties() != null
        && CollectionUtil.isNotEmpty(logStorePolicyDTO.getProperties())) {
      up.getProperties()
          .forEach(
              (key, value) -> {
                if (logStorePolicyDTO.getProperties().containsKey(key)) {
                  AbstractPropertyMetadata propertyOrNull =
                      getDeviceMetadata(ioTProduct.getMetadata()).getPropertyOrNull(key);
                  IoTDevicePropertiesBO ioTDevicePropertiesBO = new IoTDevicePropertiesBO();
                  ioTDevicePropertiesBO.withValue(propertyOrNull, value);
                  // TODO event
                  IoTDeviceLogMetadataBuilder IoTDeviceLogMetadataBuilder = builder(up);
                  IoTDeviceLogMetadataBuilder.property(key);
                  IoTDeviceLogMetadataBuilder.content(
                      StrUtil.str(value, CharsetUtil.charset("UTF-8")));
                  IoTDeviceLogMetadataBuilder.ext1(ioTDevicePropertiesBO.getPropertyName());
                  IoTDeviceLogMetadataBuilder.ext2(ioTDevicePropertiesBO.getFormatValue());
                  IoTDeviceLogMetadataBuilder.ext3(ioTDevicePropertiesBO.getSymbol());
                  if (metaEnable) {
                    ioTDeviceLogMetadataShardMapper.insertUseGeneratedKeys(
                        IoTDeviceLogMetadataBuilder.build());
                  }
                }
              });
    }
    if (MessageType.EVENT.equals(up.getMessageType())) {
      int maxStorage = 10;
      if (CollectionUtil.isNotEmpty(logStorePolicyDTO.getEvent())
          && logStorePolicyDTO.getEvent().containsKey(up.getEvent())) {
        maxStorage = logStorePolicyDTO.getEvent().get(up.getEvent()).getMaxStorage();
      }
      IoTDeviceLogMetadataBuilder IoTDeviceLogMetadataBuilder = builder(up);
      IoTDeviceLogMetadataBuilder.event(up.getEvent());
      IoTDeviceLogMetadataBuilder.content(up.getEventName());
      if (metaEnable) {
        ioTDeviceLogMetadataShardMapper.insertUseGeneratedKeys(IoTDeviceLogMetadataBuilder.build());
      }
    }
  }

  @Override
  public PageBean<IoTDeviceLogVO> pageList(LogQuery bo) {
    bo.setProductKey(null);
    PageHelper.startPage(bo.getPageNum(), bo.getPageSize());
    if (ObjectUtil.isNull(bo.getId())) {
      List<IoTDeviceLogVO> ioTDeviceLogVOS = ioTDeviceLogShardMapper.queryLogPageV2List(bo);
      return new PageBean(
          ioTDeviceLogVOS,
          new PageInfo(ioTDeviceLogVOS).getTotal(),
          bo.getPageSize(),
          bo.getPageNum());
    } else {
      List<IoTDeviceLogVO> ioTDeviceLogVOS = ioTDeviceLogShardMapper.queryLogPageV2ByIdList(bo);
      return new PageBean(
          ioTDeviceLogVOS,
          new PageInfo(ioTDeviceLogVOS).getTotal(),
          bo.getPageSize(),
          bo.getPageNum());
    }
  }

  @Override
  public IoTDeviceLogVO queryById(LogQuery logQuery) {
    IoTDeviceLogVO ioTDeviceLogVO = ioTDeviceLogMapper.queryLogById(logQuery);
    return ioTDeviceLogVO;
  }

  @Override
  public PageBean<IoTDeviceEvents> queryEventTotal(String productKey, String iotId) {
    List<IoTDeviceEvents> list = selectDevEvents(productKey);
    for (IoTDeviceEvents devEvent : list) {
      List<String> events;
      if (metaEnable) {
        events =
            ioTDeviceLogMetadataShardMapper.queryEventTotalByEventAndId(devEvent.getId(), iotId);
      } else {
        events = ioTDeviceLogMetadataMapper.queryEventTotalByEventAndId(devEvent.getId(), iotId);
      }

      int size = events.size();
      if (size > 0) {
        devEvent.setTime(events.get(0));
        devEvent.setQty(size >= 100 ? "99+" : String.valueOf(size));
      }
    }
    return new PageBean(list, new PageInfo(list).getTotal(), 1, 100);
  }

  @Override
  public PageBean<IoTDeviceLogMetadataVO> queryLogMeta(LogQuery logQuery) {
    PageHelper.startPage(logQuery.getPageNum(), logQuery.getPageSize());
    List<IoTDeviceLogMetadataVO> list;
    if (metaEnable) {
      list = ioTDeviceLogMetadataShardMapper.selectLogMetaList(logQuery);
    } else {
      list = ioTDeviceLogMetadataMapper.selectLogMetaList(logQuery);
    }

    return new PageBean(
        list, new PageInfo(list).getTotal(), logQuery.getPageSize(), logQuery.getPageNum());
  }

  @Override
  public JSONObject configMetadata() {
    return null;
  }
}
