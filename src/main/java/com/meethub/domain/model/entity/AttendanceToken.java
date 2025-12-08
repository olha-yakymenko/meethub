package com.meethub.domain.model.entity;

import com.meethub.domain.model.enums.AttendanceTokenStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "attendance_tokens")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttendanceToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String token;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "meeting_id", nullable = false)
    private Meeting meeting;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column
    private LocalDateTime usedAt;

    @Column
    @Enumerated(EnumType.STRING)
    private AttendanceTokenStatus status;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        status = AttendanceTokenStatus.ACTIVE;
    }

    public boolean isValid() {
        return status == AttendanceTokenStatus.ACTIVE &&
                LocalDateTime.now().isBefore(expiresAt);
    }
}