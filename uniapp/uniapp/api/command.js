import request from '@/utils/request'

function sendCommand(deviceId, data) {
    return request.post(`/api/strips/${deviceId}/cmd`, data)
}

function getCommandStatus(cmdId) {
    return request.get(`/api/cmd/${cmdId}`)
}

function getDeviceCommands(deviceId) {
    return request.get(`/api/commands/device/${deviceId}`)
}

export default {
    sendCommand,
    getCommandStatus,
    getDeviceCommands
}
