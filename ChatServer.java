import java.io.*;
import java.net.*;

public class ChatServer {
    public static void main(String[] args) throws Exception {
        ServerSocket serverSocket = new ServerSocket(8080);
        System.out.println("E2EE Server started on port 8080. Waiting for 2 clients...");

        // Accept Client 1 (Initiator)
        Socket client1 = serverSocket.accept();
        System.out.println("Client 1 connected.");
        ObjectOutputStream out1 = new ObjectOutputStream(client1.getOutputStream());
        out1.flush(); // Always flush immediately when creating ObjectOutputStream
        ObjectInputStream in1 = new ObjectInputStream(client1.getInputStream());
        out1.writeObject(new MessagePacket(MessagePacket.Type.INIT_ROLE, true)); 

        // Accept Client 2 (Receiver)
        Socket client2 = serverSocket.accept();
        System.out.println("Client 2 connected.");
        ObjectOutputStream out2 = new ObjectOutputStream(client2.getOutputStream());
        out2.flush();
        ObjectInputStream in2 = new ObjectInputStream(client2.getInputStream());
        out2.writeObject(new MessagePacket(MessagePacket.Type.INIT_ROLE, false)); 

        // Start dedicated relay threads
        new Thread(new ClientRelay(in1, out2, "Client 1")).start();
        new Thread(new ClientRelay(in2, out1, "Client 2")).start();
    }
}

// Thread to relay messages blindly from one client to the other
class ClientRelay implements Runnable {
    private ObjectInputStream in;
    private ObjectOutputStream out;
    private String name;

    public ClientRelay(ObjectInputStream in, ObjectOutputStream out, String name) {
        this.in = in;
        this.out = out;
        this.name = name;
    }

    public void run() {
        try {
            while (true) {
                MessagePacket packet = (MessagePacket) in.readObject();
                // Prove the server cannot read the messages
                if(packet.type == MessagePacket.Type.CHAT_MESSAGE) {
                    System.out.println("[BLIND RELAY] " + name + " sent cipher: " + packet.payload);
                }
                out.writeObject(packet);
            }
        } catch (Exception e) {
            System.out.println(name + " disconnected.");
        }
    }
}