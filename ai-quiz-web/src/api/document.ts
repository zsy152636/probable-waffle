import request from './request'
import type { Result, PageResult, DocumentItem } from '@/types'

export function getDocuments(params: { page: number; pageSize: number; keyword?: string; docType?: string }) {
  return request.get<Result<PageResult<DocumentItem>>>('/documents', { params })
}

export function getDocumentById(id: string) {
  return request.get<Result<DocumentItem>>(`/documents/${id}`)
}

export function uploadDocuments(formData: FormData) {
  return request.post<Result<DocumentItem[]>>('/documents/upload', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

export function updateDocument(id: string, data: { docName: string }) {
  return request.put<Result<null>>(`/documents/${id}`, data)
}

export function deleteDocument(id: string) {
  return request.delete<Result<null>>(`/documents/${id}`)
}
