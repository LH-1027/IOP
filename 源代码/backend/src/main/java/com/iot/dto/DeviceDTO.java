package com.iot.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;
import java.time.LocalDateTime;

// 设备相关的数据传输对象(DTO)集合：定义前后端交互时各请求/响应的数据格式
public class DeviceDTO {

    // 添加设备请求：前端新增设备时提交的字段
    @Data
    public static class AddDeviceRequest {
        private String name;                    // 设备名称
        private String brokerUrl;               // MQTT服务器地址
        private Integer brokerPort = 8883;      // 端口
        private String mqttUsername;            // 账号
        private String mqttPassword;            // 密码
        private Boolean useSsl = true;          // 是否SSL
        private String commandFormat = "JSON";  // 命令格式
    }

    // 修改命令格式请求
    @Data
    public static class UpdateFormatRequest {
        private String commandFormat;
    }

    // 订阅请求：要订阅的主题和QoS等级
    @Data
    public static class SubscribeRequest {
        private String topic;
        private Integer qos = 0;
    }

    // 发布请求：目标主题、消息内容、QoS等级
    @Data
    public static class PublishRequest {
        private String topic;
        private String payload;
        private Integer qos = 0;
    }

    // 设备响应：返回给前端展示的设备信息（不含密码等敏感字段）
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DeviceResponse {
        private Long id;
        private String name;
        private String ownerName;        // 所属用户名
        private boolean isOwner;         // 当前用户是否为设备拥有者
        private String brokerUrl;
        private Integer brokerPort;
        private Boolean useSsl;
        private Boolean connected;       // 是否已连接
        private String commandFormat;
        private LocalDateTime createdAt;
    }
}
