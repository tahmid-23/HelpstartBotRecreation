package kr.syeyoung.zombieshelpstart.helpstart;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.IntSupplier;
import java.util.regex.Matcher;
import java.util.stream.Collectors;
import kr.syeyoung.zombieshelpstart.bot.Bot;
import kr.syeyoung.zombieshelpstart.bot.BotProvider;
import kr.syeyoung.zombieshelpstart.bot.ChatListener;
import net.dv8tion.jda.api.entities.IMentionable;

public class HelpstartSession extends CompletableFuture<Void> implements ChatListener {
    private static final ScheduledExecutorService threadPoolExecutor = Executors.newSingleThreadScheduledExecutor();
    private final List<String> playerNames;
    private List<Bot> bots;
    private volatile Stage stage;
    private volatile IntSupplier nextTask;
    private GameMap map;
    private GameDifficulty gameDifficulty;
    private final HelpstartRequest helpstartRequest;
    private final State state;
    private int tries;
    private boolean hurry = false;
    private Instant timeout;
    private Instant botGotChat;
    private final List<String> logs;
    private static final List<String> advertisements = Collections.emptyList() /*Arrays.asList("Quick Announcement:", "If you are only soloing, you don't need to specify -players, the bot will read your nickname", "Also, gallery is set to badchest by default now", "Good luck!"); /* Arrays.asList("Hope you do well!", "If you appreciate the bot and want to contribute alt accounts, go to #applications.")*/;
    private final Set<Bot> gotChat;
    private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");

    public HelpstartSession(HelpstartRequest helpstartRequest) {
        this.stage = Stage.INVITE;
        this.tries = 0;
        this.logs = new ArrayList<>();
        this.gotChat = new HashSet<>();
        this.helpstartRequest = helpstartRequest;
        this.playerNames = helpstartRequest.getUsernames().stream().map(String::toLowerCase).collect(Collectors.toList());
        if (this.playerNames.size() > 3) {
            throw new IllegalArgumentException("too many players");
        } else {
            Iterator<String> var2 = this.playerNames.iterator();

            String str;
            do {
                if (!var2.hasNext()) {
                    this.bots = BotProvider.getInstance().provide(4 - this.playerNames.size());
                    this.bots.forEach((b) -> b.addListener(this));
                    this.map = helpstartRequest.getGameMap();
                    this.gameDifficulty = helpstartRequest.getGameDifficulty();
                    this.state = new State(this.playerNames, this.bots, this.map, this.helpstartRequest, this.gameDifficulty);
                    this.timeout = Instant.now().plusSeconds(180L);
                    this.botGotChat = Instant.now().plusSeconds(9L);
                    helpstartRequest.getChannel().sendMessage(helpstartRequest.getRequester().getAsMention() + ", " + this.bots.get(0).getName() + " will invite you and your allies to the party").queue((m) -> {
                        helpstartRequest.getMessages().add(m);
                        this.inviteThemAll();
                    });
                    threadPoolExecutor.schedule(this::check, 4L, TimeUnit.SECONDS);
                    threadPoolExecutor.schedule(this::check, 60L, TimeUnit.SECONDS);
                    return;
                }

                str = var2.next();
            } while(!BotProvider.getInstance().getBots().contains(str.toLowerCase()));
            this.state = null;

            helpstartRequest.getChannel().sendMessage(helpstartRequest.getRequester().getAsMention() + ", The bot couldn't help start because one of the players is a bot now :/").queue((msg) -> {
                helpstartRequest.getMessages().add(msg);
                helpstartRequest.getChannel().deleteMessages(helpstartRequest.getMessages()).queueAfter(10L, TimeUnit.SECONDS);
            });
            this.completeExceptionally(new RuntimeException("One of the players is a bot now :/"));
        }
    }

    public int inviteThemAll() {
        System.out.println("inviting all");
        StringBuilder sb = new StringBuilder("/p invite");
        this.playerNames.forEach((s) -> {
            sb.append(" ");
            sb.append(s);
        });
        this.bots.subList(1, this.bots.size()).forEach((b) -> {
            b.sendMessage("/p leave");
            sb.append(" ");
            sb.append(b.getName());
        });
        this.bots.get(0).sendMessage(sb.toString());
        this.stage = Stage.INVITE;
        this.nextTask = this::gotoLobby;
        return 60;
    }

    public int gotoLobby() {
        System.out.println("going to lobby");
        this.bots.get(0).sendMessage("/lobby");
        this.stage = Stage.GOTO_LOBBY;
        this.nextTask = this::warpOut;
        return 10;
    }

    public int warpOut() {
        System.out.println("warping out");
        this.bots.get(0).sendMessage("/p warp");
        this.stage = Stage.WARP_OUT;
        this.nextTask = this::joinGame;
        return 10;
    }

    public int joinGame() {
        System.out.println("joining game");
        this.state.setBugged(false);
        threadPoolExecutor.schedule(() -> this.bots.get(0).sendMessage(this.map.getCommand()), 5L, TimeUnit.SECONDS);
        this.stage = Stage.JOIN_GAME;
        this.nextTask = this::warpInToMakeSure;
        return 15;
    }

    public int warpInToMakeSure() {
        System.out.println("warp in to make sure");
        if (this.state.isBugged()) {
            this.bots.get(0).sendMessage("/pchat Hey, I rejoined someone else's game. Trying to type the warp command in 5 seconds.");
            return this.joinGame();
        } else {
            this.stage = Stage.WARP_IN_TO_MAKE_SURE;
            this.bots.get(0).sendMessage("/p warp");
            if (this.gameDifficulty == GameDifficulty.NORMAL) {
                this.nextTask = this::start;
            } else {
                this.nextTask = this::changeDifficulty;
            }

            return 10;
        }
    }

    public int changeDifficulty() {
        System.out.println("changing difficulty");
        this.bots.get(0).selectDifficulty(this.gameDifficulty);
        this.stage = Stage.SELECT_DIFFICULTY;
        this.nextTask = this::start;
        return 10;
    }

    public int start() {
        System.out.println("waiting for start");
        this.stage = Stage.START;
        this.nextTask = this::checkChest;
        return 25;
    }

    public int checkChest() {
        System.out.println("checking chest");
        this.stage = Stage.CHECK_CHEST;
        if (this.helpstartRequest.getChests().size() == 0) {
            this.cleanUp();
        } else {
            this.nextTask = this::checkChest2;
            Location loc = this.helpstartRequest.getGameMap().getChestLoc();

            try {
                Thread.sleep(1000L);
            } catch (InterruptedException ignored) {}

            this.bots.get(0).tryOpenChest(loc.getX(), loc.getY(), loc.getZ());
        }

        return 20;
    }

    public void cancel() {
        this.error("Canceled by user");
    }

    public void setHurry(boolean hurry) {
        this.hurry = hurry;
    }

    public int checkChest2() {
        System.out.println("really checking chest");
        String chest = state.getChest();
        boolean contains = this.helpstartRequest.getChests().contains(chest);
        boolean bad = this.helpstartRequest.isBad();
        System.out.println("chest: " + chest + " / chests: " + String.join(", ", this.helpstartRequest.getChests()) + " / contains " + contains + " / bad " + bad + " / xored " + (contains ^ bad) + " / tries " + this.tries);
        ++this.tries;
        if (this.hurry && this.tries >= 5) {
            this.bots.get(0).sendMessage("/pchat sry, it's bad chest, but you're too unlucky getting bad chest " + tries + " times in a row.");
            this.bots.get(0).sendMessage("/pchat someone else is waiting for the bot, so I'm going to disband.");
            this.error("Bad Chest but you're too lucky getting bad chest 5 times in a row, so just play");
            return 1;
        } else {
            return bad ^ contains ? this.cleanUp() : this.gotoLobby();
        }
    }

    public int cleanUp() {
        System.out.println("cleaning up");
        this.stage = Stage.GOTO_LOBBY;
        this.nextTask = this::onDone;
        for (String advertisement : advertisements) {
            this.bots.get(0).sendMessage("/pchat " + advertisement);
        }

        for (Bot bot : this.bots) {
            try {
                bot.sendMessage("/p leave");
                bot.sendMessage("/lobby");
            } catch (Exception var4) {
                var4.printStackTrace();
            }
        }

        return 20;
    }

    public int onDone() {
        System.out.println("done");
        this.stage = Stage.DONE;
        threadPoolExecutor.schedule(() -> {
            this.bots.forEach((b) -> b.removeListener(this));
            BotProvider.getInstance().returnBot(this.bots);
        }, 5L, TimeUnit.SECONDS);

        try {
            this.helpstartRequest.getChannel().deleteMessages(this.helpstartRequest.getMessages()).queueAfter(10L, TimeUnit.SECONDS);
        } catch (Exception var2) {
            var2.printStackTrace();
        }

        this.complete(null);
        this.saveLogs(false);
        return 30;
    }

    public UUID saveLogs(boolean err) {
        UUID newUID = UUID.randomUUID();
        File parent = new File("logs/" + (err ? "errors" : "plains"));
        parent.mkdirs();

        try {
            Files.write((new File(parent, newUID.toString() + ".log")).toPath(), this.logs);
            return newUID;
        } catch (IOException var5) {
            return null;
        }
    }

    public void error(String error) {
        System.out.println("error - " + error);
        if (this.stage != Stage.DONE) {
            this.stage = Stage.DONE;

            try {
                this.bots.get(0).sendMessage("/p disband");
            } catch (Exception var6) {
                var6.printStackTrace();
            }

            for (Bot bot : this.bots) {
                try {
                    bot.sendMessage("/p leave");
                    bot.sendMessage("/lobby");
                } catch (Exception var5) {
                    var5.printStackTrace();
                }
            }

            UUID uid = this.saveLogs(true);
            this.helpstartRequest.getChannel().sendMessage(this.helpstartRequest.getRequester().getAsMention() + ", The bot couldn't help start because " + error + ".\n\nThe log id for this request was " + uid.toString() + ". If you believe the bot was bugged, ~~send syeyoung this log id~~ syeyoung doesn't have his pc and Tahmid doesn't know how to fix most bugs, so sorry about that :/").mention(new IMentionable[0]).queue((msg) -> {
                this.helpstartRequest.getMessages().add(msg);
                this.helpstartRequest.getChannel().deleteMessages(this.helpstartRequest.getMessages()).queueAfter(10L, TimeUnit.SECONDS);
            });
            // this.helpstartRequest.getChannel().getJDA().openPrivateChannelById(395764893430841363L).queue((pc) -> pc.sendMessage("Error: " + uid.toString()).queue());
            threadPoolExecutor.schedule(() -> {
                this.bots.forEach((b) -> b.removeListener(this));
                BotProvider.getInstance().returnBot(this.bots);
            }, 5L, TimeUnit.SECONDS);
            this.completeExceptionally(new RuntimeException(error));
        }
    }

    public boolean check() {
        if (this.isDone()) {
            return true;
        } else if (this.botGotChat != null && this.botGotChat.isBefore(Instant.now())) {
            System.out.print("Checking if they got chats");
            this.botGotChat = null;
            List<Bot> copy = new ArrayList<>(this.bots);
            copy.removeAll(this.gotChat);
            String error = "";
            if (copy.contains(this.bots.get(0))) {
                try {
                    this.bots.get(0).disconnect("not following directions");
                } catch (Exception ignored) {}

                this.error("Main invite bot is not sending commands or sending responses to server");
            } else {
                copy.forEach((bot) -> {
                    try {
                        bot.disconnect("not following directions bot");
                    } catch (Exception ignored) {}
                });
                error = error + "\nAt least one of the bots are not sending commands or sending responses to server - " + copy.stream().map(Bot::getName).collect(Collectors.joining(", "));
                if (!copy.isEmpty()) {
                    this.error(error);
                }

            }
            return true;
        } else if (this.timeout.isBefore(Instant.now())) {
            this.error("Timed out");
            return true;
        } else {
            return false;
        }
    }

    public void onChat(Bot b, String chat) {
        if (this.stage != null && this.stage != Stage.DONE) {
            this.logs.add(sdf.format(new Date()) + " | " + b.getName() + ": " + chat);
            this.checkExceptionalSituation(b, chat);
            if (!chat.startsWith("§§§")) {
                this.gotChat.add(b);
                if (!this.check()) {
                    try {
                        if (this.stage.isComplete(this.state, b, chat)) {
                            int len;
                            this.timeout = Instant.now().plusSeconds((len = this.nextTask.getAsInt()));
                            threadPoolExecutor.schedule(this::check, len + 5, TimeUnit.SECONDS);
                        }
                    } catch (Exception var4) {
                        this.error(var4.getMessage());
                    }

                }
            }
        }
    }

    public void checkExceptionalSituation(Bot b, String chat) {
        Matcher matcher;
        if ((matcher = Patterns.EXPIRED.matcher(chat)).matches()) {
            this.error("Party invite to " + Patterns.getRealName(matcher.group(1)) + " expired");
        } else if ((matcher = Patterns.LEFT_PARTY.matcher(chat)).matches()) {
            this.error(Patterns.getRealName(matcher.group(1)) + " left the party");
        } else if ((matcher = Patterns.LEFT_PARTY_SERVER.matcher(chat)).matches()) {
            this.error(Patterns.getRealName(matcher.group(1)) + " left the server");
        } else if ((matcher = Patterns.QUIT_GAME.matcher(chat)).matches()) {
            this.error(Patterns.getRealName(matcher.group(1)) + " quit the game");
        } else if (chat.equalsIgnoreCase("§§§§§§§§§§§§§§§§§§§§DISCONNECTED")) {
            this.error("one of the bots disconnected");
        }

    }

    public List<Bot> getBots() {
        return bots;
    }

    public interface CompleteChecker {
        boolean isComplete(State var1, Bot var2, String var3);
    }

    public enum Stage implements HelpstartSession.CompleteChecker {
        INVITE {
            public boolean isComplete(State state, Bot b, String chat) {
                List<Bot> bots = state.getBots();
                Matcher m = Patterns.INVITE_REQ.matcher(chat);
                if (m.matches()) {
                    if (Patterns.getRealName(m.group(1)).equalsIgnoreCase((bots.get(0)).getName())) {
                        b.sendMessage("/p accept " + (bots.get(0)).getName());
                        return false;
                    }

                    if (b == bots.get(0)) {
                        throw new RuntimeException("Someone named " + Patterns.getRealName(m.group(1)) + " tried to glitch the bot");
                    }
                }

                if (b != bots.get(0)) {
                    return false;
                } else {
                    m = Patterns.INVITED.matcher(chat);
                    List<String> invited = state.getInvited();
                    int invCount = state.getInvCount();
                    String realName;
                    if (m.matches()) {
                        String inviter = Patterns.getRealName(m.group(1));
                        realName = Patterns.getRealName(m.group(2));
                        invited.add(realName.toLowerCase());
                        ++invCount;
                    }

                    if (chat.equalsIgnoreCase("§cYou cannot invite that player since they have ignored you.")) {
                        ++invCount;
                    } else if (chat.equalsIgnoreCase("§cYou cannot invite that player.")) {
                        ++invCount;
                    } else if (chat.equalsIgnoreCase("§cCouldn't find a player with that name!")) {
                        ++invCount;
                    } else if (Patterns.CANT_INV.matcher(chat).matches()) {
                        ++invCount;
                    }

                    state.setInvCount(invCount);
                    if (invCount == 3) {
                        List<Bot> couldntinvbots = (new ArrayList<>(bots)).stream().filter((bot) -> !invited.contains(bot.getName().toLowerCase()) && bot != bots.get(0)).collect(Collectors.toList());
                        List<String> couldntinvplayers = (state.getPlayers()).stream().filter((s) -> !invited.contains(s)).collect(Collectors.toList());
                        if (couldntinvbots.isEmpty() && couldntinvplayers.isEmpty()) {
                            state.setInvCount(100);
                            return false;
                        } else {
                            couldntinvbots.forEach((bot) -> {
                                try {
                                    bot.disconnect("Couldn't get invited");
                                } catch (Exception ignored) {}
                            });
                            throw new RuntimeException("Couldn't invite bots - " + couldntinvbots.stream().map(Bot::getName).collect(Collectors.joining(", ")) + "\nCoudln't invite players - " + String.join(", ", couldntinvplayers));
                        }
                    } else {
                        int accepts = state.getAccepts();
                        m = Patterns.JOINED.matcher(chat);
                        if (m.matches()) {
                            System.out.println("MATCHES!");
                            realName = Patterns.getRealName(m.group(1)).toLowerCase();
                            if ((state.getPlayers()).contains(realName)) {
                                ++accepts;
                            } else {
                                String finalRealName = realName;
                                if (bots.stream().anyMatch((b2) -> b2.getName().equalsIgnoreCase(finalRealName))) {
                                    ++accepts;
                                }
                            }

                            System.out.println("MATCHES! - " + accepts);
                        }

                        state.setAccepts(accepts);
                        return accepts == 3;
                    }
                }
            }
        },
        GOTO_LOBBY {
            public boolean isComplete(State state, Bot b, String chat) {
                List<Bot> bots = state.getBots();
                if (b == bots.get(0)) {
                    if (chat.equalsIgnoreCase("§cYou are already connected to this server")) {
                        return true;
                    }

                    return chat.replaceAll("§.", "").trim().isEmpty();
                }

                return false;
            }
        },
        WARP_OUT {
            public boolean isComplete(State state, Bot b, String chat) {
                List<Bot> bots = state.getBots();
                return b == bots.get(0) && chat.equalsIgnoreCase("§eYou summoned your party of §c3 §eto your server.");
            }
        },
        JOIN_GAME {
            public boolean isComplete(State state, Bot b, String chat) {
                List<Bot> bots = state.getBots();
                if (b == bots.get(0)) {
                    if (chat.equals("§e§lTo leave Zombies, type /lobby")) {
                        state.setBugged(true);
                        return true;
                    } else {
                        return Patterns.JOINED_GAME2.matcher(chat).matches();
                    }
                } else {
                    return false;
                }
            }
        },
        WARP_IN_TO_MAKE_SURE {
            public boolean isComplete(State state, Bot b, String chat) {
                List<Bot> bots = state.getBots();
                return b == bots.get(0) && chat.equals("§eYou summoned your party of §c3 §eto your server.");
            }
        },
        SELECT_DIFFICULTY {
            public boolean isComplete(State state, Bot b, String chat) {
                Matcher m = Patterns.DIFFICULTY_SELECTED.matcher(chat);
                if (m.matches()) {
                    String difficulty = m.group(2).replaceAll("§.", "").toUpperCase();
                    if (GameDifficulty.valueOf(difficulty) == state.getDifficulty()) {
                        return true;
                    } else {
                        throw new RuntimeException("the bots selected wrong difficulty :/ pls try again");
                    }
                } else if (chat.trim().equalsIgnoreCase("§f§lZombies")) {
                    throw new RuntimeException("the bots didn't select difficulty :/ pls try again");
                } else {
                    return false;
                }
            }
        },
        START {
            public boolean isComplete(State state, Bot b, String chat) {
                return chat.trim().equalsIgnoreCase("§f§lZombies");
            }
        },
        CHECK_CHEST {
            public boolean isComplete(State state, Bot b, String chat) {
                List<Bot> bots = state.getBots();
                if (b == bots.get(0)) {
                    Matcher m = Patterns.CHEST_LOCATION.matcher(chat);
                    if (!m.matches()) {
                        return false;
                    } else {
                        String chest = m.group(1);
                        state.setChest(chest.toLowerCase().replace(" ", "_"));
                        return true;
                    }
                } else {
                    return false;
                }
            }
        },
        DONE {
            public boolean isComplete(State state, Bot b, String chat) {
                return false;
            }
        }
    }
}
