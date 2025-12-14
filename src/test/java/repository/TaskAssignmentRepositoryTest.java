package com.meethub.domain.repository.jpa;

import com.meethub.domain.model.entity.TaskAssignment;
import com.meethub.domain.model.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class TaskAssignmentRepositoryTest {

    @Autowired
    private TaskAssignmentRepository repository;

    @Test
    @DisplayName("Should find task assignments by user id")
    void testFindByUserId() {
        List<TaskAssignment> assignments = repository.findByUserId(3L);
        assertThat(assignments).isNotEmpty();
        assertThat(assignments).allMatch(a -> a.getUser().getId().equals(3L));
    }

    @Test
    @DisplayName("Should find task assignments by task id")
    void testFindByTaskId() {
        List<TaskAssignment> assignments = repository.findByTaskId(1L);
        assertThat(assignments).hasSize(1);
        assertThat(assignments.get(0).getTask().getId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("Should paginate task assignments by user id")
    void testFindByUserIdWithPagination() {
        var page = repository.findByUserId(3L, PageRequest.of(0, 10));
        assertThat(page.getContent()).isNotEmpty();
        assertThat(page.getContent()).allMatch(a -> a.getUser().getId().equals(3L));
    }


    @Test
    @DisplayName("Should find assigned users by task id")
    void testFindAssignedUsersByTaskId() {
        List<User> users = repository.findAssignedUsersByTaskId(1L);
        assertThat(users).hasSize(1);
        assertThat(users.get(0).getId()).isEqualTo(3L);
    }
}
