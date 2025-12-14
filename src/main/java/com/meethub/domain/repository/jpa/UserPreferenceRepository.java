package com.meethub.domain.repository.jpa;

import com.meethub.domain.model.entity.UserPreference;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserPreferenceRepository extends JpaRepository<UserPreference, Long> {

    // Podstawowe metody wyszukiwania
    List<UserPreference> findByUserId(Long userId);
    Optional<UserPreference> findByUserIdAndPreferenceKey(Long userId, String preferenceKey);
}