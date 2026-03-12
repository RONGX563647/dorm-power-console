import { get, post, put, del, patch } from '@/utils/request'
import type {
  Device,
  StripStatus,
  TelemetryPoint,
  User,
  LoginParams,
  LoginResponse,
  CommandRequest,
  CommandResponse,
  CommandStateResponse,
  DeviceGroup,
  Alert,
  AlertConfig,
  ScheduledTask,
  DeviceHistory,
  PowerStatistics,
  BatchCommandRequest,
  SystemConfig,
  LogEntry,
  LogStatistics,
  BackupInfo,
  BackupStatistics,
  Notification,
  UnreadCount,
  SystemMonitorStatus,
  DeviceMonitorStatus,
  APIPerformance,
  ElectricityPriceRule,
  ElectricityBill,
  Student,
  StudentRoomHistory,
  StudentStatistics,
  RechargeRecord,
  RoomBalance,
  Building,
  DormRoom,
  RoomStatistics
} from '@/types'

/**
 * 认证相关 API
 */
export const authApi = {
  /**
   * 用户登录
   */
  login: (params: LoginParams) => post<LoginResponse>('/api/auth/login', params),
  
  /**
   * 获取当前用户信息
   */
  getCurrentUser: () => get<User>('/api/auth/me'),
  
  /**
   * 刷新令牌
   */
  refreshToken: () => post<{ token: string }>('/api/auth/refresh'),
  
  /**
   * 用户登出
   */
  logout: () => post('/api/auth/logout'),
  
  /**
   * 用户注册
   */
  register: (params: { username: string, email: string, password: string }) => 
    post<LoginResponse>('/api/auth/register', params),
  
  /**
   * 忘记密码
   */
  forgotPassword: (email: string) => post('/api/auth/forgot-password', { email }),
  
  /**
   * 重置密码
   */
  resetPassword: (params: { token: string, password: string }) => 
    post('/api/auth/reset-password', params)
}

/**
 * 设备相关 API
 */
export const deviceApi = {
  /**
   * 获取设备列表
   */
  getDevices: () => get<Device[]>('/api/devices'),
  
  /**
   * 获取房间设备
   */
  getRoomDevices: (room: string) => get<Device[]>(`/api/devices/room/${room}`),
  
  /**
   * 获取设备状态
   */
  getDeviceStatus: (deviceId: string) => get<StripStatus>(`/api/devices/${deviceId}/status`),
  
  /**
   * 创建设备
   */
  createDevice: (data: Partial<Device>) => post<Device>('/api/devices', data),
  
  /**
   * 更新设备
   */
  updateDevice: (deviceId: string, data: Partial<Device>) => 
    put<Device>(`/api/devices/${deviceId}`, data),
  
  /**
   * 删除设备
   */
  deleteDevice: (deviceId: string) => del(`/api/devices/${deviceId}`),
  
  /**
   * 批量删除设备
   */
  batchDeleteDevices: (deviceIds: string[]) => del(`/api/devices/batch`, { data: deviceIds }),
  
  /**
   * 获取设备遥测数据
   */
  getTelemetry: (device: string, range: string = '24h') => 
    get<TelemetryPoint[]>('/api/telemetry', { params: { device, range } }),
  
  /**
   * 获取遥测统计
   */
  getTelemetryStatistics: (params: { device: string }) => 
    get('/api/telemetry/statistics', { params }),
  
  /**
   * 获取设备历史数据
   */
  getDeviceHistory: (deviceId: string, hours: number = 24) => 
    get<DeviceHistory>(`/api/devices/${deviceId}/history`, { params: { hours } }),
  
  /**
   * 获取设备电量统计
   */
  getPowerStatistics: (deviceId: string, days: number = 7) => 
    get<PowerStatistics>(`/api/devices/${deviceId}/power`, { params: { days } })
}

/**
 * 用户管理 API
 */
export const userApi = {
  /**
   * 获取用户列表
   */
  getUsers: () => get<User[]>('/api/users'),
  
  /**
   * 创建用户
   */
  createUser: (data: { username: string, email: string, password: string, role?: string }) => 
    post<User>('/api/users', data),
  
  /**
   * 获取用户详情
   */
  getUser: (username: string) => get<User>(`/api/users/${username}`),
  
  /**
   * 更新用户
   */
  updateUser: (username: string, data: Partial<User>) => 
    put<User>(`/api/users/${username}`, data),
  
  /**
   * 删除用户
   */
  deleteUser: (username: string) => del(`/api/users/${username}`),
  
  /**
   * 修改密码
   */
  changePassword: (username: string, data: { oldPassword: string, newPassword: string }) => 
    post(`/api/users/${username}/password`, data),
  
  /**
   * 更新个人资料
   */
  updateProfile: (username: string, data: Partial<User>) => 
    patch<User>(`/api/users/${username}/profile`, data)
}

/**
 * 命令相关 API
 */
export const commandApi = {
  /**
   * 发送设备命令
   */
  sendCommand: (deviceId: string, data: { action: string, socket?: number }) => 
    post<CommandResponse>(`/api/strips/${deviceId}/cmd`, data),
  
  /**
   * 批量下发命令
   */
  sendBatchCommand: (data: BatchCommandRequest) => 
    post<{ cmdIds: string[] }>('/api/commands/batch', data),
  
  /**
   * 查询命令状态
   */
  getCommandStatus: (cmdId: string) => get<CommandStateResponse>(`/api/cmd/${cmdId}`),
  
  /**
   * 取消命令
   */
  cancelCommand: (cmdId: string) => post(`/api/commands/${cmdId}/cancel`)
}

/**
 * 设备分组 API
 */
export const groupApi = {
  /**
   * 获取所有分组
   */
  getGroups: () => get<DeviceGroup[]>('/api/groups'),
  
  /**
   * 获取分组详情
   */
  getGroup: (groupId: string) => get<DeviceGroup>(`/api/groups/${groupId}`),
  
  /**
   * 获取分组设备
   */
  getGroupDevices: (groupId: string) => get<Device[]>(`/api/groups/${groupId}/devices`),
  
  /**
   * 创建分组
   */
  createGroup: (data: Partial<DeviceGroup>) => post<DeviceGroup>('/api/groups', data),
  
  /**
   * 更新分组
   */
  updateGroup: (groupId: string, data: Partial<DeviceGroup>) => 
    put<DeviceGroup>(`/api/groups/${groupId}`, data),
  
  /**
   * 删除分组
   */
  deleteGroup: (groupId: string) => del(`/api/groups/${groupId}`),
  
  /**
   * 添加设备到分组
   */
  addDeviceToGroup: (groupId: string, deviceId: string) => 
    post(`/api/groups/${groupId}/devices/${deviceId}`),
  
  /**
   * 从分组移除设备
   */
  removeDeviceFromGroup: (groupId: string, deviceId: string) => 
    del(`/api/groups/${groupId}/devices/${deviceId}`)
}

export const deviceGroupApi = groupApi

/**
 * 告警管理 API
 */
export const alertApi = {
  /**
   * 获取设备告警列表
   */
  getDeviceAlerts: (deviceId: string, onlyUnresolved: boolean = false) => 
    get<Alert[]>(`/api/alerts/device/${deviceId}`, { params: { onlyUnresolved } }),
  
  /**
   * 获取未解决告警列表
   */
  getUnresolvedAlerts: () => get<Alert[]>('/api/alerts/unresolved'),
  
  /**
   * 获取设备告警配置
   */
  getDeviceAlertConfigs: (deviceId: string) => 
    get<AlertConfig[]>(`/api/alerts/config/${deviceId}`),
  
  /**
   * 创建告警配置
   */
  createAlertConfig: (data: Partial<AlertConfig>) => 
    post<AlertConfig>('/api/alerts/config', data),
  
  /**
   * 更新告警配置
   */
  updateAlertConfig: (deviceId: string, data: Partial<AlertConfig>) => {
    const requests = []
    
    if (data.highPowerThreshold) {
      requests.push(
        put<AlertConfig>(`/api/alerts/config/${deviceId}`, {
          type: 'power',
          thresholdMin: 0,
          thresholdMax: data.highPowerThreshold,
          enabled: data.enabled
        })
      )
    }
    
    if (data.overloadThreshold) {
      requests.push(
        put<AlertConfig>(`/api/alerts/config/${deviceId}`, {
          type: 'current',
          thresholdMin: 0,
          thresholdMax: data.overloadThreshold,
          enabled: data.enabled
        })
      )
    }
    
    return Promise.all(requests)
  },
  
  /**
   * 删除告警配置
   */
  deleteAlertConfig: (configId: string) => del(`/api/alerts/config/${configId}`),
  
  /**
   * 解决告警
   */
  resolveAlert: (alertId: string) => put(`/api/alerts/${alertId}/resolve`),
  
  /**
   * 模拟告警
   */
  simulateAlarm: (deviceId: string, type: string = 'power', value: number = 1500) => 
    post(`/api/alerts/simulate/${deviceId}`, null, { params: { type, value } })
}

/**
 * 定时任务 API
 */
export const taskApi = {
  /**
   * 获取所有任务
   */
  getTasks: () => get<ScheduledTask[]>('/api/tasks'),
  
  /**
   * 获取所有任务（别名）
   */
  getAllTasks: () => get<ScheduledTask[]>('/api/tasks'),
  
  /**
   * 获取设备任务列表
   */
  getDeviceTasks: (deviceId: string) => get<ScheduledTask[]>(`/api/tasks/device/${deviceId}`),
  
  /**
   * 创建任务
   */
  createTask: (data: Partial<ScheduledTask>) => post<ScheduledTask>('/api/tasks', data),
  
  /**
   * 更新任务
   */
  updateTask: (taskId: string, data: Partial<ScheduledTask>) => 
    put<ScheduledTask>(`/api/tasks/${taskId}`, data),
  
  /**
   * 删除任务
   */
  deleteTask: (taskId: string) => del(`/api/tasks/${taskId}`),
  
  /**
   * 切换任务状态
   */
  toggleTask: (taskId: string, enabled: boolean) => 
    put<ScheduledTask>(`/api/tasks/${taskId}/toggle`, null, { params: { enabled } }),
  
  /**
   * 立即执行任务
   */
  executeTask: (taskId: string) => post(`/api/tasks/${taskId}/execute`)
}

/**
 * 系统配置 API
 */
export const systemConfigApi = {
  /**
   * 获取所有配置
   */
  getAllConfigs: () => get<SystemConfig[]>('/api/admin/config'),
  
  /**
   * 根据分类获取配置
   */
  getConfigsByCategory: (category: string) => 
    get<SystemConfig[]>(`/api/admin/config/category/${category}`),
  
  /**
   * 获取配置值
   */
  getConfigValue: (key: string) => get<SystemConfig>(`/api/admin/config/${key}`),
  
  /**
   * 更新配置值
   */
  updateConfig: (key: string, value: string) => 
    put<SystemConfig>(`/api/admin/config/${key}`, { value }),
  
  /**
   * 批量更新配置
   */
  batchUpdateConfig: (configs: Record<string, string>) => 
    put('/api/admin/config/batch', configs),
  
  /**
   * 初始化默认配置
   */
  initDefaultConfigs: () => post('/api/admin/config/init')
}

/**
 * 日志管理 API
 */
export const logApi = {
  /**
   * 获取所有日志
   */
  getAllLogs: (params?: { page?: number, size?: number, level?: string }) =>
    get<{ content: LogEntry[], totalElements: number }>('/api/admin/logs', { params }),

  /**
   * 获取日志统计
   */
  getLogStatistics: (days: number = 7) => get<LogStatistics>(`/api/admin/logs/statistics`, { params: { days } })
}

/**
 * 数据备份管理 API
 */
export const backupApi = {
  /**
   * 获取所有备份
   */
  getAllBackups: () => get<BackupInfo[]>('/api/admin/backup'),
  
  /**
   * 获取最近的备份
   */
  getRecentBackups: () => get<BackupInfo[]>('/api/admin/backup/recent'),
  
  /**
   * 创建数据库备份
   */
  createDatabaseBackup: (description: string, createdBy: string) => 
    post<BackupInfo>('/api/admin/backup/database', null, { params: { description, createdBy } }),
  
  /**
   * 创建数据导出备份
   */
  createDataExportBackup: (description: string, createdBy: string) => 
    post<BackupInfo>('/api/admin/backup/export', null, { params: { description, createdBy } }),
  
  /**
   * 删除备份
   */
  deleteBackup: (backupId: number) => del(`/api/admin/backup/${backupId}`),
  
  /**
   * 清理过期备份
   */
  cleanupOldBackups: (retentionDays: number = 30) => 
    del('/api/admin/backup/cleanup', { params: { retentionDays } })
}

/**
 * 通知系统 API
 */
export const notificationApi = {
  /**
   * 获取用户通知
   */
  getUserNotifications: (username: string, params?: { page?: number, size?: number }) =>
    get<{ content: Notification[], totalElements: number }>('/api/notifications', { params: { username, ...params } }),

  /**
   * 获取未读通知数量
   */
  getUnreadCount: (username: string) => get<UnreadCount>(`/api/notifications/unread/count?username=${username}`),

  /**
   * 标记通知为已读
   */
  markAsRead: (notificationId: string) => put(`/api/notifications/${notificationId}/read`),

  /**
   * 标记所有通知为已读
   */
  markAllAsRead: (username: string) => put('/api/notifications/read-all', { username }),

  /**
   * 删除通知
   */
  deleteNotification: (notificationId: string) => del(`/api/notifications/${notificationId}`),

  /**
   * 获取通知统计
   */
  getStatistics: (username: string) =>
    get<{ todayCount: number, weekCount: number, unreadCount: number }>('/api/notifications/statistics', { params: { username } })
}

/**
 * MQTT模拟器 API
 */
export const simulatorApi = {
  /**
   * 启动MQTT模拟器
   */
  startSimulator: (data: any) => post<any>('/api/simulator/start', data),
  
  /**
   * 停止MQTT模拟器
   */
  stopSimulator: (taskId: string) => post<any>(`/api/simulator/stop/${taskId}`),
  
  /**
   * 获取模拟器状态
   */
  getSimulatorStatus: (taskId: string) => get<any>(`/api/simulator/status/${taskId}`),
  
  /**
   * 获取所有模拟器任务
   */
  getAllSimulatorTasks: () => get<any[]>('/api/simulator/tasks')
}

/**
 * 系统监控 API
 */
export const monitorApi = {
  /**
   * 获取系统状态
   */
  getSystemStatus: () => get<any>('/api/admin/monitor/system'),
  
  /**
   * 获取设备监控状态
   */
  getDeviceStatus: () => get<DeviceMonitorStatus>('/api/admin/monitor/devices'),
  
  /**
   * 获取API性能统计
   */
  getAPIPerformance: (hours: number = 24) => 
    get<APIPerformance[]>('/api/admin/monitor/api-performance', { params: { hours } }),
  
  /**
   * 获取历史指标
   */
  getHistoricalMetrics: (type: string, hours: number = 24) => 
    get('/api/admin/monitor/metrics', { params: { type, hours } }),
  
  /**
   * 手动收集指标
   */
  collectMetrics: () => post('/api/admin/monitor/collect'),
  
  /**
   * 清理过期指标
   */
  cleanupOldMetrics: (retentionDays: number = 7) => 
    del('/api/admin/monitor/cleanup', { params: { retentionDays } })
}

/**
 * 健康检查 API
 */
export const healthApi = {
  /**
   * 健康检查
   */
  check: () => get('/health')
}

/**
 * 电费计费 API
 */
export const billingApi = {
  /**
   * 获取电价规则
   */
  getPriceRules: () => get<ElectricityPriceRule[]>('/api/billing/price-rules'),
  
  /**
   * 创建电价规则
   */
  createPriceRule: (data: Partial<ElectricityPriceRule>) => 
    post<ElectricityPriceRule>('/api/billing/price-rules', data),
  
  /**
   * 更新电价规则
   */
  updatePriceRule: (id: string, data: Partial<ElectricityPriceRule>) => 
    put<ElectricityPriceRule>(`/api/billing/price-rules/${id}`, data),
  
  /**
   * 删除电价规则
   */
  deletePriceRule: (id: string) => del(`/api/billing/price-rules/${id}`),
  
  /**
   * 生成账单
   */
  generateBill: (roomId: string, period: string) => 
    post<ElectricityBill>('/api/billing/bills/generate', null, { params: { roomId, period } }),
  
  /**
   * 获取房间账单
   */
  getRoomBills: (roomId: string, params?: { page?: number, size?: number }) => 
    get<{ content: ElectricityBill[], totalElements: number }>('/api/billing/bills', { params: { roomId, ...params } }),
  
  /**
   * 获取待缴费账单
   */
  getPendingBills: () => get<ElectricityBill[]>('/api/billing/bills/pending'),
  
  /**
   * 缴费
   */
  payBill: (billId: string, paymentMethod: string, operator?: string) => 
    post<ElectricityBill>(`/api/billing/bills/${billId}/pay`, null, { params: { paymentMethod, operator } }),
  
  /**
   * 充值
   */
  recharge: (roomId: string, amount: number, paymentMethod: string, operator?: string) => 
    post<RechargeRecord>('/api/billing/recharge', null, { params: { roomId, amount, paymentMethod, operator } }),
  
  /**
   * 获取充值记录
   */
  getRechargeRecords: (roomId: string, params?: { page?: number, size?: number }) => 
    get<{ content: RechargeRecord[], totalElements: number }>('/api/billing/recharge-records', { params: { roomId, ...params } }),
  
  /**
   * 获取房间余额
   */
  getRoomBalance: (roomId: string) => get<RoomBalance>(`/api/billing/balance/${roomId}`)
}

/**
 * AI分析报告 API
 */
export const aiReportApi = {
  /**
   * 获取AI分析报告
   */
  getReport: (roomId: string, period: string = '7d') => 
    get(`/api/rooms/${roomId}/ai_report`, { params: { period } })
}

/**
 * 宿舍管理 API
 */
export const dormApi = {
  /**
   * 获取楼栋列表
   */
  getBuildings: () => get<Building[]>('/api/dorm/buildings'),

  /**
   * 创建楼栋
   */
  createBuilding: (data: Partial<Building>) => post<Building>('/api/dorm/buildings', data),

  /**
   * 更新楼栋
   */
  updateBuilding: (id: string, data: Partial<Building>) => put<Building>(`/api/dorm/buildings/${id}`, data),

  /**
   * 删除楼栋
   */
  deleteBuilding: (id: string) => del(`/api/dorm/buildings/${id}`),

  /**
   * 获取房间列表
   */
  getAllRooms: () => get<DormRoom[]>('/api/dorm/rooms'),

  /**
   * 获取楼栋房间
   */
  getRoomsByBuilding: (buildingId: string) =>
    get<DormRoom[]>(`/api/dorm/buildings/${buildingId}/rooms`),

  /**
   * 获取楼层房间
   */
  getRoomsByFloor: (buildingId: string, floor: number) =>
    get<DormRoom[]>(`/api/dorm/buildings/${buildingId}/floors/${floor}/rooms`),

  /**
   * 创建房间
   */
  createRoom: (data: Partial<DormRoom>) => post<DormRoom>('/api/dorm/rooms', data),

  /**
   * 更新房间
   */
  updateRoom: (id: string, data: Partial<DormRoom>) =>
    put<DormRoom>(`/api/dorm/rooms/${id}`, data),

  /**
   * 删除房间
   */
  deleteRoom: (id: string) => del(`/api/dorm/rooms/${id}`),

  /**
   * 入住
   */
  checkIn: (roomId: string, occupantCount: number) =>
    post<DormRoom>(`/api/dorm/rooms/${roomId}/check-in`, null, { params: { occupantCount } }),

  /**
   * 退宿
   */
  checkOut: (roomId: string) => post<DormRoom>(`/api/dorm/rooms/${roomId}/check-out`),

  /**
   * 获取房间统计
   */
  getRoomStatistics: () => get<RoomStatistics>('/api/dorm/rooms/statistics')
}

/**
 * 学生管理 API
 */
export const studentApi = {
  /**
   * 获取学生列表
   */
  getStudents: (params?: { page?: number, size?: number, status?: string, department?: string }) =>
    get<{ content: Student[], totalElements: number }>('/api/students', { params }),

  /**
   * 创建学生
   */
  createStudent: (data: Partial<Student>) => post<Student>('/api/students', data),

  /**
   * 获取学生详情
   */
  getStudent: (id: string) => get<Student>(`/api/students/${id}`),

  /**
   * 根据学号查询学生
   */
  getStudentByNumber: (studentNumber: string) => get<Student>(`/api/students/number/${studentNumber}`),

  /**
   * 更新学生信息
   */
  updateStudent: (id: string, data: Partial<Student>) => put<Student>(`/api/students/${id}`, data),

  /**
   * 删除学生
   */
  deleteStudent: (id: string) => del(`/api/students/${id}`),

  /**
   * 学生入住
   */
  checkInStudent: (studentId: string, roomId: string, reason?: string, operator?: string) =>
    post<Student>(`/api/students/${studentId}/check-in`, null, { params: { roomId, reason, operator } }),

  /**
   * 学生退宿
   */
  checkOutStudent: (studentId: string, reason?: string, operator?: string) =>
    post<Student>(`/api/students/${studentId}/check-out`, null, { params: { reason, operator } }),

  /**
   * 调换宿舍
   */
  swapRoom: (studentId: string, newRoomId: string, reason?: string, operator?: string) =>
    post<Student>(`/api/students/${studentId}/swap-room`, null, { params: { newRoomId, reason, operator } }),

  /**
   * 获取房间的学生列表
   */
  getStudentsByRoom: (roomId: string) => get<Student[]>(`/api/students/room/${roomId}`),

  /**
   * 获取学生入住历史
   */
  getStudentHistory: (studentId: string, params?: { page?: number, size?: number }) =>
    get<{ content: StudentRoomHistory[], totalElements: number }>(`/api/students/${studentId}/history`, { params }),

  /**
   * 搜索学生
   */
  searchStudents: (params: { keyword?: string, department?: string, status?: string, page?: number, size?: number }) =>
    get<{ content: Student[], totalElements: number }>('/api/students/search', { params }),

  /**
   * 获取未分配房间的学生
   */
  getUnassignedStudents: () => get<Student[]>('/api/students/unassigned'),

  /**
   * 获取学生统计
   */
  getStudentStatistics: () => get<StudentStatistics>('/api/students/statistics'),

  /**
   * 批量毕业处理
   */
  batchGraduate: (graduationYear: number, operator?: string) =>
    post<{ processedCount: number }>('/api/students/batch/graduate', null, { params: { graduationYear, operator } })
}

/**
 * AI智能代理 API
 */
export const agentApi = {
  /**
   * 行为学习
   */
  learnBehavior: (roomId: string, days: number = 7) => 
    get(`/api/agent/behavior/learn/${roomId}`, { params: { days } }),
  
  /**
   * 获取行为建议
   */
  getBehaviorSuggestions: (roomId: string) => 
    get(`/api/agent/behavior/suggestions/${roomId}`),
  
  /**
   * 行为预测
   */
  predictBehavior: (roomId: string, hours: number = 24) => 
    get(`/api/agent/behavior/predict/${roomId}`, { params: { hours } }),
  
  /**
   * 行为对比
   */
  compareBehavior: (roomId: string, period1Start: string, period1End: string, period2Start: string, period2End: string) => 
    get(`/api/agent/behavior/compare/${roomId}`, { params: { period1Start, period1End, period2Start, period2End } }),
  
  /**
   * 实时异常检测
   */
  detectAnomalyRealtime: (deviceId: string, data: any) => 
    post(`/api/agent/anomaly/detect/realtime/${deviceId}`, data),
  
  /**
   * 批量异常检测
   */
  detectAnomalyBatch: (deviceId: string, hours: number = 24) => 
    get(`/api/agent/anomaly/detect/batch/${deviceId}`, { params: { hours } }),
  
  /**
   * 异常预测
   */
  predictAnomaly: (deviceId: string, hours: number = 24) => 
    get(`/api/agent/anomaly/predict/${deviceId}`, { params: { hours } }),
  
  /**
   * 设备健康度
   */
  getDeviceHealth: (deviceId: string) => 
    get(`/api/agent/anomaly/health/${deviceId}`),
  
  /**
   * 房间异常统计
   */
  getRoomAnomalies: (roomId: string) => 
    get(`/api/agent/anomaly/room/${roomId}`),
  
  /**
   * 设备故障预测
   */
  predictFailure: (deviceId: string) => 
    get(`/api/agent/anomaly/failure/${deviceId}`),
  
  /**
   * 自动生成场景
   */
  autoGenerateScene: (roomId: string) => 
    post(`/api/agent/scene/auto-generate/${roomId}`),
  
  /**
   * 获取房间场景
   */
  getRoomScenes: (roomId: string) => 
    get(`/api/agent/scene/room/${roomId}`),
  
  /**
   * 执行场景
   */
  executeScene: (sceneId: string) => 
    post(`/api/agent/scene/execute/${sceneId}`),
  
  /**
   * 切换场景状态
   */
  toggleScene: (sceneId: string) => 
    put(`/api/agent/scene/toggle/${sceneId}`),
  
  /**
   * 删除场景
   */
  deleteScene: (sceneId: string) => 
    del(`/api/agent/scene/${sceneId}`),
  
  /**
   * 场景统计
   */
  getSceneStatistics: (sceneId: string) => 
    get(`/api/agent/scene/statistics/${sceneId}`),
  
  /**
   * 场景历史
   */
  getSceneHistory: (sceneId: string, days: number = 30) => 
    get(`/api/agent/scene/history/${sceneId}`, { params: { days } }),
  
  /**
   * 优化场景
   */
  optimizeScene: (sceneId: string) => 
    post(`/api/agent/scene/optimize/${sceneId}`),
  
  /**
   * 综合分析
   */
  analyzeRoom: (roomId: string) => 
    get(`/api/agent/analyze/${roomId}`)
}

/**
 * 权限管理 API
 */
export const rbacApi = {
  /**
   * 获取所有角色
   */
  getRoles: () => get('/api/rbac/roles'),
  
  /**
   * 获取启用的角色
   */
  getEnabledRoles: () => get('/api/rbac/roles/enabled'),
  
  /**
   * 创建角色
   */
  createRole: (data: any) => post('/api/rbac/roles', data),
  
  /**
   * 获取角色详情
   */
  getRole: (roleId: string) => get(`/api/rbac/roles/${roleId}`),
  
  /**
   * 更新角色
   */
  updateRole: (roleId: string, data: any) => put(`/api/rbac/roles/${roleId}`, data),
  
  /**
   * 删除角色
   */
  deleteRole: (roleId: string) => del(`/api/rbac/roles/${roleId}`),
  
  /**
   * 分配角色权限
   */
  assignRolePermissions: (roleId: string, permissionIds: string[]) => 
    post(`/api/rbac/roles/${roleId}/permissions`, { permissionIds }),
  
  /**
   * 获取角色权限
   */
  getRolePermissions: (roleId: string) => get(`/api/rbac/roles/${roleId}/permissions`),
  
  /**
   * 获取角色用户数
   */
  getRoleUserCount: (roleId: string) => get(`/api/rbac/roles/${roleId}/user-count`),
  
  /**
   * 获取所有权限
   */
  getPermissions: () => get('/api/rbac/permissions'),
  
  /**
   * 创建权限
   */
  createPermission: (data: any) => post('/api/rbac/permissions', data),
  
  /**
   * 获取权限详情
   */
  getPermission: (permissionId: string) => get(`/api/rbac/permissions/${permissionId}`),
  
  /**
   * 更新权限
   */
  updatePermission: (permissionId: string, data: any) => put(`/api/rbac/permissions/${permissionId}`, data),
  
  /**
   * 删除权限
   */
  deletePermission: (permissionId: string) => del(`/api/rbac/permissions/${permissionId}`),
  
  /**
   * 获取所有资源
   */
  getResources: () => get('/api/rbac/resources'),
  
  /**
   * 创建资源
   */
  createResource: (data: any) => post('/api/rbac/resources', data),
  
  /**
   * 获取资源树
   */
  getResourceTree: () => get('/api/rbac/resources/tree'),
  
  /**
   * 获取资源详情
   */
  getResource: (resourceId: string) => get(`/api/rbac/resources/${resourceId}`),
  
  /**
   * 更新资源
   */
  updateResource: (resourceId: string, data: any) => put(`/api/rbac/resources/${resourceId}`, data),
  
  /**
   * 删除资源
   */
  deleteResource: (resourceId: string) => del(`/api/rbac/resources/${resourceId}`),
  
  /**
   * 获取用户权限
   */
  getUserPermissions: (username: string) => get(`/api/rbac/users/${username}/permissions`),
  
  /**
   * 获取用户角色
   */
  getUserRoles: (username: string) => get(`/api/rbac/users/${username}/roles`),
  
  /**
   * 分配用户角色
   */
  assignUserRoles: (username: string, roleIds: string[]) => 
    post(`/api/rbac/users/${username}/roles`, { roleIds }),
  
  /**
   * 更新用户角色
   */
  updateUserRoles: (username: string, roleIds: string[]) => 
    put(`/api/rbac/users/${username}/roles`, { roleIds }),
  
  /**
   * 移除用户角色
   */
  removeUserRoles: (username: string) => del(`/api/rbac/users/${username}/roles`),
  
  /**
   * 检查用户权限
   */
  hasPermission: (username: string, permission: string) => 
    get(`/api/rbac/users/${username}/has-permission`, { params: { permission } }),
  
  /**
   * 检查用户角色
   */
  hasRole: (username: string, role: string) => 
    get(`/api/rbac/users/${username}/has-role`, { params: { role } }),
  
  /**
   * 初始化RBAC
   */
  initRbac: () => post('/api/rbac/init')
}

/**
 * 电源控制 API
 */
export const powerControlApi = {
  /**
   * 断电
   */
  cutoff: (roomId: string, reason?: string) => 
    post(`/api/power-control/cutoff/${roomId}`, null, { params: { reason } }),
  
  /**
   * 恢复供电
   */
  restore: (roomId: string) => post(`/api/power-control/restore/${roomId}`),
  
  /**
   * 获取房间电源状态
   */
  getStatus: (roomId: string) => get(`/api/power-control/status/${roomId}`),
  
  /**
   * 获取断电房间列表
   */
  getCutoffRooms: () => get('/api/power-control/cutoff-rooms'),
  
  /**
   * 获取欠费房间列表
   */
  getOverdueRooms: () => get('/api/power-control/overdue-rooms')
}

/**
 * IP访问控制 API
 */
export const ipAccessControlApi = {
  /**
   * 添加到白名单
   */
  addToWhitelist: (data: any) => post('/api/ip-control/whitelist', data),
  
  /**
   * 添加到黑名单
   */
  addToBlacklist: (data: any) => post('/api/ip-control/blacklist', data),
  
  /**
   * 删除IP
   */
  deleteIp: (ipAddress: string) => del(`/api/ip-control/${ipAddress}`),
  
  /**
   * 更新IP
   */
  updateIp: (ipAddress: string, data: any) => put(`/api/ip-control/${ipAddress}`, data),
  
  /**
   * 获取白名单
   */
  getWhitelist: () => get('/api/ip-control/whitelist'),
  
  /**
   * 获取黑名单
   */
  getBlacklist: () => get('/api/ip-control/blacklist'),
  
  /**
   * 获取活动IP
   */
  getActiveIps: () => get('/api/ip-control/active'),
  
  /**
   * 检查IP
   */
  checkIp: (ipAddress: string) => get(`/api/ip-control/check/${ipAddress}`),
  
  /**
   * 检查是否被封禁
   */
  isBlocked: (ipAddress: string) => get(`/api/ip-control/blocked/${ipAddress}`),
  
  /**
   * 检查是否在白名单
   */
  isWhitelisted: (ipAddress: string) => get(`/api/ip-control/whitelisted/${ipAddress}`),
  
  /**
   * 获取IP详情
   */
  getIpDetails: (ipAddress: string) => get(`/api/ip-control/${ipAddress}`),
  
  /**
   * 清理过期IP
   */
  cleanup: () => del('/api/ip-control/cleanup')
}

/**
 * 登录日志 API
 */
export const loginLogApi = {
  /**
   * 获取登录日志列表
   */
  getLogs: (params?: { page?: number, size?: number }) => 
    get('/api/login-logs', { params }),
  
  /**
   * 获取用户登录日志
   */
  getUserLogs: (username: string) => get(`/api/login-logs/user/${username}`),
  
  /**
   * 获取时间范围内的日志
   */
  getLogsByRange: (startTime: string, endTime: string) => 
    get('/api/login-logs/range', { params: { startTime, endTime } }),
  
  /**
   * 获取登录统计
   */
  getStatistics: () => get('/api/login-logs/statistics'),
  
  /**
   * 检查用户是否被锁定
   */
  isLocked: (username: string) => get(`/api/login-logs/locked/${username}`),
  
  /**
   * 检查IP是否被封禁
   */
  isBlockedIp: (ipAddress: string) => get(`/api/login-logs/blocked-ip/${ipAddress}`),
  
  /**
   * 获取最后登录时间
   */
  getLastLogin: (username: string) => get(`/api/login-logs/last-login/${username}`),
  
  /**
   * 获取活跃会话
   */
  getActiveSessions: () => get('/api/login-logs/active-sessions'),
  
  /**
   * 清理过期日志
   */
  cleanup: () => del('/api/login-logs/cleanup')
}

/**
 * 审计日志 API
 */
export const auditLogApi = {
  /**
   * 获取审计日志列表
   */
  getLogs: (params?: { page?: number, size?: number }) => 
    get('/api/audit-logs', { params }),
  
  /**
   * 获取用户审计日志
   */
  getUserLogs: (username: string) => get(`/api/audit-logs/user/${username}`),
  
  /**
   * 获取模块审计日志
   */
  getModuleLogs: (module: string) => get(`/api/audit-logs/module/${module}`),
  
  /**
   * 获取时间范围内的日志
   */
  getLogsByRange: (startTime: string, endTime: string) => 
    get('/api/audit-logs/range', { params: { startTime, endTime } }),
  
  /**
   * 获取用户时间范围内的日志
   */
  getUserLogsByRange: (username: string, startTime: string, endTime: string) => 
    get(`/api/audit-logs/user/${username}/range`, { params: { startTime, endTime } }),
  
  /**
   * 获取审计统计
   */
  getStatistics: () => get('/api/audit-logs/statistics'),
  
  /**
   * 清理过期日志
   */
  cleanup: () => del('/api/audit-logs/cleanup')
}

/**
 * 消息模板 API
 */
export const messageTemplateApi = {
  /**
   * 创建模板
   */
  createTemplate: (data: any) => post('/api/message-templates', data),
  
  /**
   * 更新模板
   */
  updateTemplate: (id: string, data: any) => put(`/api/message-templates/${id}`, data),
  
  /**
   * 删除模板
   */
  deleteTemplate: (id: string) => del(`/api/message-templates/${id}`),
  
  /**
   * 根据代码获取模板
   */
  getTemplateByCode: (templateCode: string) => get(`/api/message-templates/code/${templateCode}`),
  
  /**
   * 根据类型获取模板
   */
  getTemplatesByType: (type: string) => get(`/api/message-templates/type/${type}`),
  
  /**
   * 根据渠道获取模板
   */
  getTemplatesByChannel: (channel: string) => get(`/api/message-templates/channel/${channel}`),
  
  /**
   * 获取启用的模板
   */
  getEnabledTemplates: () => get('/api/message-templates/enabled'),
  
  /**
   * 渲染模板
   */
  renderTemplate: (templateCode: string, variables: any) => 
    post(`/api/message-templates/render/${templateCode}`, variables),
  
  /**
   * 提取变量
   */
  extractVariables: (content: string) => post('/api/message-templates/extract-variables', { content }),
  
  /**
   * 初始化默认模板
   */
  initTemplates: () => post('/api/message-templates/init')
}

/**
 * 数据字典 API
 */
export const dataDictApi = {
  /**
   * 获取所有字典类型
   */
  getTypes: () => get('/api/dict/types'),
  
  /**
   * 根据类型获取字典
   */
  getByType: (dictType: string) => get(`/api/dict/type/${dictType}`),
  
  /**
   * 获取字典树
   */
  getTree: (dictType: string) => get(`/api/dict/tree/${dictType}`),
  
  /**
   * 分页查询
   */
  getPage: (params: { dictType?: string, dictCode?: string, page?: number, size?: number }) => 
    get('/api/dict/page', { params }),
  
  /**
   * 根据代码获取字典
   */
  getByCode: (dictCode: string) => get(`/api/dict/code/${dictCode}`),
  
  /**
   * 创建字典
   */
  create: (data: any) => post('/api/dict', data),
  
  /**
   * 批量创建
   */
  batchCreate: (data: any[]) => post('/api/dict/batch', data),
  
  /**
   * 更新字典
   */
  update: (id: string, data: any) => put(`/api/dict/${id}`, data),
  
  /**
   * 删除字典
   */
  delete: (id: string) => del(`/api/dict/${id}`),
  
  /**
   * 根据标签查询
   */
  getByLabel: (dictType: string, label: string) => 
    get('/api/dict/label', { params: { dictType, label } }),
  
  /**
   * 初始化默认字典
   */
  initDict: () => post('/api/dict/init')
}

/**
 * 收款管理 API
 */
export const collectionApi = {
  /**
   * 创建收款记录
   */
  create: (data: any) => post('/api/collections', data),
  
  /**
   * 获取收款记录列表
   */
  getList: (params?: { page?: number, size?: number }) => get('/api/collections', { params }),
  
  /**
   * 获取房间收款记录
   */
  getByRoom: (roomId: string) => get(`/api/collections/room/${roomId}`),
  
  /**
   * 获取账单收款记录
   */
  getByBill: (billId: string) => get(`/api/collections/bill/${billId}`),
  
  /**
   * 清理过期记录
   */
  cleanup: () => del('/api/collections/cleanup')
}

/**
 * 固件管理 API
 */
export const firmwareApi = {
  /**
   * 创建升级任务
   */
  createUpgrade: (data: any) => post('/api/firmware/upgrade', data),
  
  /**
   * 发送固件
   */
  sendFirmware: (firmwareId: string) => post(`/api/firmware/${firmwareId}/send`),
  
  /**
   * 更新进度
   */
  updateProgress: (firmwareId: string, progress: number) => 
    put(`/api/firmware/${firmwareId}/progress`, { progress }),
  
  /**
   * 完成升级
   */
  completeUpgrade: (firmwareId: string, success: boolean, message?: string) => 
    post(`/api/firmware/${firmwareId}/complete`, { success, message }),
  
  /**
   * 取消升级
   */
  cancelUpgrade: (firmwareId: string) => post(`/api/firmware/${firmwareId}/cancel`),
  
  /**
   * 获取设备固件历史
   */
  getDeviceHistory: (deviceId: string) => get(`/api/firmware/device/${deviceId}`),
  
  /**
   * 获取设备当前固件
   */
  getDeviceCurrent: (deviceId: string) => get(`/api/firmware/device/${deviceId}/current`),
  
  /**
   * 获取待升级任务
   */
  getPending: () => get('/api/firmware/pending'),
  
  /**
   * 获取活动升级任务
   */
  getActive: () => get('/api/firmware/active'),
  
  /**
   * 获取固件详情
   */
  getDetails: (firmwareId: string) => get(`/api/firmware/${firmwareId}`)
}

/**
 * 数据导入 API
 */
export const importApi = {
  /**
   * 导入学生
   */
  importStudents: (file: File) => {
    const formData = new FormData()
    formData.append('file', file)
    return post('/api/import/students', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
  },
  
  /**
   * 导入房间
   */
  importRooms: (file: File) => {
    const formData = new FormData()
    formData.append('file', file)
    return post('/api/import/rooms', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
  },
  
  /**
   * 导入设备
   */
  importDevices: (file: File) => {
    const formData = new FormData()
    formData.append('file', file)
    return post('/api/import/devices', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
  },
  
  /**
   * 导入JSON数据
   */
  importJson: (data: any) => post('/api/import/json', data),
  
  /**
   * 下载模板
   */
  downloadTemplate: (type: string) => get(`/api/import/template/${type}`, { responseType: 'blob' })
}

/**
 * 设备历史状态 API
 */
export const deviceHistoryApi = {
  /**
   * 获取设备历史记录列表
   */
  getDeviceHistory: (deviceId: string, params?: { page?: number, size?: number }) => 
    get(`/api/devices/${deviceId}/history`, { params }),
  
  /**
   * 获取历史记录详情
   */
  getHistoryDetails: (historyId: string) => get(`/api/devices/history/${historyId}`)
}

/**
 * 遥测数据 API
 */
export const telemetryApi = {
  /**
   * 获取遥测数据
   */
  getTelemetry: (deviceId: string, range: string = '60s') => 
    get('/api/telemetry', { params: { device: deviceId, range } }),
  
  /**
   * 导出遥测数据
   */
  exportTelemetry: (deviceId: string, startTime: string, endTime: string, format: string = 'csv') => 
    get('/api/telemetry/export', { params: { device: deviceId, startTime, endTime, format }, responseType: 'blob' }),
  
  /**
   * 获取遥测统计
   */
  getStatistics: (deviceId: string) => {
    // 计算默认时间范围（最近24小时）
    const now = Date.now() / 1000;
    const start = Math.floor(now - 24 * 60 * 60);
    const end = Math.floor(now);
    return get('/api/telemetry/statistics', { params: { device: deviceId, period: 'day', start, end } });
  }
}

/**
 * AI智能客服 API
 */
export const aiChatApi = {
  /**
   * 智能对话
   */
  chat: (message: string, userId?: string) => {
    const headers: Record<string, string> = {}
    if (userId) {
      headers['X-User-Id'] = userId
    }
    return post<{ success: boolean; response: string; userId: string }>('/api/agent/chat', { message }, { headers })
  },
  
  /**
   * 意图识别
   */
  recognizeIntent: (message: string) => 
    post<{ success: boolean; intent: string; confidence: number; entities: any; needLLM: boolean; apiEndpoint?: string }>('/api/agent/intent', { message }),
  
  /**
   * 快速问答
   */
  quickReply: (message: string) => 
    post<{ success: boolean; matched: boolean; response: string }>('/api/agent/quick', { message }),
  
  /**
   * 健康检查
   */
  health: () => get<{ status: string; service: string; features: any }>('/api/agent/health')
}

/**
 * 智能节能 API
 */
export const autoSavingApi = {
  /**
   * 预测用电量
   */
  predictPower: (roomId: string, days: number = 7) => 
    get<any>('/api/saving/predict/' + roomId, { params: { days } }),
  
  /**
   * 获取小时预测
   */
  getHourlyPrediction: (roomId: string) => 
    get<any[]>('/api/saving/predict/' + roomId + '/hourly'),
  
  /**
   * 生成节能策略
   */
  getStrategies: (roomId: string) => 
    get<any[]>('/api/saving/strategies/' + roomId),
  
  /**
   * 执行节能策略
   */
  executeStrategy: (roomId: string, strategyId: string) => 
    post<{ success: boolean; roomId: string; strategyId: string; message: string }>('/api/saving/strategies/' + roomId + '/execute/' + strategyId),
  
  /**
   * 获取自动节能状态
   */
  getAutoSavingStatus: () => 
    get<{ enabled: boolean; message: string }>('/api/saving/auto/status'),
  
  /**
   * 开启/关闭自动节能
   */
  toggleAutoSaving: (enabled: boolean) => 
    post<{ success: boolean; enabled: boolean; message: string }>('/api/saving/auto/toggle', null, { params: { enabled } }),
  
  /**
   * 设置用电阈值
   */
  setThreshold: (threshold: number) => 
    post<{ success: boolean; threshold: number; message: string }>('/api/saving/auto/threshold', null, { params: { threshold } }),
  
  /**
   * 获取节能统计
   */
  getSavingStats: (roomId: string) => 
    get<any>('/api/saving/stats/' + roomId),
  
  /**
   * 获取所有房间节能统计
   */
  getAllSavingStats: () => 
    get<{ rooms: any; summary: any }>('/api/saving/stats/all'),
  
  /**
   * 节能分析报告
   */
  generateReport: (roomId: string, days: number = 7) => 
    get<{ roomId: string; prediction: any; strategies: any[]; stats: any; recommendations: string[] }>('/api/saving/report/' + roomId, { params: { days } })
}

/**
 * 命令历史 API
 */
export const commandHistoryApi = {
  /**
   * 获取设备命令历史
   */
  getDeviceCommands: (deviceId: string) => 
    get<any[]>('/api/commands/device/' + deviceId),
  
  /**
   * 获取命令详情
   */
  getCommandDetails: (cmdId: string) => 
    get<any>('/api/commands/' + cmdId)
}


