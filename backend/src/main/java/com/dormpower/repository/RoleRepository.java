package com.dormpower.repository;

import com.dormpower.model.Role;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 角色仓库接口
 */
@Repository
public interface RoleRepository extends JpaRepository<Role, String> {
    
    Optional<Role> findByCode(String code);
    
    List<Role> findByEnabledTrue();
    
    Page<Role> findByEnabled(boolean enabled, Pageable pageable);
    
    List<Role> findBySystemTrue();
    
    @Query("SELECT r FROM Role r JOIN r.permissions p WHERE p.code = :permissionCode")
    List<Role> findByPermissionCode(@Param("permissionCode") String permissionCode);
    
    @Query("SELECT r FROM Role r WHERE r.code IN :codes")
    List<Role> findByCodes(@Param("codes") List<String> codes);
    
    boolean existsByCode(String code);
}
