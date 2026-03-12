package com.dormpower.repository;

import com.dormpower.model.UserAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 用户账户仓库接口
 */
@Repository
public interface UserAccountRepository extends JpaRepository<UserAccount, String> {

    // 自定义查询方法
    Optional<UserAccount> findByEmail(String email);

}