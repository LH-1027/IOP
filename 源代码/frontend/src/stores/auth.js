import { defineStore } from 'pinia'
import { ref } from 'vue'
import { login as loginApi, register as registerApi } from '../api/auth'

export const useAuthStore = defineStore('auth', () => {
  const token = ref(localStorage.getItem('token') || '')
  const user = ref(JSON.parse(localStorage.getItem('user') || 'null'))

  async function login(data) {
    const res = await loginApi(data)
    if (res.code === 200) {
      token.value = res.data.token
      user.value = { username: res.data.username, userId: res.data.userId }
      localStorage.setItem('token', res.data.token)
      localStorage.setItem('user', JSON.stringify(user.value))
    }
    return res
  }

  async function register(data) {
    const res = await registerApi(data)
    if (res.code === 200) {
      token.value = res.data.token
      user.value = { username: res.data.username, userId: res.data.userId }
      localStorage.setItem('token', res.data.token)
      localStorage.setItem('user', JSON.stringify(user.value))
    }
    return res
  }

  function logout() {
    token.value = ''
    user.value = null
    localStorage.removeItem('token')
    localStorage.removeItem('user')
  }

  return { token, user, login, register, logout }
})
