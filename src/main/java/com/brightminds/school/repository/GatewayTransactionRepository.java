package com.brightminds.school.repository;

import com.brightminds.school.entity.GatewayTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface GatewayTransactionRepository extends JpaRepository<GatewayTransaction, UUID> {
    Optional<GatewayTransaction> findByReference(String reference);
}
