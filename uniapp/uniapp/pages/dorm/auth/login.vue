<template>
    <view class="cyber-container">
        <view class="cyber-grid"></view>
        <view class="cyber-glow"></view>
        
        <view class="login-content">
            <view class="login-header">
                <view class="logo-hologram">
                    <view class="holo-ring ring-1"></view>
                    <view class="holo-ring ring-2"></view>
                    <view class="holo-ring ring-3"></view>
                    <view class="logo-core">
                        <image class="logo-img" src="/static/logo.png" mode="aspectFit"></image>
                    </view>
                </view>
                <view class="title-wrapper">
                    <text class="title" data-text="DORMITORY POWER SYSTEM">DORMITORY POWER SYSTEM</text>
                    <view class="title-line"></view>
                </view>
                <text class="subtitle">SMART ENERGY · GREEN LIFE</text>
            </view>
            
            <view class="login-form">
                <view class="form-header">
                    <view class="header-line">
                        <text class="line-label">AUTHENTICATION</text>
                        <view class="line-decoration"></view>
                    </view>
                </view>
                
                <uni-forms ref="form" :modelValue="formData" :rules="rules">
                    <uni-forms-item name="account">
                        <view class="input-hologram">
                            <view class="input-frame">
                                <view class="frame-corner corner-tl"></view>
                                <view class="frame-corner corner-tr"></view>
                                <view class="frame-corner corner-bl"></view>
                                <view class="frame-corner corner-br"></view>
                            </view>
                            <view class="input-content">
                                <view class="input-icon">
                                    <uni-icons type="person" size="20" color="#00F5FF"></uni-icons>
                                </view>
                                <uni-easyinput 
                                    v-model="formData.account" 
                                    placeholder="ENTER ACCOUNT"
                                    :inputBorder="false"
                                    :styles="inputStyles"
                                />
                            </view>
                        </view>
                    </uni-forms-item>
                    
                    <uni-forms-item name="password">
                        <view class="input-hologram">
                            <view class="input-frame">
                                <view class="frame-corner corner-tl"></view>
                                <view class="frame-corner corner-tr"></view>
                                <view class="frame-corner corner-bl"></view>
                                <view class="frame-corner corner-br"></view>
                            </view>
                            <view class="input-content">
                                <view class="input-icon">
                                    <uni-icons type="locked" size="20" color="#00F5FF"></uni-icons>
                                </view>
                                <uni-easyinput 
                                    v-model="formData.password" 
                                    type="password" 
                                    placeholder="ENTER PASSWORD"
                                    :inputBorder="false"
                                    :styles="inputStyles"
                                />
                            </view>
                        </view>
                    </uni-forms-item>
                </uni-forms>
                
                <button class="login-btn" :class="{ 'btn-loading': loading }" @click="handleLogin" :disabled="loading">
                    <text v-if="!loading" class="btn-text">AUTHENTICATE</text>
                    <view v-else class="loading-content">
                        <view class="loading-spinner"></view>
                        <text class="btn-text">PROCESSING...</text>
                    </view>
                </button>
                
                <view class="link-row">
                    <text class="link" @click="goToRegister">
                        <uni-icons type="plus" size="14" color="#00F5FF"></uni-icons>
                        <text class="link-text">CREATE ACCOUNT</text>
                    </text>
                    <text class="link" @click="goToForgotPassword">
                        <uni-icons type="help" size="14" color="#00F5FF"></uni-icons>
                        <text class="link-text">FORGOT PASSWORD?</text>
                    </text>
                </view>
            </view>
            
            <view class="footer">
                <text class="footer-text">© 2026 DORMITORY POWER SYSTEM · ECO-FRIENDLY</text>
            </view>
        </view>
    </view>
</template>

<script>
import { mapActions } from 'vuex'

export default {
    data() {
        return {
            formData: {
                account: '',
                password: ''
            },
            rules: {
                account: {
                    rules: [{ required: true, errorMessage: 'PLEASE ENTER ACCOUNT' }]
                },
                password: {
                    rules: [{ required: true, errorMessage: 'PLEASE ENTER PASSWORD' }]
                }
            },
            loading: false,
            inputStyles: {
                backgroundColor: 'transparent',
                color: '#00F5FF'
            }
        }
    },
    methods: {
        ...mapActions(['loginAction']),
        
        async handleLogin() {
            try {
                const valid = await this.$refs.form.validate()
                if (!valid) return
                
                this.loading = true
                
                await this.loginAction({
                    account: this.formData.account,
                    password: this.formData.password
                })
                
                uni.showToast({
                    title: 'LOGIN SUCCESS',
                    icon: 'success'
                })
                
                setTimeout(() => {
                    uni.switchTab({ url: '/pages/dorm/index/index' })
                }, 1000)
            } catch (e) {
                console.error('Login failed:', e)
                uni.showToast({
                    title: e.message || 'LOGIN FAILED',
                    icon: 'none'
                })
            } finally {
                this.loading = false
            }
        },
        
        goToRegister() {
            uni.navigateTo({ url: '/pages/dorm/auth/register' })
        },
        
        goToForgotPassword() {
            uni.showToast({
                title: 'PLEASE CONTACT ADMIN',
                icon: 'none'
            })
        }
    }
}
</script>

<style lang="scss" scoped>
.cyber-container {
    min-height: 100vh;
    background: linear-gradient(180deg, #0A0E27 0%, #0F1629 50%, #0A0E27 100%);
    position: relative;
    overflow: hidden;
    display: flex;
    align-items: center;
    justify-content: center;
}

.cyber-grid {
    position: absolute;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    background-image: 
        linear-gradient(rgba(0, 245, 255, 0.03) 1px, transparent 1px),
        linear-gradient(90deg, rgba(0, 245, 255, 0.03) 1px, transparent 1px);
    background-size: 40rpx 40rpx;
    animation: gridMove 20s linear infinite;
    pointer-events: none;
}

@keyframes gridMove {
    0% { transform: translateY(0); }
    100% { transform: translateY(40rpx); }
}

.cyber-glow {
    position: absolute;
    top: 50%;
    left: 50%;
    transform: translate(-50%, -50%);
    width: 800rpx;
    height: 800rpx;
    background: radial-gradient(circle, rgba(0, 245, 255, 0.15) 0%, transparent 70%);
    pointer-events: none;
}

.login-content {
    position: relative;
    width: 100%;
    padding: 0 48rpx;
    z-index: 10;
}

.login-header {
    text-align: center;
    margin-bottom: 64rpx;
    
    .logo-hologram {
        position: relative;
        width: 160rpx;
        height: 160rpx;
        margin: 0 auto 32rpx;
        
        .holo-ring {
            position: absolute;
            top: 50%;
            left: 50%;
            transform: translate(-50%, -50%);
            border: 2rpx solid rgba(0, 245, 255, 0.3);
            border-radius: 50%;
            
            &.ring-1 {
                width: 100%;
                height: 100%;
                animation: rotate 8s linear infinite;
            }
            
            &.ring-2 {
                width: 80%;
                height: 80%;
                animation: rotate 6s linear infinite reverse;
            }
            
            &.ring-3 {
                width: 60%;
                height: 60%;
                animation: rotate 4s linear infinite;
            }
        }
        
        .logo-core {
            position: absolute;
            top: 50%;
            left: 50%;
            transform: translate(-50%, -50%);
            width: 100rpx;
            height: 100rpx;
            
            .logo-img {
                width: 100%;
                height: 100%;
            }
        }
    }
    
    .title-wrapper {
        position: relative;
        margin-bottom: 16rpx;
        
        .title {
            font-size: 40rpx;
            font-weight: 700;
            color: #00F5FF;
            letter-spacing: 4rpx;
            text-shadow: 0 0 20rpx rgba(0, 245, 255, 0.5);
        }
        
        .title-line {
            position: absolute;
            bottom: -12rpx;
            left: 50%;
            transform: translateX(-50%);
            width: 80%;
            height: 2rpx;
            background: linear-gradient(90deg, transparent 0%, #00F5FF 50%, transparent 100%);
        }
    }
    
    .subtitle {
        font-size: 22rpx;
        color: rgba(0, 245, 255, 0.6);
        letter-spacing: 4rpx;
    }
}

@keyframes rotate {
    from { transform: translate(-50%, -50%) rotate(0deg); }
    to { transform: translate(-50%, -50%) rotate(360deg); }
}

.login-form {
    background: rgba(0, 245, 255, 0.03);
    border: 1rpx solid rgba(0, 245, 255, 0.1);
    border-radius: 24rpx;
    padding: 40rpx 32rpx;
    backdrop-filter: blur(10px);
    
    .form-header {
        margin-bottom: 32rpx;
        
        .header-line {
            display: flex;
            align-items: center;
            gap: 16rpx;
            
            .line-label {
                font-size: 24rpx;
                font-weight: 600;
                color: #00F5FF;
                letter-spacing: 4rpx;
            }
            
            .line-decoration {
                flex: 1;
                height: 1rpx;
                background: linear-gradient(90deg, rgba(0, 245, 255, 0.3) 0%, transparent 100%);
            }
        }
    }
    
    .input-hologram {
        position: relative;
        margin-bottom: 24rpx;
        
        .input-frame {
            position: absolute;
            top: -8rpx;
            left: -8rpx;
            right: -8rpx;
            bottom: -8rpx;
            
            .frame-corner {
                position: absolute;
                width: 20rpx;
                height: 20rpx;
                border: 2rpx solid rgba(0, 245, 255, 0.3);
                
                &.corner-tl {
                    top: 0;
                    left: 0;
                    border-right: none;
                    border-bottom: none;
                }
                
                &.corner-tr {
                    top: 0;
                    right: 0;
                    border-left: none;
                    border-bottom: none;
                }
                
                &.corner-bl {
                    bottom: 0;
                    left: 0;
                    border-right: none;
                    border-top: none;
                }
                
                &.corner-br {
                    bottom: 0;
                    right: 0;
                    border-left: none;
                    border-top: none;
                }
            }
        }
        
        .input-content {
            display: flex;
            align-items: center;
            background: rgba(0, 245, 255, 0.05);
            border: 1rpx solid rgba(0, 245, 255, 0.2);
            border-radius: 16rpx;
            padding: 0 24rpx;
            height: 88rpx;
            
            .input-icon {
                margin-right: 16rpx;
            }
        }
    }
    
    .login-btn {
        width: 100%;
        height: 88rpx;
        background: linear-gradient(135deg, rgba(0, 245, 255, 0.15) 0%, rgba(0, 212, 255, 0.15) 100%);
        border: 1rpx solid rgba(0, 245, 255, 0.3);
        border-radius: 16rpx;
        margin-top: 32rpx;
        position: relative;
        overflow: hidden;
        
        .btn-text {
            font-size: 28rpx;
            font-weight: 600;
            color: #00F5FF;
            letter-spacing: 4rpx;
        }
        
        .loading-content {
            display: flex;
            align-items: center;
            justify-content: center;
            gap: 12rpx;
            
            .loading-spinner {
                width: 32rpx;
                height: 32rpx;
                border: 3rpx solid rgba(0, 245, 255, 0.2);
                border-top-color: #00F5FF;
                border-radius: 50%;
                animation: spin 0.8s linear infinite;
            }
        }
        
        &:active:not([disabled]) {
            background: linear-gradient(135deg, rgba(0, 245, 255, 0.25) 0%, rgba(0, 212, 255, 0.25) 100%);
            transform: scale(0.98);
        }
        
        &[disabled] {
            opacity: 0.5;
        }
    }
    
    .link-row {
        display: flex;
        justify-content: space-between;
        margin-top: 32rpx;
        
        .link {
            display: flex;
            align-items: center;
            gap: 8rpx;
            
            .link-text {
                font-size: 22rpx;
                color: rgba(0, 245, 255, 0.7);
                letter-spacing: 1rpx;
            }
            
            &:active {
                opacity: 0.7;
            }
        }
    }
}

@keyframes spin {
    to { transform: rotate(360deg); }
}

.footer {
    text-align: center;
    margin-top: 48rpx;
    
    .footer-text {
        font-size: 20rpx;
        color: rgba(0, 245, 255, 0.5);
        letter-spacing: 2rpx;
    }
}
</style>
