package com.dormpower.repository;

import com.dormpower.model.Permission;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 权限仓库接口
 */
@Repository
public interface PermissionRepository extends JpaRepository<Permission, String> {
    
    Optional<Permission> findByCode(String code);
    
    List<Permission> findByEnabledTrue();
    
    Page<Permission> findByEnabled(boolean enabled, Pageable pageable);
    
    List<Permission> findByResourceId(String resourceId);
    
    List<Permission> findByAction(String action);
    
    @Query("SELECT p FROM Permission p WHERE p.resource.id = :resourceId AND p.action = :action")
    Optional<Permission> findByResourceIdAndAction(@Param("resourceId") String resourceId, @Param("action") String action);
    
    @Query("SELECT p FROM Permission p WHERE p.code IN :codes")
    List<Permission> findByCodes(@Param("codes") List<String> codes);
    
    boolean existsByCode(String code);
}
