package cn.universal.admin.video.web;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

import cn.universal.persistence.entity.VideoPlatformInstance;
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

class VideoPlatformPermissionTest {
  public static class Permissions {
    final Set<String> allowed = new HashSet<>();
    public boolean hasPermi(String permission) { return allowed.contains(permission); }
  }

  @Configuration
  @EnableMethodSecurity(proxyTargetClass = true)
  static class Config {
    @Bean cn.universal.security.service.IoTUserService ioTUserService() { return mock(cn.universal.security.service.IoTUserService.class); }
    @Bean("ss") Permissions permissions() { return new Permissions(); }
    @Bean cn.universal.admin.video.service.HikIsupRegistrationSync hikIsupRegistrationSync() { return mock(cn.universal.admin.video.service.HikIsupRegistrationSync.class); }
    @Bean cn.universal.admin.video.service.VideoSdkConfigurationTaskService sdkConfigurationTasks() { return mock(cn.universal.admin.video.service.VideoSdkConfigurationTaskService.class); }
    @Bean cn.universal.admin.video.alarm.VideoSdkEvidenceTimelineService sdkEvidenceTimeline() { return mock(cn.universal.admin.video.alarm.VideoSdkEvidenceTimelineService.class); }
    @Bean("videoOperationAccess") cn.universal.admin.video.service.VideoOperationAccess videoOperationAccess(Permissions grants) {
      var service = mock(cn.universal.admin.system.permission.SysPermissionService.class);
      org.mockito.Mockito.when(service.getCurrentPermissions()).thenAnswer(invocation -> grants.allowed);
      org.mockito.Mockito.when(service.hasPermission(org.mockito.ArgumentMatchers.anyString()))
          .thenAnswer(invocation -> grants.allowed.contains(invocation.getArgument(0)));
      return new cn.universal.admin.video.service.VideoOperationAccess(service);
    }
    @Bean VideoPlatformController controller() { return mock(VideoPlatformController.class); }
  }

  @AfterEach void clearAuthentication() { SecurityContextHolder.clearContext(); }

  @Test void viewingDoesNotAuthorizeWritesAndEachOperationUsesItsOwnPermission() {
    SecurityContextHolder.getContext().setAuthentication(
        new UsernamePasswordAuthenticationToken("viewer", "", java.util.List.of()));
    try (var context = new AnnotationConfigApplicationContext(Config.class)) {
      var controller = context.getBean(VideoPlatformController.class);
      var permissions = context.getBean(Permissions.class);
      Map<String, Runnable> operations = Map.of(
          "device:edit", () -> controller.updateDevice("sdk", "camera", Map.of()),
          "device:remove", () -> controller.deleteDevice("sdk", "camera"),
          "device:import", () -> controller.importSdkDevices("sdk", Map.of()),
          "subscribe", () -> controller.saveSdkSubscription("sdk", Map.of()),
          "edit", () -> controller.updateSdkDefaultMediaServer("sdk", Map.of()),
          "test", () -> controller.testConnection(Map.of("instanceKey", "sdk")),
          "sync", () -> controller.startSdkDiscovery("sdk", Map.of()));
      for (var operation : operations.entrySet()) {
        permissions.allowed.clear();
        permissions.allowed.add("video:platform:view");
        assertThrows(AccessDeniedException.class, operation.getValue()::run, operation.getKey());
        permissions.allowed.add("video:platform:" + operation.getKey());
        assertDoesNotThrow(operation.getValue()::run, operation.getKey());
      }
    }
  }

  @Test void createAndEditPlatformCannotSubstituteForEachOther() {
    SecurityContextHolder.getContext().setAuthentication(
        new UsernamePasswordAuthenticationToken("editor", "", java.util.List.of()));
    try (var context = new AnnotationConfigApplicationContext(Config.class)) {
      var controller = context.getBean(VideoPlatformController.class);
      var permissions = context.getBean(Permissions.class);
      var instance = new VideoPlatformInstance();
      permissions.allowed.add("video:platform:edit");
      assertThrows(AccessDeniedException.class, () -> controller.savePlatform(instance));
      instance.setId(1L);
      assertDoesNotThrow(() -> controller.savePlatform(instance));
      permissions.allowed.clear();
      permissions.allowed.add("video:platform:add");
      assertThrows(AccessDeniedException.class, () -> controller.savePlatform(instance));
      instance.setId(null);
      assertDoesNotThrow(() -> controller.savePlatform(instance));
    }
  }

  @Test void recordingTaskReadersCannotCreateOrCancelTasks() {
    SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("viewer", "", java.util.List.of()));
    try (var context = new AnnotationConfigApplicationContext(Config.class)) {
      var c = context.getBean(VideoPlatformController.class);
      var permissions = context.getBean(Permissions.class);
      var operations = java.util.List.<Runnable>of(
          () -> c.createRecordingTask("sdk",Map.of()), () -> c.cancelRecordingTask("sdk","id"),
          () -> c.retryRecordingTask("sdk","id",Map.of()));
      permissions.allowed.add("video:platform:view");
      assertDoesNotThrow(() -> c.recordingTasks("sdk",1,20,null));
      assertDoesNotThrow(() -> c.recordingTask("sdk","id"));
      assertDoesNotThrow(() -> c.sdkOperations("sdk"));
      operations.forEach(op -> assertThrows(AccessDeniedException.class, op::run));
      permissions.allowed.add("video:platform:edit");
      operations.forEach(op -> assertDoesNotThrow(op::run));
    }
  }

  @Test void legacyVideoRoutesSeparateQueryPlayAndControlGrants() {
    SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("reader","",java.util.List.of()));
    try(var context=new AnnotationConfigApplicationContext(Config.class)) {
      var c=context.getBean(VideoPlatformController.class);var grants=context.getBean(Permissions.class);
      var viewers=java.util.List.<Runnable>of(()->c.getStreamUrl("sdk","camera","1",Map.of()),
          ()->c.getPreviewUrl("sdk","camera","1",Map.of()),()->c.getPlaybackUrl("sdk","camera","1",Map.of()),
          ()->c.controlPlayback("sdk","camera","1",Map.of()));
      grants.allowed.add("video:platform:query");
      assertDoesNotThrow(()->c.queryRecords("sdk","camera","1",Map.of()));
      viewers.forEach(view->assertThrows(AccessDeniedException.class,view::run));
      assertThrows(AccessDeniedException.class,()->c.controlPTZ("sdk","camera","1",Map.of()));
      grants.allowed.add("video:device:play");viewers.forEach(view->assertDoesNotThrow(view::run));
      assertThrows(AccessDeniedException.class,()->c.controlPTZ("sdk","camera","1",Map.of()));
      grants.allowed.add("video:device:control");assertDoesNotThrow(()->c.controlPTZ("sdk","camera","1",Map.of()));
      grants.allowed.clear();grants.allowed.add("video:platform:edit");viewers.forEach(view->assertDoesNotThrow(view::run));
    }
  }

  @Test void legacyAlarmRoutesRequireQueryAndSubscribePermissions() {
    SecurityContextHolder.getContext().setAuthentication(
        new UsernamePasswordAuthenticationToken("viewer", "", java.util.List.of()));
    try (var context = new AnnotationConfigApplicationContext(Config.class)) {
      var controller = context.getBean(VideoPlatformController.class);
      var permissions = context.getBean(Permissions.class);
      permissions.allowed.add("video:platform:view");
      assertThrows(AccessDeniedException.class, () -> controller.alarmSubscription("isup"));
      assertThrows(AccessDeniedException.class, () -> controller.saveAlarmSubscription("isup", Map.of()));
      permissions.allowed.add("video:platform:query");
      assertDoesNotThrow(() -> controller.alarmSubscription("isup"));
      assertThrows(AccessDeniedException.class, () -> controller.saveAlarmSubscription("isup", Map.of()));
      permissions.allowed.add("video:platform:subscribe");
      assertDoesNotThrow(() -> controller.saveAlarmSubscription("isup", Map.of()));
    }
  }

  @Test void gbConfigurationReadsAndWritesUseSeparatePermissionsForBothRoutes() {
    SecurityContextHolder.getContext().setAuthentication(
        new UsernamePasswordAuthenticationToken("viewer", "", java.util.List.of()));
    try (var context = new AnnotationConfigApplicationContext(Config.class)) {
      var controller = context.getBean(VideoPlatformController.class);
      var permissions = context.getBean(Permissions.class);
      permissions.allowed.add("video:platform:query");
      assertDoesNotThrow(() -> controller.wvpConfigurationCapabilities("wvp", Map.of()));
      assertDoesNotThrow(() -> controller.wvpConfigurationRead("wvp", Map.of()));
      assertDoesNotThrow(() -> controller.wvpConfigurationStatus("wvp", Map.of()));
      assertDoesNotThrow(() -> controller.executePlatformOperation("wvp", "wvpConfigRead", Map.of()));
      assertThrows(AccessDeniedException.class, () -> controller.wvpConfigurationApply("wvp", Map.of()));
      assertThrows(AccessDeniedException.class,
          () -> controller.executePlatformOperation("wvp", "wvpConfigApply", Map.of()));
      permissions.allowed.add("video:device:config");
      assertDoesNotThrow(() -> controller.wvpConfigurationApply("wvp", Map.of()));
      assertDoesNotThrow(() -> controller.executePlatformOperation("wvp", "wvpConfigApply", Map.of()));
    }
  }
}
