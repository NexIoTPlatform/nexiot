package cn.universal.websocket.protocol.config;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import cn.universal.common.util.WebInterfaceReadyChecker;
import cn.universal.persistence.entity.bo.NetworkBO;
import cn.universal.persistence.mapper.NetworkMapper;
import cn.universal.persistence.query.NetworkQuery;
import cn.universal.websocket.protocol.manager.IWebSocketServerManager;
import lombok.extern.slf4j.Slf4j;

/**
 * WebSocket 网络组件开机自启监听器。
 *
 * <p>对标 MQTT 的 ThirdMQTTServerManager，在应用启动后自动加载并启动已启用的
 * WebSocket 服务端/客户端网络组件，避免手工点击启动。
 */
@Slf4j
@Component
public class WebSocketNetworkStartupListener implements ApplicationListener<ApplicationReadyEvent> {

    @Autowired(required = false)
    private IWebSocketServerManager webSocketServerManager;

    @Autowired(required = false)
    private NetworkMapper networkMapper;

    @Autowired
    private WebInterfaceReadyChecker webInterfaceReadyChecker;

    @Autowired(required = false)
    private ExecutorService scheduledExecutor;

    private final ExecutorService localExecutor = Executors.newVirtualThreadPerTaskExecutor();

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        if (webSocketServerManager == null || networkMapper == null) {
            log.warn("[WebSocket开机自启] WebSocket管理器或NetworkMapper未注入，跳过自动启动");
            return;
        }

        ExecutorService executor = scheduledExecutor != null ? scheduledExecutor : localExecutor;
        webInterfaceReadyChecker.executeAfterWebInterfaceReady(this::startEnabledWebSocketNetworks, executor);
    }

    private void startEnabledWebSocketNetworks() {
        try {
            NetworkQuery query = new NetworkQuery();
            query.setTypes(List.of("WEB_SOCKET_SERVER", "WEB_SOCKET_CLIENT"));
            query.setState(Boolean.TRUE);

            List<NetworkBO> networks = networkMapper.selectNetworkList(query);
            if (networks == null || networks.isEmpty()) {
                log.info("[WebSocket开机自启] 未找到启用的WebSocket网络组件，跳过");
                return;
            }

            for (NetworkBO network : networks) {
                try {
                    boolean success = webSocketServerManager.startServer(network.getId());
                    if (success) {
                        log.info("[WebSocket开机自启] 启动成功: {} (id={}, type={})", network.getName(), network.getId(), network.getType());
                    } else {
                        log.warn("[WebSocket开机自启] 启动失败: {} (id={}, type={})", network.getName(), network.getId(), network.getType());
                    }
                } catch (Exception e) {
                    log.error("[WebSocket开机自启] 启动异常: {} (id={})", network.getName(), network.getId(), e);
                }
            }
        } catch (Exception e) {
            log.error("[WebSocket开机自启] 自动启动流程异常", e);
        }
    }
}
