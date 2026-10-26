package com.lms.assessment.service;

import com.lms.assessment.dto.response.SubmissionHistoryResponse;
import com.lms.assessment.entity.AssessmentAttempt;
import com.lms.assessment.entity.AttemptStatus;
import com.lms.assessment.repository.AssessmentAttemptRepository;
import com.lms.user.entity.User;
import com.lms.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SubmissionHistoryService {
    private final AssessmentAttemptRepository attempts;
    private final UserRepository users;

    /** Two bulk queries, regardless of the number of assessments or learners. */
    public List<SubmissionHistoryResponse> list(UUID instructorId) {
        var rows = instructorId == null ? attempts.findAllByOrderByStartedAtDesc()
                : attempts.findByAssessment_CreatedByOrderByStartedAtDesc(instructorId);
        if (rows.isEmpty()) return List.of();
        var names = users.findAllById(rows.stream().map(AssessmentAttempt::getStudentId).collect(Collectors.toSet()))
                .stream().collect(Collectors.toMap(User::getId, user -> user));
        record Key(UUID assessment, UUID student) {}
        Map<Key, List<AssessmentAttempt>> grouped = new LinkedHashMap<>();
        for (var attempt : rows) {
            grouped.computeIfAbsent(new Key(attempt.getAssessment().getId(), attempt.getStudentId()),
                    key -> new ArrayList<>()).add(attempt);
        }
        List<SubmissionHistoryResponse> result = new ArrayList<>();
        for (var group : grouped.values()) {
            var latest = group.get(0);
            var assessment = latest.getAssessment();
            var user = names.get(latest.getStudentId());
            Integer score = latest.getScore();
            Double percentage = score == null || assessment.getTotalMarks() <= 0 ? null
                    : Math.round(score * 1000.0 / assessment.getTotalMarks()) / 10.0;
            String grade = percentage == null ? null : percentage >= 90 ? "A" : percentage >= 80 ? "B"
                    : percentage >= 70 ? "C" : percentage >= 60 ? "D" : "F";
            result.add(new SubmissionHistoryResponse(assessment.getId(), assessment.getTitle(), assessment.getTotalMarks(),
                    assessment.getDurationMinutes(), assessment.getStatus().name(), latest.getStudentId(),
                    user == null ? "Student" : user.getName(), user == null ? "" : user.getEmail(),
                    latest.getStatus() == AttemptStatus.SUBMITTED ? "COMPLETED" : latest.getStatus().name(),
                    score, assessment.getTotalMarks(), percentage, grade, percentage == null ? null : percentage >= 50,
                    group.size(), latest.getSubmittedAt()));
        }
        result.sort(Comparator.comparing(SubmissionHistoryResponse::submittedAt,
                Comparator.nullsLast(Comparator.reverseOrder())));
        return result;
    }
}
