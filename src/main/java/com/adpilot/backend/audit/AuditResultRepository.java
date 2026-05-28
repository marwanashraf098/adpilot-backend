package com.adpilot.backend.audit;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AuditResultRepository extends JpaRepository<AuditResult, String> {
    List<AuditResult> findByBusinessNameContainingIgnoreCase(String businessName);
    List<AuditResult> findByIndustry(String industry);
    List<AuditResult> findAllByOrderByCreatedAtDesc();
}