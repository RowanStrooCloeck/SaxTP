import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.*;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;

public class Sender extends Thread {

    private InetAddress ipAddress;
    private DatagramSocket socket;
    private String filename;

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
            socket = new DatagramSocket();
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

    private void receiveData() throws IOException {
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
        int packetType = response[5];
        String headerString = new String(header);
        // if they are correct, continue
        if (headerString.equals("SaxTP") && packetType == -128) {
            // make acknowledge message
            byte[] ack = new byte[14];
            System.arraycopy(header, 0, ack, 0, header.length);
            ack[5] = 1;
            System.arraycopy(response, 6, ack, 6, 4);
            System.arraycopy(response, 10, ack, 10, 4);
            // make ack
            DatagramPacket sendAck = new DatagramPacket(ack, ack.length, ipAddress, 29588);
            // get the data, without headers
            byte[] responseData = new byte[response.length - 14];
            System.arraycopy(response, 14, responseData, 0, response.length - 14);
            // if the length is smaller than 500, full package is received
            // else, get the remaining packages
            // send ack
            socket.send(sendAck);
            if (responseData.length > 500) {
                FileOutputStream out = new FileOutputStream(filename);
                out.write(responseData, 0, responseData.length);
                out.close();
            } else {
                receiveData();
            }
        } else {
            System.out.println("Invalid response received");
        }

        System.out.println("FROM SERVER: " + new String(response));
        socket.close();
    }
}
