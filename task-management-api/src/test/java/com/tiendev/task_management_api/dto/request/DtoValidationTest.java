package com.tiendev.task_management_api.dto.request;

import com.tiendev.task_management_api.model.enums.ProjectStatus;
import com.tiendev.task_management_api.model.enums.TaskPriority;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DtoValidationTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void taskUpdateRequestValidatesCorrectly() {
        // Valid
        TaskUpdateRequest valid = TaskUpdateRequest.builder()
                .title("Valid title")
                .description("Valid description")
                .clearDeadline(true)
                .build();
        assertTrue(validator.validate(valid).isEmpty());

        // Optional fields null
        TaskUpdateRequest optionalNull = TaskUpdateRequest.builder().build();
        assertTrue(validator.validate(optionalNull).isEmpty());

        // Blank title rejected
        TaskUpdateRequest blankTitle = TaskUpdateRequest.builder().title("   ").build();
        assertFalse(validator.validate(blankTitle).isEmpty());

        // Title exceeding 200 chars rejected
        TaskUpdateRequest longTitle = TaskUpdateRequest.builder().title("A".repeat(201)).build();
        assertFalse(validator.validate(longTitle).isEmpty());

        // Description exceeding 1000 chars rejected
        TaskUpdateRequest longDesc = TaskUpdateRequest.builder().description("D".repeat(1001)).build();
        assertFalse(validator.validate(longDesc).isEmpty());
    }

    @Test
    void workspaceUpdateRequestValidatesCorrectly() {
        // Valid
        WorkspaceUpdateRequest valid = WorkspaceUpdateRequest.builder()
                .name("Marketing Team")
                .description("Workspace for marketing")
                .build();
        assertTrue(validator.validate(valid).isEmpty());

        // Optional fields null
        WorkspaceUpdateRequest optionalNull = WorkspaceUpdateRequest.builder().build();
        assertTrue(validator.validate(optionalNull).isEmpty());

        // Blank name rejected
        WorkspaceUpdateRequest blankName = WorkspaceUpdateRequest.builder().name("   ").build();
        assertFalse(validator.validate(blankName).isEmpty());

        // Name exceeding 100 chars rejected
        WorkspaceUpdateRequest longName = WorkspaceUpdateRequest.builder().name("W".repeat(101)).build();
        assertFalse(validator.validate(longName).isEmpty());

        // Description exceeding 500 chars rejected
        WorkspaceUpdateRequest longDesc = WorkspaceUpdateRequest.builder().description("D".repeat(501)).build();
        assertFalse(validator.validate(longDesc).isEmpty());
    }

    @Test
    void userUpdateRequestValidatesCorrectly() {
        // Valid
        UserUpdateRequest valid = UserUpdateRequest.builder()
                .username("john_doe")
                .avatar("https://example.com/pic.png")
                .build();
        assertTrue(validator.validate(valid).isEmpty());

        // Optional fields null
        UserUpdateRequest optionalNull = UserUpdateRequest.builder().build();
        assertTrue(validator.validate(optionalNull).isEmpty());

        // Blank username rejected
        UserUpdateRequest blankUser = UserUpdateRequest.builder().username("   ").build();
        assertFalse(validator.validate(blankUser).isEmpty());

        // Username shorter than 3 chars rejected
        UserUpdateRequest shortUser = UserUpdateRequest.builder().username("ab").build();
        assertFalse(validator.validate(shortUser).isEmpty());

        // Username exceeding 50 chars rejected
        UserUpdateRequest longUser = UserUpdateRequest.builder().username("U".repeat(51)).build();
        assertFalse(validator.validate(longUser).isEmpty());

        // Avatar exceeding 500 chars rejected
        UserUpdateRequest longAvatar = UserUpdateRequest.builder().avatar("https://example.com/" + "a".repeat(490)).build();
        assertFalse(validator.validate(longAvatar).isEmpty());
    }

    @Test
    void createDtosValidateLimits() {
        // TaskCreateRequest limits
        TaskCreateRequest taskOver = TaskCreateRequest.builder()
                .projectId(1L)
                .title("T".repeat(201))
                .priority(TaskPriority.MEDIUM)
                .build();
        assertFalse(validator.validate(taskOver).isEmpty());

        TaskCreateRequest taskDescOver = TaskCreateRequest.builder()
                .projectId(1L)
                .title("Valid title")
                .priority(TaskPriority.MEDIUM)
                .description("D".repeat(1001))
                .build();
        assertFalse(validator.validate(taskDescOver).isEmpty());

        // WorkspaceCreateRequest limits
        WorkspaceCreateRequest wsNameOver = WorkspaceCreateRequest.builder().name("W".repeat(101)).build();
        assertFalse(validator.validate(wsNameOver).isEmpty());

        WorkspaceCreateRequest wsDescOver = WorkspaceCreateRequest.builder()
                .name("Valid ws")
                .description("D".repeat(501))
                .build();
        assertFalse(validator.validate(wsDescOver).isEmpty());

        // UserCreateRequest avatar limit
        UserCreateRequest userAvatarOver = UserCreateRequest.builder()
                .username("validuser")
                .email("valid@example.com")
                .password("password123")
                .avatar("https://example.com/" + "a".repeat(490))
                .build();
        assertFalse(validator.validate(userAvatarOver).isEmpty());

        // ProjectCreateRequest limits
        ProjectCreateRequest projNameOver = ProjectCreateRequest.builder()
                .workspaceId(1L)
                .name("P".repeat(101))
                .build();
        assertFalse(validator.validate(projNameOver).isEmpty());

        ProjectCreateRequest projDescOver = ProjectCreateRequest.builder()
                .workspaceId(1L)
                .name("Valid Proj")
                .description("D".repeat(1001))
                .build();
        assertFalse(validator.validate(projDescOver).isEmpty());
    }
}
