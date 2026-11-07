package nexiot.web.ide.debug.plugins.core.service.impl;

import cn.universal.core.engine.MagicScriptContext;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import nexiot.web.ide.debug.plugins.core.annotation.MagicModule;
import nexiot.web.ide.debug.plugins.core.config.Constants;
import nexiot.web.ide.debug.plugins.core.config.JsonCodeConstants;
import nexiot.web.ide.debug.plugins.core.config.WebSocketSessionManager;
import nexiot.web.ide.debug.plugins.core.context.RequestEntity;
import nexiot.web.ide.debug.plugins.core.event.EventAction;
import nexiot.web.ide.debug.plugins.core.event.MagicEvent;
import nexiot.web.ide.debug.plugins.core.exception.MagicAPIException;
import nexiot.web.ide.debug.plugins.core.handler.MagicWebSocketDispatcher;
import nexiot.web.ide.debug.plugins.core.interceptor.ResultProvider;
import nexiot.web.ide.debug.plugins.core.model.ApiInfo;
import nexiot.web.ide.debug.plugins.core.model.JsonBean;
import nexiot.web.ide.debug.plugins.core.model.MagicNotify;
import nexiot.web.ide.debug.plugins.core.model.PathMagicEntity;
import nexiot.web.ide.debug.plugins.core.model.SelectedResource;
import nexiot.web.ide.debug.plugins.core.service.MagicAPIService;
import nexiot.web.ide.debug.plugins.core.service.MagicResourceService;
import nexiot.web.ide.debug.plugins.core.servlet.MagicRequestContextHolder;
import nexiot.web.ide.debug.plugins.function.model.FunctionInfo;
import nexiot.web.ide.debug.plugins.function.service.FunctionMagicDynamicRegistry;
import nexiot.web.ide.debug.plugins.utils.PathUtils;
import nexiot.web.ide.debug.plugins.utils.ScriptManager;
import nexiot.web.ide.debug.plugins.utils.SignUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

@MagicModule("magic")
public class DefaultMagicAPIService implements MagicAPIService, JsonCodeConstants {

  private static final Logger logger = LoggerFactory.getLogger(DefaultMagicAPIService.class);
  private final boolean throwException;
  private final ResultProvider resultProvider;
  private final String instanceId;
  private final MagicResourceService resourceService;
  private final ApplicationEventPublisher publisher;
  private final RequestMagicDynamicRegistry requestMagicDynamicRegistry;
  private final FunctionMagicDynamicRegistry functionMagicDynamicRegistry;
  private final String prefix;
  private final MagicRequestContextHolder magicRequestHolder;

  public DefaultMagicAPIService(
      ResultProvider resultProvider,
      String instanceId,
      MagicResourceService resourceService,
      RequestMagicDynamicRegistry requestMagicDynamicRegistry,
      FunctionMagicDynamicRegistry functionMagicDynamicRegistry,
      boolean throwException,
      String prefix,
      MagicRequestContextHolder magicRequestHolder,
      ApplicationEventPublisher publisher) {
    this.resultProvider = resultProvider;
    this.requestMagicDynamicRegistry = requestMagicDynamicRegistry;
    this.functionMagicDynamicRegistry = functionMagicDynamicRegistry;
    this.throwException = throwException;
    this.resourceService = resourceService;
    this.instanceId = instanceId;
    this.prefix = StringUtils.defaultIfBlank(prefix, "");
    this.magicRequestHolder = magicRequestHolder;
    this.publisher = publisher;
  }

  @SuppressWarnings({"unchecked"})
  private <T> T execute(
      RequestEntity requestEntity, PathMagicEntity info, Map<String, Object> context) {

    MagicScriptContext scriptContext = new MagicScriptContext();
    String fullGroupName = resourceService.getGroupName(info.getGroupId());
    String fullGroupPath = resourceService.getGroupPath(info.getGroupId());
    String scriptName =
        PathUtils.replaceSlash(
            String.format(
                "/%s/%s(/%s/%s)", fullGroupName, info.getName(), fullGroupPath, info.getPath()));
    scriptContext.setScriptName(scriptName);
    scriptContext.putMapIntoContext(context);
    if (requestEntity != null) {
      requestEntity.setMagicScriptContext(scriptContext);
    }
    return (T) ScriptManager.executeScript(info.getScript(), scriptContext);
  }

  @Override
  public <T> T execute(String method, String path, Map<String, Object> context) {
    return execute(null, method, path, context);
  }

  private <T> T execute(
      RequestEntity requestEntity, String method, String path, Map<String, Object> context) {
    String mappingKey =
        Objects.toString(method, "GET").toUpperCase()
            + ":"
            + PathUtils.replaceSlash(this.prefix + "/" + Objects.toString(path, ""));
    ApiInfo info = requestMagicDynamicRegistry.getMapping(mappingKey);
    if (info == null) {
      throw new MagicAPIException(String.format("找不到对应接口 [%s:%s]", method, path));
    }
    if (context == null) {
      context = new HashMap<>();
    }
    context.put("apiInfo", info);
    return execute(requestEntity, info, context);
  }

  @SuppressWarnings({"unchecked"})
  @Override
  public <T> T call(String method, String path, Map<String, Object> context) {
    RequestEntity requestEntity = RequestEntity.create();
    try {

      requestEntity
          .request(magicRequestHolder.getRequest())
          .response(magicRequestHolder.getResponse());
      return (T)
          resultProvider.buildResult(
              requestEntity, (Object) execute(requestEntity, method, path, context));
    } catch (Throwable root) {
      if (throwException) {
        throw root;
      }
      return (T) resultProvider.buildResult(requestEntity, root);
    }
  }

  @SuppressWarnings({"unchecked"})
  @Override
  public <T> T invoke(String path, Map<String, Object> context) {
    FunctionInfo functionInfo = functionMagicDynamicRegistry.getMapping(path);
    if (functionInfo == null) {
      throw new MagicAPIException(String.format("找不到对应函数 [%s]", path));
    }
    return (T) execute(null, functionInfo, context);
  }

  @Override
  public boolean upload(InputStream inputStream, String mode) throws IOException {
    return resourceService.upload(inputStream, Constants.UPLOAD_MODE_FULL.equals(mode));
  }

  @Override
  public void download(String groupId, List<SelectedResource> resources, OutputStream os)
      throws IOException {
    resourceService.export(groupId, resources, os);
  }

  @Override
  public JsonBean<?> push(
      String target, String secretKey, String mode, List<SelectedResource> resources) {
    notBlank(target, TARGET_IS_REQUIRED);
    notBlank(secretKey, SECRET_KEY_IS_REQUIRED);
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    try {
      download(null, resources, baos);
    } catch (IOException e) {
      return new JsonBean<>(-1, e.getMessage());
    }
    byte[] bytes = baos.toByteArray();
    long timestamp = System.currentTimeMillis();
    RestTemplate restTemplate = new RestTemplate();
    MultiValueMap<String, Object> param = new LinkedMultiValueMap<>();
    param.add("timestamp", timestamp);
    param.add("mode", mode);
    param.add("sign", SignUtils.sign(timestamp, secretKey, mode, bytes));
    param.add(
        "file",
        new InputStreamResource(new ByteArrayInputStream(bytes)) {
          @Override
          public String getFilename() {
            return "magic-api.zip";
          }

          @Override
          public long contentLength() {
            return bytes.length;
          }
        });
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.MULTIPART_FORM_DATA);
    return restTemplate.postForObject(target, new HttpEntity<>(param, headers), JsonBean.class);
  }

  @Override
  public boolean processNotify(MagicNotify magicNotify) {
    if (magicNotify == null || instanceId.equals(magicNotify.getFrom())) {
      return false;
    }
    logger.debug("收到通知消息:{}", magicNotify);
    switch (magicNotify.getAction()) {
      case WS_C_S:
        return processWebSocketMessageReceived(magicNotify.getClientId(), magicNotify.getContent());
      case WS_S_C:
        return processWebSocketSendMessage(magicNotify.getClientId(), magicNotify.getContent());
      case WS_S_S:
        return processWebSocketEventMessage(magicNotify.getContent());
      case CLEAR:
        publisher.publishEvent(
            new MagicEvent("clear", EventAction.CLEAR, Constants.EVENT_SOURCE_NOTIFY));
    }
    return resourceService.processNotify(magicNotify);
  }

  private boolean processWebSocketSendMessage(String clientId, String content) {
    WebSocketSessionManager.sendByClientId(clientId, content);
    return true;
  }

  private boolean processWebSocketMessageReceived(String clientId, String content) {
    MagicWebSocketDispatcher.processMessageReceived(clientId, content);
    return true;
  }

  private boolean processWebSocketEventMessage(String content) {
    MagicWebSocketDispatcher.processWebSocketEventMessage(content);
    return true;
  }
}
