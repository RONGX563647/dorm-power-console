<template>
    <view class="user-detail-container">
        <view class="user-header" v-if="user">
            <view class="user-avatar">
                <uni-icons type="person-filled" size="48" color="#fff"></uni-icons>
            </view>
            <view class="user-info">
                <text class="user-name">{{ user.username }}</text>
                <text class="user-email">{{ user.email }}</text>
            </view>
            <view class="user-role" :class="user.role.toLowerCase()">
                {{ getRoleText(user.role) }}
            </view>
        </view>
        
        <view class="form-section">
            <view class="section-title">基本信息</view>
            
            <uni-forms ref="form" :modelValue="formData" :rules="rules">
                <uni-forms-item label="用户名" name="username">
                    <uni-easyinput v-model="formData.username" placeholder="请输入用户名" />
                </uni-forms-item>
                
                <uni-forms-item label="邮箱" name="email">
                    <uni-easyinput v-model="formData.email" placeholder="请输入邮箱" />
                </uni-forms-item>
                
                <uni-forms-item label="角色" name="role">
                    <picker mode="selector" :range="roleOptions" range-key="label" @change="onRoleChange">
                        <view class="picker-value">
                            {{ getRoleText(formData.role) }}
                            <uni-icons type="arrowdown" size="16" color="#666"></uni-icons>
                        </view>
                    </picker>
                </uni-forms-item>
            </uni-forms>
            
            <button class="save-btn" type="primary" @click="handleSave" :loading="loading">保存修改</button>
        </view>
        
        <view class="password-section">
            <view class="section-title">修改密码</view>
            
            <uni-forms ref="pwdForm" :modelValue="pwdData" :rules="pwdRules">
                <uni-forms-item label="新密码" name="newPassword">
                    <uni-easyinput v-model="pwdData.newPassword" type="password" placeholder="请输入新密码" />
                </uni-forms-item>
                
                <uni-forms-item label="确认密码" name="confirmPassword">
                    <uni-easyinput v-model="pwdData.confirmPassword" type="password" placeholder="请确认新密码" />
                </uni-forms-item>
            </uni-forms>
            
            <button class="pwd-btn" @click="handleChangePassword" :loading="pwdLoading">修改密码</button>
        </view>
    </view>
</template>

<script>
import api from '@/api'

export default {
    data() {
        return {
            userId: '',
            user: null,
            formData: {
                username: '',
                email: '',
                role: ''
            },
            rules: {
                username: {
                    rules: [{ required: true, errorMessage: '请输入用户名' }]
                },
                email: {
                    rules: [
                        { required: true, errorMessage: '请输入邮箱' },
                        { format: 'email', errorMessage: '邮箱格式不正确' }
                    ]
                }
            },
            pwdData: {
                newPassword: '',
                confirmPassword: ''
            },
            pwdRules: {
                newPassword: {
                    rules: [
                        { required: true, errorMessage: '请输入新密码' },
                        { minLength: 6, errorMessage: '密码至少6个字符' }
                    ]
                },
                confirmPassword: {
                    rules: [
                        { required: true, errorMessage: '请确认密码' },
                        {
                            validateFunction: (rule, value, data, callback) => {
                                if (value !== this.pwdData.newPassword) {
                                    callback('两次密码输入不一致')
                                }
                                return true
                            }
                        }
                    ]
                }
            },
            roleOptions: [
                { value: 'ADMIN', label: '管理员' },
                { value: 'USER', label: '普通用户' }
            ],
            loading: false,
            pwdLoading: false
        }
    },
    onLoad(options) {
        this.userId = options.id
        this.loadUser()
    },
    methods: {
        async loadUser() {
            try {
                this.user = await api.user.getUserDetail(this.userId)
                this.formData = {
                    username: this.user.username,
                    email: this.user.email,
                    role: this.user.role
                }
            } catch (error) {
                console.error('加载用户失败:', error)
            }
        },
        
        async handleSave() {
            try {
                const valid = await this.$refs.form.validate()
                if (!valid) return
                
                this.loading = true
                await api.user.updateUser(this.userId, this.formData)
                
                uni.showToast({
                    title: '保存成功',
                    icon: 'success'
                })
                
                this.loadUser()
            } catch (error) {
                console.error('保存失败:', error)
            } finally {
                this.loading = false
            }
        },
        
        async handleChangePassword() {
            try {
                const valid = await this.$refs.pwdForm.validate()
                if (!valid) return
                
                this.pwdLoading = true
                await api.user.changePassword(this.userId, {
                    password: this.pwdData.newPassword
                })
                
                uni.showToast({
                    title: '密码修改成功',
                    icon: 'success'
                })
                
                this.pwdData = {
                    newPassword: '',
                    confirmPassword: ''
                }
            } catch (error) {
                console.error('修改密码失败:', error)
            } finally {
                this.pwdLoading = false
            }
        },
        
        onRoleChange(e) {
            const index = e.detail.value
            this.formData.role = this.roleOptions[index].value
        },
        
        getRoleText(role) {
            const map = {
                'ADMIN': '管理员',
                'USER': '普通用户'
            }
            return map[role] || role
        }
    }
}
</script>

<style lang="scss" scoped>
.user-detail-container {
    min-height: 100vh;
    background: #F2F7F5;
    padding-bottom: 40rpx;
}

.user-header {
    background: linear-gradient(135deg, #2A7965 0%, #3D967E 100%);
    padding: 40rpx 30rpx;
    display: flex;
    align-items: center;
    
    .user-avatar {
        width: 100rpx;
        height: 100rpx;
        border-radius: 50%;
        background: rgba(255, 255, 255, 0.2);
        display: flex;
        align-items: center;
        justify-content: center;
        margin-right: 24rpx;
    }
    
    .user-info {
        flex: 1;
        
        .user-name {
            display: block;
            font-size: 36rpx;
            font-weight: bold;
            color: #fff;
            margin-bottom: 8rpx;
        }
        
        .user-email {
            font-size: 26rpx;
            color: rgba(255, 255, 255, 0.8);
        }
    }
    
    .user-role {
        font-size: 24rpx;
        color: #fff;
        padding: 8rpx 24rpx;
        border-radius: 20rpx;
        background: rgba(255, 255, 255, 0.2);
        
        &.admin {
            background: rgba(255, 255, 255, 0.3);
        }
    }
}

.form-section, .password-section {
    background: #fff;
    margin: 20rpx;
    border-radius: 16rpx;
    padding: 30rpx;
    
    .section-title {
        font-size: 30rpx;
        font-weight: bold;
        color: #333;
        margin-bottom: 30rpx;
    }
    
    .picker-value {
        display: flex;
        justify-content: space-between;
        align-items: center;
        padding: 20rpx;
        background: #F2F7F5;
        border-radius: 8rpx;
        font-size: 28rpx;
        color: #333;
    }
    
    .save-btn, .pwd-btn {
        margin-top: 30rpx;
        border-radius: 50rpx;
        height: 88rpx;
        line-height: 88rpx;
        font-size: 32rpx;
    }
    
    .pwd-btn {
        background: #F2F7F5;
        color: #666;
    }
}
</style>
