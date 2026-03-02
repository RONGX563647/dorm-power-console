<template>
  <div class="rbac-view">
    <a-page-header
      title="权限管理"
      sub-title="角色、权限与资源管理"
      style="padding: 0 0 16px 0"
    >
      <template #extra>
        <a-space>
          <a-button @click="initRbac" :loading="initializing">
            初始化权限系统
          </a-button>
        </a-space>
      </template>
    </a-page-header>

    <a-tabs v-model:activeKey="activeTab">
      <a-tab-pane key="roles" tab="角色管理">
        <a-card>
          <template #extra>
            <a-button type="primary" @click="showRoleModal()">
              新建角色
            </a-button>
          </template>

          <a-table
            :data-source="roles"
            :columns="roleColumns"
            :loading="loadingRoles"
            :pagination="{ pageSize: 10, showSizeChanger: true, pageSizeOptions: ['5', '10', '15'] }"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'enabled'">
                <a-tag :color="record.enabled ? 'green' : 'red'">
                  {{ record.enabled ? '已启用' : '已禁用' }}
                </a-tag>
              </template>
              <template v-if="column.key === 'action'">
                <a-space>
                  <a-button type="link" size="small" @click="showRolePermissions(record)">
                    权限配置
                  </a-button>
                  <a-button type="link" size="small" @click="showRoleModal(record)">
                    编辑
                  </a-button>
                  <a-popconfirm
                    title="确定删除此角色吗？"
                    @confirm="deleteRole(record.id)"
                  >
                    <a-button type="link" size="small" danger>删除</a-button>
                  </a-popconfirm>
                </a-space>
              </template>
            </template>
          </a-table>
        </a-card>
      </a-tab-pane>

      <a-tab-pane key="permissions" tab="权限管理">
        <a-card>
          <template #extra>
            <a-button type="primary" @click="showPermissionModal()">
              新建权限
            </a-button>
          </template>

          <a-table
            :data-source="permissions"
            :columns="permissionColumns"
            :loading="loadingPermissions"
            :pagination="{ pageSize: 10, showSizeChanger: true, pageSizeOptions: ['5', '10', '15'] }"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'action'">
                <a-space>
                  <a-button type="link" size="small" @click="showPermissionModal(record)">
                    编辑
                  </a-button>
                  <a-popconfirm
                    title="确定删除此权限吗？"
                    @confirm="deletePermission(record.id)"
                  >
                    <a-button type="link" size="small" danger>删除</a-button>
                  </a-popconfirm>
                </a-space>
              </template>
            </template>
          </a-table>
        </a-card>
      </a-tab-pane>

      <a-tab-pane key="resources" tab="资源管理">
        <a-card>
          <template #extra>
            <a-button type="primary" @click="showResourceModal()">
              新建资源
            </a-button>
          </template>

          <a-table
            :data-source="resources"
            :columns="resourceColumns"
            :loading="loadingResources"
            :pagination="{ pageSize: 10, showSizeChanger: true, pageSizeOptions: ['5', '10', '15'] }"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'type'">
                <a-tag :color="getResourceTypeColor(record.type)">
                  {{ record.type }}
                </a-tag>
              </template>
              <template v-if="column.key === 'action'">
                <a-space>
                  <a-button type="link" size="small" @click="showResourceModal(record)">
                    编辑
                  </a-button>
                  <a-popconfirm
                    title="确定删除此资源吗？"
                    @confirm="deleteResource(record.id)"
                  >
                    <a-button type="link" size="small" danger>删除</a-button>
                  </a-popconfirm>
                </a-space>
              </template>
            </template>
          </a-table>
        </a-card>
      </a-tab-pane>

      <a-tab-pane key="user-roles" tab="用户角色分配">
        <a-card>
          <a-space style="margin-bottom: 16px">
            <a-input
              v-model:value="searchUsername"
              placeholder="输入用户名"
              style="width: 200px"
              @pressEnter="searchUserRoles"
            />
            <a-button type="primary" @click="searchUserRoles" :loading="searchingUser">
              查询
            </a-button>
          </a-space>

          <div v-if="currentUserRoles" style="margin-top: 16px">
            <a-descriptions title="用户角色信息" bordered :column="1">
              <a-descriptions-item label="用户名">
                {{ searchUsername }}
              </a-descriptions-item>
              <a-descriptions-item label="角色列表">
                <a-space wrap>
                  <a-tag v-for="role in currentUserRoles.roles" :key="role.id" color="blue">
                    {{ role.name }}
                  </a-tag>
                  <span v-if="!currentUserRoles.roles || currentUserRoles.roles.length === 0">
                    暂无角色
                  </span>
                </a-space>
              </a-descriptions-item>
              <a-descriptions-item label="权限列表">
                <a-space wrap>
                  <a-tag v-for="perm in currentUserRoles.permissions" :key="perm.id" color="green">
                    {{ perm.name }}
                  </a-tag>
                  <span v-if="!currentUserRoles.permissions || currentUserRoles.permissions.length === 0">
                    暂无权限
                  </span>
                </a-space>
              </a-descriptions-item>
            </a-descriptions>

            <a-divider />

            <a-card size="small" title="分配角色">
              <a-transfer
                v-model:target-keys="selectedRoleIds"
                :data-source="allRolesForTransfer"
                :titles="['可选角色', '已分配角色']"
                :render="(item: any) => item.title"
                @change="handleRoleTransfer"
              />
            </a-card>
          </div>
        </a-card>
      </a-tab-pane>
    </a-tabs>

    <a-modal
      v-model:open="roleModalVisible"
      :title="editingRole ? '编辑角色' : '新建角色'"
      @ok="handleRoleSubmit"
      :confirm-loading="submittingRole"
    >
      <a-form :model="roleForm" :label-col="{ span: 6 }" :wrapper-col="{ span: 16 }">
        <a-form-item label="角色名称" required>
          <a-input v-model:value="roleForm.name" placeholder="请输入角色名称" />
        </a-form-item>
        <a-form-item label="角色代码" required>
          <a-input v-model:value="roleForm.code" placeholder="请输入角色代码" />
        </a-form-item>
        <a-form-item label="描述">
          <a-textarea v-model:value="roleForm.description" placeholder="请输入描述" :rows="3" />
        </a-form-item>
        <a-form-item label="是否启用">
          <a-switch v-model:checked="roleForm.enabled" />
        </a-form-item>
      </a-form>
    </a-modal>

    <a-modal
      v-model:open="permissionModalVisible"
      :title="editingPermission ? '编辑权限' : '新建权限'"
      @ok="handlePermissionSubmit"
      :confirm-loading="submittingPermission"
    >
      <a-form :model="permissionForm" :label-col="{ span: 6 }" :wrapper-col="{ span: 16 }">
        <a-form-item label="权限名称" required>
          <a-input v-model:value="permissionForm.name" placeholder="请输入权限名称" />
        </a-form-item>
        <a-form-item label="权限代码" required>
          <a-input v-model:value="permissionForm.code" placeholder="请输入权限代码" />
        </a-form-item>
        <a-form-item label="资源">
          <a-select v-model:value="permissionForm.resourceId" placeholder="请选择资源">
            <a-select-option v-for="res in resources" :key="res.id" :value="res.id">
              {{ res.name }}
            </a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="描述">
          <a-textarea v-model:value="permissionForm.description" placeholder="请输入描述" :rows="3" />
        </a-form-item>
      </a-form>
    </a-modal>

    <a-modal
      v-model:open="resourceModalVisible"
      :title="editingResource ? '编辑资源' : '新建资源'"
      @ok="handleResourceSubmit"
      :confirm-loading="submittingResource"
    >
      <a-form :model="resourceForm" :label-col="{ span: 6 }" :wrapper-col="{ span: 16 }">
        <a-form-item label="资源名称" required>
          <a-input v-model:value="resourceForm.name" placeholder="请输入资源名称" />
        </a-form-item>
        <a-form-item label="资源代码" required>
          <a-input v-model:value="resourceForm.code" placeholder="请输入资源代码" />
        </a-form-item>
        <a-form-item label="资源类型" required>
          <a-select v-model:value="resourceForm.type" placeholder="请选择资源类型">
            <a-select-option value="MENU">菜单</a-select-option>
            <a-select-option value="BUTTON">按钮</a-select-option>
            <a-select-option value="API">API</a-select-option>
            <a-select-option value="DATA">数据</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="父级资源">
          <a-tree-select
            v-model:value="resourceForm.parentId"
            :tree-data="resourceTree"
            placeholder="请选择父级资源"
            allow-clear
          />
        </a-form-item>
        <a-form-item label="资源路径">
          <a-input v-model:value="resourceForm.url" placeholder="请输入资源路径" />
        </a-form-item>
        <a-form-item label="描述">
          <a-textarea v-model:value="resourceForm.description" placeholder="请输入描述" :rows="3" />
        </a-form-item>
      </a-form>
    </a-modal>

    <a-modal
      v-model:open="rolePermissionModalVisible"
      :title="`配置角色权限 - ${currentRole?.name || ''}`"
      width="700px"
      @ok="handleRolePermissionSubmit"
      :confirm-loading="submittingRolePermissions"
    >
      <a-transfer
        v-model:target-keys="selectedPermissionIds"
        :data-source="allPermissionsForTransfer"
        :titles="['可选权限', '已分配权限']"
        :render="(item: any) => item.title"
        :list-style="{
          width: '300px',
          height: '400px',
        }"
      />
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { rbacApi } from '@/api'

const activeTab = ref('roles')
const initializing = ref(false)
const loadingRoles = ref(false)
const loadingPermissions = ref(false)
const loadingResources = ref(false)
const searchingUser = ref(false)
const submittingRole = ref(false)
const submittingPermission = ref(false)
const submittingResource = ref(false)
const submittingRolePermissions = ref(false)

const roles = ref<any[]>([])
const permissions = ref<any[]>([])
const resources = ref<any[]>([])
const resourceTree = ref<any[]>([])
const searchUsername = ref('')
const currentUserRoles = ref<any>(null)
const selectedRoleIds = ref<string[]>([])
const selectedPermissionIds = ref<string[]>([])

const roleModalVisible = ref(false)
const permissionModalVisible = ref(false)
const resourceModalVisible = ref(false)
const rolePermissionModalVisible = ref(false)

const editingRole = ref<any>(null)
const editingPermission = ref<any>(null)
const editingResource = ref<any>(null)
const currentRole = ref<any>(null)

const roleForm = ref({
  name: '',
  code: '',
  description: '',
  enabled: true
})

const permissionForm = ref({
  name: '',
  code: '',
  resourceId: '',
  description: ''
})

const resourceForm = ref({
  name: '',
  code: '',
  type: 'MENU',
  parentId: '',
  url: '',
  description: ''
})

const roleColumns = [
  { title: '角色名称', dataIndex: 'name', key: 'name' },
  { title: '角色代码', dataIndex: 'code', key: 'code' },
  { title: '描述', dataIndex: 'description', key: 'description' },
  { title: '状态', dataIndex: 'enabled', key: 'enabled' },
  { title: '创建时间', dataIndex: 'createdAt', key: 'createdAt' },
  { title: '操作', key: 'action', width: 200 }
]

const permissionColumns = [
  { title: '权限名称', dataIndex: 'name', key: 'name' },
  { title: '权限代码', dataIndex: 'code', key: 'code' },
  { title: '资源', dataIndex: 'resourceName', key: 'resourceName' },
  { title: '描述', dataIndex: 'description', key: 'description' },
  { title: '操作', key: 'action', width: 150 }
]

const resourceColumns = [
  { title: '资源名称', dataIndex: 'name', key: 'name' },
  { title: '资源代码', dataIndex: 'code', key: 'code' },
  { title: '资源类型', dataIndex: 'type', key: 'type' },
  { title: '资源路径', dataIndex: 'url', key: 'url' },
  { title: '描述', dataIndex: 'description', key: 'description' },
  { title: '操作', key: 'action', width: 150 }
]

const allRolesForTransfer = computed(() => {
  return roles.value.map(role => ({
    key: role.id,
    title: role.name,
    disabled: false
  }))
})

const allPermissionsForTransfer = computed(() => {
  return permissions.value.map(perm => ({
    key: perm.id,
    title: perm.name,
    disabled: false
  }))
})

onMounted(() => {
  loadRoles()
  loadPermissions()
  loadResources()
})

const loadRoles = async () => {
  loadingRoles.value = true
  try {
    const data = await rbacApi.getRoles()
    roles.value = Array.isArray(data) ? data : []
  } catch (error) {
    message.error('加载角色失败')
    console.error(error)
  } finally {
    loadingRoles.value = false
  }
}

const loadPermissions = async () => {
  loadingPermissions.value = true
  try {
    const data = await rbacApi.getPermissions()
    permissions.value = Array.isArray(data) ? data : []
  } catch (error) {
    message.error('加载权限失败')
    console.error(error)
  } finally {
    loadingPermissions.value = false
  }
}

const loadResources = async () => {
  loadingResources.value = true
  try {
    const [data, tree] = await Promise.all([
      rbacApi.getResources(),
      rbacApi.getResourceTree()
    ])
    resources.value = Array.isArray(data) ? data : []
    resourceTree.value = buildTreeData(Array.isArray(tree) ? tree : [])
  } catch (error) {
    message.error('加载资源失败')
    console.error(error)
  } finally {
    loadingResources.value = false
  }
}

const buildTreeData = (data: any[]): any[] => {
  return data.map(item => ({
    value: item.id,
    title: item.name,
    key: item.id,
    children: item.children ? buildTreeData(item.children) : []
  }))
}

const getResourceTypeColor = (type: string) => {
  const colors: Record<string, string> = {
    MENU: 'blue',
    BUTTON: 'green',
    API: 'orange',
    DATA: 'purple'
  }
  return colors[type] || 'default'
}

const initRbac = async () => {
  initializing.value = true
  try {
    await rbacApi.initRbac()
    message.success('权限系统初始化成功')
    await Promise.all([loadRoles(), loadPermissions(), loadResources()])
  } catch (error) {
    message.error('初始化失败')
    console.error(error)
  } finally {
    initializing.value = false
  }
}

const showRoleModal = (role?: any) => {
  editingRole.value = role || null
  if (role) {
    roleForm.value = {
      name: role.name,
      code: role.code,
      description: role.description || '',
      enabled: role.enabled
    }
  } else {
    roleForm.value = {
      name: '',
      code: '',
      description: '',
      enabled: true
    }
  }
  roleModalVisible.value = true
}

const handleRoleSubmit = async () => {
  submittingRole.value = true
  try {
    if (editingRole.value) {
      await rbacApi.updateRole(editingRole.value.id, roleForm.value)
      message.success('角色更新成功')
    } else {
      await rbacApi.createRole(roleForm.value)
      message.success('角色创建成功')
    }
    roleModalVisible.value = false
    await loadRoles()
  } catch (error) {
    message.error('操作失败')
    console.error(error)
  } finally {
    submittingRole.value = false
  }
}

const deleteRole = async (roleId: string) => {
  try {
    await rbacApi.deleteRole(roleId)
    message.success('角色已删除')
    await loadRoles()
  } catch (error) {
    message.error('删除失败')
    console.error(error)
  }
}

const showPermissionModal = (permission?: any) => {
  editingPermission.value = permission || null
  if (permission) {
    permissionForm.value = {
      name: permission.name,
      code: permission.code,
      resourceId: permission.resourceId || '',
      description: permission.description || ''
    }
  } else {
    permissionForm.value = {
      name: '',
      code: '',
      resourceId: '',
      description: ''
    }
  }
  permissionModalVisible.value = true
}

const handlePermissionSubmit = async () => {
  submittingPermission.value = true
  try {
    if (editingPermission.value) {
      await rbacApi.updatePermission(editingPermission.value.id, permissionForm.value)
      message.success('权限更新成功')
    } else {
      await rbacApi.createPermission(permissionForm.value)
      message.success('权限创建成功')
    }
    permissionModalVisible.value = false
    await loadPermissions()
  } catch (error) {
    message.error('操作失败')
    console.error(error)
  } finally {
    submittingPermission.value = false
  }
}

const deletePermission = async (permissionId: string) => {
  try {
    await rbacApi.deletePermission(permissionId)
    message.success('权限已删除')
    await loadPermissions()
  } catch (error) {
    message.error('删除失败')
    console.error(error)
  }
}

const showResourceModal = (resource?: any) => {
  editingResource.value = resource || null
  if (resource) {
    resourceForm.value = {
      name: resource.name,
      code: resource.code,
      type: resource.type,
      parentId: resource.parentId || '',
      url: resource.url || '',
      description: resource.description || ''
    }
  } else {
    resourceForm.value = {
      name: '',
      code: '',
      type: 'MENU',
      parentId: '',
      url: '',
      description: ''
    }
  }
  resourceModalVisible.value = true
}

const handleResourceSubmit = async () => {
  submittingResource.value = true
  try {
    if (editingResource.value) {
      await rbacApi.updateResource(editingResource.value.id, resourceForm.value)
      message.success('资源更新成功')
    } else {
      await rbacApi.createResource(resourceForm.value)
      message.success('资源创建成功')
    }
    resourceModalVisible.value = false
    await loadResources()
  } catch (error) {
    message.error('操作失败')
    console.error(error)
  } finally {
    submittingResource.value = false
  }
}

const deleteResource = async (resourceId: string) => {
  try {
    await rbacApi.deleteResource(resourceId)
    message.success('资源已删除')
    await loadResources()
  } catch (error) {
    message.error('删除失败')
    console.error(error)
  }
}

const searchUserRoles = async () => {
  if (!searchUsername.value.trim()) {
    message.warning('请输入用户名')
    return
  }

  searchingUser.value = true
  try {
    const [rolesData, permsData] = await Promise.all([
      rbacApi.getUserRoles(searchUsername.value),
      rbacApi.getUserPermissions(searchUsername.value)
    ])
    
    currentUserRoles.value = {
      roles: Array.isArray(rolesData) ? rolesData : [],
      permissions: Array.isArray(permsData) ? permsData : []
    }
    
    selectedRoleIds.value = currentUserRoles.value.roles.map((r: any) => r.id)
  } catch (error) {
    message.error('查询失败')
    console.error(error)
  } finally {
    searchingUser.value = false
  }
}

const handleRoleTransfer = async (targetKeys: string[]) => {
  try {
    await rbacApi.updateUserRoles(searchUsername.value, targetKeys)
    message.success('角色分配成功')
    selectedRoleIds.value = targetKeys
    await searchUserRoles()
  } catch (error) {
    message.error('角色分配失败')
    console.error(error)
  }
}

const showRolePermissions = async (role: any) => {
  currentRole.value = role
  try {
    const data = await rbacApi.getRolePermissions(role.id)
    selectedPermissionIds.value = Array.isArray(data) ? data.map((p: any) => p.id) : []
    rolePermissionModalVisible.value = true
  } catch (error) {
    message.error('获取角色权限失败')
    console.error(error)
  }
}

const handleRolePermissionSubmit = async () => {
  if (!currentRole.value) return
  
  submittingRolePermissions.value = true
  try {
    await rbacApi.assignRolePermissions(currentRole.value.id, selectedPermissionIds.value)
    message.success('权限配置成功')
    rolePermissionModalVisible.value = false
  } catch (error) {
    message.error('权限配置失败')
    console.error(error)
  } finally {
    submittingRolePermissions.value = false
  }
}
</script>

<style scoped>
.rbac-view {
  padding: 24px;
}

:deep(.ant-card) {
  margin-bottom: 16px;
}

:deep(.ant-card-head-title) {
  font-weight: 600;
}
</style>
