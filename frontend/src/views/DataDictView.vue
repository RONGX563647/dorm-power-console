<template>
  <div class="data-dict-view">
    <a-page-header
      title="数据字典"
      sub-title="系统数据字典管理"
      style="padding: 0 0 16px 0"
    >
      <template #extra>
        <a-space>
          <a-button @click="initDict" :loading="initializing">
            初始化默认字典
          </a-button>
          <a-button type="primary" @click="showDictModal()">
            新建字典
          </a-button>
        </a-space>
      </template>
    </a-page-header>

    <a-row :gutter="16">
      <a-col :span="6">
        <a-card title="字典类型" size="small">
          <a-tree
            :tree-data="dictTypes"
            :selected-keys="selectedType ? [selectedType] : []"
            @select="handleTypeSelect"
          >
            <template #title="{ title, key }">
              <span>{{ title }}</span>
            </template>
          </a-tree>
        </a-card>
      </a-col>

      <a-col :span="18">
        <a-card :title="currentTypeTitle">
          <template #extra>
            <a-space>
              <a-button @click="loadDicts" :loading="loading">
                刷新
              </a-button>
              <a-button type="primary" @click="showDictModal()">
                新建
              </a-button>
            </a-space>
          </template>

          <a-table
            :data-source="dicts"
            :columns="dictColumns"
            :loading="loading"
            :pagination="{ pageSize: 10, showSizeChanger: true, pageSizeOptions: ['5', '10', '15'] }"
            size="small"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'action'">
                <a-space>
                  <a-button type="link" size="small" @click="showDictModal(record)">
                    编辑
                  </a-button>
                  <a-popconfirm
                    title="确定删除此字典吗？"
                    @confirm="deleteDict(record.id)"
                  >
                    <a-button type="link" size="small" danger>删除</a-button>
                  </a-popconfirm>
                </a-space>
              </template>
            </template>
          </a-table>
        </a-card>
      </a-col>
    </a-row>

    <a-modal
      v-model:open="dictModalVisible"
      :title="editingDict ? '编辑字典' : '新建字典'"
      @ok="handleDictSubmit"
      :confirm-loading="submitting"
    >
      <a-form :model="dictForm" :label-col="{ span: 6 }" :wrapper-col="{ span: 16 }">
        <a-form-item label="字典类型" required>
          <a-input v-model:value="dictForm.dictType" placeholder="请输入字典类型" />
        </a-form-item>
        <a-form-item label="字典代码" required>
          <a-input v-model:value="dictForm.dictCode" placeholder="请输入字典代码" />
        </a-form-item>
        <a-form-item label="字典标签" required>
          <a-input v-model:value="dictForm.dictLabel" placeholder="请输入字典标签" />
        </a-form-item>
        <a-form-item label="字典值">
          <a-input v-model:value="dictForm.dictValue" placeholder="请输入字典值" />
        </a-form-item>
        <a-form-item label="排序">
          <a-input-number v-model:value="dictForm.sort" :min="0" style="width: 100%" />
        </a-form-item>
        <a-form-item label="备注">
          <a-textarea v-model:value="dictForm.remark" placeholder="请输入备注" :rows="2" />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { dataDictApi } from '@/api'

const loading = ref(false)
const initializing = ref(false)
const submitting = ref(false)

const dictTypes = ref<any[]>([])
const selectedType = ref<string>('')
const dicts = ref<any[]>([])

const dictModalVisible = ref(false)
const editingDict = ref<any>(null)

const dictForm = ref({
  dictType: '',
  dictCode: '',
  dictLabel: '',
  dictValue: '',
  sort: 0,
  remark: ''
})

const dictColumns = [
  { title: '字典代码', dataIndex: 'dictCode', key: 'dictCode' },
  { title: '字典标签', dataIndex: 'dictLabel', key: 'dictLabel' },
  { title: '字典值', dataIndex: 'dictValue', key: 'dictValue' },
  { title: '排序', dataIndex: 'sort', key: 'sort' },
  { title: '备注', dataIndex: 'remark', key: 'remark' },
  { title: '操作', key: 'action', width: 150 }
]

const currentTypeTitle = computed(() => {
  return selectedType.value ? `字典项 - ${selectedType.value}` : '字典项'
})

onMounted(() => {
  loadTypes()
})

const loadTypes = async () => {
  try {
    const data = await dataDictApi.getTypes()
    const types = Array.isArray(data) ? data : []
    dictTypes.value = types.map((type: string) => ({
      key: type,
      title: type,
      value: type
    }))
    
    if (types.length > 0) {
      selectedType.value = types[0]
      await loadDicts()
    }
  } catch (error) {
    message.error('加载字典类型失败')
    console.error(error)
  }
}

const loadDicts = async () => {
  if (!selectedType.value) return
  
  loading.value = true
  try {
    const data = await dataDictApi.getByType(selectedType.value)
    dicts.value = Array.isArray(data) ? data : []
  } catch (error) {
    message.error('加载字典失败')
    console.error(error)
  } finally {
    loading.value = false
  }
}

const handleTypeSelect = (selectedKeys: string[]) => {
  if (selectedKeys.length > 0) {
    selectedType.value = selectedKeys[0]
    loadDicts()
  }
}

const showDictModal = (dict?: any) => {
  editingDict.value = dict || null
  if (dict) {
    dictForm.value = {
      dictType: dict.dictType,
      dictCode: dict.dictCode,
      dictLabel: dict.dictLabel,
      dictValue: dict.dictValue || '',
      sort: dict.sort || 0,
      remark: dict.remark || ''
    }
  } else {
    dictForm.value = {
      dictType: selectedType.value,
      dictCode: '',
      dictLabel: '',
      dictValue: '',
      sort: 0,
      remark: ''
    }
  }
  dictModalVisible.value = true
}

const handleDictSubmit = async () => {
  if (!dictForm.value.dictType || !dictForm.value.dictCode || !dictForm.value.dictLabel) {
    message.warning('请填写必填项')
    return
  }

  submitting.value = true
  try {
    if (editingDict.value) {
      await dataDictApi.update(editingDict.value.id, dictForm.value)
      message.success('字典更新成功')
    } else {
      await dataDictApi.create(dictForm.value)
      message.success('字典创建成功')
    }
    dictModalVisible.value = false
    await loadDicts()
  } catch (error) {
    message.error('操作失败')
    console.error(error)
  } finally {
    submitting.value = false
  }
}

const deleteDict = async (id: string) => {
  try {
    await dataDictApi.delete(id)
    message.success('字典已删除')
    await loadDicts()
  } catch (error) {
    message.error('删除失败')
    console.error(error)
  }
}

const initDict = async () => {
  initializing.value = true
  try {
    await dataDictApi.initDict()
    message.success('初始化成功')
    await loadTypes()
  } catch (error) {
    message.error('初始化失败')
    console.error(error)
  } finally {
    initializing.value = false
  }
}
</script>

<style scoped>
.data-dict-view {
  padding: 24px;
}

:deep(.ant-card) {
  margin-bottom: 16px;
}

:deep(.ant-card-head-title) {
  font-weight: 600;
}
</style>
