package com.jeff.cripto.utils;

import com.jeff.cripto.config.ConfigLoader;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

public class Signature {


    public static String generateSignature(String data, String secretKey) throws Exception {
        // Configurar o HMAC-SHA256
        Mac hmacSHA256 = Mac.getInstance("HmacSHA256");
        SecretKeySpec secretKeySpec = new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        hmacSHA256.init(secretKeySpec);

        // Gerar a assinatura
        byte[] hmacBytes = hmacSHA256.doFinal(data.getBytes(StandardCharsets.UTF_8));

        // Converter os bytes para uma string hexadecimal
        StringBuilder hexString = new StringBuilder();
        for (byte b : hmacBytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }
}
