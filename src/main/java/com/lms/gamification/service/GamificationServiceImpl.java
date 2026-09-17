package com.lms.gamification.service;

import com.lms.common.response.PageResponse;
import com.lms.gamification.dto.response.*;
import com.lms.gamification.entity.*;
import com.lms.gamification.repository.*;
import com.lms.student.repository.StudentBatchRepository;
import com.lms.user.entity.User;
import com.lms.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class GamificationServiceImpl implements GamificationService {

    private final PointRuleRepository pointRuleRepository;
    private final PointsLedgerRepository pointsLedgerRepository;
    private final BadgeRepository badgeRepository;
    private final StudentBadgeRepository studentBadgeRepository;
    private final GamificationLevelRepository levelRepository;
    private final LearningStreakRepository learningStreakRepository;
    private final StreakActivityRepository streakActivityRepository;
    private final MilestoneRepository milestoneRepository;
    private final StudentMilestoneRepository studentMilestoneRepository;
    private final UserRepository userRepository;
    private final StudentBatchRepository studentBatchRepository;

    // ─── Points ─────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public void awardPoints(UUID studentId, GamificationEventType eventType, UUID sourceEntityId) {
        String idempotencyKey = eventType.name() + ":" + sourceEntityId + ":" + studentId;

        if (pointsLedgerRepository.existsByIdempotencyKey(idempotencyKey)) {
            log.debug("Points already awarded for key={}", idempotencyKey);
            return;
        }

        Optional<PointRule> ruleOpt = pointRuleRepository.findByEventTypeAndActiveTrue(eventType.name());
        if (ruleOpt.isEmpty()) {
            log.debug("No active point rule for eventType={}", eventType);
            return;
        }

        PointsLedger entry = PointsLedger.builder()
                .studentId(studentId)
                .points(ruleOpt.get().getPoints())
                .eventType(eventType.name())
                .sourceEntityId(sourceEntityId)
                .idempotencyKey(idempotencyKey)
                .createdAt(Instant.now())
                .build();
        pointsLedgerRepository.save(entry);
        log.debug("Awarded {} points to student={} for event={}", ruleOpt.get().getPoints(), studentId, eventType);
    }

    // ─── Badges ─────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public void checkAndAwardBadges(UUID studentId) {
        List<Badge> activeBadges = badgeRepository.findByActiveTrue();
        Map<String, Long> counts = buildCriteriaCountMap(studentId);

        for (Badge badge : activeBadges) {
            if (studentBadgeRepository.existsByStudentIdAndBadgeId(studentId, badge.getId())) {
                continue;
            }
            long actualCount = counts.getOrDefault(badge.getCriteriaType(), 0L);
            if (actualCount >= badge.getCriteriaValue()) {
                StudentBadge award = StudentBadge.builder()
                        .studentId(studentId)
                        .badge(badge)
                        .awardedAt(Instant.now())
                        .build();
                studentBadgeRepository.save(award);
                log.debug("Awarded badge '{}' to student={}", badge.getName(), studentId);

                // Earning a badge also awards points
                awardPoints(studentId, GamificationEventType.BADGE_EARNED, badge.getId());
            }
        }
    }

    // ─── Milestones ─────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public void checkAndAwardMilestones(UUID studentId) {
        List<Milestone> milestones = milestoneRepository.findByActiveTrueOrderBySortOrderAsc();
        Map<String, Long> counts = buildCriteriaCountMap(studentId);

        for (Milestone milestone : milestones) {
            if (studentMilestoneRepository.existsByStudentIdAndMilestoneId(studentId, milestone.getId())) {
                continue;
            }
            long actualCount = counts.getOrDefault(milestone.getCriteriaType(), 0L);
            if (actualCount >= milestone.getCriteriaValue()) {
                StudentMilestone completion = StudentMilestone.builder()
                        .studentId(studentId)
                        .milestone(milestone)
                        .completedAt(Instant.now())
                        .build();
                studentMilestoneRepository.save(completion);
                log.debug("Awarded milestone '{}' to student={}", milestone.getName(), studentId);
            }
        }
    }

    // ─── Streaks ────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public void recordDailyActivity(UUID studentId) {
        LocalDate today = LocalDate.now(ZoneOffset.UTC);

        if (streakActivityRepository.existsByStudentIdAndActivityDate(studentId, today)) {
            return; // Already recorded today
        }

        streakActivityRepository.save(StreakActivity.builder()
                .studentId(studentId)
                .activityDate(today)
                .build());

        LearningStreak streak = learningStreakRepository.findByStudentId(studentId)
                .orElseGet(() -> {
                    LearningStreak newStreak = LearningStreak.builder()
                            .studentId(studentId)
                            .currentStreak(0)
                            .longestStreak(0)
                            .build();
                    return learningStreakRepository.save(newStreak);
                });

        LocalDate lastActivity = streak.getLastActivityDate();

        if (lastActivity == null) {
            streak.setCurrentStreak(1);
        } else {
            long daysBetween = ChronoUnit.DAYS.between(lastActivity, today);
            // Allow weekends: if last activity was Friday and today is Monday, that's 3 days but streak continues
            boolean weekendBridge = isWeekendBridge(lastActivity, today);

            if (daysBetween == 1 || weekendBridge) {
                streak.setCurrentStreak(streak.getCurrentStreak() + 1);
            } else {
                streak.setCurrentStreak(1); // Reset streak
            }
        }

        if (streak.getCurrentStreak() > streak.getLongestStreak()) {
            streak.setLongestStreak(streak.getCurrentStreak());
        }
        streak.setLastActivityDate(today);
        learningStreakRepository.save(streak);

        // Award daily activity points
        awardPoints(studentId, GamificationEventType.DAILY_ACTIVITY, UUID.nameUUIDFromBytes(
                ("DAILY:" + studentId + ":" + today).getBytes()));
    }

    /**
     * Checks if the gap between two dates is due only to weekend days.
     * Friday -> Monday (3 days gap) should not break the streak.
     */
    private boolean isWeekendBridge(LocalDate from, LocalDate to) {
        long daysBetween = ChronoUnit.DAYS.between(from, to);
        if (daysBetween <= 1) return false; // Handled by direct check

        // Check if all days between from+1 and to-1 are weekends
        LocalDate cursor = from.plusDays(1);
        while (cursor.isBefore(to)) {
            DayOfWeek dow = cursor.getDayOfWeek();
            if (dow != DayOfWeek.SATURDAY && dow != DayOfWeek.SUNDAY) {
                return false;
            }
            cursor = cursor.plusDays(1);
        }
        return true;
    }

    // ─── Dashboard Queries ──────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public GamificationSummaryResponse getSummary(UUID studentId) {
        int totalPoints = pointsLedgerRepository.sumPointsByStudentId(studentId);
        LevelResponse level = computeLevel(totalPoints);
        LearningStreak streak = learningStreakRepository.findByStudentId(studentId)
                .orElse(LearningStreak.builder().currentStreak(0).longestStreak(0).build());
        long badgeCount = studentBadgeRepository.countByStudentId(studentId);
        long milestoneCount = studentMilestoneRepository.findByStudentId(studentId).size();
        LeaderboardEntryResponse myEntry = getStudentLeaderboardEntry(studentId, "all", null);
        Long rank = myEntry != null ? myEntry.rank() : null;

        return new GamificationSummaryResponse(
                totalPoints, level, level.progress(),
                streak.getCurrentStreak(), streak.getLongestStreak(),
                badgeCount, milestoneCount, rank
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<BadgeResponse> getStudentBadges(UUID studentId) {
        List<StudentBadge> earned = studentBadgeRepository.findByStudentId(studentId);
        Set<UUID> earnedBadgeIds = earned.stream()
                .collect(Collectors.toMap(sb -> sb.getBadge().getId(), sb -> sb, (a, b) -> a))
                .values().stream()
                .collect(Collectors.toMap(sb -> sb.getBadge().getId(), sb -> sb))
                .keySet();

        Map<UUID, Instant> awardedMap = earned.stream()
                .collect(Collectors.toMap(sb -> sb.getBadge().getId(), StudentBadge::getAwardedAt, (a, b) -> a));

        List<Badge> all = badgeRepository.findByActiveTrue();
        return all.stream().map(b -> new BadgeResponse(
                b.getId(), b.getName(), b.getDescription(), b.getIcon(), b.getCategory(),
                b.getCriteriaType(), b.getCriteriaValue(), b.isActive(),
                awardedMap.getOrDefault(b.getId(), null)
        )).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<MilestoneResponse> getStudentMilestones(UUID studentId) {
        List<Milestone> milestones = milestoneRepository.findByActiveTrueOrderBySortOrderAsc();
        List<StudentMilestone> completed = studentMilestoneRepository.findByStudentId(studentId);
        Map<UUID, Instant> completedMap = completed.stream()
                .collect(Collectors.toMap(sm -> sm.getMilestone().getId(), StudentMilestone::getCompletedAt, (a, b) -> a));

        return milestones.stream().map(m -> new MilestoneResponse(
                m.getId(), m.getKey(), m.getName(), m.getDescription(), m.getIcon(),
                m.getCriteriaType(), m.getCriteriaValue(), m.getSortOrder(), m.isActive(),
                completedMap.containsKey(m.getId()),
                completedMap.getOrDefault(m.getId(), null)
        )).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public StreakResponse getStudentStreak(UUID studentId) {
        LearningStreak streak = learningStreakRepository.findByStudentId(studentId)
                .orElse(LearningStreak.builder().currentStreak(0).longestStreak(0).build());
        return new StreakResponse(streak.getCurrentStreak(), streak.getLongestStreak(), streak.getLastActivityDate());
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<PointsHistoryResponse> getPointsHistory(UUID studentId, Pageable pageable) {
        Page<PointsLedger> page = pointsLedgerRepository.findByStudentIdOrderByCreatedAtDesc(studentId, pageable);
        return PageResponse.from(page, entry -> new PointsHistoryResponse(
                entry.getId(), entry.getPoints(), entry.getEventType(),
                entry.getSourceEntityId(), entry.getCreatedAt()));
    }

    // ─── Leaderboard ────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public PageResponse<LeaderboardEntryResponse> getLeaderboard(String period, UUID batchId, Pageable pageable) {
        // Build leaderboard from points ledger grouped by student
        Instant since = periodToSince(period);

        // Get all users with points
        List<LeaderboardRaw> rawEntries = buildLeaderboardRaw(since, batchId);

        // Sort and rank
        rawEntries.sort(Comparator.comparingInt(LeaderboardRaw::totalPoints).reversed()
                .thenComparing(LeaderboardRaw::studentName, String.CASE_INSENSITIVE_ORDER));

        // Assign ranks (dense ranking)
        List<LeaderboardEntryResponse> ranked = new ArrayList<>();
        long rank = 0;
        int prevPoints = Integer.MAX_VALUE;
        for (LeaderboardRaw raw : rawEntries) {
            if (raw.totalPoints() < prevPoints) {
                rank++;
                prevPoints = raw.totalPoints();
            }
            ranked.add(new LeaderboardEntryResponse(
                    rank, raw.studentId(), raw.studentName(), raw.totalPoints(),
                    raw.levelTitle(), raw.badgeCount()));
        }

        // Paginate
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), ranked.size());
        List<LeaderboardEntryResponse> pageContent = start < ranked.size() ? ranked.subList(start, end) : List.of();
        int totalPages = (int) Math.ceil((double) ranked.size() / pageable.getPageSize());

        return new PageResponse<>(pageContent, pageable.getPageNumber(), pageable.getPageSize(),
                ranked.size(), totalPages, end >= ranked.size());
    }

    @Override
    @Transactional(readOnly = true)
    public LeaderboardEntryResponse getStudentLeaderboardEntry(UUID studentId, String period, UUID batchId) {
        Instant since = periodToSince(period);
        List<LeaderboardRaw> rawEntries = buildLeaderboardRaw(since, batchId);

        rawEntries.sort(Comparator.comparingInt(LeaderboardRaw::totalPoints).reversed()
                .thenComparing(LeaderboardRaw::studentName, String.CASE_INSENSITIVE_ORDER));

        long rank = 0;
        int prevPoints = Integer.MAX_VALUE;
        for (LeaderboardRaw raw : rawEntries) {
            if (raw.totalPoints() < prevPoints) {
                rank++;
                prevPoints = raw.totalPoints();
            }
            if (raw.studentId().equals(studentId)) {
                return new LeaderboardEntryResponse(
                        rank, raw.studentId(), raw.studentName(), raw.totalPoints(),
                        raw.levelTitle(), raw.badgeCount());
            }
        }
        return null;
    }

    // ─── Internal Helpers ───────────────────────────────────────────────────────

    private LevelResponse computeLevel(int totalPoints) {
        GamificationLevel level = levelRepository.findFirstByMinPointsLessThanEqualOrderByMinPointsDesc(totalPoints)
                .orElse(GamificationLevel.builder()
                        .levelNumber(1).title("Beginner").minPoints(0).maxPoints(99)
                        .icon("seedling").color("#78B7D0").build());

        int progress;
        if (level.getMaxPoints() == null) {
            progress = 100; // At max level
        } else {
            int range = level.getMaxPoints() - level.getMinPoints() + 1;
            int gained = totalPoints - level.getMinPoints();
            progress = Math.min(100, (int) Math.round(100.0 * gained / range));
        }

        return new LevelResponse(
                level.getId(), level.getLevelNumber(), level.getTitle(),
                level.getMinPoints(), level.getMaxPoints(),
                level.getIcon(), level.getColor(), progress);
    }

    private Map<String, Long> buildCriteriaCountMap(UUID studentId) {
        Map<String, Long> counts = new HashMap<>();

        // Count completed lessons from points ledger
        counts.put("LESSONS_COMPLETED",
                pointsLedgerRepository.countByStudentIdAndEventType(studentId, GamificationEventType.LESSON_COMPLETION.name()));
        counts.put("COURSES_COMPLETED",
                pointsLedgerRepository.countByStudentIdAndEventType(studentId, GamificationEventType.COURSE_COMPLETION.name()));
        counts.put("ASSESSMENTS_PASSED",
                pointsLedgerRepository.countByStudentIdAndEventType(studentId, GamificationEventType.ASSESSMENT_PASS.name()));
        counts.put("HIGH_SCORE",
                pointsLedgerRepository.countByStudentIdAndEventType(studentId, GamificationEventType.HIGH_SCORE.name()));

        // Streak days
        LearningStreak streak = learningStreakRepository.findByStudentId(studentId).orElse(null);
        counts.put("STREAK_DAYS", streak != null ? (long) streak.getLongestStreak() : 0L);

        return counts;
    }

    private record LeaderboardRaw(UUID studentId, String studentName, int totalPoints,
                                  String levelTitle, long badgeCount) {}

    private List<LeaderboardRaw> buildLeaderboardRaw(Instant since, UUID batchId) {
        // A batch leaderboard must contain only learners enrolled in that batch.
        // For the unfiltered leaderboard the tenant-isolated user list remains the
        // source of truth, preserving support for learners who are not in a batch.
        List<User> users;
        if (batchId == null) {
            users = userRepository.findAll();
        } else {
            List<UUID> studentIds = studentBatchRepository.findStudentUserIdsByBatchId(batchId);
            users = studentIds.isEmpty() ? List.of() : userRepository.findAllById(studentIds);
        }

        List<LeaderboardRaw> entries = new ArrayList<>();
        for (User user : users) {
            int totalPoints;
            if (since != null) {
                totalPoints = pointsLedgerRepository.sumPointsByStudentIdSince(user.getId(), since);
            } else {
                totalPoints = pointsLedgerRepository.sumPointsByStudentId(user.getId());
            }

            if (totalPoints <= 0) continue;

            LevelResponse level = computeLevel(totalPoints);
            long badges = studentBadgeRepository.countByStudentId(user.getId());

            entries.add(new LeaderboardRaw(user.getId(), user.getName(), totalPoints, level.title(), badges));
        }
        return entries;
    }

    private Instant periodToSince(String period) {
        if (period == null) return null;
        return switch (period.toLowerCase()) {
            case "weekly" -> Instant.now().minus(7, ChronoUnit.DAYS);
            case "monthly" -> Instant.now().minus(30, ChronoUnit.DAYS);
            default -> null; // "all" or unrecognized
        };
    }
}
