package com.lms.platform.service;

import com.lms.config.PlatformConfig;
import com.lms.common.exception.ApplicationException;
import com.lms.common.exception.ErrorCode;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

/** Encrypts tenant database credentials before they enter the control plane. */
@Service
public class TenantSecretCipher {
    private static final SecureRandom RANDOM = new SecureRandom();
    private final PlatformConfig platformConfig;

    public TenantSecretCipher(PlatformConfig platformConfig) {
        this.platformConfig = platformConfig;
    }

    public String encrypt(String plaintext) {
        try {
            byte[] nonce = new byte[12];
            RANDOM.nextBytes(nonce);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key(), new GCMParameterSpec(128, nonce));
            byte[] encrypted = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            byte[] packed = new byte[nonce.length + encrypted.length];
            System.arraycopy(nonce, 0, packed, 0, nonce.length);
            System.arraycopy(encrypted, 0, packed, nonce.length, encrypted.length);
            return Base64.getEncoder().encodeToString(packed);
        } catch (ApplicationException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ApplicationException(ErrorCode.INTERNAL_ERROR, "Unable to protect tenant credentials", ex);
        }
    }

    public String decrypt(String encryptedValue) {
        try {
            byte[] packed = Base64.getDecoder().decode(encryptedValue);
            if (packed.length <= 12) {
                throw new IllegalArgumentException("Invalid encrypted tenant secret");
            }
            byte[] nonce = java.util.Arrays.copyOfRange(packed, 0, 12);
            byte[] encrypted = java.util.Arrays.copyOfRange(packed, 12, packed.length);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key(), new GCMParameterSpec(128, nonce));
            return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
        } catch (ApplicationException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ApplicationException(ErrorCode.INTERNAL_ERROR, "Unable to read tenant credentials", ex);
        }
    }

    private SecretKey key() {
        String value = platformConfig.getCredentialEncryptionKey();
        if (value == null || value.isBlank()) {
            throw new ApplicationException(ErrorCode.BUSINESS_RULE_VIOLATION,
                    "Tenant provisioning requires TENANT_CREDENTIAL_ENCRYPTION_KEY");
        }
        try {
            byte[] bytes = Base64.getDecoder().decode(value);
            if (bytes.length != 32) {
                throw new IllegalArgumentException("Expected 32 bytes");
            }
            return new SecretKeySpec(bytes, "AES");
        } catch (IllegalArgumentException ex) {
            throw new ApplicationException(ErrorCode.BUSINESS_RULE_VIOLATION,
                    "TENANT_CREDENTIAL_ENCRYPTION_KEY must be a base64-encoded 32-byte value");
        }
    }
}
