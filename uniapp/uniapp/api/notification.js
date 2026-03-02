import request from '@/utils/request'

function getNotifications(params) {
    return request.get('/api/notifications', params)
}

function getUnreadCount(params) {
    return request.get('/api/notifications/unread/count', params)
}

function getPreferences(params) {
    return request.get('/api/notifications/preferences', params)
}

function updatePreferences(data) {
    return request.put('/api/notifications/preferences', data)
}

function markAsRead(notificationId, params) {
    return request.put(`/api/notifications/${notificationId}/read`, {}, params)
}

function markAllAsRead(params) {
    return request.put('/api/notifications/read-all', {}, params)
}

function deleteNotification(notificationId) {
    return request.del(`/api/notifications/${notificationId}`)
}

export default {
    getNotifications,
    getUnreadCount,
    getPreferences,
    updatePreferences,
    markAsRead,
    markAllAsRead,
    deleteNotification
}
