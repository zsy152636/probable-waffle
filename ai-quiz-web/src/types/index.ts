// ========== 通用 ==========

export interface PageResult<T> {
  records: T[]
  total: number
  page: number
  pageSize: number
}

export interface Result<T> {
  code: number
  message: string
  data: T
}

// ========== 用户 & 认证 ==========

export interface UserInfo {
  id: string
  username: string
  realName: string
  email: string
  phone: string
  status: number
  createTime: string
}

export interface LoginForm {
  username: string
  password: string
}

export interface LoginResult {
  token: string
  expiresIn: number
}

export interface AuthInfo {
  user: {
    id: string
    username: string
    realName: string
    avatar: string | null
  }
  roles: string[]
  permissions: string[]
  menus: MenuItem[]
}

// ========== 菜单 ==========

export interface MenuItem {
  id: number
  parentId: number
  name: string
  path: string | null
  component: string | null
  icon: string | null
  children?: MenuItem[]
}

// ========== 角色 ==========

export interface RoleInfo {
  id: string
  roleName: string
  roleCode: string
  description: string
  createTime?: string
}

export interface RoleForm {
  roleName: string
  roleCode: string
  description: string
}

// ========== AI 配置 ==========

export interface AIConfigItem {
  id: string
  providerName: string
  apiUrl: string
  apiToken: string
  modelName: string
  maxTokens: number
  temperature: number
  isActive: number
  createTime?: string
}

export interface AIConfigForm {
  providerName: string
  apiUrl: string
  apiToken: string
  modelName: string
  maxTokens: number
  temperature: number
}

// ========== 文档 ==========

export interface DocumentItem {
  id: string
  docNo: string
  docName: string
  docType: string
  fileSize: number
  questionCount: number
  confirmedCount: number
  hasPaper: boolean
  paperId: string | null
  questionGenTime: string | null
  uploadUserName: string
  createTime: string
  updateTime: string
}

// ========== 题目生成配置 ==========

export interface GenerateConfig {
  totalCount: number
  includeFill: boolean
  typeRatios: {
    single: number
    multi: number
    judge: number
  }
  direction?: string
}

// ========== 试题 ==========

export interface TestPaperItem {
  id: string
  paperNo: string
  paperName: string
  documentId: string
  docNo: string
  totalCount: number
  singleCount: number
  multiCount: number
  judgeCount: number
  fillCount: number
  totalScore: number
  createTime: string
}

// ========== 题目 ==========

export type QuestionType = 'SINGLE' | 'MULTI' | 'JUDGE' | 'FILL' | 'SHORT'

export interface QuestionOption {
  id?: string
  label: string
  text: string
  isCorrect: boolean
  sort: number
}

export interface QuestionItem {
  id?: string
  documentId: string
  questionType: QuestionType
  questionTitle: string
  passage?: string
  options: QuestionOption[]
  analysis: string
  difficulty: number
  score: number
  sort: number
  status: number
}

// ========== 试卷 & 答题 ==========

export interface PaperQuestion {
  id: string
  questionType: QuestionType
  questionTitle: string
  passage?: string
  options: Pick<QuestionOption, 'label' | 'text'>[]
  score: number
  sort: number
}

export interface QuizPaper {
  paperId: string
  testPaperId: string
  documentId: string
  docName: string
  totalScore: number
  totalCount: number
  durationMinutes: number
  questions: PaperQuestion[]
}

export interface UserAnswer {
  questionId: string
  userAnswer: string[]
}

export interface SubmitAnswers {
  startTime: string
  answers: UserAnswer[]
}

export interface ResultOption {
  label: string
  text: string
  isCorrect: boolean
}

export interface ResultDetail {
  questionId: string
  questionTitle: string
  passage?: string
  questionType: QuestionType
  options: ResultOption[]
  userAnswer: string[]
  correctAnswer: string[]
  isCorrect: boolean
  score: number
  earnedScore: number
  analysis: string
}

export interface QuizResultData {
  resultId: string
  testPaperId: string
  documentId: string
  docName: string
  totalScore: number
  userScore: number
  correctCount: number
  totalCount: number
  durationSeconds: number
  details: ResultDetail[]
}

export interface QuizResultItem {
  id: string
  testPaperId: string
  documentId: string
  docName: string
  totalScore: number
  userScore: number
  correctCount: number
  totalCount: number
  durationSeconds: number
  submitTime: string
}

// ========== 数据统计 ==========

export interface ScoreDistItem {
  range: string
  label: string
  count: number
  color: string
}

export interface RecentQuizItem {
  submitTime: string
  userScore: number
  totalScore: number
  docName: string
}

export interface StatsOverview {
  docCount: number
  paperCount: number
  quizCount: number
  totalQuestions: number
  correctCount: number
  accuracyRate: number
  avgScore: number
  avgDuration: number
  scoreDistribution: ScoreDistItem[]
  recentQuizzes: RecentQuizItem[]
}

// ========== 分页查询参数 ==========

export interface PageParams {
  page: number
  pageSize: number
  keyword?: string
}

// ========== 用户表单 ==========

export interface UserForm {
  username: string
  password?: string
  realName: string
  email: string
  phone: string
  status: number
  roleIds?: string[]
}

export interface ChangeStatusParams {
  status: number
}

export interface AssignRolesParams {
  roleIds: string[]
}

export interface AssignMenusParams {
  menuIds: number[]
}
