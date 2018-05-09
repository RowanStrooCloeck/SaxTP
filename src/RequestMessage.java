import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;

public class RequestMessage {
    private final byte[] header = "SaxTP".getBytes();
    private byte packetType;
    private byte[] transferID;
    private byte[] filename;

    public RequestMessage(int packetType, String filename) throws NoSuchAlgorithmException {
        this.packetType = (byte) packetType;
        this.filename = filename.getBytes();

        // Generate 4 random bytes for the transferID
        // source: https://stackoverflow.com/questions/5683206/how-to-create-an-array-of-20-random-bytes
        this.transferID = new byte[4];
        SecureRandom.getInstanceStrong().nextBytes(transferID);
    }

    public byte[] getRequestMessage() {
        int requestLength = this.header.length + 1 + this.transferID.length + this.getFilename().getBytes().length;
        byte[] requestMessage = new byte[requestLength];
        // add all the byte[] to make 1 byte[]
        // source https://stackoverflow.com/questions/5513152/easy-way-to-concatenate-two-byte-arrays
        byte[] filenameArray = this.getFilename().getBytes();
        System.arraycopy(this.header, 0, requestMessage, 0, this.header.length);
        int length = this.header.length;
        requestMessage[length] = packetType;
        length += 1;
        System.arraycopy(this.transferID, 0, requestMessage, length, transferID.length);
        length += transferID.length;
        System.arraycopy(filenameArray, 0, requestMessage, length, filenameArray.length);
        return requestMessage;
    }

    private String getFilename() {
        return new String(filename);
    }
}
