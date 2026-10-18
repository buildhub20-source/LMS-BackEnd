package com.lms.course.repository;

import com.lms.course.entity.LearningTime;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LearningTimeRepository extends JpaRepository<LearningTime, UUID> {

    Optional<LearningTime> findByStudentIdAndCourseIdAndDay(UUID studentId, UUID courseId, LocalDate day);

    /** Seconds per day across all courses, for days in [from, to]. Rows: [LocalDate, Long]. */
    @Query("""
            select t.day, sum(t.seconds) from LearningTime t
            where t.studentId = :studentId and t.day between :from and :to
            group by t.day
            """)
    List<Object[]> sumSecondsByDay(@Param("studentId") UUID studentId,
                                   @Param("from") LocalDate from,
                                   @Param("to") LocalDate to);
}
