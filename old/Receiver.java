package old;

/**
 * Created by Matthew on 5/09/2016.
 */
public class Receiver {

    public Receiver(int port, String filePath) {
        STPConnection connection = new STPConnection(port);

        while (true) {
            if (connection.isConnected()) {
                System.out.print("Set up");
                break;
            }
        }
    }

    public static void main(String[] args) {
        new Receiver(Integer.parseInt(args[0]), args[1]);
    }
}
