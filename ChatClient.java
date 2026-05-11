import java.io.*;
import java.net.*;
import java.security.PublicKey;
import java.util.Scanner;

public class ChatClient {
    public static void main(String[] args) throws Exception {
        System.out.println("Connecting to server...");
        Socket socket = new Socket("localhost", 8080);
        
        ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
        out.flush(); // Prevent stream header deadlocks
        ObjectInputStream in = new ObjectInputStream(socket.getInputStream());

        CryptoManager crypto = new CryptoManager();
        crypto.generateRSAKVPair();

        // 1. Receive our assigned role from the server
        MessagePacket rolePacket = (MessagePacket) in.readObject();
        boolean isInitiator = (boolean) rolePacket.payload;
        System.out.println("Connected. My role as Initiator is: " + isInitiator);

        // 2. Exchange RSA Public Keys
        out.writeObject(new MessagePacket(MessagePacket.Type.PUBLIC_KEY, crypto.getMyPublicKey()));
        MessagePacket pkPacket = (MessagePacket) in.readObject();
        PublicKey partnerKey = (PublicKey) pkPacket.payload;
        System.out.println("Received partner's RSA Public Key.");

        // 3. Establish the AES Session Key
        if (isInitiator) {
            crypto.generateAESKey();
            byte[] encryptedSessionKey = crypto.encryptAESKeyForTransfer(partnerKey);
            out.writeObject(new MessagePacket(MessagePacket.Type.ENCRYPTED_AES_KEY, encryptedSessionKey));
            System.out.println("Generated and sent encrypted AES session key.");
        } else {
            System.out.println("Waiting for AES session key from partner...");
            MessagePacket aesPacket = (MessagePacket) in.readObject();
            byte[] encryptedSessionKey = (byte[]) aesPacket.payload;
            crypto.receiveAndDecryptAESKey(encryptedSessionKey);
            System.out.println("Received and decrypted AES session key.");
        }

        System.out.println("\n=== SECURE E2EE SESSION ESTABLISHED ===");
        System.out.println("Start typing your messages below.\n");

        // 4. Background Thread: Listen for incoming encrypted messages
        new Thread(() -> {
            try {
                while (true) {
                    MessagePacket msgPacket = (MessagePacket) in.readObject();
                    if (msgPacket.type == MessagePacket.Type.CHAT_MESSAGE) {
                        String decrypted = crypto.decryptMessage((String) msgPacket.payload);
                        // Erase the "You: " prompt, print message, reset prompt
                        System.out.print("\rPartner: " + decrypted + "\nYou: ");
                    }
                }
            } catch (Exception e) {
                System.out.println("\nConnection closed by server/partner.");
                System.exit(0);
            }
        }).start();

        // 5. Main Thread: Capture keyboard input and send encrypted messages
        Scanner scanner = new Scanner(System.in);
        while (true) {
            System.out.print("You: ");
            String text = scanner.nextLine();
            String cipherText = crypto.encryptMessage(text);
            out.writeObject(new MessagePacket(MessagePacket.Type.CHAT_MESSAGE, cipherText));
        }
    }
}