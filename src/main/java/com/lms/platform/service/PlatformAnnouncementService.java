package com.lms.platform.service;

import com.lms.common.exception.ResourceNotFoundException;
import com.lms.platform.dto.BroadcastAnnouncementDto;
import com.lms.platform.dto.CreateAnnouncementRequest;
import com.lms.platform.entity.BroadcastAnnouncement;
import com.lms.platform.repository.BroadcastAnnouncementRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PlatformAnnouncementService {

    private final BroadcastAnnouncementRepository repository;

    public List<BroadcastAnnouncementDto> listAll() {
        return repository.findAllByOrderByCreatedAtDesc().stream()
                .map(BroadcastAnnouncementDto::from)
                .toList();
    }

    public List<BroadcastAnnouncementDto> listActive() {
        return repository.findActiveAnnouncements(Instant.now()).stream()
                .map(BroadcastAnnouncementDto::from)
                .toList();
    }

    @Transactional
    public BroadcastAnnouncementDto create(CreateAnnouncementRequest request, UUID adminId) {
        BroadcastAnnouncement announcement = BroadcastAnnouncement.builder()
                .title(request.title().trim())
                .message(request.message().trim())
                .type(request.type() != null ? request.type().toUpperCase() : "INFO")
                .active(request.active() == null || request.active())
                .startsAt(request.startsAt() != null ? request.startsAt() : Instant.now())
                .expiresAt(request.expiresAt())
                .createdBy(adminId)
                .build();

        BroadcastAnnouncement saved = repository.save(announcement);
        log.info("Platform admin {} published broadcast announcement: {}", adminId, saved.getTitle());
        return BroadcastAnnouncementDto.from(saved);
    }

    @Transactional
    public void delete(UUID id) {
        if (!repository.existsById(id)) {
            throw ResourceNotFoundException.of("BroadcastAnnouncement", id);
        }
        repository.deleteById(id);
        log.info("Platform announcement {} deleted", id);
    }

    @Transactional
    public BroadcastAnnouncementDto toggleActive(UUID id, boolean active) {
        BroadcastAnnouncement announcement = repository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("BroadcastAnnouncement", id));
        announcement.setActive(active);
        BroadcastAnnouncement saved = repository.save(announcement);
        return BroadcastAnnouncementDto.from(saved);
    }
}
