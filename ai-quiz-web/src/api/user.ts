import request from './request'
import type { Result, PageResult, UserInfo, UserForm, ChangeStatusParams, AssignRolesParams } from '@/types'

export function getUsers(params: { page: number; pageSize: number; keyword?: string }) {
  return request.get<Result<PageResult<UserInfo>>>('/users', { params })
}

export function getUserById(id: string) {
  return request.get<Result<UserInfo>>(`/users/${id}`)
}

export function addUser(data: UserForm) {
  return request.post<Result<null>>('/users', data)
}

export function updateUser(id: string, data: UserForm) {
  return request.put<Result<null>>(`/users/${id}`, data)
}

export function deleteUser(id: string) {
  return request.delete<Result<null>>(`/users/${id}`)
}

export function changeUserStatus(id: string, data: ChangeStatusParams) {
  return request.put<Result<null>>(`/users/${id}/status`, data)
}

export function assignUserRoles(id: string, data: AssignRolesParams) {
  return request.put<Result<null>>(`/users/${id}/roles`, data)
}
