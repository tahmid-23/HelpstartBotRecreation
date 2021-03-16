package kr.syeyoung.zombieshelpstart;

import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import javax.security.auth.login.LoginException;
import kr.syeyoung.zombieshelpstart.discord.DiscordBot;
import kr.syeyoung.zombieshelpstart.websocket.HelpstartWebsocketServer;
import org.java_websocket.WebSocket;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.exceptions.InvalidDataException;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.handshake.ServerHandshake;
import org.java_websocket.server.WebSocketServer;

public class Main {
    public static void main(String[] args) throws LoginException, InterruptedException, URISyntaxException {
        String host = "0.0.0.0";
        int port = 25560;
        WebSocketServer server = new HelpstartWebsocketServer(new InetSocketAddress(host, port));
        server.start();
    }
}
