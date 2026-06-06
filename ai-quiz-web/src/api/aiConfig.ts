import request from './request'
import type { Result, PageResult, AIConfigItem, AIConfigForm } from '@/types'

export function getAIConfigs(params: { page: number; pageSize: number }) {
  return request.get<Result<PageResult<AIConfigItem>>>('/ai-configs', { params })
}

export function getAIConfigById(id: string) {
  return request.get<Result<AIConfigItem>>(`/ai-configs/${id}`)
}

export function addAIConfig(data: AIConfigForm) {
  return request.post<Result<null>>('/ai-configs', data)
}

export function updateAIConfig(id: string, data: AIConfigForm) {
  return request.put<Result<null>>(`/ai-configs/${id}`, data)
}

export function deleteAIConfig(id: string) {
  return request.delete<Result<null>>(`/ai-configs/${id}`)
}

export function testAIConnection(id: string) {
  return request.post<Result<string>>(`/ai-configs/${id}/test`)
}

export function activateAIConfig(id: string) {
  return request.put<Result<null>>(`/ai-configs/${id}/activate`)
}

export function deactivateAIConfig(id: string) {
  return request.put<Result<null>>(`/ai-configs/${id}/deactivate`)
}
