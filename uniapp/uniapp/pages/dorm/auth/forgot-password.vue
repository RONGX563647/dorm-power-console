<template>
    <view class="forgot-container">
        <view class="forgot-header">
            <text class="title">忘记密码</text>
            <text class="desc">请输入您的注册邮箱，我们将发送重置密码链接</text>
        </view>
        
        <view class="forgot-form">
            <uni-forms ref="form" :modelValue="formData" :rules="rules">
                <uni-forms-item name="email">
                    <uni-easyinput 
                        v-model="formData.email" 
                        placeholder="请输入注册邮箱"
                        prefixIcon="email"
                    />
                </uni-forms-item>
            </uni-forms>
            
            <button class="submit-btn" type="primary" @click="handleSubmit" :loading="loading">发送重置链接</button>
            
            <view class="link-row">
                <text class="link" @click="goToLogin">返回登录</text>
            </view>
        </view>
    </view>
</template>

<script>
import api from '@/api'

export default {
    data() {
        return {
            formData: {
                email: ''
            },
            rules: {
                email: {
                    rules: [
                        { required: true, errorMessage: '请输入邮箱' },
                        { format: 'email', errorMessage: '邮箱格式不正确' }
                    ]
                }
            },
            loading: false
        }
    },
    methods: {
        async handleSubmit() {
            try {
                const valid = await this.$refs.form.validate()
                if (!valid) return
                
                this.loading = true
                await api.auth.forgotPassword(this.formData)
                
                uni.showToast({
                    title: '重置链接已发送',
                    icon: 'success'
                })
                
                setTimeout(() => {
                    uni.navigateBack()
                }, 1500)
            } catch (error) {
                console.error('发送失败:', error)
            } finally {
                this.loading = false
            }
        },
        
        goToLogin() {
            uni.navigateBack()
        }
    }
}
</script>

<style lang="scss" scoped>
.forgot-container {
    min-height: 100vh;
    padding: 40rpx;
    background: #F2F7F5;
}

.forgot-header {
    text-align: center;
    padding: 60rpx 0;
    
    .title {
        display: block;
        font-size: 40rpx;
        font-weight: bold;
        color: #333;
        margin-bottom: 20rpx;
    }
    
    .desc {
        font-size: 28rpx;
        color: #999;
    }
}

.forgot-form {
    background: #fff;
    border-radius: 20rpx;
    padding: 40rpx;
    
    .submit-btn {
        margin-top: 40rpx;
        border-radius: 50rpx;
        height: 88rpx;
        line-height: 88rpx;
        font-size: 32rpx;
    }
    
    .link-row {
        text-align: center;
        margin-top: 30rpx;
        
        .link {
            color: #2A7965;
            font-size: 28rpx;
        }
    }
}
</style>
