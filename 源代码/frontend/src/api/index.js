// ============================================================================
//  index.js —— axios 基础配置（所有接口请求的"总开关"）
// ----------------------------------------------------------------------------
//  统一做两件事：① 每个请求自动带上登录令牌  ② 统一处理返回和错误
//  devices.js / auth.js 里的接口都基于这个 api 实例。
// ============================================================================
import axios from 'axios'
import { ElMessage } from 'element-plus'   // Element Plus的消息弹窗组件，用于提示错误

// 创建axios实例。baseURL='/api' 表示所有请求自动加上/api前缀（再由Vite代理转给后端8080）
const api = axios.create({
  baseURL: '/api',
  timeout: 15000,        // 请求超时15秒
})

// 请求拦截器：每次发请求前自动执行。从本地取出JWT令牌，放进请求头Authorization
// 后端靠这个令牌识别"你是谁、有没有登录"
api.interceptors.request.use(config => {
  const token = localStorage.getItem('token')
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

// 响应拦截器：每次收到后端返回后自动执行
api.interceptors.response.use(
  res => res.data,        // 成功：直接返回后端data部分（省去每次写res.data）
  err => {                // 失败：统一弹窗提示错误
    const msg = err.response?.data?.message || err.message || '请求失败'
    ElMessage.error(msg)
    // 401=未授权（令牌过期/无效）：清除登录信息并跳回登录页
    if (err.response?.status === 401) {
      localStorage.removeItem('token')
      localStorage.removeItem('user')
      window.location.href = '/login'
    }
    return Promise.reject(err)
  }
)

export default api
