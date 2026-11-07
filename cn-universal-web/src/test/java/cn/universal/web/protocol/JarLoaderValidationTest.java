package cn.universal.web.protocol;

import cn.universal.core.protocol.jar.JarDriverCodecService;
import cn.universal.core.protocol.jar.ProtocolJarClassLoader;
import java.io.File;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.jar.JarFile;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;

/**
 * JAR包加载和实例化完整测试用例
 *
 * <p>用于诊断JAR包加载失败的原因： 1. JAR文件是否存在 2. JAR文件是否可读 3. 类名是否正确 4. 类是否存在 5. 类是否有无参构造函数 6. 类加载器是否正确
 */
@Slf4j
public class JarLoaderValidationTest {

  /**
   * 测试场景1：分析JAR文件内容
   *
   * <p>目的：列出JAR包中的所有类，检查期望的类是否存在
   */
  @Test
  public void test01_ListJarContents() {
    String jarPath = "/Users/liulihai/Documents/ByteCode/code/liulihai/nexiot_driver_extend/target/nexiot_driver_extend_20260105055820.jar";

    log.info("========== 测试1：分析JAR文件内容 ==========");
    log.info("JAR路径: {}", jarPath);

    File jarFile = new File(jarPath);
    if (!jarFile.exists()) {
      log.error("❌ JAR文件不存在！");
      return;
    }

    log.info("✅ JAR文件存在，大小: {} bytes", jarFile.length());

    try (JarFile jar = new JarFile(jarFile)) {
      log.info("JAR文件中的所有.class文件：");
      log.info("----------------------------------------");

      List<String> classNames = new ArrayList<>();
      jar.stream()
          .filter(entry -> entry.getName().endsWith(".class"))
          .forEach(
              entry -> {
                String className = entry.getName().replace("/", ".").replace(".class", "");
                classNames.add(className);
                log.info("  - {}", className);
              });

      log.info("----------------------------------------");
      log.info("总计: {} 个类文件", classNames.size());

      // 检查是否实现了JarDriverCodecService接口的类
      log.info("\n检查哪些类可能是编解码服务类：");
      for (String className : classNames) {
        if (className.toLowerCase().contains("codec")
            || className.toLowerCase().contains("service")) {
          log.info("  🔍 可能的编解码类: {}", className);
        }
      }

    } catch (Exception e) {
      log.error("❌ 读取JAR文件失败", e);
    }
  }

  /**
   * 测试场景2：验证类加载
   *
   * <p>目的：测试使用不同的类加载器加载指定类
   */
  @Test
  public void test02_LoadClassWithDifferentLoaders() {
    String jarPath = "/Users/liulihai/Documents/ByteCode/code/liulihai/nexiot_driver_extend/target/nexiot_driver_extend_20260105055820.jar";
    String className = "cc.nexiot.driver.service.NexIoTCodec1"; // 替换为实际的类名

    log.info("========== 测试2：验证类加载 ==========");
    log.info("JAR路径: {}", jarPath);
    log.info("类名: {}", className);

    File jarFile = new File(jarPath);
    if (!jarFile.exists()) {
      log.error("❌ JAR文件不存在！");
      return;
    }

    try {
      URL jarUrl = jarFile.toURI().toURL();
      log.info("JAR URL: {}", jarUrl);

      // 方法1：使用URLClassLoader
      log.info("\n--- 方法1：使用URLClassLoader ---");
      try (URLClassLoader loader1 =
          new URLClassLoader(new URL[] {jarUrl}, Thread.currentThread().getContextClassLoader())) {
        Class<?> clazz1 = loader1.loadClass(className);
        log.info("✅ URLClassLoader加载成功: {}", clazz1.getName());
        log.info("   类加载器: {}", clazz1.getClassLoader().getClass().getName());
      } catch (Exception e) {
        log.error("❌ URLClassLoader加载失败", e);
      }

      // 方法2：使用ProtocolJarClassLoader
      log.info("\n--- 方法2：使用ProtocolJarClassLoader ---");
      try {
        ProtocolJarClassLoader loader2 =
            new ProtocolJarClassLoader(
                new URL[] {jarUrl}, Thread.currentThread().getContextClassLoader());
        Class<?> clazz2 = loader2.loadClass(className);
        log.info("✅ ProtocolJarClassLoader加载成功: {}", clazz2.getName());
        log.info("   类加载器: {}", clazz2.getClassLoader().getClass().getName());
      } catch (Exception e) {
        log.error("❌ ProtocolJarClassLoader加载失败", e);
      }

    } catch (Exception e) {
      log.error("❌ 创建类加载器失败", e);
    }
  }

  /**
   * 测试场景3：验证类实例化
   *
   * <p>目的：测试类的构造函数和实例化过程
   */
  @Test
  public void test03_InstantiateClass() {
    String jarPath = "/Users/liulihai/Documents/ByteCode/code/liulihai/nexiot_driver_extend/target/nexiot_driver_extend_20260105055820.jar";
    String className = "cc.nexiot.driver.service.NexIoTCodec1"; // 替换为实际的类名

    log.info("========== 测试3：验证类实例化 ==========");
    log.info("JAR路径: {}", jarPath);
    log.info("类名: {}", className);

    File jarFile = new File(jarPath);
    if (!jarFile.exists()) {
      log.error("❌ JAR文件不存在！");
      return;
    }

    try {
      URL jarUrl = jarFile.toURI().toURL();
      ProtocolJarClassLoader loader =
          new ProtocolJarClassLoader(
              new URL[] {jarUrl}, Thread.currentThread().getContextClassLoader());

      // 加载类
      Class<?> clazz = loader.loadClass(className);
      log.info("✅ 类加载成功: {}", clazz.getName());

      // 检查构造函数
      log.info("\n检查构造函数：");
      Arrays.stream(clazz.getConstructors())
          .forEach(
              constructor -> {
                log.info("  - {}", constructor);
              });

      Arrays.stream(clazz.getDeclaredConstructors())
          .forEach(
              constructor -> {
                log.info("  - (声明的) {}", constructor);
              });

      // 尝试实例化
      log.info("\n尝试实例化：");
      try {
        Object instance = clazz.getDeclaredConstructor().newInstance();
        log.info("✅ 实例化成功！");
        log.info("   实例类型: {}", instance.getClass().getName());

        // 检查是否实现了JarDriverCodecService接口
        if (instance instanceof JarDriverCodecService) {
          log.info("✅ 实现了JarDriverCodecService接口");
          JarDriverCodecService service = (JarDriverCodecService) instance;
          log.info("   productKey: {}", service.productKey());
        } else {
          log.warn("⚠️  未实现JarDriverCodecService接口");
        }

      } catch (NoSuchMethodException e) {
        log.error("❌ 没有无参构造函数", e);
      } catch (Exception e) {
        log.error("❌ 实例化失败", e);
      }

    } catch (Exception e) {
      log.error("❌ 加载或实例化失败", e);
    }
  }

  /**
   * 测试场景4：检查类的方法
   *
   * <p>目的：验证类是否有必要的编解码方法
   */
  @Test
  public void test04_CheckClassMethods() {
    String jarPath = "/Users/liulihai/Documents/ByteCode/code/liulihai/nexiot_driver_extend/target/nexiot_driver_extend_20260105055820.jar";
    String className = "cc.nexiot.driver.service.NexIoTCodec1"; // 替换为实际的类名

    log.info("========== 测试4：检查类的方法 ==========");
    log.info("JAR路径: {}", jarPath);
    log.info("类名: {}", className);

    File jarFile = new File(jarPath);
    if (!jarFile.exists()) {
      log.error("❌ JAR文件不存在！");
      return;
    }

    try {
      URL jarUrl = jarFile.toURI().toURL();
      ProtocolJarClassLoader loader =
          new ProtocolJarClassLoader(
              new URL[] {jarUrl}, Thread.currentThread().getContextClassLoader());

      Class<?> clazz = loader.loadClass(className);
      log.info("✅ 类加载成功: {}", clazz.getName());

      log.info("\n所有公共方法：");
      for (Method method : clazz.getMethods()) {
        log.info("  - {}", method.toGenericString());
      }

      log.info("\n检查必要的编解码方法：");
      String[] requiredMethods = {"decode", "encode", "preDecode"};
      for (String methodName : requiredMethods) {
        try {
          Method method = clazz.getMethod(methodName, String.class, Object.class);
          log.info("  ✅ {} 方法存在", methodName);
        } catch (NoSuchMethodException e) {
          log.warn("  ⚠️  {} 方法不存在", methodName);
        }
      }

    } catch (Exception e) {
      log.error("❌ 检查方法失败", e);
    }
  }

  /**
   * 测试场景5：完整的加载、实例化、调用流程
   *
   * <p>目的：模拟真实的使用场景
   */
  @Test
  public void test05_FullWorkflow() {
    String jarPath = "/Users/liulihai/Documents/ByteCode/code/liulihai/nexiot_driver_extend/target/nexiot_driver_extend_20260105055820.jar";
    String className = "cc.nexiot.driver.service.NexIoTCodec1"; // 替换为实际的类名

    log.info("========== 测试5：完整工作流程 ==========");
    log.info("JAR路径: {}", jarPath);
    log.info("类名: {}", className);

    File jarFile = new File(jarPath);
    if (!jarFile.exists()) {
      log.error("❌ JAR文件不存在！");
      return;
    }

    try {
      // 1. 创建类加载器
      log.info("\n步骤1：创建类加载器");
      URL jarUrl = jarFile.toURI().toURL();
      ProtocolJarClassLoader loader =
          new ProtocolJarClassLoader(
              new URL[] {jarUrl}, Thread.currentThread().getContextClassLoader());
      log.info("✅ 类加载器创建成功");

      // 2. 加载类
      log.info("\n步骤2：加载类");
      Class<?> clazz = Class.forName(className, true, loader);
      log.info("✅ 类加载成功: {}", clazz.getName());

      // 3. 实例化
      log.info("\n步骤3：实例化对象");
      Object instance = clazz.getDeclaredConstructor().newInstance();
      log.info("✅ 实例化成功");

      // 4. 检查接口
      log.info("\n步骤4：检查接口实现");
      if (instance instanceof JarDriverCodecService) {
        JarDriverCodecService service = (JarDriverCodecService) instance;
        log.info("✅ 实现了JarDriverCodecService接口");

        // 5. 调用方法
        log.info("\n步骤5：调用编解码方法");
        try {
          String testPayload = "test";
          String result = service.decode(testPayload, null);
          log.info("✅ decode方法调用成功，结果: {}", result);
        } catch (Exception e) {
          log.error("❌ decode方法调用失败", e);
        }

      } else {
        log.warn("⚠️  未实现JarDriverCodecService接口，尝试反射调用");

        // 使用反射调用
        try {
          Method decodeMethod = clazz.getMethod("decode", String.class, Object.class);
          String result = (String) decodeMethod.invoke(instance, "test", null);
          log.info("✅ 反射调用decode方法成功，结果: {}", result);
        } catch (Exception e) {
          log.error("❌ 反射调用decode方法失败", e);
        }
      }

      log.info("\n========== 测试完成 ==========");

    } catch (ClassNotFoundException e) {
      log.error("❌ 类未找到: {}", className, e);
      log.error("   提示：请检查类名是否正确，是否包含包路径");
    } catch (NoSuchMethodException e) {
      log.error("❌ 找不到无参构造函数", e);
      log.error("   提示：请确保类有public无参构造函数");
    } catch (Exception e) {
      log.error("❌ 工作流程执行失败", e);
    }
  }

  /** 辅助方法：验证JAR文件完整性 */
  @Test
  public void test06_ValidateJarIntegrity() {
    String jarPath = "/Users/liulihai/Documents/ByteCode/code/liulihai/nexiot_driver_extend/target/nexiot_driver_extend_20260105055820.jar";
    String className = "cc.nexiot.driver.service.NexIoTCodec1"; // 替换为实际的类名

    log.info("========== 测试6：JAR文件完整性验证 ==========");
    log.info("JAR路径: {}", jarPath);

    File jarFile = new File(jarPath);

    // 检查文件
    log.info("\n文件基本信息：");
    log.info("  存在: {}", jarFile.exists());
    log.info("  可读: {}", jarFile.canRead());
    log.info("  大小: {} bytes", jarFile.length());
    log.info("  路径: {}", jarFile.getAbsolutePath());

    // 检查JAR完整性
    try (JarFile jar = new JarFile(jarFile)) {
      log.info("\n✅ JAR文件格式正确");

      // 检查MANIFEST
      if (jar.getManifest() != null) {
        log.info("✅ 包含MANIFEST.MF");
        jar.getManifest()
            .getMainAttributes()
            .forEach(
                (key, value) -> {
                  log.info("  {}: {}", key, value);
                });
      } else {
        log.warn("⚠️  没有MANIFEST.MF");
      }

    } catch (Exception e) {
      log.error("❌ JAR文件损坏或格式错误", e);
    }
  }
}
