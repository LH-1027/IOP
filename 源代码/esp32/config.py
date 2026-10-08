# config.py - 配置项
# 20231819403027 曹尧岚

# ==================== WiFi ====================
SSID = "Xiaomi 14"
PWD = "cyl20031027"

# ==================== MQTT 服务器 ====================
MQ_SVR = "t8151952.ala.cn-hangzhou.emqxsl.cn"
MQ_PORT = 8883
MQ_USER = "cyl"
MQ_PWD = "cyl666"
MQ_CA = "emqxsl-ca.crt"

# ==================== 设备与主题 ====================
DID = "esp32_20231819403027"
TP_SEN = f"esp32_project/{DID}/sensor"
TP_STA = f"esp32_project/{DID}/status"
TP_CTRL = f"esp32_project/{DID}/control"

# ==================== 自动模式报警阈值 ====================
TH_TEMP = 30      # 温度 (℃)
TH_HUMI = 75      # 湿度 (%)
TH_LIGHT = 1000   # 光照 (ADC, 低于此值触发)
