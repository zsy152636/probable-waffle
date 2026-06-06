import { useUserStore } from '@/stores/user'

export function hasPerm(permission: string): boolean {
  const userStore = useUserStore()
  return userStore.hasPermission(permission)
}

export function formatFileSize(bytes: number): string {
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
  return (bytes / 1024 / 1024).toFixed(1) + ' MB'
}

export function formatSeconds(seconds: number): string {
  const min = Math.floor(seconds / 60)
  const sec = seconds % 60
  return `${min.toString().padStart(2, '0')}:${sec.toString().padStart(2, '0')}`
}

export function getDifficultyText(difficulty: number): string {
  const map: Record<number, string> = { 1: '简单', 2: '中等', 3: '困难' }
  return map[difficulty] || '未知'
}

export function getDifficultyStars(difficulty: number): string {
  return '★'.repeat(difficulty) + '☆'.repeat(3 - difficulty)
}

export function getQuestionTypeLabel(type: string): string {
  const map: Record<string, string> = {
    SINGLE: '单选题',
    MULTI: '多选题',
    JUDGE: '判断题',
    FILL: '填空题',
    SHORT: '简答题'
  }
  return map[type] || type
}

export function getDocTypeLabel(type: string): string {
  const map: Record<string, string> = {
    TXT: 'TXT',
    WORD: 'Word',
    PDF: 'PDF'
  }
  return map[type] || type
}
