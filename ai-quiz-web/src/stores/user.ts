import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type { AuthInfo, MenuItem } from '@/types'
import { getAuthInfo } from '@/api/auth'

export const useUserStore = defineStore('user', () => {
  const token = ref<string>(localStorage.getItem('token') || '')
  const userId = ref('')
  const username = ref('')
  const realName = ref('')
  const avatar = ref('')
  const roles = ref<string[]>([])
  const permissions = ref<string[]>([])
  const menus = ref<MenuItem[]>([])

  const isLoggedIn = computed(() => !!token.value)

  function setToken(val: string) {
    token.value = val
    localStorage.setItem('token', val)
  }

  function clearAuth() {
    token.value = ''
    userId.value = ''
    username.value = ''
    realName.value = ''
    avatar.value = ''
    roles.value = []
    permissions.value = []
    menus.value = []
    localStorage.removeItem('token')
    localStorage.removeItem('userInfo')
  }

  function hasPermission(perm: string): boolean {
    if (roles.value.includes('ADMIN')) return true
    return permissions.value.includes(perm)
  }

  async function fetchUserInfo(): Promise<AuthInfo> {
    const { data } = await getAuthInfo()
    const info = data.data
    userId.value = info.user.id
    username.value = info.user.username
    realName.value = info.user.realName
    avatar.value = info.user.avatar || ''
    roles.value = info.roles
    permissions.value = info.permissions
    menus.value = info.menus
    return info
  }

  return {
    token, userId, username, realName, avatar,
    roles, permissions, menus,
    isLoggedIn, setToken, clearAuth, hasPermission, fetchUserInfo
  }
})
