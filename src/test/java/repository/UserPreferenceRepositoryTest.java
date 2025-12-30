package com.meethub.domain.repository.jpa;

import com.meethub.domain.model.entity.UserPreference;
import com.meethub.domain.model.enums.PrivacyLevel;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

@DataJpaTest
@ActiveProfiles("postgres")
class UserPreferenceRepositoryTest {

    @Autowired
    private UserPreferenceRepository userPreferenceRepository;

    @Test
    @DisplayName("Should find all preferences by user id")
    void testFindByUserId() {
        Long userId = 3L;

        List<UserPreference> preferences = userPreferenceRepository.findByUserId(userId);

        assertAll("All preferences by user",
                () -> assertThat(preferences).isNotEmpty(),
                () -> assertThat(preferences).hasSize(4),
                () -> preferences.forEach(pref ->
                        assertThat(pref.getUser().getId()).isEqualTo(userId)
                )
        );
    }

    @Test
    @DisplayName("Should find preference by user id and key")
    void testFindByUserIdAndPreferenceKey() {
        Long userId = 3L;
        String key = "meeting_reminders";

        Optional<UserPreference> preference = userPreferenceRepository.findByUserIdAndPreferenceKey(userId, key);

        assertAll("Preference by user and key",
                () -> assertThat(preference).isPresent(),
                () -> assertThat(preference).map(UserPreference::getPreferenceKey).hasValue(key),
                () -> assertThat(preference).map(pref -> pref.getUser().getId()).hasValue(userId)
        );
    }
}
