import request from '@/utils/request'

function getUserList(params) {
    return request.get('/api/users', params)
}

function getUserDetail(userId) {
    return request.get(`/api/users/${userId}`)
}

function updateUser(userId, data) {
    return request.put(`/api/users/${userId}`, data)
}

function changePassword(userId, data) {
    return request.post(`/api/users/${userId}/password`, data)
}

function createUser(data) {
    return request.post('/api/auth/register', data)
}

function deleteUser(userId) {
    return request.del(`/api/users/${userId}`)
}

export default {
    getUserList,
    getUserDetail,
    updateUser,
    changePassword,
    createUser,
    deleteUser
}
