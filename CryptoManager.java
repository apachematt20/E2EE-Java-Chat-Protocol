import java.security.*;
import javax.crypto.*;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

public class CryptoManager {
    private PrivateKey rsaPrivateKey;
    private PublicKey rsaPublicKey;
    private SecretKey aesSessionKey;

    // 1. Generate RSA Keys
    public void generateRSAKVPair() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        KeyPair pair = generator.generateKeyPair();
        this.rsaPrivateKey = pair.getPrivate();
        this.rsaPublicKey = pair.getPublic();
    }

    public PublicKey getMyPublicKey() {
        return this.rsaPublicKey;
    }

    // 2. Generate an AES Key
    public void generateAESKey() throws Exception {
        KeyGenerator keyGen = KeyGenerator.getInstance("AES");
        keyGen.init(256); // 256-bit AES
        this.aesSessionKey = keyGen.generateKey();
    }

    // 3. Encrypt AES key using partner's RSA Public Key
    public byte[] encryptAESKeyForTransfer(PublicKey partnerPublicKey) throws Exception {
        Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        cipher.init(Cipher.ENCRYPT_MODE, partnerPublicKey);
        return cipher.doFinal(this.aesSessionKey.getEncoded());
    }

    // 4. Decrypt received AES key using own RSA Private Key
    public void receiveAndDecryptAESKey(byte[] encryptedAesKey) throws Exception {
        Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        cipher.init(Cipher.DECRYPT_MODE, this.rsaPrivateKey);
        byte[] decryptedKeyBytes = cipher.doFinal(encryptedAesKey);
        this.aesSessionKey = new SecretKeySpec(decryptedKeyBytes, 0, decryptedKeyBytes.length, "AES");
    }

    // 5. Encrypt Chat Message (AES)
    public String encryptMessage(String plainText) throws Exception {
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.ENCRYPT_MODE, this.aesSessionKey);
        byte[] encryptedBytes = cipher.doFinal(plainText.getBytes());
        return Base64.getEncoder().encodeToString(encryptedBytes);
    }

    // 6. Decrypt Chat Message (AES)
    public String decryptMessage(String encryptedText) throws Exception {
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.DECRYPT_MODE, this.aesSessionKey);
        byte[] decodedBytes = Base64.getDecoder().decode(encryptedText);
        byte[] decryptedBytes = cipher.doFinal(decodedBytes);
        return new String(decryptedBytes);
    }
}