package com.lms.assessment.repository;

import com.lms.assessment.entity.QuestionOption;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface QuestionOptionRepository extends JpaRepository<QuestionOption, UUID> {

    List<QuestionOption> findByQuestionIdOrderByOrderIndexAsc(UUID questionId);

    List<QuestionOption> findByQuestionId(UUID questionId);

    long countByQuestionId(UUID questionId);

    long countByQuestionIdAndCorrectTrue(UUID questionId);

    void deleteByQuestionId(UUID questionId);
}
