import request from '@/utils/request'

function getPriceRules() {
    return request.get('/api/billing/price-rules')
}

function createPriceRule(data) {
    return request.post('/api/billing/price-rules', data)
}

function updatePriceRule(id, data) {
    return request.put(`/api/billing/price-rules/${id}`, data)
}

function deletePriceRule(id) {
    return request.del(`/api/billing/price-rules/${id}`)
}

function generateBill(params) {
    return request.post('/api/billing/bills/generate', params)
}

function getBills(params) {
    return request.get('/api/billing/bills', params)
}

function getPendingBills() {
    return request.get('/api/billing/bills/pending')
}

function payBill(billId, params) {
    return request.post(`/api/billing/bills/${billId}/pay`, params)
}

function recharge(params) {
    return request.post('/api/billing/recharge', params)
}

function getRechargeRecords(params) {
    return request.get('/api/billing/recharge-records', params)
}

function getRoomBalance(roomId) {
    return request.get(`/api/billing/balance/${roomId}`)
}

function getLowBalanceRooms(params) {
    return request.get('/api/billing/low-balance', params)
}

export default {
    getPriceRules,
    createPriceRule,
    updatePriceRule,
    deletePriceRule,
    generateBill,
    getBills,
    getPendingBills,
    payBill,
    recharge,
    getRechargeRecords,
    getRoomBalance,
    getLowBalanceRooms
}