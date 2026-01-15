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

package cn.universal.websocket.protocol.handle;

import java.util.Map;
import java.util.Objects;

import org.springframework.stereotype.Component;

import cn.hutool.core.util.StrUtil;
import cn.universal.common.constant.IoTConstant.DeviceStatus;
import cn.universal.common.constant.IoTConstant.ERROR_CODE;
import cn.universal.common.domain.R;
import cn.universal.dm.device.service.impl.IoTDeviceService;
import cn.universal.persistence.base.IoTDeviceLifeCycle;
import cn.universal.persistence.base.IoTDownAdapter;
import cn.universal.persistence.entity.IoTDevice;
import cn.universal.persistence.entity.SupportMapAreas;
import cn.universal.persistence.mapper.IoTDeviceMapper;
import cn.universal.persistence.mapper.SupportMapAreasMapper;
import cn.universal.websocket.protocol.entity.WebSocketDownRequest;
import cn.universal.websocket.protocol.processor.down.WebSocketDownProcessorChain;
import cn.universal.websocket.protocol.service.WebSocketSessionManager;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;

/**
 * WebSocket 下行消息处理类
 * 
 * 负责完整的设备管理业务逻辑（增删改查），参照 HTTP 协议 Handle 设计
 *
 * @author gitee.com/NexIoT
 * @version 2.0
 * @since 2026/1/13
 */
@Slf4j
@Component
public class WebSocketDownHandle extends IoTDownAdapter<WebSocketDownRequest> {

    @Resource
    private WebSocketSessionManager sessionManager;

    @Resource
    private WebSocketDownProcessorChain processorChain;

    @Resource
    private IoTDeviceService iotDeviceService;

    @Resource
    private IoTDeviceMapper ioTDeviceMapper;

    @Resource
    private SupportMapAreasMapper supportMapAreasMapper;

    @Resource(name = "ioTDeviceActionAfterService")
    private IoTDeviceLifeCycle ioTDeviceLifeCycle;

    /**
     * 处理下行消息 - 完整的设备管理逻辑实现
     *
     * @param downRequest 下行请求
     * @return 处理结果
     */
    public R<?> down(WebSocketDownRequest downRequest) {
        if (downRequest == null || downRequest.getCmd() == null) {
            log.warn("[WebSocket下行][参数异常] 下行对象为空, downRequest={}", downRequest);
            return R.error("WebSocket 下行对象为空");
        }

        log.info("[WebSocket下行][命令分发] deviceId={} productKey={} cmd={} sessionId={}",
                downRequest.getDeviceId(),
                downRequest.getProductKey(),
                downRequest.getCmd().getValue(),
                downRequest.getSessionId());

        // 前置检查
        R<?> preR = beforeDownAction(
                downRequest.getIoTProduct(), 
                downRequest.getData(), 
                downRequest);
        if (Objects.nonNull(preR)) {
            return preR;
        }

        // 命令分发
        R<?> result = null;
        switch (downRequest.getCmd()) {
            case DEV_ADD:
                result = newIoTDevice(downRequest);
                break;
            case DEV_DEL:
                result = deleteIoTDevice(downRequest);
                break;
            case DEV_UPDATE:
                result = updateIoTDevice(downRequest);
                break;
            case DEV_FUNCTION:
                result = funIoTDevice(downRequest);
                break;
            default:
                log.info("[WebSocket下行][未匹配到方法] deviceId={} productKey={} cmd={}",
                        downRequest.getDeviceId(),
                        downRequest.getProductKey(),
                        downRequest.getCmd());
        }

        return result;
    }

    /**
     * 创建设备
     */
    private R<?> newIoTDevice(WebSocketDownRequest downRequest) {
        log.info("[WebSocket下行][DEV_ADD] 开始添加设备, deviceId={}, productKey={}",
                downRequest.getDeviceId(),
                downRequest.getProductKey());

        IoTDevice ioTDevice = IoTDevice.builder()
                .productKey(downRequest.getProductKey())
                .deviceId(downRequest.getDeviceId())
                .build();

        int size = ioTDeviceMapper.selectCount(ioTDevice);
        if (size > 0) {
            log.warn("[WebSocket下行][DEV_ADD] 设备已存在, deviceId={}, productKey={}",
                    downRequest.getDeviceId(),
                    downRequest.getProductKey());
            return R.error(
                    ERROR_CODE.DEV_ADD_DEVICE_ID_EXIST.getCode(),
                    ERROR_CODE.DEV_ADD_DEVICE_ID_EXIST.getName());
        }

        // 保存设备
        Map<String, Object> saveResult = saveIoTDevice(downRequest);
        log.info("[WebSocket下行][DEV_ADD] 设备添加成功, deviceId={}", downRequest.getDeviceId());
        return R.ok(saveResult);
    }

    /**
     * 删除设备
     */
    private R<?> deleteIoTDevice(WebSocketDownRequest downRequest) {
        log.info("[WebSocket下行][DEV_DEL] 开始删除设备, deviceId={}, productKey={}",
                downRequest.getDeviceId(),
                downRequest.getProductKey());

        IoTDevice query = IoTDevice.builder()
                .productKey(downRequest.getProductKey())
                .deviceId(downRequest.getDeviceId())
                .build();

        IoTDevice device = ioTDeviceMapper.selectOne(query);
        if (device == null) {
            log.warn("[WebSocket下行][DEV_DEL] 设备不存在, deviceId={}, productKey={}",
                    downRequest.getDeviceId(),
                    downRequest.getProductKey());
            return R.error(
                    ERROR_CODE.DEV_DEL_DEVICE_NO_ID_EXIST.getCode(),
                    ERROR_CODE.DEV_DEL_DEVICE_NO_ID_EXIST.getName());
        }

        // 删除设备
        ioTDeviceMapper.delete(device);
        log.info("[WebSocket下行][DEV_DEL] 设备删除成功, deviceId={}", downRequest.getDeviceId());
        return R.ok();
    }

    /**
     * 更新设备
     */
    private R<?> updateIoTDevice(WebSocketDownRequest downRequest) {
        log.info("[WebSocket下行][DEV_UPDATE] 开始更新设备, deviceId={}, productKey={}",
                downRequest.getDeviceId(),
                downRequest.getProductKey());

        IoTDevice query = IoTDevice.builder()
                .productKey(downRequest.getProductKey())
                .deviceId(downRequest.getDeviceId())
                .build();

        IoTDevice device = ioTDeviceMapper.selectOne(query);
        if (device == null) {
            log.warn("[WebSocket下行][DEV_UPDATE] 设备不存在, deviceId={}, productKey={}",
                    downRequest.getDeviceId(),
                    downRequest.getProductKey());
            return R.error(
                    ERROR_CODE.DEV_UPDATE_DEVICE_NO_ID_EXIST.getCode(),
                    ERROR_CODE.DEV_UPDATE_DEVICE_NO_ID_EXIST.getName());
        }

        // 更新设备名称
        if (downRequest.getData() != null && downRequest.getData().getStr("deviceName") != null) {
            device.setDeviceName(downRequest.getData().getStr("deviceName"));
        }

        // 更新坐标信息
        if (downRequest.getData() != null 
                && StrUtil.isNotBlank(downRequest.getData().getStr("latitude"))
                && StrUtil.isNotBlank(downRequest.getData().getStr("longitude"))) {
            device.setCoordinate(
                    StrUtil.join(",",
                            downRequest.getData().getStr("longitude"),
                            downRequest.getData().getStr("latitude")));

            SupportMapAreas supportMapAreas = supportMapAreasMapper.selectMapAreas(
                    downRequest.getData().getStr("longitude"),
                    downRequest.getData().getStr("latitude"));
            if (supportMapAreas != null) {
                device.setAreasId(supportMapAreas.getId());
            }
        }

        ioTDeviceMapper.updateByPrimaryKey(device);
        log.info("[WebSocket下行][DEV_UPDATE] 设备更新成功, deviceId={}", downRequest.getDeviceId());
        return R.ok();
    }

    /**
     * 调用设备功能
     */
    private R<?> funIoTDevice(WebSocketDownRequest downRequest) {
        log.info("[WebSocket下行][DEV_FUNCTION] 开始调用功能, deviceId={}, productKey={}, sessionId={}",
                downRequest.getDeviceId(),
                downRequest.getProductKey(),
                downRequest.getSessionId());

        // 1. 验证设备存在
        IoTDevice query = IoTDevice.builder()
                .productKey(downRequest.getProductKey())
                .deviceId(downRequest.getDeviceId())
                .build();
        IoTDevice device = ioTDeviceMapper.selectOne(query);
        if (device == null) {
            log.warn("[WebSocket下行][DEV_FUNCTION] 设备不存在, deviceId={}, productKey={}",
                    downRequest.getDeviceId(),
                    downRequest.getProductKey());
            return R.error(
                    ERROR_CODE.DEV_CONFIG_DEVICE_NO_ID_EXIST.getCode(),
                    ERROR_CODE.DEV_CONFIG_DEVICE_NO_ID_EXIST.getName());
        }

        // 2. 验证设备是否在线（WebSocket 特定）
        if (StrUtil.isBlank(downRequest.getSessionId())) {
            log.warn("[WebSocket下行][DEV_FUNCTION] 设备未连接 WebSocket (sessionId 为空), deviceId={}",
                    downRequest.getDeviceId());
            return R.error("设备未连接 WebSocket，无法调用功能");
        }

        if (!sessionManager.isDeviceOnline(downRequest.getDeviceId())) {
            log.warn("[WebSocket下行][DEV_FUNCTION] 设备不在线, deviceId={}", 
                    downRequest.getDeviceId());
            return R.error("设备不在线，无法调用功能");
        }

        // 3. 执行处理链
        processorChain.process(downRequest);

        // 4. 调用全局函数（生命周期管理）
        R<?> globalFuncResult = callGlobalFunction(
                downRequest.getIoTProduct(), 
                device, 
                downRequest);
        if (Objects.nonNull(globalFuncResult)) {
            return globalFuncResult;
        }

        // 5. 发送消息给设备
        if (StrUtil.isNotBlank(downRequest.getPayload())) {
            boolean sendSuccess = sessionManager.sendMessageToDevice(
                    downRequest.getDeviceId(),
                    downRequest.getPayload()
            );

            if (sendSuccess) {
                log.info("[WebSocket下行][DEV_FUNCTION] 功能调用成功, deviceId={}", 
                        downRequest.getDeviceId());
                return R.ok();
            } else {
                log.error("[WebSocket下行][DEV_FUNCTION] 消息发送失败, deviceId={}", 
                        downRequest.getDeviceId());
                return R.error("消息发送失败");
            }
        } else {
            log.warn("[WebSocket下行][DEV_FUNCTION] payload 为空, deviceId={}", 
                    downRequest.getDeviceId());
            return R.error("功能数据编码结果为空");
        }
    }

    /**
     * 保存设备到数据库
     */
    private Map<String, Object> saveIoTDevice(WebSocketDownRequest downRequest) {
        IoTDevice ioTDevice = IoTDevice.builder()
                .deviceId(downRequest.getDeviceId())
                .createTime(System.currentTimeMillis() / 1000)
                .deviceName(downRequest.getData() != null ? 
                        downRequest.getData().getStr("deviceName", downRequest.getDeviceId()) 
                        : downRequest.getDeviceId())
                .state(DeviceStatus.offline.getCode())
                .iotId(downRequest.getProductKey() + downRequest.getDeviceId())
                .productName(downRequest.getIoTProduct().getName())
                .productKey(downRequest.getProductKey())
                .build();

        // 处理坐标信息
        if (downRequest.getData() != null
                && StrUtil.isNotBlank(downRequest.getData().getStr("latitude"))
                && StrUtil.isNotBlank(downRequest.getData().getStr("longitude"))) {
            ioTDevice.setCoordinate(
                    StrUtil.join(",",
                            downRequest.getData().getStr("longitude"),
                            downRequest.getData().getStr("latitude")));

            SupportMapAreas supportMapAreas = supportMapAreasMapper.selectMapAreas(
                    downRequest.getData().getStr("longitude"),
                    downRequest.getData().getStr("latitude"));
            if (supportMapAreas != null) {
                ioTDevice.setAreasId(supportMapAreas.getId());
            }
        }

        ioTDeviceMapper.insertUseGeneratedKeys(ioTDevice);
        log.debug("[WebSocket下行][DEV_ADD] 设备已插入数据库, iotId={}", ioTDevice.getIotId());

        return Map.of("iotId", ioTDevice.getIotId());
    }

    /**
     * 广播消息
     *
     * @param message 消息内容
     * @return 发送成功的数量
     */
    public int broadcast(String message) {
        try {
            log.info("[WebSocket下行][广播消息] 开始广播");
            int count = sessionManager.broadcast(message);
            log.info("[WebSocket下行][广播完成] 发送成功 {} 个会话", count);
            return count;
        } catch (Exception e) {
            log.error("[WebSocket下行][广播异常]", e);
            return 0;
        }
    }
}
