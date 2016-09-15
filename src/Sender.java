import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.SocketException;
import java.util.Arrays;
import java.util.PriorityQueue;
import java.util.Random;
import java.util.Scanner;

/**
 * Created by Matthew on 5/09/2016.
 */
public class Sender extends STPConnection {


    //file data
    private byte[] data;
    private int pointer = 0;
    //reliable connection data
    private String address;
    private int port;
    private int mss;
    private int mws;
    private double pdrop = 0;
    //dynamic sender data

    private PriorityQueue<STPPacket> packetWindow;
    private SenderWatchdog watchdog = null;
    private Random random;
    private int MAX_PACKET = 0;
    //fast -retransmit
    private int reTranACK = -1;
    private int reTranNum = 0;
    //statistic counter
    private int dataSegmentSent = 0;
    private int drops = 0;
    private int delayed = 0;
    private int retransmitted = 0;
    private int duplicateACK = 0;

    private RTTCalculator calculator;
    private static boolean CALCULATOR = false;

    public Sender(final String address, final int port, byte[] data, int mws, int mss, int timeout, double pdrop, long seed) throws SocketException {
        super();
        this.address = address;
        this.port = port;
        this.watchdog = new SenderWatchdog(this, timeout);
        this.mss = mss;
        this.mws = mws;
        if(mws < mss){ //TODO: not sure if this situation happens
            mss = mws;
        }
        this.data = data;
        packetWindow = new PriorityQueue<>();
        random = new Random(seed);
        this.pdrop = pdrop;
        //TODO: confirm if this should be random
        //lastSeq = random.nextInt();
        lastSeq = 0;
        //=============================== First SYN ==========================
        try {
            handshake();
        } catch (IOException e) {
            e.printStackTrace();
        }
        //=============================== Init Watchdog ==========================
        watchdog.reset();
        //=============================== Listen ==========================
        MAX_PACKET = mws / mss;
        if(CALCULATOR){
            calculator = new RTTCalculator();
        }
        listen(0);

    }

    public static void main(String[] args) throws FileNotFoundException {
        String ip = args[0];
        int port = Integer.parseInt(args[1]);
        StringBuilder builder = new StringBuilder();
        Scanner scanner = new Scanner(new File(args[2]), "ASCII");
        while (scanner.hasNextLine()) {
            builder.append(scanner.nextLine());
            builder.append("\n");
        }
        byte[] data = builder.toString().getBytes();
        int mws = Integer.parseInt(args[3]);
        int mss = Integer.parseInt(args[4]);

        int timeout = Integer.parseInt(args[5]);
        double pdrop = Double.parseDouble(args[6]);
        long seed = Long.parseLong(args[7]);

        try {
            Sender sender = new Sender(ip, port, data, mws, mss, timeout, pdrop, seed);
        } catch (SocketException e) {
            e.printStackTrace();
            System.exit(-1);
        }
    }

    @Override
    public void sendPacket(STPPacket packet) throws IOException {
        lastSeq += packet.getDataLength();
        if(packet.getFlagString().equals("D")){
            dataSegmentSent ++;
            if (random.nextDouble() < pdrop) {
                dropPacket(packet);
                drops++;
                return;
            }
        }
        if(calculator != null){
            calculator.onPacketSend(packet);
        }
        super.sendPacket(packet);
    }

    /**
     *  the method to retransmit packets
     * @throws IOException fail to send packet
     */
    public void retransmit() throws IOException {
        //retransmit packet last ACK
        if (FINCheck())
            return;

        //selective repeat
        STPPacket current = packetWindow.peek();
        if(isConnected()){
            if (random.nextFloat() < pdrop) {
                dropPacket(current);
                drops++;
            }else{
                super.sendPacket(current);
            }
            retransmitted ++;
        }else{
            sendUnsafePacket(current, address, port);
            //retransmitted ++; HANDSHAKE PACKET RESENT SHOULD NOT BE TAKEN INTO ACCOUNT!!!!
        }


        /*go back N
        for (STPPacket current : packetWindow) {
            if (isConnected()) {
                retransmitted ++;
                if (random.nextDouble() < pdrop) {
                    dropPacket(current);
                    drops++;
                    continue;
                }
                super.sendPacket(current);

            } else {
                retransmitted ++;
                sendUnsafePacket(current, address, port);
            }
        }
        */

        watchdog.feed();
    }


    /**
     * sender First SYN handshake
     *
     * @precondition: state is stop
     */
    private void handshake() throws IOException {
        setState(State.STOP); // stop current listening process
        STPPacket packet = new STPPacket(null);
        packet.setMss(mss);
        packet.setFlags(true, STPPacket.SYN);
        packet.setSeq(lastSeq);
        lastSeq++;
        packetWindow.add(packet);
        setState(State.WAITING_HANDSHAKE);
        try {
            sendUnsafePacket(packet, address, port);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * FIN
     *
     * @Precondition must be established state
     */
    private void shutdown() throws IOException {
        setState(State.FIN);
        STPPacket packet = new STPPacket(null);
        packet.setFlags(true, STPPacket.FIN);
        PacketUtils.setHeader(packet, lastSeq, lastAck);
        lastSeq++;
        packetWindow.add(packet);
        sendPacket(packet);
    }

    /**
     *  clean all acked packets in the sender window
     * @param packet the packet just received.
     */
    private void cleanWindow(STPPacket packet){
        STPPacket unACKPacket = packetWindow.peek();
        while (unACKPacket != null && unACKPacket.getSeq() + unACKPacket.getDataLength() <= packet.getAck()) {
            unACKPacket = packetWindow.poll();
            if (packetWindow.isEmpty()) {
                break;
            }
            unACKPacket = packetWindow.peek();
            watchdog.feed();
        }
    }
    /**
     * the method to fill the sender window
     */
    private void fillWindow() {
        while (packetWindow.size() < MAX_PACKET && pointer < data.length) {
            int to = pointer + mss;
            if (to > data.length) {
                to = data.length;
            }
            byte[] buffer = Arrays.copyOfRange(data, pointer, to);

            STPPacket send = new STPPacket(null);
            send.setData(buffer);
            PacketUtils.setHeader(send, lastSeq, lastAck);
            send.setMss(to - pointer);
            packetWindow.add(send);
            pointer = to;
            try {
                sendPacket(send);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }


    }

    private boolean FINCheck() {
        //FIN triggered
        if (pointer == data.length && packetWindow.isEmpty()) {
            try {
                shutdown();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return true;
        }
        return false;
    }


    @Override
    protected void onPacketReceived(STPPacket packet) {
        if (isConnected()) {
            if (packet.isFlagSet(STPPacket.ACK)) {
                if(calculator != null){
                    calculator.onPacketReceived(packet);
                }
                if (packet.getAck() < lastSeq && packet.getAck() == reTranACK && reTranNum == 3) {
                    //System.err.println("Fast retransmit!");
                    try {
                        retransmit();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else {
                    cleanWindow(packet);
                    fillWindow();
                }
                if (reTranACK != packet.getAck()) {
                    reTranACK = packet.getAck();
                    reTranNum = 0;
                } else {
                    duplicateACK ++;
                    reTranNum++;
                }
                if (FINCheck())
                    return;
            } else {
                System.err.println("WTF PACKET RECEIVED, WHY IT'S NOT AN ACK?");

            }
        } else {
            System.out.println("Drop");
        }
    }

    @Override
    protected void onHandshakeReceived(STPPacket packet, String address, int port) {
        if (isConnected()) {
            return;
        }
        if (address.equals(this.address) && port == this.port) {
            if(lastAck != -1)
                return;
            if (packet.isFlagSet(STPPacket.SYN) && packet.isFlagSet(STPPacket.ACK) && packet.getAck() == lastSeq) {
                watchdog.feed();
                packetWindow.clear();
                STPPacket packet1 = new STPPacket(null);
                packet1.setMss(mss);
                packet1.setFlags(true, STPPacket.ACK);
                lastAck = packet.getSeq() + 1;
                PacketUtils.setHeader(packet1, lastSeq, lastAck);
                try {
                    sendUnsafePacket(packet1, address, port);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                connectionSetUp(address, port);
                fillWindow();
                watchdog.reset();

            }

        }
    }

    @Override
    protected void onFinPacketReceived(STPPacket packet) {
        if (packet.isFlagSet(STPPacket.ACK)) {
            if (packet.getAck() == lastSeq) {
                cleanWindow(packet);
                lastAck++;
            } else {
                System.out.println("ERROR! ACK");
                System.out.println(packet.getAck() + " " + lastSeq);
            }
        } else if (packet.isFlagSet(STPPacket.FIN)) {
            if (packet.getAck() == lastSeq) {
                STPPacket packet1 = new STPPacket(null);
                packet1.setFlags(true, STPPacket.ACK);
                PacketUtils.setHeader(packet1, lastSeq, lastAck);
                try {
                    sendPacket(packet1);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                getLogger().log(data.length, dataSegmentSent, drops, delayed, retransmitted, duplicateACK);
                setState(State.FINISH);
                if(calculator != null){
                    calculator.finish();
                }

                System.exit(1);
            } else {
                System.out.println("ERROR! FIN");
                System.out.println(packet.getAck() + " " + lastSeq);
            }
        }
    }
}
