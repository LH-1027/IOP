# main.py - ESP32 综合项目入口（业务逻辑 + MQTT + 主循环）
# 20231819403027 曹尧岚
import network, ujson, time                 
from umqtt.simple import MQTTClient          
try: import ussl as ssl                      
except: import ssl                           
from config import SSID, PWD, MQ_SVR, MQ_PORT, MQ_USER, MQ_PWD, MQ_CA, DID, TP_SEN, TP_STA, TP_CTRL, TH_TEMP, TH_HUMI, TH_LIGHT
from hardware import p_dht, adc_l, btn, st, set_dev   # 传感器/按键对象、共享状态字典st、执行器控制函数


# ==================== MQTT 连接管理 ====================
mq = {"cli": None, "conn": False}            # cli:MQTT客户端对象  conn:是否已连接


# 连接 MQTT 服务器：读取证书 → 多套TLS方案依次尝试 → 注册回调并订阅控制主题
def mq_connect():
    # 读取 CA 证书
    ca = None
    try:
        with open(MQ_CA, 'rb') as f: ca = f.read()   # 二进制读取证书，用于验证服务器身份
    except: pass                                      # 读失败则 ca 保持 None，后面降级处理
    # 多种 TLS 参数兜底
    builders = []
    if ca: builders.append(lambda: {"server_hostname": MQ_SVR, "cert_reqs": ssl.CERT_REQUIRED, "cadata": ca})  # 带证书校验，最安全
    builders.append(lambda: {"server_hostname": MQ_SVR})   # 仅加密，不校验证书（兼容性最好）
    last = None                                        # 记录最后一次失败的错误，供全部失败时抛出
    for build in builders:                             # 依次尝试每套方案，谁先成功用谁
        try:
            # 创建客户端：客户端ID、服务器、端口、账号密码、60秒心跳、启用TLS、TLS参数
            cli = MQTTClient(DID, MQ_SVR, MQ_PORT, MQ_USER, MQ_PWD, keepalive=60, ssl=True, ssl_params=build())
            cli.set_callback(mq_cb)                     # 注册回调：收到订阅消息时自动调用 mq_cb
            cli.connect()                               # 发起 TCP+TLS+MQTT 连接
            cli.subscribe(TP_CTRL.encode(), qos=1)      # 订阅控制主题，之后才能收到下行命令
            mq["cli"] = cli; mq["conn"] = True          # 保存客户端、标记已连接
            return                                      # 成功即退出，不再尝试后续方案
        except Exception as e:
            last = e                                    # 本套方案失败，记下错误，继续试下一套
    mq["conn"] = False
    raise last if last else Exception("MQTT connect fail")  # 全部失败，抛错交给主循环重试


# 发布消息：把内容发送到指定 MQTT 主题，发布失败则标记断线
def mq_publish(tp, msg):
    if not mq["conn"]: return                           # 未连接直接跳过
    try: mq["cli"].publish(tp.encode(), msg.encode())   # 把消息发布到主题 tp
    except: mq["conn"] = False                          # 发布失败视为断线，主循环会重连


# 轮询接收消息：非阻塞检查是否有新消息，有则触发 mq_cb 回调
def mq_check():
    if not mq["conn"]: return
    try:
        mq["cli"].check_msg()           # 非阻塞：有消息就触发回调，没有立即返回
    except OSError as e:
        if e.args and e.args[0] == 11:  # EAGAIN：暂时没数据，正常
            return
        mq["conn"] = False              # 其它错误视为断线，主循环会自动重连
    except:
        mq["conn"] = False


# ==================== 核心业务逻辑 ====================
# MQTT 消息回调：收到控制命令时自动调用，解析JSON并执行开关/模式操作
def mq_cb(tp, msg):                                     # MQTT消息回调：tp=主题, msg=内容
    try:
        cmd = ujson.loads(msg.decode())                 # 字节解码为字符串再解析成字典
        ct, tg, val = cmd.get("cmd"), cmd.get("target"), cmd.get("value")  # 取出 命令/目标/值
        v_bool = val in ("on", "1", True)               # 把多种"开"的写法统一成布尔 True
        if ct == "set" and tg in ("led", "relay"): set_dev(tg, v_bool)     # 开关 LED 或继电器
        elif ct == "set" and tg == "mode": st["auto"] = v_bool             # 切换自动/手动模式
        elif ct == "toggle" and tg == "led": set_dev("led", not st["led"]) # 翻转 LED 状态
        pub_sta({"last_cmd": ct, "cmd_result": "ok"})   # 执行完回报状态，前端可同步更新
    except: pass                                        # 命令格式错误等异常一律忽略，避免崩溃


# 上报设备状态：把当前 LED/继电器/模式等状态打包成JSON发布到 status 主题
def pub_sta(extra=None):                                # 上报设备状态到 status 主题
    d = {"device_id": DID, "timestamp": time.time(), "led": "on" if st["led"] else "off", "relay": "on" if st["relay"] else "off", "mode": "auto" if st["auto"] else "manual"}
    if extra: d.update(extra)                           # 合并额外字段（如触发原因、命令结果）
    mq_publish(TP_STA, ujson.dumps(d))                  # 打包成JSON发布


# 主程序入口：永久循环，依次处理WiFi重连、MQTT收发、按键、心跳、传感器采集与自动控制
def run():
    print("系统启动中...")
    w = network.WLAN(network.STA_IF); w.active(True)    # 创建WiFi对象（STA客户端模式）并启用网卡

    lt_sen = lt_sta = time.ticks_ms()                   # 上次"采集"和"心跳"的时间戳，用于定时

    while True:                                         # 主循环：永久运行
        try:
            now = time.ticks_ms()                       # 当前毫秒计数

            # 1. WiFi 断线重连
            if not w.isconnected():
                print("正在连接 WiFi: %s ..." % SSID)
                w.connect(SSID, PWD)
                while not w.isconnected(): print("  等待 WiFi 连接中..."); time.sleep_ms(3000)
                print("WiFi 已连接, IP:", w.ifconfig()[0])

            # 2. MQTT 断线重连 & 接收消息
            if not mq["conn"]:                          # 未连接则尝试连接
                try:
                    print("正在连接 MQTT 服务器: %s:%d ..." % (MQ_SVR, MQ_PORT))
                    mq_connect()
                    print("MQTT 连接成功, 已订阅:", TP_CTRL)
                except Exception as e:
                    print("MQTT 连接失败, 1秒后重试:", e); time.sleep(1)
            mq_check()                                  # 轮询接收下行命令（触发 mq_cb）

            # 3. 硬件按键检测 (消抖 + 长短按)
            b_val = btn.value()                         # 读按键电平（上拉：松开=1，按下=0）
            if st["b_st"] == 1 and b_val == 0:          # 检测到"按下"瞬间
                st["b_ts"] = now; st["b_st"] = 0        # 记录按下时刻，状态改为"按下"
            elif st["b_st"] == 0 and b_val == 1:        # 检测到"松开"瞬间
                dur = time.ticks_diff(now, st["b_ts"]); st["b_st"] = 1   # 算按下时长
                if dur > 1000: st["auto"] = not st["auto"]; pub_sta({"btn": "long_press"})   # 长按>1s：切换模式
                elif dur > 50: set_dev("led", not st["led"]); pub_sta({"btn": "short_press"}) # 短按>50ms：翻转LED（兼消抖）

            # 4. MQTT 状态心跳 (10s)
            if time.ticks_diff(now, lt_sta) >= 10000:
                lt_sta = now; pub_sta({"type": "heartbeat"})            # 每10秒上报一次状态

            # 5. 传感器采集 & 自动模式判定 (5s)
            if time.ticks_diff(now, lt_sen) >= 5000:
                lt_sen = now
                try: p_dht.measure(); st["t"], st["h"] = p_dht.temperature(), p_dht.humidity()  # 读温湿度
                except: pass                            # DHT11偶尔读取失败，忽略本次

                # 光敏滑动平均滤波（新值占30%，平滑抖动）
                st["l"] = int(st["l"] * 0.7 + adc_l.read() * 0.3) if st["l"] else adc_l.read()

                print(f"数据 -> 温度:{st['t']}C, 湿度:{st['h']}%, 光照:{st['l']}")

                # 自动模式判定：任一指标越界则触发报警
                trig = st["t"] > TH_TEMP or st["h"] > TH_HUMI or st["l"] < TH_LIGHT
                if st["auto"] and trig != st["led"]:    # 仅自动模式下、且状态需变化时才动作
                    set_dev("led", trig); set_dev("relay", trig)        # 同步开关灯和继电器
                    pub_sta({"trigger": "auto"})

                # 上传传感器数据到 sensor 主题
                mq_publish(TP_SEN, ujson.dumps({"device_id": DID, "timestamp": time.time(), "temperature": st["t"], "humidity": st["h"], "light": st["l"]}))

            time.sleep_ms(100)                          # 每轮歇100ms，降低CPU占用

        except Exception as e:
            print("循环异常:", e)                        # 捕获所有异常，保证主循环不崩溃
            time.sleep(1)


if __name__ == "__main__":                              # 作为主程序运行时才启动
    run()
