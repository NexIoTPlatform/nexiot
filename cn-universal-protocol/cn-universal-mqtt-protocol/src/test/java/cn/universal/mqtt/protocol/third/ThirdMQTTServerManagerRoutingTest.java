package cn.universal.mqtt.protocol.third;

import static cn.universal.mqtt.protocol.config.MqttConstant.TopicCategory.PASSTHROUGH;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import cn.universal.common.config.InstanceIdProvider;
import cn.universal.dm.device.service.impl.IoTProductDeviceService;
import cn.universal.mqtt.protocol.entity.MQTTProductConfig;
import cn.universal.mqtt.protocol.entity.MQTTUPRequest;
import cn.universal.mqtt.protocol.metrics.MqttMetricsMananer;
import cn.universal.mqtt.protocol.processor.MqttUPProcessorChain;
import cn.universal.mqtt.protocol.ssl.MqttTlsSupport;
import cn.universal.mqtt.protocol.topic.MQTTTopicManager;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import org.eclipse.paho.mqttv5.client.*;
import org.eclipse.paho.mqttv5.common.MqttMessage;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

class ThirdMQTTServerManagerRoutingTest {
  @ParameterizedTest
  @ValueSource(strings = {"reconnect", "restart", "takeover"})
  @SuppressWarnings("unchecked")
  void receivesCorrectCategoryAfterConnectionLifecycle(String lifecycle) throws Exception {
    String network = "network1", product = "7Gc9jZqGthEz";
    String topic = "hongfa/FF8A050101005543/upload/";
    ThirdMQTTServerManager manager = new ThirdMQTTServerManager();
    var config =
        MQTTProductConfig.builder()
            .networkUnionId(network)
            .host("tcp://localhost:1883")
            .clusterEnabled(true)
            .autoReconnect(true)
            .cleanSession(true)
            .keepAliveInterval(60)
            .connectTimeout(10)
            .subscribeTopics(
                List.of(
                    MQTTProductConfig.MqttTopicConfig.builder()
                        .topic(topic)
                        .productKey(product)
                        .topicCategory(PASSTHROUGH)
                        .qos(1)
                        .enabled(true)
                        .build()))
            .build();
    var configs =
        (Map<String, MQTTProductConfig>) ReflectionTestUtils.getField(manager, "networkConfigs");
    var clients =
        (Map<String, MqttAsyncClient>) ReflectionTestUtils.getField(manager, "networkClients");
    configs.put(network, config);
    var configService = mock(ThirdMQTTConfigService.class);
    when(configService.loadAllConfigs()).thenReturn(Map.of(network, config));
    when(configService.supportMQTTNetwork(product, network)).thenReturn(true);
    ReflectionTestUtils.setField(manager, "configService", configService);
    ReflectionTestUtils.setField(manager, "mqttTopicManager", new MQTTTopicManager());
    var tls = mock(MqttTlsSupport.class);
    when(tls.resolveBrokerUri(anyString(), any())).thenReturn(config.getHost());
    ReflectionTestUtils.setField(manager, "mqttTlsSupport", tls);
    var instance = mock(InstanceIdProvider.class);
    when(instance.getInstanceId()).thenReturn("test-node");
    ReflectionTestUtils.setField(manager, "instanceIdProvider", instance);
    ReflectionTestUtils.setField(manager, "maxInflight", 500);
    ReflectionTestUtils.setField(manager, "metricsCollector", mock(MqttMetricsMananer.class));
    var products = mock(IoTProductDeviceService.class);
    when(products.getProductDecoderType(product)).thenReturn("STRING");
    ReflectionTestUtils.setField(manager, "iotProductDeviceService", products);
    var executor = mock(ExecutorService.class);
    doAnswer(
            invocation -> {
              invocation.getArgument(0, Callable.class).call();
              return null;
            })
        .when(executor)
        .submit(any(Callable.class));
    ReflectionTestUtils.setField(manager, "executorService", executor);
    var chain = mock(MqttUPProcessorChain.class);
    ReflectionTestUtils.setField(manager, "processorChain", chain);
    try (var construction =
        mockConstruction(
            MqttAsyncClient.class,
            (client, context) -> {
              when(client.isConnected()).thenReturn(true);
              doAnswer(
                      invocation -> {
                        invocation
                            .getArgument(2, MqttActionListener.class)
                            .onSuccess(mock(IMqttToken.class));
                        return mock(IMqttToken.class);
                      })
                  .when(client)
                  .connect(
                      any(MqttConnectionOptions.class), isNull(), any(MqttActionListener.class));
            })) {
      var oldClient = mock(MqttAsyncClient.class);
      clients.put(network, oldClient);
      var callbackType =
          Class.forName(ThirdMQTTServerManager.class.getName() + "$MqttCallbackHandler");
      var callbackConstructor =
          callbackType.getDeclaredConstructor(
              ThirdMQTTServerManager.class,
              String.class,
              MQTTProductConfig.class,
              MqttAsyncClient.class);
      callbackConstructor.setAccessible(true);
      MqttCallback staleCallback =
          (MqttCallback) callbackConstructor.newInstance(manager, network, config, oldClient);
      switch (lifecycle) {
        case "reconnect" ->
            ReflectionTestUtils.invokeMethod(manager, "reconnectClient", network, config);
        case "restart" -> assertTrue(manager.restartMqttClient(network));
        case "takeover" -> {
          assertTrue(
              Boolean.TRUE.equals(
                  ReflectionTestUtils.invokeMethod(manager, "stopMqttClient", network, true)));
          assertTrue(
              Boolean.TRUE.equals(
                  ReflectionTestUtils.invokeMethod(manager, "initializeClient", network, config)));
        }
        default -> fail("unexpected lifecycle");
      }
      assertEquals(1, construction.constructed().size());
      var client = construction.constructed().get(0);
      var callback = ArgumentCaptor.forClass(MqttCallback.class);
      verify(client).setCallback(callback.capture());
      var message =
          new MqttMessage(
              "{\"GSN\":\"FF8A050101005543\",\"GW_Respond\":\"73BB01090005FF04020000\"}"
                  .getBytes(java.nio.charset.StandardCharsets.UTF_8));
      staleCallback.messageArrived(topic, message);
      verifyNoInteractions(chain);
      callback.getValue().messageArrived(topic, message);
      var request = ArgumentCaptor.forClass(MQTTUPRequest.class);
      verify(chain).process(request.capture());
      assertEquals(product, request.getValue().getProductKey());
      assertEquals(PASSTHROUGH, request.getValue().getTopicCategory());
      assertEquals(PASSTHROUGH, request.getValue().getContextValue("topicCategory"));
      assertEquals(PASSTHROUGH, manager.resolveTopicCategory(network, topic));
    } finally {
      ((ExecutorService) ReflectionTestUtils.getField(manager, "scheduler")).shutdownNow();
      ((ExecutorService) ReflectionTestUtils.getField(manager, "startupExecutor")).shutdownNow();
    }
  }
}
