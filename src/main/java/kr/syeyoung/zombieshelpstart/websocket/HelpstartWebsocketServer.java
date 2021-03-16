package kr.syeyoung.zombieshelpstart.websocket;

import java.net.InetSocketAddress;
import kr.syeyoung.zombieshelpstart.bot.BotProvider;
import kr.syeyoung.zombieshelpstart.bot.impl.WebsocketBot;
import org.java_websocket.WebSocket;
import org.java_websocket.drafts.Draft;
import org.java_websocket.exceptions.InvalidDataException;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.handshake.ServerHandshake;
import org.java_websocket.handshake.ServerHandshakeBuilder;
import org.java_websocket.server.WebSocketServer;
import org.json.JSONObject;

public class HelpstartWebsocketServer extends WebSocketServer {
    public HelpstartWebsocketServer(InetSocketAddress address) {
        super(address);
    }

    public void onOpen(WebSocket conn, ClientHandshake handshake) {
    }

    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        System.out.println("closed " + conn.getRemoteSocketAddress() + " with exit code " + code + " additional info: " + reason);
        if (conn.getAttachment() != null) {
            WebsocketBot wb = conn.getAttachment();
            BotProvider.getInstance().removeBot(wb);
            wb.onChat("§§§§§§§§§§§§§§§§§§§§DISCONNECTED");
        }
    }

    @Override
    public ServerHandshakeBuilder onWebsocketHandshakeReceivedAsServer(WebSocket conn, Draft draft, ClientHandshake request) throws InvalidDataException {
        System.out.println("handshaking");
        return super.onWebsocketHandshakeReceivedAsServer(conn, draft, request);
    }

    @Override
    public void onWebsocketHandshakeReceivedAsClient(WebSocket conn, ClientHandshake request, ServerHandshake response) throws InvalidDataException {
        System.out.println("handshaking");
        super.onWebsocketHandshakeReceivedAsClient(conn, request, response);
    }

    public void onMessage(WebSocket conn, String message) {
        JSONObject object = new JSONObject(message);
        String command = object.getString("command");
        if (command.equals("connect")) {
            if (conn.getAttachment() != null) {
                conn.close();
                return;
            }

            String username = object.getString("data");
            if (BotProvider.getInstance().getAvailableBotsList().contains(username.toLowerCase())) {
                BotProvider.getInstance().kickAll(username);
                conn.close();
                return;
            }

            WebsocketBot wb = new WebsocketBot(username, conn);
            wb.sendMessage("/p leave");
            wb.sendMessage("/lobby");
            conn.setAttachment(wb);

            try {
                Thread.sleep(5000L);
            } catch (InterruptedException var8) {
                var8.printStackTrace();
            }

            BotProvider.getInstance().addBot(wb);
        } else {
            WebsocketBot wb;
            if ("disconnect".equals(command)) {
                if (conn.getAttachment() == null) {
                    conn.close();
                    return;
                }

                wb = conn.getAttachment();
                conn.close();
                BotProvider.getInstance().removeBot(wb);
                wb.onChat("§§§§§§§§§§§§§§§§§§§§DISCONNECTED");
                conn.setAttachment(null);
            } else if ("chatReceived".equals(command)) {
                if (conn.getAttachment() == null) {
                    conn.close();
                    return;
                }

                wb = conn.getAttachment();
                wb.onChat(object.getString("data"));
            }
        }

        System.out.println("received message from " + conn.getRemoteSocketAddress() + ": " + message);
    }

    public void onError(WebSocket conn, Exception ex) {
        ex.printStackTrace();
        System.err.println("an error occurred on connection " + conn.getRemoteSocketAddress() + ":" + ex);
    }

    public void onStart() {
        System.out.println("server started successfully");
        System.out.println(getAddress().getPort());
    }
}
