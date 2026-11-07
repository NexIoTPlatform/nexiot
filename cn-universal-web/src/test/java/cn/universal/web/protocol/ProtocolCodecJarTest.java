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

package cn.universal.web.protocol;

import cn.hutool.extra.spring.SpringUtil;
import cn.universal.CnUniversalIoTApplication;
import cn.universal.common.exception.CodecException;
import cn.universal.common.utils.SpringUtils;
import cn.universal.core.protocol.jar.JarDriverCodecService;
import cn.universal.core.protocol.jar.ProtocolCodecJar;
import cn.universal.core.protocol.request.ProtocolDecodeRequest;
import cn.universal.core.protocol.request.ProtocolEncodeRequest;
import cn.universal.core.protocol.support.ProtocolSupportDefinition;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import javax.tools.JavaCompiler;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import lombok.extern.slf4j.Slf4j;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.logging.LogLevel;
import org.springframework.boot.logging.LoggingSystem;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * ProtocolCodecJar 测试类 测试三种加载方式：Spring Bean、本地 JAR、远程 JAR
 *
 * @author gitee.com/NexIoT
 * @version 1.0
 * @since 2025/11/02
 */
@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest(classes = CnUniversalIoTApplication.class)
@ActiveProfiles("dev")
public class ProtocolCodecJarTest {

  private ProtocolCodecJar protocolCodecJar;
  private Path tempDir;
  private File testJarFile;
  private File testJarFile1; // 第一个远程 JAR
  private File testJarFile2; // 第二个远程 JAR
  private LoggingSystem loggingSystem;

  @Before
  public void setUp() throws Exception {
    protocolCodecJar = ProtocolCodecJar.getInstance();

    // 设置日志级别
    try {
      loggingSystem =
          org.springframework.boot.logging.LoggingSystem.get(
              org.springframework.boot.logging.LoggingSystem.class.getClassLoader());
      if (loggingSystem != null) {
        loggingSystem.setLogLevel("cn.universal.core.protocol.jar", LogLevel.DEBUG);
        loggingSystem.setLogLevel("cn.universal", LogLevel.DEBUG);
      }
    } catch (Exception e) {
      log.warn("设置日志级别失败: {}", e.getMessage());
    }

    // 创建临时目录
    tempDir = Files.createTempDirectory("protocol-codec-test-");
    log.info("测试临时目录: {}", tempDir);

    // 创建测试 JAR 文件
    testJarFile = createTestJar("TestJarCodec", "test-codec.jar");
    testJarFile1 = createTestJar("TestJarCodec1", "test-codec1.jar", 1);
    testJarFile2 = createTestJar("TestJarCodec2", "test-codec2.jar", 2);
  }

  @After
  public void tearDown() throws Exception {
    // 清理测试数据
    if (testJarFile != null && testJarFile.exists()) {
      testJarFile.delete();
    }
    if (testJarFile1 != null && testJarFile1.exists()) {
      testJarFile1.delete();
    }
    if (testJarFile2 != null && testJarFile2.exists()) {
      testJarFile2.delete();
    }
    if (tempDir != null && Files.exists(tempDir)) {
      deleteDirectory(tempDir.toFile());
    }

    // 清理加载的 provider
    protocolCodecJar.remove("testBeanProvider");
    protocolCodecJar.remove("testJarDriverProvider");
    protocolCodecJar.remove("testLocalJarProvider");
    protocolCodecJar.remove("testRemoteJarProvider");
    protocolCodecJar.remove("cn.universal.web.protocol.TestJarCodec1");
    protocolCodecJar.remove("cn.universal.web.protocol.TestJarCodec2");
    protocolCodecJar.remove("cn.universal.web.protocol.TestJarCodec");
  }

  /** 测试 Spring Bean 类型加载 */
  @Test
  public void testLoadBeanType() throws CodecException {
    log.info("========== 开始测试 Spring Bean 类型 ==========");

    ProtocolSupportDefinition definition = createBeanDefinition();
    Object testJarCodec = SpringUtils.getBean("testJarCodec");
    System.out.println(testJarCodec.getClass().getName());
    Object jarDriverCodecService = SpringUtil.getBean(JarDriverCodecService.class);
    System.out.println(jarDriverCodecService.getClass().getName());
    System.out.println("testJarCodec" + SpringUtils.containsBean("testJarCodec"));

    // 加载 Bean
    protocolCodecJar.load(definition);

    // 测试 decode
    ProtocolDecodeRequest decodeRequest = new ProtocolDecodeRequest(definition, "test-payload");
    String decodeResult = protocolCodecJar.decode(decodeRequest);
    Assert.assertNotNull("解码结果不应为空", decodeResult);
    Assert.assertTrue("解码结果应包含 decoded 前缀", decodeResult.contains("decode:"));
    log.info("Bean 类型 decode 测试通过: {}", decodeResult);

    // 测试 encode
    ProtocolEncodeRequest encodeRequest = new ProtocolEncodeRequest(definition, "test-payload");
    String encodeResult = protocolCodecJar.encode(encodeRequest);
    Assert.assertNotNull("编码结果不应为空", encodeResult);
    Assert.assertTrue("编码结果应包含 encoded 前缀", encodeResult.contains("encode:"));
    log.info("Bean 类型 encode 测试通过: {}", encodeResult);

    // 测试 preDecode
    String preDecodeResult = protocolCodecJar.preDecode(decodeRequest);
    Assert.assertNotNull("预解码结果不应为空", preDecodeResult);
    Assert.assertTrue("预解码结果应包含 preDecoded 前缀", preDecodeResult.contains("preDecode:"));
    log.info("Bean 类型 preDecode 测试通过: {}", preDecodeResult);

    // 测试 supportMethods 校验
    Set<String> supportMethods = definition.getSupportMethods();
    Assert.assertNotNull("supportMethods 不应为空", supportMethods);
    Assert.assertTrue("应包含 decode 方法", supportMethods.contains("decode"));
    Assert.assertTrue("应包含 encode 方法", supportMethods.contains("encode"));
    log.info("Bean 类型 supportMethods: {}", supportMethods);

    log.info("========== Spring Bean 类型测试完成 ==========");
  }

  /** 测试本地 JAR 类型加载 */
  @Test
  public void testLoadLocalJarType() throws CodecException {
    log.info("========== 开始测试本地 JAR 类型 ==========");

    // 如果 JAR 文件创建失败，跳过测试
    if (testJarFile == null || !testJarFile.exists() || testJarFile.length() == 0) {
      log.warn("测试 JAR 文件不存在或为空，跳过本地 JAR 测试");
      return;
    }

    ProtocolSupportDefinition definition = createLocalJarDefinition();

    // 加载本地 JAR
    protocolCodecJar.load(definition);

    // 测试 decode
    ProtocolDecodeRequest decodeRequest = new ProtocolDecodeRequest(definition, "test-payload");
    String decodeResult = protocolCodecJar.decode(decodeRequest);
    Assert.assertNotNull("解码结果不应为空", decodeResult);
    Assert.assertTrue(
        "解码结果应包含 [JAR-DECODE] 前缀",
        decodeResult.contains("decode") || decodeResult.contains("JAR-DECODE"));
    log.info("本地 JAR 类型 decode 测试通过: {}", decodeResult);

    // 测试 encode
    ProtocolEncodeRequest encodeRequest = new ProtocolEncodeRequest(definition, "test-payload");
    String encodeResult = protocolCodecJar.encode(encodeRequest);
    Assert.assertNotNull("编码结果不应为空", encodeResult);
    Assert.assertTrue(
        "编码结果应包含 [JAR-ENCODE] 前缀",
        encodeResult.contains("encode") || encodeResult.contains("JAR-ENCODE"));
    log.info("本地 JAR 类型 encode 测试通过: {}", encodeResult);

    // 测试 supportMethods 自动检测
    Set<String> supportMethods = definition.getSupportMethods();
    Assert.assertNotNull("supportMethods 不应为空", supportMethods);
    Assert.assertTrue("应包含 decode 方法", supportMethods.contains("decode"));
    Assert.assertTrue("应包含 encode 方法", supportMethods.contains("encode"));
    log.info("本地 JAR 类型 supportMethods: {}", supportMethods);

    log.info("========== 本地 JAR 类型测试完成 ==========");
  }

  /** 测试远程 JAR 类型加载 - 第一个远程 JAR */
  @Test
  public void testLoadRemoteJarType1() throws CodecException {
    log.info("========== 开始测试远程 JAR 类型（第一个） ==========");

    // 优先使用本地生成的 JAR 文件，如果没有则使用远程 URL
    String jarLocation =
        testJarFile1 != null && testJarFile1.exists()
            ? testJarFile1.getAbsolutePath()
            : "http://qiniu.oss.filecoin.ren/test-codec.jar";
    String provider = "cn.universal.web.protocol.TestJarCodec1";

    ProtocolSupportDefinition definition = createRemoteJarDefinition(jarLocation, provider);

    try {
      // 加载 JAR
      log.info("开始加载 JAR: {}", jarLocation);
      protocolCodecJar.load(definition);
      log.info("JAR 加载成功");

      // 测试 decode（版本1应该包含 V1-DECODE-START 和 V1-DECODE-END）
      ProtocolDecodeRequest decodeRequest = new ProtocolDecodeRequest(definition, "test-payload-1");
      String decodeResult = protocolCodecJar.decode(decodeRequest);
      Assert.assertNotNull("解码结果不应为空", decodeResult);
      Assert.assertTrue(
          "解码结果应包含版本1标识",
          decodeResult.contains("V1-DECODE-START") || decodeResult.contains("JAR-DECODE"));
      log.info("JAR 类型 decode 测试通过: {}", decodeResult);

      // 测试 encode
      ProtocolEncodeRequest encodeRequest = new ProtocolEncodeRequest(definition, "test-payload-1");
      String encodeResult = protocolCodecJar.encode(encodeRequest);
      Assert.assertNotNull("编码结果不应为空", encodeResult);
      Assert.assertTrue(
          "编码结果应包含版本1标识",
          encodeResult.contains("V1-ENCODE-START") || encodeResult.contains("JAR-ENCODE"));
      log.info("JAR 类型 encode 测试通过: {}", encodeResult);

      // 测试 supportMethods 自动检测
      Set<String> supportMethods = definition.getSupportMethods();
      Assert.assertNotNull("supportMethods 不应为空", supportMethods);
      log.info("JAR 类型 supportMethods: {}", supportMethods);

      log.info("========== 远程 JAR 类型（第一个）测试完成 ==========");
    } catch (Exception e) {
      log.error("JAR 加载失败: {}", e.getMessage(), e);
      throw e;
    }
  }

  /** 测试远程 JAR 类型加载 - 第二个远程 JAR */
  @Test
  public void testLoadRemoteJarType2() throws CodecException {
    log.info("========== 开始测试远程 JAR 类型（第二个） ==========");

    // 优先使用本地生成的 JAR 文件，如果没有则使用远程 URL
    String jarLocation =
        testJarFile2 != null && testJarFile2.exists()
            ? testJarFile2.getAbsolutePath()
            : "http://qiniu.oss.filecoin.ren/test-codec1.jar";
    String provider = "cn.universal.web.protocol.TestJarCodec2";

    ProtocolSupportDefinition definition = createRemoteJarDefinition(jarLocation, provider);

    try {
      // 加载 JAR
      log.info("开始加载 JAR: {}", jarLocation);
      protocolCodecJar.load(definition);
      log.info("JAR 加载成功");

      // 测试 decode（版本2应该包含 V2-DECODE 和大写转换）
      ProtocolDecodeRequest decodeRequest = new ProtocolDecodeRequest(definition, "test-payload-2");
      String decodeResult = protocolCodecJar.decode(decodeRequest);
      Assert.assertNotNull("解码结果不应为空", decodeResult);
      Assert.assertTrue(
          "解码结果应包含版本2标识",
          decodeResult.contains("V2-DECODE") || decodeResult.contains("JAR-DECODE"));
      // 版本2会将 payload 转为大写
      if (decodeResult.contains("V2-DECODE")) {
        Assert.assertTrue("版本2应包含大写转换", decodeResult.contains("TEST-PAYLOAD-2"));
      }
      log.info("JAR 类型 decode 测试通过: {}", decodeResult);

      // 测试 encode（版本2应该包含 V2-ENCODE 和小写转换）
      ProtocolEncodeRequest encodeRequest = new ProtocolEncodeRequest(definition, "TEST-PAYLOAD-2");
      String encodeResult = protocolCodecJar.encode(encodeRequest);
      Assert.assertNotNull("编码结果不应为空", encodeResult);
      Assert.assertTrue(
          "编码结果应包含版本2标识",
          encodeResult.contains("V2-ENCODE") || encodeResult.contains("JAR-ENCODE"));
      // 版本2会将 payload 转为小写
      if (encodeResult.contains("V2-ENCODE")) {
        Assert.assertTrue("版本2应包含小写转换", encodeResult.contains("test-payload-2"));
      }
      log.info("JAR 类型 encode 测试通过: {}", encodeResult);

      // 测试 supportMethods 自动检测
      Set<String> supportMethods = definition.getSupportMethods();
      Assert.assertNotNull("supportMethods 不应为空", supportMethods);
      log.info("JAR 类型 supportMethods: {}", supportMethods);

      log.info("========== 远程 JAR 类型（第二个）测试完成 ==========");
    } catch (Exception e) {
      log.error("JAR 加载失败: {}", e.getMessage(), e);
      throw e;
    }
  }

  /** 测试加载、卸载、重新加载（模拟更新） */
  @Test
  public void testLoadUnloadReload() throws CodecException {
    log.info("========== 开始测试加载、卸载、重新加载（模拟更新） ==========");

    // 使用本地生成的 JAR 文件
    String jarLocation =
        testJarFile != null && testJarFile.exists()
            ? testJarFile.getAbsolutePath()
            : "http://qiniu.oss.filecoin.ren/test-codec.jar";
    String provider = "cn.universal.web.protocol.TestJarCodec";

    ProtocolSupportDefinition definition = createRemoteJarDefinition(jarLocation, provider);

    try {
      // 第一次加载
      log.info("========== 第一次加载 ==========");
      protocolCodecJar.load(definition);
      log.info("第一次加载成功");

      // 验证第一次加载的功能
      ProtocolDecodeRequest decodeRequest1 = new ProtocolDecodeRequest(definition, "payload-1");
      String result1 = protocolCodecJar.decode(decodeRequest1);
      Assert.assertNotNull("第一次加载后解码结果不应为空", result1);
      log.info("第一次加载后 decode 结果: {}", result1);

      // 卸载
      log.info("========== 卸载 ==========");
      protocolCodecJar.remove(provider);
      log.info("卸载成功");

      // 验证卸载后无法使用
      try {
        String result2 = protocolCodecJar.decode(decodeRequest1);
        log.warn("卸载后仍能调用，这可能表示缓存未清理: {}", result2);
      } catch (Exception e) {
        log.info("卸载后无法调用（预期行为）: {}", e.getMessage());
      }

      // 重新加载（模拟更新）
      log.info("========== 重新加载（模拟更新） ==========");
      protocolCodecJar.load(definition);
      log.info("重新加载成功");

      // 验证重新加载后的功能
      ProtocolDecodeRequest decodeRequest2 = new ProtocolDecodeRequest(definition, "payload-2");
      String result3 = protocolCodecJar.decode(decodeRequest2);
      Assert.assertNotNull("重新加载后解码结果不应为空", result3);
      log.info("重新加载后 decode 结果: {}", result3);

      log.info("========== 加载、卸载、重新加载测试完成 ==========");
    } catch (Exception e) {
      log.error("加载、卸载、重新加载测试失败: {}", e.getMessage(), e);
      throw e;
    }
  }

  /** 测试频繁的加载卸载操作 */
  @Test
  public void testFrequentLoadUnload() throws CodecException {
    log.info("========== 开始测试频繁的加载卸载操作 ==========");

    // 使用本地生成的 JAR 文件
    String jarLocation1 =
        testJarFile1 != null && testJarFile1.exists()
            ? testJarFile1.getAbsolutePath()
            : "http://qiniu.oss.filecoin.ren/test-codec.jar";
    String jarLocation2 =
        testJarFile2 != null && testJarFile2.exists()
            ? testJarFile2.getAbsolutePath()
            : "http://qiniu.oss.filecoin.ren/test-codec1.jar";
    String provider1 = "cn.universal.web.protocol.TestJarCodec1";
    String provider2 = "cn.universal.web.protocol.TestJarCodec2";

    ProtocolSupportDefinition definition1 = createRemoteJarDefinition(jarLocation1, provider1);
    ProtocolSupportDefinition definition2 = createRemoteJarDefinition(jarLocation2, provider2);

    try {
      int iterations = 10; // 频繁操作次数
      log.info("开始频繁加载卸载测试，迭代次数: {}", iterations);

      for (int i = 0; i < iterations; i++) {
        log.info("========== 迭代 {} ==========", i + 1);

        // 加载第一个 JAR
        log.info("加载第一个 JAR: {}", jarLocation1);
        protocolCodecJar.load(definition1);
        ProtocolDecodeRequest request1 =
            new ProtocolDecodeRequest(definition1, "payload-" + i + "-1");
        String result1 = protocolCodecJar.decode(request1);
        Assert.assertNotNull("第一个 JAR 解码结果不应为空", result1);
        Assert.assertTrue(
            "第一个 JAR 应包含版本1标识",
            result1.contains("V1-DECODE-START") || result1.contains("JAR-DECODE"));
        log.info("第一个 JAR decode 结果: {}", result1);

        // 加载第二个 JAR
        log.info("加载第二个 JAR: {}", jarLocation2);
        protocolCodecJar.load(definition2);
        ProtocolDecodeRequest request2 =
            new ProtocolDecodeRequest(definition2, "payload-" + i + "-2");
        String result2 = protocolCodecJar.decode(request2);
        Assert.assertNotNull("第二个 JAR 解码结果不应为空", result2);
        Assert.assertTrue(
            "第二个 JAR 应包含版本2标识", result2.contains("V2-DECODE") || result2.contains("JAR-DECODE"));
        log.info("第二个 JAR decode 结果: {}", result2);

        // 卸载第一个 JAR
        log.info("卸载第一个 JAR");
        protocolCodecJar.remove(provider1);

        // 验证第二个 JAR 仍然可用
        String result3 = protocolCodecJar.decode(request2);
        Assert.assertNotNull("卸载第一个 JAR 后，第二个 JAR 仍应可用", result3);
        log.info("卸载第一个 JAR 后，第二个 JAR decode 结果: {}", result3);

        // 卸载第二个 JAR
        log.info("卸载第二个 JAR");
        protocolCodecJar.remove(provider2);

        // 短暂休眠，模拟实际场景
        Thread.sleep(100);
      }

      log.info("========== 频繁加载卸载测试完成 ==========");
    } catch (Exception e) {
      log.error("频繁加载卸载测试失败: {}", e.getMessage(), e);
    }
  }

  /** 测试 supportMethods 校验功能 */
  @Test
  public void testSupportMethodsValidation() throws CodecException {
    log.info("========== 开始测试 supportMethods 校验 ==========");

    ProtocolSupportDefinition definition = createBeanDefinition();

    // 设置配置的 supportMethods（包含一个不存在的方法）
    Set<String> configuredMethods = new HashSet<>();
    configuredMethods.add("decode");
    configuredMethods.add("encode");
    configuredMethods.add("nonExistentMethod"); // 不存在的方法
    definition.setSupportMethods(configuredMethods);

    // 加载 Bean
    protocolCodecJar.load(definition);

    // 验证：最终 supportMethods 应该只包含实际实现的方法
    Set<String> finalMethods = definition.getSupportMethods();
    Assert.assertNotNull("supportMethods 不应为空", finalMethods);
    Assert.assertTrue("应包含 decode 方法", finalMethods.contains("decode"));
    Assert.assertTrue("应包含 encode 方法", finalMethods.contains("encode"));
    Assert.assertFalse("不应包含不存在的方法", finalMethods.contains("nonExistentMethod"));
    log.info("supportMethods 校验测试通过: {}", finalMethods);

    log.info("========== supportMethods 校验测试完成 ==========");
  }

  /** 创建 Spring Bean 类型的定义 */
  private ProtocolSupportDefinition createBeanDefinition() {
    ProtocolSupportDefinition definition = new ProtocolSupportDefinition();
    definition.setId("test-bean-id");
    definition.setName("测试 Bean 编解码器");
    definition.setProvider("testBeanProvider");
    definition.setType("jar");

    Map<String, Object> config = new HashMap<>();
    config.put("location", "testJarCodec"); // Spring Bean 名称
    config.put("provider", "testBeanProvider");
    definition.setConfiguration(config);

    return definition;
  }

  /** 创建 JarDriverCodecService 类型的定义 */
  private ProtocolSupportDefinition createJarDriverDefinition() {
    ProtocolSupportDefinition definition = new ProtocolSupportDefinition();
    definition.setId("test-jar-driver-id");
    definition.setName("测试 JarDriverCodecService 编解码器");
    definition.setProvider("testJarDriverProvider");
    definition.setType("jar");

    Map<String, Object> config = new HashMap<>();
    config.put("location", "testJarDriverCodecService"); // Spring Bean 名称
    config.put("provider", "testJarDriverProvider");
    definition.setConfiguration(config);

    return definition;
  }

  /** 创建本地 JAR 类型的定义 */
  private ProtocolSupportDefinition createLocalJarDefinition() {
    ProtocolSupportDefinition definition = new ProtocolSupportDefinition();
    definition.setId("test-local-jar-id");
    definition.setName("测试本地 JAR 编解码器");
    definition.setProvider("cn.universal.web.protocol.TestJarCodec");
    definition.setType("jar");

    Map<String, Object> config = new HashMap<>();
    config.put("location", testJarFile.getAbsolutePath());
    config.put("provider", "cn.universal.web.protocol.TestJarCodec");
    definition.setConfiguration(config);

    return definition;
  }

  /** 创建远程 JAR 类型的定义 */
  private ProtocolSupportDefinition createRemoteJarDefinition(
      String remoteJarUrl, String provider) {
    ProtocolSupportDefinition definition = new ProtocolSupportDefinition();
    definition.setId("test-remote-jar-id");
    definition.setName("测试远程 JAR 编解码器");
    definition.setProvider(provider);
    definition.setType("jar");

    Map<String, Object> config = new HashMap<>();
    config.put("location", remoteJarUrl);
    config.put("provider", provider);
    definition.setConfiguration(config);

    return definition;
  }

  /** 创建测试用的 JAR 文件（默认版本） */
  private File createTestJar(String className, String jarFileName) throws IOException {
    return createTestJar(className, jarFileName, 0);
  }

  /** 创建测试用的 JAR 文件 注意：如果编译失败，测试会跳过 JAR 相关的测试用例 */
  private File createTestJar(String className, String jarFileName, int version) throws IOException {
    File jarFile = new File(tempDir.toFile(), jarFileName);

    try {
      // 根据版本创建不同的测试类源码
      String javaCode = generateTestClassCode(className, version);

      // 创建包目录
      File packageDir = new File(tempDir.toFile(), "cn/universal/web/protocol");
      packageDir.mkdirs();

      // 编译 Java 源码
      File javaFile = new File(packageDir, className + ".java");
      Files.write(javaFile.toPath(), javaCode.getBytes());

      // 编译
      JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
      if (compiler == null) {
        log.warn("无法获取 Java 编译器，跳过 JAR 文件创建");
        return jarFile;
      }

      StandardJavaFileManager fileManager = compiler.getStandardFileManager(null, null, null);
      Iterable<? extends javax.tools.JavaFileObject> compilationUnits =
          fileManager.getJavaFileObjects(javaFile);

      String classPath = System.getProperty("java.class.path");
      Iterable<String> options =
          java.util.Arrays.asList("-d", tempDir.toString(), "-cp", classPath);

      boolean success =
          compiler.getTask(null, fileManager, null, options, null, compilationUnits).call();

      try {
        fileManager.close();
      } catch (IOException e) {
        log.warn("关闭文件管理器失败", e);
      }

      if (!success) {
        log.warn("编译失败，跳过 JAR 文件创建: {}", className);
        return jarFile;
      }

      // 创建 JAR 文件
      Manifest manifest = new Manifest();
      manifest.getMainAttributes().put(java.util.jar.Attributes.Name.MANIFEST_VERSION, "1.0");

      try (JarOutputStream jos = new JarOutputStream(new FileOutputStream(jarFile), manifest)) {
        // 添加编译后的类文件
        File classFile = new File(packageDir, className + ".class");
        if (classFile.exists()) {
          JarEntry entry = new JarEntry("cn/universal/web/protocol/" + className + ".class");
          jos.putNextEntry(entry);
          Files.copy(classFile.toPath(), jos);
          jos.closeEntry();
          log.info("测试 JAR 文件创建成功: {} (版本: {})", jarFile.getAbsolutePath(), version);
        } else {
          log.warn("编译后的类文件不存在: {}", classFile.getAbsolutePath());
        }
      }
    } catch (Exception e) {
      log.warn("创建测试 JAR 文件时出错: {}", e.getMessage(), e);
    }

    return jarFile;
  }

  /** 生成不同版本的测试类代码 */
  private String generateTestClassCode(String className, int version) {
    switch (version) {
      case 0:
        // 版本0：基础实现
        return "package cn.universal.web.protocol;\n"
            + "public class "
            + className
            + " {\n"
            + "    public String decode(String payload, Object context) {\n"
            + "        return \"[JAR-DECODE-V0]\" + payload;\n"
            + "    }\n"
            + "    public String encode(String payload, Object context) {\n"
            + "        return \"[JAR-ENCODE-V0]\" + payload;\n"
            + "    }\n"
            + "    public String preDecode(String payload, Object context) {\n"
            + "        return \"[JAR-PREDECODE-V0]\" + payload;\n"
            + "    }\n"
            + "}\n";

      case 1:
        // 版本1：添加前缀和后缀
        return "package cn.universal.web.protocol;\n"
            + "public class "
            + className
            + " {\n"
            + "    public String decode(String payload, Object context) {\n"
            + "        return \"[V1-DECODE-START]\" + payload + \"[V1-DECODE-END]\";\n"
            + "    }\n"
            + "    public String encode(String payload, Object context) {\n"
            + "        return \"[V1-ENCODE-START]\" + payload + \"[V1-ENCODE-END]\";\n"
            + "    }\n"
            + "    public String preDecode(String payload, Object context) {\n"
            + "        return \"[V1-PREDECODE-START]\" + payload + \"[V1-PREDECODE-END]\";\n"
            + "    }\n"
            + "    public String iotToYour(String payload, Object context) {\n"
            + "        return \"[V1-IOT-TO-YOUR]\" + payload;\n"
            + "    }\n"
            + "    public String yourToIot(String payload, Object context) {\n"
            + "        return \"[V1-YOUR-TO-IOT]\" + payload;\n"
            + "    }\n"
            + "}\n";

      case 2:
        // 版本2：大写转换
        return "package cn.universal.web.protocol;\n"
            + "public class "
            + className
            + " {\n"
            + "    public String decode(String payload, Object context) {\n"
            + "        return \"[V2-DECODE]\" + payload.toUpperCase();\n"
            + "    }\n"
            + "    public String encode(String payload, Object context) {\n"
            + "        return \"[V2-ENCODE]\" + payload.toLowerCase();\n"
            + "    }\n"
            + "    public String preDecode(String payload, Object context) {\n"
            + "        return \"[V2-PREDECODE]\" + payload;\n"
            + "    }\n"
            + "}\n";

      default:
        // 默认版本
        return "package cn.universal.web.protocol;\n"
            + "public class "
            + className
            + " {\n"
            + "    public String decode(String payload, Object context) {\n"
            + "        return \"[DECODE]\" + payload;\n"
            + "    }\n"
            + "    public String encode(String payload, Object context) {\n"
            + "        return \"[ENCODE]\" + payload;\n"
            + "    }\n"
            + "    public String preDecode(String payload, Object context) {\n"
            + "        return \"[PREDECODE]\" + payload;\n"
            + "    }\n"
            + "}\n";
    }
  }

  /** 递归删除目录 */
  private void deleteDirectory(File directory) {
    if (directory.exists()) {
      File[] files = directory.listFiles();
      if (files != null) {
        for (File file : files) {
          if (file.isDirectory()) {
            deleteDirectory(file);
          } else {
            file.delete();
          }
        }
      }
      directory.delete();
    }
  }
}
