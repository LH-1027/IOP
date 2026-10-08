# IoT-ESP32 物联网远程监控系统
> 设计项目：基于ESP32‑S3 + EMQX(MQTT over TLS) + SpringBoot + Vue3 的环境感知与远程监控平台

## 📖 项目简介
本项目实现一套完整物联网远程监控系统，严格遵循物联网**感知层‑网络层‑应用层**三层架构。
- **感知层**：ESP32‑S3(MicroPython)采集温度、湿度、光照；控制LED、继电器；支持本地按键操作、越限自动报警、WiFi/MQTT断线自动重连。
- **网络层**：EMQX Cloud MQTT Broker，使用`MQTT over TLS 8883`加密传输，发布‑订阅模式实现设备与服务端消息中转。
- **应用层**：SpringBoot后端提供设备管理、MQTT客户端管理、JWT用户认证、WebSocket实时消息推送；Vue3前端实现Web可视化监控面板、远程下发控制指令。

传统本地监测设备只能现场查看，商用云平台封闭收费。本项目可浏览器随时随地查看环境数据，下发控制命令，适合物联网课程学习研究。

## ✨ 主要功能
### 📟 ESP32设备端（感知与控制）
1. 环境采集：DHT11温湿度，光敏电阻ADC光照采集；**一阶滑动平均滤波**抑制采样抖动，5秒采集一次数据。
2. 加密上报：JSON格式通过TLS加密MQTT上传传感器数据、设备心跳状态。
3. 远程控制：订阅控制主题，解析JSON命令远程开关LED、继电器，切换手动/自动模式。
4. 本地按键控制：软件消抖；**短按翻转LED，长按1秒切换自动/手动模式**，断网依旧可以本地操作。
5. 自动报警模式：温度>30℃ 或湿度>75% 或光照<1000，触发报警联动LED、继电器并上报报警原因。
6. 断线自愈：WiFi、MQTT断开自动重连；主循环异常捕获兜底，保证长时间稳定运行。

### 🧩 SpringBoot后端
1. 用户模块：JWT登录注册，BCrypt密码加密，设备按用户隔离。
2. 设备管理：设备增删改查，保存MQTT服务地址、账号密码、CA证书、命令格式等配置存入MySQL。
3. MQTT连接管理器：每个设备独立MQTT客户端，支持TLS证书校验、断线重连；动态订阅/取消主题。
4. 消息处理：缓存设备最近100条消息；通过WebSocket把设备实时消息推送到前端浏览器。

### 🖥️ Vue3 Web前端
1. 用户登录注册，Token持久化。
2. 设备总览卡片，展示设备在线状态，支持添加设备（支持上传CA证书）、删除设备。
3. 设备详情页：仪表展示温湿度光照；实时消息日志终端。
4. 远程控制面板：一键控制LED、继电器；自动兼容5种不同设备命令报文格式。
5. 高级功能：手动订阅/取消MQTT主题，自定义发布消息。

## 🏗️ 系统架构与数据流
> 主题命名格式：`esp32_project/{device_id}/{type}`

|方向|MQTT主题|QoS|说明|
|----|--------|---|----|
|设备 → 云端|esp32_project/esp32_20231819403027/sensor|0|温湿度光照传感器数据|
|设备 → 云端|esp32_project/esp32_20231819403027/status|0|设备心跳、状态上报|
|云端 → 设备|esp32_project/esp32_20231819403027/control|1|下发控制命令|

**上行数据流**
传感器 → ESP32 → EMQX Broker(sensor主题) → SpringBoot后端 → WebSocket → Vue前端页面

**下行控制流**
Vue前端 → SpringBoot后端 → EMQX Broker(control主题) → ESP32设备执行动作

### JSON报文示例
传感器上报：
```json
{
  "device_id":"esp32_20231819403027",
  "timestamp":1712345678,
  "temperature":28,
  "humidity":65,
  "light":2048
}
```

下发控制命令：
```json
{"cmd":"set","target":"led","value":"on"}
{"cmd":"set","target":"relay","value":"on"}
{"cmd":"set","target":"mode","value":"off"}
```

## 🔌 硬件引脚分配
|器件|GPIO引脚|功能说明|
|---|---|---|
|DHT11温湿度传感器|GPIO16|数字输入，采集温湿度|
|光敏电阻模块|GPIO4|ADC模拟输入，光照采集|
|红色LED指示灯|GPIO38|数字输出，报警与状态指示，串联限流电阻|
|继电器模块|GPIO7|数字输出，驱动外部负载|
|轻触按键|GPIO5|数字输入，内部上拉；短按/长按识别|

> 供电说明：DHT11、光敏电阻使用3.3V供电；按键内部上拉，按下电平为低电平0。

## 📂 项目目录结构
```
IoT-ESP32-Monitor
├── esp32-device/                # ESP32 MicroPython设备端代码
│   ├── config.py                # WiFi/MQTT/阈值全部配置项
│   ├── hardware.py              # 硬件引脚初始化、设备控制函数、全局状态字典
│   ├── main.py                  # 主循环、MQTT连接、业务逻辑
│   └── umqtt/simple.py          # MicroPython官方MQTT客户端库
├── springboot-backend/          # SpringBoot后端工程
│   ├── src/main/java            # Java源码 MqttConnectionManager、JWT鉴权、WebSocket
│   └── src/main/resources       # application配置、数据库SQL脚本
├── vue-frontend/                # Vue3+Vite+ElementPlus前端
│   ├── src/
│   │   ├── views/               # 登录页、设备总览、设备详情页面
│   │   ├── store/               # Pinia状态管理
│   │   └── router/              # Vue路由
├── doc/                         # 课程设计报告、接线图、架构图
└── README.md                    # 本说明文档
```

## 🚀 快速部署运行
### 1. 准备工作
1. 准备ESP32‑S3开发板；DHT11、光敏电阻、继电器、按键、LED等外设，按照引脚表接线。
2. 注册EMQX Cloud，创建MQTT实例，获取连接地址、端口8883(TLS)、用户名密码，下载CA证书。
3. 本地安装MySQL，创建对应数据库。
4. MicroPython固件烧录到ESP32‑S3。

### 2. ESP32设备端部署
1. 修改`config.py`，填入WiFi账号密码，MQTT服务信息、设备ID、报警阈值。
2. 将全部py文件上传至ESP32开发板。
3. 复位ESP32，串口查看输出日志，观察WiFi、MQTT连接状态，传感器打印数据。

> 注意：部分MicroPython固件ssl缺少`CERT_REQUIRED`属性，代码内置lambda延迟构造TLS参数，自动降级兼容。

### 3. SpringBoot后端部署
1. 修改`application.yml`：配置MySQL数据库、EMQX相关参数。
2. 启动SpringBoot服务；后端会维护每台设备独立MQTT客户端，开启WebSocket端点`/ws/{deviceId}`。
3. 数据库表自动生成(JPA)：`users`用户表，`devices`设备表。

### 4. Vue前端部署
```bash
cd vue-frontend
npm install
npm run dev
```
访问 `[http://localhost:3000](http://localhost:3000)`，注册用户，添加设备填入MQTT参数，即可查看实时数据和远程控制。

## ⚠️ 开发踩坑记录
1. **TLS握手报错 `no attribute 'CERT_REQUIRED'`**
固件ssl模块缺少属性；解决方案：lambda延迟构造TLS参数，准备两套TLS配置，异常自动降级为仅加密不校验证书。

2. 手写MQTT二进制报文连接失败
自行组装CONNECT报文剩余长度计算错误；改用官方umqtt.simple库，屏蔽底层协议细节。

3. 多设备命令格式不统一
不同设备控制报文格式不一样；数据库增加`commandFormat`字段，前端根据配置自动生成多种格式控制报文。

4. CA证书绝对路径问题
更换环境路径失效；证书加载异常自动回退JDK默认SSL；设备端全部网络操作增加try‑except，交由主循环自动重连。

## 📚 技术栈
**设备端**
- MicroPython
- umqtt.simple(MQTT客户端)
- DHT11传感器驱动

**后端**
- Spring Boot
- Spring Data JPA
- MySQL
- Eclipse Paho MQTT Java Client
- WebSocket
- JWT、BCrypt加密

**前端**
- Vue3 + Vite
- Element‑Plus
- Pinia
- Vue‑Router

**网络服务**
- EMQX Cloud MQTT Broker，MQTT 3.1.1 over TLS

## 📖 参考文献
[1] OASIS. MQTT Version 3.1.1 [S]. OASIS Standard, 2014.
[2] EMQX. EMQX Cloud 文档 [EB/OL].
[3] MicroPython. MicroPython 官方文档 [EB/OL].

## 📝 项目总结
本项目完整复现物联网三层架构，从硬件采集、MQTT加密通信、后端消息转发，到Web可视化控制全部链路打通。
既实现基础环境监测与远程控制，同时增加本地按键、断线自愈、多用户多设备平台化管理等扩展功能；在开发过程中处理固件兼容、网络抖动、多协议适配等工程实际问题，适合物联网课程设计学习。
