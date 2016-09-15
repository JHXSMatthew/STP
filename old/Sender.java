package old;

import java.io.IOException;
import java.util.Scanner;

/**
 * Created by Matthew on 5/09/2016.
 */
public class Sender {

    private byte[] data;
    private int currentPosition = 0;
    private STPConnection connection;
    public Sender(String address, int port, byte[] data, int mws, int mss, int timeout, double pdrop, long seed) {
        connection = new STPConnection();
        try {
            connection.handShake(address, port);
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(-1);
        }
        while (true) {
            if (connection.isConnected()) {
                System.out.print("Set up");
                break;
            }
        }

    }

    public static void main(String[] args) {
        String ip = args[0];
        int port = Integer.parseInt(args[1]);
        StringBuilder builder = new StringBuilder();
        Scanner scanner = new Scanner(args[2]);
        while (scanner.hasNextLine()) {
            builder.append(scanner.nextLine());
        }
        byte[] data = builder.toString().getBytes();
        int mws = Integer.parseInt(args[3]);
        int mss = Integer.parseInt(args[4]);
        int timeout = Integer.parseInt(args[5]);
        double pdrop = Double.parseDouble(args[6]);
        long seed = Long.parseLong(args[7]);

        Sender sender = new Sender(ip, port, data, mws, mss, timeout, pdrop, seed);
    }


}
