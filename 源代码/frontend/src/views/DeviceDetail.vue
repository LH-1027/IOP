<template>
  <div class="detail-page">
    <header class="header">
      <button class="back-link" @click="$router.push('/')">← 返回控制台</button>
      <div class="header-core">
        <h2>{{ device?.name || '未知设备' }}</h2>
        <span :class="['mini-status-chip', device?.connected ? 'live' : 'dead']">
         {{ device?.connected ? '已连接' : '未连接' }}
        </span>
      </div>
      <div style="flex:1"></div>
      <div class="action-cluster">
        <button class="node-action-btn connect" @click="handleConnect" :loading="connecting" :disabled="device?.connected">连接</button>
        <button class="node-action-btn disconnect" @click="handleDisconnect" :disabled="!device?.connected">断开</button>
      </div>
    </header>

    <div class="content" v-if="device">
      <div class="telemetry-bento">
        <div class="bento-box temp">
          <div class="box-head">
            <span class="box-icon">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <path d="M14 14.76V3.5a2.5 2.5 0 0 0-5 0v11.26a4.5 4.5 0 1 0 5 0z" />
              </svg>
            </span>
            <span class="box-label">温度</span>
          </div>
          <div class="box-main">
            <span class="num">{{ sensorData.temp ?? '--' }}</span>
            <span class="unit">°C</span>
          </div>
        </div>
        <div class="bento-box humi">
          <div class="box-head">
            <span class="box-icon">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <path d="M12 2.69l5.66 5.66a8 8 0 1 1-11.31 0z" />
              </svg>
            </span>
            <span class="box-label">湿度</span>
          </div>
          <div class="box-main">
            <span class="num">{{ sensorData.humi ?? '--' }}</span>
            <span class="unit">%</span>
          </div>
        </div>
        <div class="bento-box light">
          <div class="box-head">
            <span class="box-icon">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <circle cx="12" cy="12" r="5" />
                <line x1="12" y1="1" x2="12" y2="3" />
                <line x1="12" y1="21" x2="12" y2="23" />
                <line x1="4.22" y1="4.22" x2="5.64" y2="5.64" />
                <line x1="18.36" y1="18.36" x2="19.78" y2="19.78" />
                <line x1="1" y1="12" x2="3" y2="12" />
                <line x1="21" y1="12" x2="23" y2="12" />
                <line x1="4.22" y1="19.78" x2="5.64" y2="18.36" />
                <line x1="18.36" y1="5.64" x2="19.78" y2="4.22" />
              </svg>
            </span>
            <span class="box-label">光照强度</span>
          </div>
          <div class="box-main">
            <span class="num">{{ lightPercent }}</span>
          </div>
        </div>
      </div>

      <div class="grid-layout">
        <div class="left-column">
          <div class="tech-panel">
            <div class="panel-header">
              <span class="panel-icon">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                  <line x1="4" y1="21" x2="4" y2="14" /><line x1="4" y1="10" x2="4" y2="3" />
                  <line x1="12" y1="21" x2="12" y2="12" /><line x1="12" y1="8" x2="12" y2="3" />
                  <line x1="20" y1="21" x2="20" y2="16" /><line x1="20" y1="12" x2="20" y2="3" />
                  <line x1="1" y1="14" x2="7" y2="14" /><line x1="9" y1="8" x2="15" y2="8" /><line x1="17" y1="16" x2="23" y2="16" />
                </svg>
              </span>
              <span>设备控制 · 远程开关执行器</span>
            </div>
            <div class="actuator-grid">
              <div class="actuator-item">
                <div class="actuator-meta">
                  <span class="name">LED 指示灯</span>
                  <span :class="['state-badge', devStatus.led === 'on' ? 'on' : 'off']">{{ devStatus.led === 'on' ? '已开启' : '已关闭' }}</span>
                </div>
                <div class="binary-controls">
                  <button @click="sendControl('led','on')" class="bin-btn green">开灯</button>
                  <button @click="sendControl('led','off')" class="bin-btn red">关灯</button>
                </div>
              </div>

              <div class="actuator-item">
                <div class="actuator-meta">
                  <span class="name">继电器</span>
                  <span :class="['state-badge', devStatus.relay === 'on' ? 'on' : 'off']">{{ devStatus.relay === 'on' ? '已吸合' : '已断开' }}</span>
                </div>
                <div class="binary-controls">
                  <button @click="sendControl('relay','on')" class="bin-btn green">吸合</button>
                  <button @click="sendControl('relay','off')" class="bin-btn red">断开</button>
                </div>
              </div>
            </div>

            <div class="topic-bus">
              <span class="bus-lbl">发送主题：</span>
              <input v-model="ctrlTopic" class="bus-input" />
              <span class="syntax-tag">{{ device?.commandFormat || 'JSON' }}</span>
            </div>
          </div>

          <div class="tech-panel">
            <div class="panel-header">
              <span class="panel-icon">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                  <path d="M5 12.55a11 11 0 0 1 14.08 0" /><path d="M1.42 9a16 16 0 0 1 21.16 0" />
                  <path d="M8.53 16.11a6 6 0 0 1 6.95 0" /><line x1="12" y1="20" x2="12.01" y2="20" />
                </svg>
              </span>
              <span>订阅主题 · 监听设备上报数据</span>
            </div>
            <div class="matrix-form">
              <input v-model="subTopic" placeholder="订阅 Topic，如：esp32_project/设备ID/sensor" class="matrix-input" />
              <select v-model="subQos" class="matrix-select">
                <option :value="0">QoS 0</option>
                <option :value="1">QoS 1</option>
              </select>
              <button class="matrix-btn" @click="handleSubscribe" :disabled="!device?.connected">订阅</button>
            </div>
            <div class="matrix-pool" v-if="subscriptions.length">
              <div v-for="sub in subscriptions" :key="sub" class="matrix-tag">
                <span>{{ sub }}</span>
                <span class="tag-close" @click="handleUnsubscribe(sub)">×</span>
              </div>
            </div>
          </div>

          <div class="tech-panel">
            <div class="panel-header">
              <span class="panel-icon">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                  <line x1="22" y1="2" x2="11" y2="13" /><polygon points="22 2 15 22 11 13 2 9 22 2" />
                </svg>
              </span>
              <span>手动发布 · 自定义发送消息</span>
            </div>
            <input v-model="pubTopic" placeholder="目标主题 Topic" class="matrix-input" style="margin-bottom: 12px;" />
            <textarea v-model="pubPayload" rows="3" placeholder="消息内容（文本 / JSON）" class="matrix-textarea"></textarea>
            <div class="matrix-form" style="margin-top: 12px;">
              <select v-model="pubQos" class="matrix-select" style="width: 100px;">
                <option :value="0">QoS 0</option>
                <option :value="1">QoS 1</option>
              </select>
              <div style="flex:1"></div>
              <button class="matrix-btn injection" @click="handlePublish" :disabled="!device?.connected">发布</button>
            </div>
          </div>
        </div>

        <div class="right-column">
          <div class="tech-panel console-panel">
            <div class="panel-header console-header">
              <span class="header-title">
                <span class="panel-icon">
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                    <polyline points="22 12 18 12 15 21 9 3 6 12 2 12" />
                  </svg>
                </span>
                <span>实时消息日志 · 设备上报数据</span>
              </span>
              <button class="clear-terminal-btn" @click="messages = []">清空</button>
            </div>
            <div class="terminal-view" ref="msgLogRef">
              <div v-if="messages.length === 0" class="terminal-idle">等待接收设备数据...</div>
              <div v-for="(msg, i) in messages" :key="i" class="terminal-row">
                <div class="row-top">
                  <span class="t-time">[{{ formatTime(msg.timestamp) }}]</span>
                  <span class="t-topic"># {{ msg.topic }}</span>
                </div>
                <div class="row-payload">&gt; {{ msg.payload }}</div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onUnmounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import { getDeviceDetail, connectDevice, disconnectDevice, getSubscriptions,
         subscribeTopic, unsubscribeTopic, publishMessage } from '../api/devices'

// useRoute()拿到当前路由信息，route.params.id 就是URL里的设备ID（/device/5 → 5）
const route = useRoute()

// —— 响应式数据：值一变，页面自动刷新 ——
const device = ref(null)          // 当前设备信息
const connecting = ref(false)     // 是否正在连接中
const messages = ref([])          // 实时消息日志列表
const subscriptions = ref([])     // 已订阅的主题列表
const msgLogRef = ref(null)       // 指向日志DOM元素的引用

const subTopic = ref('')          // 订阅输入框内容
const subQos = ref(0)             // 订阅QoS
const pubTopic = ref('')          // 手动发布的主题
const pubPayload = ref('')        // 手动发布的内容
const pubQos = ref(0)             // 手动发布的QoS
// 生成localStorage的存储键，按设备ID区分，避免不同设备互相覆盖
const cmdKey = (k) => `mqtt_cmd_${route.params.id}_${k}`
// 控制命令要发到的主题。优先读本地保存的，没有则默认 esp32/control
const ctrlTopic = ref(localStorage.getItem(cmdKey('topic')) || 'esp32/control')

// 监听控制主题变化，一改就存进localStorage
watch(ctrlTopic, v => localStorage.setItem(cmdKey('topic'), v))

// 根据设备的"命令格式"生成要发送的命令内容。
// 同样是开灯，不同设备可能要不同写法：JSON对象 / led_on / on / ON 等
function getCommand(target, value, format) {
  const fmt = format || 'JSON'
  if (fmt === 'led_on') return target === 'led' ? `led_${value}` : `relay_${value}`
  if (fmt === 'on_off') return value
  if (fmt === 'ON_OFF') return value.toUpperCase()
  if (fmt === 'LED_ON') return target === 'led' ? `LED_${value.toUpperCase()}` : `RELAY_${value.toUpperCase()}`
  // 默认JSON格式：{"cmd":"set","target":"led","value":"on"}（对应ESP32的mq_cb解析）
  return JSON.stringify({ cmd: 'set', target, value })
}

// 传感器数据（显示在三个卡片里）和执行器状态（开关徽章）
const sensorData = reactive({ temp: null, humi: null, light: null })
const devStatus = reactive({ led: null, relay: null })
let ws = null    // WebSocket连接对象

// 计算属性：把光照原始值(0~4095)换算成百分比显示。light变了会自动重算
const lightPercent = computed(() =>
  sensorData.light != null ? Math.round(sensorData.light / 4095 * 100) + '%' : '--'
)

// 获取设备详情（页面打开时调用）
async function fetchDevice() {
  try {
    const res = await getDeviceDetail(route.params.id)
    if (res.code === 200) device.value = res.data
  } catch (e) {}
}

// 获取已订阅列表，并智能推断控制主题（把sensor/status主题改成control主题）
async function fetchSubs() {
  try {
    const res = await getSubscriptions(route.params.id)
    if (res.code === 200) {
      subscriptions.value = res.data
      for (const sub of res.data) {
        const derived = sub.replace(/\/#/, '/control').replace(/\/sensor$/, '/control').replace(/\/status$/, '/control').replace(/\/data$/, '/control')
        if (derived !== sub && (ctrlTopic.value === 'esp32/control' || !ctrlTopic.value)) {
          ctrlTopic.value = derived
          break
        }
      }
    }
  } catch (e) {}
}

// 点"连接"按钮：让后端连MQTT，成功后建立WebSocket开始接收实时数据
async function handleConnect() {
  connecting.value = true
  try {
    const res = await connectDevice(route.params.id)
    if (res.code === 200) {
      device.value.connected = true
      connectWebSocket()        // 连上后才开WebSocket接数据
    }
  } catch (e) {}
  connecting.value = false
}

// 点"断开"按钮：让后端断开MQTT，并关闭WebSocket
async function handleDisconnect() {
  try {
    await disconnectDevice(route.params.id)
    device.value.connected = false
    if (ws) ws.close()
  } catch (e) {}
}

// 点"订阅"按钮：订阅输入框里的主题
async function handleSubscribe() {
  if (!subTopic.value.trim()) return
  const topic = subTopic.value.trim()
  try {
    await subscribeTopic(route.params.id, { topic, qos: subQos.value })
    subTopic.value = ''
    fetchSubs()
    if (!ctrlTopic.value || ctrlTopic.value === 'esp32/control') {
      const derived = topic.replace(/\/#/, '/control').replace(/\/sensor/, '/control').replace(/\/status/, '/control')
      if (derived !== topic) ctrlTopic.value = derived
    }
  } catch (e) {}
}

// 点订阅标签上的"×"：取消订阅该主题
async function handleUnsubscribe(topic) {
  try {
    await unsubscribeTopic(route.params.id, { topic })
    fetchSubs()
  } catch (e) {}
}

// 点开关按钮（开灯/关灯/继电器）：生成命令并发布到控制主题
async function sendControl(target, value) {
  if (!ctrlTopic.value.trim() || !device.value?.connected) return
  const payload = getCommand(target, value, device.value?.commandFormat)  // 按格式生成命令
  try {
    await publishMessage(route.params.id, {
      topic: ctrlTopic.value.trim(),    // 发到控制主题
      payload,
      qos: 1
    })
    // 更新界面徽章
    if (target === 'led') devStatus.led = value
    else if (target === 'relay') devStatus.relay = value
  } catch (e) {}
}

// 点"发布"按钮：手动发送自定义主题和内容
async function handlePublish() {
  if (!pubTopic.value.trim()) return
  try {
    await publishMessage(route.params.id, {
      topic: pubTopic.value.trim(),
      payload: pubPayload.value,
      qos: pubQos.value
    })
    pubPayload.value = ''
  } catch (e) {}
}

// 建立WebSocket长连接，接收后端推来的实时消息
function connectWebSocket() {
  if (ws) ws.close()                                          // 先关掉旧连接
  const proto = location.protocol === 'https:' ? 'wss:' : 'ws:'   // https用wss，http用ws
  // 连接地址带上设备ID，后端据此知道该把哪台设备的消息推给我
  ws = new WebSocket(`${proto}//${location.host}/ws/${route.params.id}`)
  // onmessage：后端每推来一条消息就触发一次
  ws.onmessage = (e) => {
    try {
      const msg = JSON.parse(e.data)                          // 解析后端拼的JSON
      if (msg.type === 'message') {
        messages.value.unshift({ topic: msg.topic, payload: msg.payload, timestamp: msg.timestamp })  // 加到日志顶部
        if (messages.value.length > 100) messages.value.pop() // 超100条删最旧的
        parseMessage(msg.topic, msg.payload)                  // 提取温湿度等数据更新卡片
      }
    } catch (ex) {}
  }
}

// 解析消息内容，提取温湿度/光照/开关状态，填进卡片和徽章
// 兼容两种格式：JSON对象  "key:value,key:value"   文本
function parseMessage(topic, payload) {
  try {
    const data = JSON.parse(payload)
    // 兼容字段全名和简写（temperature/temp 都认）
    if (data.temperature != null) sensorData.temp = data.temperature
    if (data.temp != null) sensorData.temp = data.temp
    if (data.humidity != null) sensorData.humi = data.humidity
    if (data.humi != null) sensorData.humi = data.humi
    if (data.light != null) sensorData.light = data.light
    // 开关状态：1/'1'/'on' 都算开
    if (data.led != null) devStatus.led = (data.led === 1 || data.led === '1' || data.led === 'on') ? 'on' : 'off'
    if (data.relay != null) devStatus.relay = (data.relay === 1 || data.relay === '1' || data.relay === 'on') ? 'on' : 'off'
    return    // JSON解析成功就结束
  } catch (e) {}

  // —— JSON解析失败，再尝试当"温度:25,湿度:60"这种文本解析 ——
  try {
    const parts = payload.split(',')        // 按逗号拆成若干 key:value
    for (const p of parts) {
      const [k, v] = p.split(':')
      if (!k || v == null) continue
      const val = parseFloat(v)
      const str = v.trim()
      if (k.includes('温度') || k.includes('temp')) sensorData.temp = val
      if (k.includes('湿度') || k.includes('humi')) sensorData.humi = val
      if (k.includes('光照') || k.includes('光线') || k.includes('light')) sensorData.light = val
      if (k.includes('led')) devStatus.led = (str === '1' || str === 'on') ? 'on' : 'off'
      if (k.includes('relay')) devStatus.relay = (str === '1' || str === 'on') ? 'on' : 'off'
    }
  } catch (e) {}
}

// 把时间戳格式化成"时:分:秒"显示在日志里
function formatTime(ts) {
  return new Date(ts).toLocaleTimeString('zh-CN')
}

// onMounted：页面加载完成后自动执行（初始化）
onMounted(async () => {
  await fetchDevice()                                  // 1.拿设备信息
  await fetchSubs()                                    // 2.拿订阅列表
  if (device.value?.connected) connectWebSocket()      // 3.若已连接则开WebSocket接数据
})

// onUnmounted：页面关闭/离开时自动执行，关闭WebSocket释放资源
onUnmounted(() => { if (ws) ws.close() })
</script>

<style scoped>
.detail-page { min-height: 100vh; }

.header {
  background: rgba(255, 255, 255, 0.7);
  backdrop-filter: blur(12px);
  padding: 16px 40px;
  display: flex;
  align-items: center;
  border-bottom: 1px solid var(--line);
  position: sticky;
  top: 0;
  z-index: 10;
}

.back-link {
  background: transparent;
  border: none;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  color: var(--muted);
  margin-right: 24px;
  transition: color 0.15s;
}
.back-link:hover { color: var(--brand); }

.header-core {
  display: flex;
  align-items: center;
  gap: 14px;
}

.header-core h2 {
  font-size: 20px;
  font-weight: 800;
  color: var(--ink);
}

.mini-status-chip {
  font-size: 12px;
  font-weight: 600;
  padding: 3px 12px;
  border-radius: 999px;
}
.mini-status-chip.live { background: #dcfce7; color: #15803d; }
.mini-status-chip.dead { background: #f1f5f9; color: #64748b; }

.action-cluster { display: flex; gap: 10px; }

.node-action-btn {
  border: 1px solid #e2e8f0;
  border-radius: 10px;
  padding: 8px 18px;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.18s;
}
.node-action-btn.connect {
  background: linear-gradient(135deg, #6366f1, #8b5cf6);
  border: none;
  color: #ffffff;
  box-shadow: 0 6px 16px rgba(99, 102, 241, 0.25);
}
.node-action-btn.connect:hover:not(:disabled) { transform: translateY(-1px); }
.node-action-btn.disconnect { background: #ffffff; color: #ef4444; border-color: #fecaca; }
.node-action-btn.disconnect:hover:not(:disabled) { background: #fef2f2; }
.node-action-btn:disabled { opacity: 0.4; cursor: not-allowed; }

.content {
  max-width: 1400px;
  margin: 0 auto;
  padding: 32px 40px;
}

/* 数据指标卡 */
.telemetry-bento {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 22px;
  margin-bottom: 24px;
}

.bento-box {
  background: #ffffff;
  border: 1px solid var(--line);
  border-radius: var(--radius);
  padding: 24px;
  box-shadow: var(--shadow-sm);
  position: relative;
  overflow: hidden;
  transition: transform 0.2s, box-shadow 0.2s;
}
.bento-box::before {
  content: '';
  position: absolute;
  top: 0; left: 0; bottom: 0;
  width: 4px;
}
.bento-box.temp::before { background: linear-gradient(180deg, #fb7185, #f43f5e); }
.bento-box.humi::before { background: linear-gradient(180deg, #38bdf8, #0ea5e9); }
.bento-box.light::before { background: linear-gradient(180deg, #fbbf24, #f59e0b); }
.bento-box:hover { transform: translateY(-3px); box-shadow: var(--shadow-md); }

.box-head {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 16px;
}

.box-icon {
  width: 38px;
  height: 38px;
  border-radius: 11px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.box-icon svg { width: 20px; height: 20px; }
.bento-box.temp .box-icon { background: #fee2e2; color: #f43f5e; }
.bento-box.humi .box-icon { background: #e0f2fe; color: #0ea5e9; }
.bento-box.light .box-icon { background: #fef3c7; color: #f59e0b; }

.box-label {
  font-size: 14px;
  font-weight: 600;
  color: var(--muted);
}

.box-main {
  display: flex;
  align-items: baseline;
}

.box-main .num {
  font-size: 42px;
  font-weight: 800;
  color: var(--ink);
  line-height: 1;
}

.box-main .unit {
  font-size: 18px;
  font-weight: 600;
  color: #94a3b8;
  margin-left: 6px;
}

/* 左右分栏 */
.grid-layout {
  display: grid;
  grid-template-columns: 540px 1fr;
  gap: 22px;
  align-items: start;
}

.tech-panel {
  background: #ffffff;
  border: 1px solid var(--line);
  border-radius: var(--radius);
  padding: 24px;
  margin-bottom: 22px;
  box-shadow: var(--shadow-sm);
}

.panel-header {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 15px;
  font-weight: 700;
  color: var(--ink);
  border-bottom: 1px solid var(--line);
  padding-bottom: 14px;
  margin-bottom: 18px;
}

.panel-icon {
  width: 30px;
  height: 30px;
  border-radius: 9px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  background: var(--brand-soft);
  color: var(--brand);
}
.panel-icon svg { width: 17px; height: 17px; }

.header-title {
  display: flex;
  align-items: center;
  gap: 10px;
}

/* 执行器开关 */
.actuator-grid {
  display: flex;
  flex-direction: column;
  gap: 14px;
  margin-bottom: 18px;
}

.actuator-item {
  background: #f8fafc;
  border: 1px solid var(--line);
  border-radius: 12px;
  padding: 16px 18px;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.actuator-meta {
  display: flex;
  flex-direction: column;
  gap: 5px;
}

.actuator-meta .name {
  font-size: 14px;
  font-weight: 700;
  color: var(--ink);
}

.state-badge {
  font-size: 12px;
  font-weight: 600;
  width: max-content;
  padding: 2px 10px;
  border-radius: 999px;
}
.state-badge.on { color: #15803d; background: #dcfce7; }
.state-badge.off { color: #64748b; background: #f1f5f9; }

.binary-controls { display: flex; gap: 8px; }

.bin-btn {
  border: 1px solid #e2e8f0;
  background: #ffffff;
  border-radius: 9px;
  padding: 8px 18px;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.15s;
}
.bin-btn.green:hover { background: #dcfce7; border-color: #86efac; color: #15803d; }
.bin-btn.red:hover { background: #fee2e2; border-color: #fca5a5; color: #b91c1c; }

.topic-bus {
  background: #0f172a;
  border-radius: 12px;
  color: #ffffff;
  padding: 12px 16px;
  display: flex;
  align-items: center;
  gap: 10px;
}
.bus-lbl { font-size: 13px; font-weight: 500; color: #94a3b8; white-space: nowrap; }
.bus-input {
  flex: 1;
  background: transparent;
  border: none;
  color: #ffffff;
  font-family: ui-monospace, Menlo, Consolas, monospace;
  font-size: 13px;
}
.bus-input:focus { outline: none; }
.syntax-tag { font-size: 12px; background: var(--brand); color: #ffffff; padding: 2px 10px; border-radius: 999px; font-weight: 600; }

/* 表单输入 */
.matrix-form { display: flex; gap: 10px; }
.matrix-input, .matrix-select, .matrix-textarea {
  border: 1px solid #e2e8f0;
  border-radius: 10px;
  background: #f8fafc;
  padding: 10px 14px;
  font-size: 13px;
  font-family: inherit;
  color: var(--ink);
  transition: all 0.15s;
}
.matrix-input { flex: 1; }
.matrix-textarea { width: 100%; resize: vertical; }
.matrix-input:focus, .matrix-select:focus, .matrix-textarea:focus {
  outline: none;
  border-color: var(--brand);
  background: #ffffff;
  box-shadow: 0 0 0 3px rgba(99, 102, 241, 0.12);
}

.matrix-btn {
  background: linear-gradient(135deg, #6366f1, #8b5cf6);
  color: #ffffff;
  border: none;
  border-radius: 10px;
  padding: 0 22px;
  font-weight: 600;
  font-size: 13px;
  cursor: pointer;
  box-shadow: 0 6px 16px rgba(99, 102, 241, 0.22);
  transition: transform 0.15s;
}
.matrix-btn:hover:not(:disabled) { transform: translateY(-1px); }
.matrix-btn:disabled { opacity: 0.45; cursor: not-allowed; box-shadow: none; }
.matrix-btn.injection { background: linear-gradient(135deg, #0ea5e9, #6366f1); }

.matrix-pool { display: flex; flex-wrap: wrap; gap: 8px; margin-top: 14px; }
.matrix-tag {
  background: var(--brand-soft);
  color: var(--brand-dark);
  border-radius: 999px;
  padding: 4px 12px;
  font-size: 12px;
  font-weight: 600;
  display: flex;
  align-items: center;
  gap: 8px;
}
.tag-close { cursor: pointer; color: #a5b4fc; font-size: 14px; }
.tag-close:hover { color: var(--brand-dark); }

/* 右侧实时日志控制台 */
.console-panel {
  display: flex;
  flex-direction: column;
  height: 660px;
}

.console-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.clear-terminal-btn {
  background: transparent;
  border: none;
  font-size: 13px;
  font-weight: 600;
  color: #94a3b8;
  cursor: pointer;
  padding: 4px 10px;
  border-radius: 8px;
  transition: all 0.15s;
}
.clear-terminal-btn:hover { color: #ef4444; background: #fef2f2; }

.terminal-view {
  flex: 1;
  overflow-y: auto;
  font-size: 13px;
  padding: 4px;
  background: #0f172a;
  border-radius: 12px;
  font-family: ui-monospace, Menlo, Consolas, monospace;
}

.terminal-idle {
  color: #64748b;
  padding: 40px 0;
  text-align: center;
}

.terminal-row {
  padding: 10px 12px;
  border-bottom: 1px solid rgba(148, 163, 184, 0.12);
}

.row-top {
  display: flex;
  gap: 12px;
  margin-bottom: 4px;
}
.t-time { color: #64748b; font-size: 12px; }
.t-topic { color: #818cf8; font-weight: 600; }
.row-payload {
  color: #e2e8f0;
  word-break: break-all;
  padding-left: 10px;
}
</style>