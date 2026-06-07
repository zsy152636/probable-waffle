import request from './request'
import type { Result, PageResult, GenConfigItem } from '@/types'

export function listConfigs(params: { page?: number; pageSize?: number }) {
  return request.get<Result<PageResult<GenConfigItem>>>('/question-gen-configs', { params })
}

export function listActiveConfigs() {
  return request.get<Result<GenConfigItem[]>>('/question-gen-configs/active')
}

export function getConfig(id: string) {
  return request.get<Result<GenConfigItem>>(`/question-gen-configs/${id}`)
}

export function saveConfig(data: GenConfigItem) {
  return request.post<Result<GenConfigItem>>('/question-gen-configs', data)
}

export function updateConfig(id: string, data: GenConfigItem) {
  return request.put<Result<null>>(`/question-gen-configs/${id}`, data)
}

export function deleteConfig(id: string) {
  return request.delete<Result<null>>(`/question-gen-configs/${id}`)
}
