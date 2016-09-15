import java.io.*;
import java.util.Formatter;

/**
 * Created by Matthew on 13/09/2016.
 */
public class PacketLogger {

    private boolean print = true;
    private String format = "%-4s %-4s %-3s %-10s %-10s %-10s %n";
    private BufferedWriter f;

    //files...
    public PacketLogger(String logFileName) {
        File f = new File(logFileName);
        if(!f.exists()){
            try {
                f.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        try {
            this.f = new BufferedWriter(new FileWriter(f));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void log(PacketType type, double time, STPPacket packet) {
        if(print){
            System.err.format(format, type.toString(), time, packet.getFlagString(), packet.getSeq(), packet.getDataLength(), packet.getAck());
        }

        try {
            f.write(String.format(format, type.toString(), time, packet.getFlagString(), packet.getSeq(), packet.getDataLength(), packet.getAck()));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void log(int dataTransferred, int dataSegmentSend, int dropped , int delayed , int retransmitted , int duplicatedACK){
        try {
            f.write("\n");
            f.write("DataTransferred: " +  dataTransferred + "\n" );
            f.write("DataSegmentSend: " +  dataSegmentSend  + "\n");
            f.write("Dropped: " +  dropped + "\n" );
            f.write("Delayed: " +  delayed + "\n" );
            f.write("Retransmitted: " +  retransmitted + "\n" );
            f.write("DuplicatedACK: " +  duplicatedACK + "\n"  );
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void log(int dataReceived, int dataSegmentReceived, int duplicateSegmentReceived){
        try {
            f.write("\n");
            f.write("DataReceived: " +  dataReceived + "\n" );
            f.write("DataSegmentReceived: " +  dataSegmentReceived + "\n"  );
            f.write("DuplicateSegmentReceived: " +  duplicateSegmentReceived + "\n"  );
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void save(){
        try {
            f.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public enum PacketType {
        snd, rcv, drop;
    }
}
