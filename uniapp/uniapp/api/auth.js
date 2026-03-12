import request from '@/utils/request'

export function healthCheck() {
    return request.get('/health', {}, { noAuth: true })
}

export function login(data) {
    return request.post('/api/auth/login', data, { noAuth: true })
}

export function register(data) {
    return request.post('/api/auth/register', data, { noAuth: true })
}

export function getCurrentUser() {
    return request.get('/api/auth/me')
}

export function refreshToken() {
    return request.post('/api/auth/refresh')
}

export function logout() {
    return request.post('/api/auth/logout')
}

export function forgotPassword(data) {
    return request.post('/api/auth/forgot-password', data, { noAuth: true })
}

export default {
    healthCheck,
    login,
    register,
    getCurrentUser,
    refreshToken,
    logout,
    forgotPassword
}
