import { createRouter, createWebHistory, RouteRecordRaw } from 'vue-router'
import { useUserStore } from '@/stores/user'

const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/login/LoginView.vue'),
    meta: { title: '登录' }
  },
  {
    path: '/',
    component: () => import('@/views/layout/MainLayout.vue'),
    redirect: '/home',
    children: [
      {
        path: 'home',
        name: 'Home',
        component: () => import('@/views/home/HomeView.vue'),
        meta: { title: '主页', icon: 'HomeFilled' }
      },
      {
        path: 'system/user',
        name: 'UserManage',
        component: () => import('@/views/system/UserManageView.vue'),
        meta: { title: '用户管理', permission: 'user:list' }
      },
      {
        path: 'system/role',
        name: 'RoleManage',
        component: () => import('@/views/system/RoleManageView.vue'),
        meta: { title: '权限管理', permission: 'role:list' }
      },
      {
        path: 'system/ai',
        name: 'AIConfig',
        component: () => import('@/views/system/AIConfigView.vue'),
        meta: { title: 'AI配置', permission: 'ai:list' }
      },
      {
        path: 'app/document',
        name: 'DocumentList',
        component: () => import('@/views/app/DocumentListView.vue'),
        meta: { title: '文档列表', permission: 'doc:list' }
      },
      {
        path: 'app/document/:docId/questions',
        name: 'QuestionConfirm',
        component: () => import('@/views/app/QuestionConfirmView.vue'),
        meta: { title: '题目确认', permission: 'question:confirm' }
      },
      {
        path: 'app/test-paper',
        name: 'TestPaperList',
        component: () => import('@/views/app/TestPaperListView.vue'),
        meta: { title: '试题列表', permission: 'paper:list' }
      },
      {
        path: 'app/test-paper/:paperId/quiz',
        name: 'QuizTake',
        component: () => import('@/views/app/QuizTakeView.vue'),
        meta: { title: '在线答题', permission: 'quiz:take' }
      },
      {
        path: 'app/test-paper/:paperId/quiz/result/:resultId',
        name: 'QuizResult',
        component: () => import('@/views/app/QuizTakeView.vue'),
        meta: { title: '答题成绩', permission: 'quiz:take' }
      },
      {
        path: 'app/quiz-results',
        name: 'QuizResultList',
        component: () => import('@/views/app/QuizResultListView.vue'),
        meta: { title: '答题记录', permission: 'quiz:take' }
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

const whiteList = ['/login']

router.beforeEach(async (to, _from, next) => {
  document.title = (to.meta.title as string) || 'AI 智能答题系统'
  const userStore = useUserStore()

  if (whiteList.includes(to.path)) {
    if (userStore.isLoggedIn) {
      next('/home')
    } else {
      next()
    }
    return
  }

  if (!userStore.isLoggedIn) {
    next(`/login?redirect=${to.path}`)
    return
  }

  if (!userStore.userId) {
    try {
      await userStore.fetchUserInfo()
    } catch {
      userStore.clearAuth()
      next(`/login?redirect=${to.path}`)
      return
    }
  }

  const permission = to.meta.permission as string | undefined
  if (permission && !userStore.hasPermission(permission)) {
    next('/home')
    return
  }

  next()
})

export default router
