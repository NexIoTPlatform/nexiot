package cn.universal.web.config;

import static org.junit.jupiter.api.Assertions.*;

import cn.universal.web.service.EmqxAuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

/**
 * EMQX 认证配置测试类
 *
 * @author gitee.com/NexIoT
 * @version 1.0
 * @since 2025/1/20
 */
@SpringBootTest
@ActiveProfiles("test")
class EmqxAuthConfigTest {

  @Autowired private EmqxAuthService emqxAuthService;

  @Autowired private BCryptPasswordEncoder bCryptPasswordEncoder;

  @Test
  void testEmqxAuthServiceBean() {
    assertNotNull(emqxAuthService, "EmqxAuthService Bean 应该被正确创建");
  }

  @Test
  void testBCryptPasswordEncoderBean() {
    assertNotNull(bCryptPasswordEncoder, "BCryptPasswordEncoder Bean 应该被正确创建");
  }

  @Test
  void testPasswordEncoding() {
    String rawPassword = "testPassword123";
    String encodedPassword = bCryptPasswordEncoder.encode(rawPassword);

    assertNotNull(encodedPassword, "编码后的密码不应该为空");
    assertNotEquals(rawPassword, encodedPassword, "编码后的密码应该与原始密码不同");
    assertTrue(bCryptPasswordEncoder.matches(rawPassword, encodedPassword), "密码匹配应该成功");
  }
}
