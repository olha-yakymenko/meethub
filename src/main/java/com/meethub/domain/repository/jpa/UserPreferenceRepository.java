// UserPreferenceRepository.java
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
    List<UserPreference> findByPreferenceKey(String preferenceKey);

    // Wyszukiwanie z wartością
    List<UserPreference> findByUserIdAndPreferenceValue(Long userId, String preferenceValue);
    List<UserPreference> findByPreferenceKeyAndPreferenceValue(String preferenceKey, String preferenceValue);

    // Sprawdzanie istnienia
    boolean existsByUserIdAndPreferenceKey(Long userId, String preferenceKey);
    boolean existsByUserIdAndPreferenceKeyAndPreferenceValue(Long userId, String preferenceKey, String preferenceValue);

    // Usuwanie
    @Modifying
    @Query("DELETE FROM UserPreference up WHERE up.user.id = :userId AND up.preferenceKey = :preferenceKey")
    void deleteByUserIdAndPreferenceKey(@Param("userId") Long userId, @Param("preferenceKey") String preferenceKey);

    @Modifying
    @Query("DELETE FROM UserPreference up WHERE up.user.id = :userId")
    void deleteAllByUserId(@Param("userId") Long userId);

    // Aktualizacja
    @Modifying
    @Query("UPDATE UserPreference up SET up.preferenceValue = :preferenceValue WHERE up.user.id = :userId AND up.preferenceKey = :preferenceKey")
    void updatePreferenceValue(@Param("userId") Long userId, @Param("preferenceKey") String preferenceKey, @Param("preferenceValue") String preferenceValue);

    // Pobieranie z privacy level
    List<UserPreference> findByUserIdAndPrivacyLevel(Long userId, String privacyLevel);
    List<UserPreference> findByPrivacyLevel(String privacyLevel);

    // Wyszukiwanie z LIKE dla wartości
    @Query("SELECT up FROM UserPreference up WHERE up.user.id = :userId AND up.preferenceValue LIKE %:value%")
    List<UserPreference> findByUserIdAndPreferenceValueContaining(@Param("userId") Long userId, @Param("value") String value);

    // Pobieranie kluczy dla użytkownika
    @Query("SELECT up.preferenceKey FROM UserPreference up WHERE up.user.id = :userId")
    List<String> findPreferenceKeysByUserId(@Param("userId") Long userId);

    // Pobieranie unikalnych kluczy w systemie
    @Query("SELECT DISTINCT up.preferenceKey FROM UserPreference up")
    List<String> findDistinctPreferenceKeys();

    // Statystyki
    @Query("SELECT COUNT(up) FROM UserPreference up WHERE up.user.id = :userId")
    Long countByUserId(@Param("userId") Long userId);

    @Query("SELECT COUNT(DISTINCT up.user.id) FROM UserPreference up WHERE up.preferenceKey = :preferenceKey")
    Long countUsersWithPreferenceKey(@Param("preferenceKey") String preferenceKey);

    // Bulk operations
    @Modifying
    @Query("UPDATE UserPreference up SET up.preferenceValue = :newValue WHERE up.preferenceKey = :preferenceKey AND up.preferenceValue = :oldValue")
    int updateAllByPreferenceKeyAndValue(@Param("preferenceKey") String preferenceKey,
                                         @Param("oldValue") String oldValue,
                                         @Param("newValue") String newValue);

    // Pobieranie z paginacją
    @Query("SELECT up FROM UserPreference up WHERE up.user.id = :userId ORDER BY up.preferenceKey")
    List<UserPreference> findByUserIdOrderByPreferenceKey(@Param("userId") Long userId);

    // Wyszukiwanie użytkowników z określoną preferencją
    @Query("SELECT up.user.id FROM UserPreference up WHERE up.preferenceKey = :preferenceKey AND up.preferenceValue = :preferenceValue")
    List<Long> findUserIdsByPreferenceKeyAndValue(@Param("preferenceKey") String preferenceKey,
                                                  @Param("preferenceValue") String preferenceValue);
}