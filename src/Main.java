import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

/**
 * Created by jpelczar on 19.02.16.
 */
public class Main {

    public static void main(String[] args) {
        try {
            //It is only example data
            Socks5SocketFactory socketFactory = new Socks5SocketFactory("127.0.0.1", (short) 1080, "4.31.198.44", (short) 80, "user", "pass");
            Socket socket = socketFactory.createSocket();
            System.out.println("Socket is connected: " + socket.isConnected());

            PrintWriter writer = new PrintWriter(socket.getOutputStream());
            writer.println("GET / HTTP/1.1");
            writer.println("Host: ietf.org");
            writer.println();
            writer.flush();
            BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            String t;
            while ((t = br.readLine()) != null) System.out.println(t);
            br.close();


        } catch (IOException | Socks5SocketFactory.IncorrectIpAddressException | Socks5SocketFactory.AuthorizationFailedException | Socks5SocketFactory.RequestFailedException | Socks5SocketFactory.SocksNotSupportedByServerException e) {
            e.printStackTrace();
        }
    }
}
