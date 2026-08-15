import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import { apiRequest, type AuthResult, type User } from './api'

const TOKEN_KEY = 'aurora.access-token'

export const useSessionStore = defineStore('session', () => {
  const accessToken = ref(readStoredToken())
  const user = ref<User | null>(null)
  const authenticated = computed(() => Boolean(accessToken.value && user.value))

  async function login(email: string, password: string) {
    const result = await apiRequest<AuthResult>('/auth/login', {
      method: 'POST',
      body: JSON.stringify({ email, password }),
    })
    applyAuthResult(result)
  }

  async function register(email: string, password: string, displayName: string) {
    const result = await apiRequest<AuthResult>('/auth/register', {
      method: 'POST',
      body: JSON.stringify({ email, password, displayName }),
    })
    applyAuthResult(result)
  }

  async function restore() {
    if (!accessToken.value) return
    try {
      user.value = await apiRequest<User>('/me', {
        headers: { Authorization: `Bearer ${accessToken.value}` },
      })
    } catch {
      logout()
    }
  }

  function logout() {
    accessToken.value = null
    user.value = null
    try {
      sessionStorage.removeItem(TOKEN_KEY)
    } catch {
      // The in-memory session still logs out when browser storage is unavailable.
    }
  }

  function applyAuthResult(result: AuthResult) {
    accessToken.value = result.accessToken
    user.value = result.user
    try {
      sessionStorage.setItem(TOKEN_KEY, result.accessToken)
    } catch {
      // Continue with an in-memory session in privacy-restricted browsers.
    }
  }

  return { accessToken, user, authenticated, login, register, restore, logout }
})

function readStoredToken(): string | null {
  try {
    return sessionStorage.getItem(TOKEN_KEY)
  } catch {
    return null
  }
}
