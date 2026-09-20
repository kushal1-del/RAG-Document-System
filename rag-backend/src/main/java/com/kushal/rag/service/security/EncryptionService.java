package com.kushal.rag.service.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Encrypts and decrypts sensitive values such as user API keys.
 *
 * AES-GCM provides both encryption and integrity protection.
 *
 * The encryption secret comes from configuration/environment and is never
 * stored alongside the encrypted API key.
 */
@Service
public class EncryptionService {

	private static final String ALGORITHM = "AES/GCM/NoPadding";
	private static final int GCM_TAG_LENGTH = 128;
	private static final int IV_LENGTH = 12;

	private final SecretKeySpec secretKey;
	private final SecureRandom secureRandom;

	public EncryptionService(@Value("${app.encryption.secret}") String encryptionSecret) {

		if (encryptionSecret == null || encryptionSecret.isBlank()) {
			throw new IllegalStateException("APP_ENCRYPTION_SECRET is not configured.");
		}

		this.secretKey = createSecretKey(encryptionSecret);
		this.secureRandom = new SecureRandom();
	}

	/**
	 * Encrypts the supplied plaintext.
	 *
	 * The random IV is stored together with the ciphertext because the IV is not
	 * secret. A new IV is generated for every encryption.
	 */
	public String encrypt(String plaintext) {

		if (plaintext == null || plaintext.isBlank()) {
			throw new IllegalArgumentException("Value to encrypt cannot be blank.");
		}

		try {
			byte[] iv = new byte[IV_LENGTH];
			secureRandom.nextBytes(iv);

			Cipher cipher = Cipher.getInstance(ALGORITHM);

			GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);

			cipher.init(Cipher.ENCRYPT_MODE, secretKey, gcmSpec);

			byte[] encryptedBytes = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

			String encodedIv = Base64.getEncoder().encodeToString(iv);

			String encodedCiphertext = Base64.getEncoder().encodeToString(encryptedBytes);

			return encodedIv + ":" + encodedCiphertext;

		} catch (Exception e) {
			throw new RuntimeException("Failed to encrypt sensitive value.", e);
		}
	}

	/**
	 * Decrypts a value previously produced by encrypt().
	 */
	public String decrypt(String encryptedValue) {

		if (encryptedValue == null || encryptedValue.isBlank()) {
			throw new IllegalArgumentException("Encrypted value cannot be blank.");
		}

		try {
			String[] parts = encryptedValue.split(":", 2);

			if (parts.length != 2) {
				throw new IllegalArgumentException("Invalid encrypted value format.");
			}

			byte[] iv = Base64.getDecoder().decode(parts[0]);
			byte[] encryptedBytes = Base64.getDecoder().decode(parts[1]);

			Cipher cipher = Cipher.getInstance(ALGORITHM);

			GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);

			cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmSpec);

			byte[] decryptedBytes = cipher.doFinal(encryptedBytes);

			return new String(decryptedBytes, StandardCharsets.UTF_8);

		} catch (Exception e) {
			throw new RuntimeException("Failed to decrypt sensitive value.", e);
		}
	}

	/**
	 * Converts the configured encryption secret into a fixed 256-bit AES key using
	 * SHA-256.
	 */
	private SecretKeySpec createSecretKey(String encryptionSecret) {

		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");

			byte[] keyBytes = digest.digest(encryptionSecret.getBytes(StandardCharsets.UTF_8));

			return new SecretKeySpec(keyBytes, "AES");

		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException("SHA-256 algorithm is not available.", e);
		}
	}
}