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

package cn.universal.rule.rulego.client;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpStatus;
import cn.hutool.json.JSONUtil;
import cn.universal.rule.rulego.model.RulegoApiResponse;
import cn.universal.rule.rulego.model.RulegoChainInfo;
import cn.universal.rule.rulego.model.RulegoChainListResponse;
import cn.universal.rule.rulego.model.RulegoChainResponse;
import cn.universal.rule.rulego.model.RulegoSaveRequest;
import cn.universal.rule.rulego.model.RulegoSuccessResponse;
import cn.universal.rule.rulego.model.RulegoTriggerRequest;
import cn.universal.rule.rulego.model.RulegoTriggerResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * rulego API客户端 - 基于Hutool实现
 *
 * @author gitee.com/NexIoT
 * @since 2025/01/15
 */
@Slf4j
@Component
public class RulegoApiClient {

  @Value("${rulego.api.base-url:http://localhost:9090}")
  private String baseUrl;

  @Value("${rulego.web.base-url:http://127.0.0.1:9090/#}")
  private String webUrl;

  @Value("${rulego.api.token:}")
  private String token;

  /**
   * 保存规则链基础信息
   *
   * @param request 保存请求
   * @return 响应结果
   */
  public RulegoApiResponse<RulegoSuccessResponse> saveChainInfo(RulegoSaveRequest request) {
    String url = baseUrl + "/api/v1/rules/" + request.getId() + "/base";
    return postRequest(url, request, RulegoSuccessResponse.class);
  }

  /**
   * 获取规则链列表
   *
   * @return 规则链列表响应
   */
  public RulegoApiResponse<RulegoChainListResponse> getChainList() {
    String url = baseUrl + "/api/v1/rules";
    return getRequest(url, RulegoChainListResponse.class);
  }

  /**
   * 获取规则链详情
   *
   * @param chainId 规则链ID
   * @return 规则链详情响应
   */
  public RulegoApiResponse<RulegoChainInfo> getChainDetail(String chainId) {
    String url = baseUrl + "/api/v1/rules/" + chainId;
    return getRequest(url, RulegoChainInfo.class);
  }

  /**
   * 保存规则链DSL
   *
   * @param chainId 规则链ID
   * @param dsl DSL内容
   * @return 响应结果
   */
  public RulegoApiResponse<RulegoChainResponse> saveChainDsl(String chainId, String dsl) {
    String url = baseUrl + "/api/v1/rules/" + chainId;
    return postRequest(url, dsl, RulegoChainResponse.class);
  }

  /**
   * 部署规则链
   *
   * @param chainId 规则链ID
   * @return 响应结果
   */
  public RulegoApiResponse<RulegoSuccessResponse> deployChain(String chainId) {
    String url = baseUrl + "/api/v1/rules/" + chainId + "/operate/start";
    return postRequest(url, null, RulegoSuccessResponse.class);
  }

  /**
   * 停止规则链
   *
   * @param chainId 规则链ID
   * @return 响应结果
   */
  public RulegoApiResponse<RulegoSuccessResponse> stopChain(String chainId) {
    String url = baseUrl + "/api/v1/rules/" + chainId + "/operate/stop";
    return postRequest(url, null, RulegoSuccessResponse.class);
  }

  /**
   * 删除规则链
   *
   * @param chainId 规则链ID
   * @return 响应结果
   */
  public RulegoApiResponse<RulegoSuccessResponse> deleteChain(String chainId) {
    String url = baseUrl + "/api/v1/rules/" + chainId;
    return deleteRequest(url, RulegoSuccessResponse.class);
  }

  /**
   * 触发规则链执行（同步）
   *
   * @param request 触发请求
   * @return 执行结果
   */
  public RulegoApiResponse<RulegoTriggerResponse> triggerChainSync(RulegoTriggerRequest request) {
    String url = baseUrl + "/api/v1/rules/" + request.getChainId() + "/trigger";
    return postRequest(url, request, RulegoTriggerResponse.class);
  }

  /**
   * 触发规则链执行（异步）
   *
   * @param request 触发请求
   * @return 响应结果
   */
  public RulegoApiResponse<RulegoChainResponse> triggerChainAsync(RulegoTriggerRequest request) {
    String url = baseUrl + "/api/v1/rules/" + request.getChainId() + "/trigger/async";
    return postRequest(url, request, RulegoChainResponse.class);
  }

  /**
   * 获取设计器URL
   *
   * @param chainId 规则链ID
   * @return 设计器URL
   */
  public String getDesignerUrl(String chainId) {
    return webUrl + chainId;
  }

  /**
   * GET请求 - 基于Hutool实现
   *
   * @param url 请求URL
   * @param dataType 数据类型
   * @param <T> 数据类型泛型
   * @return 统一响应结果
   */
  private <T> RulegoApiResponse<T> getRequest(String url, Class<T> dataType) {
    log.debug("发送GET请求: {}", url);

    try {
      HttpRequest request = HttpRequest.get(url).header("Content-Type", "application/json");

      // 添加认证头
      if (StrUtil.isNotBlank(token)) {
        request.header("Authorization", "Bearer " + token);
      }

      HttpResponse response = request.execute();
      log.debug("GET请求响应状态码: {}", response.getStatus());

      return handleResponse(response, dataType);
    } catch (Exception e) {
      log.error("GET请求失败: {}, 错误: {}", url, e.getMessage(), e);
      return RulegoApiResponse.error(e.getMessage());
    }
  }

  /**
   * POST请求 - 基于Hutool实现
   *
   * @param url 请求URL
   * @param requestBody 请求体
   * @param dataType 数据类型
   * @param <T> 数据类型泛型
   * @return 统一响应结果
   */
  private <T> RulegoApiResponse<T> postRequest(String url, Object requestBody, Class<T> dataType) {
    log.debug("发送POST请求: {}, 请求体: {}", url, requestBody);

    try {
      HttpRequest request = HttpRequest.post(url).header("Content-Type", "application/json");

      // 添加认证头
      if (StrUtil.isNotBlank(token)) {
        request.header("Authorization", "Bearer " + token);
      }

      // 设置请求体
      if (requestBody != null) {
        if (requestBody instanceof String) {
          request.body((String) requestBody);
        } else {
          request.body(JSONUtil.toJsonStr(requestBody));
        }
      }

      HttpResponse response = request.execute();
      log.debug("POST请求响应状态码: {}", response.getStatus());

      return handleResponse(response, dataType);
    } catch (Exception e) {
      log.error("POST请求失败: {}, 错误: {}", url, e.getMessage(), e);
      return RulegoApiResponse.error(e.getMessage());
    }
  }

  /**
   * DELETE请求 - 基于Hutool实现
   *
   * @param url 请求URL
   * @param dataType 数据类型
   * @param <T> 数据类型泛型
   * @return 统一响应结果
   */
  private <T> RulegoApiResponse<T> deleteRequest(String url, Class<T> dataType) {
    log.debug("发送DELETE请求: {}", url);

    try {
      HttpRequest request = HttpRequest.delete(url).header("Content-Type", "application/json");

      // 添加认证头
      if (StrUtil.isNotBlank(token)) {
        request.header("Authorization", "Bearer " + token);
      }

      HttpResponse response = request.execute();
      log.debug("DELETE请求响应状态码: {}", response.getStatus());

      return handleResponse(response, dataType);
    } catch (Exception e) {
      log.error("DELETE请求失败: {}, 错误: {}", url, e.getMessage(), e);
      return RulegoApiResponse.error(e.getMessage());
    }
  }

  /**
   * 处理HTTP响应
   *
   * @param response HTTP响应
   * @param <T> 响应类型泛型
   * @return 处理后的响应对象
   */
  @SuppressWarnings("unchecked")
  private <T> RulegoApiResponse<T> handleResponse(HttpResponse response, Class<T> dataType) {
    int statusCode = response.getStatus();
    String responseBody = response.body();

    log.debug("响应状态码: {}, 响应体: {}", statusCode, responseBody);

    // HTTP 200表示成功
    if (statusCode == HttpStatus.HTTP_OK) {
      // 有响应体，直接解析
      if (StrUtil.isNotBlank(responseBody)) {
        try {
          T data;
          // 根据不同的数据类型进行特殊处理
          if (RulegoChainListResponse.class.equals(dataType)) {
            data = (T) handleChainListResponse(responseBody);
          } else if (RulegoChainInfo.class.equals(dataType)) {
            data = (T) handleChainInfoResponse(responseBody);
          } else {
            // 其他类型直接解析JSON
            data = JSONUtil.toBean(responseBody, dataType);
          }
          return RulegoApiResponse.success(data, "操作成功");
        } catch (Exception e) {
          log.warn("响应体解析失败: {}, 错误: {}", responseBody, e.getMessage());
          return RulegoApiResponse.error("响应解析失败: " + e.getMessage());
        }
      } else {
        // 没有响应体，创建默认成功响应
        log.debug("响应体为空，创建默认成功响应");
        return RulegoApiResponse.success(null, "操作成功");
      }
    } else {
      // 非200状态码，创建错误响应
      String errorMessage =
          StrUtil.isNotBlank(responseBody) ? responseBody : "HTTP错误: " + statusCode;
      log.warn("HTTP错误: 状态码={}, 错误信息={}", statusCode, errorMessage);
      return RulegoApiResponse.error(errorMessage);
    }
  }

  /**
   * 处理规则链详情响应
   *
   * @param responseBody 响应体
   * @return 转换后的响应对象
   */
  private RulegoChainInfo handleChainInfoResponse(String responseBody) {
    try {
      log.debug("开始处理规则链详情响应: {}", responseBody);

      // 解析原始响应
      var jsonObject = JSONUtil.parseObj(responseBody);

      // RuleGo返回有两种可能结构：
      // 1) 扁平结构: { id, name, root, disabled, additionalInfo, dsl? }
      // 2) 嵌套结构: { ruleChain: { id, name, root, disabled, additionalInfo }, metadata: {...} }
      var hasNestedRuleChain = jsonObject.containsKey("ruleChain");
      var ruleChainObj = hasNestedRuleChain ? jsonObject.getJSONObject("ruleChain") : jsonObject;

      log.debug("解析规则链详情: {}", ruleChainObj.getStr("id"));

      // 获取additionalInfo对象，如果不存在则创建空对象
      var additionalInfo = ruleChainObj.getJSONObject("additionalInfo");
      if (additionalInfo == null) {
        additionalInfo = JSONUtil.createObj();
      }

      // dsl：为保证同步DSL完整性，这里将整个返回体作为DSL保存
      // 这样能够保留 ruleChain + metadata 的完整结构
      String dsl = JSONUtil.toJsonStr(jsonObject);

      // 创建RulegoChainInfo对象
      var chainInfo =
          cn.universal.rule.rulego.model.RulegoChainInfo.builder()
              .id(ruleChainObj.getStr("id"))
              .name(ruleChainObj.getStr("name"))
              .root(ruleChainObj.getBool("root", false))
              .description(additionalInfo.getStr("description", ""))
              .status(ruleChainObj.getBool("disabled", false) ? "disabled" : "enabled")
              .createTime(additionalInfo.getStr("createTime", ""))
              .updateTime(additionalInfo.getStr("updateTime", ""))
              .dsl(dsl)
              .build();

      log.debug("成功转换规则链详情: {}", chainInfo.getId());

      return chainInfo;
    } catch (Exception e) {
      log.error("处理规则链详情响应失败: {}", e.getMessage(), e);
      throw new RuntimeException("处理规则链详情响应失败: " + e.getMessage());
    }
  }

  /**
   * 处理规则链列表响应 RuleGo API返回的格式是: {"items": [{"ruleChain": {...}}, ...], "page": 1, "size": 20,
   * "total": 2} 我们需要转换为: {"success": true, "message": "success", "code": "200", "data": [...]}
   *
   * @param responseBody 响应体
   * @return 转换后的响应对象
   */
  private RulegoChainListResponse handleChainListResponse(String responseBody) {
    try {
      log.debug("开始处理规则链列表响应: {}", responseBody);

      // 解析原始响应
      var jsonObject = JSONUtil.parseObj(responseBody);
      var items = jsonObject.getJSONArray("items");

      log.debug("解析到items数组，长度: {}", items.size());

      // 转换items为RulegoChainInfo列表
      var chainInfoList = new java.util.ArrayList<cn.universal.rule.rulego.model.RulegoChainInfo>();

      for (int i = 0; i < items.size(); i++) {
        var item = items.getJSONObject(i);
        var ruleChain = item.getJSONObject("ruleChain");

        log.debug("处理第{}个规则链: {}", i, ruleChain.getStr("id"));

        // 创建RulegoChainInfo对象
        var chainInfo =
            cn.universal.rule.rulego.model.RulegoChainInfo.builder()
                .id(ruleChain.getStr("id"))
                .name(ruleChain.getStr("name"))
                .root(ruleChain.getBool("root", false))
                .description(ruleChain.getJSONObject("additionalInfo").getStr("description", ""))
                .status(ruleChain.getBool("disabled", false) ? "disabled" : "enabled")
                .createTime(ruleChain.getJSONObject("additionalInfo").getStr("createTime", ""))
                .updateTime(ruleChain.getJSONObject("additionalInfo").getStr("updateTime", ""))
                .build();

        chainInfoList.add(chainInfo);
      }

      log.debug("成功转换{}个规则链信息", chainInfoList.size());

      // 创建标准响应格式
      var result =
          RulegoChainListResponse.builder()
              .success(true)
              .message("获取规则链列表成功")
              .code("200")
              .data(chainInfoList)
              .build();

      log.debug(
          "创建响应对象: success={}, message={}, dataSize={}",
          result.isSuccess(),
          result.getMessage(),
          result.getData() != null ? result.getData().size() : 0);

      return result;
    } catch (Exception e) {
      log.error("处理规则链列表响应失败: {}", e.getMessage(), e);
      return RulegoChainListResponse.builder()
          .success(false)
          .message("处理响应失败: " + e.getMessage())
          .code("500")
          .build();
    }
  }
}
