import java.io.IOException;
import java.util.Scanner;

public class Main {


    public static void main(String[] args) {
        new Main().run();
    }

    public void run() {
        // start sender and receiver
        Sender sender = new Sender();
        sender.start();

        // get user input to request given file
        Scanner scanner = new Scanner(System.in);
        String input = scanner.nextLine();
        try {
            sender.sendRequest(input);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
