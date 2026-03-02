package com.dormpower.repository;

import com.dormpower.model.DataDict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DataDictRepository extends JpaRepository<DataDict, Long> {

    Optional<DataDict> findByDictCode(String dictCode);

    List<DataDict> findByDictTypeOrderBySortAsc(String dictType);

    List<DataDict> findByDictTypeAndEnabledTrueOrderBySortAsc(String dictType);

    List<DataDict> findByParentCodeOrderBySortAsc(String parentCode);

    List<DataDict> findByEnabledTrueOrderByDictTypeAscSortAsc();

    @Query("SELECT DISTINCT d.dictType FROM DataDict d ORDER BY d.dictType")
    List<String> findAllDictTypes();

    Optional<DataDict> findByDictTypeAndDictCode(String dictType, String dictCode);

    Optional<DataDict> findByDictTypeAndIsDefaultTrue(String dictType);

    boolean existsByDictCode(String dictCode);

    Page<DataDict> findByDictType(String dictType, Pageable pageable);

    long countByDictType(String dictType);
}
