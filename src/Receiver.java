import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.SocketException;
import java.util.PriorityQueue;

/**
 * Created by Matthew on 5/09/2016.
 * The receiver
 */
public class Receiver extends STPConnection {

    private File file;
    private BufferedWriter writer;
    private PriorityQueue<STPPacket> buffer = new PriorityQueue<>();

    //statistics
    private int dataReceived = 0;
    private int dataSegmentReceived = 0;
    private int duplicatedSegment = 0;


    public Receiver(int port, String fileName) throws SocketException {
        super(port);
        file = new File(fileName);
        setState(State.WAITING_HANDSHAKE);
        listen(0); //listen on a header value, must be changed after Handshake
    }

    public static void main(String[] args) throws SocketException {
        int port = Integer.parseInt(args[0]);
        String fileName = args[1];

        Receiver receiver = new Receiver(port, fileName);
    }

    /**
     * @param data the string to write to the file
     */
    private void write(String data) {
        try {
            if (writer == null) {
                if (!file.exists()) {
                    file.createNewFile();
                }
                writer = new BufferedWriter(new FileWriter(file));
            }
            writer.write(data);
            dataReceived += data.length();


        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * try to get a cumulative the Ack number
     * selective ack
     */
    private void checkBuffer() {
        STPPacket packet = buffer.peek();
        if (packet == null) {
            return;
        }
        while (lastAck == packet.getSeq()) {
            packet = buffer.poll();
            lastAck += packet.getDataLength();
            write(packet.getDataString());
            if (buffer.isEmpty()) {
                break;
            }
            packet = buffer.peek();
        }
    }


    @Override
    protected void onPacketReceived(STPPacket packet) {
        if (packet.isFlagSet(STPPacket.FIN)) {
            //ACK
            setMss(0);
            setState(State.FIN);
            STPPacket packet1 = new STPPacket(null);
            packet1.setFlags(true, STPPacket.ACK);
            lastAck++;
            PacketUtils.setHeader(packet1, lastSeq, lastAck);
            try {
                sendPacket(packet1);
            } catch (IOException e) {
                e.printStackTrace();
            }

            //FIN
            STPPacket packet2 = new STPPacket(null);
            packet2.setFlags(true, STPPacket.FIN);
            lastSeq++;
            PacketUtils.setHeader(packet2, lastSeq, lastAck);
            try {
                sendPacket(packet2);
            } catch (IOException e) {
                e.printStackTrace();
            }

        } else {
            if (packet.getSeq() == lastAck) {
                //correct packet order
                dataSegmentReceived++;
                lastAck += packet.getDataLength();
                write(packet.getDataString());
                // I really should delay it and see if next packet arrives, however, spec says I should not delay it. :(
            } else if (packet.getSeq() > lastAck) {
                boolean b = false;
                for (STPPacket temp : buffer) {
                    if (temp.getSeq() == packet.getSeq()) {
                        b = true;
                        break;
                    }
                }
                if (!b) {
                    buffer.add(packet);
                    dataSegmentReceived++;
                } else {
                    duplicatedSegment++;
                }
            } else {
                duplicatedSegment++;
            }
            checkBuffer();
            STPPacket packet1 = new STPPacket(null);
            PacketUtils.setHeader(packet1, lastSeq, lastAck);
            packet1.setFlags(true, STPPacket.ACK);
            try {
                sendPacket(packet1);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    protected void onHandshakeReceived(STPPacket packet, String address, int port) {
        if (isConnected()) {
            return;
        }
        if (packet.isFlagSet(STPPacket.SYN)) {
            if (lastAck != -1)
                return;

            super.bufferPacketInfo(address, port);
            lastAck = packet.getSeq() + 1;
            STPPacket packet1 = new STPPacket(null);
            packet1.setFlags(true, STPPacket.ACK);
            packet1.setFlags(true, STPPacket.SYN);
            packet1.setData(null);
            PacketUtils.setHeader(packet1, lastSeq, lastAck);
            lastSeq++;
            try {
                sendUnsafePacket(packet1, address, port);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else if (packet.isFlagSet(STPPacket.ACK)) {
            if (getLastAddress().equals(address) && getLastPort() == port && lastAck == packet.getSeq()) {
                setMss(packet.getMss());
                connectionSetUp(address, port);
                //System.out.println("Handshake Done!");
            } else {
                System.out.println("drop ACK packet from sender, not reliable handshake, send SYN first!");
            }
        }

    }

    @Override
    protected void onFinPacketReceived(STPPacket packet) {
        if (packet.isFlagSet(STPPacket.ACK)) {
            if (packet.getAck() == lastSeq) { //check state FIN_WAIT_2
                try {
                    writer.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                getLogger().log(dataReceived, dataSegmentReceived, duplicatedSegment);
                setState(State.FINISH);
                System.exit(1);
            } else {
                System.out.println("ERROR!");
            }
        }
    }
}
