package com.lms.assessment.repository;

import com.lms.assessment.entity.TestCase;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface TestCaseRepository extends JpaRepository<TestCase, UUID> {

    List<TestCase> findByQuestionIdOrderByIdAsc(UUID questionId);

    /** Students only see sample test cases — hidden ones are withheld. */
    List<TestCase> findByQuestionIdAndSampleTrueOrderByIdAsc(UUID questionId);

    long countByQuestionId(UUID questionId);
}
