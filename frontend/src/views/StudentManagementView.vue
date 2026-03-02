<template>
  <div class="student-management-view">
    <div class="page-header">
      <h1>学生管理</h1>
      <a-button type="primary" @click="showCreateModal">
        <template #icon><PlusOutlined /></template>
        添加学生
      </a-button>
    </div>

    <a-row :gutter="16" style="margin-bottom: 24px">
      <a-col :span="4">
        <a-card size="small">
          <a-statistic title="总学生数" :value="statistics?.totalStudents || 0" />
        </a-card>
      </a-col>
      <a-col :span="4">
        <a-card size="small">
          <a-statistic title="在读学生" :value="statistics?.activeStudents || 0" />
        </a-card>
      </a-col>
      <a-col :span="4">
        <a-card size="small">
          <a-statistic title="已毕业" :value="statistics?.graduatedStudents || 0" />
        </a-card>
      </a-col>
      <a-col :span="4">
        <a-card size="small">
          <a-statistic title="已分配宿舍" :value="statistics?.assignedStudents || 0" />
        </a-card>
      </a-col>
      <a-col :span="4">
        <a-card size="small">
          <a-statistic title="未分配宿舍" :value="statistics?.unassignedStudents || 0" />
        </a-card>
      </a-col>
      <a-col :span="4">
        <a-card size="small">
          <a-statistic title="入住率" :value="statistics?.occupancyRate || 0" suffix="%" />
        </a-card>
      </a-col>
    </a-row>

    <a-card>
      <div class="filter-bar">
        <a-space>
          <a-input-search
            v-model:value="searchKeyword"
            placeholder="搜索姓名或学号"
            style="width: 250px"
            @search="handleSearch"
          />
          <a-select
            v-model:value="filterStatus"
            style="width: 150px"
            placeholder="状态筛选"
            allowClear
            @change="handleFilterChange"
          >
            <a-select-option value="ACTIVE">在读</a-select-option>
            <a-select-option value="GRADUATED">已毕业</a-select-option>
            <a-select-option value="SUSPENDED">休学</a-select-option>
          </a-select>
          <a-select
            v-model:value="filterDepartment"
            style="width: 180px"
            placeholder="院系筛选"
            allowClear
            @change="handleFilterChange"
          >
            <a-select-option value="计算机学院">计算机学院</a-select-option>
            <a-select-option value="电子工程学院">电子工程学院</a-select-option>
            <a-select-option value="机械工程学院">机械工程学院</a-select-option>
            <a-select-option value="经济管理学院">经济管理学院</a-select-option>
            <a-select-option value="外国语学院">外国语学院</a-select-option>
          </a-select>
          <a-button @click="loadUnassignedStudents">查看未分配学生</a-button>
          <a-button type="primary" @click="showBatchGraduateModal">
            批量毕业处理
          </a-button>
        </a-space>
      </div>

      <a-table
        :columns="studentColumns"
        :data-source="students"
        :loading="loading"
        :pagination="pagination"
        :row-key="(record: Student) => record.id"
        @change="handleTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'name'">
            <span style="color: #1a1a1a; font-weight: 500">{{ record.name }}</span>
          </template>
          <template v-if="column.key === 'gender'">
            <a-tag :color="record.gender === 'MALE' ? 'blue' : 'pink'">
              {{ record.gender === 'MALE' ? '男' : '女' }}
            </a-tag>
          </template>
          <template v-if="column.key === 'status'">
            <a-tag :color="getStatusColor(record.status)">
              {{ getStatusName(record.status) }}
            </a-tag>
          </template>
          <template v-if="column.key === 'room'">
            <span v-if="record.roomNumber" style="color: #52c41a">{{ record.roomNumber }}</span>
            <span v-else style="color: #1a1a1a">未分配</span>
          </template>
          <template v-if="column.key === 'action'">
            <a-space>
              <a-button type="link" size="small" @click="viewStudent(record.id)">查看</a-button>
              <a-button type="link" size="small" @click="editStudent(record)">编辑</a-button>
              <a-button
                v-if="record.status === 'ACTIVE' && !record.roomId"
                type="link"
                size="small"
                @click="showCheckInModal(record)"
              >
                入住
              </a-button>
              <a-button
                v-if="record.roomId"
                type="link"
                size="small"
                @click="showSwapRoomModal(record)"
              >
                换宿
              </a-button>
              <a-button
                v-if="record.roomId"
                type="link"
                size="small"
                @click="handleCheckOut(record.id)"
              >
                退宿
              </a-button>
              <a-popconfirm title="确定删除此学生？" @confirm="deleteStudent(record.id)">
                <a-button type="link" size="small" danger>删除</a-button>
              </a-popconfirm>
            </a-space>
          </template>
        </template>
      </a-table>
    </a-card>

    <a-modal
      v-model:open="modalVisible"
      :title="editingStudent ? '编辑学生' : '添加学生'"
      @ok="handleModalOk"
    >
      <a-form ref="formRef" :model="formData" :rules="formRules" layout="vertical">
        <a-form-item label="学号" name="studentNumber">
          <a-input
            v-model:value="formData.studentNumber"
            placeholder="请输入学号"
            :disabled="!!editingStudent"
          />
        </a-form-item>
        <a-form-item label="姓名" name="name">
          <a-input v-model:value="formData.name" placeholder="请输入姓名" />
        </a-form-item>
        <a-form-item label="性别" name="gender">
          <a-radio-group v-model:value="formData.gender">
            <a-radio value="MALE">男</a-radio>
            <a-radio value="FEMALE">女</a-radio>
          </a-radio-group>
        </a-form-item>
        <a-form-item label="院系" name="department">
          <a-select v-model:value="formData.department" placeholder="请选择院系">
            <a-select-option value="计算机学院">计算机学院</a-select-option>
            <a-select-option value="电子工程学院">电子工程学院</a-select-option>
            <a-select-option value="机械工程学院">机械工程学院</a-select-option>
            <a-select-option value="经济管理学院">经济管理学院</a-select-option>
            <a-select-option value="外国语学院">外国语学院</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="专业" name="major">
          <a-input v-model:value="formData.major" placeholder="请输入专业" />
        </a-form-item>
        <a-form-item label="年级" name="grade">
          <a-input-number v-model:value="formData.grade" :min="1" :max="6" style="width: 100%" />
        </a-form-item>
        <a-form-item label="手机号" name="phone">
          <a-input v-model:value="formData.phone" placeholder="请输入手机号" />
        </a-form-item>
        <a-form-item label="邮箱" name="email">
          <a-input v-model:value="formData.email" placeholder="请输入邮箱" />
        </a-form-item>
        <a-form-item label="状态" name="status">
          <a-select v-model:value="formData.status">
            <a-select-option value="ACTIVE">在读</a-select-option>
            <a-select-option value="GRADUATED">已毕业</a-select-option>
            <a-select-option value="SUSPENDED">休学</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="毕业年份" name="graduationYear">
          <a-input-number v-model:value="formData.graduationYear" :min="2000" :max="2100" style="width: 100%" />
        </a-form-item>
      </a-form>
    </a-modal>

    <a-modal
      v-model:open="checkInModalVisible"
      title="学生入住"
      @ok="handleCheckIn"
    >
      <a-form :model="checkInForm" layout="vertical">
        <a-form-item label="学生">
          <span>{{ checkInForm.studentName }}</span>
        </a-form-item>
        <a-form-item label="房间ID" required>
          <a-input v-model:value="checkInForm.roomId" placeholder="请输入房间ID" />
        </a-form-item>
        <a-form-item label="入住原因">
          <a-input v-model:value="checkInForm.reason" placeholder="例如：新生入住" />
        </a-form-item>
      </a-form>
    </a-modal>

    <a-modal
      v-model:open="swapRoomModalVisible"
      title="调换宿舍"
      @ok="handleSwapRoom"
    >
      <a-form :model="swapRoomForm" layout="vertical">
        <a-form-item label="学生">
          <span>{{ swapRoomForm.studentName }}</span>
        </a-form-item>
        <a-form-item label="当前房间">
          <span>{{ swapRoomForm.currentRoom }}</span>
        </a-form-item>
        <a-form-item label="新房间ID" required>
          <a-input v-model:value="swapRoomForm.newRoomId" placeholder="请输入新房间ID" />
        </a-form-item>
        <a-form-item label="调换原因">
          <a-input v-model:value="swapRoomForm.reason" placeholder="例如：个人申请" />
        </a-form-item>
      </a-form>
    </a-modal>

    <a-modal
      v-model:open="batchGraduateModalVisible"
      title="批量毕业处理"
      @ok="handleBatchGraduate"
    >
      <a-form :model="batchGraduateForm" layout="vertical">
        <a-form-item label="毕业年份" required>
          <a-input-number v-model:value="batchGraduateForm.graduationYear" :min="2000" :max="2100" style="width: 100%" />
        </a-form-item>
        <a-form-item>
          <a-alert type="warning" message="此操作将自动为指定毕业年份的所有学生办理退宿，并标记为已毕业" />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { PlusOutlined } from '@ant-design/icons-vue'
import { studentApi } from '@/api'
import type { Student, StudentStatistics } from '@/types'

const loading = ref(false)
const students = ref<Student[]>([])
const statistics = ref<StudentStatistics | null>(null)

const searchKeyword = ref('')
const filterStatus = ref<string>()
const filterDepartment = ref<string>()

const pagination = ref({
  current: 1,
  pageSize: 10,
  total: 0,
  showSizeChanger: true,
  pageSizeOptions: ['5', '10', '15'],
  showTotal: (total: number) => `共 ${total} 条`
})

const studentColumns = [
  { title: '学号', dataIndex: 'studentNumber', key: 'studentNumber' },
  { title: '姓名', dataIndex: 'name', key: 'name' },
  { title: '性别', dataIndex: 'gender', key: 'gender' },
  { title: '院系', dataIndex: 'department', key: 'department' },
  { title: '专业', dataIndex: 'major', key: 'major' },
  { title: '年级', dataIndex: 'grade', key: 'grade' },
  { title: '状态', dataIndex: 'status', key: 'status' },
  { title: '宿舍', key: 'room' },
  { title: '操作', key: 'action', width: 300 }
]

const modalVisible = ref(false)
const editingStudent = ref<Student | null>(null)
const formRef = ref()
const formData = ref<Partial<Student>>({
  studentNumber: '',
  name: '',
  gender: 'MALE',
  department: '',
  major: '',
  grade: 1,
  phone: '',
  email: '',
  status: 'ACTIVE',
  graduationYear: new Date().getFullYear() + 4
})
const formRules = {
  studentNumber: [{ required: true, message: '请输入学号' }],
  name: [{ required: true, message: '请输入姓名' }],
  gender: [{ required: true, message: '请选择性别' }],
  department: [{ required: true, message: '请选择院系' }],
  major: [{ required: true, message: '请输入专业' }],
  grade: [{ required: true, message: '请输入年级' }]
}

const checkInModalVisible = ref(false)
const checkInForm = ref({ studentId: '', studentName: '', roomId: '', reason: '新生入住' })

const swapRoomModalVisible = ref(false)
const swapRoomForm = ref({ studentId: '', studentName: '', currentRoom: '', newRoomId: '', reason: '个人申请' })

const batchGraduateModalVisible = ref(false)
const batchGraduateForm = ref({ graduationYear: new Date().getFullYear() })

const loadStudents = async () => {
  loading.value = true
  try {
    const result = await studentApi.getStudents({
      page: pagination.value.current - 1,
      size: pagination.value.pageSize,
      status: filterStatus.value,
      department: filterDepartment.value
    })
    students.value = result.content || []
    pagination.value.total = result.totalElements || 0
  } catch (error: any) {
    message.error('加载学生列表失败')
  } finally {
    loading.value = false
  }
}

const loadStatistics = async () => {
  try {
    statistics.value = await studentApi.getStudentStatistics()
  } catch (error: any) {
    console.error('加载统计失败', error)
  }
}

const handleSearch = () => {
  if (searchKeyword.value) {
    loading.value = true
    studentApi.searchStudents({
      keyword: searchKeyword.value,
      page: 0,
      size: pagination.value.pageSize
    }).then(result => {
      students.value = result.content || []
      pagination.value.total = result.totalElements || 0
    }).catch(() => {
      message.error('搜索失败')
    }).finally(() => {
      loading.value = false
    })
  } else {
    pagination.value.current = 1
    loadStudents()
  }
}

const handleFilterChange = () => {
  pagination.value.current = 1
  loadStudents()
}

const loadUnassignedStudents = async () => {
  loading.value = true
  try {
    students.value = await studentApi.getUnassignedStudents()
    pagination.value.total = students.value.length
  } catch (error: any) {
    message.error('加载未分配学生失败')
  } finally {
    loading.value = false
  }
}

const handleTableChange = (pag: any) => {
  pagination.value.current = pag.current
  pagination.value.pageSize = pag.pageSize
  loadStudents()
}

const showCreateModal = () => {
  editingStudent.value = null
  formData.value = {
    studentNumber: '',
    name: '',
    gender: 'MALE',
    department: '',
    major: '',
    grade: 1,
    phone: '',
    email: '',
    status: 'ACTIVE',
    graduationYear: new Date().getFullYear() + 4
  }
  modalVisible.value = true
}

const viewStudent = (id: string) => {
  message.info('查看学生详情功能待实现')
}

const editStudent = (student: Student) => {
  editingStudent.value = student
  formData.value = { ...student }
  modalVisible.value = true
}

const handleModalOk = async () => {
  try {
    await formRef.value?.validate()
    if (editingStudent.value) {
      await studentApi.updateStudent(editingStudent.value.id, formData.value)
      message.success('更新成功')
    } else {
      await studentApi.createStudent(formData.value)
      message.success('创建成功')
    }
    modalVisible.value = false
    loadStudents()
    loadStatistics()
  } catch (error: any) {
    message.error('操作失败')
  }
}

const deleteStudent = async (id: string) => {
  try {
    await studentApi.deleteStudent(id)
    message.success('删除成功')
    loadStudents()
    loadStatistics()
  } catch (error: any) {
    message.error('删除失败')
  }
}

const showCheckInModal = (student: Student) => {
  checkInForm.value = { studentId: student.id, studentName: student.name, roomId: '', reason: '新生入住' }
  checkInModalVisible.value = true
}

const handleCheckIn = async () => {
  if (!checkInForm.value.roomId) {
    message.warning('请输入房间ID')
    return
  }
  try {
    await studentApi.checkInStudent(checkInForm.value.studentId, checkInForm.value.roomId, checkInForm.value.reason)
    message.success('入住成功')
    checkInModalVisible.value = false
    loadStudents()
    loadStatistics()
  } catch (error: any) {
    message.error('入住失败')
  }
}

const showSwapRoomModal = (student: Student) => {
  swapRoomForm.value = {
    studentId: student.id,
    studentName: student.name,
    currentRoom: student.roomNumber || '',
    newRoomId: '',
    reason: '个人申请'
  }
  swapRoomModalVisible.value = true
}

const handleSwapRoom = async () => {
  if (!swapRoomForm.value.newRoomId) {
    message.warning('请输入新房间ID')
    return
  }
  try {
    await studentApi.swapRoom(swapRoomForm.value.studentId, swapRoomForm.value.newRoomId, swapRoomForm.value.reason)
    message.success('换宿成功')
    swapRoomModalVisible.value = false
    loadStudents()
  } catch (error: any) {
    message.error('换宿失败')
  }
}

const handleCheckOut = async (studentId: string) => {
  try {
    await studentApi.checkOutStudent(studentId, '毕业退宿')
    message.success('退宿成功')
    loadStudents()
    loadStatistics()
  } catch (error: any) {
    message.error('退宿失败')
  }
}

const showBatchGraduateModal = () => {
  batchGraduateForm.value = { graduationYear: new Date().getFullYear() }
  batchGraduateModalVisible.value = true
}

const handleBatchGraduate = async () => {
  if (!batchGraduateForm.value.graduationYear) {
    message.warning('请输入毕业年份')
    return
  }
  try {
    const result = await studentApi.batchGraduate(batchGraduateForm.value.graduationYear)
    message.success(`成功处理 ${result.processedCount} 名学生`)
    batchGraduateModalVisible.value = false
    loadStudents()
    loadStatistics()
  } catch (error: any) {
    message.error('批量处理失败')
  }
}

const getStatusName = (status: string) => {
  const map: Record<string, string> = { ACTIVE: '在读', GRADUATED: '已毕业', SUSPENDED: '休学' }
  return map[status] || status
}

const getStatusColor = (status: string) => {
  const map: Record<string, string> = { ACTIVE: 'green', GRADUATED: 'blue', SUSPENDED: 'orange' }
  return map[status] || 'default'
}

onMounted(() => {
  loadStudents()
  loadStatistics()
})
</script>

<style scoped>
.student-management-view {
  padding: 24px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.page-header h1 {
  color: #1a1a1a;
  margin: 0;
  font-size: 24px;
}

.filter-bar {
  margin-bottom: 16px;
}
</style>
