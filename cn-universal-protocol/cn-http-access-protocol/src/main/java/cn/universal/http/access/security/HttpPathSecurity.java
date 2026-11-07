package cn.universal.http.access.security;

import cn.universal.http.access.web.HttpAccessException;
import java.util.Locale;
import org.springframework.stereotype.Component;

@Component
public class HttpPathSecurity {

  public void validateRawUri(String rawUri, String routePath) {
    String raw = rawUri == null ? "" : rawUri.toLowerCase(Locale.ROOT);
    if (raw.contains("%2f")
        || raw.contains("%5c")
        || raw.contains("%25")
        || raw.contains("\\")
        || raw.contains("//")) {
      throw badPath();
    }
    if (routePath == null || !routePath.startsWith("/") || routePath.contains("\\")
        || routePath.contains("//")) {
      throw badPath();
    }
    for (String segment : routePath.split("/", -1)) {
      if (".".equals(segment) || "..".equals(segment)) {
        throw badPath();
      }
    }
  }

  private HttpAccessException badPath() {
    return new HttpAccessException(400, "INVALID_PATH", "Invalid request path");
  }
}
