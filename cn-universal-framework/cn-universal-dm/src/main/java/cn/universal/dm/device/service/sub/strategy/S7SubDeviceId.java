// package cn.universal.dm.device.service.strategy;
//
// import cn.hutool.core.util.StrUtil;
// import cn.universal.core.message.DownRequest;
// import lombok.extern.slf4j.Slf4j;
// import org.springframework.stereotype.Component;
//
/// **
// * S7西门子协议子设备ID生成策略 规则：网关设备ID + "." + DB块号 + "." + 偏移地址 例如：860048070262660.DB1.0,
// * 860048070262660.DB2.100
// *
// * @author system
// * @date 2025-01-16
// */
// @Slf4j
// @Component
// public class S7SubDeviceIdStrategy implements SubDeviceIdGeneration {
//
//  private static final String PROTOCOL_NAME = "s7";
//
//  @Override
//  public boolean supports(String transportProtocol) {
//    return PROTOCOL_NAME.equalsIgnoreCase(transportProtocol);
//  }
//
//  @Override
//  public String generateSubDeviceId(DownRequest downRequest) {
//    if (downRequest == null) {
//      throw new IllegalArgumentException("DownRequest cannot be null");
//    }
//
//    String gwDeviceId = downRequest.getGwDeviceId();
////    String dbNumber = downRequest.getDbNumber(); // S7的DB块号
////    String offset = downRequest.getOffset(); // S7的偏移地址
////
////    if (StrUtil.isBlank(gwDeviceId)) {
////      throw new IllegalArgumentException("Gateway device ID cannot be blank");
////    }
////
////    if (StrUtil.isBlank(dbNumber)) {
////      throw new IllegalArgumentException("DB number cannot be blank for S7 protocol");
////    }
////
////    if (StrUtil.isBlank(offset)) {
////      throw new IllegalArgumentException("Offset cannot be blank for S7 protocol");
////    }
//
//    // 验证DB块号格式 (DB1, DB2, etc.)
//    if (!isValidDbNumber(dbNumber)) {
//      throw new IllegalArgumentException("Invalid S7 DB number format: " + dbNumber);
//    }
//
//    // 验证偏移地址格式 (数字)
//    if (!isValidOffset(offset)) {
//      throw new IllegalArgumentException("Invalid S7 offset format: " + offset);
//    }
//
//    String subDeviceId = gwDeviceId + "." + dbNumber + "." + offset;
//
//    log.debug(
//        "Generated S7 sub-device ID: {} for gateway: {} with DB: {} offset: {}",
//        subDeviceId,
//        gwDeviceId,
//        dbNumber,
//        offset);
//
//    return subDeviceId;
//  }
//
//  @Override
//  public boolean validateSubDeviceId(String deviceId, DownRequest downRequest) {
//    if (StrUtil.isBlank(deviceId) || downRequest == null) {
//      return false;
//    }
//
//    String gwDeviceId = downRequest.getGwDeviceId();
//    String dbNumber = downRequest.getDbNumber();
//    String offset = downRequest.getOffset();
//
//    if (StrUtil.isBlank(gwDeviceId) || StrUtil.isBlank(dbNumber) || StrUtil.isBlank(offset)) {
//      return false;
//    }
//
//    // 检查格式：网关ID.DB块号.偏移地址
//    String expectedId = gwDeviceId + "." + dbNumber + "." + offset;
//    return expectedId.equals(deviceId);
//  }
//
//  /** 验证S7 DB块号格式 支持：DB1, DB2, DB100, etc. */
//  private boolean isValidDbNumber(String dbNumber) {
//    if (StrUtil.isBlank(dbNumber)) {
//      return false;
//    }
//
//    return dbNumber.matches("DB\\d+");
//  }
//
//  /** 验证S7偏移地址格式 支持：数字格式 */
//  private boolean isValidOffset(String offset) {
//    if (StrUtil.isBlank(offset)) {
//      return false;
//    }
//
//    try {
//      Integer.parseInt(offset);
//      return true;
//    } catch (NumberFormatException e) {
//      return false;
//    }
//  }
// }
