package com.dormpower.repository;

import com.dormpower.model.Building;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 楼栋Repository
 */
@Repository
public interface BuildingRepository extends JpaRepository<Building, String> {

    /**
     * 根据编码查询楼栋
     */
    Optional<Building> findByCode(String code);

    /**
     * 查询启用的楼栋
     */
    List<Building> findByEnabledOrderByCodeAsc(boolean enabled);

    /**
     * 根据名称模糊查询
     */
    List<Building> findByNameContaining(String name);
}
