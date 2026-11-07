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

package cn.universal.admin.system.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.exceptions.ExceptionUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.spring.SpringUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.universal.admin.common.service.BaseServiceImpl;
import cn.universal.admin.platform.service.debug.DriverRevisionService;
import cn.universal.admin.system.service.IoTDeviceProtocolService;
import cn.universal.common.constant.IoTConstant.ProductFlushType;
import cn.universal.common.event.EventTopics;
import cn.universal.common.event.ProtocolUpdatedEvent;
import cn.universal.common.event.processer.EventPublisher;
import cn.universal.common.exception.IoTException;
import cn.universal.core.protocol.jar.JarDriverCodecService;
import cn.universal.core.protocol.jar.ProtocolCodecJar;
import cn.universal.core.protocol.jar.downloader.JarDownloader;
import cn.universal.core.protocol.jar.resolver.LocalJarLocationResolver;
import cn.universal.core.protocol.jscrtipt.JsMethodExtractor;
import cn.universal.core.protocol.jscrtipt.ProtocolCodecJscript;
import cn.universal.core.protocol.magic.MagicScriptMethodExtractor;
import cn.universal.core.protocol.magic.ProtocolCodecMagic;
import cn.universal.core.protocol.support.ProtocolCodecSupport.CodecMethod;
import cn.universal.dm.device.service.impl.IoTCacheRemoveService;
import cn.universal.ossm.service.ISysOssService;
import cn.universal.persistence.entity.IoTDeviceProtocol;
import cn.universal.persistence.entity.IoTProduct;
import cn.universal.persistence.entity.IoTUser;
import cn.universal.persistence.entity.bo.IoTDeviceProtocolBO;
import cn.universal.persistence.entity.vo.CodecBeanInfo;
import cn.universal.persistence.entity.vo.JarValidationResult;
import cn.universal.persistence.mapper.IoTDeviceProtocolMapper;
import cn.universal.persistence.mapper.IoTProductMapper;
import cn.universal.security.support.DeptDataPermissionSupport.DeptDataPermissionScope;
import com.github.pagehelper.PageHelper;
import jakarta.annotation.Resource;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * 设备协议Service业务层处理 @Author ruoyi
 *
 * @since 2023-01-06
 */
@Service
@Slf4j
public class IoTDeviceProtocolServiceImpl extends BaseServiceImpl
    implements IoTDeviceProtocolService {

  @Resource private IoTDeviceProtocolMapper ioTDeviceProtocolMapper;
  @Resource private DriverRevisionService driverRevisionService;
  @Resource private IoTProductMapper ioTProductMapper;
  @Resource private ISysOssService ossService;
  @Resource private IoTCacheRemoveService iotCacheRemoveService;
  @Resource private EventPublisher eventPublisher;
  @Resource private ApplicationEventPublisher applicationEventPublisher;
  @Resource private ApplicationContext applicationContext;

  @Override
  public int countProtocol(String id) {
    return ioTDeviceProtocolMapper.selectCount(IoTDeviceProtocol.builder().id(id).build());
  }

  /**
   * 查询设备协议
   *
   * @param id 设备协议主键
   * @return 设备协议
   */
  @Override
  public IoTDeviceProtocol selectDevProtocolById(String id, String unionId) {
    IoTDeviceProtocol protocol = ioTDeviceProtocolMapper.selectByPrimaryKey(id);
    if (protocol == null || StrUtil.isBlank(unionId)) {
      return protocol;
    }
    IoTUser currentUser = queryIotUser(unionId);
    if (currentUser == null) {
      return null;
    }
    return canAccessProtocol(protocol, currentUser) ? protocol : null;
  }

  @Override
  public int countByProvider(String provider) {
    return ioTDeviceProtocolMapper.countByProvider(provider);
  }

  /**
   * 查询设备协议列表
   *
   * @param ioTDeviceProtocol 设备协议
   * @return 设备协议
   */
  @Override
  @Transactional
  public List<IoTDeviceProtocol> selectDevProtocolList(
      IoTDeviceProtocol ioTDeviceProtocol, IoTUser iotUser) {
    String scopedUnionId = null;
    if (iotUser != null && !iotUser.isAdmin()) {
      ioTDeviceProtocol.setTenantId(resolveTenantId(iotUser.getUnionId()));
      scopedUnionId = resolveTenantOwnerUnionId(iotUser.getUnionId());
      applyDataPermissionScope(ioTDeviceProtocol, iotUser);
    }
    return ioTDeviceProtocolMapper.selectDevProtocolList(ioTDeviceProtocol, scopedUnionId);
  }

  /**
   * 新增设备协议
   *
   * @param ioTDeviceProtocol 设备协议
   * @return 结果
   */
  @Override
  public int insertDevProtocol(IoTDeviceProtocol ioTDeviceProtocol) {
    if (StrUtil.isBlank(ioTDeviceProtocol.getTenantId())) {
      ioTDeviceProtocol.setTenantId(resolveTenantId(currentTenantUser()));
    }
    int rows = ioTDeviceProtocolMapper.insertDevProtocol(ioTDeviceProtocol);
    if (rows > 0) {
      iotCacheRemoveService.removeDevProtocolCache(ioTDeviceProtocol.getId());
    }
    return rows;
  }

  @Override
  public int insertList(List<IoTDeviceProtocol> ioTDeviceProtocolList) {
    String tenantId = resolveTenantId(currentTenantUser());
    ioTDeviceProtocolList.forEach(
        protocol -> {
          if (StrUtil.isBlank(protocol.getTenantId())) {
            protocol.setTenantId(tenantId);
          }
        });
    int rows = ioTDeviceProtocolMapper.insertProtocolList(ioTDeviceProtocolList);
    if (rows > 0) {
      ioTDeviceProtocolList.forEach(
          protocol -> iotCacheRemoveService.removeDevProtocolCache(protocol.getId()));
    }
    return rows;
  }

  /**
   * 修改设备协议
   *
   * @param ioTDeviceProtocolBO 设备协议
   * @return 结果
   */
  @Override
  @Transactional
  public int updateDevProtocol(IoTDeviceProtocolBO ioTDeviceProtocolBO, IoTUser iotUser) {
    ioTDeviceProtocolMapper.lockForUpdate(ioTDeviceProtocolBO.getId());
    IoTDeviceProtocol ioTDeviceProtocol =
        BeanUtil.toBean(ioTDeviceProtocolBO, IoTDeviceProtocol.class);
    IoTDeviceProtocol oldIoTDeviceProtocol =
        requireProtocolAccess(ioTDeviceProtocol.getId(), iotUser, "该协议没有操作权限！");
    driverRevisionService.checkExpected(oldIoTDeviceProtocol, ioTDeviceProtocolBO.getExpectedSourceHash());
    ioTDeviceProtocol.setTenantId(
        StrUtil.blankToDefault(
            oldIoTDeviceProtocol.getTenantId(), resolveTenantId(iotUser.getUnionId())));
    JSONObject config = parseConfiguration(oldIoTDeviceProtocol.getConfiguration());
    String oldProviderCandidate = config.getStr("provider");
    String oldType = oldIoTDeviceProtocol.getType();
    if (StrUtil.isBlank(oldProviderCandidate)
        && ("magic".equals(oldType) || "jscript".equals(oldType))) {
      oldProviderCandidate = oldIoTDeviceProtocol.getId();
    }
    final String oldProvider = oldProviderCandidate;
    if ("jar".equals(ioTDeviceProtocolBO.getType())) {
      // 状态变更
      if (ioTDeviceProtocolBO.getUrl() == null) {
        if (iotUser.getIdentity() != 0) {
          throw new IoTException("请联系管理员审核插件后发布");
        }
        int rows = ioTDeviceProtocolMapper.updateDevProtocol(ioTDeviceProtocol);
        if (rows > 0) {
          iotCacheRemoveService.removeDevProtocolCache(ioTDeviceProtocol.getId());
        }
        return rows;

      } else {
        if (!ioTDeviceProtocolBO.getFileName().equals(config.getStr("provider"))) {
          ioTDeviceProtocol.setState((byte) 0);
        }
        if ("jscript".equals(oldIoTDeviceProtocol.getType())) {
          ioTDeviceProtocol.setState((byte) 0);
        }
      }
    }

    if ("jar".equals(oldIoTDeviceProtocol.getType())) {
      // 删除旧oss资源
      if (!config.getStr("location").equals(ioTDeviceProtocolBO.getUrl())) {
        //        JSONObject object1 = JSONUtil.parseObj(oldIoTDeviceProtocol.getConfiguration());
        String[] a = {config.getStr("location")};
        ossService.deleteWithValidByUrls(a, false);
        ioTDeviceProtocol.setState((byte) 0);
      }
    }
    JSONObject object = mergeConfigurations(config, parseConfiguration(ioTDeviceProtocolBO.getConfiguration()));
    String provider = oldIoTDeviceProtocol.getId();
    if ("jscript".equals(ioTDeviceProtocolBO.getType())) {
      provider = ioTDeviceProtocolBO.getId();
      if (ioTDeviceProtocolBO.getNeedBs4Decode() != null) {
        object.set("needBs4Decode", ioTDeviceProtocolBO.getNeedBs4Decode());
      }
      object.set("location", ioTDeviceProtocolBO.getJscript());
      object.set("provider", provider);
      Set<String> strings = JsMethodExtractor.extractMethodNames(ioTDeviceProtocolBO.getJscript());
      Collection<String> intersection = CollUtil.intersection(method, strings);
      if (CollUtil.isEmpty(intersection)) {
        throw new IoTException("预编译失败,没有包含任何一个有效方法");
      }
      object.set("supportMethods", intersection);

    } else if ("magic".equals(ioTDeviceProtocolBO.getType())) {
      Set<String> supportMethods;
      try {
        supportMethods =
            MagicScriptMethodExtractor.extractSupportMethods(
                ioTDeviceProtocolBO.getJscript(), method);
      } catch (RuntimeException e) {
        String message =
            StrUtil.blankToDefault(e.getMessage(), ExceptionUtil.getRootCauseMessage(e));
        throw new IoTException(message);
      }
      if (ioTDeviceProtocolBO.getNeedBs4Decode() != null) {
        object.set("needBs4Decode", ioTDeviceProtocolBO.getNeedBs4Decode());
      }
      object.set("location", ioTDeviceProtocolBO.getJscript());
      object.set("provider", provider);
      if (CollUtil.isEmpty(supportMethods)) {
        throw new IoTException("预编译失败,没有包含任何一个有效方法");
      }
      object.put("supportMethods", supportMethods);

    } else if ("spring".equals(ioTDeviceProtocolBO.getType())) {
      // Spring Bean类型
      provider = ioTDeviceProtocolBO.getBeanName();
      object.set("location", ioTDeviceProtocolBO.getBeanName());
      object.set("provider", ioTDeviceProtocolBO.getBeanName());
      // 校验Bean并获取supportMethods
      JarValidationResult validation =
          validateJarMethods(ioTDeviceProtocolBO.getBeanName(), "spring");
      if (!validation.getSuccess()) {
        throw new IoTException("校验Bean失败: " + validation.getErrorMessage());
      }
      object.set("supportMethods", validation.getSupportMethods());
      ioTDeviceProtocol.setState((byte) 1); // Spring Bean默认已发布
    } else {
      // jar类型
      provider = ioTDeviceProtocolBO.getFileName();
      object.set("provider", ioTDeviceProtocolBO.getFileName());
      object.set("location", ioTDeviceProtocolBO.getUrl());
      // 校验Jar包并获取supportMethods，传入provider作为类名
      JarValidationResult jarValidation =
          validateJarMethods(
              ioTDeviceProtocolBO.getUrl(), "jar", ioTDeviceProtocolBO.getFileName());
      if (!jarValidation.getSuccess()) {
        throw new IoTException("校验Jar包失败: " + jarValidation.getErrorMessage());
      }
      object.set("supportMethods", jarValidation.getSupportMethods());
    }
    if ("magic".equals(oldType) && "magic".equals(ioTDeviceProtocolBO.getType())) {
      driverRevisionService.recordChange(oldIoTDeviceProtocol, ioTDeviceProtocolBO.getJscript(),
          StrUtil.blankToDefault(ioTDeviceProtocolBO.getVersion(), oldIoTDeviceProtocol.getVersion()),
          iotUser.getUnionId());
    }
    ioTDeviceProtocol.setConfiguration(object.toString());
    int rows = ioTDeviceProtocolMapper.updateDevProtocol(ioTDeviceProtocol);
    if (rows > 0) {
      JSONObject object1 = new JSONObject();
      object1.set("type", ProductFlushType.script.name());
      object1.set("provider", provider);
      String customType = ioTDeviceProtocolBO.getType();
      String newProvider = provider;
      String newType = customType;
      object1.set("customType", customType);
      object1.set("productKey", ioTDeviceProtocol.getId());
      object1.set("oldProvider", oldProvider);
      object1.set("oldCustomType", oldType);

      // 仅失效当前 productKey 的协议定义，并在事务提交后通知所有节点清理脚本缓存。
      iotCacheRemoveService.removeDevProtocolCache(ioTDeviceProtocol.getId());
      runAfterCommit(
          () -> {
            removeCodecProvider(oldProvider, oldType);
            removeCodecProvider(newProvider, newType);
            eventPublisher.publishEvent(EventTopics.PROTOCOL_UPDATED, object1);
            applicationEventPublisher.publishEvent(
                new ProtocolUpdatedEvent(this, newProvider, newType));
          });
    }
    return rows;
  }

  private void removeCodecProvider(String provider, String type) {
    if (StrUtil.isBlank(provider) || StrUtil.isBlank(type)) {
      return;
    }
    switch (type) {
      case "jscript" -> ProtocolCodecJscript.getInstance().remove(provider);
      case "magic" -> ProtocolCodecMagic.getInstance().remove(provider);
      case "jar" -> ProtocolCodecJar.getInstance().remove(provider);
      default -> {
        // Spring Bean 编解码器由 Spring 容器管理，不存在脚本缓存需要清理。
      }
    }
  }

  private void runAfterCommit(Runnable action) {
    if (TransactionSynchronizationManager.isActualTransactionActive()
        && TransactionSynchronizationManager.isSynchronizationActive()) {
      TransactionSynchronizationManager.registerSynchronization(
          new TransactionSynchronization() {
            @Override
            public void afterCommit() {
              action.run();
            }
          });
    } else {
      action.run();
    }
  }

  /**
   * 批量删除设备协议
   *
   * @param ids 需要删除的设备协议主键
   * @return 结果
   */
  @Override
  @Transactional
  public int deleteDevProtocolByIds(String[] ids) {
    IoTUser currentUser = requireCurrentUser();
    List<IoTDeviceProtocol> list = new ArrayList<>();
    Set<String> deleteIds = new HashSet<>();
    for (String id : ids) {
      IoTDeviceProtocol protocol = ioTDeviceProtocolMapper.selectByPrimaryKey(id);
      if (protocol == null) {
        continue;
      }
      if (!canAccessProtocol(protocol, currentUser)) {
        throw new IoTException("该协议没有操作权限！");
      }
      if (deleteIds.add(protocol.getId())) {
        list.add(protocol);
      }
    }
    if (list.isEmpty()) {
      return 0;
    }
    String[] a =
        list.stream()
            .filter(s -> "jar".equals(s.getType()))
            .map(
                i -> {
                  JSONObject object1 = JSONUtil.parseObj(i.getConfiguration());
                  return object1.getStr("location");
                })
            .toArray(String[]::new);
    ossService.deleteWithValidByUrls(a, false);
    list.forEach(protocol -> driverRevisionService.delete(protocol.getId()));
    int rs =
        ioTDeviceProtocolMapper.deleteDevProtocolByIds(deleteIds.toArray(new String[0]));
    // 删除编解码缓存
    list.stream()
        .forEach(
            s -> {
              String provider = null;
              if ("jscript".equals(s.getType())) {
                provider = s.getId();
                ProtocolCodecJscript.getInstance().remove(provider);
              } else if ("magic".equals(s.getType())) {
                provider = s.getId();
                ProtocolCodecMagic.getInstance().remove(provider);
              } else {
                provider = s.toDefinition().getProvider();
                ProtocolCodecJar.getInstance().remove(provider);
              }
            });
    if (rs > 0) {
      list.forEach(protocol -> iotCacheRemoveService.removeDevProtocolCache(protocol.getId()));
    }
    return rs;
  }

  /**
   * 删除设备协议信息
   *
   * @param id 设备协议主键
   * @return 结果
   */
  @Override
  public int deleteDevProtocolById(String id) {
    return deleteDevProtocolByIds(new String[] {id});
  }

  /**
   * 查询未创建协议的产品列表（归属人为当前用户）
   *
   * @param unionId 用户unionId
   * @param searchKey 搜索关键词（产品名称或productKey）
   * @param pageNum 页码
   * @param pageSize 每页数量
   * @return 产品分页列表
   */
  @Override
  public List<IoTProduct> selectProductsWithoutProtocol(
      String unionId, String searchKey, int pageNum, int pageSize) {
    IoTUser currentUser =
        StrUtil.isBlank(unionId) ? currentTenantUser() : queryIotUser(unionId);
    String scopedUnionId =
        currentUser == null || currentUser.isAdmin()
            ? null
            : resolveTenantOwnerUnionId(currentUser.getUnionId());
    String tenantId =
        currentUser == null || currentUser.isAdmin() ? null : resolveTenantId(currentUser);
    IoTProduct scope = new IoTProduct();
    DeptDataPermissionScope permissionScope = resolveDataPermissionScope(currentUser);
    if (permissionScope != null
        && !permissionScope.isSuperAdmin()
        && !permissionScope.isTenantAdmin()
        && !permissionScope.isAll()) {
      scope.setScopeDeptIds(new ArrayList<>(permissionScope.getScopeDeptIds()));
      scope.setScopeUnionIds(new ArrayList<>(permissionScope.getScopeUnionIds()));
      scope.setScopeDenied(permissionScope.isDenied());
    }
    PageHelper.startPage(pageNum, pageSize);
    return ioTDeviceProtocolMapper.selectProductsWithoutProtocol(
        scopedUnionId, tenantId, searchKey, scope);
  }

  /**
   * 获取所有可用的Codec Bean列表
   *
   * @return Bean列表
   */
  @Override
  public List<CodecBeanInfo> getAvailableCodecBeans() {
    try {
      // 1. 从Spring容器获取所有JarDriverCodecService类型的Bean
      Map<String, JarDriverCodecService> beans =
          applicationContext.getBeansOfType(JarDriverCodecService.class);
      log.info("扫描到 {} 个JarDriverCodecService Bean", beans.size());

      // 2. 查询数据库已注册的spring类型Bean
      IoTDeviceProtocol query = IoTDeviceProtocol.builder().type("spring").build();
      List<IoTDeviceProtocol> registeredProtocols =
          ioTDeviceProtocolMapper.selectDevProtocolList(query, null);
      Set<String> registeredBeans =
          registeredProtocols.stream()
              .map(
                  p -> {
                    try {
                      JSONObject config = JSONUtil.parseObj(p.getConfiguration());
                      return config.getStr("location");
                    } catch (Exception e) {
                      return null;
                    }
                  })
              .filter(s -> s != null)
              .collect(Collectors.toSet());

      log.info("数据库中已注册 {} 个spring类型Bean", registeredBeans.size());

      // 3. 组装返回数据
      List<CodecBeanInfo> result = new ArrayList<>();
      beans.forEach(
          (beanName, service) -> {
            try {
              CodecBeanInfo info =
                  CodecBeanInfo.builder()
                      .beanName(beanName)
                      .productKey(service.productKey())
                      .className(service.getClass().getName())
                      .methods(detectBeanMethods(service))
                      .isRegistered(registeredBeans.contains(beanName))
                      .description("Spring Bean")
                      .build();
              result.add(info);
              log.debug(
                  "Bean: {}, productKey: {}, registered: {}",
                  beanName,
                  service.productKey(),
                  info.getIsRegistered());
            } catch (Exception e) {
              log.error("处理Bean {} 时出错: {}", beanName, e.getMessage());
            }
          });

      log.info("返回 {} 个Bean信息", result.size());
      return result;
    } catch (Exception e) {
      log.error("获取Codec Bean列表失败", e);
      return new ArrayList<>();
    }
  }

  /**
   * 校验Jar包或Bean的方法有效性
   *
   * @param location Jar包URL或Bean名称
   * @param type 类型: jar/spring
   * @return 校验结果
   */
  @Override
  public JarValidationResult validateJarMethods(String location, String type) {
    return validateJarMethods(location, type, null);
  }

  /**
   * 校验Jar包或Bean的方法有效性（带类名）
   *
   * @param location Jar包URL或Bean名称
   * @param type 类型: jar/spring
   * @param providedClassName 用户提供的类名（可选）
   * @return 校验结果
   */
  @Override
  public JarValidationResult validateJarMethods(
      String location, String type, String providedClassName) {
    try {
      List<String> supportMethods = new ArrayList<>();
      String className = "";

      if ("spring".equals(type)) {
        // Bean类型，从Spring容器获取
        try {
          Object bean = SpringUtil.getBean(location);
          className = bean.getClass().getName();
          supportMethods = detectBeanMethods(bean);
        } catch (Exception e) {
          log.error("Bean {} 不存在: {}", location, e.getMessage());
          return JarValidationResult.builder()
              .success(false)
              .errorMessage("Bean不存在: " + location)
              .supportMethods(new ArrayList<>())
              .build();
        }
      } else if ("jar".equals(type)) {
        // Jar类型，下载并加载
        try {
          String localPath = location;
          // 如果是远程URL，先下载
          if (location.startsWith("http://") || location.startsWith("https://")) {
            JarDownloader downloader = new JarDownloader();
            localPath = downloader.download(location);
          }

          // 确定要使用的类名：优先使用用户提供的，否则扫描JAR包
          String actualClassName = providedClassName;
          if (actualClassName == null || actualClassName.trim().isEmpty()) {
            log.info("用户未提供类名，开始扫描JAR包...");
            actualClassName = findCodecClassInJar(localPath);
            if (actualClassName == null) {
              return JarValidationResult.builder()
                  .success(false)
                  .errorMessage("JAR包中未找到实现JarDriverCodecService接口的类")
                  .supportMethods(new ArrayList<>())
                  .build();
            }
          } else {
            log.info("使用用户提供的类名: {}", actualClassName);
          }

          // 使用确定的类名加载
          LocalJarLocationResolver resolver = new LocalJarLocationResolver();
          Object codecInstance = resolver.resolve(localPath, actualClassName);
          className = codecInstance.getClass().getName();
          supportMethods = detectJarMethods(codecInstance);

        } catch (Exception e) {
          log.error("Jar {} 校验失败: {}", location, e.getMessage(), e);
          return JarValidationResult.builder()
              .success(false)
              .errorMessage("Jar包加载失败: " + e.getMessage())
              .supportMethods(new ArrayList<>())
              .build();
        }
      } else {
        return JarValidationResult.builder()
            .success(false)
            .errorMessage("不支持的类型: " + type)
            .supportMethods(new ArrayList<>())
            .build();
      }

      boolean success = !supportMethods.isEmpty();
      return JarValidationResult.builder()
          .success(success)
          .supportMethods(supportMethods)
          .className(className)
          .errorMessage(success ? null : "未检测到有效的编解码方法")
          .build();

    } catch (Exception e) {
      log.error("校验 {} 时发生异常: {}", location, e.getMessage(), e);
      return JarValidationResult.builder()
          .success(false)
          .errorMessage("校验失败: " + e.getMessage())
          .supportMethods(new ArrayList<>())
          .build();
    }
  }

  /**
   * 从DJAR包中扫描找到实现JarDriverCodecService接口的类
   *
   * @param jarPath JAR文件路径
   * @return 实现类的全限定名，如果未找到则返回null
   */
  private String findCodecClassInJar(String jarPath) {
    try {
      java.io.File jarFile = new java.io.File(jarPath);
      if (!jarFile.exists()) {
        log.error("JAR文件不存在: {}", jarPath);
        return null;
      }

      // 创建类加载器
      java.net.URL jarUrl = jarFile.toURI().toURL();
      java.net.URLClassLoader tempLoader =
          new java.net.URLClassLoader(
              new java.net.URL[] {jarUrl}, this.getClass().getClassLoader());

      // 扫描JAR包中的所有class文件
      try (java.util.jar.JarFile jar = new java.util.jar.JarFile(jarFile)) {
        java.util.Enumeration<java.util.jar.JarEntry> entries = jar.entries();

        while (entries.hasMoreElements()) {
          java.util.jar.JarEntry entry = entries.nextElement();
          String entryName = entry.getName();

          // 只处理.class文件，跳过内部类
          if (entryName.endsWith(".class") && !entryName.contains("$")) {
            // 转换为类名
            String className = entryName.replace("/", ".").replace(".class", "");

            try {
              // 尝试加载类
              Class<?> clazz = tempLoader.loadClass(className);

              // 检查是否实现了JarDriverCodecService接口
              if (cn.universal.core.protocol.jar.JarDriverCodecService.class.isAssignableFrom(
                  clazz)) {
                log.info("在JAR包中找到实现类: {}", className);
                tempLoader.close();
                return className;
              }
            } catch (ClassNotFoundException | NoClassDefFoundError e) {
              // 类加载失败，继续扫描
              log.debug("无法加载类 {}: {}", className, e.getMessage());
            }
          }
        }
      }

      tempLoader.close();
      log.warn("JAR包中未找到实现JarDriverCodecService接口的类: {}", jarPath);
      return null;

    } catch (Exception e) {
      log.error("扫描JAR包失败: {}", jarPath, e);
      return null;
    }
  }

  /**
   * 检测Bean支持的方法
   *
   * @param bean Bean实例
   * @return 支持的方法列表
   */
  private List<String> detectBeanMethods(Object bean) {
    List<String> methods = new ArrayList<>();
    Class<?> clazz = bean.getClass();

    // 检查所有CodecMethod
    for (CodecMethod codecMethod : CodecMethod.values()) {
      try {
        Method method = clazz.getMethod(codecMethod.name(), String.class, Object.class);
        if (method != null && method.getReturnType() == String.class) {
          methods.add(codecMethod.name());
        }
      } catch (NoSuchMethodException e) {
        // 方法不存在，跳过
      } catch (Exception e) {
        log.warn("检测方法 {} 时出错: {}", codecMethod.name(), e.getMessage());
      }
    }

    return methods;
  }

  /**
   * 检测Jar包支持的方法
   *
   * @param codecInstance Jar实例
   * @return 支持的方法列表
   */
  private List<String> detectJarMethods(Object codecInstance) {
    // Jar包和Bean的检测逻辑相同
    return detectBeanMethods(codecInstance);
  }

  private IoTUser requireCurrentUser() {
    IoTUser currentUser = currentTenantUser();
    if (currentUser == null || StrUtil.isBlank(currentUser.getUnionId())) {
      throw new IoTException("获取当前用户失败，请重新登录后重试");
    }
    return currentUser;
  }

  private IoTDeviceProtocol requireProtocolAccess(String id, IoTUser currentUser, String message) {
    IoTDeviceProtocol protocol = ioTDeviceProtocolMapper.selectByPrimaryKey(id);
    if (protocol == null) {
      throw new IoTException("协议不存在或已删除");
    }
    if (!canAccessProtocol(protocol, currentUser)) {
      throw new IoTException(message);
    }
    return protocol;
  }

  private boolean canAccessProtocol(IoTDeviceProtocol protocol, IoTUser currentUser) {
    if (protocol == null || currentUser == null) {
      return false;
    }
    if (currentUser.isAdmin()) {
      return true;
    }
    IoTProduct product =
        ioTProductMapper.selectOne(IoTProduct.builder().productKey(protocol.getId()).build());
    String resourceTenantId =
        StrUtil.blankToDefault(protocol.getTenantId(), product == null ? null : product.getTenantId());
    String creatorId = product == null ? null : product.getCreatorId();
    Long deptId = resolveCreatorDeptId(creatorId);
    return canAccessDeptResource(resourceTenantId, deptId, creatorId, currentUser);
  }

  private Long resolveCreatorDeptId(String creatorId) {
    if (StrUtil.isBlank(creatorId)) {
      return null;
    }
    IoTUser creator = queryIotUser(creatorId);
    return creator == null ? null : creator.getDeptId();
  }

  private JSONObject parseConfiguration(String configuration) {
    if (StrUtil.isBlank(configuration)) {
      return new JSONObject();
    }
    try {
      return JSONUtil.parseObj(configuration);
    } catch (Exception e) {
      log.warn("协议扩展配置解析失败，将忽略非法配置: {}", configuration, e);
      return new JSONObject();
    }
  }

  private JSONObject mergeConfigurations(JSONObject... configurations) {
    JSONObject merged = new JSONObject();
    if (configurations == null) {
      return merged;
    }
    for (JSONObject configuration : configurations) {
      if (configuration != null) {
        merged.putAll(configuration);
      }
    }
    return merged;
  }
}
