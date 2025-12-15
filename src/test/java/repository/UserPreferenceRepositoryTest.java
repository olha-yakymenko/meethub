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

@DataJpaTest
@ActiveProfiles("postgres")
class UserPreferenceRepositoryTest {

    @Autowired
    private UserPreferenceRepository userPreferenceRepository;

    @Test
    @DisplayName("Should find all preferences by user id")
    void testFindByUserId() {
        Long userId = 3L; // istniejący użytkownik w data.sql

        List<UserPreference> preferences = userPreferenceRepository.findByUserId(userId);

        assertThat(preferences).isNotEmpty();
        assertThat(preferences).hasSize(4);
        assertThat(preferences).allSatisfy(pref -> assertThat(pref.getUser().getId()).isEqualTo(userId));
    }

    @Test
    @DisplayName("Should find preference by user id and key")
    void testFindByUserIdAndPreferenceKey() {
        Long userId = 3L;
        String key = "meeting_reminders";

        Optional<UserPreference> preference = userPreferenceRepository.findByUserIdAndPreferenceKey(userId, key);

        assertThat(preference).isPresent();
        assertThat(preference.get().getPreferenceKey()).isEqualTo(key);
        assertThat(preference.get().getUser().getId()).isEqualTo(userId);
    }

}
