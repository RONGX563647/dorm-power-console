<template>
  <div class="socket-matrix">
    <div class="matrix-grid">
      <div
        v-for="socket in sockets"
        :key="socket.id"
        class="socket-item"
        :class="{ 'socket-active': socket.on, 'socket-inactive': !socket.on }"
      >
        <div class="socket-header">
          <span class="socket-name">{{ socket.name }}</span>
          <a-switch
            :checked="socket.on"
            @change="handleSocketChange(socket.id, $event)"
            :loading="controlling"
          />
        </div>
        <div class="socket-info">
          <span class="socket-power">
            <ThunderboltOutlined style="margin-right: 4px" />
            {{ socket.power_w.toFixed(1) }} W
          </span>
          <span v-if="socket.current_a" class="socket-current">
            {{ socket.current_a.toFixed(2) }} A
          </span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ThunderboltOutlined } from '@ant-design/icons-vue'

interface Socket {
  id: number
  name: string
  on: boolean
  power_w: number
  current_a?: number
}

defineProps<{
  sockets: Socket[]
  controlling?: boolean
}>()

const emit = defineEmits<{
  (e: 'socketChange', socketId: number, checked: boolean): void
}>()

const handleSocketChange = (socketId: number, checked: boolean) => {
  emit('socketChange', socketId, checked)
}
</script>

<style scoped>
.socket-matrix {
  width: 100%;
}

.matrix-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 16px;
}

.socket-item {
  padding: 16px;
  border-radius: 8px;
  border: 1px solid rgba(0, 212, 255, 0.15);
  transition: all 0.3s;
}

.socket-active {
  background: rgba(0, 212, 255, 0.05);
  border-color: rgba(0, 212, 255, 0.3);
}

.socket-inactive {
  background: rgba(16, 24, 40, 0.4);
  border-color: rgba(0, 212, 255, 0.1);
}

.socket-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.socket-name {
  color: #e8f4ff;
  font-weight: 500;
  font-size: 14px;
}

.socket-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.socket-power {
  color: #00d4ff;
  font-size: 13px;
  font-weight: 500;
}

.socket-current {
  color: #8ba3c7;
  font-size: 12px;
}
</style>