package com.komori.persistence.repository;

import com.komori.persistence.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<UserEntity, Long> {
    boolean existsByApiKey(String apiKey);

    boolean existsByUserId(String userId);

    boolean existsByEmail(String email);

    UserEntity findByApiKey(String apiKey);
}
