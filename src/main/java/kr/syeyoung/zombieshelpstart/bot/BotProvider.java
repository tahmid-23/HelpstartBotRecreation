package kr.syeyoung.zombieshelpstart.bot;

import kr.syeyoung.zombieshelpstart.discord.DiscordBot;

import javax.security.auth.login.LoginException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class BotProvider {
    private final List<Bot> existing = new ArrayList<>();
    private final List<Bot> available = new ArrayList<>();
    private final List<String> bots = new ArrayList<>();
    private final DiscordBot discordBot;

    {
        DiscordBot temp;
        try {
            temp = new DiscordBot();
        } catch (LoginException | InterruptedException e) {
            e.printStackTrace();
            System.err.println("Discord Bot did not load!");
            temp = null;
        }

        discordBot = temp;
    }

    private static final BotProvider botProvider = new BotProvider();

    private BotProvider() {
    }

    public synchronized void addBot(Bot b) {
        if (!b.isDisconnected()) {
            int previous = this.existing.size();
            if (!this.existing.contains(b)) {
                this.existing.add(b);
                this.bots.add(b.getName().toLowerCase());
            }

            if (!this.available.contains(b)) {
                this.available.add(b);
            }

            if (previous == 2 && this.existing.size() == 3) {
                this.discordBot.notifyThirdBot();
            }

            this.notifyAll();
        }
    }

    public synchronized List<String> getAvailableBotsList() {
        return this.available.stream().map(Bot::getName).collect(Collectors.toList());
    }

    public synchronized void removeBot(Bot b) {
        this.available.remove(b);
        this.existing.remove(b);
        this.bots.remove(b.getName().toLowerCase());
    }

    public synchronized List<String> getBots() {
        return this.existing.stream().map(Bot::getName).collect(Collectors.toList());
    }

    public synchronized List<Bot> provide(int size) {
        List<Bot> b = new ArrayList<>(this.available.subList(0, size));
        this.available.removeAll(b);
        return b;
    }

    public synchronized void returnBot(List<Bot> bots) {

        for (Bot b : bots) {
            if (!b.isDisconnected() && this.available.stream().noneMatch((b2) -> b2.getName().equalsIgnoreCase(b.getName()))) {
                this.available.add(b);
            }
        }

        this.notifyAll();
    }

    public synchronized void kickAll(String nickname) {
        for (Bot b : this.existing) {
            if (b.getName().equalsIgnoreCase(nickname)) {
                try {
                    b.disconnect("same username joined");
                } catch (Exception ignored) {}
            }
        }

    }

    public int getAvailableBots() {
        return this.available.size();
    }

    public static BotProvider getInstance() {
        return botProvider;
    }
}
