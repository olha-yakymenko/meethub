package com.meethub.domain.repository.jpa;

import com.meethub.domain.model.entity.TaskAssignment;
import com.meethub.domain.model.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

@DataJpaTest
@ActiveProfiles("postgres")
class TaskAssignmentRepositoryTest {

    @Autowired
    private TaskAssignmentRepository repository;

    @Test
    @DisplayName("Should find task assignments by user id")
    void testFindByUserId() {
        List<TaskAssignment> assignments = repository.findByUserId(3L);

        assertAll("Task assignments by user id",
                () -> assertThat(assignments).isNotEmpty(),
                () -> assertThat(assignments).allMatch(a -> a.getUser().getId().equals(3L))
        );
    }

    @Test
    @DisplayName("Should find task assignments by task id")
    void testFindByTaskId() {
        List<TaskAssignment> assignments = repository.findByTaskId(1L);

        assertAll("Task assignments by task id",
                () -> assertThat(assignments).hasSize(1),
                () -> assertThat(assignments.get(0).getTask().getId()).isEqualTo(1L)
        );
    }

    @Test
    @DisplayName("Should paginate task assignments by user id")
    void testFindByUserIdWithPagination() {
        var page = repository.findByUserId(3L, PageRequest.of(0, 10));

        assertAll("Paginated task assignments by user id",
                () -> assertThat(page.getContent()).isNotEmpty(),
                () -> assertThat(page.getContent()).allMatch(a -> a.getUser().getId().equals(3L))
        );
    }

    @Test
    @DisplayName("Should find assigned users by task id")
    void testFindAssignedUsersByTaskId() {
        List<User> users = repository.findAssignedUsersByTaskId(1L);

        assertAll("Assigned users by task id",
                () -> assertThat(users).hasSize(1),
                () -> assertThat(users.get(0).getId()).isEqualTo(3L)
        );
    }
}
