import java.util.Arrays;
import java.util.Comparator;

/**
 * Created by Matthew on 5/09/2016.
 * The STP packet
 */
public class STPPacket implements Comparator<STPPacket>, Comparable<STPPacket> {
    // constant for checking flags
    public static int ACK = 0;
    public static int SYN = 1;
    public static int FIN = 2;
    public static int HEADER_SIZE = 14;
    //packet header
    private int seq = 0;
    private int ack = 0;
    private int mss = 0;
    // 0 -- ACK
    // 1 -- SYN
    // 2 -- FIN
    // 3-7 reserved.
    // 8-15 I simply want an even number for packet header, so let's leave 1 byte there.
    private boolean[] flags = new boolean[8];
    //data
    private byte[] data = null;

    /**
     * packet structure
     * ------------- 4 bytes of Seq -----------------
     * ------------- 4 bytes of ack -----------------
     * ------------- 4 bytes of mss size    ----------------- actually ,this one is tricky. you'll see when you check my sender code. lmao
     * ------------- 2 bytes for flags ---------------
     * -------------      data      -----------------
     *
     * @param packet packet
     */
    public STPPacket(byte[] packet) {
        if (packet == null) {
            return;
        }
        seq = PacketUtils.get4BytesInt(packet, 0); // now at 3
        ack = PacketUtils.get4BytesInt(packet, 4); // now at 7
        mss = PacketUtils.get4BytesInt(packet, 8);  //now at 11
        //bits to boolean
        for (int i = 0; i < 8; i++) {
            // if (i < 8) {
            flags[i] = (packet[12] & (0b00000001 << i)) != 0;
            //}else {
            //   flags[i] = (packet[13] & (0b00000001 << i)) != 0; //so many flags, no, I do not really need those
            // }
        }
        // now at 13
        if (HEADER_SIZE != packet.length) {
            data = Arrays.copyOfRange(packet, HEADER_SIZE, HEADER_SIZE + mss); //packet.length
        }

    }

    public void setFlags(boolean b, int flag) {
        flags[flag] = b;
    }


    public String getDataString() {
        return new String(data);
    }

    public int getSeq() {
        return seq;
    }

    public void setSeq(int seq) {
        this.seq = seq;
    }

    public int getAck() {
        return ack;
    }

    public void setAck(int ack) {
        this.ack = ack;
    }

    public int getMss() {
        return mss;
    }

    public void setMss(int mss) {
        this.mss = mss;
    }

    public int getDataLength() {
        if (data == null) {
            return 0;
        }
        return data.length;
    }

    public void setData(byte[] buffer) {
        this.data = buffer;
    }

    public boolean isFlagSet(int position) {
        return flags[position];
    }

    public byte[] toBytes() {
        int size = HEADER_SIZE;
        try {
            size += data.length;
        } catch (Exception e) {

        }
        byte[] returnValue = new byte[size];
        PacketUtils.fill4BytesFromInt(seq, returnValue, 0);
        PacketUtils.fill4BytesFromInt(ack, returnValue, 4);
        PacketUtils.fill4BytesFromInt(mss, returnValue, 8);
        for (int i = 0; i < 8; i++) {
            if (!flags[i]) {
                continue;
            }
            if (i < 4) {
                returnValue[12] = (byte) (returnValue[12] | (0b00000001 << i));
            } else {
                returnValue[13] = (byte) (returnValue[13] | (0b00000001 << i));
            }
        }
        if (data != null) {
            for (int i = 0; i < data.length; i++) {
                returnValue[i + HEADER_SIZE] = data[i];
            }
        }
        return returnValue;
    }

    public String getFlagString() {
        StringBuilder builder = new StringBuilder();
        if (flags[SYN]) {
            builder.append("S");
        }
        if (flags[ACK]) {
            builder.append("A");
        }
        if (flags[FIN]) {
            builder.append("F");
        }
        if ((!flags[SYN] && !flags[ACK] && !flags[FIN]) && data != null) {
            builder.append("D");
        }
        return builder.toString();
    }

    @Override
    public int compare(STPPacket o1, STPPacket o2) {
        return o1.seq - o2.seq;
    }


    @Override
    public int compareTo(STPPacket o) {
        return getSeq() - o.getSeq();
    }
}
