import request from '@/utils/request'

function cutoffPower(roomId, params) {
    return request.post(`/api/power-control/cutoff/${roomId}`, params)
}

function restorePower(roomId, params) {
    return request.post(`/api/power-control/restore/${roomId}`, params)
}

function getPowerStatus(roomId) {
    return request.get(`/api/power-control/status/${roomId}`)
}

function getCutoffRooms() {
    return request.get('/api/power-control/cutoff-rooms')
}

function getOverdueRooms() {
    return request.get('/api/power-control/overdue-rooms')
}

export default {
    cutoffPower,
    restorePower,
    getPowerStatus,
    getCutoffRooms,
    getOverdueRooms
}