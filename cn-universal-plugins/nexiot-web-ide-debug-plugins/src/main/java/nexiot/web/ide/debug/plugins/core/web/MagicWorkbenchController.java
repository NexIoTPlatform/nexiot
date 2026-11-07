package nexiot.web.ide.debug.plugins.core.web;

import cn.universal.core.engine.MagicResourceLoader;
import cn.universal.core.engine.MagicScriptEngine;
import cn.universal.core.engine.ScriptClass;
import cn.universal.core.engine.parsing.Span;
import cn.universal.core.engine.parsing.Tokenizer;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import nexiot.web.ide.debug.plugins.core.annotation.Valid;
import nexiot.web.ide.debug.plugins.core.config.Constants;
import nexiot.web.ide.debug.plugins.core.config.MagicAPIProperties;
import nexiot.web.ide.debug.plugins.core.config.MagicConfiguration;
import nexiot.web.ide.debug.plugins.core.context.MagicUser;
import nexiot.web.ide.debug.plugins.core.exception.MagicLoginException;
import nexiot.web.ide.debug.plugins.core.interceptor.Authorization;
import nexiot.web.ide.debug.plugins.core.model.JsonBean;
import nexiot.web.ide.debug.plugins.core.model.MagicEntity;
import nexiot.web.ide.debug.plugins.core.model.Options;
import nexiot.web.ide.debug.plugins.core.model.Plugin;
import nexiot.web.ide.debug.plugins.core.model.SelectedResource;
import nexiot.web.ide.debug.plugins.core.service.MagicAPIService;
import nexiot.web.ide.debug.plugins.core.servlet.MagicHttpServletRequest;
import nexiot.web.ide.debug.plugins.core.servlet.MagicHttpServletResponse;
import nexiot.web.ide.debug.plugins.modules.db.SQLModule;
import nexiot.web.ide.debug.plugins.utils.ClassScanner;
import nexiot.web.ide.debug.plugins.utils.IoUtils;
import nexiot.web.ide.debug.plugins.utils.SignUtils;
import nexiot.web.ide.debug.plugins.utils.WebUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.util.ResourceUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

public class MagicWorkbenchController extends MagicController implements MagicExceptionHandler {

  private static final Logger logger = LoggerFactory.getLogger(MagicWorkbenchController.class);

  private static final Pattern SINGLE_LINE_COMMENT_TODO =
      Pattern.compile("((TODO)|(todo)|(fixme)|(FIXME))[ \t]+[^\n]+");

  private static final Pattern MULTI_LINE_COMMENT_TODO =
      Pattern.compile("((TODO)|(todo)|(fixme)|(FIXME))[ \t]+[^\n(?!*/)]+");

  private final String secretKey;

  private final List<Plugin> plugins;

  private final MagicAPIProperties properties;

  private String allClassTxt;

  public MagicWorkbenchController(
      MagicConfiguration configuration, MagicAPIProperties properties, List<Plugin> plugins) {
    super(configuration);
    this.properties = properties;
    this.plugins = plugins;
    this.secretKey = properties.getSecretKey();
    // 给前端添加代码提示
    MagicScriptEngine.addScriptClass(SQLModule.class);
    MagicScriptEngine.addScriptClass(MagicAPIService.class);
  }

  @GetMapping({"", "/"})
  @Valid(requireLogin = false)
  public String redirectIndex(MagicHttpServletRequest request) {
    if (request.getRequestURI().endsWith("/")) {
      return "redirect:./index.html";
    }
    return "redirect:" + properties.getWeb() + "/index.html";
  }

  @GetMapping("/config.json")
  @Valid(requireLogin = false)
  @ResponseBody
  public Map<String, Object> readConfig() {
    Map<String, Object> configJson = new HashMap<>();
    configJson.put("persistenceResponseBody", properties.isPersistenceResponseBody());
    configJson.put("version", properties.getVersion());
    configJson.put("web", properties.getWeb());
    configJson.put("prefix", properties.getPrefix());
    configJson.put("autoImportModuleList", properties.getAutoImportModuleList());
    configJson.put("autoImportPackage", properties.getAutoImportPackage());
    return configJson;
  }

  @GetMapping(value = "/classes.txt", produces = "text/plain")
  @ResponseBody
  @Valid(requireLogin = false)
  private String readClass() {
    if (allClassTxt == null) {
      try {
        allClassTxt = ClassScanner.compress(ClassScanner.scan());
      } catch (Throwable t) {
        logger.warn("扫描Class失败", t);
        allClassTxt = "";
      }
    }
    return allClassTxt;
  }

  /** 获取所有class */
  @PostMapping("/classes")
  @ResponseBody
  @Valid(requireLogin = false)
  public JsonBean<Map<String, Object>> classes() {
    Map<String, ScriptClass> classMap = MagicScriptEngine.getScriptClassMap();
    classMap.putAll(MagicResourceLoader.getModules());
    Map<String, Object> values = new HashMap<>();
    values.put("classes", classMap);
    values.put("extensions", MagicScriptEngine.getExtensionScriptClass());
    values.put("functions", MagicScriptEngine.getFunctions());
    return new JsonBean<>(values);
  }

  /**
   * 获取单个class
   *
   * @param className 类名
   */
  @PostMapping("/class")
  @ResponseBody
  public JsonBean<Set<ScriptClass>> clazz(String className) {
    if (StringUtils.isBlank(className)) {
      return new JsonBean<>(Collections.emptySet());
    }
    return new JsonBean<>(MagicScriptEngine.getScriptClass(className));
  }

  /** 登录 */
  @PostMapping("/login")
  @ResponseBody
  @Valid(requireLogin = false)
  public JsonBean<Boolean> login(
      String username,
      String password,
      MagicHttpServletRequest request,
      MagicHttpServletResponse response)
      throws MagicLoginException {
    if (configuration.getAuthorizationInterceptor().requireLogin()) {
      if (StringUtils.isBlank(username) && StringUtils.isBlank(password)) {
        try {
          configuration
              .getAuthorizationInterceptor()
              .getUserByToken(request.getHeader(Constants.MAGIC_TOKEN_HEADER));
        } catch (MagicLoginException ignored) {
          return new JsonBean<>(false);
        }
      } else {
        MagicUser user = configuration.getAuthorizationInterceptor().login(username, password);
        response.setHeader(Constants.MAGIC_TOKEN_HEADER, user.getToken());
        response.setHeader(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, Constants.MAGIC_TOKEN_HEADER);
      }
    }
    return new JsonBean<>(true);
  }

  @PostMapping("/user")
  @ResponseBody
  public JsonBean<MagicUser> user(MagicHttpServletRequest request) {
    if (configuration.getAuthorizationInterceptor().requireLogin()) {
      try {
        return new JsonBean<>(
            configuration
                .getAuthorizationInterceptor()
                .getUserByToken(request.getHeader(Constants.MAGIC_TOKEN_HEADER)));
      } catch (MagicLoginException ignored) {

      }
    }
    return new JsonBean<>(MagicUser.guest());
  }

  @PostMapping("/logout")
  @ResponseBody
  @Valid(requireLogin = false)
  public JsonBean<Void> logout(MagicHttpServletRequest request) {
    configuration
        .getAuthorizationInterceptor()
        .logout(request.getHeader(Constants.MAGIC_TOKEN_HEADER));
    return new JsonBean<>();
  }

  @GetMapping("/plugins")
  @Valid(requireLogin = false)
  @ResponseBody
  public JsonBean<List<Plugin>> plugins() {
    return new JsonBean<>(plugins);
  }

  @RequestMapping("/options")
  @ResponseBody
  @Valid(requireLogin = false)
  public JsonBean<List<List<String>>> options() {
    return new JsonBean<>(
        Stream.of(Options.values())
            .map(item -> Arrays.asList(item.getValue(), item.getName(), item.getDefaultValue()))
            .collect(Collectors.toList()));
  }

  @GetMapping("/reload")
  @ResponseBody
  public JsonBean<Boolean> reload(MagicHttpServletRequest request) {
    isTrue(allowVisit(request, Authorization.RELOAD), PERMISSION_INVALID);
    MagicConfiguration.getMagicResourceService().refresh();
    return new JsonBean<>(true);
  }

  @PostMapping("/search")
  @ResponseBody
  public JsonBean<List<Map<String, Object>>> search(
      String keyword, MagicHttpServletRequest request) {
    if (StringUtils.isBlank(keyword)) {
      return new JsonBean<>(Collections.emptyList());
    }
    return new JsonBean<>(
        entities(request, Authorization.VIEW).stream()
            .filter(it -> it.getScript().contains(keyword))
            .map(
                it -> {
                  String script = it.getScript();
                  int index = script.indexOf(keyword);
                  int endIndex = script.indexOf("\n", index + keyword.length());
                  index = script.lastIndexOf("\n", index) + 1;
                  Span span = new Span(script, index, endIndex == -1 ? script.length() : endIndex);
                  return new HashMap<String, Object>() {
                    {
                      put("id", it.getId());
                      put("text", span.getText().trim());
                      put("line", span.getLine().getLineNumber());
                    }
                  };
                })
            .collect(Collectors.toList()));
  }

  @GetMapping("/todo")
  @ResponseBody
  @Valid
  public JsonBean<List<Map<String, Object>>> todo(MagicHttpServletRequest request) {
    List<MagicEntity> entities = entities(request, Authorization.VIEW);
    List<Map<String, Object>> result = new ArrayList<>(entities.size());
    for (MagicEntity entity : entities) {
      try {
        List<Span> comments = Tokenizer.tokenize(entity.getScript(), true).comments();
        for (Span comment : comments) {
          String text = comment.getText();
          Pattern pattern =
              text.startsWith("//") ? SINGLE_LINE_COMMENT_TODO : MULTI_LINE_COMMENT_TODO;
          Matcher matcher = pattern.matcher(text);
          while (matcher.find()) {
            result.add(
                new HashMap<String, Object>() {
                  {
                    put("id", entity.getId());
                    put("text", matcher.group(0).trim());
                    put("line", comment.getLine().getLineNumber());
                  }
                });
          }
        }
      } catch (Exception ignored) {
      }
    }
    return new JsonBean<>(result);
  }

  @RequestMapping(value = "/config-js")
  @ResponseBody
  @Valid(requireLogin = false)
  public void configJs(MagicHttpServletResponse response) throws IOException {
    response.setContentType("application/javascript");
    response.setCharacterEncoding("UTF-8");
    byte[] bytes = "var MAGIC_EDITOR_CONFIG = {}".getBytes();
    if (configuration.getEditorConfig() != null) {
      try {
        String path = configuration.getEditorConfig();
        if (path.startsWith(ResourceUtils.CLASSPATH_URL_PREFIX)) {
          path = path.substring(ResourceUtils.CLASSPATH_URL_PREFIX.length());
          bytes = IoUtils.bytes(new ClassPathResource(path).getInputStream());
        } else {
          File file = ResourceUtils.getFile(configuration.getEditorConfig());
          bytes = Files.readAllBytes(Paths.get(file.toURI()));
        }
      } catch (IOException e) {
        logger.warn("读取编辑器配置文件{}失败", configuration.getEditorConfig());
      }
    }
    try (OutputStream stream = response.getOutputStream()) {
      stream.write(bytes);
      stream.flush();
    }
  }

  @RequestMapping("/download")
  @ResponseBody
  @Valid(authorization = Authorization.DOWNLOAD)
  public void download(
      String groupId,
      @RequestBody(required = false) List<SelectedResource> resources,
      MagicHttpServletRequest request,
      MagicHttpServletResponse response)
      throws IOException {
    isTrue(allowVisit(request, Authorization.DOWNLOAD), PERMISSION_INVALID);
    response.setContentType(MediaType.APPLICATION_OCTET_STREAM_VALUE);
    ByteArrayOutputStream os = new ByteArrayOutputStream();
    magicAPIService.download(groupId, resources, os);
    String filename = "magic-api-all.zip";
    if (StringUtils.isBlank(groupId)) {
      filename = "magic-api-group.zip";
    }
    response.setHeader(
        HttpHeaders.CONTENT_DISPOSITION,
        "attachment; filename=" + URLEncoder.encode(filename, "UTF-8"));
    try (OutputStream stream = response.getOutputStream()) {
      stream.write(os.toByteArray());
      stream.flush();
    }
  }

  @RequestMapping("/upload")
  @Valid(readonly = false, authorization = Authorization.UPLOAD)
  @ResponseBody
  public JsonBean<Boolean> upload(MultipartFile file, String mode, MagicHttpServletRequest request)
      throws IOException {
    notNull(file, FILE_IS_REQUIRED);
    isTrue(allowVisit(request, Authorization.UPLOAD), PERMISSION_INVALID);
    if (configuration.getMagicBackupService() != null) {
      configuration.getMagicBackupService().doBackupAll("上传前，系统自动全量备份", WebUtils.currentUserName());
    }
    return new JsonBean<>(magicAPIService.upload(file.getInputStream(), mode));
  }

  @RequestMapping("/push")
  @ResponseBody
  @Valid(authorization = Authorization.PUSH)
  public JsonBean<?> push(
      @RequestHeader("magic-push-target") String target,
      @RequestHeader("magic-push-secret-key") String secretKey,
      @RequestHeader("magic-push-mode") String mode,
      @RequestBody List<SelectedResource> resources,
      MagicHttpServletRequest request) {
    isTrue(allowVisit(request, Authorization.PUSH), PERMISSION_INVALID);
    return magicAPIService.push(target, secretKey, mode, resources);
  }

  @ResponseBody
  @Valid(requireLogin = false)
  public JsonBean<Void> receivePush(MultipartFile file, String mode, Long timestamp, String sign)
      throws IOException {
    notNull(timestamp, SIGN_IS_INVALID);
    notBlank(mode, SIGN_IS_INVALID);
    notBlank(sign, SIGN_IS_INVALID);
    notNull(file, SIGN_IS_INVALID);
    byte[] bytes = IoUtils.bytes(file.getInputStream());
    isTrue(sign.equals(SignUtils.sign(timestamp, secretKey, mode, bytes)), SIGN_IS_INVALID);
    if (configuration.getMagicBackupService() != null) {
      configuration.getMagicBackupService().doBackupAll("推送前，系统自动全量备份", WebUtils.currentUserName());
    }
    magicAPIService.upload(new ByteArrayInputStream(bytes), mode);
    return new JsonBean<>();
  }
}
