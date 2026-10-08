/*
 Navicat Premium Data Transfer

 Source Server         : localhost
 Source Server Type    : MySQL
 Source Server Version : 80042 (8.0.42)
 Source Host           : localhost:3306
 Source Schema         : iot_platform

 Target Server Type    : MySQL
 Target Server Version : 80042 (8.0.42)
 File Encoding         : 65001

 Date: 18/06/2026 14:22:22
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for devices
-- ----------------------------
DROP TABLE IF EXISTS `devices`;
CREATE TABLE `devices`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `device_id` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `owner_id` bigint NOT NULL,
  `broker_url` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `broker_port` int NOT NULL DEFAULT 8883,
  `mqtt_username` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `mqtt_password` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `use_ssl` tinyint(1) NOT NULL DEFAULT 1,
  `ca_cert_path` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `connected` tinyint(1) NOT NULL DEFAULT 0,
  `command_format` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT 'JSON',
  `subscriptions` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `owner_id`(`owner_id` ASC) USING BTREE,
  CONSTRAINT `devices_ibfk_1` FOREIGN KEY (`owner_id`) REFERENCES `users` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 7 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of devices
-- ----------------------------
INSERT INTO `devices` VALUES (1, 'zty', NULL, 1, 'f93a9d44.ala.cn-hangzhou.emqxsl.cn', 8883, 'esp32', '123456', 1, 'E:\\2025-2026-2\\物联网技术及应用\\作业\\综合项目-ESP32_MQTT_远程控制\\code\\backend\\uploads\\certs\\1\\1.crt', 0, 'led_on', 'esp32/#', '2026-06-13 22:16:33');
INSERT INTO `devices` VALUES (2, 'cyl', NULL, 1, 't8151952.ala.cn-hangzhou.emqxsl.cn', 8883, 'cyl', 'cyl666', 1, 'E:\\2025-2026-2\\物联网技术及应用\\作业\\综合项目-ESP32_MQTT_远程控制\\code\\backend\\uploads\\certs\\1\\2.crt', 0, 'JSON', 'esp32_project/esp32_20231819403027/#', '2026-06-13 22:17:33');
INSERT INTO `devices` VALUES (6, 'ljc', NULL, 1, 'ha389383.ala.cn-hangzhou.emqxsl.cn', 8883, 'user1', 'rootroot', 1, 'E:\\2025-2026-2\\物联网技术及应用\\作业\\综合项目-ESP32_MQTT_远程控制\\code\\backend\\uploads\\certs\\1\\6.crt', 0, 'LED_ON', 'device/#', '2026-06-13 22:29:17');

-- ----------------------------
-- Table structure for users
-- ----------------------------
DROP TABLE IF EXISTS `users`;
CREATE TABLE `users`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `username` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `password` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `email` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `username`(`username` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of users
-- ----------------------------
INSERT INTO `users` VALUES (1, 'cyl', '$2a$10$0P.uWzjUX57EXilwnIixxekBoNJ9UuHx6IaF5hEw5bfb3GWs9Yb6u', '', '2026-06-13 22:15:58');

SET FOREIGN_KEY_CHECKS = 1;
