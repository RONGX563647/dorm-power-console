# 未来主义风格全局改造指南

## 改造日期
2026-03-02

---

## 一、设计理念

将整个应用从传统青绿主题升级为**未来主义科技风格**，融入以下核心元素：

### 1. 全息投影效果
- ✅ 动态网格背景（无限滚动动画）
- ✅ 顶部光晕效果（径向渐变）
- ✅ 全息圆环（多层旋转动画）
- ✅ 脉冲指示器（扩散动画）
- ✅ 扫描效果（缩放呼吸动画）

### 2. 数据可视化
- ✅ 全息功率圆盘（三层旋转环）
- ✅ 实时功率柱状图（发光效果）
- ✅ 数据流动画（平滑过渡）
- ✅ 性能指标展示（实时更新）

### 3. 3D空间感
- ✅ 深色背景渐变（#0A0E27 → #0F1629）
- ✅ 半透明叠加层（rgba透明度）
- ✅ 发光阴影效果（box-shadow）
- ✅ 毛玻璃边框（border + opacity）

### 4. 故障艺术效果
- ✅ 文字抖动动画（glitch-1, glitch-2）
- ✅ 色彩分离效果（RGB偏移）
- ✅ 扫描线效果（clip-path）
- ✅ 随机闪烁（opacity变化）

---

## 二、色彩系统

### 主色调
```scss
$cyber-primary: #00F5FF;    // 青色发光（全息效果）
$cyber-secondary: #00D4FF;  // 次青色
$cyber-accent: #FF00FF;     // 品红（故障效果）
$cyber-warning: #FF6B35;    // 橙色（警告）
$cyber-success: #00FF88;    // 绿色（成功）
$cyber-danger: #FF0055;     // 红色（危险）
```

### 背景色系
```scss
$bg-dark-primary: #0A0E27;   // 主背景
$bg-dark-secondary: #0F1629; // 渐变中间色
$bg-dark-tertiary: #1A1F3A;  // 浅色背景
```

### 文字色系
```scss
$text-primary: #00F5FF;              // 主文字
$text-secondary: rgba(0, 245, 255, 0.7);  // 次文字
$text-muted: rgba(0, 245, 255, 0.5);      // 辅助文字
```

### 透明度层级
```scss
$alpha-low: 0.03;     // 背景层
$alpha-medium: 0.08;  // 选中态
$alpha-high: 0.15;    // 光晕效果
$alpha-ultra: 0.25;   // 强调效果
```

---

## 三、动画系统

### 1. 网格滚动动画
```scss
@keyframes gridMove {
    0% { transform: translateY(0); }
    100% { transform: translateY(40rpx); }
}
// 应用：背景网格无限滚动
```

### 2. 脉冲动画
```scss
@keyframes pulse {
    0% { transform: scale(1); opacity: 1; }
    100% { transform: scale(1.8); opacity: 0; }
}
// 应用：状态指示器扩散效果
```

### 3. 旋转动画
```scss
@keyframes rotate {
    from { transform: rotate(0deg); }
    to { transform: rotate(360deg); }
}
// 应用：全息圆环旋转
```

### 4. 扫描动画
```scss
@keyframes scan {
    0%, 100% { transform: scale(1); opacity: 0.2; }
    50% { transform: scale(1.1); opacity: 0.4; }
}
// 应用：图标容器呼吸效果
```

### 5. 数据流动画
```scss
@keyframes dataFlow {
    0% { transform: translateX(-100%); }
    100% { transform: translateX(100%); }
}
// 应用：进度条流动效果
```

### 6. 故障艺术动画
```scss
@keyframes glitch-1 {
    0% { transform: translateX(0); }
    20% { transform: translateX(-2rpx); }
    40% { transform: translateX(2rpx); }
    60% { transform: translateX(-2rpx); }
    80% { transform: translateX(2rpx); }
    100% { transform: translateX(0); }
}

@keyframes glitch-2 {
    0% { transform: translateX(0); }
    20% { transform: translateX(2rpx); }
    40% { transform: translateX(-2rpx); }
    60% { transform: translateX(2rpx); }
    80% { transform: translateX(-2rpx); }
    100% { transform: translateX(0); }
}
// 应用：文字抖动效果
```

### 7. 边框发光动画
```scss
@keyframes borderGlow {
    0%, 100% { background-position: 0% 50%; }
    50% { background-position: 100% 50%; }
}
// 应用：渐变边框流动效果
```

---

## 四、核心组件样式

### 1. 全息容器
```scss
@mixin hologram-bg {
    position: relative;
    background: linear-gradient(180deg, $bg-dark-primary 0%, $bg-dark-secondary 50%, $bg-dark-primary 100%);
    
    &::before {
        // 网格背景
        background-image: 
            linear-gradient(rgba($cyber-primary, $alpha-low) 1px, transparent 1px),
            linear-gradient(90deg, rgba($cyber-primary, $alpha-low) 1px, transparent 1px);
        animation: gridMove 20s linear infinite;
    }
    
    &::after {
        // 顶部光晕
        background: radial-gradient(circle, rgba($cyber-primary, $alpha-high) 0%, transparent 70%);
    }
}
```

### 2. 全息卡片
```scss
@mixin hologram-card {
    background: rgba($cyber-primary, $alpha-low);
    border: 1rpx solid rgba($cyber-primary, 0.2);
    border-radius: 20rpx;
    box-shadow: $glow-sm;
    backdrop-filter: blur(10px);
    
    &:active {
        background: rgba($cyber-primary, $alpha-medium);
        box-shadow: $glow-md;
        transform: scale(0.98);
    }
}
```

### 3. 全息按钮
```scss
@mixin hologram-button {
    background: rgba($cyber-primary, $alpha-medium);
    border: 1rpx solid rgba($cyber-primary, 0.3);
    border-radius: 12rpx;
    color: $cyber-primary;
    font-weight: 600;
    letter-spacing: 2rpx;
    box-shadow: $glow-sm;
    
    &:active:not([disabled]) {
        background: rgba($cyber-primary, $alpha-high);
        box-shadow: $glow-md;
        transform: scale(0.95);
    }
}
```

### 4. 发光文字
```scss
@mixin glow-text {
    color: $cyber-primary;
    text-shadow: $glow-md;
}
```

### 5. 故障艺术效果
```scss
@mixin glitch-effect {
    position: relative;
    
    &::before,
    &::after {
        content: attr(data-text);
        position: absolute;
        top: 0;
        left: 0;
        width: 100%;
        height: 100%;
    }
    
    &::before {
        color: $cyber-accent;
        animation: glitch-1 2s infinite linear alternate-reverse;
        clip-path: polygon(0 0, 100% 0, 100% 35%, 0 35%);
    }
    
    &::after {
        color: $cyber-secondary;
        animation: glitch-2 3s infinite linear alternate-reverse;
        clip-path: polygon(0 65%, 100% 65%, 100% 100%, 0 100%);
    }
}
```

### 6. 全息边框
```scss
@mixin holo-border {
    position: relative;
    
    &::before {
        content: '';
        position: absolute;
        top: -2rpx;
        left: -2rpx;
        right: -2rpx;
        bottom: -2rpx;
        background: linear-gradient(
            45deg,
            $cyber-primary,
            $cyber-secondary,
            $cyber-accent,
            $cyber-primary
        );
        background-size: 400% 400%;
        border-radius: inherit;
        animation: borderGlow 3s ease infinite;
        z-index: -1;
    }
}
```

---

## 五、已改造页面

### ✅ 已完成
1. **控制中心** (`/pages/dorm/control/index.vue`)
   - 全息功率圆盘
   - 设备选择器
   - 插座控制卡片
   - 快捷操作按钮

2. **首页** (`/pages/dorm/index/index.vue`)
   - 用户头像（旋转环）
   - 功率监控（全息圆盘）
   - 设备控制中心入口
   - 节能监控卡片
   - 快捷功能网格

### 🚧 待改造
3. **登录/注册页面**
4. **宿舍列表页面**
5. **设备详情页面**
6. **告警列表页面**
7. **个人中心页面**
8. **其他页面**

---

## 六、改造步骤

### 步骤1：引入全局样式
```vue
<style lang="scss">
@import "@/styles/cyber.scss";
</style>
```

### 步骤2：替换容器类
```vue
<!-- 修改前 -->
<view class="container">

<!-- 修改后 -->
<view class="cyber-container">
    <view class="cyber-grid"></view>
    <view class="cyber-glow"></view>
    <!-- 内容 -->
</view>
```

### 步骤3：替换组件类
```vue
<!-- 修改前 -->
<view class="card">

<!-- 修改后 -->
<view class="cyber-card">
```

### 步骤4：替换颜色值
```scss
// 修改前
color: #2A7965;
background: #F2F7F5;

// 修改后
color: #00F5FF;
background: rgba(0, 245, 255, 0.03);
```

### 步骤5：添加动画效果
```vue
<view class="holo-circle">
    <view class="circle-ring ring-1"></view>
    <view class="circle-ring ring-2"></view>
    <view class="circle-ring ring-3"></view>
</view>
```

---

## 七、设计规范

### 1. 文字规范
- 标题：32-48rpx，font-weight: 600-700
- 正文：24-28rpx，font-weight: 400-500
- 辅助文字：18-22rpx，font-weight: 400
- 字间距：2-4rpx（英文大写）
- 行高：1.5-1.8

### 2. 间距规范
- 页面边距：32rpx
- 卡片间距：20-24rpx
- 元素间距：12-16rpx
- 内边距：20-32rpx

### 3. 圆角规范
- 大圆角：20-24rpx（卡片）
- 中圆角：12-16rpx（按钮）
- 小圆角：8-12rpx（标签）
- 圆形：50%（图标容器）

### 4. 阴影规范
```scss
$glow-sm: 0 0 10rpx rgba(0, 245, 255, 0.3);
$glow-md: 0 0 20rpx rgba(0, 245, 255, 0.5);
$glow-lg: 0 0 30rpx rgba(0, 245, 255, 0.7);
$glow-xl: 0 0 40rpx rgba(0, 245, 255, 0.9);
```

---

## 八、性能优化

### 1. 动画性能
```scss
// ✅ 推荐：使用 transform 和 opacity（GPU加速）
animation: rotate 3s linear infinite;

// ❌ 避免：使用 width/height/left/top
// animation: widthChange 3s linear infinite;
```

### 2. 透明度优化
```scss
// ✅ 推荐：使用 rgba
background: rgba(0, 245, 255, 0.03);

// ❌ 避免：使用 opacity
// opacity: 0.03;
```

### 3. 滤镜优化
```scss
// ✅ 推荐：适度使用 blur
filter: blur(10rpx);

// ❌ 避免：过度使用 blur
// filter: blur(50rpx);
```

---

## 九、兼容性说明

### 支持平台
- ✅ 微信小程序
- ✅ H5
- ✅ App（iOS/Android）

### 注意事项
1. `backdrop-filter` 在部分安卓机型不支持
2. `filter: blur()` 性能消耗较大，谨慎使用
3. 复杂动画可能导致低端机卡顿
4. 建议使用 `will-change` 优化动画性能

---

## 十、总结

### 改造成果
- ✅ 创建了全局未来主义样式文件
- ✅ 完成了控制中心和首页的改造
- ✅ 建立了完整的设计规范
- ✅ 提供了可复用的组件样式

### 后续工作
- 🚧 继续改造其他页面
- 🚧 优化动画性能
- 🚧 测试跨平台兼容性
- 🚧 收集用户反馈

---

**文档版本**: v1.0  
**最后更新**: 2026-03-02  
**维护团队**: UI/UX设计团队
