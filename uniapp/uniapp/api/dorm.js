import request from '@/utils/request'

function getBuildings() {
    return request.get('/api/dorm/buildings')
}

function getRooms(params) {
    return request.get('/api/dorm/rooms', params)
}

function checkIn(roomId, data) {
    return request.post(`/api/dorm/rooms/${roomId}/check-in`, data)
}

function checkOut(roomId, data) {
    return request.post(`/api/dorm/rooms/${roomId}/check-out`, data)
}

function getRoomDetail(roomId) {
    return request.get(`/api/dorm/rooms/${roomId}`)
}

export default {
    getBuildings,
    getRooms,
    checkIn,
    checkOut,
    getRoomDetail
}
