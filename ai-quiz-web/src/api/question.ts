import request from './request'
import type { Result, PageResult, QuestionItem, GenerateConfig } from '@/types'

export function generateQuestions(docId: string, config: GenerateConfig) {
  return request.post<Result<QuestionItem[]>>(`/documents/${docId}/generate-questions`, config, {
    timeout: 300000
  })
}

export function getDocumentQuestions(docId: string, params: { status?: number; questionType?: string; page?: number; pageSize?: number }) {
  return request.get<Result<PageResult<QuestionItem>>>(`/documents/${docId}/questions`, { params })
}

export function updateQuestion(id: string, data: Partial<QuestionItem>) {
  return request.put<Result<null>>(`/questions/${id}`, data)
}

export function deleteQuestion(id: string) {
  return request.delete<Result<null>>(`/questions/${id}`)
}

export function batchConfirmQuestions(ids: string[]) {
  return request.post<Result<{ paperId: string }>>('/questions/batch-confirm', { ids })
}

export function batchDeleteQuestions(ids: string[]) {
  return request.post<Result<null>>('/questions/batch-delete', { ids })
}

export function cancelGenerateQuestions(docId: string) {
  return request.delete<Result<null>>(`/documents/${docId}/questions`)
}
