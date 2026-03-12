<script>
import {
    mapMutations
} from 'vuex'
import {
    version
} from './package.json'
import request from '@/utils/request'

export default {
    onLaunch: function() {
        console.log('App Launch')
        
        const token = request.getToken()
        if (token) {
            this.checkLoginStatus()
        }
    },
    onShow: function() {
        console.log('App Show')
    },
    onHide: function() {
        console.log('App Hide')
    },
    globalData: {
        test: ''
    },
    methods: {
        ...mapMutations(['setUniverifyErrorMsg', 'setUniverifyLogin']),
        
        async checkLoginStatus() {
            try {
                const user = await this.$store.dispatch('getCurrentUser')
                if (user) {
                    console.log('用户已登录:', user.username)
                }
            } catch (error) {
                console.log('Token已过期，需要重新登录')
                request.removeToken()
            }
        }
    }
}
</script>

<style lang="scss">
    @import '@/uni_modules/uni-scss/index.scss';
    /* #ifndef APP-PLUS-NVUE */
    @import './common/uni.css';
    @import '@/static/customicons.css';
    
    page {
        background-color: #f5f5f5;
        height: 100%;
        font-size: 28rpx;
    }

    .fix-pc-padding {
        padding: 0 50px;
    }

    .uni-header-logo {
        padding: 30rpx;
        flex-direction: column;
        justify-content: center;
        align-items: center;
        margin-top: 10rpx;
    }

    .uni-header-image {
        width: 100px;
        height: 100px;
    }

    .uni-hello-text {
        color: #7A7E83;
    }

    .uni-hello-addfile {
        text-align: center;
        line-height: 300rpx;
        background: #FFF;
        padding: 50rpx;
        margin-top: 10px;
        font-size: 38rpx;
        color: #808080;
    }
    /* #endif*/
</style>
