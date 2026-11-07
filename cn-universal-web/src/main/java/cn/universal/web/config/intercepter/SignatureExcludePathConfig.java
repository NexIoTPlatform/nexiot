/*
 * 签名校验排除路径配置
 */
package cn.universal.web.config.intercepter;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/** 统一维护需要排除签名校验的路径白名单 */
@Slf4j
@Component
@ConfigurationProperties(prefix = "security.request-sign")
public class SignatureExcludePathConfig {

  /** 从配置文件注入的自定义排除路径列表 配置示例：security.request-sign.exclude-paths[0]=/admin/custom/path */
  private List<String> excludePaths;

  /** 缓存转换后的正则表达式Pattern集合（避免每次匹配都重新转换，提升性能） */
  private Set<Pattern> excludePatterns;

  /**
   * 获取需要排除签名校验的完整正则Pattern集合（硬编码 + 配置文件注入）
   *
   * @return 排除路径对应的Pattern集合
   */
  public Set<Pattern> getAllExcludePatterns() {
    // 双重检查锁，保证懒加载和线程安全
    if (excludePatterns == null) {
      synchronized (this) {
        if (excludePatterns == null) {
          Set<String> allPaths = new HashSet<>();

          // ============ 硬编码的默认排除路径 ============
          // 文件上传/下载接口
          allPaths.add("/admin/system/oss/upload");
          allPaths.add("/admin/v1/product/uploadImage");
          // OAuth和登录接口
          allPaths.add("/oauth/token");
          allPaths.add("/login");
          // 公开视频分享接口需要支持 EventSource，浏览器原生连接无法附带自定义签名头。
          allPaths.add("/api/video/public/**");
          // SDK 网关告警回调使用独立共享 token 鉴权，不能依赖前端请求签名头。
          allPaths.add("/api/video/sdk-events/**");
          allPaths.add("/api/video/hosting/**");
          // Node-RED Edge Agent 使用独立共享 token 和隧道 token 鉴权，不能依赖前端请求签名头。
          allPaths.add("/admin/v1/edge/**");
          allPaths.add("/admin/v1/node-red/tunnel/**");
          // Video AI Runtime Agent 使用 enrollment token / node token 鉴权，不能依赖前端请求签名头。
          allPaths.add("/admin/v1/video-ai/runtime-nodes/agent/**");
          // 通配符匹配（支持 后缀、前缀、包含 三种格式）
          allPaths.add("**/export");
          allPaths.add("**/openapi/**"); // 改造后支持包含匹配
          // ============ 配置文件注入的自定义路径 ============
          if (excludePaths != null && !excludePaths.isEmpty()) {
            allPaths.addAll(excludePaths);
            log.info("[签名排除路径配置] 已加载配置文件自定义路径: {}", excludePaths);
          }

          // 将所有通配符路径转换为正则Pattern
          excludePatterns = new HashSet<>();
          for (String path : allPaths) {
            Pattern pattern = convertPathToPattern(path);
            excludePatterns.add(pattern);
          }
        }
      }
    }
    return excludePatterns;
  }

  /**
   * 将自定义通配符路径转换为正则表达式Pattern
   *
   * @param path 自定义通配符路径
   * @return 对应的正则Pattern
   */
  private Pattern convertPathToPattern(String path) {
    if (path == null || path.isEmpty()) {
      return Pattern.compile("");
    }

    // 先初始化正则字符串（不先整体quote，避免通配符被转义后无法替换）
    String regex = path;

    // 替换自定义通配符为正则表达式（优先级：包含匹配 > 后缀匹配 > 前缀匹配）
    // 注意：先处理前后都有**的场景，避免被单一通配符替换覆盖
    if (path.startsWith("**") && path.endsWith("**")) {
      // 处理 **/xxx/** 格式（包含匹配）
      String middle = path.substring(2, path.length() - 2);
      // 仅转义中间部分的特殊字符，避免/被转义影响路径匹配
      String middleRegex = Pattern.quote(middle);
      regex = ".*" + middleRegex + ".*";
    } else if (path.startsWith("**")) {
      // 处理 **/xxx 格式（后缀匹配）
      String suffix = path.substring(2);
      String suffixRegex = Pattern.quote(suffix);
      regex = ".*" + suffixRegex + "$";
    } else if (path.endsWith("/**")) {
      // 处理 /xxx/** 格式（前缀匹配）
      String prefix = path.substring(0, path.length() - 3);
      String prefixRegex = Pattern.quote(prefix);
      // 正则^表示开头，.*表示后续任意字符（精准前缀匹配）
      regex = "^" + prefixRegex + ".*";
    }

    // 最后：对整个正则中的普通路径特殊字符进行转义（避免.、?等影响）
    // 修正：移除重复转义，仅对非通配符部分进行转义（已在上面分步骤处理）
    // 编译为Pattern，仅匹配路径本身（不忽略大小写，路径匹配通常大小写敏感）
    return Pattern.compile(regex);
  }

  /**
   * 判断指定路径是否应该排除签名校验（正则匹配版本）
   *
   * @param uri 请求URI
   * @return true表示应该排除，false表示应该进行签名校验
   */
  public boolean isExcludePath(String uri) {
    if (uri == null || uri.isEmpty()) {
      return false;
    }

    Set<Pattern> allExcludePatterns = getAllExcludePatterns();
    for (Pattern pattern : allExcludePatterns) {
      if (pattern.matcher(uri).matches()) {
        return true;
      }
    }

    return false;
  }

  // ============ Getter & Setter ============
  public List<String> getExcludePaths() {
    return excludePaths;
  }

  public void setExcludePaths(List<String> excludePaths) {
    this.excludePaths = excludePaths;
  }
}
