package com.lms.common.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Who to call about this learner.
 *
 * <p>One contact, embedded — this replaces the father/mother/guardian set a
 * schools model would carry. Adult learners have a contact, not a guardian.
 */
@Embeddable
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmergencyContact {

    @Column(name = "emergency_contact_name", length = 150)
    private String name;

    /** Free text: spouse, parent, sibling, friend, colleague. */
    @Column(name = "emergency_contact_relation", length = 60)
    private String relation;

    @Column(name = "emergency_contact_phone", length = 20)
    private String phone;

    @Column(name = "emergency_contact_email", length = 255)
    private String email;
}
