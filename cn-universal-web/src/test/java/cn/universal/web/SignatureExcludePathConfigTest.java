package cn.universal.web;

import static org.junit.jupiter.api.Assertions.*;

import cn.universal.web.config.intercepter.SignatureExcludePathConfig;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.test.context.junit.jupiter.SpringExtension;

/** SignatureExcludePathConfig 单元测试 覆盖：精确匹配、前缀匹配、后缀匹配、包含匹配、不匹配场景 */
@ExtendWith(SpringExtension.class)
@EnableConfigurationProperties(
    SignatureExcludePathConfig.class) // 启用配置属性绑定，兼容被测试类的 @ConfigurationProperties
class SignatureExcludePathConfigTest {

  // 被测试对象
  private SignatureExcludePathConfig signatureExcludePathConfig;

  /** 每个测试方法执行前初始化被测试对象 */
  @BeforeEach
  void setUp() {
    signatureExcludePathConfig = new SignatureExcludePathConfig();

    // 手动注入自定义配置路径（模拟 yml 中的 security.request-sign.exclude-paths 配置）
    List<String> customExcludePaths =
        Arrays.asList(
            "/custom/api/test", // 自定义精确匹配
            "/custom/admin/**", // 自定义前缀匹配
            "**/custom/export", // 自定义后缀匹配
            "**/custom/openapi/**" // 自定义包含匹配
            );
    signatureExcludePathConfig.setExcludePaths(customExcludePaths);
  }

  /** 测试 1：精确匹配场景 */
  @Test
  void testIsExcludePath_ExactMatch() {
    // 硬编码的精确路径
    assertTrue(signatureExcludePathConfig.isExcludePath("/oauth/token"));
    assertTrue(signatureExcludePathConfig.isExcludePath("/login"));
    assertTrue(signatureExcludePathConfig.isExcludePath("/admin/system/oss/upload"));

    // 自定义配置的精确路径
    assertTrue(signatureExcludePathConfig.isExcludePath("/custom/api/test"));

    // 精确路径不匹配（大小写、后缀差异）
    assertFalse(signatureExcludePathConfig.isExcludePath("/oauth/token123"));
    assertFalse(signatureExcludePathConfig.isExcludePath("/Login"));
  }

  /** 测试 2：前缀匹配场景（/xxx/** 格式） */
  @Test
  void testIsExcludePath_PrefixMatch() {
    // 自定义前缀匹配（/custom/admin/**）
    assertTrue(signatureExcludePathConfig.isExcludePath("/custom/admin/user/list"));
    assertTrue(signatureExcludePathConfig.isExcludePath("/custom/admin/role/add"));
    assertTrue(signatureExcludePathConfig.isExcludePath("/custom/admin/"));

    // 前缀不匹配
    assertFalse(signatureExcludePathConfig.isExcludePath("/custom/adminX/user/list"));
    assertFalse(signatureExcludePathConfig.isExcludePath("/other/admin/user/list"));
  }

  /** */
  @Test
  void testIsExcludePath_SuffixMatch() {
    // 硬编码的后缀匹配（**/export）
    assertTrue(signatureExcludePathConfig.isExcludePath("/admin/system/export"));
    assertTrue(signatureExcludePathConfig.isExcludePath("/api/v1/user/export"));

    // 自定义后缀匹配（**/custom/export）
    assertTrue(signatureExcludePathConfig.isExcludePath("/a/b/c/custom/export"));
    assertTrue(signatureExcludePathConfig.isExcludePath("/custom/export"));

    // 后缀不匹配
    assertFalse(signatureExcludePathConfig.isExcludePath("/admin/system/export123"));
    assertFalse(signatureExcludePathConfig.isExcludePath("/custom/exportX"));
  }

  /** */
  @Test
  void testIsExcludePath_ContainMatch() {
    // 硬编码的包含匹配（**/openapi/**）
    assertTrue(signatureExcludePathConfig.isExcludePath("/api/api/openapi/v1/iot/product/list"));
    assertTrue(signatureExcludePathConfig.isExcludePath("/openapi/v1/device"));
    assertTrue(signatureExcludePathConfig.isExcludePath("/a/b/c/openapi/d/e/f"));

    // 自定义包含匹配（**/custom/openapi/**）
    assertTrue(signatureExcludePathConfig.isExcludePath("/custom/api/openapi/v1/test"));
    assertTrue(signatureExcludePathConfig.isExcludePath("/a/custom/openapi/b"));

    // 包含不匹配
    assertFalse(signatureExcludePathConfig.isExcludePath("/api/api/openapix/v1/iot/product/list"));
    assertFalse(signatureExcludePathConfig.isExcludePath("/custom/openapix/v1/test"));
  }

  /** 测试 5：不匹配场景（边界值、无效路径） */
  @Test
  void testIsExcludePath_NoMatch() {
    // 空路径、null
    assertFalse(signatureExcludePathConfig.isExcludePath(null));
    assertFalse(signatureExcludePathConfig.isExcludePath(""));

    // 无匹配的随机路径
    assertFalse(signatureExcludePathConfig.isExcludePath("/api/v1/unknown/path"));
    assertFalse(signatureExcludePathConfig.isExcludePath("/admin/unknown/export123"));
    assertFalse(signatureExcludePathConfig.isExcludePath("/custom/openapiX/v1/test"));
  }
}
