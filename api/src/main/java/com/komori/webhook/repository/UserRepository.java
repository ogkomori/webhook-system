package com.komori.webhook.repository;

import com.komori.webhook.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<UserEntity, Long> {
    boolean existsByApiKey(String apiKey);

    boolean existsByUserId(String userId);

    boolean existsByEmail(String email);
}
