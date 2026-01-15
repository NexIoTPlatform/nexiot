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

package cn.universal.databridge.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import cn.hutool.json.JSONUtil;
import cn.universal.databridge.entity.DataBridgeConfig;
import cn.universal.databridge.entity.ResourceConnection;
import cn.universal.databridge.exception.DataBridgeException;
import cn.universal.databridge.logger.DataBridgeLogger;
import cn.universal.databridge.mapper.DataBridgeConfigMapper;
import cn.universal.databridge.plugin.DataBridgePlugin;
import cn.universal.databridge.util.ConfigValidator;
import cn.universal.databridge.vo.DataBridgeConfigVO;
import cn.universal.persistence.entity.IoTDevice;
import cn.universal.persistence.entity.IoTUserApplication;
import cn.universal.persistence.entity.bo.IoTProductBO;
import cn.universal.persistence.mapper.IoTDeviceMapper;
import cn.universal.persistence.mapper.IoTProductMapper;
import cn.universal.persistence.mapper.IoTUserApplicationMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;

/**
 * 数据桥接配置服务
 *
 * @author gitee.com/NexIoT
 * @version 1.0
 * @since 2025/1/15
 */
@Service
@Slf4j
public class DataBridgeConfigService {

    @Resource
    private Map<String, DataBridgePlugin> bridgePlugins;

    @Resource
    private DataBridgeLogger dataBridgeLogger;

    @Resource
    private DataBridgeConfigMapper dataBridgeConfigMapper;

    @Resource
    private IoTProductMapper ioTProductMapper;

    @Resource
    private IoTUserApplicationMapper ioTUserApplicationMapper;

    @Resource
    private ResourceConnectionService resourceConnectionService;

    @Resource
    private IoTDeviceMapper ioTDeviceMapper;

    /**
     * 创建桥接配置
     */
    @Transactional(rollbackFor = Exception.class)
    public Long createDataBridgeConfig(DataBridgeConfig config) {
        // 预处理：反解码可能被 XSS 过滤器转义的脚本
        if (config.getMagicScript() != null) {
            config.setMagicScript(decodeHtmlEntities(config.getMagicScript()));
        }
        // 1. 验证配置
        ConfigValidator.validateDataBridgeConfig(config);
        if (!validateConfig(config)) {
            throw new DataBridgeException("CONFIG_VALIDATION_FAILED", "桥接配置验证失败");
        }

        // 2. 检查重名
        if (isNameExists(config.getName(), null)) {
            throw new RuntimeException("桥接配置名称已存在");
        }

        // 3. 设置默认值
        if (config.getStatus() == null) {
            config.setStatus(1); // 默认启用
        }
        config.setCreateTime(LocalDateTime.now());
        config.setUpdateTime(LocalDateTime.now());

        // 4. 保存配置
        int result = dataBridgeConfigMapper.insertSelective(config);
        if (result <= 0) {
            throw new RuntimeException("创建桥接配置失败");
        }

        log.info("创建数据桥接配置成功，名称: {}, ID: {}", config.getName(), config.getId());
        return config.getId();
    }

    /**
     * 验证配置
     */
    public Boolean validateConfig(DataBridgeConfig config) {
        DataBridgePlugin plugin = bridgePlugins.get(config.getBridgeType().name());
        if (plugin == null) {
            return false;
        }

        return plugin.validateConfig(config);
    }

    /**
     * 根据源范围获取配置列表
     */
    public List<DataBridgeConfig> getConfigsBySourceScope(DataBridgeConfig.SourceScope sourceScope) {
        DataBridgeConfig condition = new DataBridgeConfig();
        condition.setSourceScope(sourceScope);
        condition.setStatus(1); // 只查询启用的配置
        return dataBridgeConfigMapper.select(condition);
    }

    /**
     * 获取产品的活跃配置
     */
    public List<DataBridgeConfig> getActiveConfigsByProductKey(String productKey) {
        if (productKey == null || productKey.trim().isEmpty()) {
            throw new IllegalArgumentException("产品KEY不能为空");
        }
        return dataBridgeConfigMapper.selectActiveConfigsByProductKey(productKey);
    }

    public List<DataBridgeConfig> getActiveConfigsByProductKeyAndDeviceId(String productKey, String deviceId) {
        IoTDevice ioTDevice = ioTDeviceMapper.selectIoTDevice(productKey, deviceId);
        if (ioTDevice == null) {
            return null;
        }
        return dataBridgeConfigMapper.selectActiveConfigsByApplication(ioTDevice.getApplication());
    }

    /**
     * 获取所有配置
     */
    public List<DataBridgeConfig> getAllConfigs() {
        return dataBridgeConfigMapper.selectAll();
    }

    /**
     * 根据创建者获取配置列表
     */
    public List<DataBridgeConfig> getConfigsByCreateBy(String createBy) {
        if (createBy == null || createBy.trim().isEmpty()) {
            throw new IllegalArgumentException("创建者不能为空");
        }
        return dataBridgeConfigMapper.selectByCreateBy(createBy);
    }

    /**
     * 获取所有配置的VO列表
     */
    public List<DataBridgeConfigVO> getAllConfigVOs() {
        List<DataBridgeConfig> configs = dataBridgeConfigMapper.selectAll();
        return convertToVOs(configs);
    }

    /**
     * 根据创建者获取配置VO列表
     */
    public List<DataBridgeConfigVO> getConfigVOsByCreateBy(String createBy) {
        if (createBy == null || createBy.trim().isEmpty()) {
            throw new IllegalArgumentException("创建者不能为空");
        }
        List<DataBridgeConfig> configs = dataBridgeConfigMapper.selectByCreateBy(createBy);
        return convertToVOs(configs);
    }

    /**
     * 将Entity转换为VO
     */
    private List<DataBridgeConfigVO> convertToVOs(List<DataBridgeConfig> configs) {
        return configs.stream().map(this::convertToVO).collect(Collectors.toList());
    }

    /**
     * 将单个Entity转换为VO
     */
    private DataBridgeConfigVO convertToVO(DataBridgeConfig config) {
        return DataBridgeConfigVO.builder()
                .id(config.getId())
                .name(config.getName())
                .sourceScope(config.getSourceScope())
                .sourceProductKeys(config.getSourceProductKeys())
                .sourceProductNames(parseSourceProductNames(config.getSourceProductKeys()))
                .sourceApplicationId(config.getSourceApplicationId())
                .sourceApplicationName(getSourceApplicationName(config.getSourceApplicationId()))
                .targetResourceId(config.getTargetResourceId())
                .targetResourceName(getTargetResourceName(config.getTargetResourceId()))
                .bridgeType(config.getBridgeType())
                .template(config.getTemplate())
                .magicScript(config.getMagicScript())
                .config(config.getConfig())
                .status(config.getStatus())
                .description(config.getDescription())
                .createBy(config.getCreateBy())
                .createTime(config.getCreateTime())
                .updateBy(config.getUpdateBy())
                .updateTime(config.getUpdateTime())
                .build();
    }

    /**
     * 解析源产品名称列表
     */
    private List<IoTProductBO> parseSourceProductNames(String sourceProductKeys) {
        if (sourceProductKeys == null || sourceProductKeys.trim().isEmpty()) {
            return List.of();
        }
        try {
            List<String> cleanProductKeys = new ArrayList<>();
            // 尝试解析JSON数组格式
            if (JSONUtil.isTypeJSONArray(sourceProductKeys.trim())) {
                // 使用Hutool解析JSON数组
                List<String> productKeys = JSONUtil.toList(sourceProductKeys, String.class);
                for (String productKey : productKeys) {
                    if (productKey != null && !productKey.trim().isEmpty()) {
                        cleanProductKeys.add(productKey.trim());
                    }
                }
            } else {
                // 兼容旧的逗号分隔格式
                String[] productKeys = sourceProductKeys.split(",");
                for (String productKey : productKeys) {
                    String trimmedKey = productKey.trim();
                    if (!trimmedKey.isEmpty()) {
                        cleanProductKeys.add(trimmedKey);
                    }
                }
            }

            if (cleanProductKeys.isEmpty()) {
                return List.of();
            }
            // 批量查询产品信息
            List<IoTProductBO> products =
                    ioTProductMapper.selectProductNameByProductKeys(cleanProductKeys);
            return products;
        } catch (Exception e) {
            log.warn("解析源产品名称失败: {}", e.getMessage());
            return List.of();
        }
    }

    /**
     * 获取源应用名称
     */
    private String getSourceApplicationName(Long sourceApplicationId) {
        if (sourceApplicationId == null) {
            return null;
        }
        try {
            // 将Long类型的ID转换为String类型
            String appUniqueId = String.valueOf(sourceApplicationId);
            IoTUserApplication application =
                    ioTUserApplicationMapper.selectIotUserApplicationById(appUniqueId);
            if (application != null && application.getAppName() != null) {
                return application.getAppName();
            } else {
                // 如果查询不到应用名称，使用ID作为降级方案
                return "应用" + sourceApplicationId;
            }
        } catch (Exception e) {
            log.warn("查询应用名称失败，ApplicationId: {}, 错误: {}", sourceApplicationId, e.getMessage());
            // 查询失败时使用ID作为降级方案
            return "应用" + sourceApplicationId;
        }
    }

    /**
     * 获取目标资源名称
     */
    private String getTargetResourceName(Long targetResourceId) {
        if (targetResourceId == null) {
            return null;
        }
        try {
            // 使用ResourceConnectionService查询资源名称
            ResourceConnection resource = resourceConnectionService.getById(targetResourceId);
            if (resource != null && resource.getName() != null) {
                return resource.getName();
            } else {
                // 如果查询不到资源名称，使用ID作为降级方案
                return "资源" + targetResourceId;
            }
        } catch (Exception e) {
            log.warn("查询资源名称失败，ResourceId: {}, 错误: {}", targetResourceId, e.getMessage());
            // 查询失败时使用ID作为降级方案
            return "资源" + targetResourceId;
        }
    }

    /**
     * 更新配置状态
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateConfigStatus(Long id, Integer status, String updateBy) {
        if (id == null) {
            throw new IllegalArgumentException("配置ID不能为空");
        }
        if (status == null || (status != 0 && status != 1)) {
            throw new IllegalArgumentException("状态值无效，只能为0或1");
        }

        DataBridgeConfig config = new DataBridgeConfig();
        config.setId(id);
        config.setStatus(status);
        config.setUpdateBy(updateBy);
        config.setUpdateTime(LocalDateTime.now());

        int result = dataBridgeConfigMapper.updateByPrimaryKeySelective(config);
        if (result <= 0) {
            throw new RuntimeException("更新桥接配置状态失败");
        }

        log.info("更新桥接配置状态，ID: {}, 状态: {}", id, status);
    }

    /**
     * 删除配置
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteConfig(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("配置ID不能为空");
        }

        // 检查配置是否存在
        DataBridgeConfig existing = dataBridgeConfigMapper.selectByPrimaryKey(id);
        if (existing == null) {
            throw new RuntimeException("桥接配置不存在");
        }

        int result = dataBridgeConfigMapper.deleteByPrimaryKey(id);
        if (result <= 0) {
            throw new RuntimeException("删除桥接配置失败");
        }

        log.info("删除桥接配置，ID: {}", id);
    }

    /**
     * 根据ID获取配置
     */
    public DataBridgeConfig getById(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("配置ID不能为空");
        }
        return dataBridgeConfigMapper.selectByPrimaryKey(id);
    }

    /**
     * 更新桥接配置
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateDataBridgeConfig(DataBridgeConfig config) {
        // 预处理：反解码可能被 XSS 过滤器转义的脚本
        if (config.getMagicScript() != null) {
            config.setMagicScript(decodeHtmlEntities(config.getMagicScript()));
        }
        // 1. 验证配置
        if (!validateConfig(config)) {
            // throw new RuntimeException("桥接配置验证失败");
        }

        // 2. 检查重名
        if (isNameExists(config.getName(), config.getId())) {
            throw new RuntimeException("桥接配置名称已存在");
        }

        // 3. 设置更新时间
        config.setUpdateTime(LocalDateTime.now());

        // 4. 更新配置
        int result = dataBridgeConfigMapper.updateByPrimaryKeySelective(config);
        if (result <= 0) {
            throw new RuntimeException("更新桥接配置失败");
        }

        log.info("更新数据桥接配置成功，ID: {}", config.getId());
    }

    /**
     * 根据桥接类型获取配置
     */
    public List<DataBridgeConfig> getConfigsByBridgeType(DataBridgeConfig.BridgeType bridgeType) {
        if (bridgeType == null) {
            throw new IllegalArgumentException("桥接类型不能为空");
        }
        return dataBridgeConfigMapper.selectConfigsByBridgeType(bridgeType.name());
    }

    /**
     * 根据目标资源ID获取配置
     */
    public List<DataBridgeConfig> getConfigsByTargetResourceId(Long targetResourceId) {
        if (targetResourceId == null) {
            throw new IllegalArgumentException("目标资源ID不能为空");
        }
        return dataBridgeConfigMapper.selectConfigsByTargetResourceId(targetResourceId);
    }

    /**
     * 批量更新配置状态
     */
    @Transactional(rollbackFor = Exception.class)
    public void batchUpdateConfigStatus(List<Long> ids, Integer status, String updateBy) {
        if (ids == null || ids.isEmpty()) {
            throw new IllegalArgumentException("配置ID列表不能为空");
        }
        if (status == null || (status != 0 && status != 1)) {
            throw new IllegalArgumentException("状态值无效，只能为0或1");
        }

        int result = dataBridgeConfigMapper.batchUpdateStatus(ids, status, updateBy);
        if (result <= 0) {
            throw new RuntimeException("批量更新桥接配置状态失败");
        }

        log.info("批量更新桥接配置状态，数量: {}, 状态: {}", result, status);
    }

    /**
     * 检查名称是否存在
     */
    private boolean isNameExists(String name, Long excludeId) {
        DataBridgeConfig existing = dataBridgeConfigMapper.selectByName(name, excludeId);
        return existing != null;
    }

    /**
     * 验证配置的基本字段
     */
    public boolean validateBasicFields(DataBridgeConfig config) {
        if (config == null) {
            return false;
        }

        // 基本字段验证
        if (config.getName() == null || config.getName().trim().isEmpty()) {
            return false;
        }

        if (config.getSourceScope() == null) {
            return false;
        }

        if (config.getTargetResourceId() == null) {
            return false;
        }

        if (config.getBridgeType() == null) {
            return false;
        }

        if (config.getTemplate() == null || config.getTemplate().trim().isEmpty()) {
            return false;
        }

        return true;
    }

    /**
     * 反解码常见 HTML 实体，避免脚本符号被转义（例如 =&gt; ）
     */
    private String decodeHtmlEntities(String text) {
        if (text == null) return null;
        String cur = text;
        // 最多双轮，处理 &amp;lt; 这类嵌套实体
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
