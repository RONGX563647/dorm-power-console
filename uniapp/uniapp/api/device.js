import request from '@/utils/request'

function getDeviceList() {
    return request.get('/api/devices')
}

function getDeviceDetail(deviceId) {
    return request.get(`/api/devices/${deviceId}`)
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

function updateDevice(deviceId, data) {
    return request.put(`/api/devices/${deviceId}`, data)
}

function deleteDevice(deviceId) {
    return request.del(`/api/devices/${deviceId}`)
}

export default {
    getDeviceList,
    getDeviceDetail,
    getDeviceStatus,
    getDevicesByRoom,
    createDevice,
    updateDevice,
    deleteDevice
}
