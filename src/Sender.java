import java.io.FileOutputStream;
import java.io.IOException;
import java.net.*;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

public class Sender extends Thread {

    private InetAddress ipAddress;
    private DatagramSocket socket;
    private String filename;
    private Map<Integer, byte[]> messages = new HashMap<>();

    /**
     * Empty constructor
     */
    public Sender() {
    }

    /**
     * Main run method, overrides Thread run method
     */
    @Override
    public void run() {
        System.out.println("Sender live.");
        try {
            socket = new DatagramSocket(29588);
            ipAddress = InetAddress.getByName("vanviegen.net");
        } catch (SocketException | UnknownHostException e) {
            e.printStackTrace();
        }
    }

    public void sendRequest(String filename) throws IOException {
        this.filename = filename;
        System.out.println("Requesting file: " + filename);
        try {
            // send message
            RequestMessage requestMessage = new RequestMessage(0, filename);
            byte[] sendData = requestMessage.getRequestMessage();
            DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, ipAddress, 29588);
            socket.send(sendPacket);
            receiveData();

        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }

    /**
     *
     * @throws IOException
     */
    private void receiveData() throws IOException {
        // get data
        byte[] receivedData = new byte[1024];
        DatagramPacket responseDataPacket = new DatagramPacket(receivedData, receivedData.length);
        socket.receive(responseDataPacket);

        // get information
        int packetLength = responseDataPacket.getLength();
        byte[] response = new byte[packetLength];
        System.arraycopy(receivedData, 0, response, 0, response.length);

        // send acknowledgement
        byte[] ack = new byte[14];
        System.arraycopy(response, 0, ack, 0, 5);
        ack[5] = (byte) 1;
        System.arraycopy(response, 6, ack, 6, 4);
        System.arraycopy(response, 10, ack, 10, 4);
        DatagramPacket sendAck = new DatagramPacket(ack, ack.length, ipAddress, 29588);
        socket.send(sendAck);

        // get data without headers
        byte[] responseData = new byte[response.length - 14];
        System.arraycopy(response, 14, responseData, 0, response.length - 14);

        // save sequenceID with data
        byte[] sequence = new byte[4];
        System.arraycopy(response, 10, sequence, 0, 4);
        Integer sequenceID = convertByteToInt(sequence);
        messages.put(sequenceID, responseData);

        // while receivedData >= 500 bytes
        while (responseData.length >= 500) {
            // get other packages
            // get data
            responseDataPacket = new DatagramPacket(receivedData, receivedData.length);
            socket.receive(responseDataPacket);

            // get information
            packetLength = responseDataPacket.getLength();
            response = new byte[packetLength];
            System.arraycopy(receivedData, 0, response, 0, response.length);

            // send acknowledgement
            ack = new byte[14];
            System.arraycopy(response, 0, ack, 0, 5);
            ack[5] = (byte) 1;
            System.arraycopy(response, 6, ack, 6, 4);
            System.arraycopy(response, 10, ack, 10, 4);
            sendAck = new DatagramPacket(ack, ack.length, ipAddress, 29588);
            socket.send(sendAck);

            // get data without headers
            responseData = new byte[response.length - 14];
            System.arraycopy(response, 14, responseData, 0, response.length - 14);

            // save sequenceID with data
            sequence = new byte[4];
            System.arraycopy(response, 10, sequence, 0, 4);
            sequenceID = convertByteToInt(sequence);
            messages.put(sequenceID, responseData);
        }
        // reconstruct all packages
        int startPos = 0;
        int messageSize = messages.size();
        byte[] fullPackage = new byte[500 * messageSize];
        for (int i = 0; i < messageSize; i++) {
            byte[] savedMessage = messages.get(i);
            System.arraycopy(savedMessage, 0, fullPackage, startPos, savedMessage.length);
            startPos += savedMessage.length;
        }

        // output all packages in 1 file.
        FileOutputStream out = new FileOutputStream(filename);
        out.write(fullPackage, 0, fullPackage.length);
        out.close();
    }

    /**
     * Method to get an integer out of the sequenceID which is 4 bytes
     * source: https://stackoverflow.com/questions/4950598/convert-byte-to-int
     *
     * @param b the byte[] to convert to Integer
     * @return return the byte[] as Integer
     */
    private Integer convertByteToInt(byte[] b) {
        int value = 0;
        for (byte aB : b) value = (value << 8) | aB;
        return value;
    }

    /*private void receiveData() throws IOException {
        // receive answer from server
        byte[] receiveData = new byte[1024];
        DatagramPacket responseDataPacket = new DatagramPacket(receiveData, receiveData.length);
        socket.receive(responseDataPacket);
        // get packet length
        int packetLength = responseDataPacket.getLength();
        // make new byte[] with only the useful bytes
        byte[] response = new byte[packetLength];
        System.arraycopy(receiveData, 0, response, 0, response.length);
        // get header and packetType to check if they are correct.
        byte[] header = new byte[5];
        System.arraycopy(response, 0, header, 0, header.length);
        int packetType = Byte.toUnsignedInt(response[5]);
        String headerString = new String(header);
        // if they are correct, continue
        if (headerString.equals("SaxTP") && packetType == 128) {
            // make acknowledge message
            byte[] ack = new byte[14];
            System.arraycopy(header, 0, ack, 0, header.length);
            int unsignedPacketType = 1 & 0xFF;
            ack[5] = (byte) unsignedPacketType;
            System.arraycopy(response, 6, ack, 6, 4);
            System.arraycopy(response, 10, ack, 10, 4);
            // make ack
            DatagramPacket sendAck = new DatagramPacket(ack, ack.length, ipAddress, 29588);
            // get the data, without headers
            byte[] responseData = new byte[response.length - 14];
            System.arraycopy(response, 14, responseData, 0, response.length - 14);
            // send ack
            socket.send(sendAck);
            // save sequence with data
            byte[] sequence = new byte[4];
            System.arraycopy(response, 10, sequence, 0, 4);
            Integer sequenceID = convertByteToInt(sequence);
            messages.put(sequenceID, responseData);
            // if the length is smaller than 500, full package is received
            // else, get the remaining packages
            if (responseData.length < 500) {
                // add all parts together and download
                int startPos = 0;
                int messageSize = messages.size();
                byte[] fullPackage = new byte[500 * messageSize];
                for (int i = 0; i < messageSize; i++) {
                    byte[] savedMessage = messages.get(i);
                    System.arraycopy(savedMessage, 0, fullPackage, startPos, savedMessage.length);
                    startPos += savedMessage.length;
                }
                FileOutputStream out = new FileOutputStream(filename);
                out.write(fullPackage, 0, fullPackage.length);
                out.close();
            } else {
                // while to receive all packages from the whole file
                receiveData();
            }
        } else {
            System.out.println("Invalid response received");
        }

        System.out.println("FROM SERVER: " + new String(response));
        socket.close();
    }*/
}
