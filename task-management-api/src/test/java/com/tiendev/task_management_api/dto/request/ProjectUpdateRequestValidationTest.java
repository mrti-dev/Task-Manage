package com.tiendev.task_management_api.dto.request;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProjectUpdateRequestValidationTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void rejectsBlankProjectName() {
        ProjectUpdateRequest request = ProjectUpdateRequest.builder().name("   ").build();

        assertFalse(validator.validate(request).isEmpty());
    }

    @Test
    void acceptsValidPartialUpdate() {
        ProjectUpdateRequest request = ProjectUpdateRequest.builder().name("Launch plan").build();

        assertTrue(validator.validate(request).isEmpty());
    }
}
