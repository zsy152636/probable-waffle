import request from './request'
import type { Result, PageResult, TestPaperItem } from '@/types'

export function getTestPapers(params: { page: number; pageSize: number; keyword?: string }) {
  return request.get<Result<PageResult<TestPaperItem>>>('/test-papers', { params })
}

export function getTestPaperById(id: string) {
  return request.get<Result<TestPaperItem>>(`/test-papers/${id}`)
}

export function deleteTestPaper(id: string) {
  return request.delete<Result<null>>(`/test-papers/${id}`)
}
