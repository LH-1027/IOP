// ============================================================================
//  router/index.js —— 前端路由表（决定哪个URL显示哪个页面）
// ============================================================================
import { createRouter, createWebHistory } from 'vue-router'

// 路由表：每条规则 = URL路径 → 对应的页面组件
// component用 () => import(...) 是"懒加载"：访问到才加载该页面，首屏更快
// meta.requiresAuth: true 表示这个页面需要登录才能访问
const routes = [
  { path: '/login', name: 'Login', component: () => import('../views/Login.vue') },
  { path: '/register', name: 'Register', component: () => import('../views/Register.vue') },
  { path: '/', name: 'Dashboard', component: () => import('../views/Dashboard.vue'), meta: { requiresAuth: true } },          // 设备列表页
  { path: '/device/:id', name: 'DeviceDetail', component: () => import('../views/DeviceDetail.vue'), meta: { requiresAuth: true } },  // 设备详情页，:id是设备ID
]

const router = createRouter({
  history: createWebHistory(),   // 使用HTML5历史模式，URL里没有#号
  routes,
})

// 全局前置守卫：每次跳转页面前都会执行，用来做登录拦截
// to=要去的页面, from=来自的页面, next=放行函数
router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('token')        // 有令牌=已登录
  if (to.meta.requiresAuth && !token) {
    next('/login')                                   // 需要登录但没登录 → 踢去登录页
  } else if ((to.path === '/login' || to.path === '/register') && token) {
    next('/')                                        // 已登录还想去登录/注册页 → 直接回首页
  } else {
    next()                                           // 其它情况正常放行
  }
})

export default router
