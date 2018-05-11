import java.io.IOException;
import java.net.DatagramSocket;
import java.util.Scanner;
import java.util.concurrent.Semaphore;

public class Main {

    public static void main(String[] args) {
        new Main().run();
    }

    public void run() {
        try {
            // get user input to request given file
            System.out.println("Please write the name of the file you want to request:");
            Scanner scanner = new Scanner(System.in);
            String filename = scanner.nextLine();

            // make socket and semaphore to initialize sender/receiver
            DatagramSocket socket = new DatagramSocket(29588);
            Semaphore semRequestComplete = new Semaphore(0);
            Semaphore semReceiveMessage = new Semaphore(1, true);

            // create sender and receiver and start them
            Thread sender = new Thread(new Sender(socket, filename, semRequestComplete, semReceiveMessage));
            Thread receiver = new Thread(new Receiver(socket, filename, semRequestComplete, semReceiveMessage));
            sender.start();
            receiver.start();


        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }
}
