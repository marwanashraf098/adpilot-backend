package com.adpilot.backend.adaccount;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface AdAccountRepository extends JpaRepository<AdAccount, String> {
    List<AdAccount> findByUserId(String userId);
    boolean existsByUserIdAndPlatformAccountId(String userId, String platformAccountId);
}