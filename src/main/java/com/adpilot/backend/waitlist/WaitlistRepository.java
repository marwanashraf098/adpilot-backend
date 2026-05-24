package com.adpilot.backend.waitlist;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WaitlistRepository extends JpaRepository<Waitlist, String> {
    boolean existsByEmail(String email);
}