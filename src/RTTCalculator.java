import java.util.HashMap;

/**
 * Created by Matthew on 15/09/2016.
 * extended calculator for writing report.
 */
public class RTTCalculator {

    private static double alpha = 0.125;
    private static double beta = 0.25;
    private double estimatedRTT = 0;
    private double devRTT = 0;
    private HashMap<Integer, Long> packetHashMap = new HashMap<>();

    public void onPacketReceived(STPPacket packet) {
        if (packetHashMap.containsKey(packet.getAck())) {
            if (estimatedRTT == 0) {
                estimatedRTT = System.currentTimeMillis() - packetHashMap.remove(packet.getAck());
            } else {
                long sampleRTT = System.currentTimeMillis() - packetHashMap.remove(packet.getAck());
                estimatedRTT = estimatedRTT * (1 - alpha) + alpha * sampleRTT;
                devRTT = (1 - beta) * devRTT + beta * Math.abs(sampleRTT - estimatedRTT);
            }
        } else {
            System.err.println("cannot find" + packet.getAck());
        }
    }

    public void onPacketSend(STPPacket packet) {
        packetHashMap.put(packet.getSeq() + packet.getDataLength(), System.currentTimeMillis());
        //System.out.println("save" + (packet.getSeq() + packet.getDataLength()) );
    }

    public void finish() {
        System.out.println("estimatedRTT = " + (float) estimatedRTT + " devRTT = " + (float) devRTT);
    }
}
