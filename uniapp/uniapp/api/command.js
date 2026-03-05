import request from '@/utils/request'

function sendCommand(deviceId, data) {
    return request.post(`/api/strips/${deviceId}/cmd`, data)
}

function batchCommand(data) {
    return request.post('/api/commands/batch', data)
}

function getCommandStatus(cmdId) {
    return request.get(`/api/commands/${cmdId}`)
}

function getDeviceCommands(deviceId) {
    return request.get(`/api/commands/device/${deviceId}`)
}

export default {
    sendCommand,
    batchCommand,
    getCommandStatus,
    getDeviceCommands
}
