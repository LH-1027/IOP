<template>
  <div class="dashboard">
    <header class="header">
      <div class="header-left">
        <span class="system-tag">设备总览</span>
        <h1>设备控制台</h1>
        <div class="user-status">当前用户：<span class="badge-txt">{{ authStore.user?.username }}</span></div>
      </div>
      <div class="header-right">
        <button class="tech-btn primary" @click="showAddDialog = true">+ 添加设备</button>
        <button class="tech-btn text-danger" @click="handleLogout">退出登录</button>
      </div>
    </header>

    <div class="content">
      <div v-if="devices.length === 0" class="empty-holder">
        <div class="terminal-prompt">> 暂无设备</div>
        <p>请点击右上角"添加设备"以接入新的 MQTT 设备。</p>
      </div>

      <div class="device-grid">
        <div v-for="device in devices" :key="device.id" :class="['bento-card', { 'online-edge': device.connected }]" @click="goDetail(device.id)">
          <div class="card-meta">
            <span class="node-id">设备ID #{{ device.id }}</span>
            <span :class="['status-dot', device.connected ? 'active' : 'inactive']">
              {{ device.connected ? '在线' : '离线' }}
            </span>
          </div>
          <h2 class="device-name">{{ device.name }}</h2>

          <div class="device-specs">
            <div class="spec-line">
              <span class="lbl">服务器：</span>
              <span class="val">{{ device.brokerUrl }}:{{ device.brokerPort }}</span>
            </div>
            <div class="spec-line" v-if="device.useSsl">
              <span class="lbl">协议：</span>
              <span class="val security-text">MQTT / SSL-TLS 加密</span>
            </div>
          </div>

          <div class="card-action">
            <button class="inline-del-btn" @click.stop="handleDelete(device.id)">删除设备</button>
          </div>
        </div>
      </div>
    </div>

    <el-dialog v-model="showAddDialog" title="[ 添加 MQTT 设备 ]" width="560px" class="tech-dialog">
      <el-form :model="addForm" label-position="top" class="dialog-form">
        <el-form-item label="设备名称" required>
          <el-input v-model="addForm.name" placeholder="如：客厅 ESP32" />
        </el-form-item>
        <el-form-item label="服务器地址" required>
          <el-input v-model="addForm.brokerUrl" placeholder="如：broker.emqx.io" />
        </el-form-item>

        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="端口">
              <el-input-number v-model="addForm.brokerPort" :min="1" :max="65535" style="width:100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="SSL 加密">
              <div class="switch-block">
                <span>启用 SSL/TLS</span>
                <el-switch v-model="addForm.useSsl" active-color="#111827" />
              </div>
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="MQTT 用户名">
              <el-input v-model="addForm.mqttUsername" placeholder="选填" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="MQTT 密码">
              <el-input v-model="addForm.mqttPassword" type="password" placeholder="选填" show-password />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="命令格式">
          <el-select v-model="addForm.commandFormat" style="width:100%">
            <el-option value="JSON" label="JSON 格式（键值对）" />
            <el-option value="led_on" label="拼接小写（target_value）" />
            <el-option value="on_off" label="纯小写（on/off）" />
            <el-option value="ON_OFF" label="纯大写（ON/OFF）" />
            <el-option value="LED_ON" label="拼接大写（TARGET_VALUE）" />
          </el-select>
        </el-form-item>

        <el-form-item label="CA 证书（.crt / .pem）">
          <el-upload ref="uploadRef" :auto-upload="false" :limit="1" accept=".crt,.pem"
            :on-change="(file) => certFile = file.raw" class="tech-upload">
            <button type="button" class="file-trigger">上传证书文件</button>
          </el-upload>
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-actions">
          <button class="flat-btn" @click="showAddDialog = false">取消</button>
          <button class="flat-btn primary" @click="handleAddDevice">确认添加</button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
// ============================================================================
//  Dashboard.vue —— 设备列表页（控制台首页）
// ----------------------------------------------------------------------------
//  显示当前用户的所有设备卡片，支持：添加设备、删除设备、点卡片进详情页。
//  每5秒自动刷新一次列表，保持在线状态最新。
// ============================================================================
import { ref, reactive, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'                 // 登录状态管理
import { getDevices, addDevice, deleteDevice } from '../api/devices'
import { ElMessage, ElMessageBox } from 'element-plus'        // 提示弹窗/确认框

const router = useRouter()             // 路由对象，用于跳转页面
const authStore = useAuthStore()       // 当前登录用户信息
const devices = ref([])                // 设备列表
const showAddDialog = ref(false)       // 是否显示"添加设备"弹窗
const certFile = ref(null)             // 用户选择的证书文件
let timer = null                       // 定时刷新的计时器

// 添加设备表单的数据，和弹窗里的输入框双向绑定
const addForm = reactive({
  name: '', brokerUrl: '', brokerPort: 8883,
  mqttUsername: '', mqttPassword: '', useSsl: true, commandFormat: 'JSON'
})

// 获取设备列表并刷新页面
async function fetchDevices() {
  try {
    const res = await getDevices()
    if (res.code === 200) devices.value = res.data
  } catch (e) { /* 错误已由响应拦截器统一提示 */ }
}

// 提交"添加设备"表单
async function handleAddDevice() {
  // 必填校验
  if (!addForm.name.trim() || !addForm.brokerUrl.trim()) {
    ElMessage.warning('请填写设备名称和Broker地址')
    return
  }
  try {
    // 因为要上传文件，用FormData把字段一个个装进去（multipart表单）
    const fd = new FormData()
    fd.append('name', addForm.name.trim())
    fd.append('brokerUrl', addForm.brokerUrl.trim())
    fd.append('brokerPort', addForm.brokerPort)
    fd.append('mqttUsername', addForm.mqttUsername)
    fd.append('mqttPassword', addForm.mqttPassword)
    fd.append('useSsl', addForm.useSsl)
    fd.append('commandFormat', addForm.commandFormat)
    if (certFile.value) fd.append('certFile', certFile.value)   // 有证书才加

    await addDevice(fd)                          // 调接口提交
    ElMessage.success('设备添加成功')
    showAddDialog.value = false                  // 关弹窗
    addForm.name = ''; addForm.brokerUrl = ''; certFile.value = null  // 清空表单
    fetchDevices()                               // 刷新列表
  } catch (e) { /* handled */ }
}

// 删除设备（先弹确认框，确认后才删）
async function handleDelete(id) {
  try {
    await ElMessageBox.confirm('确定删除该设备？', '确认', { type: 'warning' })
    await deleteDevice(id)
    ElMessage.success('设备已删除')
    fetchDevices()
  } catch (e) { /* 用户取消或出错 */ }
}

// 点设备卡片：跳转到该设备的详情页
function goDetail(id) { router.push(`/device/${id}`) }
// 退出登录：清除登录状态并回登录页
function handleLogout() { authStore.logout(); router.push('/login') }

// 页面加载后：先拉一次列表，再每5秒自动刷新一次
onMounted(() => {
  fetchDevices()
  timer = setInterval(fetchDevices, 5000)
})

// 页面关闭时：停掉定时器，避免内存泄漏
onUnmounted(() => { if (timer) clearInterval(timer) })
</script>

<style scoped>
.dashboard { min-height: 100vh; }

.header {
  background: rgba(255, 255, 255, 0.7);
  backdrop-filter: blur(12px);
  padding: 20px 40px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-bottom: 1px solid var(--line);
  position: sticky;
  top: 0;
  z-index: 10;
}

.system-tag {
  font-size: 12px;
  font-weight: 600;
  color: var(--brand);
  letter-spacing: 0.5px;
}

.header h1 {
  font-size: 24px;
  font-weight: 800;
  color: var(--ink);
  margin: 4px 0 6px 0;
}

.user-status {
  font-size: 13px;
  color: var(--muted);
}

.badge-txt {
  background: var(--brand-soft);
  color: var(--brand-dark);
  padding: 2px 10px;
  border-radius: 999px;
  font-weight: 600;
}

.tech-btn {
  border: 1px solid #e2e8f0;
  background: #ffffff;
  padding: 9px 18px;
  font-size: 13px;
  font-weight: 600;
  border-radius: 10px;
  cursor: pointer;
  margin-left: 10px;
  transition: all 0.18s;
}
.tech-btn:hover { border-color: #cbd5e1; }

.tech-btn.primary {
  background: linear-gradient(135deg, #6366f1, #8b5cf6);
  border: none;
  color: #ffffff;
  box-shadow: 0 6px 16px rgba(99, 102, 241, 0.25);
}
.tech-btn.primary:hover {
  transform: translateY(-1px);
  box-shadow: 0 10px 22px rgba(99, 102, 241, 0.32);
}

.tech-btn.text-danger {
  color: #ef4444;
}
.tech-btn.text-danger:hover {
  border-color: #fecaca;
  background: #fef2f2;
}

.content {
  max-width: 1280px;
  margin: 0 auto;
  padding: 40px;
}

.empty-holder {
  border: 1.5px dashed #cbd5e1;
  border-radius: var(--radius);
  padding: 80px 40px;
  text-align: center;
  background: rgba(255, 255, 255, 0.6);
}

.terminal-prompt {
  font-size: 18px;
  font-weight: 700;
  color: var(--ink);
  margin-bottom: 8px;
}

.empty-holder p {
  font-size: 14px;
  color: var(--muted);
}

/* 卡片网格 */
.device-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 22px;
}

.bento-card {
  background: #ffffff;
  border: 1px solid var(--line);
  border-radius: var(--radius);
  padding: 24px;
  cursor: pointer;
  display: flex;
  flex-direction: column;
  box-shadow: var(--shadow-sm);
  transition: transform 0.22s cubic-bezier(0.16, 1, 0.3, 1), box-shadow 0.22s;
  position: relative;
  overflow: hidden;
}

.bento-card::before {
  content: '';
  position: absolute;
  top: 0; left: 0; right: 0;
  height: 3px;
  background: linear-gradient(90deg, #6366f1, #8b5cf6);
  opacity: 0;
  transition: opacity 0.2s;
}

.bento-card:hover {
  transform: translateY(-4px);
  box-shadow: var(--shadow-md);
}
.bento-card:hover::before { opacity: 1; }

.bento-card.online-edge::before { opacity: 1; background: linear-gradient(90deg, #10b981, #34d399); }

.card-meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 12px;
  font-weight: 600;
  color: #94a3b8;
  margin-bottom: 14px;
}

.status-dot {
  padding: 3px 10px;
  border-radius: 999px;
  font-weight: 600;
  font-size: 12px;
}
.status-dot.active { background: #dcfce7; color: #15803d; }
.status-dot.inactive { background: #f1f5f9; color: #64748b; }

.device-name {
  font-size: 19px;
  font-weight: 800;
  color: var(--ink);
  margin-bottom: 18px;
}

.device-specs {
  flex: 1;
  background: #f8fafc;
  padding: 14px 16px;
  border-radius: 12px;
  font-size: 13px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.spec-line {
  display: flex;
}
.spec-line .lbl { color: #94a3b8; width: 72px; font-weight: 500; }
.spec-line .val { color: #334155; word-break: break-all; }
.security-text { color: var(--brand) !important; font-weight: 600; }

.card-action {
  margin-top: 18px;
  border-top: 1px solid var(--line);
  padding-top: 14px;
  display: flex;
  justify-content: flex-end;
}

.inline-del-btn {
  background: transparent;
  border: none;
  font-size: 13px;
  font-weight: 600;
  color: #cbd5e1;
  cursor: pointer;
  padding: 4px 8px;
  border-radius: 8px;
  transition: all 0.15s;
}
.bento-card:hover .inline-del-btn {
  color: #ef4444;
}
.inline-del-btn:hover {
  background: #fef2f2;
}

/* 添加设备弹窗 */
.tech-dialog :deep(.el-dialog) {
  border-radius: 20px;
  overflow: hidden;
  box-shadow: var(--shadow-md);
}
.tech-dialog :deep(.el-dialog__header) {
  margin-right: 0;
  padding-bottom: 16px;
  border-bottom: 1px solid var(--line);
}
.tech-dialog :deep(.el-dialog__title) {
  font-weight: 800;
  color: var(--ink);
}
.dialog-form :deep(.el-form-item__label) {
  font-size: 13px;
  font-weight: 600;
  color: #334155;
}
.dialog-form :deep(.el-input__wrapper),
.dialog-form :deep(.el-select__wrapper),
.dialog-form :deep(.el-input-number) {
  border-radius: 10px;
  box-shadow: none;
}
.dialog-form :deep(.el-input__wrapper) {
  border: 1px solid #e2e8f0;
}
.dialog-form :deep(.el-input__wrapper.is-focus) {
  border-color: var(--brand) !important;
  box-shadow: 0 0 0 3px rgba(99, 102, 241, 0.12) !important;
}
.switch-block {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
  background: #f8fafc;
  padding: 5px 14px;
  border-radius: 10px;
  border: 1px solid #e2e8f0;
  font-size: 13px;
  color: #475569;
}
.tech-upload .file-trigger {
  background: var(--brand-soft);
  color: var(--brand-dark);
  border: 1px dashed #c7d2fe;
  border-radius: 10px;
  padding: 8px 16px;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.15s;
}
.tech-upload .file-trigger:hover { background: #e0e7ff; }
.dialog-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}
.flat-btn {
  background: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 10px;
  padding: 9px 22px;
  font-size: 13px;
  font-weight: 600;
  color: #475569;
  cursor: pointer;
  transition: all 0.15s;
}
.flat-btn:hover { border-color: #cbd5e1; }
.flat-btn.primary {
  background: linear-gradient(135deg, #6366f1, #8b5cf6);
  border: none;
  color: #ffffff;
  box-shadow: 0 6px 16px rgba(99, 102, 241, 0.25);
}
.flat-btn.primary:hover { transform: translateY(-1px); }
</style>