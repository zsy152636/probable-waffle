import request from './request'
import type { Result, PageResult, RoleInfo, RoleForm, MenuItem, AssignMenusParams } from '@/types'

export function getRoles(params: { page: number; pageSize: number }) {
  return request.get<Result<PageResult<RoleInfo>>>('/roles', { params })
}

export function getAllRoles() {
  return request.get<Result<RoleInfo[]>>('/roles/all')
}

export function addRole(data: RoleForm) {
  return request.post<Result<null>>('/roles', data)
}

export function updateRole(id: string, data: RoleForm) {
  return request.put<Result<null>>(`/roles/${id}`, data)
}

export function deleteRole(id: string) {
  return request.delete<Result<null>>(`/roles/${id}`)
}

export function getRoleMenus(id: string) {
  return request.get<Result<number[]>>(`/roles/${id}/menus`)
}

export function assignRoleMenus(id: string, data: AssignMenusParams) {
  return request.put<Result<null>>(`/roles/${id}/menus`, data)
}
