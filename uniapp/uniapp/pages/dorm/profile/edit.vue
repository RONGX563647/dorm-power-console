<template>
    <view class="edit-profile-container">
        <view class="form-section">
            <uni-forms ref="form" :modelValue="formData" :rules="rules">
                <uni-forms-item label="用户名" name="username">
                    <uni-easyinput v-model="formData.username" placeholder="请输入用户名" />
                </uni-forms-item>
                
                <uni-forms-item label="邮箱" name="email">
                    <uni-easyinput v-model="formData.email" placeholder="请输入邮箱" />
                </uni-forms-item>
            </uni-forms>
            
            <button class="save-btn" type="primary" @click="handleSave" :loading="loading">保存修改</button>
        </view>
    </view>
</template>

<script>
import api from '@/api'
import { mapState, mapActions } from 'vuex'

export default {
    data() {
        return {
            formData: {
                username: '',
                email: ''
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
            loading: false
        }
    },
    computed: {
        ...mapState(['user'])
    },
    onLoad() {
        if (this.user) {
            this.formData = {
                username: this.user.username,
                email: this.user.email
            }
        }
    },
    methods: {
        ...mapActions(['getCurrentUser']),
        
        async handleSave() {
            try {
                const valid = await this.$refs.form.validate()
                if (!valid) return
                
                this.loading = true
                await api.user.updateUser(this.user.id, this.formData)
                
                uni.showToast({
                    title: '保存成功',
                    icon: 'success'
                })
                
                await this.getCurrentUser()
            } catch (error) {
                console.error('保存失败:', error)
            } finally {
                this.loading = false
            }
        }
    }
}
</script>

<style lang="scss" scoped>
.edit-profile-container {
    min-height: 100vh;
    background: #F2F7F5;
    padding: 20rpx;
}

.form-section {
    background: #fff;
    border-radius: 16rpx;
    padding: 30rpx;
    
    .save-btn {
        margin-top: 40rpx;
        border-radius: 50rpx;
        height: 88rpx;
        line-height: 88rpx;
        font-size: 32rpx;
    }
}
</style>
