package kr.syeyoung.zombieshelpstart.bot.impl;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import kr.syeyoung.zombieshelpstart.bot.Bot;
import kr.syeyoung.zombieshelpstart.bot.ChatListener;
import kr.syeyoung.zombieshelpstart.helpstart.GameDifficulty;
import kr.syeyoung.zombieshelpstart.websocket.WebsocketPayloadUtil;
import org.java_websocket.WebSocket;
import org.json.JSONArray;

public class WebsocketBot implements Bot {
    private final String username;
    private final WebSocket webSocket;
    private final List<ChatListener> chatListenerList = new CopyOnWriteArrayList<>();

    public WebsocketBot(String username, WebSocket webSocket) {
        this.username = username;
        this.webSocket = webSocket;
    }

    public String getName() {
        return this.username;
    }

    public void sendMessage(String message) {
        this.webSocket.send(WebsocketPayloadUtil.createCommand("chat", message).toString());
        this.chatListenerList.forEach((c) -> c.onChat(this, "§§§ Sending... " + message));
    }

    public void tryOpenChest(int x, int y, int z) {
        this.webSocket.send(WebsocketPayloadUtil.createCommand("open", (new JSONArray()).put(x).put(y).put(z)).toString());
        this.chatListenerList.forEach((c) -> c.onChat(this, "§§§ opening chest... " + x + "," + y + "," + z));
    }

    public boolean isDisconnected() {
        return this.webSocket.isClosed() || this.webSocket.isClosing();
    }

    public void disconnect(String reason) {
        this.webSocket.close(3000, reason);
    }

    public void addListener(ChatListener listener) {
        this.chatListenerList.add(listener);
    }

    public void removeListener(ChatListener listener) {
        this.chatListenerList.remove(listener);
    }

    public void onChat(String chat) {
        this.chatListenerList.forEach((t) -> t.onChat(this, chat));
    }

    public void selectDifficulty(GameDifficulty difficulty) {
        this.webSocket.send(WebsocketPayloadUtil.createCommand("difficulty", difficulty.name()).toString());
        this.chatListenerList.forEach((c) -> c.onChat(this, "§§§ changing difficulty... " + difficulty.name()));
    }

    public boolean equals(Object obj) {
        return obj instanceof WebsocketBot && ((WebsocketBot)obj).getName().equals(this.getName());
    }

    public int hashCode() {
        return this.getName().hashCode();
    }
}