import java.io.FileOutputStream;
import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Semaphore;

public class Receiver implements Runnable {

    private InetAddress ipAddress;
    private DatagramSocket socket;
    private Map<Integer, byte[]> messages = new HashMap<>();
    private Semaphore semRequestComplete;
    private Semaphore semReceiveMessage;
    private String filename;

    /**
     * Constructor of Receiver
     * Socket needed for receiving/sending messages
     * Filename to know the filename to write to
     * 2 Semaphores to ensure that every package will be received
     */
    public Receiver(DatagramSocket socket, String filename, Semaphore semRequestComplete, Semaphore semReceiveMessage) throws IOException, InterruptedException {
        this.ipAddress = InetAddress.getByName("vanviegen.net");
        this.socket = socket;
        this.semRequestComplete = semRequestComplete;
        this.semReceiveMessage = semReceiveMessage;
        this.filename = filename;
    }

    /**
     * Main run method, overrides Thread run method
     */
    @Override
    public void run() {
        System.out.println("Receiver live.");
        try {
            receiveDataFromServer();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * Get the package or packages from the requested file
     *
     * @throws IOException Socket and FileOutputStream throw IOExceptions
     */
    public void receiveDataFromServer() throws IOException, InterruptedException {
        // get data
        byte[] receivedData = new byte[1024];
        DatagramPacket responseDataPacket = new DatagramPacket(receivedData, receivedData.length);
        semReceiveMessage.acquire();
        socket.receive(responseDataPacket);
        semReceiveMessage.release();

        // send acknowledgement
        DatagramPacket acknowledgement = createAck(receivedData);
        socket.send(acknowledgement);

        // get information
        int packetLength = responseDataPacket.getLength();
        byte[] response = new byte[packetLength];
        System.arraycopy(receivedData, 0, response, 0, response.length);
        byte[] sequence = new byte[4];
        System.arraycopy(response, 10, sequence, 0, 4);
        Integer sequenceID = getUnsignedInt(sequence);

        // get data without headers
        byte[] responseData = new byte[response.length - 14];
        System.arraycopy(response, 14, responseData, 0, response.length - 14);

        // save sequenceID with data
        messages.put(sequenceID, responseData);

        // boolean for the while loop
        boolean receivedAllPackages = false;
        if (responseData.length < 500) {
            // this means that the whole file is only 1 package
            receivedAllPackages = true;
            // release so the sender will know all packages have been received
            semRequestComplete.release();
        }
        // while receivedData >= 500 bytes
        while (!receivedAllPackages) {
            // get other packages
            // get data
            responseDataPacket = new DatagramPacket(receivedData, receivedData.length);
            semReceiveMessage.acquire();
            socket.receive(responseDataPacket);
            semReceiveMessage.release();

            // get information
            packetLength = responseDataPacket.getLength();
            response = new byte[packetLength];
            System.arraycopy(receivedData, 0, response, 0, response.length);
            // get sequenceID
            sequence = new byte[4];
            System.arraycopy(response, 10, sequence, 0, 4);
            sequenceID = getUnsignedInt(sequence);
            // get data without headers
            responseData = new byte[response.length - 14];
            System.arraycopy(response, 14, responseData, 0, response.length - 14);

            // check size, if 500 bytes. send ack and save
            if (responseData.length >= 500) {
                // send acknowledgement
                acknowledgement = createAck(receivedData);
                socket.send(acknowledgement);

                // save sequenceID with data
                messages.put(sequenceID, responseData);
            } else {
                // if size < 500 bytes, check if you have all packages
                if (sequenceID <= messages.size()) {
                    // only if you do, save and send ack.
                    // send acknowledgement
                    acknowledgement = createAck(receivedData);
                    socket.send(acknowledgement);

                    // save sequenceID with data
                    messages.put(sequenceID, responseData);

                    // exit while loop
                    receivedAllPackages = true;
                    // release so the sender will know all packages have been received
                    semRequestComplete.release();
                }
            }


        }
        // reconstruct all packages
        FileOutputStream out = new FileOutputStream(filename);
        for (int i = 0; i < messages.size(); i++) {
            out.write(messages.get(i), 0, messages.get(i).length);
        }
        out.close();
    }


    /**
     * Method to get an Integer out of the sequenceID
     * source: https://stackoverflow.com/questions/7619058/convert-a-byte-array-to-integer-in-java-and-vice-versa
     *
     * @param data the byte[] to be converted to Integer
     * @return an Integer based on the byte[]
     */
    private static Integer getUnsignedInt(byte[] data) {
        ByteBuffer buffer = ByteBuffer.wrap(data);
        return buffer.getInt();
    }

    /**
     * Method to make an acknowledgement for a received message
     *
     * @param message Message you received you want to make an acknowledgement for
     * @return returns a DatagramPacket you can send through the socket to send the acknowledgement
     */
    private DatagramPacket createAck(byte[] message) {
        byte[] ack = new byte[14];
        System.arraycopy(message, 0, ack, 0, 14);
        ack[5] = (byte) 1; // packetType
        return new DatagramPacket(ack, ack.length, ipAddress, 29588);
    }
}
