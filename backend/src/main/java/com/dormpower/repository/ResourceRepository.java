package com.dormpower.repository;

import com.dormpower.model.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 资源仓库接口
 */
@Repository
public interface ResourceRepository extends JpaRepository<Resource, String> {
    
    Optional<Resource> findByCode(String code);
    
    List<Resource> findByEnabledTrue();
    
    Page<Resource> findByEnabled(boolean enabled, Pageable pageable);
    
    List<Resource> findByType(String type);
    
    List<Resource> findByParentId(Long parentId);
    
    @Query("SELECT r FROM Resource r WHERE r.parentId = 0 ORDER BY r.sortOrder")
    List<Resource> findRootResources();
    
    @Query("SELECT r FROM Resource r WHERE r.parentId = :parentId ORDER BY r.sortOrder")
    List<Resource> findByParentIdOrderBySortOrder(@Param("parentId") Long parentId);
    
    @Query("SELECT r FROM Resource r WHERE r.type = :type AND r.enabled = true ORDER BY r.sortOrder")
    List<Resource> findByTypeAndEnabledTrueOrderBySortOrder(@Param("type") String type);
    
    boolean existsByCode(String code);
}
