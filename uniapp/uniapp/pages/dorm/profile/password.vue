<template>
    <view class="password-container">
        <view class="form-section">
            <uni-forms ref="form" :modelValue="formData" :rules="rules">
                <uni-forms-item label="当前密码" name="oldPassword">
                    <uni-easyinput v-model="formData.oldPassword" type="password" placeholder="请输入当前密码" />
                </uni-forms-item>
                
                <uni-forms-item label="新密码" name="newPassword">
                    <uni-easyinput v-model="formData.newPassword" type="password" placeholder="请输入新密码" />
                </uni-forms-item>
                
                <uni-forms-item label="确认密码" name="confirmPassword">
                    <uni-easyinput v-model="formData.confirmPassword" type="password" placeholder="请确认新密码" />
                </uni-forms-item>
            </uni-forms>
            
            <button class="submit-btn" type="primary" @click="handleSubmit" :loading="loading">修改密码</button>
        </view>
    </view>
</template>

<script>
import api from '@/api'
import { mapState } from 'vuex'

export default {
    data() {
        return {
            formData: {
                oldPassword: '',
                newPassword: '',
                confirmPassword: ''
            },
            rules: {
                oldPassword: {
                    rules: [{ required: true, errorMessage: '请输入当前密码' }]
                },
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
                                if (value !== this.formData.newPassword) {
                                    callback('两次密码输入不一致')
                                }
                                return true
                            }
                        }
                    ]
                }
            },
            loading: false
        }
    },
    computed: {
        ...mapState(['user'])
    },
    methods: {
        async handleSubmit() {
            try {
                const valid = await this.$refs.form.validate()
                if (!valid) return
                
                this.loading = true
                await api.user.changePassword(this.user.id, {
                    oldPassword: this.formData.oldPassword,
                    password: this.formData.newPassword
                })
                
                uni.showToast({
                    title: '密码修改成功',
                    icon: 'success'
                })
                
                setTimeout(() => {
                    uni.navigateBack()
                }, 1500)
            } catch (error) {
                console.error('修改密码失败:', error)
            } finally {
                this.loading = false
            }
        }
    }
}
</script>

<style lang="scss" scoped>
.password-container {
    min-height: 100vh;
    background: #F2F7F5;
    padding: 20rpx;
}

.form-section {
    background: #fff;
    border-radius: 16rpx;
    padding: 30rpx;
    
    .submit-btn {
        margin-top: 40rpx;
        border-radius: 50rpx;
        height: 88rpx;
        line-height: 88rpx;
        font-size: 32rpx;
    }
}
</style>
