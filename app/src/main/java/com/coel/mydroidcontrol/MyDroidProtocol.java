package com.coel.mydroidcontrol;

/**
 * Created by coel on 05.05.2016.
 */
public class MyDroidProtocol {
    public final static byte[] SIGNATURE = {'C', 'D', 'R', 'D'};
    public final static int MAX_PASSWORD_LENGTH = 32;
    public final static int MIN_SPEED = 0;
    public final static int MAX_SPEED = 255;
    public final static int NETMSG_LOGIN = 1;
    public final static int NETMSG_AUTH_OK = 2;
    public final static int NETMSG_AUTH_FAIL = 3;
    public final static int NETMSG_CDROID_INPUT = 4;
    public final static int NETMSG_CLIENT_DISCONNECT = 5;
    public final static int NETMSG_CDROID_DISCONNECT = 6;
    public final static int NETMSG_CAPTURE_SHOT = 7;
    public final static int NETMSG_SHOT_BEGIN = 8;
    public final static int NETMSG_SHOT_PIECE = 9;
    public final static int NETMSG_SHOT_END = 10;

    public static boolean checkSignature(byte[] in) {
        if (in == null) {
            return false;
        }
        if (in.length < SIGNATURE.length) {
            return false;
        }
        for (int i = 0; i < SIGNATURE.length; i++) {
            if (in[i] != SIGNATURE[i]) {
                return false;
            }
        }
        return true;
    }

    public static void putSignature(byte[] out) {
        if (out == null) {
            return;
        }
        if (out.length < SIGNATURE.length) {
            return;
        }
        for (int i = 0; i < SIGNATURE.length; i++) {
            out[i] = SIGNATURE[i];
        }
    }
}
