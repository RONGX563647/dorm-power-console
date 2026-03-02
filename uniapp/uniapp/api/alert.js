import request from '@/utils/request'

function getDeviceAlerts(deviceId, params) {
    return request.get(`/api/alerts/device/${deviceId}`, params)
}

function getUnresolvedAlerts(params) {
    return request.get('/api/alerts/unresolved', params)
}

function getAlertConfig(deviceId) {
    return request.get(`/api/alerts/config/${deviceId}`)
}

function acknowledgeAlert(alertId) {
    return request.put(`/api/alerts/${alertId}/acknowledge`)
}

function batchAcknowledge(alertIds) {
    return request.put('/api/alerts/batch-acknowledge', { ids: alertIds })
}

export default {
    getDeviceAlerts,
    getUnresolvedAlerts,
    getAlertConfig,
    acknowledgeAlert,
    batchAcknowledge
}
