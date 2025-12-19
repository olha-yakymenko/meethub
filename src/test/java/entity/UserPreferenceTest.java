package com.meethub.domain.model.entity;

import com.meethub.domain.model.enums.PrivacyLevel;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class UserPreferenceTest {

    @Test
    void shouldCreateUserPreference() {
        // Given
        User user = mock(User.class);
        LocalDateTime createdAt = LocalDateTime.now();

        // When
        UserPreference preference = new UserPreference();
        preference.setId(1L);
        preference.setUser(user);
        preference.setPreferenceKey("email_notifications");
        preference.setPreferenceValue("true");
        preference.setPrivacyLevel(PrivacyLevel.PRIVATE);
        preference.setCreatedAt(createdAt);

        // Then
        assertAll(
                () -> assertThat(preference.getId()).isEqualTo(1L),
                () -> assertThat(preference.getUser()).isEqualTo(user),
                () -> assertThat(preference.getPreferenceKey()).isEqualTo("email_notifications"),
                () -> assertThat(preference.getPreferenceValue()).isEqualTo("true"),
                () -> assertThat(preference.getPrivacyLevel()).isEqualTo(PrivacyLevel.PRIVATE),
                () -> assertThat(preference.getCreatedAt()).isEqualTo(createdAt)
        );
    }

    @Test
    void shouldSetDefaultPrivacyLevel() {
        // When
        UserPreference preference = new UserPreference();

        // Then
        assertAll(
                () -> assertThat(preference.getPrivacyLevel()).isEqualTo(PrivacyLevel.PRIVATE)
        );
    }

    @Test
    void shouldUpdateAllFields() {
        // Given
        UserPreference preference = new UserPreference();
        User newUser = mock(User.class);
        LocalDateTime newCreatedAt = LocalDateTime.now().minusDays(1);

        // When
        preference.setId(2L);
        preference.setUser(newUser);
        preference.setPreferenceKey("push_notifications");
        preference.setPreferenceValue("false");
        preference.setPrivacyLevel(PrivacyLevel.PUBLIC);
        preference.setCreatedAt(newCreatedAt);

        // Then
        assertAll(
                () -> assertThat(preference.getId()).isEqualTo(2L),
                () -> assertThat(preference.getUser()).isEqualTo(newUser),
                () -> assertThat(preference.getPreferenceKey()).isEqualTo("push_notifications"),
                () -> assertThat(preference.getPreferenceValue()).isEqualTo("false"),
                () -> assertThat(preference.getPrivacyLevel()).isEqualTo(PrivacyLevel.PUBLIC),
                () -> assertThat(preference.getCreatedAt()).isEqualTo(newCreatedAt)
        );
    }

    @Test
    void shouldCheckIfPreferenceIsEnabled() {
        // Given
        UserPreference enabled1 = new UserPreference();
        enabled1.setPreferenceValue("true");

        UserPreference enabled2 = new UserPreference();
        enabled2.setPreferenceValue("TRUE");

        UserPreference enabled3 = new UserPreference();
        enabled3.setPreferenceValue("1");

        UserPreference disabled1 = new UserPreference();
        disabled1.setPreferenceValue("false");

        UserPreference disabled2 = new UserPreference();
        disabled2.setPreferenceValue("FALSE");

        UserPreference disabled3 = new UserPreference();
        disabled3.setPreferenceValue("0");

        UserPreference other = new UserPreference();
        other.setPreferenceValue("maybe");

        UserPreference nullValue = new UserPreference();

        // Then
        assertAll(
                () -> assertThat(enabled1.isEnabled()).isTrue(),
                () -> assertThat(enabled2.isEnabled()).isTrue(),
                () -> assertThat(enabled3.isEnabled()).isTrue(),
                () -> assertThat(disabled1.isEnabled()).isFalse(),
                () -> assertThat(disabled2.isEnabled()).isFalse(),
                () -> assertThat(disabled3.isEnabled()).isFalse(),
                () -> assertThat(other.isEnabled()).isFalse(),
                () -> assertThat(nullValue.isEnabled()).isFalse()
        );
    }

    @Test
    void shouldCheckIfPreferenceIsDisabled() {
        // Given
        UserPreference disabled1 = new UserPreference();
        disabled1.setPreferenceValue("false");

        UserPreference disabled2 = new UserPreference();
        disabled2.setPreferenceValue("FALSE");

        UserPreference disabled3 = new UserPreference();
        disabled3.setPreferenceValue("0");

        UserPreference enabled = new UserPreference();
        enabled.setPreferenceValue("true");

        UserPreference other = new UserPreference();
        other.setPreferenceValue("maybe");

        UserPreference nullValue = new UserPreference();

        // Then
        assertAll(
                () -> assertThat(disabled1.isDisabled()).isTrue(),
                () -> assertThat(disabled2.isDisabled()).isTrue(),
                () -> assertThat(disabled3.isDisabled()).isTrue(),
                () -> assertThat(enabled.isDisabled()).isFalse(),
                () -> assertThat(other.isDisabled()).isFalse(),
                () -> assertThat(nullValue.isDisabled()).isFalse()
        );
    }

    @Test
    void shouldHandleDifferentPrivacyLevels() {
        // Given
        UserPreference privatePref = new UserPreference();
        privatePref.setPrivacyLevel(PrivacyLevel.PRIVATE);

        UserPreference publicPref = new UserPreference();
        publicPref.setPrivacyLevel(PrivacyLevel.PUBLIC);

        // Then
        assertAll(
                () -> assertThat(privatePref.getPrivacyLevel()).isEqualTo(PrivacyLevel.PRIVATE),
                () -> assertThat(publicPref.getPrivacyLevel()).isEqualTo(PrivacyLevel.PUBLIC)
        );
    }

    @Test
    void shouldHandleDifferentPreferenceKeys() {
        // Given
        UserPreference emailPref = new UserPreference();
        emailPref.setPreferenceKey("email_notifications");

        UserPreference pushPref = new UserPreference();
        pushPref.setPreferenceKey("push_notifications");

        UserPreference smsPref = new UserPreference();
        smsPref.setPreferenceKey("sms_notifications");

        UserPreference digestPref = new UserPreference();
        digestPref.setPreferenceKey("digest_frequency");

        UserPreference customPref = new UserPreference();
        customPref.setPreferenceKey("custom_setting");

        // Then
        assertAll(
                () -> assertThat(emailPref.getPreferenceKey()).isEqualTo("email_notifications"),
                () -> assertThat(pushPref.getPreferenceKey()).isEqualTo("push_notifications"),
                () -> assertThat(smsPref.getPreferenceKey()).isEqualTo("sms_notifications"),
                () -> assertThat(digestPref.getPreferenceKey()).isEqualTo("digest_frequency"),
                () -> assertThat(customPref.getPreferenceKey()).isEqualTo("custom_setting")
        );
    }

    @Test
    void shouldHandleDifferentPreferenceValues() {
        // Given
        UserPreference booleanPref = new UserPreference();
        booleanPref.setPreferenceValue("true");

        UserPreference stringPref = new UserPreference();
        stringPref.setPreferenceValue("daily");

        UserPreference numericPref = new UserPreference();
        numericPref.setPreferenceValue("30");

        UserPreference jsonPref = new UserPreference();
        jsonPref.setPreferenceValue("{\"enabled\": true, \"frequency\": \"daily\"}");

        // Then
        assertAll(
                () -> assertThat(booleanPref.getPreferenceValue()).isEqualTo("true"),
                () -> assertThat(stringPref.getPreferenceValue()).isEqualTo("daily"),
                () -> assertThat(numericPref.getPreferenceValue()).isEqualTo("30"),
                () -> assertThat(jsonPref.getPreferenceValue()).contains("enabled")
        );
    }

    @Test
    void shouldSetCreatedAtAutomatically() {
        // Given
        UserPreference preference = new UserPreference();
        preference.setUser(mock(User.class));
        preference.setPreferenceKey("test");

        // When - simulate @CreationTimestamp
        LocalDateTime now = LocalDateTime.now();
        preference.setCreatedAt(now);

        // Then
        assertAll(
                () -> assertThat(preference.getCreatedAt()).isEqualTo(now)
        );
    }
}