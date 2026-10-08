package com.iot.config;

import com.iot.service.MqttConnectionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.*;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    // 连接管理器，通过它拿到设备消息
    private final MqttConnectionManager connectionManager;

    // 构造方法：Spring自动注入connectionManager
    public WebSocketConfig(MqttConnectionManager connectionManager) {
        this.connectionManager = connectionManager;
    }

    /*
     * 注册WebSocket端点。
     * addHandler(处理器, 路径)：前端连接 /ws/5 这种地址时，交给MqttWebSocketHandler处理
     * "{deviceId}" 是路径里的变量，代表设备ID
     * setAllowedOrigins("*")：允许任何网站来源连接
     */
    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(new MqttWebSocketHandler(connectionManager), "/ws/{deviceId}")
                .setAllowedOrigins("*");
    }
}

class MqttWebSocketHandler extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(MqttWebSocketHandler.class);

    /*
     * 核心数据结构：设备ID → 该设备页面的所有浏览器连接集合
     * static：所有连接共享同一份记录
     * Set用ConcurrentHashMap.newKeySet()创建，保证多线程安全
     */
    private static final Map<Long, java.util.Set<WebSocketSession>> sessions = new ConcurrentHashMap<>();

    private final MqttConnectionManager connectionManager;

    /*
     * 构造方法：注入连接管理器，并向它注册"收到MQTT消息后怎么推给前端"的回调
     * 这段lambda就是之前 MqttConnectionManager 里 pushHandler 的真正实现
     */
    public MqttWebSocketHandler(MqttConnectionManager connectionManager) {
        this.connectionManager = connectionManager;
        // 注册推送回调 参数 deviceId=哪台设备, message=收到的消息
        connectionManager.setPushHandler((deviceId, message) -> {
            // 找出"正在看这台设备"的所有浏览器连接
            java.util.Set<WebSocketSession> set = sessions.get(deviceId);
            if (set != null) {
                // 把消息手工拼成一段JSON字符串。形如：
                // {"type":"message","topic":"xxx","payload":"yyy","timestamp":123}
                // escapeJson用来转义内容里的引号等特殊字符，否则JSON会拼坏
                String json = String.format(
                        "{\"type\":\"message\",\"topic\":\"%s\",\"payload\":\"%s\",\"timestamp\":%d}",
                        escapeJson(message.getTopic()),
                        escapeJson(message.getPayload()),
                        message.getTimestamp()
                );
                // 遍历每个浏览器连接，逐个把JSON发过去
                for (WebSocketSession s : set) {
                    try {
                        if (s.isOpen()) s.sendMessage(new TextMessage(json));  // 连接还开着才发
                    } catch (IOException e) {
                        log.warn("WebSocket推送失败: {}", e.getMessage());
                    }
                }
            }
        });
    }

    /*
     * 浏览器成功建立WebSocket连接时，Spring自动调用此方法。
     * 作用：从URL解析出设备ID，把这个连接登记到sessions里
     */
    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        Long deviceId = getDeviceId(session);                 // 从 /ws/5 里取出 5
        if (deviceId != null) {
            // computeIfAbsent：如果这台设备还没有集合就新建一个，然后把当前连接加进去
            sessions.computeIfAbsent(deviceId, k -> ConcurrentHashMap.newKeySet()).add(session);
            log.info("WebSocket连接: deviceId={}", deviceId);
        }
    }

    /*
     * 浏览器关闭页面/断开连接时，Spring自动调用此方法。
     * 作用：把这个连接从sessions里移除，避免给已关闭的连接推送
     */
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        Long deviceId = getDeviceId(session);
        if (deviceId != null) {
            java.util.Set<WebSocketSession> set = sessions.get(deviceId);
            if (set != null) set.remove(session);
            log.info("WebSocket断开: deviceId={}", deviceId);
        }
    }

    /*
     * 从连接的URL路径里解析出设备ID。
     * 例如路径是 /ws/5，按"/"切开得到 ["", "ws", "5"]，取最后一段"5"转成数字
     */
    private Long getDeviceId(WebSocketSession session) {
        String path = session.getUri().getPath();   // 取出路径，如 /ws/5
        String[] parts = path.split("/");            // 按斜杠切分
        try {
            return Long.parseLong(parts[parts.length - 1]);  // 最后一段转成Long
        } catch (NumberFormatException e) {
            return null;                              // 不是数字就返回null
        }
    }

    /*
     * JSON转义：手工拼JSON时，内容里如果有引号、反斜杠、换行等会破坏JSON格式，
     * 这里把它们替换成转义写法。例如 " → \"，换行 → \n
     */
    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t");
    }
}
