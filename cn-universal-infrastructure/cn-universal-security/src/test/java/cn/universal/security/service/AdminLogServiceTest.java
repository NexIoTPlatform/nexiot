package cn.universal.security.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import cn.universal.common.constant.Constants;
import cn.universal.persistence.entity.SysLogininfor;
import cn.universal.persistence.mapper.SysLogininforMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

class AdminLogServiceTest {

  private final SysLogininforMapper mapper = mock(SysLogininforMapper.class);
  private final AdminLogService service = new AdminLogService();

  @BeforeEach
  void setUp() {
    ReflectionTestUtils.setField(service, "sysLogininforMapper", mapper);
    RequestContextHolder.resetRequestAttributes();
  }

  @AfterEach
  void tearDown() {
    RequestContextHolder.resetRequestAttributes();
  }

  @Test
  void providerCanPassNullRequestAndRetainForwardedIpAndUserAgent() {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addHeader("X-Forwarded-For", "192.168.31.105, 192.168.31.194");
    request.addHeader(
        "User-Agent",
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
            + "(KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");
    RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

    service.recordLogininfor("tenant-user-123", Constants.LOGIN_SUCCESS, "密码登录成功", null);

    SysLogininfor saved = savedLog();
    assertEquals("tenant-user-123", saved.getUserName());
    assertEquals(Constants.SUCCESS, saved.getStatus());
    assertEquals("密码登录成功", saved.getMsg());
    assertEquals("192.168.31.105", saved.getIpaddr());
    assertEquals("Chrome", saved.getBrowser());
    assertNotNull(saved.getLoginTime());
  }

  @Test
  void missingRequestContextStillPersistsFailedLogin() {
    service.recordLogininfor("unknown-account", Constants.LOGIN_FAIL, "用户名或密码错误", null);

    SysLogininfor saved = savedLog();
    assertEquals("unknown-account", saved.getUserName());
    assertEquals(Constants.FAIL, saved.getStatus());
    assertEquals("用户名或密码错误", saved.getMsg());
    assertEquals("unknown", saved.getIpaddr());
    assertNotNull(saved.getLoginTime());
  }

  @Test
  void explicitRequestWithoutUserAgentStillPersistsLogin() {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setRemoteAddr("192.168.31.105");

    service.recordLogininfor("tenant-user-123", Constants.LOGIN_SUCCESS, "密码登录成功", request);

    SysLogininfor saved = savedLog();
    assertEquals("tenant-user-123", saved.getUserName());
    assertEquals(Constants.SUCCESS, saved.getStatus());
    assertEquals("192.168.31.105", saved.getIpaddr());
    assertEquals("unknown", saved.getBrowser());
    assertEquals("unknown", saved.getOs());
    assertNotNull(saved.getLoginTime());
  }

  private SysLogininfor savedLog() {
    ArgumentCaptor<SysLogininfor> captor = ArgumentCaptor.forClass(SysLogininfor.class);
    verify(mapper).insertSelective(captor.capture());
    return captor.getValue();
  }
}
