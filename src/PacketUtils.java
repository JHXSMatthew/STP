/**
 * Created by Matthew on 5/09/2016
 * some help method for packets.
 */
public class PacketUtils {

    public static int get4BytesInt(byte[] data, int offset) {
        return ((data[offset] & 0xff) << 24) | ((data[offset + 1] & 0xff) << 16) |
                ((data[offset + 2] & 0xff) << 8) | (data[offset + 3] & 0xff);
    }

    public static void fill4BytesFromInt(int i, byte[] data, int offset) {
        data[offset] = (byte) (i >> 24);
        data[offset + 1] = (byte) (i >> 16);
        data[offset + 2] = (byte) (i >> 8);
        data[offset + 3] = (byte) (i >> 0);
    }

    public static void setHeader(STPPacket packet, int seq, int ack) {
        packet.setSeq(seq);
        packet.setAck(ack);
    }
}
