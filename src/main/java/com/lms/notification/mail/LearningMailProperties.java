package com.lms.notification.mail;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Settings for the learning notification mails.
 *
 * <p>Separate from {@code lms.mail}, which decides whether mail is sent at all:
 * these switches decide which of the learning events are worth a mail. The
 * assessment and toolkit mails fan out to every active student, so each one can
 * be turned off on its own without silencing password resets.
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "lms.learning-mail")
public class LearningMailProperties {

    private boolean enrollmentEnabled = true;

    private boolean assessmentEnabled = true;

    private boolean liveSessionEnabled = true;

    private boolean liveReminderEnabled = true;

    private boolean campusToolkitEnabled = true;

    /** Signature line at the foot of each mail. */
    private String signature = "The GradusMind team";

    /**
     * Zone the times in these mails are rendered in. Sessions are stored as
     * instants, so without this every student would be told a UTC time.
     */
    private String timeZone = "Asia/Kolkata";

    /** How that zone is named to the reader. */
    private String timeZoneLabel = "IST";
}
