package cn.universal.dm.device.service.push;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import cn.universal.persistence.entity.bo.UPPushBO;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

class TcpPushClientManagerTest {

  @Test
  void shouldDecodePresetAndCustomDelimiters() {
    TcpPushClientManager manager = new TcpPushClientManager();
    try {
      assertEquals("\r\n", manager.decodeDelimiter("\\r\\n"));
      assertEquals("\0", manager.decodeDelimiter("\\0"));
      assertEquals("@@", manager.decodeDelimiter("@@"));
      assertEquals("\n", manager.decodeDelimiter("\n"));
    } finally {
      manager.close();
    }
  }

  @Test
  void shouldRejectInvalidDelimiter() {
    TcpPushClientManager manager = new TcpPushClientManager();
    try {
      assertThrows(IllegalArgumentException.class, () -> manager.decodeDelimiter(""));
      assertThrows(IllegalArgumentException.class, () -> manager.decodeDelimiter("\\x"));
      assertThrows(IllegalArgumentException.class, () -> manager.decodeDelimiter("delimiter-is-too-long"));
    } finally {
      manager.close();
    }
  }

  @Test
  void shouldWriteUtf8JsonWithLineBoundary() throws Exception {
    try (ServerSocket server = new ServerSocket(0, 1, InetAddress.getLoopbackAddress())) {
      CompletableFuture<String> received =
          CompletableFuture.supplyAsync(
              () -> {
                try (Socket socket = server.accept();
                    BufferedReader reader =
                        new BufferedReader(
                            new InputStreamReader(
                                socket.getInputStream(), StandardCharsets.UTF_8))) {
                  return reader.readLine();
                } catch (Exception e) {
                  throw new IllegalStateException(e);
                }
              });
      TcpPushClientManager manager = new TcpPushClientManager();
      try {
        UPPushBO.TcpPushConfig config =
            UPPushBO.TcpPushConfig.builder()
                .host(InetAddress.getLoopbackAddress().getHostAddress())
                .port(server.getLocalPort())
                .connectionMode("PERSISTENT")
                .frameMode("LINE")
                .delimiter("\\n")
                .charset("UTF-8")
                .connectTimeoutMillis(1000)
                .writeTimeoutMillis(1000)
                .build();

        manager.send("app-1", config, "{\"temperature\":23.5}");

        assertEquals("{\"temperature\":23.5}", received.get(2, TimeUnit.SECONDS));
      } finally {
        manager.close();
      }
    }
  }
}
