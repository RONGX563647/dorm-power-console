package com.dormpower.repository;

import com.dormpower.model.UserRole;
import com.dormpower.model.UserRoleId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 用户角色关联仓库接口
 */
@Repository
public interface UserRoleRepository extends JpaRepository<UserRole, UserRoleId> {
    
    List<UserRole> findByUsername(String username);
    
    List<UserRole> findByRoleId(String roleId);
    
    Optional<UserRole> findByUsernameAndRoleId(String username, String roleId);
    
    void deleteByUsernameAndRoleId(String username, String roleId);
    
    void deleteByUsername(String username);
    
    void deleteByRoleId(String roleId);
    
    @Query("SELECT ur.roleId FROM UserRole ur WHERE ur.username = :username")
    List<String> findRoleIdsByUsername(@Param("username") String username);
    
    @Query("SELECT COUNT(ur) FROM UserRole ur WHERE ur.roleId = :roleId")
    long countByRoleId(@Param("roleId") String roleId);
    
    boolean existsByUsernameAndRoleId(String username, String roleId);
}
