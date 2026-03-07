import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import AppLayout from '@/components/AppLayout.vue'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      name: 'home',
      component: () => import('@/views/HomeView.vue'),
      meta: { requiresAuth: false }
    },
    {
      path: '/login',
      name: 'login',
      component: () => import('@/views/LoginView.vue'),
      meta: { requiresAuth: false }
    },
    {
      path: '/register',
      name: 'register',
      component: () => import('@/views/RegisterView.vue'),
      meta: { requiresAuth: false }
    },
    {
      path: '/app',
      component: AppLayout,
      meta: { requiresAuth: true },
      children: [
        {
          path: '',
          redirect: '/app/dashboard'
        },
        {
          path: 'dashboard',
          name: 'dashboard',
          component: () => import('@/views/DashboardView.vue')
        },
        {
          path: 'devices',
          name: 'devices',
          component: () => import('@/views/DevicesView.vue')
        },
        {
          path: 'devices/:id',
          name: 'device-detail',
          component: () => import('@/views/DeviceDetailView.vue')
        },
        {
          path: 'history',
          name: 'history',
          component: () => import('@/views/HistoryView.vue')
        },
        {
          path: 'live',
          name: 'live',
          component: () => import('@/views/LiveView.vue')
        },
        {
          path: 'ai',
          name: 'ai',
          component: () => import('@/views/AIReportView.vue')
        },
        {
          path: 'aggregate',
          name: 'aggregate',
          component: () => import('@/views/DeviceAggregateView.vue')
        },
        {
          path: 'groups',
          name: 'groups',
          component: () => import('@/views/DeviceGroupView.vue')
        },
        {
          path: 'users',
          name: 'users',
          component: () => import('@/views/UserManagementView.vue')
        },
        {
          path: 'alerts',
          name: 'alerts',
          component: () => import('@/views/AlertManagementView.vue')
        },
        {
          path: 'tasks',
          name: 'tasks',
          component: () => import('@/views/TaskManagementView.vue')
        },
        {
          path: 'api-test',
          name: 'api-test',
          component: () => import('@/views/APITestView.vue')
        },
        {
          path: 'system-config',
          name: 'system-config',
          component: () => import('@/views/SystemConfigView.vue')
        },
        {
          path: 'logs',
          name: 'logs',
          component: () => import('@/views/LogManagementView.vue')
        },
        {
          path: 'backup',
          name: 'backup',
          component: () => import('@/views/BackupManagementView.vue')
        },
        {
          path: 'notifications',
          name: 'notifications',
          component: () => import('@/views/NotificationView.vue')
        },
        {
          path: 'monitor',
          name: 'monitor',
          component: () => import('@/views/SystemMonitorView.vue')
        },
        {
          path: 'billing',
          name: 'billing',
          component: () => import('@/views/BillingView.vue')
        },
        {
          path: 'dorm',
          name: 'dorm',
          component: () => import('@/views/DormManagementView.vue')
        },
        {
          path: 'students',
          name: 'students',
          component: () => import('@/views/StudentManagementView.vue')
        },
        {
          path: 'profile',
          name: 'profile',
          component: () => import('@/views/ProfileView.vue')
        },
        {
          path: 'notification-settings',
          name: 'notification-settings',
          component: () => import('@/views/NotificationSettingsView.vue')
        },
        {
          path: 'agent',
          name: 'agent',
          component: () => import('@/views/AgentView.vue')
        },
        {
          path: 'rbac',
          name: 'rbac',
          component: () => import('@/views/RbacView.vue')
        },
        {
          path: 'power-control',
          name: 'power-control',
          component: () => import('@/views/PowerControlView.vue')
        },
        {
          path: 'ip-control',
          name: 'ip-control',
          component: () => import('@/views/IpAccessControlView.vue')
        },
        {
          path: 'login-logs',
          name: 'login-logs',
          component: () => import('@/views/LoginLogView.vue')
        },
        {
          path: 'audit-logs',
          name: 'audit-logs',
          component: () => import('@/views/AuditLogView.vue')
        },
        {
          path: 'message-templates',
          name: 'message-templates',
          component: () => import('@/views/MessageTemplateView.vue')
        },
        {
          path: 'data-dict',
          name: 'data-dict',
          component: () => import('@/views/DataDictView.vue')
        },
        {
          path: 'collections',
          name: 'collections',
          component: () => import('@/views/CollectionView.vue')
        },
        {
          path: 'firmware',
          name: 'firmware',
          component: () => import('@/views/FirmwareView.vue')
        },
        {
          path: 'import',
          name: 'import',
          component: () => import('@/views/DataImportView.vue')
        },
        {
          path: 'telemetry',
          name: 'telemetry',
          component: () => import('@/views/TelemetryView.vue')
        },
        {
          path: 'ai-chat',
          name: 'ai-chat',
          component: () => import('@/views/AIChatView.vue')
        },
        {
          path: 'auto-saving',
          name: 'auto-saving',
          component: () => import('@/views/AutoSavingView.vue')
        },
        {
          path: 'command-history',
          name: 'command-history',
          component: () => import('@/views/CommandHistoryView.vue')
        },
        {
          path: 'mqtt-simulator',
          name: 'mqtt-simulator',
          component: () => import('@/views/MqttSimulatorView.vue')
        }
      ]
    }
  ]
})

/**
 * 路由守卫
 */
router.beforeEach((to, from, next) => {
  const authStore = useAuthStore()
  
  if (to.meta.requiresAuth && !authStore.isAuthenticated) {
    next('/login')
  } else if ((to.path === '/login' || to.path === '/register') && authStore.isAuthenticated) {
    next('/app/dashboard')
  } else {
    next()
  }
})

export default router
