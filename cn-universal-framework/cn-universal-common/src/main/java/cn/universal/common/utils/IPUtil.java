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

package cn.universal.common.utils;

import java.util.Set;

public class IPUtil {

  /**
   * 根据IP,及可用Ip列表来判断ip是否包含在白名单之中
   *
   * @param ip
   * @param ipList
   * @return
   */
  public static boolean checkIPWhitelist(String ip, Set<String> ipList) {
    if (ipList.isEmpty() || ipList.contains(ip)) {
      return true;
    }
    for (String allow : ipList) {
      if (allow.indexOf("-") > -1) { // 处理 类似 192.168.0.0-192.168.2.1
        String[] tempAllow = allow.split("-");
        String[] from = tempAllow[0].split("\\.");
        String[] end = tempAllow[1].split("\\.");
        String[] tag = ip.split("\\.");
        boolean check = true;
        for (int i = 0; i < 4; i++) { // 对IP从左到右进行逐段匹配
          int s = Integer.valueOf(from[i]);
          int t = Integer.valueOf(tag[i]);
          int e = Integer.valueOf(end[i]);
          if (!(s <= t && t <= e)) {
            check = false;
            break;
          }
        }
        if (check) {
          return true;
        }
      } else if (allow.contains("/")) { // 处理 网段 xxx.xxx.xxx./24
        int splitIndex = allow.indexOf("/");
        // 取出子网段
        String ipSegment = allow.substring(0, splitIndex); // 192.168.3.0
        // 子网数
        String netmask = allow.substring(splitIndex + 1); // 24
        // ip 转二进制
        long ipLong = ipToLong(ip);
        // 子网二进制
        long maskLong = (2L << 32 - 1) - (2L << Integer.valueOf(32 - Integer.valueOf(netmask)) - 1);
        // ip与和子网相与 得到 网络地址
        String calcSegment = longToIP(ipLong & maskLong);
        // 如果计算得出网络地址和库中网络地址相同 则合法
        if (ipSegment.equals(calcSegment)) {
          return true;
        }
      }
    }
    return false;
  }

  private static long ipToLong(String strIP) {
    long[] ip = new long[4];
    // 先找到IP地址字符串中.的位置
    int position1 = strIP.indexOf(".");
    int position2 = strIP.indexOf(".", position1 + 1);
    int position3 = strIP.indexOf(".", position2 + 1);
    // 将每个.之间的字符串转换成整型
    ip[0] = Long.parseLong(strIP.substring(0, position1));
    ip[1] = Long.parseLong(strIP.substring(position1 + 1, position2));
    ip[2] = Long.parseLong(strIP.substring(position2 + 1, position3));
    ip[3] = Long.parseLong(strIP.substring(position3 + 1));
    return (ip[0] << 24) + (ip[1] << 16) + (ip[2] << 8) + ip[3];
  }

  // 将10进制整数形式转换成127.0.0.1形式的IP地址
  private static String longToIP(long longIP) {
    StringBuilder sb = new StringBuilder("");
    // 直接右移24位
    sb.append(String.valueOf(longIP >>> 24));
    sb.append(".");
    // 将高8位置0，然后右移16位
    sb.append(String.valueOf((longIP & 0x00FFFFFF) >>> 16));
    sb.append(".");
    sb.append(String.valueOf((longIP & 0x0000FFFF) >>> 8));
    sb.append(".");
    sb.append(String.valueOf(longIP & 0x000000FF));
    return sb.toString();
  }
}
