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

    boolean existsByEmail(String email);

    List<User> findByEmailContainingOrFirstNameContainingOrLastNameContaining(
                String email, String firstName, String lastName);

}