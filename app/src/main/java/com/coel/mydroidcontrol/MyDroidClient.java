package com.coel.mydroidcontrol;

import android.os.Bundle;
import android.os.Looper;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.Button;

import java.lang.Thread;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.util.concurrent.CountDownLatch;


/**
 * Created by coel on 04.05.2016.
 */
public class MyDroidClient {
    private static final int MIN_DATAGRAM_PACKET_SIZE = 8;
    private static final int MAX_DATAGRAM_PACKET_SIZE = 128;
    private static final int SEND_INPUT_MILLIS = 500;
    private static final long CONNECT_TIMEOUT_MILLIS = 10000;
    private DatagramSocket m_socket;
    private volatile int m_state;
    private long m_beginConnectMillis;
    private InetAddress m_droidAddress;
    private int m_droidPort;
    private MyDroidInput m_currentInput;
    private boolean m_anyInputSent;
    private long m_lastInputSendMillis;
    private CountDownLatch m_clientThreadLatch;
    private volatile Handler m_uiHandler;
    private volatile Handler m_clientHandler;
    private Thread m_clientThread;
    private OnAuthListener m_OnAuthListener;
    private OnAuthErrorListener m_OnAuthErrorListener;
    private OnDisconnectListener m_OnDisconnectListener;

    public MyDroidClient() {
        m_state = MyDroidClientState.DISCONNECTED;
        m_socket = null;
        createUiHandler();
        m_clientHandler = null;
    }

    public int getState() {
        return m_state;
    }

    public void connect(String host, int port, String password) {
        if (m_clientThread == null) {
            createClientThread();
        }
        Bundle msgdata = new Bundle();
        MyDroidAuthTry authTry = new MyDroidAuthTry(host, port, password);
        msgdata.putParcelable("authTry", authTry);
        sendMessageToClientThread(MyDroidClientThreadMessage.CONNECT, msgdata);
    }

    public void disconnect() {
        if (m_clientThread == null) {
            return;
        }
        sendMessageToClientThread(MyDroidClientThreadMessage.DISCONNECT, null);
        destroyClientThread();
    }

    public void setInput(MyDroidInput input) {
        Bundle msgdata = new Bundle();
        msgdata.putParcelable("input", input);
        sendMessageToClientThread(MyDroidClientThreadMessage.INPUT, msgdata);
    }

    private void createUiHandler() {
        final MyDroidClient client = this;
        m_uiHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                Bundle bundle = msg.getData();
                switch (msg.what) {
                    case MyDroidUiThreadMessage.AUTH_OK:
                        if (m_OnAuthListener != null) {
                            m_OnAuthListener.onAuth(client, true);
                        }
                        break;
                    case MyDroidUiThreadMessage.AUTH_FAIL:
                        if (m_OnAuthListener != null) {
                            m_OnAuthListener.onAuth(client, false);
                        }
                        disconnect();
                        break;
                    case MyDroidUiThreadMessage.AUTH_ERROR:
                        if (m_OnAuthErrorListener != null) {
                            String errorMessage = bundle.getString("error");
                            m_OnAuthErrorListener.onAuthError(client, errorMessage);
                        }
                        disconnect();
                        break;
                    case MyDroidUiThreadMessage.DISCONNECT:
                        if (m_OnDisconnectListener != null) {
                            m_OnDisconnectListener.onDisconnect(client);
                        }
                        disconnect();
                        break;
                }
            }
        };
    }

    private void createClientThread() {
        Log.d("mydroidcontrol", "creating client thread...");
        if (m_clientThread != null) {
            return;
        }
        m_clientThreadLatch = new CountDownLatch(1);
        m_clientThread = new Thread() {
            public void run() {
                Looper.prepare();
                m_clientHandler = new Handler() {
                    public void handleMessage(Message msg) {
                        switch (msg.what) {
                            case MyDroidClientThreadMessage.KILL:
                                stopClientThread();
                                break;
                            case MyDroidClientThreadMessage.CONNECT:
                                onUiMessageConnect(msg);
                                break;
                            case MyDroidClientThreadMessage.DISCONNECT:
                                onUiMessageDisconnect(msg);
                                break;
                            case MyDroidClientThreadMessage.INPUT:
                                onUiMessageNewInput(msg);
                                break;
                        };
                    }
                };
                m_clientThreadLatch.countDown();
                Looper.loop();
            }
        };
        m_clientThread.start();
        try {
            m_clientThreadLatch.await();
        } catch (java.lang.InterruptedException ex) {
        }
    }

    private void destroyClientThread() {
        Log.d("mydroidcontrol", "destroying client thread...");
        if (m_clientThread == null) {
            return;
        }
        sendMessageToClientThread(MyDroidClientThreadMessage.KILL, null);
        try {
            m_clientThread.join(); // wait until client thread finishes
        } catch (java.lang.InterruptedException ex) {
        }
        m_clientThread = null;
    }

    private void stopClientThread() {
        m_state = MyDroidClientState.DISCONNECTED;
        if (m_socket != null) {
            m_socket.close();
        }
        m_socket = null;
        m_clientHandler = null;
        Looper myLooper = Looper.myLooper();
        if (myLooper!=null) {
            myLooper.quit();
        }
    }

    private void sendAuthErrorMessageToUiThread(String errorMessage) {
        Bundle authErrorData = new Bundle();
        authErrorData.putString("error", errorMessage);
        sendMessageToUiThread(MyDroidUiThreadMessage.AUTH_ERROR, authErrorData);
    }

    private void onUiMessageConnect(Message msg) {
        Log.d("mydroidcontrol", "processing 'connect' message from ui thread");
        m_state = MyDroidClientState.CONNECTING;
        Bundle msgdata = msg.getData();
        MyDroidAuthTry authTry = msgdata.getParcelable("authTry");

        if (authTry == null) {
            m_state = MyDroidClientState.DISCONNECTED;
            sendAuthErrorMessageToUiThread("authTry is null");
            return;
        }

        m_socket = null;
        try {
            m_socket = new DatagramSocket();
        } catch (java.net.SocketException ex) {
            m_socket = null;
            m_state = MyDroidClientState.DISCONNECTED;
            sendAuthErrorMessageToUiThread(ex.getMessage());
            return;
        }
        try {
            m_socket.setSoTimeout(1);
        } catch (java.net.SocketException ex) {
            m_socket.close();
            m_socket = null;
            m_state = MyDroidClientState.DISCONNECTED;
            sendAuthErrorMessageToUiThread(ex.getMessage());
            return;
        }

        byte[] password_bytes = null;
        if (authTry.password != null) {
            password_bytes = authTry.password.getBytes(Charset.forName("UTF-8"));
        }
        if (password_bytes != null && password_bytes.length > MyDroidProtocol.MAX_PASSWORD_LENGTH) {
            m_state = MyDroidClientState.DISCONNECTED;
            sendAuthErrorMessageToUiThread("password is too long");
            return;
        }

        InetAddress ia;
        try {
            ia = InetAddress.getByName(authTry.host);
        } catch (UnknownHostException ex) {
            m_state = MyDroidClientState.DISCONNECTED;
            sendAuthErrorMessageToUiThread(ex.getMessage());
            return;
        }

        m_droidAddress = ia;
        m_droidPort = authTry.port;
        m_anyInputSent = false;
        m_beginConnectMillis = System.currentTimeMillis();

        byte[] packet_data = new byte[8+MyDroidProtocol.MAX_PASSWORD_LENGTH];
        MyDroidProtocol.putSignature(packet_data);
        packet_data[4] = MyDroidProtocol.NETMSG_LOGIN;
        packet_data[5] = 0;
        packet_data[6] = MyDroidProtocol.MAX_PASSWORD_LENGTH;
        packet_data[7] = 0;
        if (password_bytes != null) {
            for (int i = 0; i < password_bytes.length; i++) {
                packet_data[8+i] = password_bytes[i];
            }
            if (password_bytes.length < MyDroidProtocol.MAX_PASSWORD_LENGTH) {
                packet_data[8+password_bytes.length] = 0;
            }
        } else {
            packet_data[8] = 0;
        }
        DatagramPacket packet = new DatagramPacket(packet_data, packet_data.length);
        packet.setAddress(ia);
        packet.setPort(authTry.port);
        try {
            m_socket.send(packet);
        } catch(java.io.IOException ex) {
            m_state = MyDroidClientState.DISCONNECTED;
            sendAuthErrorMessageToUiThread(ex.getMessage());
            return;
        }

        scheduleReceiveDatagram();
        scheduleClientLoop();
    }

    private void onUiMessageDisconnect(Message msg) {
        Log.d("mydroidcontrol", "processing 'disconnect' message from ui thread");
        if (m_state == MyDroidClientState.DISCONNECTED) {
            return;
        }
        m_state = MyDroidClientState.DISCONNECTED;

        byte[] packet_data = new byte[8];
        MyDroidProtocol.putSignature(packet_data);
        packet_data[4] = MyDroidProtocol.NETMSG_CLIENT_DISCONNECT;
        packet_data[5] = 0;
        packet_data[6] = 0;
        packet_data[7] = 0;
        DatagramPacket packet = new DatagramPacket(packet_data, packet_data.length);
        packet.setAddress(m_droidAddress);
        packet.setPort(m_droidPort);
        try {
            m_socket.send(packet);
        } catch(java.io.IOException ex) {
            Log.e("mydroidcontrol", ex.getMessage());
        }
    }

    private void onUiMessageNewInput(Message msg) {
        MyDroidInput old = m_currentInput;
        Bundle msgdata = msg.getData();
        MyDroidInput input = msgdata.getParcelable("input");
        if (input == null) {
            return;
        }
        if (input == m_currentInput) {
            Log.w("mydroidcontrol", "current input and new input is the same object");
        }
        m_currentInput = new MyDroidInput(input);
        if (!m_currentInput.equals(old)) {
            sendInput();
        }
        else
        {
            Log.d("mydroidcontrol", "'new' input equals 'old'. "
                    + "\n" + " 'new': " + m_currentInput.toString()
                    + "\n" + " 'old': " + old.toString());
        }
    }

    private void sendInput() {
        m_anyInputSent = true;
        m_lastInputSendMillis = System.currentTimeMillis();

        if (m_currentInput == null) {
            m_currentInput = new MyDroidInput();
        }

        byte[] packet_data = new byte[8+5];
        MyDroidProtocol.putSignature(packet_data);
        packet_data[4] = MyDroidProtocol.NETMSG_CDROID_INPUT;
        packet_data[5] = 0;
        packet_data[6] = 5;
        packet_data[7] = 0;
        packet_data[8] = m_currentInput.moveForward ? (byte) 1 : (byte) 0;
        packet_data[9] = m_currentInput.moveBackward ? (byte) 1 : (byte) 0;
        packet_data[10] = m_currentInput.turnLeft ? (byte) 1 : (byte) 0;
        packet_data[11] = m_currentInput.turnRight ? (byte) 1 : (byte) 0;
        packet_data[12] = (byte) m_currentInput.speed;
        DatagramPacket packet = new DatagramPacket(packet_data, packet_data.length);
        packet.setAddress(m_droidAddress);
        packet.setPort(m_droidPort);
        try {
            m_socket.send(packet);
        } catch(java.io.IOException ex) {
            Log.e("mydroidcontrol", ex.getMessage());
        }
    }

    private void scheduleReceiveDatagram() {
        Runnable receiveDatagramRunnable = new Runnable() {
            public void run() {
                if (m_socket == null) {
                    return;
                }
                receiveDatagram();
                m_clientHandler.postDelayed(this, 10);
            }
        };
        m_clientHandler.postDelayed(receiveDatagramRunnable, 10);
    }

    private void scheduleClientLoop() {
        Runnable clientLoopRunnable = new Runnable() {
            public void run() {
                if (m_socket == null) {
                    return;
                }
                clientLoop();
                m_clientHandler.postDelayed(this, 50);
            }
        };
        m_clientHandler.postDelayed(clientLoopRunnable, 50);
    }

    private void receiveDatagram() {
        if (m_socket == null) {
            return;
        }
        try {
            while (true) {
                byte[] buffer = new byte[MAX_DATAGRAM_PACKET_SIZE];
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                m_socket.receive(packet);
                processPacket(packet);
            }
        } catch (java.io.IOException ex) {
        }
    }

    private void clientLoop() {
        if (m_socket == null) {
            return;
        }
        if (m_state == MyDroidClientState.CONNECTING) {
            long millis = System.currentTimeMillis();
            if ((millis - m_beginConnectMillis) > CONNECT_TIMEOUT_MILLIS) {
                m_state = MyDroidClientState.DISCONNECTED;
                sendAuthErrorMessageToUiThread("timeout");
            }
        }
        if (m_state == MyDroidClientState.ACTIVE) {
            if (!m_anyInputSent) {
                sendInput();
            } else {
                long millis = System.currentTimeMillis();
                if ((millis - m_lastInputSendMillis) > SEND_INPUT_MILLIS) {
                    sendInput();
                }
            }
        }
    }

    private void processPacket(DatagramPacket packet) {
        { // check address and port
            InetAddress ia = packet.getAddress();
            int port = packet.getPort();
            if (!ia.equals(m_droidAddress)) {
                return;
            }
            if (port != m_droidPort) {
                return;
            }
        }
        int packet_len = packet.getLength();
        if (packet_len < MIN_DATAGRAM_PACKET_SIZE) {
            return;
        }
        if (packet_len > MAX_DATAGRAM_PACKET_SIZE) {
            return;
        }
        byte[] data = packet.getData();
        if (!MyDroidProtocol.checkSignature(data)) {
            return;
        }
        int msgid = ((int) data[5]) << 8 | ((int) data[4]);
        int msgdatalen = ((int) data[7]) << 8 | ((int) data[6]);
        if ((msgdatalen + 8) != packet_len) {
            return;
        }
        byte[] msgdata = new byte[msgdatalen];
        System.arraycopy(data, 8, msgdata, 0, msgdatalen);
        processMyDroidMessage(msgid, msgdata);
    }

    private void processMyDroidMessage(int msgid, byte[] msgdata) {
        if (msgid == MyDroidProtocol.NETMSG_AUTH_OK) {
            m_state = MyDroidClientState.ACTIVE;
            sendMessageToUiThread(MyDroidUiThreadMessage.AUTH_OK, null);
            return;
        }
        if (msgid == MyDroidProtocol.NETMSG_AUTH_FAIL) {
            m_state = MyDroidClientState.DISCONNECTED;
            sendMessageToUiThread(MyDroidUiThreadMessage.AUTH_FAIL, null);
            return;
        }
        if (msgid == MyDroidProtocol.NETMSG_CDROID_DISCONNECT) {
            m_state = MyDroidClientState.DISCONNECTED;
            if (m_socket != null) {
                m_socket.close();
            }
            m_socket = null;
            sendMessageToUiThread(MyDroidUiThreadMessage.DISCONNECT, null);
            return;
        }
    }

    private void sendMessageToUiThread(int what, Bundle msgdata) {
        if (m_uiHandler == null) {
            Log.e("mydroidcontrol", "ui thread handler is null");
            return;
        }
        Message msg = m_uiHandler.obtainMessage(what);
        if (msgdata != null) {
            msg.setData(msgdata);
        }
        m_uiHandler.sendMessage(msg);
    }

    private void sendMessageToClientThread(int what, Bundle msgdata) {
        if (m_clientHandler == null) {
            Log.e("mydroidcontrol", "client thread handler is null");
            return;
        }
        Message msg = m_clientHandler.obtainMessage(what);
        if (msgdata != null) {
            msg.setData(msgdata);
        }
        m_clientHandler.sendMessage(msg);
    }

    public void setOnAuthListener(OnAuthListener listener) {
        m_OnAuthListener = listener;
    }

    public void setOnAuthErrorListener(OnAuthErrorListener listener) {
        m_OnAuthErrorListener = listener;
    }

    public void setOnDisconnectListener(OnDisconnectListener listener) {
        m_OnDisconnectListener = listener;
    }

    /* TODO:
    public void captureShot();
    public void lockShot() throws IllegalStateException;
    public int getShotWidth();
    public int getShotHeight();
    public int getShotFormat();
    public byte[] getShotData();
    public void unlockShot();
    public void setOnCaptureListener(OnCaptureListener listener) {
    }
    public interface OnCaptureListener {
        void onCapture(MyDroidClient cl);
    }
     */

    /**
     * Interface definition for a callback to be invoked when auth response received
     */
    public interface OnAuthListener {
        /**
         * Called when there is response from droid or none
         *
         * @param cl The client
         * @param ok True - auth ok, False - auth fail
         */
        void onAuth(MyDroidClient cl, boolean ok);
    }

    public interface OnDisconnectListener {
        void onDisconnect(MyDroidClient cl);
    }

    public interface OnAuthErrorListener {
        void onAuthError(MyDroidClient cl, String errorMessage);
    }
}
