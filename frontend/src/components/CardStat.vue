<template>
  <a-card class="card-stat">
    <a-statistic
      :title="title"
      :value="value"
      :precision="precision"
      :suffix="suffix"
      :value-style="valueStyle"
    >
      <template #prefix>
        <slot name="prefix"></slot>
      </template>
      <template #suffix>
        <slot name="suffix">
          {{ suffix }}
        </slot>
      </template>
    </a-statistic>
    <div v-if="description" class="stat-description">
      {{ description }}
    </div>
  </a-card>
</template>

<script setup lang="ts">
defineProps<{
  title: string
  value: number | string
  precision?: number
  suffix?: string
  valueStyle?: Record<string, string>
  description?: string
}>()
</script>

<style scoped>
.card-stat {
  background: rgba(16, 24, 40, 0.6);
  border: 1px solid rgba(0, 212, 255, 0.15);
  transition: all 0.3s;
}

.card-stat:hover {
  border-color: rgba(0, 212, 255, 0.3);
  box-shadow: 0 4px 12px rgba(0, 212, 255, 0.1);
}

:deep(.ant-statistic-title) {
  color: #8ba3c7 !important;
  font-size: 14px !important;
  margin-bottom: 8px !important;
}

:deep(.ant-statistic-content) {
  color: #e8f4ff !important;
  font-size: 24px !important;
  font-weight: 600 !important;
}

.stat-description {
  margin-top: 8px;
  color: #8ba3c7;
  font-size: 12px;
  line-height: 1.4;
}
</style>