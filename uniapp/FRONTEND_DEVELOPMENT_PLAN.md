# 前端开发计划

## 一、API接口分析

### 1. 认证管理
- `/api/auth/login` - 登录
- `/api/auth/register` - 注册
- `/api/auth/forgot-password` - 忘记密码
- `/api/auth/reset-password` - 重置密码
- `/api/auth/logout` - 登出
- `/api/auth/me` - 获取当前用户
- `/api/auth/refresh` - 刷新令牌

### 2. 设备管理
- `/api/devices` - 获取设备列表
- `/api/devices/{deviceId}` - 获取设备详情
- `/api/devices/{deviceId}/status` - 获取设备状态
- `/api/devices/room/{room}` - 获取房间设备列表

### 3. 设备控制
- `/api/strips/{deviceId}/cmd` - 下发控制命令
- `/api/commands/batch` - 批量下发命令
- `/api/commands/device/{deviceId}` - 获取命令历史

### 4. 遥测数据
- `/api/telemetry` - 获取遥测数据
- `/api/telemetry/statistics` - 获取用电统计
- `/api/telemetry/export` - 导出遥测数据

### 5. 断电控制
- `/api/power-control/cutoff/{roomId}` - 手动断电
- `/api/power-control/restore/{roomId}` - 恢复供电
- `/api/power-control/status/{roomId}` - 获取断电状态
- `/api/power-control/cutoff-rooms` - 获取断电房间
- `/api/power-control/overdue-rooms` - 获取欠费房间

### 6. 计费管理
- `/api/billing/price-rules` - 电价规则管理
- `/api/billing/bills` - 账单管理
- `/api/billing/bills/pending` - 待缴费账单
- `/api/billing/bills/{billId}/pay` - 缴纳电费
- `/api/billing/recharge` - 余额充值
- `/api/billing/balance/{roomId}` - 获取房间余额
- `/api/billing/low-balance` - 余额不足房间

### 7. 系统监控
- `/api/admin/monitor/system` - 系统状态
- `/api/admin/monitor/devices` - 设备状态
- `/api/admin/monitor/api-performance` - API性能
- `/api/admin/monitor/metrics` - 历史指标

### 8. 用户管理
- `/api/users` - 用户列表
- `/api/users/{username}` - 用户详情
- `/api/users/{username}/password` - 修改密码
- `/api/users/{username}/profile` - 更新个人资料

## 二、前端页面结构

### 1. 认证页面
- `pages/dorm/auth/login.vue` - 登录页面
- `pages/dorm/auth/register.vue` - 注册页面
- `pages/dorm/auth/forgot-password.vue` - 忘记密码页面

### 2. 主页面
- `pages/dorm/index/index.vue` - 首页
- `pages/dorm/control/index.vue` - 设备控制中心

### 3. 设备管理
- `pages/dorm/device/list.vue` - 设备列表
- `pages/dorm/device/detail.vue` - 设备详情
- `pages/dorm/device/edit.vue` - 设备编辑

### 4. 遥测数据
- `pages/dorm/telemetry/index.vue` - 用电数据
- `pages/dorm/telemetry/statistics.vue` - 统计报表
- `pages/dorm/ai-report/index.vue` - AI分析报告

### 5. 断电控制
- `pages/dorm/power-control/index.vue` - 断电控制中心
- `pages/dorm/power-control/room-detail.vue` - 房间断电详情

### 6. 计费管理
- `pages/dorm/billing/index.vue` - 计费管理
- `pages/dorm/billing/bills.vue` - 账单列表
- `pages/dorm/billing/recharge.vue` - 充值页面
- `pages/dorm/billing/price-rules.vue` - 电价规则

### 7. 系统监控
- `pages/dorm/monitor/system.vue` - 系统监控
- `pages/dorm/monitor/devices.vue` - 设备监控
- `pages/dorm/monitor/api.vue` - API监控

### 8. 用户管理
- `pages/dorm/users/list.vue` - 用户列表
- `pages/dorm/users/detail.vue` - 用户详情
- `pages/dorm/users/edit.vue` - 用户编辑

### 9. 个人中心
- `pages/dorm/profile/index.vue` - 个人资料
- `pages/dorm/settings/index.vue` - 系统设置

## 三、技术栈

### 1. 前端框架
- Vue 3 + Vite
- uni-app (跨端开发)
- SCSS (样式)

### 2. 状态管理
- Vuex (状态管理)
- Pinia (可选)

### 3. HTTP请求
- axios (API调用)
- 拦截器 (认证、错误处理)

### 4. UI组件
- uni-ui (基础组件)
- 自定义组件 (未来主义风格)

### 5. 工具库
- dayjs (时间处理)
- lodash-es (工具函数)
- echarts (数据可视化)

## 四、开发计划

### 阶段一：基础架构
1. 创建项目结构
2. 配置路由
3. 实现认证系统
4. 搭建API请求框架

### 阶段二：核心功能
1. 首页和设备控制
2. 设备管理
3. 遥测数据
4. 断电控制

### 阶段三：高级功能
1. 计费管理
2. 系统监控
3. 用户管理
4. 个人中心

### 阶段四：测试与优化
1. API对接测试
2. 性能优化
3. 兼容性测试
4. UI细节调整

## 五、API对接策略

### 1. 认证流程
- 登录获取token
- token存储 (localStorage)
- 请求拦截器添加Authorization头
- 响应拦截器处理401错误
- 自动刷新token

### 2. 错误处理
- 统一错误处理
- 错误提示
- 网络状态检测
- 重试机制

### 3. 数据缓存
- 设备列表缓存
- 遥测数据缓存
- 账单数据缓存
- 缓存过期策略

### 4. 性能优化
- 防抖和节流
- 虚拟列表
- 懒加载
- 预加载

## 六、UI设计规范

### 1. 未来主义风格
- 深色背景 (#0A0E27)
- 青色发光 (#00F5FF)
- 全息投影效果
- 3D空间感
- 故障艺术效果

### 2. 响应式设计
- 移动端优先
- 适配不同屏幕
- 触摸友好
- 流畅动画

### 3. 交互体验
- 即时反馈
- 微动画
- 加载状态
- 错误提示

## 七、开发注意事项

### 1. 安全性
- XSS防护
- CSRF防护
- 密码加密
- 敏感信息保护

### 2. 可维护性
- 代码规范
- 组件复用
- 文档完善
- 测试覆盖

### 3. 性能
- 首屏加载速度
- 运行时性能
- 内存使用
- 网络请求优化

## 八、预期交付物

1. 完整的前端项目
2. 所有页面实现
3. API对接完成
4. 未来主义风格UI
5. 性能优化
6. 测试报告

---

**开发时间**: 预计 2-3 周
**团队成员**: 前端开发工程师
**技术支持**: 后端API团队