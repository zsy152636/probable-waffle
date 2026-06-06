import request from './request'
import type { Result, PageResult, QuizPaper, QuizResultData, QuizResultItem, SubmitAnswers } from '@/types'

export function getQuizPaper(testPaperId: string) {
  return request.get<Result<QuizPaper>>(`/quiz/${testPaperId}/paper`)
}

export function submitQuiz(testPaperId: string, data: SubmitAnswers) {
  return request.post<Result<QuizResultData>>(`/quiz/${testPaperId}/submit`, data)
}

export function getQuizResults(params: { page: number; pageSize: number; keyword?: string }) {
  return request.get<Result<PageResult<QuizResultItem>>>('/quiz/results', { params })
}

export function getQuizResultById(resultId: string) {
  return request.get<Result<QuizResultData>>(`/quiz/results/${resultId}`)
}

export function deleteQuizResult(resultId: string) {
  return request.delete<Result<null>>(`/quiz/results/${resultId}`)
}
