/**
 * 设备类型定义
 */
export interface Device {
  id: string
  name: string
  room: string
  online: boolean
  lastSeen: string
}

/**
 * 插孔状态
 */
export interface SocketStatus {
  id: number
  name: string
  on: boolean
  power_w: number
  current_a?: number
  voltage_v?: number
}

/**
 * 设备状态
 */
export interface StripStatus {
  device_id: string
  ts: number
  total_power_w: number
  sockets: SocketStatus[]
  online: boolean
}

/**
 * 遥测数据点
 */
export interface TelemetryPoint {
  ts: number
  power_w: number
  current_a?: number
  voltage_v?: number
  device_id?: string
}

/**
 * 用户信息
 */
export interface User {
  id?: string
  username: string
  email: string
  role: 'admin' | 'user'
  phone?: string
  department?: string
  createdAt?: string
  lastLoginAt?: string
}

/**
 * 登录请求参数
 */
export interface LoginParams {
  account: string
  password: string
}

/**
 * 登录响应
 */
export interface LoginResponse {
  token: string
  user: User
}

/**
 * 命令类型
 */
export type CommandType = 'ON' | 'OFF' | 'RESTART' | 'QUERY' | 'TOGGLE'

/**
 * 命令状态
 */
export type CommandState = 'pending' | 'success' | 'failed' | 'timeout' | 'cancelled'

/**
 * 命令请求
 */
export interface CommandRequest {
  device_id: string
  socket_id?: number
  type: CommandType
}

/**
 * 命令响应
 */
export interface CommandResponse {
  cmd_id: string
}

/**
 * 命令状态响应
 */
export interface CommandStateResponse {
  cmd_id: string
  state: CommandState
}

/**
 * 仪表板事件
 */
export interface DashboardEvent {
  id: string
  type: 'REPORT' | 'CMD' | 'ALERT' | 'SYSTEM'
  time: string
  detail: string
  status: 'ok' | 'warn' | 'fail'
}

/**
 * 时间范围
 */
export type TimeRange = '60s' | '5m' | '30m' | '1h' | '24h'

/**
 * 规则配置
 */
export interface RuleConfig {
  id: string
  name: string
  enabled: boolean
  scope: string
}

/**
 * 顶级消费者
 */
export interface TopConsumer {
  name: string
  value: number
  percent: number
}

/**
 * 负载分布
 */
export interface LoadDistribution {
  id: string
  label: string
  power: number
  percent: number
}

/**
 * 设备分组
 */
export interface DeviceGroup {
  id: string
  name: string
  description?: string
  createdAt?: string
  updatedAt?: string
}

/**
 * 告警
 */
export interface Alert {
  id: string
  deviceId: string
  type: 'HIGH_POWER' | 'OFFLINE' | 'OVERLOAD' | 'ABNORMAL'
  severity: 'INFO' | 'WARNING' | 'ERROR' | 'CRITICAL'
  message: string
  value?: number
  threshold?: number
  resolved: boolean
  resolvedAt?: string
  createdAt: string
}

/**
 * 告警配置
 */
export interface AlertConfig {
  deviceId: string
  highPowerThreshold?: number
  offlineTimeout?: number
  overloadThreshold?: number
  enabled: boolean
}

/**
 * 定时任务
 */
export interface ScheduledTask {
  id: string
  name: string
  deviceId: string
  action: 'ON' | 'OFF' | 'TOGGLE'
  cronExpression: string
  enabled: boolean
  description?: string
  createdAt?: string
  updatedAt?: string
}

/**
 * 设备历史
 */
export interface DeviceHistory {
  id: string
  deviceId: string
  status: StripStatus
  timestamp: string
}

/**
 * 用电统计
 */
export interface PowerStatistics {
  deviceId?: string
  startTime: string
  endTime: string
  avgPower: number
  peakPower: number
  peakTime: string
  minPower: number
  totalEnergy: number
  dataPoints: number
}

/**
 * 批量命令请求
 */
export interface BatchCommandRequest {
  deviceIds: string[]
  action: string
  socket?: number
}

/**
 * 系统配置项
 */
export interface SystemConfig {
  id: string
  key: string
  value: string
  category: string
  description?: string
  updatedAt?: string
}

/**
 * 日志条目
 */
export interface LogEntry {
  id: string
  level: 'DEBUG' | 'INFO' | 'WARN' | 'ERROR'
  message: string
  source?: string
  timestamp: string
}

/**
 * 日志统计
 */
export interface LogStatistics {
  total: number
  byLevel: Record<string, number>
  byDay: Record<string, number>
}

/**
 * 备份信息
 */
export interface BackupInfo {
  id: string
  name: string
  size: number
  createdAt: string
  type: 'FULL' | 'INCREMENTAL'
}

/**
 * 备份统计
 */
export interface BackupStatistics {
  totalBackups: number
  totalSize: number
  lastBackupTime?: string
}

/**
 * 通知
 */
export interface Notification {
  id: string
  title: string
  message: string
  type: 'INFO' | 'WARNING' | 'ERROR' | 'SUCCESS'
  read: boolean
  createdAt: string
}

/**
 * 未读通知数量
 */
export interface UnreadCount {
  count: number
}

/**
 * 系统监控状态
 */
export interface SystemMonitorStatus {
  cpuCores: number
  cpuUsage: number
  memory: {
    total: number
    used: number
    free: number
    usagePercent: number
  }
  disk: {
    total: number
    used: number
    free: number
    usagePercent: number
  }
  uptime: number
  timestamp: string
}

/**
 * 设备监控状态
 */
export interface DeviceMonitorStatus {
  totalDevices: number
  onlineDevices: number
  offlineDevices: number
  totalPower: number
  avgPowerPerDevice: number
}

/**
 * API性能统计
 */
export interface APIPerformance {
  endpoint: string
  method: string
  avgResponseTime: number
  maxResponseTime: number
  minResponseTime: number
  requestCount: number
  errorCount: number
  errorRate: number
}

/**
 * 电价规则
 */
export interface ElectricityPriceRule {
  id: string
  name: string
  pricePerKwh: number
  startTime?: string
  endTime?: string
  type: 'NORMAL' | 'PEAK' | 'VALLEY'
  enabled: boolean
  createdAt?: string
  updatedAt?: string
}

/**
 * 电费账单
 */
export interface ElectricityBill {
  id: string
  roomId: string
  period: string
  previousReading: number
  currentReading: number
  consumption: number
  amount: number
  status: 'PENDING' | 'PAID' | 'OVERDUE'
  paidAt?: string
  paymentMethod?: string
  createdAt?: string
  updatedAt?: string
}

/**
 * 充值记录
 */
export interface RechargeRecord {
  id: string
  roomId: string
  amount: number
  paymentMethod: string
  operator?: string
  createdAt: string
}

/**
 * 房间余额
 */
export interface RoomBalance {
  roomId: string
  balance: number
  lastRechargeAt?: string
  updatedAt?: string
}

/**
 * 楼栋
 */
export interface Building {
  id: string
  name: string
  code: string
  description?: string
  totalFloors: number
  address?: string
  manager?: string
  contact?: string
  enabled: boolean
  createdAt?: number
  updatedAt?: number
}

/**
 * 宿舍房间
 */
export interface DormRoom {
  id: string
  buildingId: string
  buildingName?: string
  roomNumber: string
  floor: number
  roomType?: string
  capacity: number
  currentOccupants: number
  electricityQuota?: number
  deviceId?: string
  priceRuleId?: string
  status: 'OCCUPIED' | 'VACANT' | 'MAINTENANCE'
  remark?: string
  enabled: boolean
  createdAt?: number
  updatedAt?: number
}

/**
 * 房间统计
 */
export interface RoomStatistics {
  totalRooms: number
  occupiedRooms: number
  vacantRooms: number
  maintenanceRooms: number
  occupancyRate: number
  totalBuildings: number
}

/**
 * 学生
 */
export interface Student {
  id: string
  studentNumber: string
  name: string
  gender: 'MALE' | 'FEMALE'
  department: string
  major: string
  grade: number
  phone?: string
  email?: string
  status: 'ACTIVE' | 'GRADUATED' | 'SUSPENDED'
  roomId?: string
  roomNumber?: string
  checkInDate?: string
  graduationYear?: number
  createdAt?: string
  updatedAt?: string
}

/**
 * 学生入住历史
 */
export interface StudentRoomHistory {
  id: string
  studentId: string
  studentName?: string
  roomId: string
  roomNumber?: string
  checkInDate: string
  checkOutDate?: string
  reason: string
  operator: string
  createdAt?: string
}

/**
 * 学生统计
 */
export interface StudentStatistics {
  totalStudents: number
  activeStudents: number
  graduatedStudents: number
  suspendedStudents: number
  assignedStudents: number
  unassignedStudents: number
  occupancyRate: number
}
