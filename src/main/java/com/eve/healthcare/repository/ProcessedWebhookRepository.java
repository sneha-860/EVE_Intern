package com.eve.healthcare.repository;

import com.eve.healthcare.entity.ProcessedWebhook;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProcessedWebhookRepository extends JpaRepository<ProcessedWebhook, Long> {
    boolean existsByEventId(String eventId);
}
