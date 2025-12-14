package com.meethub.domain.repository.jpa;

import com.meethub.domain.model.entity.Task;
import com.meethub.domain.model.enums.TaskStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class TaskRepositoryTest {

    @Autowired
    private TaskRepository taskRepository;

    @Test
    @DisplayName("Should find tasks by meeting id")
    void testFindByMeetingId() {
        // Pobieramy ID spotkania 'Test Meeting'
        Long meetingId = 1L; // dopasuj do ID w bazie testowej, np. H2 w pamięci

        List<Task> tasks = taskRepository.findByMeetingId(meetingId);

        assertThat(tasks).isNotEmpty();
        assertThat(tasks).hasSize(3); // mamy 3 zadania przypisane do Test Meeting
        assertThat(tasks).allSatisfy(task -> assertThat(task.getMeeting().getId()).isEqualTo(meetingId));
    }

    @Test
    @DisplayName("Should find tasks by creator user id")
    void testFindByCreatedById() {
        // Sprawdzamy zadania stworzone przez test.organizer@example.com
        Long organizerId = 2L; // dopasuj do faktycznego ID użytkownika w H2

        List<Task> tasks = taskRepository.findByCreatedById(organizerId);

        assertThat(tasks).isNotEmpty();
        assertThat(tasks).hasSize(2); // Organizer utworzył 2 zadania
        assertThat(tasks).allSatisfy(task -> assertThat(task.getCreatedBy().getId()).isEqualTo(organizerId));
    }

    @Test
    @DisplayName("Should find all tasks")
    void testFindAll() {
        List<Task> tasks = taskRepository.findAll();

        assertThat(tasks).isNotEmpty();
        assertThat(tasks).hasSizeGreaterThanOrEqualTo(3); // 3 zadania w data.sql
    }
}
