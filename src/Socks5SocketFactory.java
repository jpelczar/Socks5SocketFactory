import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Only IPv4, TCP/IP stream and authentication supported.
 * Created by jpelczar on 22.02.16.
 */
public class Socks5SocketFactory {

    private String proxyIp;
    private short proxyPort;
    private String username;
    private String password;

    private byte[] destIpBytes;
    private byte[] destPortBytes;

    public Socks5SocketFactory(String proxyIp, short proxyPort, String destIp, short destPort, String username, String password) throws IncorrectIpAddressException {
        this.proxyIp = proxyIp;
        this.proxyPort = proxyPort;
        this.username = username;
        this.password = password;

        destIpBytes = new byte[4];
        String[] destIpStrings = destIp.split(Pattern.quote("."));
        if (destIpStrings.length < 4) {
            throw new IncorrectIpAddressException();
        }
        destIpBytes[0] = (byte) Integer.parseInt(destIpStrings[0]);
        destIpBytes[1] = (byte) Integer.parseInt(destIpStrings[1]);
        destIpBytes[2] = (byte) Integer.parseInt(destIpStrings[2]);
        destIpBytes[3] = (byte) Integer.parseInt(destIpStrings[3]);

        destPortBytes = new byte[2];
        destPortBytes = ByteBuffer.allocate(2).putShort(destPort).array();
    }

    public Socket createSocket() throws
            IOException, SocksNotSupportedByServerException, AuthorizationFailedException, RequestFailedException {
        Socket socket = new Socket();
        socket.connect(new InetSocketAddress(proxyIp, proxyPort));

        DataInputStream in = new DataInputStream(socket.getInputStream());
        DataOutputStream out = new DataOutputStream(socket.getOutputStream());

        byte[] hello = new byte[3];
        hello[0] = (byte) 0x05; //SOCKS version number (SOCKS v5)
        hello[1] = (byte) 0x01; //number of authentication methods supported
        hello[2] = (byte) 0x02; //authentication methods supported - only one (with authentication)

        out.write(hello);
        out.flush();

        byte[] response = new byte[2];
        in.readFully(response);
        if (response[0] != (byte) 0x05 || response[1] != (byte) 0x02) { //Server response: first byte - SOCKS version number, second byte - chosen authentication method, 1 byte, or 0xFF if no acceptable methods were offered
            throw new SocksNotSupportedByServerException();
        }

        List<Byte> bytes = new ArrayList<>();

        bytes.add((byte) 0x01); //version number (must be 0x01)

        byte[] usernameBytes = username.getBytes();
        bytes.add((byte) usernameBytes.length); //username - length
        for (byte b : usernameBytes) { //username - variable length
            bytes.add(b);
        }

        byte[] passwordBytes = password.getBytes();
        bytes.add((byte) passwordBytes.length); //password - length
        for (byte b : passwordBytes) {  //password - variable length
            bytes.add(b);
        }

        byte[] authRequest = new byte[bytes.size()]; //translate byte list into byte array
        for (int i = 0; i < bytes.size(); i++) {
            authRequest[i] = bytes.get(i);
        }

        out.write(authRequest);
        out.flush();

        response = new byte[2];
        in.readFully(response);
        if (response[1] != (byte) 0x00) { //Server response: first byte - version number (omitted), second byte - status code (0x00 - success)
            throw new AuthorizationFailedException();
        }

        byte[] connectionRequest = new byte[10];
        connectionRequest[0] = (byte) 0x05; //SOCKS version number
        connectionRequest[1] = (byte) 0x01; //command code 0x01 TCP/IP stream, 0x02 TCP/IP port binding, 0x03 UDP port
        connectionRequest[2] = (byte) 0x00; //reserved, must be 0x00
        connectionRequest[3] = (byte) 0x01; //address type: 0x01 IPv4, 0x03 Domain name, 0x04 IPv6
        //destination ip address section
        connectionRequest[4] = destIpBytes[0];
        connectionRequest[5] = destIpBytes[1];
        connectionRequest[6] = destIpBytes[2];
        connectionRequest[7] = destIpBytes[3];
        //destination port number address section
        connectionRequest[8] = destPortBytes[0];
        connectionRequest[9] = destPortBytes[1];

        out.write(connectionRequest);
        out.flush();

        response = new byte[10];
        in.readFully(response);

        if (response[1] != (byte) 0x00) {
            throw new RequestFailedException();
        }

        return socket;
    }

    public class IncorrectIpAddressException extends Exception {
        public IncorrectIpAddressException() {
            super("Proper IP address syntax - x.x.x.x");
        }
    }

    public class SocksNotSupportedByServerException extends Exception {
        public SocksNotSupportedByServerException() {
            super("SOCKS v5 is not supported by proxy server");
        }
    }

    public class AuthorizationFailedException extends Exception {
        public AuthorizationFailedException() {
            super("Proxy server authorization failed");
        }
    }

    public class RequestFailedException extends Exception {
        public RequestFailedException() {
            super("Proxy server request failed");
        }
    }

}
