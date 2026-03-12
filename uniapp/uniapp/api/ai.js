import request from '@/utils/request'

function getRoomAIReport(roomId, params) {
    return request.get(`/api/rooms/${roomId}/ai_report`, params)
}

export default {
    getRoomAIReport
}
