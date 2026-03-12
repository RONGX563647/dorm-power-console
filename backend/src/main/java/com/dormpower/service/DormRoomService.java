package com.dormpower.service;

import com.dormpower.model.Building;
import com.dormpower.model.DormRoom;
import com.dormpower.repository.BuildingRepository;
import com.dormpower.repository.DormRoomRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 宿舍房间管理服务
 */
@Service
public class DormRoomService {

    @Autowired
    private BuildingRepository buildingRepository;

    @Autowired
    private DormRoomRepository dormRoomRepository;

    /**
     * 创建楼栋
     */
    @CacheEvict(value = "buildings", allEntries = true)
    public Building createBuilding(Building building) {
        building.setId("bld_" + UUID.randomUUID().toString().substring(0, 8));
        building.setCreatedAt(System.currentTimeMillis() / 1000);
        building.setEnabled(true);
        return buildingRepository.save(building);
    }

    /**
     * 获取所有楼栋
     */
    public List<Building> getAllBuildings() {
        return buildingRepository.findAll();
    }

    /**
     * 获取启用的楼栋
     */
    @Cacheable(value = "buildings", key = "'enabled'")
    public List<Building> getEnabledBuildings() {
        return buildingRepository.findByEnabledOrderByCodeAsc(true);
    }

    /**
     * 更新楼栋
     */
    @CacheEvict(value = "buildings", allEntries = true)
    public Building updateBuilding(String id, Building building) {
        Building existing = buildingRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Building not found"));

        existing.setName(building.getName());
        existing.setCode(building.getCode());
        existing.setDescription(building.getDescription());
        existing.setTotalFloors(building.getTotalFloors());
        existing.setAddress(building.getAddress());
        existing.setManager(building.getManager());
        existing.setContact(building.getContact());
        existing.setEnabled(building.isEnabled());
        existing.setUpdatedAt(System.currentTimeMillis() / 1000);

        return buildingRepository.save(existing);
    }

    /**
     * 删除楼栋
     */
    @CacheEvict(value = "buildings", allEntries = true)
    public void deleteBuilding(String id) {
        buildingRepository.deleteById(id);
    }

    /**
     * 创建房间
     */
    public DormRoom createRoom(DormRoom room) {
        room.setId("room_" + UUID.randomUUID().toString().substring(0, 8));
        room.setCreatedAt(System.currentTimeMillis() / 1000);
        room.setEnabled(true);
        room.setStatus("VACANT");
        room.setCurrentOccupants(0);
        return dormRoomRepository.save(room);
    }

    /**
     * 获取所有房间
     */
    public List<DormRoom> getAllRooms() {
        return dormRoomRepository.findAll();
    }

    /**
     * 根据楼栋获取房间
     */
    public List<DormRoom> getRoomsByBuilding(String buildingId) {
        return dormRoomRepository.findByBuildingIdOrderByFloorAscRoomNumberAsc(buildingId);
    }

    /**
     * 根据楼栋和楼层获取房间
     */
    public List<DormRoom> getRoomsByBuildingAndFloor(String buildingId, int floor) {
        return dormRoomRepository.findByBuildingIdAndFloorOrderByRoomNumberAsc(buildingId, floor);
    }

    /**
     * 更新房间
     */
    public DormRoom updateRoom(String id, DormRoom room) {
        DormRoom existing = dormRoomRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Room not found"));

        existing.setBuildingId(room.getBuildingId());
        existing.setFloor(room.getFloor());
        existing.setRoomNumber(room.getRoomNumber());
        existing.setRoomType(room.getRoomType());
        existing.setCapacity(room.getCapacity());
        existing.setElectricityQuota(room.getElectricityQuota());
        existing.setDeviceId(room.getDeviceId());
        existing.setPriceRuleId(room.getPriceRuleId());
        existing.setStatus(room.getStatus());
        existing.setRemark(room.getRemark());
        existing.setEnabled(room.isEnabled());
        existing.setUpdatedAt(System.currentTimeMillis() / 1000);

        return dormRoomRepository.save(existing);
    }

    /**
     * 删除房间
     */
    public void deleteRoom(String id) {
        dormRoomRepository.deleteById(id);
    }

    /**
     * 入住
     */
    public DormRoom checkIn(String roomId, int occupantCount) {
        DormRoom room = dormRoomRepository.findById(roomId)
                .orElseThrow(() -> new RuntimeException("Room not found"));

        if (!"VACANT".equals(room.getStatus())) {
            throw new RuntimeException("Room is not vacant");
        }

        room.setCurrentOccupants(occupantCount);
        room.setStatus("OCCUPIED");
        room.setUpdatedAt(System.currentTimeMillis() / 1000);

        return dormRoomRepository.save(room);
    }

    /**
     * 退宿
     */
    public DormRoom checkOut(String roomId) {
        DormRoom room = dormRoomRepository.findById(roomId)
                .orElseThrow(() -> new RuntimeException("Room not found"));

        room.setCurrentOccupants(0);
        room.setStatus("VACANT");
        room.setUpdatedAt(System.currentTimeMillis() / 1000);

        return dormRoomRepository.save(room);
    }

    /**
     * 获取房间统计
     */
    public Map<String, Object> getRoomStatistics() {
        Map<String, Object> stats = new HashMap<>();

        long totalRooms = dormRoomRepository.count();
        long vacantRooms = dormRoomRepository.findByStatus("VACANT").size();
        long occupiedRooms = dormRoomRepository.findByStatus("OCCUPIED").size();
        long maintenanceRooms = dormRoomRepository.findByStatus("MAINTENANCE").size();

        stats.put("totalRooms", totalRooms);
        stats.put("vacantRooms", vacantRooms);
        stats.put("occupiedRooms", occupiedRooms);
        stats.put("maintenanceRooms", maintenanceRooms);
        stats.put("occupancyRate", totalRooms > 0 ? (double) occupiedRooms / totalRooms * 100 : 0);

        return stats;
    }

    /**
     * 根据设备ID获取房间
     */
    public DormRoom getRoomByDeviceId(String deviceId) {
        return dormRoomRepository.findByDeviceId(deviceId)
                .orElseThrow(() -> new RuntimeException("Room not found for device"));
    }
}
