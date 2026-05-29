package com.onlinebanking.util;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

@Converter
public class AttributeEncryptor implements AttributeConverter<String, String> {

    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int IV_SIZE = 12;
    private static final int TAG_SIZE = 128;
    private static SecretKeySpec SECRET_KEY_SPEC;

    public static void setSecretKey(String key) {
        initSecretKey(key);
    }

    private static void initSecretKey(String key) {
        if (SECRET_KEY_SPEC != null) {
            return;
        }
        if (key == null || key.isBlank()) {
            key = System.getenv("AES_SECRET");
        }
        if (key == null || key.isBlank()) {
            throw new IllegalStateException("AES_SECRET must be provided for encryption");
        }
        byte[] rawKey = key.getBytes(StandardCharsets.UTF_8);
        if (rawKey.length != 16 && rawKey.length != 24 && rawKey.length != 32) {
            throw new IllegalStateException("AES_SECRET must be 16, 24, or 32 characters long");
        }
        SECRET_KEY_SPEC = new SecretKeySpec(rawKey, "AES");
    }

    @Override
    public String convertToDatabaseColumn(String attribute) {
        if (attribute == null) {
            return null;
        }
        initSecretKey(null);
        try {
            byte[] iv = new byte[IV_SIZE];
            SecureRandom random = new SecureRandom();
            random.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, SECRET_KEY_SPEC, new GCMParameterSpec(TAG_SIZE, iv));
            byte[] encrypted = cipher.doFinal(attribute.getBytes(StandardCharsets.UTF_8));

            ByteBuffer buffer = ByteBuffer.allocate(iv.length + encrypted.length);
            buffer.put(iv);
            buffer.put(encrypted);
            return Base64.getEncoder().encodeToString(buffer.array());
        } catch (Exception e) {
            throw new IllegalStateException("Unable to encrypt attribute", e);
        }
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }
        initSecretKey(null);
        try {
            byte[] decoded = Base64.getDecoder().decode(dbData);
            ByteBuffer buffer = ByteBuffer.wrap(decoded);
            byte[] iv = new byte[IV_SIZE];
            buffer.get(iv);
            byte[] encrypted = new byte[buffer.remaining()];
            buffer.get(encrypted);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, SECRET_KEY_SPEC, new GCMParameterSpec(TAG_SIZE, iv));
            return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("Unable to decrypt attribute", e);
        }
    }
}
