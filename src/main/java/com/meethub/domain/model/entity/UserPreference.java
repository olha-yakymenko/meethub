//package com.meethub.domain.model.entity;
//
//import com.meethub.domain.model.enums.PrivacyLevel;
//import jakarta.persistence.*;
//import lombok.Getter;
//import lombok.Setter;
//import org.hibernate.annotations.CreationTimestamp;
//
//import java.time.LocalDateTime;
//
//@Entity
//@Table(name = "user_preferences")
//@Getter
//@Setter
//public class UserPreference {
//    @Id
//    @GeneratedValue(strategy = GenerationType.IDENTITY)
//    private Long id;
//
//    @ManyToOne(fetch = FetchType.LAZY)
//    @JoinColumn(name = "user_id", nullable = false)
//    private User user;
//
//    @Column(name = "preference_key", nullable = false, length = 100)
//    private String preferenceKey;
//
//    @Column(name = "preference_value", length = 500)
//    private String preferenceValue;
//
//    @Enumerated(EnumType.STRING)
//    @Column(name = "privacy_level", nullable = false)
//    private PrivacyLevel privacyLevel = PrivacyLevel.PRIVATE;
//
//    @CreationTimestamp
//    @Column(name = "created_at", nullable = false, updatable = false)
//    private LocalDateTime createdAt;
//}




// UserPreference.java - dodaj tę metodę
package com.meethub.domain.model.entity;

import com.meethub.domain.model.enums.PrivacyLevel;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_preferences")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserPreference {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "preference_key", nullable = false, length = 100)
    private String preferenceKey;

    @Column(name = "preference_value", length = 500)
    private String preferenceValue;

    @Enumerated(EnumType.STRING)
    @Column(name = "privacy_level", nullable = false)
    private PrivacyLevel privacyLevel = PrivacyLevel.PRIVATE;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;


    // Metoda pomocnicza
    public boolean isEnabled() {
        return "true".equalsIgnoreCase(preferenceValue) || "1".equals(preferenceValue);
    }

    public boolean isDisabled() {
        return "false".equalsIgnoreCase(preferenceValue) || "0".equals(preferenceValue);
    }
}