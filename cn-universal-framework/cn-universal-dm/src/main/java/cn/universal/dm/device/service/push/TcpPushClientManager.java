package cn.universal.dm.device.service.push;

import cn.hutool.core.util.StrUtil;
import cn.universal.persistence.entity.bo.UPPushBO;
import jakarta.annotation.PreDestroy;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import javax.net.SocketFactory;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class TcpPushClientManager {

  private static final int DEFAULT_CONNECT_TIMEOUT_MILLIS = 5000;
  private static final int DEFAULT_WRITE_TIMEOUT_MILLIS = 3000;
  private static final int MAX_DELIMITER_CHARACTERS = 16;

  private final Map<String, Connection> connections = new ConcurrentHashMap<>();
  private final Map<String, Object> locks = new ConcurrentHashMap<>();
  private final ExecutorService writerExecutor =
      Executors.newThreadPerTaskExecutor(Thread.ofVirtual().name("tcp-push-writer-", 0).factory());

  public void send(String appUniqueId, UPPushBO.TcpPushConfig config, String payload)
      throws Exception {
    validate(config);
    byte[] bytes = frame(config, payload);
    long start = System.currentTimeMillis();
    log.debug(
        "[TCP推送] 准备发送: appUniqueId={}, endpoint={}:{}, connectionMode={}, tls={}, frameMode={}, bytes={}",
        appUniqueId,
        config.getHost(),
        config.getPort(),
        config.getConnectionMode(),
        config.isTls(),
        config.getFrameMode(),
        bytes.length);
    if ("SHORT".equalsIgnoreCase(config.getConnectionMode())) {
      try (Socket socket = openSocket(config)) {
        write(socket, bytes, timeout(config.getWriteTimeoutMillis(), DEFAULT_WRITE_TIMEOUT_MILLIS));
      }
      log.debug(
          "[TCP推送] 短连接发送完成: appUniqueId={}, endpoint={}:{}, payload={}, elapsedMs={}",
          appUniqueId,
          config.getHost(),
          config.getPort(),
          payload,
          System.currentTimeMillis() - start);
      return;
    }
    Connection connection = connection(appUniqueId, config);
    synchronized (connection) {
      if (!connection.isUsable()) {
        connection.reconnect(config);
      }
      try {
        write(
            connection.socket,
            bytes,
            timeout(config.getWriteTimeoutMillis(), DEFAULT_WRITE_TIMEOUT_MILLIS));
      } catch (Exception firstFailure) {
        log.debug(
            "[TCP推送] 长连接写入失败，准备重连后重试: appUniqueId={}, endpoint={}:{}, error={}",
            appUniqueId,
            config.getHost(),
            config.getPort(),
            firstFailure.getMessage(),
            firstFailure);
        connection.reconnect(config);
        write(
            connection.socket,
            bytes,
            timeout(config.getWriteTimeoutMillis(), DEFAULT_WRITE_TIMEOUT_MILLIS));
      }
    }
    log.debug(
        "[TCP推送] 长连接发送完成: appUniqueId={}, endpoint={}:{}, payload={}, elapsedMs={}",
        appUniqueId,
        config.getHost(),
        config.getPort(),
        payload,
        System.currentTimeMillis() - start);
  }

  public void testConnection(UPPushBO.TcpPushConfig config) throws Exception {
    validate(config);
    long start = System.currentTimeMillis();
    log.debug(
        "[TCP推送] 开始测试连接: endpoint={}:{}, tls={}, timeoutMs={}",
        config.getHost(),
        config.getPort(),
        config.isTls(),
        timeout(config.getConnectTimeoutMillis(), DEFAULT_CONNECT_TIMEOUT_MILLIS));
    try (Socket ignored = openSocket(config)) {
      // A successful connect is sufficient; no test payload is sent here.
    }
    log.debug(
        "[TCP推送] 测试连接成功: endpoint={}:{}, elapsedMs={}",
        config.getHost(),
        config.getPort(),
        System.currentTimeMillis() - start);
  }

  public void invalidate(String appUniqueId) {
    Connection connection = connections.remove(appUniqueId);
    locks.remove(appUniqueId);
    if (connection != null) {
      connection.close();
      log.debug("[TCP推送] 已失效应用长连接: appUniqueId={}", appUniqueId);
    }
  }

  @PreDestroy
  public void close() {
    log.debug("[TCP推送] 开始关闭客户端管理器: connectionCount={}", connections.size());
    connections.values().forEach(Connection::close);
    connections.clear();
    locks.clear();
    writerExecutor.shutdownNow();
    log.debug("[TCP推送] 客户端管理器已关闭");
  }

  private Connection connection(String appUniqueId, UPPushBO.TcpPushConfig config)
      throws Exception {
    String fingerprint = fingerprint(config);
    Connection current = connections.get(appUniqueId);
    if (current != null && current.fingerprint.equals(fingerprint)) {
      log.debug("[TCP推送] 复用应用长连接: appUniqueId={}", appUniqueId);
      return current;
    }
    synchronized (locks.computeIfAbsent(appUniqueId, ignored -> new Object())) {
      current = connections.get(appUniqueId);
      if (current != null && current.fingerprint.equals(fingerprint)) {
        log.debug("[TCP推送] 复用应用长连接: appUniqueId={}", appUniqueId);
        return current;
      }
      if (current != null) {
        log.debug("[TCP推送] 配置已变化，关闭旧长连接: appUniqueId={}", appUniqueId);
        current.close();
      }
      Connection replacement = new Connection(fingerprint, openSocket(config));
      connections.put(appUniqueId, replacement);
      log.debug(
          "[TCP推送] 已创建应用长连接: appUniqueId={}, endpoint={}:{}, tls={}",
          appUniqueId,
          config.getHost(),
          config.getPort(),
          config.isTls());
      return replacement;
    }
  }

  private Socket openSocket(UPPushBO.TcpPushConfig config) throws Exception {
    SocketFactory factory =
        config.isTls() ? SSLSocketFactory.getDefault() : SocketFactory.getDefault();
    Socket socket = factory.createSocket();
    int connectTimeout = timeout(config.getConnectTimeoutMillis(), DEFAULT_CONNECT_TIMEOUT_MILLIS);
    long start = System.currentTimeMillis();
    try {
      socket.connect(new InetSocketAddress(config.getHost(), config.getPort()), connectTimeout);
      socket.setTcpNoDelay(true);
      socket.setKeepAlive(true);
      socket.setSoTimeout(connectTimeout);
      if (socket instanceof SSLSocket sslSocket) {
        sslSocket.startHandshake();
      }
    } catch (Exception e) {
      closeQuietly(socket);
      throw e;
    }
    log.debug(
        "[TCP推送] 连接建立成功: endpoint={}:{}, tls={}, elapsedMs={}",
        config.getHost(),
        config.getPort(),
        config.isTls(),
        System.currentTimeMillis() - start);
    return socket;
  }

  private void write(Socket socket, byte[] bytes, int timeoutMillis) throws Exception {
    Future<?> future =
        writerExecutor.submit(
            () -> {
              try {
                OutputStream output = socket.getOutputStream();
                output.write(bytes);
                output.flush();
              } catch (Exception e) {
                throw new TcpWriteException(e);
              }
            });
    try {
      future.get(timeoutMillis, TimeUnit.MILLISECONDS);
    } catch (TimeoutException e) {
      future.cancel(true);
      closeQuietly(socket);
      throw new IllegalStateException("TCP 写入超时", e);
    } catch (ExecutionException e) {
      Throwable cause = e.getCause();
      if (cause instanceof TcpWriteException && cause.getCause() instanceof Exception exception) {
        throw exception;
      }
      throw new IllegalStateException("TCP 写入失败", cause);
    }
  }

  private byte[] frame(UPPushBO.TcpPushConfig config, String payload) {
    Charset charset =
        StrUtil.isBlank(config.getCharset())
            ? StandardCharsets.UTF_8
            : Charset.forName(config.getCharset());
    String framed = payload;
    if (!"RAW".equalsIgnoreCase(config.getFrameMode())) {
      framed += decodeDelimiter(config.getDelimiter());
    }
    return framed.getBytes(charset);
  }

  String decodeDelimiter(String delimiter) {
    if (delimiter == null || delimiter.isEmpty()) {
      throw new IllegalArgumentException("TCP 分隔符不能为空");
    }
    StringBuilder decoded = new StringBuilder();
    int characterCount = 0;
    for (int index = 0; index < delimiter.length(); ) {
      char current = delimiter.charAt(index);
      if (current == '\\') {
        if (index + 1 >= delimiter.length()) {
          throw new IllegalArgumentException("TCP 分隔符不能以反斜杠结尾");
        }
        char escaped = delimiter.charAt(index + 1);
        switch (escaped) {
          case 'r' -> decoded.append('\r');
          case 'n' -> decoded.append('\n');
          case '0' -> decoded.append('\0');
          case 't' -> decoded.append('\t');
          default -> throw new IllegalArgumentException("TCP 分隔符包含不支持的转义: \\" + escaped);
        }
        index += 2;
      } else {
        int codePoint = delimiter.codePointAt(index);
        if (Character.isISOControl(codePoint)
            && codePoint != '\r'
            && codePoint != '\n'
            && codePoint != '\0'
            && codePoint != '\t') {
          throw new IllegalArgumentException("TCP 分隔符包含不支持的控制字符");
        }
        decoded.appendCodePoint(codePoint);
        index += Character.charCount(codePoint);
      }
      if (++characterCount > MAX_DELIMITER_CHARACTERS) {
        throw new IllegalArgumentException(
            "TCP 分隔符解析后不能超过 " + MAX_DELIMITER_CHARACTERS + " 个字符");
      }
    }
    return decoded.toString();
  }

  private void validate(UPPushBO.TcpPushConfig config) {
    if (config == null || StrUtil.isBlank(config.getHost())) {
      throw new IllegalArgumentException("TCP 服务器地址不能为空");
    }
    if (config.getPort() == null || config.getPort() < 1 || config.getPort() > 65535) {
      throw new IllegalArgumentException("TCP 端口必须在 1-65535 之间");
    }
    if (StrUtil.isNotBlank(config.getCharset())) {
      Charset.forName(config.getCharset());
    }
    if (!"RAW".equalsIgnoreCase(config.getFrameMode())) {
      decodeDelimiter(config.getDelimiter());
    }
  }

  private int timeout(Integer value, int defaultValue) {
    return value == null || value <= 0 ? defaultValue : value;
  }

  private String fingerprint(UPPushBO.TcpPushConfig config) {
    return Integer.toHexString(
        Objects.hash(
            config.getHost(),
            config.getPort(),
            config.isTls(),
            config.getConnectionMode(),
            config.getConnectTimeoutMillis()));
  }

  private void closeQuietly(Socket socket) {
    try {
      socket.close();
    } catch (Exception e) {
      log.debug("[TCP推送] 关闭连接失败: {}", e.getMessage());
    }
  }

  private final class Connection {
    private final String fingerprint;
    private Socket socket;

    private Connection(String fingerprint, Socket socket) {
      this.fingerprint = fingerprint;
      this.socket = socket;
    }

    private boolean isUsable() {
      return socket != null && socket.isConnected() && !socket.isClosed();
    }

    private void reconnect(UPPushBO.TcpPushConfig config) throws Exception {
      close();
      socket = openSocket(config);
      log.debug(
          "[TCP推送] 长连接重连成功: endpoint={}:{}", config.getHost(), config.getPort());
    }

    private void close() {
      if (socket != null) {
        closeQuietly(socket);
        socket = null;
      }
    }
  }

  private static final class TcpWriteException extends RuntimeException {
    private TcpWriteException(Throwable cause) {
      super(cause);
    }
  }
}
