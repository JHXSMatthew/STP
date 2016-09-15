import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;

/**
 * Created by Matthew on 5/09/2016.
 */
public abstract class STPConnection {

    private DatagramSocket socket;
    private State state = State.STOP;
    private long time = System.currentTimeMillis();

    private String connectionAddress = "nohting";
    private int connectionPort;
    private PacketLogger logger;

    private DatagramPacket packet; // track last packet received.

    private int mss = 0;
    protected int lastSeq = 0;
    protected int lastAck = -1;

    public STPConnection() throws SocketException {
        socket = new DatagramSocket();
        logger = new PacketLogger("Sender_log.txt");
    }

    public STPConnection(int port) throws SocketException {
        socket = new DatagramSocket(port);
        logger = new PacketLogger("Receiver_log.txt");
    }




    protected void listen(int mss) {
        //System.out.println("Listen on " + socket.getLocalPort());
        this.mss = mss;
        while (state != State.FINISH) {
            packet = new DatagramPacket(new byte[this.mss + STPPacket.HEADER_SIZE], this.mss + STPPacket.HEADER_SIZE);
            try {
                socket.receive(packet);
            } catch (IOException e) {
                e.printStackTrace();
                System.out.println("STPPacket Received IO Error!");
                System.exit(-1);
            }
            if (reliable(packet)) { //only allow either reliable packet or handshake stuff
                STPPacket stpPacket = new STPPacket(packet.getData());
                logger.log(PacketLogger.PacketType.rcv, getTime(), stpPacket);
                if (state == State.FIN) {
                    onFinPacketReceived(stpPacket);
                } else {
                    onPacketReceived(stpPacket);
                }
            } else if (state == State.WAITING_HANDSHAKE) {
                STPPacket stpPacket = new STPPacket(packet.getData());
                logger.log(PacketLogger.PacketType.rcv, getTime(), stpPacket);
                onHandshakeReceived(new STPPacket(packet.getData()), packet.getAddress().getHostAddress(), packet.getPort());
            } else {
                System.out.println("Unhandle statement!");

            }
        }
    }


    public void bufferPacketInfo(String connectionAddress, int port) {
        this.connectionAddress = connectionAddress;
        this.connectionPort = port;
    }

    /**
     * @return the last packet received address.
     */
    public String getLastAddress() {
        return connectionAddress;
    }

    /**
     * @return the last packet received port.
     */
    public int getLastPort() {
        return connectionPort;
    }


    public boolean reliable(DatagramPacket packet) {
        return (state == State.ESTABLISHED || state == State.FIN) && packet.getPort() == this.connectionPort && packet.getAddress().getHostAddress().equals(connectionAddress);
    }

    public boolean isConnected() {
        return state == State.ESTABLISHED;
    }

    protected void setState(State state) {
        this.state = state;
        if(state == State.FINISH){
            logger.save();
        }
    }

    /**
     * explicitly set mss
     *
     * @param mss the mss to set
     */
    protected void setMss(int mss) {
        this.mss = mss;
    }

    /**
     * note: this is an unsafe method to send packet, it works before connection set-up but not reliable at all!!!
     *
     * @param packet  packet
     * @param address
     * @param port
     * @UNSAFE
     */
    protected void sendUnsafePacket(STPPacket packet, String address, int port) throws IOException {
        logger.log(PacketLogger.PacketType.snd, getTime(), packet);
        byte[] packetData = packet.toBytes();
        DatagramPacket p = new DatagramPacket(packetData, packetData.length, InetAddress.getByName(address), port);

        socket.send(p);
    }

    /**
     * reliable data sending
     *
     * @param packet the packet to send
     * @throws IOException
     * @Precondition connection not lost.
     */
    protected void sendPacket(STPPacket packet) throws IOException {
        sendUnsafePacket(packet, connectionAddress, connectionPort);
    }

    protected void dropPacket(STPPacket packet) {
        logger.log(PacketLogger.PacketType.drop, getTime(), packet);
    }

    protected PacketLogger getLogger(){
        return logger;
    }

    protected void connectionSetUp(String address, int port) {
        this.connectionAddress = address;
        this.connectionPort = port;
        setState(State.ESTABLISHED);
    }


    private double getTime() {
        return System.currentTimeMillis() - time;
    }

    protected abstract void onPacketReceived(STPPacket packet);

    protected abstract void onHandshakeReceived(STPPacket packet, String address, int port);

    protected abstract void onFinPacketReceived(STPPacket packet);


    public enum State {
        STOP,
        WAITING_HANDSHAKE,
        ESTABLISHED,
        FIN,
        FINISH;
    }
}
