package com.lms.gamification.service;

import com.lms.common.exception.ResourceNotFoundException;
import com.lms.gamification.dto.request.*;
import com.lms.gamification.dto.response.*;
import com.lms.gamification.entity.*;
import com.lms.gamification.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/** Admin CRUD operations for gamification configuration. */
@Service
@RequiredArgsConstructor
public class GamificationAdminService {

    private final BadgeRepository badgeRepository;
    private final GamificationLevelRepository levelRepository;
    private final MilestoneRepository milestoneRepository;
    private final PointRuleRepository pointRuleRepository;

    // ─── Badges ─────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<BadgeResponse> listBadges() {
        return badgeRepository.findAll().stream()
                .map(b -> new BadgeResponse(b.getId(), b.getName(), b.getDescription(),
                        b.getIcon(), b.getCategory(), b.getCriteriaType(), b.getCriteriaValue(),
                        b.isActive(), null))
                .toList();
    }

    @Transactional
    public BadgeResponse createBadge(CreateBadgeRequest request) {
        Badge badge = Badge.builder()
                .name(request.name())
                .description(request.description())
                .icon(request.icon())
                .category(request.category())
                .criteriaType(request.criteriaType())
                .criteriaValue(request.criteriaValue())
                .active(request.active())
                .build();
        badge = badgeRepository.save(badge);
        return toBadgeResponse(badge);
    }

    @Transactional
    public BadgeResponse updateBadge(UUID id, CreateBadgeRequest request) {
        Badge badge = badgeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Badge not found with id: " + id));
        badge.setName(request.name());
        badge.setDescription(request.description());
        badge.setIcon(request.icon());
        badge.setCategory(request.category());
        badge.setCriteriaType(request.criteriaType());
        badge.setCriteriaValue(request.criteriaValue());
        badge.setActive(request.active());
        badge = badgeRepository.save(badge);
        return toBadgeResponse(badge);
    }

    @Transactional
    public void deleteBadge(UUID id) {
        if (!badgeRepository.existsById(id)) {
            throw new ResourceNotFoundException("Badge not found with id: " + id);
        }
        badgeRepository.deleteById(id);
    }

    // ─── Levels ─────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<LevelResponse> listLevels() {
        return levelRepository.findAllByOrderByLevelNumberAsc().stream()
                .map(l -> new LevelResponse(l.getId(), l.getLevelNumber(), l.getTitle(),
                        l.getMinPoints(), l.getMaxPoints(), l.getIcon(), l.getColor(), 0))
                .toList();
    }

    @Transactional
    public LevelResponse createLevel(CreateLevelRequest request) {
        GamificationLevel level = GamificationLevel.builder()
                .levelNumber(request.levelNumber())
                .title(request.title())
                .minPoints(request.minPoints())
                .maxPoints(request.maxPoints())
                .icon(request.icon())
                .color(request.color())
                .build();
        level = levelRepository.save(level);
        return toLevelResponse(level);
    }

    @Transactional
    public LevelResponse updateLevel(UUID id, CreateLevelRequest request) {
        GamificationLevel level = levelRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Level not found with id: " + id));
        level.setLevelNumber(request.levelNumber());
        level.setTitle(request.title());
        level.setMinPoints(request.minPoints());
        level.setMaxPoints(request.maxPoints());
        level.setIcon(request.icon());
        level.setColor(request.color());
        level = levelRepository.save(level);
        return toLevelResponse(level);
    }

    @Transactional
    public void deleteLevel(UUID id) {
        if (!levelRepository.existsById(id)) {
            throw new ResourceNotFoundException("Level not found with id: " + id);
        }
        levelRepository.deleteById(id);
    }

    // ─── Milestones ─────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<MilestoneResponse> listMilestones() {
        return milestoneRepository.findByActiveTrueOrderBySortOrderAsc().stream()
                .map(m -> new MilestoneResponse(m.getId(), m.getKey(), m.getName(),
                        m.getDescription(), m.getIcon(), m.getCriteriaType(), m.getCriteriaValue(),
                        m.getSortOrder(), m.isActive(), false, null))
                .toList();
    }

    @Transactional
    public MilestoneResponse createMilestone(CreateMilestoneRequest request) {
        Milestone milestone = Milestone.builder()
                .key(request.key())
                .name(request.name())
                .description(request.description())
                .icon(request.icon())
                .criteriaType(request.criteriaType())
                .criteriaValue(request.criteriaValue())
                .sortOrder(request.sortOrder() != null ? request.sortOrder() : 0)
                .active(request.active())
                .build();
        milestone = milestoneRepository.save(milestone);
        return toMilestoneResponse(milestone);
    }

    @Transactional
    public MilestoneResponse updateMilestone(UUID id, CreateMilestoneRequest request) {
        Milestone milestone = milestoneRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Milestone not found with id: " + id));
        milestone.setKey(request.key());
        milestone.setName(request.name());
        milestone.setDescription(request.description());
        milestone.setIcon(request.icon());
        milestone.setCriteriaType(request.criteriaType());
        milestone.setCriteriaValue(request.criteriaValue());
        milestone.setSortOrder(request.sortOrder() != null ? request.sortOrder() : 0);
        milestone.setActive(request.active());
        milestone = milestoneRepository.save(milestone);
        return toMilestoneResponse(milestone);
    }

    @Transactional
    public void deleteMilestone(UUID id) {
        if (!milestoneRepository.existsById(id)) {
            throw new ResourceNotFoundException("Milestone not found with id: " + id);
        }
        milestoneRepository.deleteById(id);
    }

    // ─── Point Rules ────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<PointRuleResponse> listPointRules() {
        return pointRuleRepository.findAll().stream()
                .map(pr -> new PointRuleResponse(pr.getId(), pr.getEventType(), pr.getPoints(), pr.isActive()))
                .toList();
    }

    @Transactional
    public PointRuleResponse updatePointRule(UUID id, UpdatePointRuleRequest request) {
        PointRule rule = pointRuleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Point rule not found with id: " + id));
        rule.setPoints(request.points());
        rule.setActive(request.active());
        rule = pointRuleRepository.save(rule);
        return new PointRuleResponse(rule.getId(), rule.getEventType(), rule.getPoints(), rule.isActive());
    }

    // ─── Mappers ────────────────────────────────────────────────────────────────

    private BadgeResponse toBadgeResponse(Badge b) {
        return new BadgeResponse(b.getId(), b.getName(), b.getDescription(), b.getIcon(),
                b.getCategory(), b.getCriteriaType(), b.getCriteriaValue(), b.isActive(), null);
    }

    private LevelResponse toLevelResponse(GamificationLevel l) {
        return new LevelResponse(l.getId(), l.getLevelNumber(), l.getTitle(),
                l.getMinPoints(), l.getMaxPoints(), l.getIcon(), l.getColor(), 0);
    }

    private MilestoneResponse toMilestoneResponse(Milestone m) {
        return new MilestoneResponse(m.getId(), m.getKey(), m.getName(), m.getDescription(),
                m.getIcon(), m.getCriteriaType(), m.getCriteriaValue(), m.getSortOrder(), m.isActive(),
                false, null);
    }
}
