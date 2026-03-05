import request from '@/utils/request'

function getSystemStatus() {
    return request.get('/api/admin/monitor/system')
}

function getDeviceStatus() {
    return request.get('/api/admin/monitor/devices')
}

function getApiPerformance(params) {
    return request.get('/api/admin/monitor/api-performance', params)
}

function getMetrics(params) {
    return request.get('/api/admin/monitor/metrics', params)
}

function collectMetrics() {
    return request.post('/api/admin/monitor/collect')
}

function cleanupMetrics(params) {
    return request.del('/api/admin/monitor/cleanup', params)
}

export default {
    getSystemStatus,
    getDeviceStatus,
    getApiPerformance,
    getMetrics,
    collectMetrics,
    cleanupMetrics
}