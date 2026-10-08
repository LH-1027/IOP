# hardware.py - 硬件初始化与设备控制
# 20231819403027 曹尧岚
import machine, dht                          # machine:操作GPIO/ADC等底层硬件  dht:温湿度传感器驱动

# ==================== 传感器 ====================
p_dht = dht.DHT11(machine.Pin(16))           # DHT11温湿度传感器，数据引脚接 GPIO16
adc_l = machine.ADC(machine.Pin(4)); adc_l.atten(machine.ADC.ATTN_11DB)  # 光敏ADC接GPIO4，11dB衰减使量程约0~3.3V

# ==================== 执行器与指示灯 ====================
led_r = machine.Pin(38, machine.Pin.OUT)     # LED指示灯，GPIO38，输出模式
relay = machine.Pin(7, machine.Pin.OUT)      # 继电器，GPIO7，输出模式
btn = machine.Pin(5, machine.Pin.IN, machine.Pin.PULL_UP)  # 按键，GPIO5，输入+上拉（松开=1，按下=0）

# 全局状态字典
# led/relay:执行器开关  auto:是否自动模式  t/h/l:最新温度/湿度/光照  b_st:按键状态  b_ts:按下时刻
st = {"led": False, "relay": False, "auto": True, "t": 0, "h": 0, "l": 0, "b_st": 1, "b_ts": 0}

# 控制执行器开关：tg 指定目标(led/relay)，val 为True则通电，同时更新状态字典与GPIO电平
def set_dev(tg, val):
    if tg == "led": st["led"] = val; led_r.value(1 if val else 0)      # 开关LED并记录状态
    elif tg == "relay": st["relay"] = val; relay.value(1 if val else 0) # 开关继电器并记录状态
