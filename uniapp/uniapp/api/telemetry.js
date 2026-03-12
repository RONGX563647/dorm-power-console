import request from '@/utils/request'

function getTelemetry(params) {
    return request.get('/api/telemetry', params)
}

function getStatistics(params) {
    return request.get('/api/telemetry/statistics', params)
}

export default {
    getTelemetry,
    getStatistics
}
