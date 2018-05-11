import java.io.IOException;
import java.net.*;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

public class Sender implements Runnable {

    private InetAddress ipAddress;
    private DatagramSocket socket;
    private Semaphore semRequestComplete;
    private Semaphore semReceiveMessage;
    private String filename;

    /**
     * Constructor of Sender
     * Socket needed for sending/receiving messages
     * Filename to know the to be requested filename
     * 2 Semaphores to ensure that a new request will be send when not all packages have arrived
     */
    public Sender(DatagramSocket socket, String filename, Semaphore semRequestComplete, Semaphore semReceiveMessage) throws IOException {
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
        System.out.println("Sender live.");
        try {
            sendInitialRequest();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Main method to start to send a request to the server
     *
     * @throws IOException Socket can throw IOExceptions
     */
    private void sendInitialRequest() throws IOException {
        try {
            System.out.println("Requesting file: " + filename);
            // send message
            RequestMessage requestMessage = new RequestMessage(0, filename);
            byte[] sendData = requestMessage.getRequestMessage();
            DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, ipAddress, 29588);
            socket.send(sendPacket);

            // continue until the full request has been received
            while (!semRequestComplete.tryAcquire()) {
                // if tryacquire returns false after trying to acquire for 2500 miliseconds, resend package
                if (!semReceiveMessage.tryAcquire(2500, TimeUnit.MILLISECONDS)) {
                    socket.send(sendPacket);
                } else {
                    // if the tryaquire has succeeded, release so the receiver can aquire.
                    semReceiveMessage.release();
                }
            }
        } catch (NoSuchAlgorithmException | InterruptedException e) {
            e.printStackTrace();
        }
    }
}
