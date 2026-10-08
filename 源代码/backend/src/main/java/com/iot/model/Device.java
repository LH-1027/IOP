package com.iot.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

// 设备实体类：对应数据库 devices 表，描述一台 MQTT 设备的所有信息
@Entity
@Table(name = "devices")
@Data                       // Lombok：自动生成 getter/setter/toString 等
@NoArgsConstructor
@AllArgsConstructor
@Builder                    // Lombok：支持 Device.builder()...build() 链式构造
public class Device {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)  // 主键，自增
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;                    // 设备名称

    @ManyToOne(fetch = FetchType.LAZY)      // 多台设备属于一个用户（懒加载）
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;                     // 设备所属用户

    // MQTT 连接配置
    @Column(name = "broker_url", nullable = false, length = 255)
    private String brokerUrl;               // MQTT服务器地址

    @Column(name = "broker_port", nullable = false)
    @Builder.Default
    private Integer brokerPort = 8883;      // MQTT端口，默认8883(TLS)

    @Column(name = "mqtt_username", length = 100)
    private String mqttUsername;            // MQTT登录账号

    @Column(name = "mqtt_password", length = 100)
    private String mqttPassword;            // MQTT登录密码

    @Column(name = "use_ssl", nullable = false)
    @Builder.Default
    private Boolean useSsl = true;          // 是否启用SSL/TLS加密

    @Column(name = "ca_cert_path", length = 500)
    private String caCertPath;              // 上传的CA证书文件路径

    @Column(name = "connected", nullable = false)
    @Builder.Default
    private Boolean connected = false;      // 当前是否已连接

    @Column(name = "command_format", length = 20)
    @Builder.Default
    private String commandFormat = "JSON";  // 命令格式：JSON / led_on / on_off

    @Column(name = "subscriptions", columnDefinition = "TEXT")
    private String subscriptions;  // 已订阅的topic列表，逗号分隔，用于重连后恢复订阅

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;        // 创建时间

    // 持久化前自动调用：插入数据库前设置创建时间
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
