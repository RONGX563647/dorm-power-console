import request from '@/utils/request'

function getUserList(params) {
    return request.get('/api/users', params)
}

function getUserDetail(username) {
    return request.get(`/api/users/${username}`)
}

function updateUser(username, data) {
    return request.put(`/api/users/${username}`, data)
}

function updateProfile(username, data) {
    return request.patch(`/api/users/${username}/profile`, data)
}

function changePassword(username, data) {
    return request.post(`/api/users/${username}/password`, data)
}

function createUser(data) {
    return request.post('/api/users', data)
}

function deleteUser(username) {
    return request.del(`/api/users/${username}`)
}

export default {
    getUserList,
    getUserDetail,
    updateUser,
    updateProfile,
    changePassword,
    createUser,
    deleteUser
}
