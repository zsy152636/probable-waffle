import request from './request'
import type { Result, StatsOverview } from '@/types'

export function getStatsOverview() {
  return request.get<Result<StatsOverview>>('/stats/overview')
}
