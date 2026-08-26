import { defineStore } from 'pinia'
import { ref } from 'vue'
import { login, getUserInfo } from '@/api/request'

export interface UserInfo {
  id: number
  username: string
  nickname: string
  role: string
  avatarUrl: string
}

export const useUserStore = defineStore('user', () => {
  const token = ref(localStorage.getItem('token') || '')
  const userInfo = ref<UserInfo | null>(null)

  async function loginAction(username: string, password: string) {
    const data = await login(username, password)
    token.value = data.token
    localStorage.setItem('token', data.token)
    return data
  }

  async function fetchUserInfo() {
    const data = await getUserInfo()
    userInfo.value = data
    return data
  }

  function logout() {
    token.value = ''
    userInfo.value = null
    localStorage.removeItem('token')
  }

  return { token, userInfo, loginAction, fetchUserInfo, logout }
})
