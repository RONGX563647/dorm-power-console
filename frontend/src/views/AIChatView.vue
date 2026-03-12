<template>
  <div class="ai-chat-view">
    <a-page-header
      title="AI智能客服"
      sub-title="智能对话、意图识别与快速问答"
      style="padding: 0 0 16px 0"
    />

    <a-card title="智能对话">
      <!-- 房间选择 -->
      <div class="room-selector" style="margin-bottom: 16px;">
        <a-form layout="inline">
          <a-form-item label="选择房间：">
            <a-select
              v-model:value="selectedRoom"
              :options="roomOptions"
              placeholder="请选择房间"
              style="width: 200px"
            >
              <template #option="{ value, label }">
                <div>{{ label }}</div>
              </template>
            </a-select>
          </a-form-item>
        </a-form>
      </div>
      
      <!-- 常见问题快捷按钮 -->
      <div class="quick-questions" v-if="!sending">
        <a-tag 
          v-for="question in quickQuestions" 
          :key="question"
          @click="useQuickQuestion(question)"
          style="margin-right: 8px; margin-bottom: 8px; cursor: pointer;"
        >
          {{ question }}
        </a-tag>
      </div>
      
      <div class="chat-container">
        <div class="chat-messages" ref="chatMessagesRef">
          <div v-for="(message, index) in chatMessages" :key="index" class="chat-message" :class="message.type">
            <div class="message-content">
              <div class="message-header">
                <span class="message-sender">{{ message.sender }}</span>
                <span class="message-time">{{ message.time }}</span>
              </div>
              <div class="message-text">{{ message.text }}</div>
              <div v-if="message.intentInfo" class="intent-info">
                <a-tag size="small" color="blue">意图识别</a-tag>
                <div class="intent-details">
                  <span>意图: {{ message.intentInfo.intent }}</span>
                  <span>置信度: {{ (message.intentInfo.confidence * 100).toFixed(1) }}%</span>
                  <span v-if="message.intentInfo.apiEndpoint">API: {{ message.intentInfo.apiEndpoint }}</span>
                </div>
              </div>
            </div>
          </div>
        </div>
        <div class="chat-input">
          <a-input
            v-model:value="chatInput"
            placeholder="请输入您的问题..."
            @keyup.enter="sendMessage"
          >
            <template #append>
              <a-button type="primary" @click="sendMessage" :loading="sending">
                发送
              </a-button>
            </template>
          </a-input>
        </div>
      </div>
    </a-card>

    <a-card title="服务状态" style="margin-top: 16px">
      <div class="status-container">
        <a-button type="primary" @click="checkHealth" :loading="checkingHealth">
          检查状态
        </a-button>
        
        <div v-if="healthStatus" style="margin-top: 16px">
          <a-descriptions :column="1" size="small">
            <a-descriptions-item label="服务状态">
              <a-tag :color="healthStatus.status === 'UP' ? 'green' : 'red'">
                {{ healthStatus.status }}
              </a-tag>
            </a-descriptions-item>
            <a-descriptions-item label="服务名称">
              {{ healthStatus.service }}
            </a-descriptions-item>
            <a-descriptions-item label="功能状态">
              <a-list size="small">
                <a-list-item v-for="(status, feature) in healthStatus.features" :key="feature">
                  <span>{{ feature }}: </span>
                  <a-tag :color="status ? 'green' : 'red'">
                    {{ status ? '正常' : '异常' }}
                  </a-tag>
                </a-list-item>
              </a-list>
            </a-descriptions-item>
          </a-descriptions>
        </div>
      </div>
    </a-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, nextTick } from 'vue'
import { message } from 'ant-design-vue'
import { aiChatApi, dormApi } from '@/api'

const chatInput = ref('')

const sending = ref(false)
const checkingHealth = ref(false)

const chatMessages = ref<any[]>([])
const healthStatus = ref<any>(null)

const chatMessagesRef = ref<HTMLElement>()

// 房间选择相关
const rooms = ref<any[]>([])
const roomOptions = ref<any[]>([])
const selectedRoom = ref('')

// 常见问题快捷按钮
const quickQuestions = [
  '你好',
  '查询用电量',
  '设备状态',
  '电费账单',
  '节能建议',
  '告警信息',
  '如何使用系统'
]

// 使用快速问题
const useQuickQuestion = (question: string) => {
  chatInput.value = question
  sendMessage()
}

// 获取房间列表
  const getRooms = async () => {
    try {
      const response = await dormApi.getAllRooms()
      rooms.value = response
      roomOptions.value = response.map((room: any) => ({
        value: room.id,
        label: room.roomNumber
      }))
    } catch (error) {
      console.error('获取房间列表失败:', error)
      message.error('获取房间列表失败，请稍后再试')
    }
  }

onMounted(() => {
  // 添加欢迎消息
  addSystemMessage('您好！我是宿舍用电管理助手，有什么可以帮您？')
  // 检查服务状态
  checkHealth()
  // 获取房间列表
  getRooms()
})

const addSystemMessage = (text: string, intentInfo?: any) => {
  chatMessages.value.push({
    type: 'system',
    sender: 'AI助手',
    text,
    intentInfo,
    time: new Date().toLocaleTimeString()
  })
  scrollToBottom()
}

const addUserMessage = (text: string) => {
  chatMessages.value.push({
    type: 'user',
    sender: '您',
    text,
    time: new Date().toLocaleTimeString()
  })
  scrollToBottom()
}

const scrollToBottom = async () => {
  await nextTick()
  if (chatMessagesRef.value) {
    chatMessagesRef.value.scrollTop = chatMessagesRef.value.scrollHeight
  }
}

// 状态管理
const waitingForRoomId = ref(false)
const pendingIntent = ref<any>(null)
const pendingMessage = ref('')

const sendMessage = async () => {
  let messageText = chatInput.value.trim()
  if (!messageText) {
    return
  }

  // 检查是否在等待房间ID
  if (waitingForRoomId.value && pendingIntent.value) {
    handleRoomIdInput(messageText)
    return
  }

  // 如果用户选择了房间，在消息中添加房间信息
  if (selectedRoom.value) {
    messageText = `${messageText} 房间号: ${selectedRoom.value}`
  }

  addUserMessage(chatInput.value.trim())
  chatInput.value = ''
  sending.value = true

  // 添加正在处理的提示
  const processingMessageId = chatMessages.value.length
  addSystemMessage('正在处理您的问题...')

  try {
    // 1. 首先尝试快速问答（响应最快）
    const quickResult = await aiChatApi.quickReply(messageText)
    
    // 2. 同时进行意图识别
    const intentResult = await aiChatApi.recognizeIntent(messageText)
    
    // 3. 如果快速问答匹配成功，直接使用快速回答
    if (quickResult.matched) {
      // 替换处理中消息
      chatMessages.value[processingMessageId] = {
        type: 'system',
        sender: 'AI助手',
        text: quickResult.response,
        intentInfo: intentResult,
        time: new Date().toLocaleTimeString()
      }
    } else {
      // 4. 检查是否需要房间ID
      if (needsRoomId(intentResult.intent) && !intentResult.entities.roomId) {
        // 替换处理中消息，提示用户输入房间ID
        chatMessages.value[processingMessageId] = {
          type: 'system',
          sender: 'AI助手',
          text: '请提供您的房间号，例如 A-101',
          intentInfo: intentResult,
          time: new Date().toLocaleTimeString()
        }
        // 进入等待房间ID状态
        waitingForRoomId.value = true
        pendingIntent.value = intentResult
        pendingMessage.value = messageText
      } else {
        // 5. 否则使用智能对话
        const chatResponse = await aiChatApi.chat(messageText)
        if (chatResponse.success) {
          // 替换处理中消息
          chatMessages.value[processingMessageId] = {
            type: 'system',
            sender: 'AI助手',
            text: chatResponse.response,
            intentInfo: intentResult,
            time: new Date().toLocaleTimeString()
          }
        } else {
          // 替换处理中消息
          chatMessages.value[processingMessageId] = {
            type: 'system',
            sender: 'AI助手',
            text: '抱歉，我无法理解您的问题，请尝试换一种方式提问。',
            intentInfo: intentResult,
            time: new Date().toLocaleTimeString()
          }
        }
      }
    }
  } catch (error) {
    console.error('处理消息失败:', error)
    // 替换处理中消息为错误提示
    chatMessages.value[processingMessageId] = {
      type: 'system',
      sender: 'AI助手',
      text: '抱歉，系统暂时无法响应，请稍后再试。',
      time: new Date().toLocaleTimeString()
    }
  } finally {
    sending.value = false
    scrollToBottom()
  }
}

// 检查意图是否需要房间ID
const needsRoomId = (intent: string): boolean => {
  const roomRequiredIntents = ['POWER_QUERY', 'DEVICE_STATUS', 'BILL_QUERY', 'SCENE_CONTROL']
  return roomRequiredIntents.includes(intent)
}

// 处理用户输入的房间ID
const handleRoomIdInput = async (roomId: string) => {
  addUserMessage(roomId)
  chatInput.value = ''
  sending.value = true
  
  // 添加正在处理的提示
  const processingMessageId = chatMessages.value.length
  addSystemMessage('正在处理您的问题...')

  try {
    if (pendingIntent.value && pendingMessage.value) {
      // 根据意图调用相应的API
      const response = await processWithRoomId(pendingMessage.value, pendingIntent.value, roomId)
      
      // 替换处理中消息
      chatMessages.value[processingMessageId] = {
        type: 'system',
        sender: 'AI助手',
        text: response,
        intentInfo: {
          ...pendingIntent.value,
          entities: {
            ...pendingIntent.value.entities,
            roomId
          }
        },
        time: new Date().toLocaleTimeString()
      }
    }
  } catch (error) {
    console.error('处理房间ID失败:', error)
    // 替换处理中消息为错误提示
    chatMessages.value[processingMessageId] = {
      type: 'system',
      sender: 'AI助手',
      text: '抱歉，系统暂时无法响应，请稍后再试。',
      time: new Date().toLocaleTimeString()
    }
  } finally {
    sending.value = false
    waitingForRoomId.value = false
    pendingIntent.value = null
    pendingMessage.value = ''
    scrollToBottom()
  }
}

// 根据意图和房间ID处理请求
const processWithRoomId = async (message: string, intent: any, roomId: string): Promise<string> => {
  // 这里可以根据不同的意图调用不同的API
  // 目前使用智能对话API，传入房间信息
  const chatResponse = await aiChatApi.chat(`${message} 房间号: ${roomId}`)
  if (chatResponse.success) {
    return chatResponse.response
  }
  return '抱歉，我无法理解您的问题，请尝试换一种方式提问。'
}

const checkHealth = async () => {
  checkingHealth.value = true
  try {
    const result = await aiChatApi.health()
    healthStatus.value = result
  } catch (error) {
    console.error('检查健康状态失败:', error)
    message.error('检查失败，请稍后再试')
  } finally {
    checkingHealth.value = false
  }
}
</script>

<style scoped>
.ai-chat-view {
  padding: 24px;
}

.quick-questions {
  margin-bottom: 16px;
  padding: 12px;
  background-color: #f9f9f9;
  border-radius: 8px;
  border: 1px solid #f0f0f0;
}

.quick-questions :deep(.ant-tag) {
  transition: all 0.3s;
}

.quick-questions :deep(.ant-tag:hover) {
  background-color: #e6f7ff;
  border-color: #91d5ff;
  color: #1890ff;
}

.chat-container {
  height: 500px;
  display: flex;
  flex-direction: column;
}

.chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
  border: 1px solid #f0f0f0;
  border-radius: 8px;
  margin-bottom: 16px;
}

.chat-message {
  margin-bottom: 16px;
  max-width: 80%;
}

.chat-message.user {
  align-self: flex-end;
  margin-left: auto;
}

.chat-message.system {
  align-self: flex-start;
  margin-right: auto;
}

.message-content {
  padding: 12px 16px;
  border-radius: 8px;
}

.chat-message.user .message-content {
  background-color: #e6f7ff;
  border: 1px solid #91d5ff;
}

.chat-message.system .message-content {
  background-color: #f6ffed;
  border: 1px solid #b7eb8f;
}

.message-header {
  display: flex;
  justify-content: space-between;
  margin-bottom: 8px;
  font-size: 12px;
  color: #8c8c8c;
}

.message-text {
  line-height: 1.5;
  margin-bottom: 8px;
}

.intent-info {
  margin-top: 8px;
  padding: 8px;
  background-color: #f5f5f5;
  border-radius: 4px;
  font-size: 12px;
}

.intent-details {
  margin-top: 4px;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.intent-details span {
  color: #666;
}

.chat-input {
  width: 100%;
}

.status-container {
  display: flex;
  flex-direction: column;
}

:deep(.ant-card) {
  margin-bottom: 16px;
}

:deep(.ant-card-head-title) {
  font-weight: 600;
}
</style>