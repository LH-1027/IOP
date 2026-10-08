package com.iot.controller;

import com.iot.dto.DeviceDTO;
import com.iot.model.Device;
import com.iot.model.User;
import com.iot.repository.DeviceRepository;
import com.iot.repository.UserRepository;
import com.iot.security.UserPrincipal;
import com.iot.service.MqttConnectionManager;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/devices")
public class DeviceController {

    // —— 三个依赖：数据库访问 + MQTT连接管理，由Spring通过构造方法自动注入 ——
    private final DeviceRepository deviceRepository;       // 操作设备表
    private final UserRepository userRepository;           // 操作用户表
    private final MqttConnectionManager connectionManager; // 管理MQTT连接

    // 证书上传目录：项目运行目录 + /uploads/certs/，用户上传的CA证书存这里
    private static final String CERT_UPLOAD_DIR = System.getProperty("user.dir") + "/uploads/certs/";

    // 构造方法：Spring启动时自动把上面三个依赖传进来
    public DeviceController(DeviceRepository deviceRepository,
                            UserRepository userRepository,
                            MqttConnectionManager connectionManager) {
        this.deviceRepository = deviceRepository;
        this.userRepository = userRepository;
        this.connectionManager = connectionManager;
    }

    /*
     * 取出当前登录用户。
     * UserPrincipal 是从JWT令牌里解析出来的"当前登录者"信息，里面有用户ID。
     * 这里再用ID去数据库查出完整的User对象。
     */
    private User getUser(UserPrincipal principal) {
        return userRepository.findById(principal.getUserId())
                .orElseThrow(() -> new RuntimeException("用户不存在"));
    }

    // ==================== 设备 CRUD（增删查） ====================

    /*
     * 获取设备列表  GET /api/devices
     * @AuthenticationPrincipal：自动注入当前登录用户（由JWT过滤器提前解析好）
     * 只返回"当前用户名下"的设备
     */
    @GetMapping
    public ResponseEntity<?> listDevices(@AuthenticationPrincipal UserPrincipal principal) {
        User user = getUser(principal);                              // 当前用户
        List<Device> devices = deviceRepository.findByOwner(user);  // 查他的所有设备
        // 把每个Device实体转换成给前端看的DeviceResponse（stream+map=对列表逐个转换）
        List<DeviceDTO.DeviceResponse> list = devices.stream().map(this::toResponse).collect(Collectors.toList());
        return ResponseEntity.ok(Map.of("code", 200, "data", list));
    }

    /*
     * 添加设备  POST /api/devices
     * 因为要支持上传证书文件，所以用 @RequestParam 逐个接收表单字段（而不是@RequestBody接JSON）
     * required=false 表示该字段可不填，defaultValue 表示不填时的默认值
     */
    @PostMapping
    public ResponseEntity<?> addDevice(@AuthenticationPrincipal UserPrincipal principal,
                                       @RequestParam("name") String name,
                                       @RequestParam("brokerUrl") String brokerUrl,
                                       @RequestParam(value = "brokerPort", defaultValue = "8883") Integer brokerPort,
                                       @RequestParam(value = "mqttUsername", required = false) String mqttUsername,
                                       @RequestParam(value = "mqttPassword", required = false) String mqttPassword,
                                       @RequestParam(value = "useSsl", defaultValue = "true") Boolean useSsl,
                                       @RequestParam(value = "commandFormat", defaultValue = "JSON") String commandFormat,
                                       @RequestParam(value = "certFile", required = false) MultipartFile certFile) {
        try {
            User user = getUser(principal);

            // 用builder链式构造一个Device对象，把前端传的字段一个个填进去
            Device device = Device.builder()
                    .name(name)
                    .brokerUrl(brokerUrl)
                    .brokerPort(brokerPort != null ? brokerPort : 8883)
                    .mqttUsername(mqttUsername)
                    .mqttPassword(mqttPassword)
                    .useSsl(useSsl != null ? useSsl : true)
                    .commandFormat(commandFormat != null ? commandFormat : "JSON")
                    .owner(user)                 // 设置归属人=当前用户
                    .connected(false)            // 新设备默认未连接
                    .build();

            device = deviceRepository.save(device);   // 存进数据库，存完device就有了自增ID

            // 如果上传了证书文件，就保存到磁盘，并把路径记到设备上
            if (certFile != null && !certFile.isEmpty()) {
                Path certDir = Paths.get(CERT_UPLOAD_DIR + user.getId() + "/");  // 按用户ID分目录
                Files.createDirectories(certDir);                               // 目录不存在则创建
                Path certPath = certDir.resolve(device.getId() + ".crt");       // 文件名用设备ID
                certFile.transferTo(certPath.toFile());                         // 把上传内容写入文件
                device.setCaCertPath(certPath.toString());                      // 记录路径
                deviceRepository.save(device);                                  // 再存一次更新路径
            }

            return ResponseEntity.ok(Map.of("code", 200, "data", toResponse(device), "message", "设备添加成功"));
        } catch (Exception e) {
            // 出错就返回400和错误信息
            return ResponseEntity.badRequest().body(Map.of("code", 400, "message", e.getMessage()));
        }
    }

    /*
     * 获取设备详情  GET /api/devices/{id}
     * {id} 是路径里的变量，用 @PathVariable 接收
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> deviceDetail(@AuthenticationPrincipal UserPrincipal principal,
                                          @PathVariable Long id) {
        Device device = deviceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("设备不存在"));   // 查不到就报错
        return ResponseEntity.ok(Map.of("code", 200, "data", toResponse(device)));
    }

    /*
     * 删除设备  DELETE /api/devices/{id}
     * 先断开MQTT连接，再从数据库删除
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteDevice(@AuthenticationPrincipal UserPrincipal principal,
                                          @PathVariable Long id) {
        Device device = deviceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("设备不存在"));
        connectionManager.disconnect(id);     // 先断开连接，释放资源
        deviceRepository.delete(device);      // 再删数据库记录
        return ResponseEntity.ok(Map.of("code", 200, "message", "设备已删除"));
    }

    // ==================== MQTT 操作（连接/订阅/发布等） ====================

    /*
     * 连接设备  POST /api/devices/{id}/connect
     * 让后端建立到该设备MQTT服务器的连接
     */
    @PostMapping("/{id}/connect")
    public ResponseEntity<?> connect(@PathVariable Long id) {
        try {
            Device device = deviceRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("设备不存在"));
            connectionManager.connect(device);    // 调用大脑去建连接
            device.setConnected(true);            // 更新连接状态
            deviceRepository.save(device);
            return ResponseEntity.ok(Map.of("code", 200, "message", "连接成功"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("code", 400, "message", e.getMessage()));
        }
    }

    /*
     * 断开设备  POST /api/devices/{id}/disconnect
     */
    @PostMapping("/{id}/disconnect")
    public ResponseEntity<?> disconnect(@PathVariable Long id) {
        connectionManager.disconnect(id);                 // 断开MQTT连接
        deviceRepository.findById(id).ifPresent(d -> {    // 如果设备存在则更新状态
            d.setConnected(false);
            deviceRepository.save(d);
        });
        return ResponseEntity.ok(Map.of("code", 200, "message", "已断开"));
    }

    /*
     * 订阅主题  POST /api/devices/{id}/subscribe
     * @RequestBody：请求体是JSON，自动转成SubscribeRequest对象（含topic和qos）
     */
    @PostMapping("/{id}/subscribe")
    public ResponseEntity<?> subscribe(@PathVariable Long id,
                                       @RequestBody DeviceDTO.SubscribeRequest request) {
        try {
            connectionManager.subscribe(id, request.getTopic(),
                    request.getQos() != null ? request.getQos() : 0);   // qos没填默认0
            return ResponseEntity.ok(Map.of("code", 200, "message", "订阅成功"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("code", 400, "message", e.getMessage()));
        }
    }

    /*
     * 取消订阅  POST /api/devices/{id}/unsubscribe
     * 这里请求体直接用Map接收，取body里的"topic"字段
     */
    @PostMapping("/{id}/unsubscribe")
    public ResponseEntity<?> unsubscribe(@PathVariable Long id,
                                         @RequestBody Map<String, String> body) {
        try {
            connectionManager.unsubscribe(id, body.get("topic"));
            return ResponseEntity.ok(Map.of("code", 200, "message", "已取消订阅"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("code", 400, "message", e.getMessage()));
        }
    }

    /*
     * 发布消息  POST /api/devices/{id}/publish
     * 前端点"开灯"等控制操作最终走这个接口，把命令发到control主题
     */
    @PostMapping("/{id}/publish")
    public ResponseEntity<?> publish(@PathVariable Long id,
                                     @RequestBody DeviceDTO.PublishRequest request) {
        try {
            connectionManager.publish(id, request.getTopic(), request.getPayload(),
                    request.getQos() != null ? request.getQos() : 0);
            return ResponseEntity.ok(Map.of("code", 200, "message", "发布成功"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("code", 400, "message", e.getMessage()));
        }
    }

    /*
     * 获取订阅列表  GET /api/devices/{id}/subscriptions
     * 返回该设备当前订阅了哪些主题
     */
    @GetMapping("/{id}/subscriptions")
    public ResponseEntity<?> subscriptions(@PathVariable Long id) {
        List<String> subs = connectionManager.getSubscriptions(id);
        return ResponseEntity.ok(Map.of("code", 200, "data", subs));
    }

    /*
     * 获取历史消息  GET /api/devices/{id}/messages
     * 返回该设备最近收到的消息（用于刚打开页面时补上之前的消息）
     */
    @GetMapping("/{id}/messages")
    public ResponseEntity<?> messages(@PathVariable Long id) {
        List<MqttConnectionManager.MqttMessageRecord> msgs = connectionManager.getMessages(id);
        return ResponseEntity.ok(Map.of("code", 200, "data", msgs));
    }

    // ==================== 辅助方法 ====================

    /*
     * 把数据库的 Device 实体转换成返回给前端的 DeviceResponse。
     * 作用：过滤掉密码、证书路径等不该给前端的敏感字段
     *      connected字段用"实时"连接状态(向connectionManager查)，比数据库里的更准
     */
    private DeviceDTO.DeviceResponse toResponse(Device device) {
        return DeviceDTO.DeviceResponse.builder()
                .id(device.getId())
                .name(device.getName())
                .ownerName(device.getOwner().getUsername())   // 所属用户名
                .isOwner(true)
                .brokerUrl(device.getBrokerUrl())
                .brokerPort(device.getBrokerPort())
                .useSsl(device.getUseSsl())
                .commandFormat(device.getCommandFormat())
                .connected(connectionManager.isConnected(device.getId()))  // 实时查连接状态
                .createdAt(device.getCreatedAt())
                .build();
    }
}
