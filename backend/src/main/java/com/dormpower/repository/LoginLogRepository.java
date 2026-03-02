package com.dormpower.repository;

import com.dormpower.model.LoginLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface LoginLogRepository extends JpaRepository<LoginLog, Long> {

    Page<LoginLog> findByUsernameOrderByLoginTsDesc(String username, Pageable pageable);

    Page<LoginLog> findByStatusOrderByLoginTsDesc(String status, Pageable pageable);

    Page<LoginLog> findAllByOrderByLoginTsDesc(Pageable pageable);

    List<LoginLog> findByUsernameAndLoginTsAfterOrderByLoginTsDesc(String username, long ts);

    @Query("SELECT l FROM LoginLog l WHERE l.loginTs BETWEEN :startTime AND :endTime ORDER BY l.loginTs DESC")
    Page<LoginLog> findByTimeRange(long startTime, long endTime, Pageable pageable);

    @Query("SELECT COUNT(l) FROM LoginLog l WHERE l.username = :username AND l.status = 'SUCCESS' AND l.loginTs > :since")
    long countSuccessfulLoginsSince(String username, long since);

    @Query("SELECT COUNT(l) FROM LoginLog l WHERE l.username = :username AND l.status = 'FAILED' AND l.loginTs > :since")
    long countFailedLoginsSince(String username, long since);

    @Query("SELECT COUNT(l) FROM LoginLog l WHERE l.status = 'FAILED' AND l.ipAddress = :ipAddress AND l.loginTs > :since")
    long countFailedLoginsFromIpSince(String ipAddress, long since);

    @Query("SELECT l.ipAddress, COUNT(l) as cnt FROM LoginLog l WHERE l.loginTs > :since GROUP BY l.ipAddress ORDER BY cnt DESC")
    List<Object[]> findTopIpAddresses(long since, Pageable pageable);

    @Query("SELECT l.username, COUNT(l) as cnt FROM LoginLog l WHERE l.status = 'SUCCESS' AND l.loginTs > :since GROUP BY l.username ORDER BY cnt DESC")
    List<Object[]> findTopActiveUsers(long since, Pageable pageable);

    long deleteByLoginTsBefore(long ts);
}
