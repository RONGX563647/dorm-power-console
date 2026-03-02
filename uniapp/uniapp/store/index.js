import request from '@/utils/request'
import api from '@/api'

// #ifndef VUE3
import Vue from 'vue'
import Vuex from 'vuex'
Vue.use(Vuex)
const store = new Vuex.Store({
// #endif

// #ifdef VUE3
import { createStore } from 'vuex'
const store = createStore({
// #endif
    state: {
        hasLogin: false,
        isUniverifyLogin: false,
        loginProvider: "",
        openid: null,
        testvuex: false,
        colorIndex: 0,
        colorList: ['#FF0000', '#00FF00', '#0000FF'],
        noMatchLeftWindow: true,
        active: 'componentPage',
        leftWinActive: '/pages/component/view/view',
        activeOpen: '',
        menu: [],
        univerifyErrorMsg: '',
        username: "foo",
        sex: "男",
        age: 10,
        
        user: null,
        token: '',
        devices: [],
        currentDevice: null,
        notifications: [],
        unreadCount: 0,
        alerts: [],
        buildings: [],
        rooms: []
    },
    mutations: {
        login(state, provider) {
            state.hasLogin = true;
            state.loginProvider = provider;
        },
        logout(state) {
            state.hasLogin = false
            state.openid = null
            state.user = null
            state.token = ''
            request.removeToken()
        },
        setOpenid(state, openid) {
            state.openid = openid
        },
        setTestTrue(state) {
            state.testvuex = true
        },
        setTestFalse(state) {
            state.testvuex = false
        },
        setColorIndex(state, index) {
            state.colorIndex = index
        },
        setMatchLeftWindow(state, matchLeftWindow) {
            state.noMatchLeftWindow = !matchLeftWindow
        },
        setActive(state, tabPage) {
            state.active = tabPage
        },
        setLeftWinActive(state, leftWinActive) {
            state.leftWinActive = leftWinActive
        },
        setActiveOpen(state, activeOpen) {
            state.activeOpen = activeOpen
        },
        setMenu(state, menu) {
            state.menu = menu
        },
        setUniverifyLogin(state, payload) {
            typeof payload !== 'boolean' ? payload = !!payload : '';
            state.isUniverifyLogin = payload;
        },
        setUniverifyErrorMsg(state,payload = ''){
            state.univerifyErrorMsg = payload
        },
        increment(state) {
          state.age++;
        },
        incrementTen(state, payload) {
          state.age += payload.amount
        },
        resetAge(state){
          state.age = 10
        },
        
        SET_USER(state, user) {
            state.user = user
            state.hasLogin = !!user
        },
        SET_TOKEN(state, token) {
            state.token = token
            request.setToken(token)
        },
        SET_DEVICES(state, devices) {
            state.devices = devices
        },
        SET_CURRENT_DEVICE(state, device) {
            state.currentDevice = device
        },
        SET_NOTIFICATIONS(state, notifications) {
            state.notifications = notifications
        },
        SET_UNREAD_COUNT(state, count) {
            state.unreadCount = count
        },
        SET_ALERTS(state, alerts) {
            state.alerts = alerts
        },
        SET_BUILDINGS(state, buildings) {
            state.buildings = buildings
        },
        SET_ROOMS(state, rooms) {
            state.rooms = rooms
        }
    },
    getters: {
        currentColor(state) {
            return state.colorList[state.colorIndex]
        },
        doubleAge(state) {
          return state.age * 2;
        },
        isAdmin(state) {
            return state.user && (state.user.role === 'ADMIN' || state.user.role === 'admin')
        },
        onlineDevices(state) {
            return state.devices.filter(d => d.online)
        },
        offlineDevices(state) {
            return state.devices.filter(d => !d.online)
        }
    },
    actions: {
        incrementAsync(context , payload) {
          context.commit('incrementTen',payload)
        },
        getUserOpenId: async function({
            commit,
            state
        }) {
            return await new Promise((resolve, reject) => {
                if (state.openid) {
                    resolve(state.openid)
                } else {
                    uni.login({
                        success: (data) => {
                            commit('login')
                            setTimeout(function() {
                                const openid = '123456789'
                                console.log('uni.request mock openid[' + openid + ']');
                                commit('setOpenid', openid)
                                resolve(openid)
                            }, 1000)
                        },
                        fail: (err) => {
                            console.log('uni.login 接口调用失败，将无法正常使用开放接口等服务', err)
                            reject(err)
                        }
                    })
                }
            })
        },
        getPhoneNumber: function({
            commit
        }, univerifyInfo) {
            return new Promise((resolve, reject) => {
                uni.request({
                    url: 'https://97fca9f2-41f6-449f-a35e-3f135d4c3875.bspapp.com/http/univerify-login',
                    method: 'POST',
                    data: univerifyInfo,
                    success: (res) => {
                        const data = res.data
                        if (data.success) {
                            resolve(data.phoneNumber)
                        } else {
                            reject(res)
                        }

                    },
                    fail: (err) => {
                        reject(res)
                    }
                })
            })
        },
        
        async loginAction({ commit }, loginData) {
            try {
                const res = await api.auth.login(loginData)
                const token = res.token || (res.data && res.data.token)
                const user = res.user || (res.data && res.data.user)
                if (token) {
                    commit('SET_TOKEN', token)
                }
                if (user) {
                    commit('SET_USER', user)
                }
                commit('login', 'custom')
                return res
            } catch (error) {
                throw error
            }
        },
        
        async logoutAction({ commit }) {
            try {
                await api.auth.logout()
            } catch (e) {
                console.error('logout error', e)
            }
            commit('logout')
        },
        
        async getCurrentUser({ commit }) {
            try {
                const user = await api.auth.getCurrentUser()
                commit('SET_USER', user)
                return user
            } catch (error) {
                throw error
            }
        },
        
        async fetchDevices({ commit }) {
            try {
                const devices = await api.device.getDeviceList()
                commit('SET_DEVICES', devices)
                return devices
            } catch (error) {
                throw error
            }
        },
        
        async fetchNotifications({ commit }, params) {
            try {
                const notifications = await api.notification.getNotifications(params)
                commit('SET_NOTIFICATIONS', notifications)
                return notifications
            } catch (error) {
                throw error
            }
        },
        
        async fetchUnreadCount({ commit }, params) {
            try {
                const data = await api.notification.getUnreadCount(params)
                commit('SET_UNREAD_COUNT', data.count || 0)
                return data
            } catch (error) {
                throw error
            }
        },
        
        async fetchAlerts({ commit }) {
            try {
                const alerts = await api.alert.getUnresolvedAlerts()
                commit('SET_ALERTS', alerts)
                return alerts
            } catch (error) {
                throw error
            }
        },
        
        async fetchBuildings({ commit }) {
            try {
                const buildings = await api.dorm.getBuildings()
                commit('SET_BUILDINGS', buildings)
                return buildings
            } catch (error) {
                throw error
            }
        },
        
        async fetchRooms({ commit }, params) {
            try {
                const rooms = await api.dorm.getRooms(params)
                commit('SET_ROOMS', rooms)
                return rooms
            } catch (error) {
                throw error
            }
        }
    }
})

export default store
