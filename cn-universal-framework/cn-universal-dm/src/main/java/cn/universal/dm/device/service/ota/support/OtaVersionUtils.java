/*
 *
 * Copyright (c) 2025, NexIoT. All Rights Reserved.
 *
 * @Description: OTA版本比较工具
 * @Author: gitee.com/NexIoT
 *
 */

package cn.universal.dm.device.service.ota.support;

import cn.hutool.core.util.StrUtil;
import java.math.BigInteger;

/** OTA版本工具 */
public final class OtaVersionUtils {

  private OtaVersionUtils() {}

  public static int compareVersion(String left, String right) {
    if (StrUtil.equals(left, right)) {
      return 0;
    }
    if (StrUtil.isBlank(left)) {
      return -1;
    }
    if (StrUtil.isBlank(right)) {
      return 1;
    }
    String[] leftParts = left.split("[._-]");
    String[] rightParts = right.split("[._-]");
    int length = Math.max(leftParts.length, rightParts.length);
    for (int i = 0; i < length; i++) {
      String l = i < leftParts.length ? leftParts[i] : "0";
      String r = i < rightParts.length ? rightParts[i] : "0";
      int result = compareToken(l, r);
      if (result != 0) {
        return result;
      }
    }
    return left.compareToIgnoreCase(right);
  }

  public static boolean isTargetNewer(String currentVersion, String targetVersion) {
    if (StrUtil.isBlank(targetVersion)) {
      return false;
    }
    return compareVersion(targetVersion, currentVersion) > 0;
  }

  private static int compareToken(String left, String right) {
    boolean leftNumeric = StrUtil.isNumeric(left);
    boolean rightNumeric = StrUtil.isNumeric(right);
    if (leftNumeric && rightNumeric) {
      // OTA 版本号可能来自设备构建号，不能假设每段都能放进 int。
      return new BigInteger(left).compareTo(new BigInteger(right));
    }
    return left.compareToIgnoreCase(right);
  }
}
