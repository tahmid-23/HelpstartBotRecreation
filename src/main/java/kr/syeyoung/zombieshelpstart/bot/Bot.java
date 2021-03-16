package kr.syeyoung.zombieshelpstart.bot;

import kr.syeyoung.zombieshelpstart.helpstart.GameDifficulty;

public interface Bot {
    String getName();

    void sendMessage(String paramString);

    void addListener(ChatListener paramChatListener);

    void removeListener(ChatListener paramChatListener);

    void selectDifficulty(GameDifficulty paramGameDifficulty);

    void tryOpenChest(int paramInt1, int paramInt2, int paramInt3);

    boolean isDisconnected();

    void disconnect(String paramString);
}
