package com.tgfc.som.util;

import org.jasypt.encryption.pbe.StandardPBEStringEncryptor;

/**
 * Jasypt 加解密工具
 * 
 * 使用方式:
 * 1. 修改 SECRET_KEY 為你的加密金鑰
 * 2. 執行 main 方法，將要加密的字串傳入 encrypt()
 * 3. 將輸出的 ENC(...) 貼到 application.properties
 */
public class JasyptUtil {

    // 加密金鑰 (與 jasypt.encryptor.password 相同)
    private static final String SECRET_KEY = "GEMINI";
    
    // 加密演算法 (與 application.properties 中的設定相同)
    private static final String ALGORITHM = "PBEWithMD5AndDES";

    public static void main(String[] args) {
        // ========== 加密範例 ==========
        String plainText = "somdba";
        String encrypted = encrypt(plainText);
        System.out.println("原文: " + plainText);
        System.out.println("加密後: ENC(" + encrypted + ")");
        
        // ========== 解密範例 ==========
        String decrypted = decrypt(encrypted);
        System.out.println("解密後: " + decrypted);
    }

    /**
     * 加密字串
     */
    public static String encrypt(String plainText) {
        StandardPBEStringEncryptor encryptor = createEncryptor();
        return encryptor.encrypt(plainText);
    }

    /**
     * 解密字串
     */
    public static String decrypt(String encryptedText) {
        StandardPBEStringEncryptor encryptor = createEncryptor();
        return encryptor.decrypt(encryptedText);
    }

    private static StandardPBEStringEncryptor createEncryptor() {
        StandardPBEStringEncryptor encryptor = new StandardPBEStringEncryptor();
        encryptor.setPassword(SECRET_KEY);
        encryptor.setAlgorithm(ALGORITHM);
        return encryptor;
    }
}
