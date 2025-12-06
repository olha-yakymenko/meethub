package com.meethub.domain.repository.jpa;

import com.meethub.domain.model.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

    List<Category> findByCreatedById(Long userId);

    List<Category> findByNameContainingIgnoreCase(String name);

    List<Category> findByCreatedByIdAndNameContainingIgnoreCase(Long userId, String name);
}