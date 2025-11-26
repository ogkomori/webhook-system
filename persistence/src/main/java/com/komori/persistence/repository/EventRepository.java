package com.komori.persistence.repository;

import com.komori.persistence.entity.EventEntity;
import com.komori.persistence.enumerated.EventStatus;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface EventRepository extends JpaRepository<EventEntity, Long> {
    EventEntity findByEventId(String eventId);

    @Transactional
    @Modifying
    @Query(value = """
    update EventEntity e
        set e.status = :status where e.eventId = :eventId
    """)
    void updateEventStatus(@Param("eventId") String eventId, @Param("status") EventStatus status);
}
