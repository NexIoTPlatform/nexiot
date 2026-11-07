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

package cn.universal.web.config.log;

import cn.hutool.core.util.HashUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.servlet.JakartaServletUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.util.WebUtils;

/**
 * @author gitee.com/NexIoT
 * @version 1.0
 * @since 2023/04/19
 */
@Slf4j
public class WebLogKits {

  private WebLogKits() {}

  public static JSONObject getParameters(HttpServletRequest request) {
    Enumeration<String> paramNames = request.getParameterNames();
    Map<String, Object> resultMap = new HashMap<>();
    while (paramNames.hasMoreElements()) {
      String paramName = paramNames.nextElement();
      String[] pv = request.getParameterValues(paramName);
      for (int i = 0; i < pv.length; i++) {
        if (pv[i].length() > 0) {
          pv[i] = shortenString(pv[i]);
        }
      }
      if (pv.length == 1) {
        resultMap.put(paramName, pv[0]);
      } else {
        resultMap.put(paramName, pv);
      }
    }
    return JSONUtil.parseObj(resultMap);
  }

  public static JSONObject getJsonData(HttpServletRequest request) {
    if (RequestMethod.POST.name().equalsIgnoreCase(request.getMethod())
        && StrUtil.containsAnyIgnoreCase(request.getContentType(), "json")) {
      String read = getPayload(request);
      try {
        if (StrUtil.isNotBlank(read)) {
          JSONObject jsonObject = JSONUtil.parseObj(read);
          tryShortenLargeStringInJson(jsonObject);
          return jsonObject;
        }
      } catch (Exception e) {
        log.info("json数据非法，{}", read);
      }
    }

    return new JSONObject();
  }

  public static JSONObject getHeadersInfo(HttpServletRequest request) {
    Map<String, String> map = new HashMap<>();
    map.put("IP", JakartaServletUtil.getClientIP(request));
    map.put("URI", request.getRequestURI());
    map.put("METHOD", request.getMethod());
    Enumeration<String> headerNames = request.getHeaderNames();
    try {
      while (headerNames.hasMoreElements()) {
        String key = headerNames.nextElement();
        String value = request.getHeader(key);
        if (RequestHeaderHelper.matchHeader(key)) {
          map.put(key, value);
        }
      }
    } catch (Exception ignore) {
    }

    return JSONUtil.parseObj(map);
  }

  public static String tryShortenRespContent(String content, int length) {
    if (content == null) {
      return "";
    }

    if (JSONUtil.isTypeJSON(content)) {
      content = WebLogKits.tryShortenLargeJson(content);
    } else {
      int min = Math.min(content.length(), length);
      if (min < content.length() - 3) {
        content = content.substring(0, min) + "...";
      } else {
        content = content.substring(0, min);
      }
    }
    return content;
  }

  private static String tryShortenLargeJson(String json) {
    if (json.length() > 1000) {
      JSONObject jsonObject = JSONUtil.parseObj(json);
      tryShortenLargeStringInJson(jsonObject);
      return jsonObject.toString();
    }
    return json;
  }

  private static String genHashKey(HttpServletRequest request, String domain) {
    Object flag = request.getAttribute(domain + "Flag");
    int hash = HashUtil.dekHash(request.getRequestURI() + flag + domain);
    return hash + "";
  }

  public static void setDmAttr(HttpServletRequest request, String domain) {
    request.setAttribute(domain + "Flag", System.currentTimeMillis());
    String hashKey = genHashKey(request, domain);
    request.setAttribute(hashKey, true);
  }

  public static boolean isDmAttrExist(HttpServletRequest request, String domain) {
    String hashKey = genHashKey(request, domain);
    Object b = request.getAttribute(hashKey);
    return Objects.equals(b, true);
  }

  private static void tryShortenLargeStringInJson(JSONObject paramJson) {
    Set<Entry<String, Object>> entries = paramJson.entrySet();
    for (Entry<String, Object> entry : entries) {
      if (entry.getValue() instanceof JSONObject) {
        tryShortenLargeStringInJson((JSONObject) entry.getValue());
      } else if (entry.getValue() instanceof JSONArray) {
        JSONArray value = (JSONArray) entry.getValue();
        JSONArray newArray = new JSONArray();
        for (Object o : value) {
          if (o instanceof JSONObject) {
            tryShortenLargeStringInJson((JSONObject) o);
            newArray.add(o);
          } else if (o instanceof String) {
            String str = (String) o;
            newArray.add(shortenString(str));
          } else {
            newArray.add(o);
          }
        }
        entry.setValue(newArray);
      } else if (entry.getValue() instanceof String) {
        String value = (String) entry.getValue();
        entry.setValue(shortenString(value));
      }
    }
  }

  private static String shortenString(String str) {
    if (!str.startsWith("http")) {
      if (str.length() > 100) {
        return str.substring(0, 100) + "...";
      }
    }
    return str;
  }

  private static String getPayload(HttpServletRequest request) {
    RequestWrapper wrapper = WebUtils.getNativeRequest(request, RequestWrapper.class);
    return wrapper.getBodyString();
  }
}
