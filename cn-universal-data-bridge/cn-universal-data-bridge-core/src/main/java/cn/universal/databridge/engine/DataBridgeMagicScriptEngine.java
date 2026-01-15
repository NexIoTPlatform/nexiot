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

package cn.universal.databridge.engine;

import cn.hutool.core.util.StrUtil;
import cn.universal.core.engine.MagicScript;
import cn.universal.core.engine.MagicScriptContext;
import cn.universal.databridge.entity.DataBridgeConfig;
import cn.universal.databridge.entity.ResourceConnection;
import cn.universal.persistence.base.BaseUPRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 数据桥接Magic脚本引擎 适配ProtocolCodecMagic到数据桥接场景
 *
 * @version 2.0 @Author gitee.com/NexIoT
 * @since 2025/1/15
 */
@Component
@Slf4j
public class DataBridgeMagicScriptEngine {

    /**
     * 脚本缓存
     */
    private final Map<String, MagicScript> scriptCache = new ConcurrentHashMap<>();

    /**
     * 执行数据桥接Magic脚本 - 输出方向 (IoT -> 外部系统)
     */
    public Object executeIotToYourScript(
            String script,
            BaseUPRequest request,
            DataBridgeConfig config,
            ResourceConnection connection) {
        try {
            // 构建脚本上下文
            MagicScriptContext context = buildScriptContext(request, config, connection);

            // 获取或创建脚本
            MagicScript magicScript = getOrCreateScript(script, "iotToYour");

            // 执行脚本
            Object result = magicScript.execute(context);

            log.debug(
                    "Magic脚本iotToYour执行成功，返回类型: {}",
                    result != null ? result.getClass().getSimpleName() : "null");
            return result;

        } catch (Exception e) {
            log.error("Magic脚本iotToYour执行失败: {}", e.getMessage(), e);
            throw new RuntimeException("Magic脚本iotToYour执行失败: " + e.getMessage(), e);
        }
    }

    /**
     * 执行数据桥接Magic脚本 - 输入方向 (外部系统 -> IoT)
     */
    public Object executeYourToIotScript(
            String script, Object externalData, DataBridgeConfig config, ResourceConnection connection) {
        try {
            // 构建脚本上下文
            MagicScriptContext context = buildYourToIotScriptContext(externalData, config, connection);

            // 获取或创建脚本
            MagicScript magicScript = getOrCreateScript(script, "yourToIot");

            // 执行脚本
            Object result = magicScript.execute(context);

            log.debug(
                    "Magic脚本yourToIot执行成功，返回类型: {}",
                    result != null ? result.getClass().getSimpleName() : "null");
            return result;

        } catch (Exception e) {
            log.error("Magic脚本yourToIot执行失败: {}", e.getMessage(), e);
            throw new RuntimeException("Magic脚本yourToIot执行失败: " + e.getMessage(), e);
        }
    }

    /**
     * 批量执行数据桥接Magic脚本
     */
    public List<Object> executeBatchDataBridgeScript(
            String script,
            List<BaseUPRequest> requests,
            DataBridgeConfig config,
            ResourceConnection connection) {
        try {
            // 构建脚本上下文
            MagicScriptContext context = buildBatchScriptContext(requests, config, connection);

            // 获取或创建脚本
            MagicScript magicScript = getOrCreateScript(script, "iotToYour");

            // 执行脚本
            Object result = magicScript.execute(context);

            // 处理返回结果
            if (result instanceof List) {
                @SuppressWarnings("unchecked")
                List<Object> resultList = (List<Object>) result;
                log.debug("Magic脚本批量执行成功，返回数量: {}", resultList.size());
                return resultList;
            } else {
                log.warn(
                        "Magic脚本批量执行返回类型不是List: {}",
                        result != null ? result.getClass().getSimpleName() : "null");
                return List.of(result);
            }

        } catch (Exception e) {
            log.error("Magic脚本批量执行失败: {}", e.getMessage(), e);
            throw new RuntimeException("Magic脚本批量执行失败: " + e.getMessage(), e);
        }
    }

    /**
     * 验证Magic脚本语法
     */
    public boolean validateScript(String script) {
        try {
            if (StrUtil.isBlank(script)) {
                return false;
            }

            // 容错：反解码可能被 XSS 过滤器转义的脚本
            script = htmlDecode(script);

            // 创建脚本并编译
            MagicScript magicScript = MagicScript.create(script, null);
            magicScript.compile();

            return true;
        } catch (Exception e) {
            log.error("Magic脚本验证失败: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * 构建脚本上下文
     */
    private MagicScriptContext buildScriptContext(
            BaseUPRequest request, DataBridgeConfig config, ResourceConnection connection) {
        MagicScriptContext context = new MagicScriptContext();

        // 基础数据
        context.set("request", request);
        context.set("config", config);
        context.set("connection", connection);

        // 设备信息
        if (request.getIoTDeviceDTO() != null) {
            context.set("deviceId", request.getIoTDeviceDTO().getDeviceId());
            context.set("productKey", request.getIoTDeviceDTO().getProductKey());
        }

        // 消息信息
        context.set("messageType", request.getMessageType().name());
        context.set("timestamp", System.currentTimeMillis());
        context.set("properties", request.getProperties());

        // 配置信息
        if (StrUtil.isNotBlank(config.getConfig())) {
            try {
                context.set("configJson", cn.hutool.json.JSONUtil.parseObj(config.getConfig()));
            } catch (Exception e) {
                log.warn("配置JSON解析失败: {}", e.getMessage());
            }
        }

        return context;
    }

    /**
     * 构建批量脚本上下文
     */
    private MagicScriptContext buildBatchScriptContext(
            List<BaseUPRequest> requests, DataBridgeConfig config, ResourceConnection connection) {
        MagicScriptContext context = new MagicScriptContext();

        // 批量数据
        context.set("requests", requests);
        context.set("config", config);
        context.set("connection", connection);
        context.set("batchSize", requests.size());

        // 配置信息
        if (StrUtil.isNotBlank(config.getConfig())) {
            try {
                context.set("configJson", cn.hutool.json.JSONUtil.parseObj(config.getConfig()));
            } catch (Exception e) {
                log.warn("配置JSON解析失败: {}", e.getMessage());
            }
        }

        return context;
    }

    /**
     * 构建yourToIot脚本上下文
     */
    private MagicScriptContext buildYourToIotScriptContext(
            Object externalData, DataBridgeConfig config, ResourceConnection connection) {
        MagicScriptContext context = new MagicScriptContext();

        // 外部数据
        context.set("externalData", externalData);
        context.set("config", config);
        context.set("connection", connection);

        // 配置信息
        if (StrUtil.isNotBlank(config.getConfig())) {
            try {
                context.set("configJson", cn.hutool.json.JSONUtil.parseObj(config.getConfig()));
            } catch (Exception e) {
                log.warn("配置JSON解析失败: {}", e.getMessage());
            }
        }

        return context;
    }

    /**
     * 获取或创建脚本
     */
    private MagicScript getOrCreateScript(String script, String method) {
        // 容错：反解码可能被 XSS 过滤器转义的脚本
        final String decodedScript = htmlDecode(script);
        // 使用脚本内容和方法的hash作为缓存key
        String cacheKey = String.valueOf((decodedScript + method).hashCode());

        return scriptCache.computeIfAbsent(
                cacheKey,
                key -> {
                    try {
                        // 包装脚本，添加必要的函数调用
                        String wrappedScript = wrapScript(decodedScript, method);

                        // 创建并编译脚本
                        MagicScript magicScript = MagicScript.create(wrappedScript, null);
                        magicScript.compile();

                        log.debug("创建新的Magic脚本，缓存key: {}, 方法: {}", cacheKey, method);
                        return magicScript;

                    } catch (Exception e) {
                        log.error("创建Magic脚本失败: {}", e.getMessage(), e);
                        throw new RuntimeException("创建Magic脚本失败: " + e.getMessage(), e);
                    }
                });
    }

    /**
     * 包装脚本，添加必要的函数调用
     * <p>
     * 处理以下场景：
     * 1. 脚本只定义函数，无调用 -> 自动添加调用语句
     * 2. 脚本已有顶层 return 调用 -> 直接返回
     * 3. 脚本是纯逻辑，无函数定义 -> 包装成脚本体
     */
    private String wrapScript(String script, String method) {
        String trimmed = script.trim();

        // 检查脚本是否为函数定义（var/function iotToYour/yourToIot）
        boolean isFunctionDef = trimmed.matches("(?s).*(var|function)\\s+" + method + "\\s*[=(].*");

        // 检查是否已有顶层的函数调用 return 语句
        boolean hasTopLevelReturn = trimmed.matches("(?s).*\\breturn\\s+" + method + "\\s*\\(.*");

        // 如果是函数定义且没有调用，添加调用
        if (isFunctionDef && !hasTopLevelReturn) {
            return script + "\nreturn " + method + "(request, config, connection);";
        }

        // 如果已有顶层 return 调用，直接返回
        if (hasTopLevelReturn) {
            return script;
        }

        // 默认场景：脚本是纯逻辑，需要包装调用
        return switch (method) {
            case "iotToYour" -> script + "\nreturn iotToYour(request, config, connection);";
            case "yourToIot" -> script + "\nreturn yourToIot(externalData, config, connection);";
            default -> script + "\nreturn processData(request, config, connection);";
        };
    }

    /**
     * 清除脚本缓存
     */
    public void clearCache() {
        scriptCache.clear();
        log.info("Magic脚本缓存已清除");
    }

    /**
     * 获取缓存大小
     */
    public int getCacheSize() {
        return scriptCache.size();
    }

    /**
     * HTML 实体反解码，避免 =&gt;、&lt; 等影响脚本解析
     */
    private String htmlDecode(String text) {
        if (text == null) return null;
        String cur = text;
        for (int i = 0; i < 2; i++) {
            String prev = cur;
            cur = cur
                    .replace("&lt;", "<")
                    .replace("&gt;", ">")
                    .replace("&quot;", "\"")
                    .replace("&#39;", "'")
                    .replace("&amp;", "&");
            if (prev.equals(cur)) break;
        }
        return cur;
    }
}
