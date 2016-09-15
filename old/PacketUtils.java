package old;

/**
 * Created by Matthew on 5/09/2016.
 */
public class PacketUtils {

    public static int getPowerOfTwo(int num) {
        int returnValue = 2;
        while (num > returnValue) {
            returnValue *= 2;
        }
        return returnValue;
    }
}
