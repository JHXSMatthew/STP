package old;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.StringTokenizer;

/**
 * Created by Matthew on 5/09/2016.
 */
public class STPConnection {

    private static int HANDSHAKE_LENGTH = 1024;
    private DatagramSocket socket;
    private State state = State.STOP;
    private String address;
    private int port;
    private int mss;

    /**
     * @param port listen on that port
     */
    public STPConnection(int port) {
        try {
            socket = new DatagramSocket(port);
        } catch (Exception e) {
            System.err.println("Port Bind Error!");
            e.printStackTrace();
            System.exit(-1);
        }
        init();
    }

    /**
     * sender constructor
     */
    public STPConnection() {
        try {
            socket = new DatagramSocket();
        } catch (Exception e) {
            System.err.println("Port Bind Error!");
            e.printStackTrace();
            System.exit(-1);
        }


    }

    /**
     * listen for handshake packet.
     */
    private void init() {
        System.out.println("Waitting for Handshake packet....");
        state = State.WAITING_HANDSHAKE;
        while (state == State.WAITING_HANDSHAKE) {
            DatagramPacket packet = new DatagramPacket(new byte[HANDSHAKE_LENGTH], HANDSHAKE_LENGTH);
            try {
                socket.receive(packet);
            } catch (IOException e) {
                e.printStackTrace();
                System.out.println("STPPacket Received IO Error!");
                System.exit(-1);
            }
            String message = new String(packet.getData());
            DEBUG.print(message);
            StringTokenizer tokenizer = new StringTokenizer(message, "_");
            String type = tokenizer.nextToken();
            DatagramPacket outgoing;
            String relpy = null;
            switch (type) {
                case "SYN":
                    relpy = "SYN-ACK_receiver";
                    state = State.WAITING_HANDSHAKE;
                    address = packet.getAddress().getHostAddress();
                    port = packet.getPort();
                    DEBUG.print("SYN received from " + address + ":" + port);
                    break;
                case "ACK":
                    if (address.equals(packet.getAddress().getHostAddress()) && packet.getPort() == this.port) {
                        state = State.SET_UP;
                        DEBUG.print("ACK received from " + address + ":" + port);
                    }

                    continue;
                case "SYN-ACK":
                    if (address.equals(packet.getAddress().getHostAddress()) && packet.getPort() == this.port) {
                        state = State.SET_UP;
                        relpy = "ACK_sender";
                        DEBUG.print("SYN-ACK received from " + address + ":" + port);
                    } else {
                        //not the correct receiver,ignore!!!
                        continue;
                    }
                    break;
                default:
                    continue;
            }
            outgoing = new DatagramPacket(relpy.getBytes(), relpy.length());
            outgoing.setAddress(packet.getAddress());
            outgoing.setPort(packet.getPort());
            try {
                socket.send(outgoing);
            } catch (IOException e) {
                e.printStackTrace();
                System.exit(-1);
            }
        }
    }

    public boolean isConnected() {
        return state == State.SET_UP;
    }

    /**
     * TODO: implement heartbeat
     */
    public void heartbeat() {

    }


    /**
     * The first handshake from sender SYN
     *
     * @param ip   the address of receiver
     * @param port the port of receiver
     */
    public void handShake(String ip, int port) throws IOException {
        state = State.STOP;
        String header = "SYN_sender";
        DatagramPacket packet = new DatagramPacket(header.getBytes(), header.length());
        packet.setAddress(InetAddress.getByName(ip));
        packet.setPort(port);
        state = State.WAITING_HANDSHAKE;
        this.address = ip;
        this.port = port;
        socket.send(packet);
        DEBUG.print("SYN send to " + ip + ":" + port + ", " + header);
        init();

    }


    public enum State {
        STOP,
        WAITING_HANDSHAKE,
        SET_UP;
    }


}
