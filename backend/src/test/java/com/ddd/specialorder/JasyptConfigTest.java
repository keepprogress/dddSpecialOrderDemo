package com.ddd.specialorder;

import org.jasypt.encryption.pbe.StandardPBEStringEncryptor;
import org.jasypt.iv.NoIvGenerator;
import org.junit.jupiter.api.Test;

public class JasyptConfigTest {

    @Test
    public void generateEncryptedPassword() {
        // ==========================================
        // 1. 設定您的加密金鑰 (Master Password)
        //    這就是將來啟動時要輸入的 --jasypt.encryptor.password
        String masterKey = "my_secret_key"; 

        // 2. 設定您要加密的真實資料 (例如 Oracle 密碼)
        String realPassword = "somdba"; 
        // ==========================================

        StandardPBEStringEncryptor encryptor = new StandardPBEStringEncryptor();
        encryptor.setPassword(masterKey);
        
        // 必須與 application.properties 設定一致
        encryptor.setAlgorithm("PBEWithMD5AndDES");
        encryptor.setIvGenerator(new NoIvGenerator());

        String encrypted = encryptor.encrypt(realPassword);

        System.out.println("--------------------------------------------------");
        System.out.println("原始密碼: " + realPassword);
        System.out.println("Master Key: " + masterKey);
        System.out.println("加密結果 (請複製下面這串):");
        System.out.println("ENC(" + encrypted + ")");
        System.out.println("--------------------------------------------------");
    }
}
