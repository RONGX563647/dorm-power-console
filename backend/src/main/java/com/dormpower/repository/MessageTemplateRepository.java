package com.dormpower.repository;

import com.dormpower.model.MessageTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MessageTemplateRepository extends JpaRepository<MessageTemplate, Long> {

    Optional<MessageTemplate> findByTemplateCode(String templateCode);

    List<MessageTemplate> findByTypeOrderByCreatedAtDesc(String type);

    List<MessageTemplate> findByChannelOrderByCreatedAtDesc(String channel);

    List<MessageTemplate> findByEnabledTrueOrderByCreatedAtDesc();

    Optional<MessageTemplate> findByTemplateCodeAndEnabledTrue(String templateCode);

    List<MessageTemplate> findByTypeAndEnabledTrueOrderByCreatedAtDesc(String type);

    boolean existsByTemplateCode(String templateCode);
}
