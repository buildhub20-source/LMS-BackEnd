package com.lms.auth.service;

import com.lms.auth.dto.request.ForgotPasswordRequest;
import com.lms.auth.dto.request.ResetPasswordRequest;

/** Forgotten-password flow: request a reset, then redeem the token. */
public interface PasswordResetService {

    /**
     * Issues a reset token and emails it.
     *
     * <p>Always completes silently, whether or not the address is known, so the
     * endpoint cannot be used to enumerate accounts.
     */
    void requestReset(ForgotPasswordRequest request);

    /**
     * Redeems a reset token, sets the new password and revokes every existing
     * session for that user.
     */
    void resetPassword(ResetPasswordRequest request);
}
