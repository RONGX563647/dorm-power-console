import request from '@/utils/request'

function getUserRoles(username) {
    return request.get(`/api/rbac/users/${username}/roles`)
}

function checkPermission(username, permissionCode) {
    return request.get(`/api/rbac/users/${username}/has-permission`, { permissionCode })
}

export default {
    getUserRoles,
    checkPermission
}
