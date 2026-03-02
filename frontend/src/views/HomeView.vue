<template>
  <div class="home-view" @mousemove="handleMouseMove">
    <div class="cyber-bg">
      <div class="gradient-orb orb-1"></div>
      <div class="gradient-orb orb-2"></div>
      <div class="gradient-orb orb-3"></div>
      <canvas ref="particleCanvas" class="particle-canvas"></canvas>
      <div class="grid-overlay"></div>
    </div>

    <header class="home-header">
      <div class="header-content">
        <div class="logo-section">
          <div class="hologram-logo">
            <div class="hologram-rings">
              <div class="ring ring-1"></div>
              <div class="ring ring-2"></div>
              <div class="ring ring-3"></div>
            </div>
            <ThunderboltOutlined class="logo-icon" />
          </div>
          <div class="logo-text">
            <h1 class="neon-text" data-text="智慧宿舍电力管理平台">
              智慧宿舍电力管理平台
            </h1>
            <div class="terminal-line">
              <span class="prompt">$</span>
              <span class="command">{{ terminalCommand }}</span>
              <span class="cursor">_</span>
            </div>
          </div>
        </div>
        <div class="header-actions">
          <button class="cyber-button" @click="goToLogin">
            <span class="button-content">
              <span class="button-text">进入系统</span>
              <span class="button-icon"><RightOutlined /></span>
            </span>
            <span class="button-glitch"></span>
          </button>
        </div>
      </div>
    </header>

    <main class="home-main">
      <section class="hero-section">
        <div class="hero-content">
          <div class="hero-left">
            <div class="hero-badge">
              <span class="badge-dot"></span>
              <span>NEXT-GEN IOT PLATFORM</span>
            </div>
            
            <h2 class="hero-title">
              <span class="title-line">
                <span class="word" v-for="(word, i) in titleWords1" :key="i" :style="{ animationDelay: `${i * 0.1}s` }">
                  {{ word }}
                </span>
              </span>
              <span class="title-line">
                <span class="word highlight" v-for="(word, i) in titleWords2" :key="i" :style="{ animationDelay: `${(i + 3) * 0.1}s` }">
                  {{ word }}
                </span>
              </span>
            </h2>

            <div class="tech-pills">
              <div class="pill" v-for="tech in heroTechs" :key="tech.name">
                <span class="pill-icon"><component :is="tech.icon" /></span>
                <span class="pill-text">{{ tech.name }}</span>
                <span class="pill-dot"></span>
              </div>
            </div>

            <div class="code-window">
              <div class="window-header">
                <div class="window-controls">
                  <span class="control close"></span>
                  <span class="control minimize"></span>
                  <span class="control maximize"></span>
                </div>
                <span class="window-title">system.config.ts</span>
                <div class="window-actions">
                  <span class="action-copy">复制</span>
                </div>
              </div>
              <div class="window-body">
                <pre class="code-block"><code><span class="line-number">1</span>  <span class="keyword">export</span> <span class="keyword">const</span> <span class="variable">SystemConfig</span> = {
<span class="line-number">2</span>    <span class="comment">// 核心架构</span>
<span class="line-number">3</span>    <span class="property">architecture</span>: {
<span class="line-number">4</span>      <span class="property">backend</span>: <span class="string">'Spring Boot 3.2'</span>,
<span class="line-number">5</span>      <span class="property">frontend</span>: <span class="string">'Vue 3.4 + TypeScript'</span>,
<span class="line-number">6</span>      <span class="property">database</span>: <span class="string">'TimescaleDB + Redis'</span>,
<span class="line-number">7</span>      <span class="property">ai</span>: <span class="string">'TensorFlow 2.15'</span>
<span class="line-number">8</span>    },
<span class="line-number">9</span>    
<span class="line-number">10</span>   <span class="comment">// 性能指标</span>
<span class="line-number">11</span>   <span class="property">performance</span>: {
<span class="line-number">12</span>     <span class="property">latency</span>: <span class="number">'&lt;50ms'</span>,
<span class="line-number">13</span>     <span class="property">throughput</span>: <span class="number">'50K+/s'</span>,
<span class="line-number">14</span>     <span class="property">uptime</span>: <span class="number">'99.99%'</span>
<span class="line-number">15</span>   }
<span class="line-number">16</span> };</code></pre>
              </div>
            </div>
          </div>

          <div class="hero-right">
            <div class="visual-container" ref="visualContainer">
              <div class="hologram-effect">
                <img src="/static/image.png" alt="System" class="system-image" />
                <div class="hologram-overlay"></div>
                <div class="scan-effect"></div>
              </div>
              
              <div class="floating-tech">
                <div class="tech-node node-1" @click="showTechInfo('TimescaleDB')">
                  <div class="node-pulse"></div>
                  <DatabaseOutlined />
                  <span>TimescaleDB</span>
                </div>
                <div class="tech-node node-2" @click="showTechInfo('WebSocket')">
                  <div class="node-pulse"></div>
                  <ThunderboltOutlined />
                  <span>WebSocket</span>
                </div>
                <div class="tech-node node-3" @click="showTechInfo('AI Engine')">
                  <div class="node-pulse"></div>
                  <RobotOutlined />
                  <span>AI Engine</span>
                </div>
                <div class="tech-node node-4" @click="showTechInfo('Redis')">
                  <div class="node-pulse"></div>
                  <ApiOutlined />
                  <span>Redis Cache</span>
                </div>
              </div>

              <div class="data-streams">
                <svg class="stream-svg" viewBox="0 0 400 300">
                  <path class="data-path path-1" d="M50,150 Q150,50 250,150 T450,150" />
                  <path class="data-path path-2" d="M50,100 Q150,200 250,100 T450,100" />
                  <path class="data-path path-3" d="M50,200 Q150,100 250,200 T450,200" />
                  <circle class="data-dot dot-1" r="4">
                    <animateMotion dur="3s" repeatCount="indefinite" path="M50,150 Q150,50 250,150 T450,150" />
                  </circle>
                  <circle class="data-dot dot-2" r="4">
                    <animateMotion dur="4s" repeatCount="indefinite" path="M50,100 Q150,200 250,100 T450,100" />
                  </circle>
                  <circle class="data-dot dot-3" r="4">
                    <animateMotion dur="5s" repeatCount="indefinite" path="M50,200 Q150,100 250,200 T450,200" />
                  </circle>
                </svg>
              </div>
            </div>
          </div>
        </div>
      </section>

      <section class="tech-stack-section">
        <div class="section-header">
          <div class="header-line">
            <div class="line-segment"></div>
            <div class="line-dot"></div>
            <div class="line-segment"></div>
          </div>
          <h3 class="section-title">
            <span class="title-text">TECH STACK</span>
            <span class="title-number">01</span>
          </h3>
          <p class="section-subtitle">构建下一代智慧能源管理系统的核心技术</p>
        </div>

        <div class="tech-grid">
          <div class="tech-card-3d" v-for="(tech, index) in techStack" :key="index" :style="{ '--delay': `${index * 0.1}s` }">
            <div class="card-inner">
              <div class="card-front">
                <div class="card-gradient" :style="{ background: tech.gradient }"></div>
                <div class="card-content">
                  <div class="tech-icon-3d">
                    <component :is="tech.icon" />
                  </div>
                  <h4>{{ tech.title }}</h4>
                  <p>{{ tech.description }}</p>
                  <div class="tech-stats">
                    <div class="stat-item">
                      <span class="stat-label">{{ tech.metric.label }}</span>
                      <span class="stat-value">{{ tech.metric.value }}</span>
                    </div>
                  </div>
                </div>
              </div>
              <div class="card-back">
                <div class="back-content">
                  <h5>技术标签</h5>
                  <div class="tech-tags-3d">
                    <span class="tag-3d" v-for="tag in tech.tags" :key="tag">{{ tag }}</span>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </section>

      <section class="features-section">
        <div class="section-header">
          <div class="header-line">
            <div class="line-segment"></div>
            <div class="line-dot"></div>
            <div class="line-segment"></div>
          </div>
          <h3 class="section-title">
            <span class="title-text">CORE FEATURES</span>
            <span class="title-number">02</span>
          </h3>
          <p class="section-subtitle">从数据采集到智能决策的完整技术链路</p>
        </div>

        <div class="features-showcase">
          <div class="feature-card-hologram" v-for="(feature, index) in features" :key="index">
            <div class="hologram-border"></div>
            <div class="feature-icon-holo">
              <component :is="feature.icon" />
              <div class="icon-glow"></div>
            </div>
            <h4>{{ feature.title }}</h4>
            <p>{{ feature.description }}</p>
            <div class="feature-metric">
              <div class="metric-bar">
                <div class="bar-fill" :style="{ width: feature.progress }"></div>
              </div>
              <div class="metric-info">
                <span class="metric-label">{{ feature.metric.label }}</span>
                <span class="metric-value">{{ feature.metric.value }}</span>
              </div>
            </div>
            <div class="feature-number">{{ String(index + 1).padStart(2, '0') }}</div>
          </div>
        </div>
      </section>

      <section class="innovation-section">
        <div class="section-header">
          <div class="header-line">
            <div class="line-segment"></div>
            <div class="line-dot"></div>
            <div class="line-segment"></div>
          </div>
          <h3 class="section-title">
            <span class="title-text">INNOVATION</span>
            <span class="title-number">03</span>
          </h3>
          <p class="section-subtitle">探索前沿技术在能源管理领域的应用</p>
        </div>

        <div class="innovation-grid">
          <div class="innovation-card-cyber" v-for="(item, index) in innovations" :key="index">
            <div class="cyber-lines">
              <div class="cyber-line line-top"></div>
              <div class="cyber-line line-right"></div>
              <div class="cyber-line line-bottom"></div>
              <div class="cyber-line line-left"></div>
            </div>
            <div class="innovation-icon-cyber">
              <component :is="item.icon" />
            </div>
            <h4>{{ item.title }}</h4>
            <p>{{ item.description }}</p>
            <div class="cyber-badge">{{ item.badge }}</div>
            <div class="cyber-corner corner-tl"></div>
            <div class="cyber-corner corner-tr"></div>
            <div class="cyber-corner corner-bl"></div>
            <div class="cyber-corner corner-br"></div>
          </div>
        </div>
      </section>

      <section class="opensource-section">
        <div class="opensource-container">
          <div class="code-matrix">
            <div class="matrix-column" v-for="i in 20" :key="i">
              <div class="matrix-char" v-for="j in 30" :key="j" :style="{ animationDelay: `${Math.random() * 5}s` }">
                {{ getRandomChar() }}
              </div>
            </div>
          </div>
          
          <div class="opensource-content">
            <div class="github-icon-cyber">
              <GithubOutlined />
              <div class="icon-rings">
                <div class="ring-outer"></div>
                <div class="ring-inner"></div>
              </div>
            </div>
            <h3>OPEN SOURCE</h3>
            <p>我们相信技术的力量，致力于推动智慧能源管理的开源生态。核心代码遵循 MIT 协议开源。</p>
            
            <div class="stats-showcase">
              <div class="stat-card" v-for="stat in openSourceStats" :key="stat.label">
                <div class="stat-value">{{ stat.value }}</div>
                <div class="stat-label">{{ stat.label }}</div>
                <div class="stat-bar"></div>
              </div>
            </div>

            <div class="action-buttons-cyber">
              <button class="cyber-button" @click="showDocs">
                <span class="button-content">
                  <BookOutlined />
                  <span>查看文档</span>
                </span>
              </button>
              <button class="cyber-button outline" @click="showGithub">
                <span class="button-content">
                  <GithubOutlined />
                  <span>GitHub</span>
                </span>
              </button>
            </div>
          </div>
        </div>
      </section>

      <section class="cta-section">
        <div class="cta-container">
          <div class="cta-bg-animation">
            <div class="wave wave-1"></div>
            <div class="wave wave-2"></div>
            <div class="wave wave-3"></div>
          </div>
          <div class="cta-content">
            <h3 class="cta-title">
              <span class="title-glitch" data-text="开始探索">开始探索</span>
            </h3>
            <p class="cta-subtitle">体验技术驱动的智慧能源管理</p>
            <div class="cta-buttons">
              <button class="cyber-button large" @click="goToLogin">
                <span class="button-content">
                  <span>进入系统</span>
                  <RightOutlined />
                </span>
              </button>
            </div>
          </div>
        </div>
      </section>
    </main>

    <footer class="home-footer">
      <div class="footer-content">
        <div class="footer-brand">
          <ThunderboltOutlined class="footer-icon" />
          <span>Dorm Power</span>
        </div>
        <p>© 2024 Built with ❤️ by Tech Enthusiasts. Powered by Cutting-Edge Technology.</p>
        <div class="footer-links">
          <a href="#">技术博客</a>
          <a href="#">开发文档</a>
          <a href="#">贡献指南</a>
          <a href="#">问题反馈</a>
        </div>
      </div>
    </footer>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import {
  ThunderboltOutlined,
  RightOutlined,
  ApiOutlined,
  DatabaseOutlined,
  RobotOutlined,
  MonitorOutlined,
  SafetyCertificateOutlined,
  LineChartOutlined,
  ControlOutlined,
  ExperimentOutlined,
  GithubOutlined,
  BookOutlined,
  CloudOutlined,
  CodeOutlined,
  RocketOutlined
} from '@ant-design/icons-vue'

const router = useRouter()
const particleCanvas = ref<HTMLCanvasElement | null>(null)
const visualContainer = ref<HTMLElement | null>(null)

const titleWords1 = ['用', '技术', '重新', '定义']
const titleWords2 = ['电力', '管理', '系统']

const terminalCommand = ref('initializing system...')
const commands = [
  'loading modules...',
  'connecting to database...',
  'starting AI engine...',
  'system ready ✓'
]

const heroTechs = [
  { name: '微服务', icon: ApiOutlined },
  { name: '时序数据库', icon: DatabaseOutlined },
  { name: '实时流', icon: ThunderboltOutlined },
  { name: '机器学习', icon: RobotOutlined }
]

const techStack = [
  {
    title: '微服务架构',
    description: 'Spring Cloud 微服务架构，实现服务独立部署与扩展',
    icon: ApiOutlined,
    gradient: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
    tags: ['Spring Boot', 'Spring Cloud', 'Docker', 'K8s'],
    metric: { label: '服务数量', value: '12+' }
  },
  {
    title: '时序数据存储',
    description: 'TimescaleDB 高性能时序数据库，毫秒级查询响应',
    icon: DatabaseOutlined,
    gradient: 'linear-gradient(135deg, #f093fb 0%, #f5576c 100%)',
    tags: ['TimescaleDB', 'PostgreSQL', 'Redis', 'ClickHouse'],
    metric: { label: '写入性能', value: '50K+/s' }
  },
  {
    title: '实时数据流',
    description: 'WebSocket 全双工通信，实时推送设备状态',
    icon: ThunderboltOutlined,
    gradient: 'linear-gradient(135deg, #4facfe 0%, #00f2fe 100%)',
    tags: ['WebSocket', 'RabbitMQ', 'Kafka', 'gRPC'],
    metric: { label: '数据延迟', value: '<50ms' }
  },
  {
    title: 'AI 智能分析',
    description: '机器学习模型，实现用电行为预测与异常检测',
    icon: RobotOutlined,
    gradient: 'linear-gradient(135deg, #43e97b 0%, #38f9d7 100%)',
    tags: ['TensorFlow', 'PyTorch', 'Scikit-learn', 'ONNX'],
    metric: { label: '预测精度', value: '99.2%' }
  }
]

const features = [
  {
    title: '实时监控',
    description: 'WebSocket 长连接实现毫秒级数据推送',
    icon: MonitorOutlined,
    progress: '95%',
    metric: { label: '数据延迟', value: '<50ms' }
  },
  {
    title: '异常检测',
    description: '基于深度学习的实时异常识别系统',
    icon: SafetyCertificateOutlined,
    progress: '98%',
    metric: { label: '检测准确率', value: '99.2%' }
  },
  {
    title: '数据分析',
    description: '时序数据库高效存储与多维度分析',
    icon: LineChartOutlined,
    progress: '92%',
    metric: { label: '查询性能', value: '50M+/s' }
  },
  {
    title: '远程控制',
    description: 'MQTT 协议实现设备双向通信',
    icon: ControlOutlined,
    progress: '90%',
    metric: { label: '响应时间', value: '<100ms' }
  },
  {
    title: 'AI 预测',
    description: 'LSTM 神经网络预测用电趋势',
    icon: ExperimentOutlined,
    progress: '96%',
    metric: { label: '预测精度', value: 'MAPE <5%' }
  },
  {
    title: '开放 API',
    description: 'RESTful API 设计，支持第三方集成',
    icon: ApiOutlined,
    progress: '100%',
    metric: { label: 'API 数量', value: '250+' }
  }
]

const innovations = [
  {
    title: '边缘计算',
    description: '设备端预处理和实时响应',
    icon: CloudOutlined,
    badge: 'EDGE COMPUTING'
  },
  {
    title: '数字孪生',
    description: '构建虚拟仿真模型',
    icon: CodeOutlined,
    badge: 'DIGITAL TWIN'
  },
  {
    title: '联邦学习',
    description: '保护隐私的协同学习',
    icon: RocketOutlined,
    badge: 'FEDERATED ML'
  },
  {
    title: '区块链存证',
    description: '关键操作记录上链',
    icon: SafetyCertificateOutlined,
    badge: 'BLOCKCHAIN'
  }
]

const openSourceStats = [
  { value: '50K+', label: 'Lines of Code' },
  { value: '250+', label: 'API Endpoints' },
  { value: '35+', label: 'Components' },
  { value: 'MIT', label: 'License' }
]

let commandIndex = 0
let commandInterval: number | null = null
let particleAnimationId: number | null = null
let mouseX = 0
let mouseY = 0

onMounted(() => {
  initParticles()
  startTerminalAnimation()
})

onBeforeUnmount(() => {
  if (commandInterval) clearInterval(commandInterval)
  if (particleAnimationId) cancelAnimationFrame(particleAnimationId)
})

const initParticles = () => {
  if (!particleCanvas.value) return
  
  const canvas = particleCanvas.value
  const ctx = canvas.getContext('2d')
  if (!ctx) return

  canvas.width = window.innerWidth
  canvas.height = window.innerHeight

  const particles: any[] = []
  const particleCount = 100

  for (let i = 0; i < particleCount; i++) {
    particles.push({
      x: Math.random() * canvas.width,
      y: Math.random() * canvas.height,
      vx: (Math.random() - 0.5) * 0.5,
      vy: (Math.random() - 0.5) * 0.5,
      size: Math.random() * 2 + 1
    })
  }

  const animate = () => {
    ctx.clearRect(0, 0, canvas.width, canvas.height)
    
    particles.forEach((p, i) => {
      p.x += p.vx
      p.y += p.vy

      if (p.x < 0 || p.x > canvas.width) p.vx *= -1
      if (p.y < 0 || p.y > canvas.height) p.vy *= -1

      ctx.beginPath()
      ctx.arc(p.x, p.y, p.size, 0, Math.PI * 2)
      ctx.fillStyle = 'rgba(42, 121, 101, 0.5)'
      ctx.fill()

      particles.forEach((p2, j) => {
        if (i === j) return
        const dx = p.x - p2.x
        const dy = p.y - p2.y
        const dist = Math.sqrt(dx * dx + dy * dy)

        if (dist < 150) {
          ctx.beginPath()
          ctx.moveTo(p.x, p.y)
          ctx.lineTo(p2.x, p2.y)
          ctx.strokeStyle = `rgba(42, 121, 101, ${0.2 * (1 - dist / 150)})`
          ctx.stroke()
        }
      })
    })

    particleAnimationId = requestAnimationFrame(animate)
  }

  animate()
}

const startTerminalAnimation = () => {
  commandInterval = window.setInterval(() => {
    commandIndex = (commandIndex + 1) % commands.length
    terminalCommand.value = commands[commandIndex]
  }, 2000)
}

const handleMouseMove = (e: MouseEvent) => {
  mouseX = e.clientX
  mouseY = e.clientY
}

const getRandomChar = () => {
  const chars = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789@#$%^&*'
  return chars[Math.floor(Math.random() * chars.length)]
}

const showTechInfo = (tech: string) => {
  message.info(`${tech} - 点击查看详细信息`)
}

const goToLogin = () => {
  router.push('/login')
}

const showDocs = () => {
  message.info('技术文档正在完善中...')
}

const showGithub = () => {
  message.info('GitHub 仓库即将开放...')
}
</script>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=JetBrains+Mono:wght@400;500;600;700&family=Orbitron:wght@400;500;600;700;800;900&display=swap');

.home-view {
  min-height: 100vh;
  background: #000000;
  position: relative;
  overflow-x: hidden;
  font-family: 'JetBrains Mono', monospace;
}

.cyber-bg {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  pointer-events: none;
  z-index: 0;
}

.gradient-orb {
  position: absolute;
  border-radius: 50%;
  filter: blur(80px);
  opacity: 0.3;
  animation: orbFloat 20s ease-in-out infinite;
}

.orb-1 {
  width: 600px;
  height: 600px;
  background: radial-gradient(circle, #667eea 0%, transparent 70%);
  top: -200px;
  left: -200px;
}

.orb-2 {
  width: 500px;
  height: 500px;
  background: radial-gradient(circle, #f093fb 0%, transparent 70%);
  bottom: -150px;
  right: -150px;
  animation-delay: -10s;
}

.orb-3 {
  width: 400px;
  height: 400px;
  background: radial-gradient(circle, #4facfe 0%, transparent 70%);
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  animation-delay: -5s;
}

@keyframes orbFloat {
  0%, 100% { transform: translate(0, 0) scale(1); }
  33% { transform: translate(50px, -50px) scale(1.1); }
  66% { transform: translate(-50px, 50px) scale(0.9); }
}

.particle-canvas {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
}

.grid-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-image: 
    linear-gradient(rgba(42, 121, 101, 0.03) 1px, transparent 1px),
    linear-gradient(90deg, rgba(42, 121, 101, 0.03) 1px, transparent 1px);
  background-size: 100px 100px;
  animation: gridPulse 4s ease-in-out infinite;
}

@keyframes gridPulse {
  0%, 100% { opacity: 0.5; }
  50% { opacity: 1; }
}

.home-header {
  position: sticky;
  top: 0;
  z-index: 100;
  background: rgba(0, 0, 0, 0.7);
  backdrop-filter: blur(30px);
  border-bottom: 1px solid rgba(42, 121, 101, 0.3);
}

.header-content {
  max-width: 1600px;
  margin: 0 auto;
  padding: 1.5rem 3rem;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.logo-section {
  display: flex;
  align-items: center;
  gap: 1.5rem;
}

.hologram-logo {
  width: 60px;
  height: 60px;
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
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
  width: 80%;
  height: 80%;
  top: 10%;
  left: 10%;
  animation-direction: reverse;
  animation-duration: 8s;
}

.ring-3 {
  width: 60%;
  height: 60%;
  top: 20%;
  left: 20%;
  animation-duration: 6s;
}

@keyframes ringRotate {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

.logo-icon {
  font-size: 28px;
  color: #2A7965;
  z-index: 1;
  filter: drop-shadow(0 0 10px rgba(42, 121, 101, 0.8));
}

.logo-text h1 {
  margin: 0;
  font-size: 20px;
  font-weight: 700;
  font-family: 'Orbitron', sans-serif;
  letter-spacing: 2px;
}

.neon-text {
  color: #fff;
  text-shadow: 
    0 0 5px #2A7965,
    0 0 10px #2A7965,
    0 0 20px #2A7965,
    0 0 40px #2A7965;
  animation: neonPulse 2s ease-in-out infinite;
}

@keyframes neonPulse {
  0%, 100% { 
    text-shadow: 
      0 0 5px #2A7965,
      0 0 10px #2A7965,
      0 0 20px #2A7965,
      0 0 40px #2A7965;
  }
  50% { 
    text-shadow: 
      0 0 10px #2A7965,
      0 0 20px #2A7965,
      0 0 40px #2A7965,
      0 0 80px #2A7965;
  }
}

.terminal-line {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  margin-top: 0.5rem;
  font-size: 12px;
  color: #2A7965;
}

.prompt {
  color: #4facfe;
  font-weight: 700;
}

.cursor {
  animation: cursorBlink 1s step-end infinite;
}

@keyframes cursorBlink {
  0%, 100% { opacity: 1; }
  50% { opacity: 0; }
}

.cyber-button {
  position: relative;
  background: transparent;
  border: 2px solid #2A7965;
  color: #2A7965;
  padding: 0.75rem 2rem;
  font-size: 14px;
  font-weight: 600;
  font-family: 'Orbitron', sans-serif;
  letter-spacing: 1px;
  cursor: pointer;
  overflow: hidden;
  transition: all 0.3s ease;
  clip-path: polygon(
    0 0,
    calc(100% - 10px) 0,
    100% 10px,
    100% 100%,
    10px 100%,
    0 calc(100% - 10px)
  );
}

.cyber-button::before {
  content: '';
  position: absolute;
  top: 0;
  left: -100%;
  width: 100%;
  height: 100%;
  background: linear-gradient(90deg, transparent, rgba(42, 121, 101, 0.3), transparent);
  transition: left 0.5s ease;
}

.cyber-button:hover::before {
  left: 100%;
}

.cyber-button:hover {
  background: rgba(42, 121, 101, 0.1);
  box-shadow: 
    0 0 20px rgba(42, 121, 101, 0.5),
    inset 0 0 20px rgba(42, 121, 101, 0.1);
}

.button-content {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  position: relative;
  z-index: 1;
}

.button-glitch {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: #2A7965;
  opacity: 0;
}

.cyber-button:hover .button-glitch {
  animation: buttonGlitch 0.3s ease;
}

@keyframes buttonGlitch {
  0%, 100% { opacity: 0; transform: translate(0); }
  20% { opacity: 0.8; transform: translate(-5px, 5px); }
  40% { opacity: 0.8; transform: translate(5px, -5px); }
  60% { opacity: 0.8; transform: translate(-5px, -5px); }
  80% { opacity: 0.8; transform: translate(5px, 5px); }
}

.cyber-button.outline {
  background: transparent;
}

.cyber-button.large {
  padding: 1rem 3rem;
  font-size: 16px;
}

.home-main {
  position: relative;
  z-index: 1;
}

.hero-section {
  padding: 6rem 3rem;
  max-width: 1600px;
  margin: 0 auto;
}

.hero-content {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 6rem;
  align-items: center;
}

.hero-badge {
  display: inline-flex;
  align-items: center;
  gap: 0.75rem;
  padding: 0.5rem 1.5rem;
  background: rgba(42, 121, 101, 0.1);
  border: 1px solid rgba(42, 121, 101, 0.3);
  border-radius: 50px;
  font-size: 11px;
  font-weight: 600;
  letter-spacing: 2px;
  color: #2A7965;
  margin-bottom: 2rem;
}

.badge-dot {
  width: 8px;
  height: 8px;
  background: #2A7965;
  border-radius: 50%;
  animation: badgePulse 2s ease-in-out infinite;
}

@keyframes badgePulse {
  0%, 100% { transform: scale(1); opacity: 1; }
  50% { transform: scale(1.5); opacity: 0.5; }
}

.hero-title {
  font-size: 4rem;
  font-weight: 900;
  font-family: 'Orbitron', sans-serif;
  line-height: 1.2;
  margin-bottom: 2rem;
}

.title-line {
  display: block;
  margin-bottom: 0.5rem;
}

.word {
  display: inline-block;
  color: #fff;
  animation: wordFadeIn 0.8s ease forwards;
  opacity: 0;
  transform: translateY(30px);
}

.word.highlight {
  background: linear-gradient(135deg, #2A7965 0%, #4facfe 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

@keyframes wordFadeIn {
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.tech-pills {
  display: flex;
  flex-wrap: wrap;
  gap: 1rem;
  margin-bottom: 3rem;
}

.pill {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.5rem 1rem;
  background: rgba(42, 121, 101, 0.05);
  border: 1px solid rgba(42, 121, 101, 0.2);
  border-radius: 8px;
  font-size: 13px;
  color: #fff;
  transition: all 0.3s ease;
  cursor: pointer;
}

.pill:hover {
  background: rgba(42, 121, 101, 0.1);
  border-color: #2A7965;
  transform: translateY(-2px);
}

.pill-icon {
  font-size: 16px;
  color: #2A7965;
}

.pill-dot {
  width: 4px;
  height: 4px;
  background: #2A7965;
  border-radius: 50%;
}

.code-window {
  background: rgba(10, 10, 10, 0.8);
  border: 1px solid rgba(42, 121, 101, 0.3);
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 
    0 20px 60px rgba(0, 0, 0, 0.5),
    0 0 1px rgba(42, 121, 101, 0.5);
}

.window-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0.75rem 1rem;
  background: rgba(42, 121, 101, 0.05);
  border-bottom: 1px solid rgba(42, 121, 101, 0.2);
}

.window-controls {
  display: flex;
  gap: 0.5rem;
}

.control {
  width: 12px;
  height: 12px;
  border-radius: 50%;
}

.control.close { background: #ff5f56; }
.control.minimize { background: #ffbd2e; }
.control.maximize { background: #27ca40; }

.window-title {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.6);
}

.window-actions {
  font-size: 11px;
  color: #2A7965;
  cursor: pointer;
}

.window-body {
  padding: 1.5rem;
}

.code-block {
  margin: 0;
  font-size: 13px;
  line-height: 1.8;
  color: rgba(255, 255, 255, 0.9);
}

.line-number {
  display: inline-block;
  width: 30px;
  color: rgba(255, 255, 255, 0.3);
  text-align: right;
  margin-right: 1rem;
}

.keyword { color: #c792ea; }
.variable { color: #82aaff; }
.property { color: #c3e88d; }
.string { color: #c3e88d; }
.number { color: #f78c6c; }
.comment { color: rgba(255, 255, 255, 0.4); font-style: italic; }

.hero-right {
  position: relative;
}

.visual-container {
  position: relative;
}

.hologram-effect {
  position: relative;
  border-radius: 20px;
  overflow: hidden;
  box-shadow: 
    0 0 50px rgba(42, 121, 101, 0.3),
    inset 0 0 50px rgba(42, 121, 101, 0.1);
}

.system-image {
  width: 100%;
  height: auto;
  display: block;
  filter: brightness(1.1) contrast(1.1);
}

.hologram-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: linear-gradient(
    135deg,
    rgba(42, 121, 101, 0.1) 0%,
    transparent 50%,
    rgba(79, 172, 254, 0.1) 100%
  );
  pointer-events: none;
}

.scan-effect {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 2px;
  background: linear-gradient(90deg, transparent, #2A7965, transparent);
  animation: scanLine 4s linear infinite;
  box-shadow: 0 0 20px #2A7965;
}

@keyframes scanLine {
  0% { top: 0; opacity: 1; }
  100% { top: 100%; opacity: 0; }
}

.floating-tech {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  pointer-events: none;
}

.tech-node {
  position: absolute;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.5rem;
  padding: 0.75rem 1rem;
  background: rgba(0, 0, 0, 0.8);
  backdrop-filter: blur(10px);
  border: 1px solid rgba(42, 121, 101, 0.5);
  border-radius: 12px;
  color: #fff;
  font-size: 12px;
  cursor: pointer;
  pointer-events: auto;
  transition: all 0.3s ease;
  animation: nodeFloat 6s ease-in-out infinite;
}

.tech-node:hover {
  transform: scale(1.1);
  border-color: #2A7965;
  box-shadow: 0 0 30px rgba(42, 121, 101, 0.5);
}

.node-pulse {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  width: 100%;
  height: 100%;
  border: 2px solid #2A7965;
  border-radius: 12px;
  animation: nodePulse 2s ease-out infinite;
}

@keyframes nodePulse {
  0% { transform: translate(-50%, -50%) scale(1); opacity: 1; }
  100% { transform: translate(-50%, -50%) scale(1.5); opacity: 0; }
}

.node-1 { top: 5%; right: 5%; animation-delay: 0s; }
.node-2 { top: 40%; left: -10%; animation-delay: 1.5s; }
.node-3 { bottom: 15%; right: -5%; animation-delay: 3s; }
.node-4 { bottom: 5%; left: 10%; animation-delay: 4.5s; }

@keyframes nodeFloat {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-15px); }
}

.data-streams {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  pointer-events: none;
  opacity: 0.3;
}

.stream-svg {
  width: 100%;
  height: 100%;
}

.data-path {
  fill: none;
  stroke: #2A7965;
  stroke-width: 2;
  stroke-dasharray: 5, 5;
  animation: dashMove 2s linear infinite;
}

@keyframes dashMove {
  to { stroke-dashoffset: -20; }
}

.data-dot {
  fill: #2A7965;
  filter: drop-shadow(0 0 5px #2A7965);
}

.section-header {
  text-align: center;
  margin-bottom: 4rem;
}

.header-line {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 1rem;
  margin-bottom: 1.5rem;
}

.line-segment {
  width: 80px;
  height: 1px;
  background: linear-gradient(90deg, transparent, #2A7965, transparent);
}

.line-dot {
  width: 10px;
  height: 10px;
  background: #2A7965;
  border-radius: 50%;
  box-shadow: 0 0 20px #2A7965;
  animation: dotPulse 2s ease-in-out infinite;
}

@keyframes dotPulse {
  0%, 100% { transform: scale(1); }
  50% { transform: scale(1.3); }
}

.section-title {
  font-size: 3rem;
  font-weight: 900;
  font-family: 'Orbitron', sans-serif;
  color: #fff;
  margin: 0 0 1rem 0;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 1rem;
}

.title-number {
  font-size: 1rem;
  color: #2A7965;
  opacity: 0.5;
}

.section-subtitle {
  font-size: 1.1rem;
  color: rgba(255, 255, 255, 0.6);
  margin: 0;
}

.tech-stack-section {
  padding: 6rem 3rem;
  max-width: 1600px;
  margin: 0 auto;
}

.tech-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 2rem;
}

.tech-card-3d {
  perspective: 1000px;
  height: 400px;
}

.card-inner {
  position: relative;
  width: 100%;
  height: 100%;
  transition: transform 0.8s;
  transform-style: preserve-3d;
  animation: cardFadeIn 0.8s ease forwards;
  animation-delay: var(--delay);
  opacity: 0;
}

.tech-card-3d:hover .card-inner {
  transform: rotateY(180deg);
}

@keyframes cardFadeIn {
  to { opacity: 1; }
}

.card-front,
.card-back {
  position: absolute;
  width: 100%;
  height: 100%;
  backface-visibility: hidden;
  border-radius: 20px;
  overflow: hidden;
}

.card-front {
  background: rgba(10, 10, 10, 0.6);
  border: 1px solid rgba(42, 121, 101, 0.3);
  backdrop-filter: blur(10px);
}

.card-gradient {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 150px;
  opacity: 0.1;
}

.card-content {
  padding: 2rem;
  height: 100%;
  display: flex;
  flex-direction: column;
}

.tech-icon-3d {
  width: 70px;
  height: 70px;
  background: rgba(42, 121, 101, 0.1);
  border: 2px solid #2A7965;
  border-radius: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 32px;
  color: #2A7965;
  margin-bottom: 1.5rem;
  box-shadow: 0 0 30px rgba(42, 121, 101, 0.3);
}

.card-front h4 {
  font-size: 1.5rem;
  font-weight: 700;
  color: #fff;
  margin: 0 0 1rem 0;
  font-family: 'Orbitron', sans-serif;
}

.card-front p {
  font-size: 0.9rem;
  color: rgba(255, 255, 255, 0.6);
  line-height: 1.6;
  margin: 0 0 auto 0;
}

.tech-stats {
  padding-top: 1rem;
  border-top: 1px solid rgba(42, 121, 101, 0.2);
}

.stat-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.stat-label {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.5);
}

.stat-value {
  font-size: 18px;
  font-weight: 700;
  color: #2A7965;
  font-family: 'Orbitron', sans-serif;
}

.card-back {
  background: rgba(42, 121, 101, 0.1);
  border: 1px solid #2A7965;
  transform: rotateY(180deg);
  display: flex;
  align-items: center;
  justify-content: center;
}

.back-content {
  padding: 2rem;
  text-align: center;
}

.back-content h5 {
  font-size: 1rem;
  color: #2A7965;
  margin: 0 0 1rem 0;
  font-family: 'Orbitron', sans-serif;
}

.tech-tags-3d {
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem;
  justify-content: center;
}

.tag-3d {
  padding: 0.5rem 1rem;
  background: rgba(42, 121, 101, 0.2);
  border: 1px solid rgba(42, 121, 101, 0.5);
  border-radius: 8px;
  font-size: 12px;
  color: #fff;
}

.features-section {
  padding: 6rem 3rem;
  background: rgba(10, 10, 10, 0.4);
}

.features-showcase {
  max-width: 1400px;
  margin: 0 auto;
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 2rem;
}

.feature-card-hologram {
  position: relative;
  background: rgba(10, 10, 10, 0.6);
  border: 1px solid rgba(42, 121, 101, 0.3);
  border-radius: 16px;
  padding: 2rem;
  backdrop-filter: blur(10px);
  transition: all 0.3s ease;
  overflow: hidden;
}

.hologram-border {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  border: 2px solid transparent;
  border-radius: 16px;
  background: linear-gradient(135deg, rgba(42, 121, 101, 0.5), transparent, rgba(79, 172, 254, 0.5)) border-box;
  -webkit-mask: linear-gradient(#fff 0 0) padding-box, linear-gradient(#fff 0 0);
  -webkit-mask-composite: destination-out;
  mask-composite: exclude;
  opacity: 0;
  transition: opacity 0.3s ease;
}

.feature-card-hologram:hover .hologram-border {
  opacity: 1;
}

.feature-icon-holo {
  position: relative;
  width: 60px;
  height: 60px;
  background: rgba(42, 121, 101, 0.1);
  border: 2px solid #2A7965;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 28px;
  color: #2A7965;
  margin-bottom: 1.5rem;
}

.icon-glow {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  width: 100%;
  height: 100%;
  background: radial-gradient(circle, rgba(42, 121, 101, 0.3), transparent);
  filter: blur(20px);
}

.feature-card-hologram h4 {
  font-size: 1.25rem;
  font-weight: 700;
  color: #fff;
  margin: 0 0 0.75rem 0;
  font-family: 'Orbitron', sans-serif;
}

.feature-card-hologram p {
  font-size: 0.9rem;
  color: rgba(255, 255, 255, 0.6);
  line-height: 1.6;
  margin: 0 0 1.5rem 0;
}

.feature-metric {
  margin-top: auto;
}

.metric-bar {
  height: 4px;
  background: rgba(42, 121, 101, 0.2);
  border-radius: 2px;
  overflow: hidden;
  margin-bottom: 0.75rem;
}

.bar-fill {
  height: 100%;
  background: linear-gradient(90deg, #2A7965, #4facfe);
  border-radius: 2px;
  transition: width 1s ease;
}

.metric-info {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.feature-number {
  position: absolute;
  top: 1rem;
  right: 1rem;
  font-size: 3rem;
  font-weight: 900;
  color: rgba(42, 121, 101, 0.1);
  font-family: 'Orbitron', sans-serif;
  line-height: 1;
}

.innovation-section {
  padding: 6rem 3rem;
  max-width: 1600px;
  margin: 0 auto;
}

.innovation-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 2rem;
}

.innovation-card-cyber {
  position: relative;
  background: rgba(10, 10, 10, 0.6);
  border: 1px solid rgba(42, 121, 101, 0.3);
  border-radius: 16px;
  padding: 2rem;
  backdrop-filter: blur(10px);
  transition: all 0.3s ease;
  overflow: hidden;
}

.cyber-lines {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  pointer-events: none;
}

.cyber-line {
  position: absolute;
  background: #2A7965;
  opacity: 0;
  transition: opacity 0.3s ease;
}

.line-top,
.line-bottom {
  left: 0;
  right: 0;
  height: 1px;
}

.line-left,
.line-right {
  top: 0;
  bottom: 0;
  width: 1px;
}

.line-top { top: 0; }
.line-bottom { bottom: 0; }
.line-left { left: 0; }
.line-right { right: 0; }

.innovation-card-cyber:hover .cyber-line {
  opacity: 1;
  animation: cyberLineMove 1s ease forwards;
}

@keyframes cyberLineMove {
  0% { transform: scaleX(0); }
  100% { transform: scaleX(1); }
}

.innovation-card-cyber:hover .line-left,
.innovation-card-cyber:hover .line-right {
  animation: cyberLineMoveV 1s ease forwards;
}

@keyframes cyberLineMoveV {
  0% { transform: scaleY(0); }
  100% { transform: scaleY(1); }
}

.innovation-icon-cyber {
  width: 60px;
  height: 60px;
  background: rgba(42, 121, 101, 0.1);
  border: 2px solid #2A7965;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 28px;
  color: #2A7965;
  margin-bottom: 1.5rem;
}

.innovation-card-cyber h4 {
  font-size: 1.25rem;
  font-weight: 700;
  color: #fff;
  margin: 0 0 0.75rem 0;
  font-family: 'Orbitron', sans-serif;
}

.innovation-card-cyber p {
  font-size: 0.9rem;
  color: rgba(255, 255, 255, 0.6);
  line-height: 1.6;
  margin: 0 0 1rem 0;
}

.cyber-badge {
  display: inline-block;
  padding: 0.25rem 0.75rem;
  background: rgba(42, 121, 101, 0.1);
  border: 1px solid rgba(42, 121, 101, 0.5);
  border-radius: 6px;
  font-size: 10px;
  font-weight: 600;
  letter-spacing: 1px;
  color: #2A7965;
  font-family: 'Orbitron', sans-serif;
}

.cyber-corner {
  position: absolute;
  width: 20px;
  height: 20px;
  border: 2px solid #2A7965;
  opacity: 0;
  transition: opacity 0.3s ease;
}

.innovation-card-cyber:hover .cyber-corner {
  opacity: 1;
}

.corner-tl { top: 10px; left: 10px; border-right: none; border-bottom: none; }
.corner-tr { top: 10px; right: 10px; border-left: none; border-bottom: none; }
.corner-bl { bottom: 10px; left: 10px; border-right: none; border-top: none; }
.corner-br { bottom: 10px; right: 10px; border-left: none; border-top: none; }

.opensource-section {
  padding: 6rem 3rem;
  background: rgba(10, 10, 10, 0.4);
}

.opensource-container {
  max-width: 1400px;
  margin: 0 auto;
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 4rem;
  align-items: center;
}

.code-matrix {
  position: relative;
  height: 500px;
  overflow: hidden;
  background: rgba(0, 0, 0, 0.5);
  border-radius: 20px;
  border: 1px solid rgba(42, 121, 101, 0.3);
}

.matrix-column {
  display: inline-block;
  width: 5%;
  height: 100%;
  overflow: hidden;
}

.matrix-char {
  display: block;
  font-size: 14px;
  color: #2A7965;
  opacity: 0.3;
  animation: matrixFall 10s linear infinite;
  text-align: center;
  line-height: 1.5;
}

@keyframes matrixFall {
  0% { transform: translateY(-100%); opacity: 0; }
  10% { opacity: 0.3; }
  90% { opacity: 0.3; }
  100% { transform: translateY(100%); opacity: 0; }
}

.opensource-content {
  text-align: center;
}

.github-icon-cyber {
  position: relative;
  width: 100px;
  height: 100px;
  margin: 0 auto 2rem;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 50px;
  color: #2A7965;
}

.icon-rings {
  position: absolute;
  width: 100%;
  height: 100%;
}

.ring-outer,
.ring-inner {
  position: absolute;
  border: 2px solid rgba(42, 121, 101, 0.3);
  border-radius: 50%;
  animation: ringRotate 10s linear infinite;
}

.ring-outer {
  width: 100%;
  height: 100%;
  top: 0;
  left: 0;
}

.ring-inner {
  width: 70%;
  height: 70%;
  top: 15%;
  left: 15%;
  animation-direction: reverse;
  animation-duration: 8s;
}

.opensource-content h3 {
  font-size: 3rem;
  font-weight: 900;
  font-family: 'Orbitron', sans-serif;
  color: #fff;
  margin: 0 0 1rem 0;
}

.opensource-content p {
  font-size: 1.1rem;
  color: rgba(255, 255, 255, 0.6);
  line-height: 1.8;
  margin: 0 0 2rem 0;
}

.stats-showcase {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 1.5rem;
  margin-bottom: 2rem;
}

.stat-card {
  background: rgba(10, 10, 10, 0.6);
  border: 1px solid rgba(42, 121, 101, 0.3);
  border-radius: 12px;
  padding: 1.5rem;
  transition: all 0.3s ease;
}

.stat-card:hover {
  border-color: #2A7965;
  transform: translateY(-5px);
}

.stat-card .stat-value {
  font-size: 2rem;
  font-weight: 900;
  color: #2A7965;
  font-family: 'Orbitron', sans-serif;
  margin-bottom: 0.5rem;
}

.stat-card .stat-label {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.5);
}

.stat-bar {
  height: 2px;
  background: rgba(42, 121, 101, 0.2);
  margin-top: 1rem;
  border-radius: 1px;
  overflow: hidden;
}

.stat-card:hover .stat-bar {
  background: linear-gradient(90deg, #2A7965, transparent);
}

.action-buttons-cyber {
  display: flex;
  gap: 1rem;
  justify-content: center;
}

.cta-section {
  padding: 6rem 3rem;
}

.cta-container {
  max-width: 900px;
  margin: 0 auto;
  position: relative;
  border-radius: 24px;
  overflow: hidden;
}

.cta-bg-animation {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  overflow: hidden;
}

.wave {
  position: absolute;
  width: 200%;
  height: 200%;
  background: radial-gradient(ellipse at center, rgba(42, 121, 101, 0.1) 0%, transparent 70%);
  animation: waveMove 10s ease-in-out infinite;
}

.wave-1 { top: -50%; left: -50%; animation-delay: 0s; }
.wave-2 { top: -50%; left: -50%; animation-delay: -3.33s; }
.wave-3 { top: -50%; left: -50%; animation-delay: -6.66s; }

@keyframes waveMove {
  0%, 100% { transform: translate(0, 0) rotate(0deg); }
  50% { transform: translate(25%, 25%) rotate(180deg); }
}

.cta-content {
  position: relative;
  padding: 4rem;
  text-align: center;
  background: rgba(10, 10, 10, 0.8);
  backdrop-filter: blur(20px);
  border: 1px solid rgba(42, 121, 101, 0.3);
  border-radius: 24px;
}

.cta-title {
  font-size: 3rem;
  font-weight: 900;
  font-family: 'Orbitron', sans-serif;
  margin: 0 0 1rem 0;
}

.title-glitch {
  position: relative;
  color: #fff;
}

.title-glitch::before,
.title-glitch::after {
  content: attr(data-text);
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
}

.title-glitch::before {
  color: #ff00ff;
  animation: glitchTitle1 2s infinite linear alternate-reverse;
  clip-path: polygon(0 0, 100% 0, 100% 45%, 0 45%);
}

.title-glitch::after {
  color: #00ffff;
  animation: glitchTitle2 3s infinite linear alternate-reverse;
  clip-path: polygon(0 55%, 100% 55%, 100% 100%, 0 100%);
}

@keyframes glitchTitle1 {
  0% { transform: translate(0); }
  20% { transform: translate(-3px, 3px); }
  40% { transform: translate(-3px, -3px); }
  60% { transform: translate(3px, 3px); }
  80% { transform: translate(3px, -3px); }
  100% { transform: translate(0); }
}

@keyframes glitchTitle2 {
  0% { transform: translate(0); }
  20% { transform: translate(3px, -3px); }
  40% { transform: translate(3px, 3px); }
  60% { transform: translate(-3px, -3px); }
  80% { transform: translate(-3px, 3px); }
  100% { transform: translate(0); }
}

.cta-subtitle {
  font-size: 1.1rem;
  color: rgba(255, 255, 255, 0.6);
  margin: 0 0 2rem 0;
}

.cta-buttons {
  display: flex;
  gap: 1rem;
  justify-content: center;
}

.home-footer {
  background: rgba(0, 0, 0, 0.9);
  border-top: 1px solid rgba(42, 121, 101, 0.3);
  padding: 2rem 3rem;
}

.footer-content {
  max-width: 1600px;
  margin: 0 auto;
  text-align: center;
}

.footer-brand {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  font-size: 20px;
  font-weight: 700;
  font-family: 'Orbitron', sans-serif;
  color: #fff;
  margin-bottom: 0.5rem;
}

.footer-icon {
  font-size: 24px;
  color: #2A7965;
}

.footer-content p {
  margin: 0 0 1rem 0;
  font-size: 14px;
  color: rgba(255, 255, 255, 0.5);
}

.footer-links {
  display: flex;
  gap: 2rem;
  justify-content: center;
}

.footer-links a {
  color: rgba(255, 255, 255, 0.6);
  text-decoration: none;
  font-size: 14px;
  transition: color 0.3s ease;
}

.footer-links a:hover {
  color: #2A7965;
}

@media (max-width: 1200px) {
  .tech-grid,
  .innovation-grid {
    grid-template-columns: repeat(2, 1fr);
  }
  
  .stats-showcase {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 768px) {
  .hero-content {
    grid-template-columns: 1fr;
    text-align: center;
  }
  
  .hero-title {
    font-size: 2.5rem;
  }
  
  .tech-grid,
  .innovation-grid,
  .features-showcase {
    grid-template-columns: 1fr;
  }
  
  .opensource-container {
    grid-template-columns: 1fr;
  }
  
  .code-matrix {
    height: 300px;
  }
  
  .stats-showcase {
    grid-template-columns: repeat(2, 1fr);
  }
  
  .footer-links {
    flex-wrap: wrap;
  }
}
</style>
