-- IoT MQTT Manager 数据库初始化
DROP DATABASE IF EXISTS iot_platform;
CREATE DATABASE iot_platform
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;

USE iot_platform;

CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    email VARCHAR(100),
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

CREATE TABLE devices (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    device_id VARCHAR(100),
    owner_id BIGINT NOT NULL,
    broker_url VARCHAR(255) NOT NULL,
    broker_port INT NOT NULL DEFAULT 8883,
    mqtt_username VARCHAR(100),
    mqtt_password VARCHAR(100),
    use_ssl TINYINT(1) NOT NULL DEFAULT 1,
    ca_cert_path VARCHAR(500),
    connected TINYINT(1) NOT NULL DEFAULT 0,
    command_format VARCHAR(20) DEFAULT 'JSON',
    subscriptions TEXT,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (owner_id) REFERENCES users(id)
) ENGINE=InnoDB;
