<template>
  <a-layout class="app-layout">
    <a-layout-sider
      v-model:collapsed="collapsed"
      :trigger="null"
      collapsible
      class="cyber-sidebar"
      width="260"
    >
      <div class="sidebar-bg">
        <div class="cyber-grid"></div>
        <div class="floating-particles">
          <div v-for="i in 15" :key="i" class="particle" :style="getParticleStyle(i)"></div>
        </div>
        <div class="data-streams">
          <div class="stream stream-1"></div>
          <div class="stream stream-2"></div>
          <div class="stream stream-3"></div>
        </div>
      </div>

      <div class="logo">
        <div class="logo-hologram">
          <div class="hologram-rings">
            <div class="ring ring-1"></div>
            <div class="ring ring-2"></div>
            <div class="ring ring-3"></div>
          </div>
          <ThunderboltOutlined class="logo-icon" />
        </div>
        <div v-if="!collapsed" class="logo-text">
          <span class="logo-title">DORM POWER</span>
          <span class="logo-subtitle">SMART ENERGY SYSTEM</span>
        </div>
      </div>

      <!-- 搜索框 -->
      <div class="menu-search">
        <a-input
          v-model:value="searchValue"
          placeholder="搜索功能..."
          size="small"
          prefix="🔍"
          @input="handleSearch"
        />
      </div>

      <a-menu
        v-model:selectedKeys="selectedKeys"
        v-model:openKeys="openKeys"
        mode="inline"
        :theme="'dark'"
        class="cyber-menu"
        @select="handleMenuSelect"
      >
        <!-- 常用功能 -->
        <a-menu-item key="/app/dashboard">
          <template #icon>
            <DashboardOutlined />
          </template>
          <span>仪表盘</span>
          <div class="menu-glitch"></div>
        </a-menu-item>

        <a-menu-item key="/app/devices">
          <template #icon>
            <DesktopOutlined />
          </template>
          <span>设备列表</span>
          <div class="menu-glitch"></div>
        </a-menu-item>

        <a-menu-item key="/app/live">
          <template #icon>
            <LineChartOutlined />
          </template>
          <span>实时监控</span>
          <div class="menu-glitch"></div>
        </a-menu-item>

        <a-menu-item key="/app/alerts">
          <template #icon>
            <AlertOutlined />
          </template>
          <span>告警管理</span>
          <div class="menu-glitch"></div>
        </a-menu-item>

        <!-- 设备管理 -->
        <a-sub-menu key="device-management">
          <template #icon>
            <PartitionOutlined />
          </template>
          <template #title>设备管理</template>
          <a-menu-item key="/app/groups">设备分组</a-menu-item>
          <a-menu-item key="/app/aggregate">聚合视图</a-menu-item>
          <a-menu-item key="/app/command-history">命令历史</a-menu-item>
        </a-sub-menu>

        <!-- 数据监控 -->
        <a-sub-menu key="data-monitoring">
          <template #icon>
            <HistoryOutlined />
          </template>
          <template #title>数据监控</template>
          <a-menu-item key="/app/history">历史数据</a-menu-item>
          <a-menu-item key="/app/ai">AI 报告</a-menu-item>
        </a-sub-menu>

        <!-- 宿舍管理 -->
        <a-sub-menu key="dorm-management">
          <template #icon>
            <HomeOutlined />
          </template>
          <template #title>宿舍管理</template>
          <a-menu-item key="/app/dorm">楼栋房间</a-menu-item>
          <a-menu-item key="/app/students">学生管理</a-menu-item>
          <a-menu-item key="/app/billing">计费管理</a-menu-item>
          <a-menu-item key="/app/power-control">电源控制</a-menu-item>
        </a-sub-menu>

        <!-- AI智能功能 -->
        <a-sub-menu key="ai-features">
          <template #icon>
            <RobotOutlined />
          </template>
          <template #title>AI智能功能</template>
          <a-menu-item key="/app/agent">AI智能代理</a-menu-item>
          <a-menu-item key="/app/ai-chat">AI智能客服</a-menu-item>
          <a-menu-item key="/app/auto-saving">智能节能</a-menu-item>
        </a-sub-menu>

        <!-- 系统管理 -->
        <a-sub-menu key="system-management">
          <template #icon>
            <SettingOutlined />
          </template>
          <template #title>系统管理</template>
          <a-menu-item key="/app/users">用户管理</a-menu-item>
          <a-menu-item key="/app/system-config">系统配置</a-menu-item>
          <a-menu-item key="/app/logs">日志管理</a-menu-item>
          <a-menu-item key="/app/backup">数据备份</a-menu-item>
          <a-menu-item key="/app/api-test">API 测试</a-menu-item>
        </a-sub-menu>

        <!-- 系统监控 -->
        <a-sub-menu key="system-monitor">
          <template #icon>
            <MonitorOutlined />
          </template>
          <template #title>系统监控</template>
          <a-menu-item key="/app/monitor">系统状态</a-menu-item>
          <a-menu-item key="/app/notifications">通知中心</a-menu-item>
        </a-sub-menu>

        <!-- 高级功能 -->
        <a-sub-menu key="advanced-features">
          <template #icon>
            <ApiOutlined />
          </template>
          <template #title>高级功能</template>
          <a-menu-item key="/app/firmware">固件管理</a-menu-item>
          <a-menu-item key="/app/telemetry">遥测数据</a-menu-item>
          <a-menu-item key="/app/import">数据导入</a-menu-item>
          <a-menu-item key="/app/message-templates">消息模板</a-menu-item>
          <a-menu-item key="/app/data-dict">数据字典</a-menu-item>
          <a-menu-item key="/app/collections">收款管理</a-menu-item>
        </a-sub-menu>
      </a-menu>
    </a-layout-sider>

    <a-layout class="main-layout">
      <a-layout-header class="cyber-header">
        <div class="header-bg">
          <div class="header-grid"></div>
          <div class="header-scanline"></div>
        </div>
        
        <div class="header-left">
          <button class="cyber-collapse-btn" @click="toggleCollapsed">
            <MenuUnfoldOutlined v-if="collapsed" />
            <MenuFoldOutlined v-else />
          </button>

          <div class="cyber-breadcrumb">
            <div class="breadcrumb-item" v-for="(item, index) in breadcrumbItems" :key="item.path">
              <span class="breadcrumb-text">{{ item.title }}</span>
              <span v-if="index < breadcrumbItems.length - 1" class="breadcrumb-separator">/</span>
            </div>
          </div>
        </div>

        <div class="header-right">
          <div class="notification-badge">
            <a-badge :count="0" :number-style="{ backgroundColor: '#E86F50' }">
              <BellOutlined class="header-icon" />
            </a-badge>
          </div>

          <a-dropdown>
            <div class="user-profile">
              <div class="user-avatar">
                <div class="avatar-ring"></div>
                <a-avatar :size="36" style="background: linear-gradient(135deg, #2A7965 0%, #1a5c4d 100%)">
                  <template #icon>
                    <UserOutlined />
                  </template>
                </a-avatar>
              </div>
              <div class="user-info">
                <span class="username">{{ authStore.user?.username || 'Admin' }}</span>
                <span class="user-role">Administrator</span>
              </div>
              <DownOutlined class="dropdown-icon" />
            </div>
            <template #overlay>
              <a-menu class="cyber-dropdown" @click="handleUserMenuClick">
                <a-menu-item key="profile">
                  <UserOutlined />
                  <span>个人信息</span>
                </a-menu-item>
                <a-menu-item key="notification-settings">
                  <BellOutlined />
                  <span>通知设置</span>
                </a-menu-item>
                <a-menu-divider />
                <a-menu-item key="logout" style="color: #ff4d4f">
                  <LogoutOutlined />
                  <span>退出登录</span>
                </a-menu-item>
              </a-menu>
            </template>
          </a-dropdown>
        </div>
      </a-layout-header>

      <a-layout-content class="cyber-content">
        <div class="content-wrapper">
          <div class="content-border">
            <div class="border-corner corner-tl"></div>
            <div class="border-corner corner-tr"></div>
            <div class="border-corner corner-bl"></div>
            <div class="border-corner corner-br"></div>
          </div>
          <router-view />
        </div>
      </a-layout-content>
    </a-layout>
  </a-layout>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { message } from 'ant-design-vue'
import {
  ThunderboltOutlined,
  DashboardOutlined,
  DesktopOutlined,
  HistoryOutlined,
  LineChartOutlined,
  ExperimentOutlined,
  MenuFoldOutlined,
  MenuUnfoldOutlined,
  BellOutlined,
  UserOutlined,
  DownOutlined,
  SettingOutlined,
  LogoutOutlined,
  FundViewOutlined,
  PartitionOutlined,
  AlertOutlined,
  ClockCircleOutlined,
  TeamOutlined,
  ApiOutlined,
  MonitorOutlined,
  FileTextOutlined,
  DatabaseOutlined,
  NotificationOutlined,
  HomeOutlined,
  RobotOutlined,
  SafetyOutlined
} from '@ant-design/icons-vue'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()

const collapsed = ref(false)
const selectedKeys = ref<string[]>([])
const openKeys = ref<string[]>(['device-management', 'data-monitoring'])
const searchValue = ref('')

const breadcrumbItems = computed(() => {
  const items = [
    { path: '/app/dashboard', title: '首页' }
  ]
  
  if (route.path !== '/app/dashboard') {
    const categoryMap: Record<string, string> = {
      'dashboard': '首页',
      'devices': '设备管理',
      'groups': '设备管理',
      'aggregate': '设备管理',
      'command-history': '设备管理',
      'live': '数据监控',
      'history': '数据监控',
      'ai': '数据监控',
      'alerts': '系统管理',
      'tasks': '系统管理',
      'users': '系统管理',
      'api-test': '系统管理',
      'system-config': '系统管理',
      'logs': '系统管理',
      'backup': '系统管理',
      'monitor': '系统监控',
      'notifications': '系统监控',
      'dorm': '宿舍管理',
      'students': '宿舍管理',
      'billing': '宿舍管理',
      'power-control': '宿舍管理',
      'agent': 'AI智能功能',
      'ai-chat': 'AI智能功能',
      'auto-saving': 'AI智能功能',
      'firmware': '高级功能',
      'telemetry': '高级功能',
      'import': '高级功能',
      'message-templates': '高级功能',
      'data-dict': '高级功能',
      'collections': '高级功能'
    }
    
    const titleMap: Record<string, string> = {
      '/app/dashboard': '仪表盘',
      '/app/devices': '设备列表',
      '/app/history': '历史数据',
      '/app/live': '实时监控',
      '/app/ai': 'AI 报告',
      '/app/aggregate': '聚合视图',
      '/app/groups': '设备分组',
      '/app/alerts': '告警管理',
      '/app/tasks': '定时任务',
      '/app/users': '用户管理',
      '/app/api-test': 'API 测试',
      '/app/system-config': '系统配置',
      '/app/logs': '日志管理',
      '/app/backup': '数据备份',
      '/app/monitor': '系统状态',
      '/app/notifications': '通知中心',
      '/app/dorm': '楼栋房间',
      '/app/students': '学生管理',
      '/app/billing': '计费管理',
      '/app/power-control': '电源控制',
      '/app/profile': '个人信息',
      '/app/notification-settings': '通知设置',
      '/app/agent': 'AI智能代理',
      '/app/ai-chat': 'AI智能客服',
      '/app/auto-saving': '智能节能',
      '/app/command-history': '命令历史',
      '/app/firmware': '固件管理',
      '/app/telemetry': '遥测数据',
      '/app/import': '数据导入',
      '/app/message-templates': '消息模板',
      '/app/data-dict': '数据字典',
      '/app/collections': '收款管理'
    }
    
    const path = route.path.split('/')[2]
    if (path) {
      const category = categoryMap[path]
      const title = titleMap[`/app/${path}`]
      if (category && title) {
        items.push({ path: route.path, title: `${category} / ${title}` })
      } else if (title) {
        items.push({ path: route.path, title })
      }
    }
  }
  
  return items
})

const getParticleStyle = (index: number) => {
  const size = Math.random() * 3 + 1
  return {
    left: `${Math.random() * 100}%`,
    top: `${Math.random() * 100}%`,
    width: `${size}px`,
    height: `${size}px`,
    animationDelay: `${Math.random() * 5}s`,
    animationDuration: `${Math.random() * 10 + 10}s`
  }
}

const toggleCollapsed = () => {
  collapsed.value = !collapsed.value
}

const handleMenuSelect = ({ key }: { key: string }) => {
  router.push(key)
}

const handleUserMenuClick = ({ key }: { key: string }) => {
  switch (key) {
    case 'profile':
      router.push('/app/profile')
      break
    case 'notification-settings':
      router.push('/app/notification-settings')
      break
    case 'logout':
      authStore.logout()
      message.success('已退出登录')
      router.push('/')
      break
  }
}

const handleSearch = (value: string) => {
  if (!value) return
  
  // 搜索功能映射
  const searchMap: Record<string, string> = {
    '仪表盘': '/app/dashboard',
    '设备': '/app/devices',
    '监控': '/app/live',
    '告警': '/app/alerts',
    '分组': '/app/groups',
    '聚合': '/app/aggregate',
    '历史': '/app/history',
    '报告': '/app/ai',
    '楼栋': '/app/dorm',
    '学生': '/app/students',
    '计费': '/app/billing',
    '电源': '/app/power-control',
    '代理': '/app/agent',
    '客服': '/app/ai-chat',
    '节能': '/app/auto-saving',
    '命令': '/app/command-history',
    '用户': '/app/users',
    '配置': '/app/system-config',
    '日志': '/app/logs',
    '备份': '/app/backup',
    '测试': '/app/api-test',
    '状态': '/app/monitor',
    '通知': '/app/notifications',
    '固件': '/app/firmware',
    '遥测': '/app/telemetry',
    '导入': '/app/import',
    '模板': '/app/message-templates',
    '字典': '/app/data-dict',
    '收款': '/app/collections'
  }
  
  for (const [keyword, path] of Object.entries(searchMap)) {
    if (keyword.includes(value)) {
      router.push(path)
      searchValue.value = ''
      break
    }
  }
}

watch(
  () => route.path,
  (newPath) => {
    const path = newPath.split('/')[2]
    if (path) {
      selectedKeys.value = [`/app/${path}`]
      const categoryMap: Record<string, string> = {
      'devices': 'device-management',
      'groups': 'device-management',
      'aggregate': 'device-management',
      'command-history': 'device-management',
      'live': 'data-monitoring',
      'history': 'data-monitoring',
      'ai': 'data-monitoring',
      'alerts': 'system-management',
      'tasks': 'system-management',
      'users': 'system-management',
      'api-test': 'system-management',
      'system-config': 'system-management',
      'logs': 'system-management',
      'backup': 'system-management',
      'monitor': 'system-monitor',
      'notifications': 'system-monitor',
      'dorm': 'dorm-management',
      'students': 'dorm-management',
      'billing': 'dorm-management',
      'power-control': 'dorm-management',
      'agent': 'ai-features',
      'ai-chat': 'ai-features',
      'auto-saving': 'ai-features',
      'firmware': 'advanced-features',
      'telemetry': 'advanced-features',
      'import': 'advanced-features',
      'message-templates': 'advanced-features',
      'data-dict': 'advanced-features',
      'collections': 'advanced-features'
    }
      const category = categoryMap[path]
      if (category && !openKeys.value.includes(category)) {
        openKeys.value.push(category)
      }
    } else {
      selectedKeys.value = ['/app/dashboard']
    }
  },
  { immediate: true }
)
</script>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Orbitron:wght@400;500;600;700;800;900&family=JetBrains+Mono:wght@400;500;600;700&display=swap');

.app-layout {
  min-height: 100vh;
  background: var(--color-bg-base);
}

.cyber-sidebar {
  position: relative;
  background: var(--sidebar-bg);
  border-right: 1px solid var(--color-border);
  overflow: hidden;
  box-shadow: var(--shadow-md);
}

.sidebar-bg {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  pointer-events: none;
  overflow: hidden;
}

.cyber-grid {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-image: 
    linear-gradient(rgba(42, 121, 101, 0.05) 1px, transparent 1px),
    linear-gradient(90deg, rgba(42, 121, 101, 0.05) 1px, transparent 1px);
  background-size: 30px 30px;
  animation: gridMove 20s linear infinite;
}

@keyframes gridMove {
  0% { transform: translate(0, 0); }
  100% { transform: translate(30px, 30px); }
}

.floating-particles {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
}

.particle {
  position: absolute;
  background: #2A7965;
  border-radius: 50%;
  opacity: 0.3;
  animation: particleFloat 15s ease-in-out infinite;
}

@keyframes particleFloat {
  0%, 100% { transform: translate(0, 0) scale(1); opacity: 0.3; }
  50% { transform: translate(20px, -20px) scale(1.5); opacity: 0.6; }
}

.data-streams {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
}

.stream {
  position: absolute;
  width: 2px;
  height: 100%;
  background: linear-gradient(180deg, 
    transparent 0%, 
    rgba(42, 121, 101, 0.5) 50%, 
    transparent 100%
  );
  animation: streamFlow 3s linear infinite;
}

.stream-1 { left: 10%; animation-delay: 0s; }
.stream-2 { left: 50%; animation-delay: 1s; }
.stream-3 { left: 90%; animation-delay: 2s; }

@keyframes streamFlow {
  0% { transform: translateY(-100%); }
  100% { transform: translateY(100%); }
}

.logo {
  position: relative;
  display: flex;
  align-items: center;
  padding: 24px 20px;
  border-bottom: 1px solid var(--color-border);
  background: linear-gradient(180deg, 
    var(--color-primary-bg) 0%, 
    transparent 100%
  );
}

.menu-search {
  padding: 12px 20px;
  border-bottom: 1px solid var(--color-border);
}

.menu-search :deep(.ant-input) {
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid var(--color-primary-border);
  border-radius: 8px;
  color: var(--color-text-primary);
  font-family: 'JetBrains Mono', monospace;
  font-size: 12px;
}

.menu-search :deep(.ant-input:focus) {
  border-color: var(--color-primary);
  box-shadow: 0 0 20px var(--color-primary-bg);
}

.menu-search :deep(.ant-input::placeholder) {
  color: var(--color-text-tertiary);
}

.logo-hologram {
  position: relative;
  width: 48px;
  height: 48px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.hologram-rings {
  position: absolute;
  width: 100%;
  height: 100%;
}

.ring {
  position: absolute;
  border: 2px solid rgba(42, 121, 101, 0.5);
  border-radius: 50%;
  animation: ringRotate 10s linear infinite;
}

.ring-1 {
  width: 100%;
  height: 100%;
  top: 0;
  left: 0;
}

.ring-2 {
  width: 75%;
  height: 75%;
  top: 12.5%;
  left: 12.5%;
  animation-direction: reverse;
  animation-duration: 8s;
}

.ring-3 {
  width: 50%;
  height: 50%;
  top: 25%;
  left: 25%;
  animation-duration: 6s;
}

@keyframes ringRotate {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

.logo-icon {
  font-size: 24px;
  color: var(--color-primary);
  z-index: 1;
  filter: drop-shadow(0 0 10px var(--color-primary-bg));
  animation: iconPulse 2s ease-in-out infinite;
}

@keyframes iconPulse {
  0%, 100% { filter: drop-shadow(0 0 10px var(--color-primary-bg)); }
  50% { filter: drop-shadow(0 0 20px var(--color-primary-bg)); }
}

.logo-text {
  margin-left: 16px;
  display: flex;
  flex-direction: column;
}

.logo-title {
  font-size: 16px;
  font-weight: 900;
  font-family: 'Orbitron', sans-serif;
  color: var(--color-primary);
  letter-spacing: 2px;
  text-shadow: 0 0 10px var(--color-primary-bg);
}

.logo-subtitle {
  font-size: 9px;
  font-family: 'JetBrains Mono', monospace;
  color: var(--sidebar-text);
  letter-spacing: 1px;
  margin-top: 2px;
}

.cyber-menu {
  position: relative;
  background: transparent;
  border: none;
  margin-top: 8px;
  font-family: 'JetBrains Mono', monospace;
}

.cyber-menu :deep(.ant-menu-item) {
  position: relative;
  color: var(--sidebar-text);
  margin: 4px 12px;
  border-radius: 8px;
  height: 48px;
  line-height: 48px;
  transition: all 0.3s ease;
  overflow: hidden;
}

.cyber-menu :deep(.ant-menu-item::before) {
  content: '';
  position: absolute;
  left: 0;
  top: 0;
  bottom: 0;
  width: 3px;
  background: var(--color-primary);
  transform: scaleY(0);
  transition: transform 0.3s ease;
}

.cyber-menu :deep(.ant-menu-item:hover) {
  color: var(--color-primary);
  background: var(--sidebar-hover-bg);
  transform: translateX(4px);
}

.cyber-menu :deep(.ant-menu-item:hover::before) {
  transform: scaleY(1);
}

.cyber-menu :deep(.ant-menu-item-selected) {
  color: var(--sidebar-text-active);
  background: var(--sidebar-active-bg);
  box-shadow: 
    inset 0 0 20px var(--color-primary-bg),
    0 0 20px var(--color-primary-bg);
}

.cyber-menu :deep(.ant-menu-item-selected::before) {
  transform: scaleY(1);
}

.menu-glitch {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: #2A7965;
  opacity: 0;
  pointer-events: none;
}

.cyber-menu :deep(.ant-menu-item:hover .menu-glitch) {
  animation: menuGlitch 0.3s ease;
}

@keyframes menuGlitch {
  0%, 100% { opacity: 0; transform: translate(0); }
  20% { opacity: 0.1; transform: translate(-2px, 2px); }
  40% { opacity: 0.1; transform: translate(2px, -2px); }
  60% { opacity: 0.1; transform: translate(-2px, -2px); }
  80% { opacity: 0.1; transform: translate(2px, 2px); }
}

.cyber-menu :deep(.ant-menu-submenu-title) {
  color: rgba(255, 255, 255, 0.7);
  margin: 4px 12px;
  border-radius: 8px;
  height: 48px;
  line-height: 48px;
  transition: all 0.3s ease;
}

.cyber-menu :deep(.ant-menu-submenu-title:hover) {
  color: #2A7965;
  background: rgba(42, 121, 101, 0.1);
}

.cyber-menu :deep(.ant-menu-sub) {
  background: rgba(0, 0, 0, 0.3);
}

.cyber-menu :deep(.ant-menu-item-icon) {
  font-size: 18px;
  margin-right: 12px;
}

.main-layout {
  background: var(--color-bg-base);
}

.cyber-header {
  position: relative;
  background: var(--header-bg);
  border-bottom: var(--header-border);
  padding: 0 24px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  backdrop-filter: blur(20px);
  box-shadow: var(--header-shadow);
  height: var(--header-height);
}

.header-bg {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  pointer-events: none;
  overflow: hidden;
}

.header-grid {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-image: 
    linear-gradient(rgba(42, 121, 101, 0.03) 1px, transparent 1px),
    linear-gradient(90deg, rgba(42, 121, 101, 0.03) 1px, transparent 1px);
  background-size: 50px 50px;
}

.header-scanline {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 2px;
  background: linear-gradient(90deg, 
    transparent, 
    rgba(42, 121, 101, 0.8), 
    rgba(79, 172, 254, 0.8), 
    transparent
  );
  animation: headerScan 4s linear infinite;
}

@keyframes headerScan {
  0% { transform: translateX(-100%); }
  100% { transform: translateX(100%); }
}

.header-left {
  position: relative;
  display: flex;
  align-items: center;
  gap: 20px;
}

.cyber-collapse-btn {
  width: 40px;
  height: 40px;
  background: var(--color-primary-bg);
  border: 1px solid var(--color-primary-border);
  border-radius: 8px;
  color: var(--color-primary);
  font-size: 18px;
  cursor: pointer;
  transition: all 0.3s ease;
  display: flex;
  align-items: center;
  justify-content: center;
}

.cyber-collapse-btn:hover {
  background: var(--color-primary-bg);
  border-color: var(--color-primary);
  box-shadow: 0 0 20px var(--color-primary-bg);
  transform: scale(1.05);
}

.cyber-breadcrumb {
  display: flex;
  align-items: center;
  gap: 8px;
  font-family: 'JetBrains Mono', monospace;
  font-size: 13px;
}

.breadcrumb-item {
  display: flex;
  align-items: center;
  gap: 8px;
}

.breadcrumb-text {
  color: var(--color-text-secondary);
  transition: color 0.3s ease;
}

.breadcrumb-item:last-child .breadcrumb-text {
  color: var(--color-primary);
  font-weight: 600;
}

.breadcrumb-separator {
  color: var(--color-text-tertiary);
}

.header-right {
  position: relative;
  display: flex;
  align-items: center;
  gap: 20px;
}

.notification-badge {
  position: relative;
}

.header-icon {
  font-size: 20px;
  color: var(--color-text-secondary);
  cursor: pointer;
  padding: 10px;
  transition: all 0.3s ease;
  border-radius: 8px;
}

.header-icon:hover {
  color: var(--color-primary);
  background: var(--color-primary-bg);
  box-shadow: 0 0 20px var(--color-primary-bg);
}

.user-profile {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 8px 16px;
  background: var(--color-primary-bg);
  border: 1px solid var(--color-primary-border);
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.3s ease;
}

.user-profile:hover {
  background: var(--color-primary-bg);
  border-color: var(--color-primary);
  box-shadow: 0 0 20px var(--color-primary-bg);
}

.user-avatar {
  position: relative;
}

.avatar-ring {
  position: absolute;
  top: -3px;
  left: -3px;
  right: -3px;
  bottom: -3px;
  border: 2px solid var(--color-primary);
  border-radius: 50%;
  animation: avatarRingPulse 2s ease-in-out infinite;
}

@keyframes avatarRingPulse {
  0%, 100% { transform: scale(1); opacity: 1; }
  50% { transform: scale(1.1); opacity: 0.5; }
}

.user-info {
  display: flex;
  flex-direction: column;
}

.username {
  font-size: 14px;
  font-weight: 600;
  font-family: 'Orbitron', sans-serif;
  color: var(--color-text-primary);
  letter-spacing: 0.5px;
}

.user-role {
  font-size: 10px;
  font-family: 'JetBrains Mono', monospace;
  color: var(--color-text-tertiary);
  letter-spacing: 1px;
  text-transform: uppercase;
}

.dropdown-icon {
  font-size: 12px;
  color: var(--color-text-tertiary);
}

.cyber-content {
  position: relative;
  margin: 20px;
  min-height: calc(100vh - 104px);
}

.content-wrapper {
  position: relative;
  background: var(--card-bg);
  backdrop-filter: blur(20px);
  border: var(--card-border);
  border-radius: var(--card-radius);
  padding: 24px;
  min-height: calc(100vh - 152px);
  box-shadow: var(--card-shadow);
  animation: contentFadeIn 0.5s ease;
}

@keyframes contentFadeIn {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.content-border {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  pointer-events: none;
}

.border-corner {
  position: absolute;
  width: 30px;
  height: 30px;
  border: 2px solid rgba(42, 121, 101, 0.5);
  transition: all 0.3s ease;
}

.corner-tl { top: 10px; left: 10px; border-right: none; border-bottom: none; }
.corner-tr { top: 10px; right: 10px; border-left: none; border-bottom: none; }
.corner-bl { bottom: 10px; left: 10px; border-right: none; border-top: none; }
.corner-br { bottom: 10px; right: 10px; border-left: none; border-top: none; }

.content-wrapper:hover .border-corner {
  border-color: #2A7965;
  box-shadow: 0 0 10px rgba(42, 121, 101, 0.5);
}

.cyber-dropdown {
  background: rgba(10, 10, 10, 0.95);
  backdrop-filter: blur(20px);
  border: 1px solid rgba(42, 121, 101, 0.3);
  border-radius: 12px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.6);
  padding: 8px;
}

.cyber-dropdown :deep(.ant-dropdown-menu-item) {
  border-radius: 8px;
  color: rgba(255, 255, 255, 0.8);
  font-family: 'JetBrains Mono', monospace;
  padding: 10px 16px;
  transition: all 0.3s ease;
}

.cyber-dropdown :deep(.ant-dropdown-menu-item:hover) {
  background: rgba(42, 121, 101, 0.2);
  color: #2A7965;
}

@media (max-width: 768px) {
  .cyber-content {
    margin: 16px;
  }
  
  .content-wrapper {
    padding: 16px;
  }
  
  .cyber-header {
    padding: 0 16px;
  }
  
  .user-info {
    display: none;
  }
  
  .cyber-breadcrumb {
    display: none;
  }
}
</style>
