package com.komori.persistence.repository;

import com.komori.persistence.entity.DeliveryAttemptEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DeliveryAttemptRepository extends JpaRepository<DeliveryAttemptEntity, Long> {
}
