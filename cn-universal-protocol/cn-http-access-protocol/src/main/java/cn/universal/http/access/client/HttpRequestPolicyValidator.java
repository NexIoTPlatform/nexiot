package cn.universal.http.access.client;

import cn.universal.http.access.config.HttpIntegrationConfig.HttpOutboundPolicy;
import cn.universal.http.access.model.HttpRequestSpec;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.URI;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

/** Enforces a non-bypassable outbound boundary around driver-produced request specifications. */
@Component
public class HttpRequestPolicyValidator {

  public URI validateAndResolve(HttpOutboundPolicy policy, HttpRequestSpec spec) {
    if (spec == null || blank(spec.getUrl())) {
      throw new IllegalArgumentException("Driver must provide an absolute outbound URL");
    }
    URI uri;
    try {
      uri = URI.create(spec.getUrl());
    } catch (Exception e) {
      throw new IllegalArgumentException("Outbound URL is invalid", e);
    }
    if (!("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme()))
        || uri.getUserInfo() != null || blank(uri.getHost())) {
      throw new IllegalArgumentException("Outbound URL must be absolute HTTP(S) without user info");
    }
    if (spec.getQuery() != null && !spec.getQuery().isEmpty()) {
      UriComponentsBuilder builder = UriComponentsBuilder.fromUri(uri);
      spec.getQuery().forEach(builder::queryParam);
      uri = builder.build().encode().toUri();
    }
    validateAddresses(uri.getHost(), policy.isAllowPrivateNetwork());
    return uri;
  }

  private void validateAddresses(String host, boolean allowPrivateNetwork) {
    try {
      for (InetAddress address : InetAddress.getAllByName(host)) {
        if (address.isAnyLocalAddress()
            || address.isLoopbackAddress()
            || address.isLinkLocalAddress()
            || address.isMulticastAddress()
            || (!allowPrivateNetwork && isPrivate(address))) {
          throw new IllegalArgumentException("Outbound address is blocked by network policy");
        }
      }
    } catch (IllegalArgumentException e) {
      throw e;
    } catch (Exception e) {
      throw new IllegalArgumentException("Unable to resolve outbound host", e);
    }
  }

  private boolean isPrivate(InetAddress address) {
    byte[] bytes = address.getAddress();
    if (address instanceof Inet4Address) {
      int first = bytes[0] & 0xff;
      int second = bytes[1] & 0xff;
      return first == 10 || (first == 172 && second >= 16 && second <= 31)
          || (first == 192 && second == 168) || (first == 100 && second >= 64 && second <= 127)
          || first == 0;
    }
    if (address instanceof Inet6Address) {
      return ((bytes[0] & 0xff) & 0xfe) == 0xfc;
    }
    return true;
  }

  private boolean blank(String value) {
    return value == null || value.isBlank();
  }
}
