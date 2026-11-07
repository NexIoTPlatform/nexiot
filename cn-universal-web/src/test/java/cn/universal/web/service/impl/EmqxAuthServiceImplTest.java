package cn.universal.web.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import cn.universal.common.constant.IoTConstant;
import cn.universal.persistence.base.IoTDeviceLifeCycle;
import cn.universal.persistence.entity.IoTDevice;
import cn.universal.persistence.entity.IoTProduct;
import cn.universal.persistence.entity.IoTUser;
import cn.universal.persistence.entity.IoTUserApplication;
import cn.universal.web.dto.EmqxAuthRequest;
import cn.universal.web.dto.EmqxAuthResponse;
import cn.universal.web.service.EmqxAuthLogService;
import cn.universal.web.service.EmqxAuthQueryService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

class EmqxAuthServiceImplTest {

  private static final String PRODUCT_KEY = "pk001";
  private static final String DEVICE_ID = "dev001";
  private static final String APP_ID = "app001";

  @Test
  void deviceAclAllowsOnlyOwnDeviceTopicsAndDeniesFallbacks() {
    EmqxAuthQueryService queryService = mock(EmqxAuthQueryService.class);
    when(queryService.queryProductByKey(PRODUCT_KEY)).thenReturn(product(PRODUCT_KEY, "productSecret"));
    when(queryService.queryDeviceByProductKeyAndDeviceId(PRODUCT_KEY, DEVICE_ID))
        .thenReturn(device("deviceSecret"));

    EmqxAuthResponse response =
        service(queryService)
            .authenticate(request(PRODUCT_KEY, "deviceSecret", PRODUCT_KEY + "." + DEVICE_ID));

    assertEquals("allow", response.getResult());
    assertAllows(response, "$thing/up/property/pk001/dev001/#", "publish");
    assertAllows(response, "eq $thing/down/pk001/dev001/#", "subscribe");
    assertDenies(response, "$thing/#");
    assertDenies(response, "$ota/#");
    assertFalse(allows(response, "$thing/up/property/pk001/dev002/#", "publish"));
  }

  @Test
  void productAclAllowsOnlyOwnProductTopicsAndDeniesFallbacks() {
    EmqxAuthQueryService queryService = mock(EmqxAuthQueryService.class);
    when(queryService.queryProductByKey(PRODUCT_KEY)).thenReturn(product(PRODUCT_KEY, "productSecret"));

    EmqxAuthResponse response =
        service(queryService).authenticate(request(PRODUCT_KEY, "productSecret", "product-client"));

    assertEquals("allow", response.getResult());
    assertAllows(response, "$thing/up/property/pk001/#", "publish");
    assertAllows(response, "eq $thing/down/pk001/#", "subscribe");
    assertDenies(response, "$thing/#");
    assertDenies(response, "#");
    assertFalse(allows(response, "$thing/up/property/other/#", "publish"));
  }

  @Test
  void applicationAclAllowsOnlyOwnApplicationSubscriptionAndDeniesFallbacks() {
    EmqxAuthQueryService queryService = mock(EmqxAuthQueryService.class);
    when(queryService.queryApplicationById(APP_ID)).thenReturn(application(APP_ID, "appSecret"));

    EmqxAuthResponse response =
        service(queryService).authenticate(request(APP_ID, "appSecret", "app-client"));

    assertEquals("allow", response.getResult());
    assertAllows(response, "$thing/app001/#", "subscribe");
    assertAllows(response, "eq $thing/app001/#", "subscribe");
    assertDenies(response, "$thing/#");
    assertFalse(allows(response, "$thing/other/#", "subscribe"));
  }

  @Test
  void userAclAllowsOnlyBoundApplicationsAndDeniesFallbacks() {
    EmqxAuthQueryService queryService = mock(EmqxAuthQueryService.class);
    IoTUser user = new IoTUser();
    user.setPassword(new BCryptPasswordEncoder().encode("userSecret"));
    when(queryService.queryUserByUsername("alice")).thenReturn(user);
    when(queryService.queryApplicationsByUnionId("alice"))
        .thenReturn(List.of(application(APP_ID, "appSecret")));

    EmqxAuthResponse response =
        service(queryService).authenticate(request("alice", "userSecret", "user-client"));

    assertEquals("allow", response.getResult());
    assertAllows(response, "$thing/app001/#", "subscribe");
    assertDenies(response, "$thing/#");
    assertFalse(allows(response, "$thing/other/#", "subscribe"));
  }

  private EmqxAuthServiceImpl service(EmqxAuthQueryService queryService) {
    EmqxAuthServiceImpl service = new EmqxAuthServiceImpl(
        queryService, mock(EmqxAuthLogService.class), mock(IoTDeviceLifeCycle.class));
    ReflectionTestUtils.setField(service, "thingPrefix", "$thing");
    return service;
  }

  private EmqxAuthRequest request(String username, String password, String clientId) {
    EmqxAuthRequest request = new EmqxAuthRequest();
    request.setUsername(username);
    request.setPassword(password);
    request.setClientid(clientId);
    return request;
  }

  private IoTProduct product(String productKey, String secret) {
    IoTProduct product = new IoTProduct();
    product.setProductKey(productKey);
    product.setProductSecret(secret);
    product.setState((byte) 0);
    product.setIsDeleted(0);
    return product;
  }

  private IoTDevice device(String secret) {
    IoTDevice device = new IoTDevice();
    device.setDeviceSecret(secret);
    return device;
  }

  private IoTUserApplication application(String appId, String secret) {
    IoTUserApplication application = new IoTUserApplication();
    application.setAppId(appId);
    application.setAppSecret(secret);
    application.setAppStatus(0);
    application.setDeleted(0);
    application.setMqttEnable(IoTConstant.ONE_TRUE);
    return application;
  }

  private void assertAllows(EmqxAuthResponse response, String topic, String action) {
    assertTrue(allows(response, topic, action), "expected allow " + action + " " + topic);
  }

  private void assertDenies(EmqxAuthResponse response, String topic) {
    assertTrue(
        response.getAcl().stream()
            .anyMatch(rule -> topic.equals(rule.getTopic()) && "deny".equals(rule.getPermission())),
        "expected deny fallback for " + topic);
  }

  private boolean allows(EmqxAuthResponse response, String topic, String action) {
    assertNotNull(response.getAcl());
    return response.getAcl().stream()
        .anyMatch(
            rule ->
                topic.equals(rule.getTopic())
                    && action.equals(rule.getAction())
                    && "allow".equals(rule.getPermission()));
  }
}
