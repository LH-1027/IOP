package com.iot.service;

import com.iot.model.Device;
import com.iot.repository.DeviceRepository;
import lombok.Setter;
import org.eclipse.paho.client.mqttv3.*;                       // Eclipse Paho：Java版MQTT客户端库
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.net.ssl.SSLSocketFactory;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.SSLContext;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

//@Service：告诉Spring这是一个服务类，Spring会自动创建它的唯一实例(单例)，其它类（如DeviceController）需要用它时Spring会自动注入。
@Service
public class MqttConnectionManager {

    // 日志对象：用 log.info()/warn()/error() 打印运行信息
    private static final Logger log = LoggerFactory.getLogger(MqttConnectionManager.class);

    // 设备数据访问层：用来读写数据库里的设备（比如把订阅列表存回数据库）
    private final DeviceRepository deviceRepository;

    // —— 下面3个Map是核心数据结构，key都是"设备ID"，把每台设备的状态隔离开 ——
    // 该设备的MQTT客户端对象。ConcurrentHashMap是线程安全的哈希表，多线程同时读写不会出错
    private final Map<Long, MqttClient> clients = new ConcurrentHashMap<>();
    // 该设备当前订阅的主题列表
    private final Map<Long, List<String>> subscriptions = new ConcurrentHashMap<>();
    // 该设备最近收到的消息队列（只保留最近100条，防止内存无限增长）
    private final Map<Long, LinkedList<MqttMessageRecord>> messageQueues = new ConcurrentHashMap<>();

    // 注册推送回调：WebSocketConfig启动时会调用它，把"如何推给前端"的逻辑塞进来
    // 消息推送回调：一个"钩子"，收到MQTT消息后通过它把消息推给前端。具体实现由WebSocketConfig注册进来
    @Setter
    private MessagePushHandler pushHandler;

    // 构造方法：Spring创建本类时，自动把DeviceRepository传进来（依赖注入）
    public MqttConnectionManager(DeviceRepository deviceRepository) {
        this.deviceRepository = deviceRepository;
    }

    /*
     * 建立 MQTT 连接（核心方法）
     * synchronized：加锁，保证同一时刻只有一个线程能进来，避免两个请求同时给同一设备建连接
     */
    public synchronized void connect(Device device) {
        // 查重：如果这台设备已经有连接了，直接返回，不重复连
        if (clients.containsKey(device.getId())) {
            log.info("设备 {} 已连接", device.getId());
            return;
        }

        try {
            // 拼接连接地址   用SSL就是 ssl://，否则 tcp://
            // 最终形如：ssl://xxx.emqxsl.cn:8883
            String protocol = device.getUseSsl() ? "ssl://" : "tcp://";
            String url = protocol + device.getBrokerUrl() + ":" + device.getBrokerPort();
            // 客户端ID必须全局唯一，否则服务器会把同名旧连接踢掉，所以加一段随机字符串
            String clientId = "web_mqtt_" + device.getId() + "_" + UUID.randomUUID().toString().substring(0, 8);

            // 创建客户端对象  MemoryPersistence表示消息状态存在内存里
            MqttClient client = new MqttClient(url, clientId, new MemoryPersistence());

            // 配置连接选项
            MqttConnectOptions options = new MqttConnectOptions();
            options.setCleanSession(true);          // true=不保留上次会话，每次都是全新连接
            options.setAutomaticReconnect(true);    // 断线后自动重连
            options.setConnectionTimeout(15);       // 连接超时15秒
            options.setKeepAliveInterval(60);       // 每60秒发一次心跳，告诉服务器"我还在"

            // 如果设备配了账号，就设置账号密码
            if (device.getMqttUsername() != null && !device.getMqttUsername().isEmpty()) {
                options.setUserName(device.getMqttUsername());
                // 密码要转成char[]；如果没填密码就给个空数组
                options.setPassword(device.getMqttPassword() != null
                        ? device.getMqttPassword().toCharArray() : new char[0]);
            }

            // 配置SSL/TLS加密
            if (device.getUseSsl() && device.getCaCertPath() != null) {
                // 情况1：要SSL且上传了证书 → 尝试用证书建立严格加密连接
                Path certPath = Path.of(device.getCaCertPath());
                if (Files.exists(certPath)) {
                    try {
                        SSLSocketFactory ssf = createSSLSocketFactory(device.getCaCertPath());
                        options.setSocketFactory(ssf);
                        log.info("已加载CA证书: {}", device.getCaCertPath());
                    } catch (Exception e) {
                        // 证书加载失败 → 退而用JDK默认SSL（不校验自定义证书）
                        log.warn("CA证书加载失败，使用默认SSL: {}", e.getMessage());
                        options.setSocketFactory(SSLSocketFactory.getDefault());
                    }
                } else {
                    // 情况2：要SSL但证书文件找不到 → 用默认SSL
                    log.warn("CA证书文件不存在: {}，使用默认SSL", device.getCaCertPath());
                    options.setSocketFactory(SSLSocketFactory.getDefault());
                }
            } else if (device.getUseSsl()) {
                // 情况3：要SSL但没配证书 → 用默认SSL
                options.setSocketFactory(SSLSocketFactory.getDefault());
            }

            // 注册回调：定义"连接断了/收到消息/发送完成"时分别做什么
            Long deviceId = device.getId();
            client.setCallback(new MqttCallback() {
                // 连接意外断开时触发（automaticReconnect会自动重连，这里只记日志）
                @Override
                public void connectionLost(Throwable cause) {
                    log.warn("设备 {} MQTT断开: {}", deviceId, cause.getMessage());
                }

                // 每当收到订阅主题的新消息，这个方法被自动调用
                @Override
                public void messageArrived(String topic, MqttMessage message) {
                    String payload = new String(message.getPayload());  // 消息内容(字节)转成字符串
                    log.debug("设备 {} 收到: {} -> {}", deviceId, topic, payload);
                    addMessage(deviceId, topic, payload);               // 存入队列 + 推送给前端
                }

                // 消息成功送达服务器时触发
                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {
                }
            });

            // 发起连接
            client.connect(options);
            // 连接成功后，把客户端和空的订阅列表、消息队列存进3个Map
            clients.put(device.getId(), client);
            subscriptions.putIfAbsent(device.getId(), new CopyOnWriteArrayList<>());
            messageQueues.putIfAbsent(device.getId(), new LinkedList<>());

            log.info("设备 {} MQTT已连接: {}", device.getId(), url);

            // 恢复订阅：上次保存在数据库里的订阅主题，重新订一遍
            String savedSubs = device.getSubscriptions();               // 形如 "a/topic,b/topic"
            if (savedSubs != null && !savedSubs.isEmpty()) {
                for (String topic : savedSubs.split(",")) {             // 按逗号拆成一个个主题
                    topic = topic.trim();                              // 去掉首尾空格
                    if (!topic.isEmpty()) {
                        try {
                            client.subscribe(topic, 0);                // 重新订阅，QoS=0
                            subscriptions.get(device.getId()).add(topic);
                            log.info("设备 {} 恢复订阅: {}", device.getId(), topic);
                        } catch (MqttException e) {
                            log.warn("恢复订阅失败: {}", topic);
                        }
                    }
                }
            }

        } catch (MqttException e) {
            // 连接过程出错，记日志并抛出异常
            log.error("设备 {} MQTT连接失败: {}", device.getId(), e.getMessage());
            throw new RuntimeException("MQTT连接失败: " + e.getMessage());
        }
    }

    /*
     * 断开连接：关闭客户端，并清空该设备在3个Map里的数据
     */
    public void disconnect(Long deviceId) {
        // remove会"取出并删除"该设备的客户端
        MqttClient client = clients.remove(deviceId);
        if (client != null) {
            try {
                if (client.isConnected()) client.disconnect();  // 先断开网络连接
                client.close();                                  // 再释放资源
            } catch (MqttException e) {
                log.warn("断开异常: {}", e.getMessage());
            }
        }
        // 清理这台设备的订阅列表和消息队列，释放内存
        subscriptions.remove(deviceId);
        messageQueues.remove(deviceId);
        log.info("设备 {} 已断开", deviceId);
    }

    /*
     * 订阅主题：让后端开始监听某个主题，并把订阅存进数据库
     * qos：服务质量 0=最多一次(可能丢) 1=至少一次 2=恰好一次
     */
    public void subscribe(Long deviceId, String topic, int qos) {
        MqttClient client = clients.get(deviceId);
        // 没连接就不能订阅，抛异常提示
        if (client == null || !client.isConnected()) {
            throw new RuntimeException("设备未连接");
        }
        try {
            client.subscribe(topic, qos);                       // 向服务器登记订阅
            List<String> subs = subscriptions.get(deviceId);
            if (!subs.contains(topic)) subs.add(topic);         // 内存里记一份（避免重复）

            // 持久化到数据库：把订阅列表拼成逗号字符串存进设备记录，下次重连可恢复
            deviceRepository.findById(deviceId).ifPresent(dev -> {
                String joined = String.join(",", subs);
                dev.setSubscriptions(joined);
                deviceRepository.save(dev);
            });

            log.info("设备 {} 订阅: {} (qos={})", deviceId, topic, qos);
        } catch (MqttException e) {
            throw new RuntimeException("订阅失败: " + e.getMessage());
        }
    }

    /*
     * 取消订阅：不再监听某主题，并同步更新数据库里的订阅列表
     */
    public void unsubscribe(Long deviceId, String topic) {
        MqttClient client = clients.get(deviceId);
        if (client != null) {
            try {
                client.unsubscribe(topic);                       // 通知服务器取消订阅
                List<String> subs = subscriptions.getOrDefault(deviceId, Collections.emptyList());
                subs.remove(topic);                              // 从内存列表移除
                // 同步更新数据库
                deviceRepository.findById(deviceId).ifPresent(dev -> {
                    dev.setSubscriptions(String.join(",", subs));
                    deviceRepository.save(dev);
                });
            } catch (MqttException e) {
                log.warn("取消订阅失败: {}", e.getMessage());
            }
        }
    }

    /*
     * 发布消息：把一条消息发送到指定主题
     */
    public void publish(Long deviceId, String topic, String payload, int qos) {
        MqttClient client = clients.get(deviceId);
        if (client == null || !client.isConnected()) {
            throw new RuntimeException("设备未连接");
        }
        try {
            MqttMessage msg = new MqttMessage(payload.getBytes());  // 内容转字节并包成MQTT消息
            msg.setQos(qos);                                        // 设置服务质量等级
            client.publish(topic, msg);                             // 发到服务器，服务器再转给设备
            log.info("设备 {} 发布: {} -> {}", deviceId, topic, payload);
        } catch (MqttException e) {
            throw new RuntimeException("发布失败: " + e.getMessage());
        }
    }

    /* 获取某设备当前的订阅列表（给前端展示用） */
    public List<String> getSubscriptions(Long deviceId) {
        return subscriptions.getOrDefault(deviceId, Collections.emptyList());
    }

    /* 获取某设备的历史消息 */
    public List<MqttMessageRecord> getMessages(Long deviceId) {
        return new ArrayList<>(messageQueues.getOrDefault(deviceId, new LinkedList<>()));
    }

    /* 判断某设备当前是否处于已连接状态 */
    public boolean isConnected(Long deviceId) {
        MqttClient client = clients.get(deviceId);
        return client != null && client.isConnected();
    }

    /*
     * 添加消息并推送
     * 做两件事：存进该设备的消息队列(最多100条)  通过回调推送给前端
     */
    private void addMessage(Long deviceId, String topic, String payload) {
        // 把主题、内容、当前时间戳打包成一条记录
        MqttMessageRecord record = new MqttMessageRecord(topic, payload, System.currentTimeMillis());
        LinkedList<MqttMessageRecord> queue = messageQueues.get(deviceId);
        if (queue != null) {
            queue.addFirst(record);                       // 新消息插到队头（最新的在最前）
            if (queue.size() > 100) queue.removeLast();   // 超过100条就删掉最旧的
        }
        // 通过回调推送到WebSocket（如果回调已注册）
        if (pushHandler != null) {
            pushHandler.push(deviceId, record);
        }
    }

    /*
     * 创建 SSL Socket Factory：把用户上传的CA证书做成"信任库"，
     * 让Java在TLS握手时用这张证书去验证服务器身份 这是里"严格加密"用到的工具方法。
     */
    private SSLSocketFactory createSSLSocketFactory(String certPath) throws Exception {
        // 1) 读取证书文件，解析成X.509证书对象
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        byte[] certBytes = Files.readAllBytes(Path.of(certPath));
        X509Certificate cert = (X509Certificate) cf.generateCertificate(new ByteArrayInputStream(certBytes));

        // 2) 创建一个空的密钥库(KeyStore)，把这张证书作为"受信任的CA"放进去
        KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
        ks.load(null, null);                  // 用null初始化一个空库
        ks.setCertificateEntry("ca", cert);   // 以别名"ca"存入证书

        // 3) 用这个密钥库初始化信任管理器(决定信任哪些服务器证书)
        TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init(ks);

        // 4) 用信任管理器构建SSL上下文，最终生成SocketFactory(造加密连接的工厂)
        SSLContext ctx = SSLContext.getInstance("TLS");
        ctx.init(null, tmf.getTrustManagers(), null);
        return ctx.getSocketFactory();
    }

    /*
     * 消息记录：一条MQTT消息的数据结构（主题、内容、时间戳）
     * static class：嵌套类，独立于外部类实例存在。这里当作简单的数据容器用
     */
    public static class MqttMessageRecord {
        private String topic;       // 消息来自哪个主题
        private String payload;     // 消息内容
        private long timestamp;     // 收到时间（毫秒）

        public MqttMessageRecord(String topic, String payload, long timestamp) {
            this.topic = topic;
            this.payload = payload;
            this.timestamp = timestamp;
        }

        // getter：供外部读取这三个字段（比如序列化成JSON时要用）
        public String getTopic() { return topic; }
        public String getPayload() { return payload; }
        public long getTimestamp() { return timestamp; }
    }

    /*
     * 消息推送接口：定义"推送一条消息"长什么样，但不规定怎么推。
     * 真正的推送逻辑由WebSocketConfig实现并通过setPushHandler注册。
     * 这种"定义接口、别处实现"的设计让本类不必依赖WebSocket，降低耦合。
     */
    public interface MessagePushHandler {
        void push(Long deviceId, MqttMessageRecord message);
    }
}
