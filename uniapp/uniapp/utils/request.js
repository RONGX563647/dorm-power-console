const BASE_URL = 'http://localhost:8000'
const TIMEOUT = 10000

let token = ''

function setToken(newToken) {
    token = newToken
    try {
        uni.setStorageSync('token', newToken)
    } catch (e) {
        console.error('保存token失败', e)
    }
}

function getToken() {
    if (!token) {
        try {
            token = uni.getStorageSync('token') || ''
        } catch (e) {
            console.error('获取token失败', e)
        }
    }
    return token
}

function removeToken() {
    token = ''
    try {
        uni.removeStorageSync('token')
    } catch (e) {
        console.error('删除token失败', e)
    }
}

function request(options) {
    return new Promise((resolve, reject) => {
        const header = {
            'Content-Type': 'application/json',
            ...options.header
        }
        
        const currentToken = getToken()
        if (currentToken && !options.noAuth) {
            header['Authorization'] = `Bearer ${currentToken}`
        }
        
        uni.request({
            url: BASE_URL + options.url,
            method: options.method || 'GET',
            data: options.data,
            header: header,
            timeout: options.timeout || TIMEOUT,
            success: (res) => {
                const data = res.data
                
                if (res.statusCode === 200) {
                    if (data.code === 200) {
                        resolve(data.data)
                    } else if (data.code === undefined) {
                        resolve(data)
                    } else {
                        handleError(data.code, data.message)
                        reject(data)
                    }
                } else if (res.statusCode === 401) {
                    removeToken()
                    uni.showToast({
                        title: '登录已过期，请重新登录',
                        icon: 'none'
                    })
                    setTimeout(() => {
                        uni.reLaunch({
                            url: '/pages/dorm/auth/login'
                        })
                    }, 1500)
                    reject(data)
                } else if (res.statusCode === 403) {
                    uni.showToast({
                        title: '无操作权限',
                        icon: 'none'
                    })
                    reject(data)
                } else if (res.statusCode === 404) {
                    uni.showToast({
                        title: '资源不存在',
                        icon: 'none'
                    })
                    reject(data)
                } else if (res.statusCode >= 500) {
                    uni.showToast({
                        title: '服务器错误，请稍后重试',
                        icon: 'none'
                    })
                    reject(data)
                } else {
                    handleError(data.code, data.message)
                    reject(data)
                }
            },
            fail: (err) => {
                console.error('请求失败:', err)
                uni.showToast({
                    title: '网络请求失败',
                    icon: 'none'
                })
                reject(err)
            }
        })
    })
}

function handleError(code, message) {
    const errorMsg = message || '请求失败'
    uni.showToast({
        title: errorMsg,
        icon: 'none'
    })
}

function get(url, data, options = {}) {
    return request({
        url,
        method: 'GET',
        data,
        ...options
    })
}

function post(url, data, options = {}) {
    return request({
        url,
        method: 'POST',
        data,
        ...options
    })
}

function put(url, data, options = {}) {
    return request({
        url,
        method: 'PUT',
        data,
        ...options
    })
}

function del(url, data, options = {}) {
    return request({
        url,
        method: 'DELETE',
        data,
        ...options
    })
}

export default {
    request,
    get,
    post,
    put,
    del,
    setToken,
    getToken,
    removeToken,
    BASE_URL
}
