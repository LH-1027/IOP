import api from './index'

// 获取当前用户的设备列表  → GET /api/devices
export function getDevices() {
  return api.get('/devices')
}

// 获取某台设备的详情  → GET /api/devices/{id}
export function getDeviceDetail(id) {
  return api.get(`/devices/${id}`)
}

// 删除某台设备  → DELETE /api/devices/{id}
export function deleteDevice(id) {
  return api.delete(`/devices/${id}`)
}

// 添加设备  → POST /api/devices
// 因为可能要上传证书文件，所以用 multipart/form-data 格式（表单上传），参数是FormData对象
export function addDevice(formData) {
  return api.post('/devices', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

// ===== MQTT 操作（都对应后端的 /{id}/xxx 接口）=====

// 连接设备（让后端建立到该设备的MQTT连接）  → POST /api/devices/{id}/connect
export function connectDevice(id) {
  return api.post(`/devices/${id}/connect`)
}

// 断开设备连接  → POST /api/devices/{id}/disconnect
export function disconnectDevice(id) {
  return api.post(`/devices/${id}/disconnect`)
}

// 订阅主题  → POST /api/devices/{id}/subscribe   data形如 { topic, qos }
export function subscribeTopic(id, data) {
  return api.post(`/devices/${id}/subscribe`, data)
}

// 取消订阅  → POST /api/devices/{id}/unsubscribe   data形如 { topic }
export function unsubscribeTopic(id, data) {
  return api.post(`/devices/${id}/unsubscribe`, data)
}

// 发布消息（控制设备/手动发消息都走这里）  → POST /api/devices/{id}/publish   data形如 { topic, payload, qos }
export function publishMessage(id, data) {
  return api.post(`/devices/${id}/publish`, data)
}

// 获取该设备当前的订阅列表  → GET /api/devices/{id}/subscriptions
export function getSubscriptions(id) {
  return api.get(`/devices/${id}/subscriptions`)
}

// 获取该设备的历史消息  → GET /api/devices/{id}/messages
export function getMessages(id) {
  return api.get(`/devices/${id}/messages`)
}
