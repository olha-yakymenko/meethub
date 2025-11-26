// src/main/java/com/meethub/domain/repository/jpa/UserRepository.java
package com.meethub.domain.repository.jpa;

import com.meethub.domain.model.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    @Query("SELECT u FROM User u WHERE u.email LIKE %:query% OR u.firstName LIKE %:query% OR u.lastName LIKE %:query%")
    List<User> searchUsers(@Param("query") String query);

    boolean existsByEmail(String email);

    List<User> findByRoleIn(List<String> roles);

    /**
     * Znajdź użytkowników po grupie/departamencie
     */
    @Query("SELECT u FROM User u JOIN UserGroupMember ugm ON u.id = ugm.user.id " +
            "JOIN UserGroup ug ON ugm.group.id = ug.id " +
            "WHERE ug.name = :groupName")
    List<User> findByGroupName(@Param("groupName") String groupName);


    /**
     * Znajdź użytkowników po wielu emailach
     */
    @Query("SELECT u FROM User u WHERE u.email IN :emails")
    List<User> findByEmails(@Param("emails") List<String> emails);

    /**
     * Znajdź użytkowników po ID
     */
    @Query("SELECT u FROM User u WHERE u.id IN :userIds")
    List<User> findByIds(@Param("userIds") List<Long> userIds);

    List<User> findByEmailContainingOrFirstNameContainingOrLastNameContaining(
                String email, String firstName, String lastName);


    @Query("SELECT u FROM User u WHERE u.emailNotificationsEnabled = true")
    List<User> findByEmailNotificationsEnabledTrue();

    @Query("SELECT u FROM User u WHERE u.pushNotificationsEnabled = true")
    List<User> findByPushNotificationsEnabledTrue();

    @Query("SELECT u FROM User u WHERE u.smsNotificationsEnabled = true AND u.phoneNumber IS NOT NULL")
    List<User> findBySmsNotificationsEnabledTrue();

    // Metoda dla użytkowników związanych ze spotkaniem
    @Query("SELECT DISTINCT u FROM User u " +
            "LEFT JOIN u.meetingParticipants mp " +
            "LEFT JOIN mp.meeting m " +
            "WHERE m.id = :meetingId")
    List<User> findUsersByMeetingId(@Param("meetingId") Long meetingId);

    // Wyszukiwanie użytkowników z określonym kanałem powiadomień
    @Query("SELECT u FROM User u JOIN u.enabledNotificationChannels enc WHERE enc = :channel")
    List<User> findByEnabledNotificationChannelsContaining(@Param("channel") String channel);

    // Statystyki
    @Query("SELECT COUNT(u) FROM User u WHERE u.enabled = true")
    Long countActiveUsers();

    @Query("SELECT COUNT(u) FROM User u WHERE u.emailNotificationsEnabled = true")
    Long countUsersWithEmailNotifications();

    // Wyszukiwanie z paginacją i filtrami
    @Query("SELECT u FROM User u WHERE " +
            "(:search IS NULL OR LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(u.firstName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(u.lastName) LIKE LOWER(CONCAT('%', :search, '%')))")
    List<User> findBySearchTerm(@Param("search") String search);


    List<User> findByDigestEnabledTrue();

}