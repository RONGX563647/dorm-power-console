<template>
    <view class="register-container">
        <view class="bg-decoration">
            <view class="circle circle-1"></view>
            <view class="circle circle-2"></view>
        </view>
        
        <view class="register-header">
            <view class="back-btn" @click="goToLogin">
                <uni-icons type="left" size="20" color="#fff"></uni-icons>
            </view>
            <text class="title">创建账号</text>
            <text class="subtitle">加入我们，开启智能节能生活</text>
        </view>
        
        <scroll-view scroll-y class="form-scroll">
            <view class="register-form">
                <view class="form-section">
                    <view class="section-title">
                        <uni-icons type="person" size="18" color="#2A7965"></uni-icons>
                        <text>基本信息</text>
                    </view>
                    
                    <uni-forms ref="form" :modelValue="formData" :rules="rules">
                        <uni-forms-item name="username">
                            <view class="input-wrapper">
                                <uni-icons type="person" size="20" color="#66A392"></uni-icons>
                                <uni-easyinput 
                                    v-model="formData.username" 
                                    placeholder="请输入用户名（至少3个字符）"
                                    :inputBorder="false"
                                    :styles="inputStyles"
                                />
                            </view>
                        </uni-forms-item>
                        
                        <uni-forms-item name="email">
                            <view class="input-wrapper">
                                <uni-icons type="email" size="20" color="#66A392"></uni-icons>
                                <uni-easyinput 
                                    v-model="formData.email" 
                                    placeholder="请输入邮箱地址"
                                    :inputBorder="false"
                                    :styles="inputStyles"
                                />
                            </view>
                        </uni-forms-item>
                    </uni-forms>
                </view>
                
                <view class="form-section">
                    <view class="section-title">
                        <uni-icons type="locked" size="18" color="#2A7965"></uni-icons>
                        <text>设置密码</text>
                    </view>
                    
                    <uni-forms ref="form" :modelValue="formData" :rules="rules">
                        <uni-forms-item name="password">
                            <view class="input-wrapper">
                                <uni-icons type="locked" size="20" color="#66A392"></uni-icons>
                                <uni-easyinput 
                                    v-model="formData.password" 
                                    type="password" 
                                    placeholder="请输入密码（至少6位）"
                                    :inputBorder="false"
                                    :styles="inputStyles"
                                />
                            </view>
                        </uni-forms-item>
                        
                        <uni-forms-item name="confirmPassword">
                            <view class="input-wrapper">
                                <uni-icons type="locked" size="20" color="#66A392"></uni-icons>
                                <uni-easyinput 
                                    v-model="formData.confirmPassword" 
                                    type="password" 
                                    placeholder="请再次输入密码"
                                    :inputBorder="false"
                                    :styles="inputStyles"
                                />
                            </view>
                        </uni-forms-item>
                    </uni-forms>
                    
                    <view class="password-tips">
                        <view class="tip-item" :class="{ active: passwordStrength >= 1 }">
                            <view class="tip-dot"></view>
                            <text>至少6个字符</text>
                        </view>
                        <view class="tip-item" :class="{ active: passwordStrength >= 2 }">
                            <view class="tip-dot"></view>
                            <text>包含数字和字母</text>
                        </view>
                        <view class="tip-item" :class="{ active: passwordStrength >= 3 }">
                            <view class="tip-dot"></view>
                            <text>包含特殊字符</text>
                        </view>
                    </view>
                </view>
                
                <view class="agreement">
                    <checkbox :checked="agreed" @click="agreed = !agreed" color="#2A7965" />
                    <text class="agreement-text">
                        我已阅读并同意
                        <text class="link" @click.stop="showAgreement('user')">《用户协议》</text>
                        和
                        <text class="link" @click.stop="showAgreement('privacy')">《隐私政策》</text>
                    </text>
                </view>
                
                <button class="register-btn" :class="{ 'btn-loading': loading }" @click="handleRegister" :disabled="loading || !agreed">
                    <text v-if="!loading">立即注册</text>
                    <view v-else class="loading-content">
                        <view class="loading-spinner"></view>
                        <text>注册中...</text>
                    </view>
                </button>
                
                <view class="link-row">
                    <text class="link" @click="goToLogin">
                        <uni-icons type="arrow-left" size="14" color="#2A7965"></uni-icons>
                        已有账号？返回登录
                    </text>
                </view>
            </view>
        </scroll-view>
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
                confirmPassword: ''
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
            loading: false,
            agreed: false,
            inputStyles: {
                backgroundColor: 'transparent',
                color: '#2A7965'
            }
        }
    },
    computed: {
        passwordStrength() {
            const password = this.formData.password
            let strength = 0
            if (password.length >= 6) strength++
            if (/[0-9]/.test(password) && /[a-zA-Z]/.test(password)) strength++
            if (/[^0-9a-zA-Z]/.test(password)) strength++
            return strength
        }
    },
    methods: {
        async handleRegister() {
            if (!this.agreed) {
                uni.showToast({
                    title: '请先同意用户协议',
                    icon: 'none'
                })
                return
            }
            
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
                    title: '注册成功',
                    icon: 'success'
                })
                
                setTimeout(() => {
                    uni.navigateBack()
                }, 1500)
            } catch (error) {
                console.error('注册失败:', error)
                uni.showToast({
                    title: error.message || '注册失败',
                    icon: 'none'
                })
            } finally {
                this.loading = false
            }
        },
        
        goToLogin() {
            uni.navigateBack()
        },
        
        showAgreement(type) {
            uni.showToast({
                title: type === 'user' ? '用户协议' : '隐私政策',
                icon: 'none'
            })
        }
    }
}
</script>

<style lang="scss" scoped>
.register-container {
    min-height: 100vh;
    background: linear-gradient(135deg, #2A7965 0%, #3D967E 100%);
    position: relative;
    overflow: hidden;
    display: flex;
    flex-direction: column;
}

.bg-decoration {
    position: absolute;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    pointer-events: none;
    
    .circle {
        position: absolute;
        border-radius: 50%;
        background: rgba(255, 255, 255, 0.1);
    }
    
    .circle-1 {
        width: 300rpx;
        height: 300rpx;
        top: -100rpx;
        left: -100rpx;
        animation: float 8s ease-in-out infinite;
    }
    
    .circle-2 {
        width: 400rpx;
        height: 400rpx;
        bottom: -150rpx;
        right: -150rpx;
        animation: float 10s ease-in-out infinite reverse;
    }
}

@keyframes float {
    0%, 100% {
        transform: translateY(0) scale(1);
    }
    50% {
        transform: translateY(-30rpx) scale(1.05);
    }
}

.register-header {
    padding: 60rpx 40rpx 40rpx;
    position: relative;
    z-index: 1;
    
    .back-btn {
        width: 64rpx;
        height: 64rpx;
        background: rgba(255, 255, 255, 0.2);
        border-radius: 50%;
        display: flex;
        align-items: center;
        justify-content: center;
        margin-bottom: 24rpx;
        backdrop-filter: blur(10px);
    }
    
    .title {
        display: block;
        font-size: 48rpx;
        font-weight: 700;
        color: #fff;
        margin-bottom: 12rpx;
    }
    
    .subtitle {
        display: block;
        font-size: 26rpx;
        color: rgba(255, 255, 255, 0.8);
    }
}

.form-scroll {
    flex: 1;
    position: relative;
    z-index: 1;
}

.register-form {
    background: #fff;
    border-radius: 32rpx 32rpx 0 0;
    padding: 40rpx;
    min-height: 100%;
    
    .form-section {
        margin-bottom: 40rpx;
        
        .section-title {
            display: flex;
            align-items: center;
            gap: 8rpx;
            font-size: 28rpx;
            font-weight: 600;
            color: #2A7965;
            margin-bottom: 24rpx;
        }
    }
    
    .input-wrapper {
        display: flex;
        align-items: center;
        background: #F2F7F5;
        border-radius: 24rpx;
        padding: 0 24rpx;
        margin-bottom: 20rpx;
        border: 2rpx solid transparent;
        transition: all 0.3s ease;
        
        &:focus-within {
            background: #fff;
            border-color: #2A7965;
            box-shadow: 0 4rpx 16rpx rgba(42, 121, 101, 0.15);
        }
    }
    
    .password-tips {
        display: flex;
        flex-wrap: wrap;
        gap: 16rpx;
        margin-top: 16rpx;
        
        .tip-item {
            display: flex;
            align-items: center;
            gap: 8rpx;
            font-size: 22rpx;
            color: #999;
            
            .tip-dot {
                width: 12rpx;
                height: 12rpx;
                border-radius: 50%;
                background: #E8F5F0;
                transition: all 0.3s ease;
            }
            
            &.active {
                color: #2A7965;
                
                .tip-dot {
                    background: #2A7965;
                }
            }
        }
    }
    
    .agreement {
        display: flex;
        align-items: flex-start;
        gap: 12rpx;
        margin: 32rpx 0;
        
        checkbox {
            margin-top: 4rpx;
        }
        
        .agreement-text {
            flex: 1;
            font-size: 24rpx;
            color: #66A392;
            line-height: 1.6;
            
            .link {
                color: #2A7965;
                font-weight: 500;
            }
        }
    }
    
    .register-btn {
        margin-top: 32rpx;
        border-radius: 50rpx;
        height: 96rpx;
        line-height: 96rpx;
        font-size: 32rpx;
        font-weight: 600;
        background: linear-gradient(135deg, #2A7965 0%, #3D967E 100%);
        color: #fff;
        box-shadow: 0 8rpx 24rpx rgba(42, 121, 101, 0.3);
        transition: all 0.3s ease;
        
        &:active {
            transform: scale(0.98);
            box-shadow: 0 4rpx 12rpx rgba(42, 121, 101, 0.3);
        }
        
        &.btn-loading {
            background: linear-gradient(135deg, #3D967E 0%, #4AAB8F 100%);
        }
        
        &[disabled] {
            opacity: 0.5;
        }
        
        .loading-content {
            display: flex;
            align-items: center;
            justify-content: center;
            gap: 12rpx;
            
            .loading-spinner {
                width: 32rpx;
                height: 32rpx;
                border: 4rpx solid rgba(255, 255, 255, 0.3);
                border-top-color: #fff;
                border-radius: 50%;
                animation: spin 0.8s linear infinite;
            }
        }
    }
    
    .link-row {
        text-align: center;
        margin-top: 32rpx;
        padding-bottom: 40rpx;
        
        .link {
            display: inline-flex;
            align-items: center;
            gap: 4rpx;
            color: #2A7965;
            font-size: 26rpx;
            transition: all 0.3s ease;
            
            &:active {
                opacity: 0.7;
            }
        }
    }
}

@keyframes spin {
    to {
        transform: rotate(360deg);
    }
}
</style>
