// package cn.universal.dm.device.service.strategy;
//
// import cn.universal.core.message.DownRequest;
// import lombok.extern.slf4j.Slf4j;
// import org.springframework.stereotype.Component;
// import cn.hutool.core.util.StrUtil;
//
/// **
// * OPC UA协议子设备ID生成策略
// * 规则：网关设备ID + "_" + NodeId
// * 例如：860048070262660_ns=2;i=123, 860048070262660_ns=1;s=Temperature
// *
// * @author system
// * @date 2025-01-16
// */
// @Slf4j
// @Component
// public class OpcUaSubDeviceIdStrategy implements SubDeviceIdGeneration {
//
//    private static final String PROTOCOL_NAME = "opcua";
//
//    @Override
//    public boolean supports(String transportProtocol) {
//        return PROTOCOL_NAME.equalsIgnoreCase(transportProtocol);
//    }
//
//    @Override
//    public String generateSubDeviceId(DownRequest downRequest) {
//        if (downRequest == null) {
//            throw new IllegalArgumentException("DownRequest cannot be null");
//        }
//
//        String gwDeviceId = downRequest.getGwDeviceId();
//        String nodeId = downRequest.getNodeId(); // OPC UA的NodeId字段
//
//        if (StrUtil.isBlank(gwDeviceId)) {
//            throw new IllegalArgumentException("Gateway device ID cannot be blank");
//        }
//
//        if (StrUtil.isBlank(nodeId)) {
//            throw new IllegalArgumentException("NodeId cannot be blank for OPC UA protocol");
//        }
//
//        // 验证NodeId格式 (ns=数字;i=数字 或 ns=数字;s=字符串)
//        if (!isValidNodeId(nodeId)) {
//            throw new IllegalArgumentException("Invalid OPC UA NodeId format: " + nodeId);
//        }
//
//        // 将NodeId中的特殊字符替换为安全字符
//        String safeNodeId = nodeId.replace(":", "_").replace(";", "_").replace("=", "_");
//        String subDeviceId = gwDeviceId + "_" + safeNodeId;
//
//        log.debug("Generated OPC UA sub-device ID: {} for gateway: {} with NodeId: {}",
//                 subDeviceId, gwDeviceId, nodeId);
//
//        return subDeviceId;
//    }
//
//    @Override
//    public boolean validateSubDeviceId(String deviceId, DownRequest downRequest) {
//        if (StrUtil.isBlank(deviceId) || downRequest == null) {
//            return false;
//        }
//
//        String gwDeviceId = downRequest.getGwDeviceId();
//        String nodeId = downRequest.getNodeId();
//
//        if (StrUtil.isBlank(gwDeviceId) || StrUtil.isBlank(nodeId)) {
//            return false;
//        }
//
//        // 检查格式：网关ID_安全NodeId
//        String safeNodeId = nodeId.replace(":", "_").replace(";", "_").replace("=", "_");
//        String expectedId = gwDeviceId + "_" + safeNodeId;
//        return expectedId.equals(deviceId);
//    }
//
//    /**
//     * 验证OPC UA NodeId格式
//     * 支持：ns=数字;i=数字 或 ns=数字;s=字符串
//     */
//    private boolean isValidNodeId(String nodeId) {
//        if (StrUtil.isBlank(nodeId)) {
//            return false;
//        }
//
//        // 简单的格式验证：ns=数字;i=数字 或 ns=数字;s=字符串
//        return nodeId.matches("ns=\\d+;[is]=\\w+");
//    }
// }
