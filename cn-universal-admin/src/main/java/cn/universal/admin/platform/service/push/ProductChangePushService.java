/*
 *
 * Copyright (c) 2025, NexIoT. All Rights Reserved.
 *
 * @Description: 产品变更推送服务，向订阅应用推送产品CRUD事件
 * @Author: gitee.com/NexIoT
 *
 */

package cn.universal.admin.platform.service.push;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.universal.dm.device.service.push.transform.AppPushConfigChangeNotifier;
import cn.universal.persistence.entity.IoTProduct;
import cn.universal.persistence.entity.IoTUserApplication;
import cn.universal.persistence.mapper.IoTUserApplicationMapper;
import jakarta.annotation.Resource;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * 产品变更推送服务
 * <p>
 * 当产品发生增删改查时，向订阅了产品变更的应用推送通知
 */
@Service
@Slf4j
public class ProductChangePushService {

    @Resource
    private IoTUserApplicationMapper applicationMapper;

    @Resource
    private AppPushConfigChangeNotifier appPushConfigChangeNotifier;

    /**
     * 产品变更事件类型
     */
    public enum ProductChangeType {
        CREATE("create", "产品创建"),
        UPDATE("update", "产品更新"),
        DELETE("delete", "产品删除");

        private final String code;
        private final String desc;

        ProductChangeType(String code, String desc) {
            this.code = code;
            this.desc = desc;
        }

        public String getCode() {
            return code;
        }

        public String getDesc() {
            return desc;
        }
    }

    /**
     * 推送产品变更事件（异步）
     *
     * @param product    产品信息
     * @param changeType 变更类型
     */
    @Async
    public void pushProductChange(IoTProduct product, ProductChangeType changeType) {
        if (product == null) {
            return;
        }

        log.info("开始推送产品变更事件: productKey={}, changeType={}", 
                product.getProductKey(), changeType.getDesc());

        // 查询所有启用产品订阅的应用
        List<IoTUserApplication> subscribedApps = getSubscribedApplications();
        if (CollUtil.isEmpty(subscribedApps)) {
            log.debug("没有应用订阅产品变更，跳过推送");
            return;
        }

        // 构建推送消息
        JSONObject message = buildPushMessage(product, changeType);

        // 向所有订阅应用推送
        for (IoTUserApplication app : subscribedApps) {
            try {
                pushToApplication(app, message);
            } catch (Exception e) {
                log.error("推送产品变更到应用失败: appId={}, error={}", 
                        app.getAppId(), e.getMessage());
            }
        }

        log.info("产品变更事件推送完成: productKey={}, 推送应用数={}", 
                product.getProductKey(), subscribedApps.size());
    }

    /**
     * 获取订阅了产品变更的应用列表
     */
    private List<IoTUserApplication> getSubscribedApplications() {
        // 查询所有应用，过滤出启用了产品订阅的应用
        List<IoTUserApplication> allApps = applicationMapper.selectAll();
        
        return allApps.stream()
                .filter(this::isProductSubscriptionEnabled)
                .toList();
    }

    /**
     * 检查应用是否启用了产品订阅
     * 同时需要检查 HTTP 推送是否已配置并启用
     */
    private boolean isProductSubscriptionEnabled(IoTUserApplication app) {
        if (app == null || StrUtil.isBlank(app.getCfg())) {
            return false;
        }
        
        try {
            JSONObject cfg = JSONUtil.parseObj(app.getCfg());
            
            // 检查产品订阅是否开启
            JSONObject subscription = cfg.getJSONObject("subscription");
            if (subscription == null || !subscription.getBool("productEnabled", false)) {
                return false;
            }
            
            // 检查 HTTP 推送是否已配置并启用（产品订阅复用 HTTP 配置）
            JSONObject httpConfig = cfg.getJSONObject("http");
            if (httpConfig == null) {
                return false;
            }
            
            // HTTP 必须启用且配置了推送地址
            boolean httpEnabled = httpConfig.getBool("enable", false);
            String httpUrl = httpConfig.getStr("url");
            
            return httpEnabled && StrUtil.isNotBlank(httpUrl);
        } catch (Exception e) {
            log.warn("解析应用配置失败: appId={}", app.getAppId());
            return false;
        }
    }

    /**
     * 构建推送消息
     */
    private JSONObject buildPushMessage(IoTProduct product, ProductChangeType changeType) {
        JSONObject message = new JSONObject();
        message.set("eventType", "PRODUCT_CHANGE");
        message.set("changeType", changeType.getCode());
        message.set("timestamp", System.currentTimeMillis());
        
        // 产品基本信息
        JSONObject productInfo = new JSONObject();
        productInfo.set("productId", product.getProductId());
        productInfo.set("productKey", product.getProductKey());
        productInfo.set("name", product.getName());
        productInfo.set("deviceNode", product.getDeviceNode());
        productInfo.set("transportProtocol", product.getTransportProtocol());

        // 删除事件不包含完整信息，只包含标识
        if (changeType != ProductChangeType.DELETE) {
            productInfo.set("metadata", product.getMetadata());
            productInfo.set("thirdConfiguration", product.getThirdConfiguration());
        }
        
        message.set("product", productInfo);
        return message;
    }

    /**
     * 向单个应用推送消息（复用 HTTP 推送配置）
     */
    private void pushToApplication(IoTUserApplication app, JSONObject message) {
        String cfg = app.getCfg();
        if (StrUtil.isBlank(cfg)) {
            return;
        }

        JSONObject appCfg = JSONUtil.parseObj(cfg);
        
        // 从 HTTP 配置获取推送地址（产品订阅复用 HTTP 配置）
        JSONObject httpConfig = appCfg.getJSONObject("http");
        if (httpConfig == null) {
            return;
        }

        // 获取 HTTP 推送地址
        String pushUrl = httpConfig.getStr("url");
        if (StrUtil.isBlank(pushUrl)) {
            log.debug("应用未配置HTTP推送地址: appId={}", app.getAppId());
            return;
        }
        
        // 获取 HTTP 推送密钥
        String appSecret = httpConfig.getStr("appSecret");

        // 添加应用标识
        message.set("appId", app.getAppId());
        message.set("appUniqueId", app.getAppUniqueId());

        // HTTP推送
        try {
            HttpResponse response = HttpRequest.post(pushUrl)
                    .header("Content-Type", "application/json")
                    .header("X-App-Id", app.getAppId())
                    .header("X-App-Secret", StrUtil.isNotBlank(appSecret) ? appSecret : app.getAppSecret())
                    .body(message.toString())
                    .timeout(5000)
                    .execute();
            
            if (response.isOk()) {
                log.debug("推送产品变更成功: appId={}, url={}", app.getAppId(), pushUrl);
            } else {
                log.warn("推送产品变更失败: appId={}, status={}", app.getAppId(), response.getStatus());
            }
        } catch (Exception e) {
            log.error("HTTP推送失败: appId={}, url={}, error={}", 
                    app.getAppId(), pushUrl, e.getMessage());
        }
    }

    /**
     * 更新应用的产品订阅配置
     *
     * @param appUniqueId 应用唯一ID
     * @param enabled     是否启用（复用HTTP推送地址，不需要单独配置pushUrl）
     * @return 是否更新成功
     */
    public boolean updateProductSubscription(String appUniqueId, boolean enabled) {
        IoTUserApplication app = applicationMapper.selectOne(
                IoTUserApplication.builder().appUniqueId(appUniqueId).build());
        
        if (app == null) {
            log.warn("应用不存在: appUniqueId={}", appUniqueId);
            return false;
        }

        // 获取或创建配置
        String cfgStr = app.getCfg();
        JSONObject cfg = StrUtil.isBlank(cfgStr) ? new JSONObject() : JSONUtil.parseObj(cfgStr);
        
        // 获取或创建订阅配置
        JSONObject subscription = cfg.getJSONObject("subscription");
        if (subscription == null) {
            subscription = new JSONObject();
        }
        
        // 更新产品订阅配置（只保存 enabled 状态，推送地址复用 HTTP 配置）
        subscription.set("productEnabled", enabled);
        
        cfg.set("subscription", subscription);
        app.setCfg(cfg.toString());
        
        int result = applicationMapper.updateByPrimaryKeySelective(app);
        log.info("更新产品订阅配置: appUniqueId={}, enabled={}, result={}", 
                appUniqueId, enabled, result > 0);
        if (result > 0) {
            appPushConfigChangeNotifier.notifyAppConfigChanged(appUniqueId, "PRODUCT_SUBSCRIPTION_UPDATED");
        }
        
        return result > 0;
    }

    /**
     * 获取应用的产品订阅配置
     *
     * @param appUniqueId 应用唯一ID
     * @return 订阅配置（只包含 enabled 状态，推送地址复用 HTTP 配置）
     */
    public JSONObject getProductSubscription(String appUniqueId) {
        IoTUserApplication app = applicationMapper.selectOne(
                IoTUserApplication.builder().appUniqueId(appUniqueId).build());
        
        if (app == null || StrUtil.isBlank(app.getCfg())) {
            return new JSONObject().set("enabled", false);
        }

        try {
            JSONObject cfg = JSONUtil.parseObj(app.getCfg());
            JSONObject subscription = cfg.getJSONObject("subscription");
            if (subscription == null) {
                return new JSONObject().set("enabled", false);
            }
            
            return new JSONObject().set("enabled", subscription.getBool("productEnabled", false));
        } catch (Exception e) {
            log.warn("解析订阅配置失败: appUniqueId={}", appUniqueId);
            return new JSONObject().set("enabled", false);
        }
    }
}
