package com.dormpower.repository;

import com.dormpower.model.ElectricityPriceRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 电价规则Repository
 */
@Repository
public interface ElectricityPriceRuleRepository extends JpaRepository<ElectricityPriceRule, String> {

    /**
     * 查找启用的规则
     */
    List<ElectricityPriceRule> findByEnabled(boolean enabled);

    /**
     * 查找默认规则
     */
    Optional<ElectricityPriceRule> findFirstByEnabledTrueOrderByCreatedAtAsc();
}
