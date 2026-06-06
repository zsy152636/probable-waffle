import request from './request'
import type { Result, LoginForm, LoginResult, AuthInfo } from '@/types'

export function login(data: LoginForm) {
  return request.post<Result<LoginResult>>('/auth/login', data)
}

export function logout() {
  return request.post<Result<null>>('/auth/logout')
}

export function getAuthInfo() {
  return request.get<Result<AuthInfo>>('/auth/info')
}
