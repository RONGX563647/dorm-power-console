import request from '@/utils/request'

function getDeviceList() {
    return request.get('/api/devices')
}

function getDeviceStatus(deviceId) {
    return request.get(`/api/devices/${deviceId}/status`)
}

function getDevicesByRoom(roomId) {
    return request.get(`/api/devices/room/${roomId}`)
}

function createDevice(data) {
    return request.post('/api/devices', data)
}

function getDeviceHistory(deviceId, params) {
    return request.get(`/api/devices/${deviceId}/history`, params)
}

export default {
    getDeviceList,
    getDeviceStatus,
    getDevicesByRoom,
    createDevice,
    getDeviceHistory
}
