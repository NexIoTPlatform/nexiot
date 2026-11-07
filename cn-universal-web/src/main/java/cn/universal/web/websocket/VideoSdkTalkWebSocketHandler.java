package cn.universal.web.websocket;

import jakarta.annotation.PreDestroy;
import java.io.ByteArrayOutputStream;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.AbstractWebSocketHandler;
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator;
import org.springframework.web.util.UriComponentsBuilder;

@Component
public class VideoSdkTalkWebSocketHandler extends AbstractWebSocketHandler {
  private final VideoSdkTalkTickets tickets;
  private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
  private final Map<String, Relay> relays = new ConcurrentHashMap<>();
  public VideoSdkTalkWebSocketHandler(VideoSdkTalkTickets tickets) { this.tickets = tickets; }

  @Override public void afterConnectionEstablished(WebSocketSession session) throws Exception {
    session.setBinaryMessageSizeLimit(8192); session.setTextMessageSizeLimit(128);
    try {
      String token = UriComponentsBuilder.fromUri(session.getUri()).build().getQueryParams().getFirst("token");
      var uri = tickets.consume(token);
      Relay relay = new Relay(session);
      relays.put(session.getId(), relay);
      client.newWebSocketBuilder().connectTimeout(Duration.ofSeconds(5)).buildAsync(uri, relay)
          .orTimeout(6, TimeUnit.SECONDS).whenComplete((upstream, error) -> { if (error != null) relay.close(CloseStatus.SERVER_ERROR); });
    } catch (Exception ex) { session.close(CloseStatus.POLICY_VIOLATION); }
  }
  @Override protected void handleBinaryMessage(WebSocketSession session, BinaryMessage message) {
    Relay relay = relays.get(session.getId());
    if (relay != null) relay.send(message.getPayload());
  }
  @Override protected void handleTextMessage(WebSocketSession session, TextMessage message) {
    Relay relay = relays.get(session.getId());
    if (relay == null) return;
    if (!"ping".equals(message.getPayload()) || relay.upstream == null) { relay.close(CloseStatus.POLICY_VIOLATION); return; }
    try { relay.upstream.sendText("ping", true).get(3, TimeUnit.SECONDS); }
    catch (Exception ex) { relay.close(CloseStatus.SERVER_ERROR); }
  }
  @Override public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
    Relay relay = relays.remove(session.getId()); if (relay != null) relay.close(status);
  }
  @Override public void handleTransportError(WebSocketSession session, Throwable exception) {
    afterConnectionClosed(session, CloseStatus.SERVER_ERROR);
  }
  @PreDestroy public void shutdown() { relays.values().forEach(relay -> relay.close(CloseStatus.GOING_AWAY)); client.close(); }

  private final class Relay implements WebSocket.Listener {
    private final WebSocketSession downstream;
    private final AtomicBoolean closed = new AtomicBoolean();
    private final ByteArrayOutputStream binary = new ByteArrayOutputStream();
    private final StringBuilder text = new StringBuilder();
    private volatile WebSocket upstream;
    Relay(WebSocketSession downstream) { this.downstream = new ConcurrentWebSocketSessionDecorator(downstream, 3000, 65536); }
    @Override public void onOpen(WebSocket socket) {
      upstream = socket;
      if (closed.get()) socket.abort(); else socket.request(1);
    }
    void send(ByteBuffer pcm) {
      if (closed.get() || upstream == null) { close(CloseStatus.POLICY_VIOLATION); return; }
      try { upstream.sendBinary(pcm, true).get(3, TimeUnit.SECONDS); }
      catch (Exception ex) { close(CloseStatus.SERVER_ERROR); }
    }
    @Override public CompletionStage<?> onBinary(WebSocket socket, ByteBuffer data, boolean last) {
      try {
        if (binary.size() + data.remaining() > 32768) throw new IllegalArgumentException("音频包过大");
        byte[] bytes = new byte[data.remaining()]; data.get(bytes); binary.writeBytes(bytes);
        if (last) { downstream.sendMessage(new BinaryMessage(binary.toByteArray())); binary.reset(); }
        socket.request(1);
      } catch (Exception ex) { close(CloseStatus.SERVER_ERROR); }
      return null;
    }
    @Override public CompletionStage<?> onText(WebSocket socket, CharSequence data, boolean last) {
      try {
        if (text.length() + data.length() > 4096) throw new IllegalArgumentException("控制消息过大");
        text.append(data);
        if (last) { downstream.sendMessage(new TextMessage(text.toString())); text.setLength(0); }
        socket.request(1);
      } catch (Exception ex) { close(CloseStatus.SERVER_ERROR); }
      return null;
    }
    @Override public CompletionStage<?> onClose(WebSocket socket, int status, String reason) {
      close(status == 1000 ? CloseStatus.NORMAL : CloseStatus.SERVER_ERROR); return null;
    }
    @Override public void onError(WebSocket socket, Throwable error) { close(CloseStatus.SERVER_ERROR); }
    void close(CloseStatus status) {
      if (!closed.compareAndSet(false, true)) return;
      relays.remove(downstream.getId(), this);
      if (upstream != null) upstream.abort();
      try { if (downstream.isOpen()) downstream.close(status); } catch (Exception ignored) {}
    }
  }
}
