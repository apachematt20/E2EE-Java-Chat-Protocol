import java.io.Serializable;

public class MessagePacket implements Serializable {
    private static final long serialVersionUID = 1L;

    public enum Type {
        INIT_ROLE,          // Server assigns role (Initiator or Receiver)
        PUBLIC_KEY,         // Exchanging RSA Public Keys
        ENCRYPTED_AES_KEY,  // Sending the AES session key
        CHAT_MESSAGE        // A standard encrypted chat message
    }

    public Type type;
    public Object payload;

    public MessagePacket(Type type, Object payload) {
        this.type = type;
        this.payload = payload;
    }
}