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

import cn.hutool.core.util.StrUtil;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Created by terry on 2017/8/1. */
public class UrlUtils {

  private static final String RE_TOP_DOMAIN =
      "(com\\.cn|net\\.cn|gov\\.cn|org\\.nz|org\\" + ".cn|com|net|org|gov|cc|biz|info|cn|co|me)";

  // 一级域名提取
  private static final String RE_TOP_1 = "(\\w*\\.?){1}\\." + RE_TOP_DOMAIN;

  // 二级域名提取
  private static final String RE_TOP_2 = "(\\w*\\.?){2}\\." + RE_TOP_DOMAIN;

  // 三级域名提取
  private static final String RE_TOP_3 = "(\\w*\\.?){3}\\." + RE_TOP_DOMAIN;

  private static final Pattern PATTEN_IP =
      Pattern.compile("((http://)|(https://))?((\\d+\\.){3}(\\d+))");
  private static final Pattern PATTEN_TOP1 = Pattern.compile(RE_TOP_1);
  private static final Pattern PATTEN_TOP2 = Pattern.compile(RE_TOP_2);
  private static final Pattern PATTEN_TOP3 = Pattern.compile(RE_TOP_3);

  public static String getDomain(String url, int level) {
    Matcher matcher = PATTEN_IP.matcher(url);
    if (matcher.find()) {
      return matcher.group(4);
    }

    switch (level) {
      case 1:
        matcher = PATTEN_TOP1.matcher(url);
        break;
      case 2:
        matcher = PATTEN_TOP2.matcher(url);
        break;
      case 3:
        matcher = PATTEN_TOP3.matcher(url);
        break;
      default:
        return "";
    }
    if (matcher.find()) {
      return matcher.group(0);
    }
    return "";
  }

  public static void main(String[] args) {
    String[] urls = {
      "yunfan.tcp.univiot.cn",
      "http://meiwen.me/src/index.html",
      "http://1000chi.com/game/index.html",
      "http://see.xidian.edu.cn/cpp/html/1429.html",
      "https://docs.python.org/2/howto/regex.html",
      "https://www.google.com.hk/search?client=aff-cs-360chromium&hs=TSj&q=url%E8%A7%A3%E6%9E%90%E5%9F%9F%E5%90"
          + "%8Dre&oq=url%E8%A7%A3%E6%9E%90%E5%9F%9F%E5%90%8Dre&gs_l=serp.3...74418.86867.0.87673.28.25.2.0.0.0.541"
          + ".2454.2-6j0j1j1.8.0....0...1c.1j4.53.serp..26.2.547.IuHTj4uoyHg",
      "file:///D:/code/echarts-2.0.3/doc/example/tooltip.html",
      "http://api.mongodb.org/python/current/faq.html#is-pymongo-thread-safe",
      "https://pypi.python.org/pypi/publicsuffix/",
      "http://127.0.0.1:8000",
    };

    System.out.println(StrUtil.subBefore("yunfan.tcp.univiot.cn", ".", false));
  }

  public static void test() {}
}
