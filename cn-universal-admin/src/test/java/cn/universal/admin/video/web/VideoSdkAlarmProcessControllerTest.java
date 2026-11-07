package cn.universal.admin.video.web;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import cn.universal.admin.system.permission.SysPermissionService;
import cn.universal.admin.video.alarm.VideoSdkAlarmProcessService;
import cn.universal.admin.video.service.VideoOperationAccess;
import cn.universal.admin.video.service.VideoPlatformInstanceService;
import cn.universal.common.exception.BaseException;
import cn.universal.persistence.entity.IoTUser;
import cn.universal.persistence.entity.VideoPlatformInstance;
import cn.universal.security.service.IoTUserService;
import cn.universal.security.utils.SecurityUtils;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.context.SecurityContextHolder;

class VideoSdkAlarmProcessControllerTest {
  public static class Grants {
    final Set<String> allowed = new HashSet<>();
  }

  @Configuration
  @EnableMethodSecurity(proxyTargetClass = true)
  static class Config {
    @Bean
    IoTUserService ioTUserService() {
      return mock(IoTUserService.class);
    }

    @Bean
    Grants grants() {
      return new Grants();
    }

    @Bean
    VideoPlatformInstanceService instances() {
      return mock(VideoPlatformInstanceService.class);
    }

    @Bean
    VideoSdkAlarmProcessService processes() {
      return mock(VideoSdkAlarmProcessService.class);
    }

    @Bean("videoOperationAccess")
    VideoOperationAccess access(Grants grants) {
      var permissions = mock(SysPermissionService.class);
      when(permissions.getCurrentPermissions()).thenAnswer(invocation -> grants.allowed);
      return new VideoOperationAccess(permissions);
    }

    @Bean
    VideoSdkAlarmProcessController controller(
        VideoPlatformInstanceService instances, VideoSdkAlarmProcessService processes) {
      return new VideoSdkAlarmProcessController(instances, processes);
    }
  }

  private static AnnotationConfigApplicationContext context() {
    SecurityContextHolder.getContext()
        .setAuthentication(
            new UsernamePasswordAuthenticationToken("reader", "", java.util.List.of()));
    return new AnnotationConfigApplicationContext(Config.class);
  }

  private static IoTUser user(String tenant, Integer identity) {
    var user = new IoTUser();
    user.setUnionId("reader");
    user.setTenantId(tenant);
    user.setIdentity(identity);
    return user;
  }

  private static VideoPlatformInstance instance(String tenant, String type) {
    return VideoPlatformInstance.builder()
        .tenantId(tenant)
        .instanceKey("isup")
        .platformType(type)
        .build();
  }

  @AfterEach
  void clearAuthentication() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void bothRoutesRequireAReadGrantBeforeResolvingTheInstance() {
    try (var context = context();
        var security = mockStatic(SecurityUtils.class)) {
      security.when(SecurityUtils::getUnionId).thenReturn("reader");
      var controller = context.getBean(VideoSdkAlarmProcessController.class);
      var grants = context.getBean(Grants.class);
      var instances = context.getBean(VideoPlatformInstanceService.class);
      var processes = context.getBean(VideoSdkAlarmProcessService.class);
      grants.allowed.add("video:device:control");
      assertThrows(AccessDeniedException.class, () -> controller.list("isup", Map.of()));
      assertThrows(
          AccessDeniedException.class, () -> controller.events("isup", "process", Map.of()));
      verifyNoInteractions(instances, processes);
      var instance = instance("tenant-a", "hik_isup");
      when(instances.getByInstanceKey("isup")).thenReturn(instance);
      when(context.getBean(IoTUserService.class).selectUserByUnionId("reader"))
          .thenReturn(user("tenant-a", 1));
      for (String permission :
          Set.of("video:platform:view", "video:platform:query", "video:platform:edit", "*:*:*")) {
        grants.allowed.clear();
        grants.allowed.add(permission);
        assertDoesNotThrow(() -> controller.list("isup", Map.of()));
        assertDoesNotThrow(() -> controller.events("isup", "process", Map.of()));
      }
      verify(processes, times(4)).list(same(instance), anyMap());
      verify(processes, times(4)).events(same(instance), eq("process"), anyMap());
    }
  }

  @Test
  void missingLoginUserIsRejectedBeforeAnyInstanceOrProcessRead() {
    try (var context = context();
        var security = mockStatic(SecurityUtils.class)) {
      security.when(SecurityUtils::getUnionId).thenReturn("removed-user");
      context.getBean(Grants.class).allowed.add("video:platform:view");
      var controller = context.getBean(VideoSdkAlarmProcessController.class);
      assertThrows(BaseException.class, () -> controller.list("isup", Map.of()));
      assertThrows(BaseException.class, () -> controller.events("isup", "process", Map.of()));
      verifyNoInteractions(
          context.getBean(VideoPlatformInstanceService.class),
          context.getBean(VideoSdkAlarmProcessService.class));
    }
  }

  @Test
  void foreignTenantCannotUseRequestTenantToAuthorizeEitherRoute() {
    try (var context = context();
        var security = mockStatic(SecurityUtils.class)) {
      security.when(SecurityUtils::getUnionId).thenReturn("reader");
      context.getBean(Grants.class).allowed.add("video:platform:view");
      when(context.getBean(IoTUserService.class).selectUserByUnionId("reader"))
          .thenReturn(user("tenant-a", 1));
      when(context.getBean(VideoPlatformInstanceService.class).getByInstanceKey("isup"))
          .thenReturn(instance("tenant-b", "hik_isup"));
      var controller = context.getBean(VideoSdkAlarmProcessController.class);
      var spoofed = Map.of("deviceId", "ipc", "tenantId", "tenant-b");
      assertThrows(BaseException.class, () -> controller.list("isup", spoofed));
      assertThrows(BaseException.class, () -> controller.events("isup", "process", spoofed));
      verifyNoInteractions(context.getBean(VideoSdkAlarmProcessService.class));
    }
  }

  @Test
  void databaseInstanceIsPassedUnchangedAndLegacyUserTenantFallsBackToUnionId() {
    try (var context = context();
        var security = mockStatic(SecurityUtils.class)) {
      security.when(SecurityUtils::getUnionId).thenReturn("reader");
      context.getBean(Grants.class).allowed.add("video:platform:query");
      when(context.getBean(IoTUserService.class).selectUserByUnionId("reader"))
          .thenReturn(user(null, 1));
      var instance = instance("reader", "hik_isup");
      when(context.getBean(VideoPlatformInstanceService.class).getByInstanceKey("isup"))
          .thenReturn(instance);
      var parameters = Map.of("deviceId", "ipc", "tenantId", "foreign");
      context.getBean(VideoSdkAlarmProcessController.class).list("isup", parameters);
      verify(context.getBean(VideoSdkAlarmProcessService.class))
          .list(same(instance), same(parameters));
      assertEquals("reader", instance.getTenantId());
    }
  }

  @Test
  void adminCanReadAnotherTenantButCannotBypassProcessServiceScopeChecks() {
    try (var context = context();
        var security = mockStatic(SecurityUtils.class)) {
      security.when(SecurityUtils::getUnionId).thenReturn("reader");
      context.getBean(Grants.class).allowed.add("video:platform:view");
      when(context.getBean(IoTUserService.class).selectUserByUnionId("reader"))
          .thenReturn(user("admin-tenant", 0));
      var instance = instance("tenant-b", "hik_isup");
      when(context.getBean(VideoPlatformInstanceService.class).getByInstanceKey("isup"))
          .thenReturn(instance);
      var processes = context.getBean(VideoSdkAlarmProcessService.class);
      var controller = context.getBean(VideoSdkAlarmProcessController.class);
      assertDoesNotThrow(() -> controller.list("isup", Map.of()));
      when(processes.events(same(instance), eq("foreign-process"), anyMap()))
          .thenThrow(new BaseException("告警过程不存在或无权访问"));
      assertThrows(
          BaseException.class, () -> controller.events("isup", "foreign-process", Map.of()));
      verify(processes).list(same(instance), anyMap());
      verify(processes).events(same(instance), eq("foreign-process"), anyMap());
    }
  }

  @Test
  void nonexistentAndUnsupportedPlatformInstancesNeverReachTheProcessService() {
    try (var context = context();
        var security = mockStatic(SecurityUtils.class)) {
      security.when(SecurityUtils::getUnionId).thenReturn("reader");
      context.getBean(Grants.class).allowed.add("video:platform:view");
      when(context.getBean(IoTUserService.class).selectUserByUnionId("reader"))
          .thenReturn(user("tenant-a", 1));
      var controller = context.getBean(VideoSdkAlarmProcessController.class);
      assertThrows(BaseException.class, () -> controller.list("missing", Map.of()));
      when(context.getBean(VideoPlatformInstanceService.class).getByInstanceKey("isup"))
          .thenReturn(instance("tenant-a", "wvp"));
      assertThrows(BaseException.class, () -> controller.list("isup", Map.of()));
      when(context.getBean(VideoPlatformInstanceService.class).getByInstanceKey("isup"))
          .thenReturn(instance("tenant-a", null));
      assertThrows(BaseException.class, () -> controller.events("isup", "process", Map.of()));
      verifyNoInteractions(context.getBean(VideoSdkAlarmProcessService.class));
    }
  }
}
