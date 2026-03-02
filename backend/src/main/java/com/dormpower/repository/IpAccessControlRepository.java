package com.dormpower.repository;

import com.dormpower.model.IpAccessControl;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface IpAccessControlRepository extends JpaRepository<IpAccessControl, Long> {

    Optional<IpAccessControl> findByIpAddress(String ipAddress);

    List<IpAccessControl> findByTypeOrderByCreatedAtDesc(String type);

    List<IpAccessControl> findByEnabledTrueOrderByCreatedAtDesc();

    @Query("SELECT i FROM IpAccessControl i WHERE i.type = :type AND i.enabled = true AND (i.expiresAt = 0 OR i.expiresAt > :now)")
    List<IpAccessControl> findActiveByType(String type, long now);

    @Query("SELECT i FROM IpAccessControl i WHERE i.enabled = true AND (i.expiresAt = 0 OR i.expiresAt > :now)")
    List<IpAccessControl> findAllActive(long now);

    boolean existsByIpAddress(String ipAddress);

    void deleteByIpAddress(String ipAddress);
}
