package com.lms.course.dto.request;

/** Payload for rejecting a course under review. Reason is optional. */
public class RejectCourseRequest {

    private String reason;

    public RejectCourseRequest() {
    }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
