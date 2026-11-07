package nexiot.web.ide.debug.plugins.core.config;

import cn.universal.core.engine.MagicScriptDebugContext;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;
import nexiot.web.ide.debug.plugins.core.context.MagicConsoleSession;
import nexiot.web.ide.debug.plugins.core.event.EventAction;
import nexiot.web.ide.debug.plugins.core.model.MagicNotify;
import nexiot.web.ide.debug.plugins.core.model.Pair;
import nexiot.web.ide.debug.plugins.core.service.MagicNotifyService;
import nexiot.web.ide.debug.plugins.utils.JsonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.socket.TextMessage;

public class WebSocketSessionManager {

  private static final Logger logger = LoggerFactory.getLogger(WebSocketSessionManager.class);

  private static final Map<String, MagicConsoleSession> SESSIONS = new ConcurrentHashMap<>();
  private static final Map<String, MagicScriptDebugContext> CONTEXTS = new ConcurrentHashMap<>();
  private static final int CHECK_INTERVAL = 20;
  private static final int KEEPALIVE_TIMEOUT = 60 * 1000;
  private static final List<Pair<String, String>> MESSAGE_CACHE = new ArrayList<>(200);
  // 为每个会话创建独立的锁，避免使用可能为null的clientId作为锁对象
  private static final Map<String, ReentrantLock> SESSION_LOCKS = new ConcurrentHashMap<>();
  // 定时任务线程池，用于后台任务执行
  private static final ScheduledThreadPoolExecutor logScheduler =
      new ScheduledThreadPoolExecutor(1, r -> new Thread(r, "magic-api-send-log-task"));
  private static final ScheduledThreadPoolExecutor cleanScheduler =
      new ScheduledThreadPoolExecutor(1, r -> new Thread(r, "magic-api-websocket-clean-task"));
  private static MagicNotifyService magicNotifyService;
  private static String instanceId;

  static {
    // 1秒1次发送日志
    logScheduler.scheduleAtFixedRate(WebSocketSessionManager::flushLog, 1, 1, TimeUnit.SECONDS);
    // 每20秒检测一次会话状态
    cleanScheduler.scheduleAtFixedRate(
        WebSocketSessionManager::checkSession, CHECK_INTERVAL, CHECK_INTERVAL, TimeUnit.SECONDS);
  }

  public static void add(MagicConsoleSession session) {
    String clientId = session.getClientId();
    if (clientId != null) {
      SESSIONS.put(clientId, session);
      // 为每个会话创建独立的锁
      SESSION_LOCKS.computeIfAbsent(clientId, k -> new ReentrantLock());
    }
  }

  public static MagicConsoleSession getConsoleSession(String clientId) {
    return SESSIONS.get(clientId);
  }

  public static Collection<MagicConsoleSession> getSessions() {
    return SESSIONS.values();
  }

  public static void remove(MagicConsoleSession session) {
    if (session.getClientId() != null) {
      remove(session.getClientId());
    }
  }

  public static void remove(String sessionId) {
    SESSIONS.remove(sessionId);
    // 清理对应的锁
    SESSION_LOCKS.remove(sessionId);
  }

  public static void sendToAll(MessageType messageType, Object... values) {
    String content = buildMessage(messageType, values);
    sendToAll(content);
  }

  private static void sendToAll(String content) {
    getSessions().stream()
        .filter(MagicConsoleSession::writeable)
        .forEach(session -> sendBySession(session, content));
    sendToMachineByClientId(null, content);
  }

  public static void sendLogs(String sessionId, String message) {
    synchronized (MESSAGE_CACHE) {
      MESSAGE_CACHE.add(Pair.of(sessionId, message));
      if (MESSAGE_CACHE.size() >= 100) {
        flushLog();
      }
    }
  }

  public static void flushLog() {
    try {
      Map<String, List<String>> messages;
      synchronized (MESSAGE_CACHE) {
        messages =
            MESSAGE_CACHE.stream()
                .collect(
                    Collectors.groupingBy(
                        Pair::getFirst, Collectors.mapping(Pair::getSecond, Collectors.toList())));
        MESSAGE_CACHE.clear();
      }
      messages.forEach(
          (clientId, logs) ->
              sendByClientId(clientId, logs.size() > 1 ? MessageType.LOGS : MessageType.LOG, logs));
    } catch (Exception e) {
      logger.warn("发送日志失败", e);
    }
  }

  public static void sendByClientId(String clientId, MessageType messageType, Object... values) {
    MagicConsoleSession session = findSession(clientId);
    String content = buildMessage(messageType, values);
    if (session != null && session.writeable()) {
      sendBySession(session, content);
    } else {
      sendToMachineByClientId(clientId, content);
    }
  }

  public static void sendToOther(
      String excludeClientId, MessageType messageType, Object... values) {
    String content = buildMessage(messageType, values);
    getSessions().stream()
        .filter(MagicConsoleSession::writeable)
        .filter(it -> !it.getClientId().equals(excludeClientId))
        .forEach(session -> sendBySession(session, content));
    sendToMachineByClientId(null, content);
  }

  public static void sendToMachineByClientId(String clientId, String content) {
    if (magicNotifyService != null) {
      // 通知其他机器去发送消息
      magicNotifyService.sendNotify(
          new MagicNotify(instanceId, EventAction.WS_S_C, clientId, content));
    }
  }

  public static void sendToMachine(MessageType messageType, Object... args) {
    if (magicNotifyService != null) {
      // 通知其他机器去发送消息
      magicNotifyService.sendNotify(
          new MagicNotify(instanceId, EventAction.WS_S_S, null, buildMessage(messageType, args)));
    }
  }

  public static String buildMessage(MessageType messageType, Object... values) {
    StringBuilder builder = new StringBuilder(messageType.name().toLowerCase());
    if (values != null) {
      for (int i = 0, len = values.length; i < len; i++) {
        builder.append(",");
        Object value = values[i];
        if (i + 1 < len || value instanceof CharSequence || value instanceof Number) {
          builder.append(value);
        } else {
          builder.append(JsonUtils.toJsonString(value));
        }
      }
    }
    return builder.toString();
  }

  public static void sendByClientId(String clientId, String content) {
    if (clientId == null) {
      getSessions().stream()
          .filter(MagicConsoleSession::writeable)
          .forEach(session -> sendBySession(session, content));
    } else {
      MagicConsoleSession session = findSession(clientId);
      if (session != null) {
        sendBySession(session, content);
      }
    }
  }

  public static void sendBySession(MagicConsoleSession session, String content) {
    if (session == null || content == null) {
      return;
    }

    // 检查会话是否可写
    if (!session.writeable()) {
      return;
    }

    String clientId = session.getClientId();
    if (clientId == null) {
      // 如果clientId为null，使用session对象本身作为锁
      synchronized (session) {
        try {
          session.getWebSocketSession().sendMessage(new TextMessage(content));
        } catch (Exception e) {
          logger.debug("发送WebSocket消息失败 (无clientId): {}", e.getMessage());
        }
      }
      return;
    }

    // 使用独立的锁对象，避免阻塞时间过长
    ReentrantLock lock = SESSION_LOCKS.computeIfAbsent(clientId, k -> new ReentrantLock());
    if (lock.tryLock()) {
      try {
        // 双重检查，确保会话仍然可用
        if (session.writeable()) {
          session.getWebSocketSession().sendMessage(new TextMessage(content));
        }
      } catch (Exception e) {
        logger.debug("发送WebSocket消息失败: {}", e.getMessage());
      } finally {
        lock.unlock();
      }
    } else {
      // 如果无法获取锁，记录但不阻塞（避免死锁）
      logger.debug("无法获取会话锁，跳过发送消息: {}", clientId);
    }
  }

  public static MagicConsoleSession findSession(String clientId) {
    return getSessions().stream()
        .filter(it -> Objects.equals(clientId, it.getClientId()))
        .findFirst()
        .orElse(null);
  }

  public static void setMagicNotifyService(MagicNotifyService magicNotifyService) {
    WebSocketSessionManager.magicNotifyService = magicNotifyService;
  }

  public static void setInstanceId(String instanceId) {
    WebSocketSessionManager.instanceId = instanceId;
  }

  public static void addMagicScriptContext(
      String sessionAndScriptId, MagicScriptDebugContext context) {
    CONTEXTS.put(sessionAndScriptId, context);
  }

  public static MagicScriptDebugContext findMagicScriptContext(String sessionAndScriptId) {
    return CONTEXTS.get(sessionAndScriptId);
  }

  public static void removeMagicScriptContext(String sessionAndScriptId) {
    CONTEXTS.remove(sessionAndScriptId);
  }

  private static void checkSession() {
    try {
      long currentTime = System.currentTimeMillis();
      long activateTime = currentTime - KEEPALIVE_TIMEOUT;

      // 收集需要关闭的会话，避免在遍历时修改集合
      List<Map.Entry<String, MagicConsoleSession>> toClose = new ArrayList<>();

      // 遍历所有会话，检查超时并发送PING
      for (Map.Entry<String, MagicConsoleSession> entry : SESSIONS.entrySet()) {
        MagicConsoleSession session = entry.getValue();
        if (session == null) {
          continue;
        }

        // 检查是否超时
        if (session.getActivateTime() < activateTime) {
          toClose.add(entry);
          continue;
        }

        // 发送PING消息（非阻塞方式，避免阻塞定时任务线程）
        try {
          String pingMessage = buildMessage(MessageType.PING);
          sendBySession(session, pingMessage);
        } catch (Exception e) {
          logger.debug("发送PING消息失败，会话可能已断开: {}", entry.getKey(), e);
          // 如果发送失败，可能连接已断开，标记为需要关闭
          toClose.add(entry);
        }
      }

      // 关闭超时的会话
      for (Map.Entry<String, MagicConsoleSession> entry : toClose) {
        try {
          MagicConsoleSession session = entry.getValue();
          if (session != null) {
            SESSIONS.remove(entry.getKey());
            session.close();
            sendToAll(MessageType.USER_LOGOUT, session.getAttributes());
          }
        } catch (Exception e) {
          logger.debug("关闭会话失败: {}", entry.getKey(), e);
        }
      }
    } catch (Exception e) {
      logger.warn("检查WebSocket会话状态失败", e);
    }
  }
}
