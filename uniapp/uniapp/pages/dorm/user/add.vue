<template>
    <view class="add-user-container">
        <view class="form-section">
            <uni-forms ref="form" :modelValue="formData" :rules="rules">
                <uni-forms-item label="用户名" name="username">
                    <uni-easyinput v-model="formData.username" placeholder="请输入用户名" />
                </uni-forms-item>
                
                <uni-forms-item label="邮箱" name="email">
                    <uni-easyinput v-model="formData.email" placeholder="请输入邮箱" />
                </uni-forms-item>
                
                <uni-forms-item label="密码" name="password">
                    <uni-easyinput v-model="formData.password" type="password" placeholder="请输入密码" />
                </uni-forms-item>
                
                <uni-forms-item label="确认密码" name="confirmPassword">
                    <uni-easyinput v-model="formData.confirmPassword" type="password" placeholder="请确认密码" />
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
            
            <button class="submit-btn" type="primary" @click="handleSubmit" :loading="loading">创建用户</button>
        </view>
    </view>
</template>

<script>
import api from '@/api'

export default {
    data() {
        return {
            formData: {
                username: '',
                email: '',
                password: '',
                confirmPassword: '',
                role: 'USER'
            },
            rules: {
                username: {
                    rules: [
                        { required: true, errorMessage: '请输入用户名' },
                        { minLength: 3, errorMessage: '用户名至少3个字符' }
                    ]
                },
                email: {
                    rules: [
                        { required: true, errorMessage: '请输入邮箱' },
                        { format: 'email', errorMessage: '邮箱格式不正确' }
                    ]
                },
                password: {
                    rules: [
                        { required: true, errorMessage: '请输入密码' },
                        { minLength: 6, errorMessage: '密码至少6个字符' }
                    ]
                },
                confirmPassword: {
                    rules: [
                        { required: true, errorMessage: '请确认密码' },
                        {
                            validateFunction: (rule, value, data, callback) => {
                                if (value !== this.formData.password) {
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
            loading: false
        }
    },
    methods: {
        async handleSubmit() {
            try {
                const valid = await this.$refs.form.validate()
                if (!valid) return
                
                this.loading = true
                await api.auth.register({
                    username: this.formData.username,
                    email: this.formData.email,
                    password: this.formData.password
                })
                
                uni.showToast({
                    title: '创建成功',
                    icon: 'success'
                })
                
                setTimeout(() => {
                    uni.navigateBack()
                }, 1500)
            } catch (error) {
                console.error('创建失败:', error)
            } finally {
                this.loading = false
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
.add-user-container {
    min-height: 100vh;
    background: #F2F7F5;
    padding: 20rpx;
}

.form-section {
    background: #fff;
    border-radius: 16rpx;
    padding: 30rpx;
    
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
    
    .submit-btn {
        margin-top: 40rpx;
        border-radius: 50rpx;
        height: 88rpx;
        line-height: 88rpx;
        font-size: 32rpx;
    }
}
</style>
