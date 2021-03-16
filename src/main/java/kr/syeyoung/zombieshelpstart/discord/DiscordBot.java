package kr.syeyoung.zombieshelpstart.discord;

import com.beust.jcommander.JCommander;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import javax.security.auth.login.LoginException;
import kr.syeyoung.zombieshelpstart.bot.BotProvider;
import kr.syeyoung.zombieshelpstart.helpstart.GameMap;
import kr.syeyoung.zombieshelpstart.helpstart.HelpstartExecutor;
import kr.syeyoung.zombieshelpstart.helpstart.HelpstartRequest;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.ReadyEvent;
import net.dv8tion.jda.api.events.message.MessageBulkDeleteEvent;
import net.dv8tion.jda.api.events.message.MessageDeleteEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.ChunkingFilter;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import org.jetbrains.annotations.NotNull;

public class DiscordBot extends ListenerAdapter {
    private final JDA jda;
    private static final List<Long> allowedChannels = Arrays.asList(344145510132744192L, 746168787073499176L, 816010112098762832L);
    private static final Map<Long, HelpstartRequest> requests = new HashMap<>();
    private static final Map<Long, String> bans_discord = new HashMap<Long, String>() {

    };
    private static final Map<String, String> bans_mc = new HashMap<String, String>() {
        {
            {
                put("choco_chungus", "punishment");
                put("curtenfulchungus", "punishment");
            }
        }
    };
    private Set<Role> blacklistRoles = new HashSet<>();

    public DiscordBot() throws LoginException, InterruptedException {
        this.jda = JDABuilder
                .createDefault(System.getenv("TOKEN_OR_HOWEVER_YOU_WANT_TO_DO_THIS_PART"))
                .enableIntents(GatewayIntent.GUILD_MEMBERS)
                .setMemberCachePolicy(MemberCachePolicy.ALL)
                .setChunkingFilter(ChunkingFilter.ALL)
                .setStatus(OnlineStatus.DO_NOT_DISTURB)
                .addEventListeners(this)
                .setActivity(Activity.watching("Bots"))
                .build();
        this.jda.awaitReady();
    }

    @Override
    public void onReady(@Nonnull ReadyEvent event) {
    }

    public void onMessageReceived(@Nonnull MessageReceivedEvent event) {
        if (allowedChannels.contains(event.getChannel().getIdLong())) {
            if (this.blacklistRoles == null) {
                this.blacklistRoles = this.jda.getGuildById("344145105868947457").getRoles().stream().filter((r) -> r.getName().toLowerCase().contains("blacklist")).collect(Collectors.toSet());
            }

            Member u;
            if (event.getMessage().getContentRaw().equals("!reloadRoles")) {
                u = event.getMember();
                if (!u.getRoles().contains(u.getGuild().getRoleById("361187594324934657")) && u.getIdLong() != 332836587576492033L) {
                    event.getTextChannel().sendMessage("Est-ce que vous avez une rôle Staff ou vous êtes un developer du bot?").queue((msg) -> event.getTextChannel().deleteMessages(Arrays.asList(msg, event.getMessage())).queueAfter(1L, TimeUnit.MINUTES));
                } else {
                    this.blacklistRoles = this.jda.getGuildById("344145105868947457").getRoles().stream().filter((r) -> r.getName().toLowerCase().contains("blacklist")).collect(Collectors.toSet());
                    event.getTextChannel().sendMessage("Reloaded blacklist role list :: " + this.blacklistRoles.stream().map(Role::getName).collect(Collectors.joining(", "))).queue((msg) -> event.getTextChannel().deleteMessages(Arrays.asList(msg, event.getMessage())).queueAfter(1L, TimeUnit.MINUTES));
                }
            }

            String content, command = event.getMessage().getContentRaw();
            if (command.equalsIgnoreCase("!botinfo") || command.equalsIgnoreCase("!bots") || command.equalsIgnoreCase("!bi") || command.equalsIgnoreCase("!botingo")) {
                content = String.join(", ", BotProvider.getInstance().getBots()).replaceAll("_", "\\\\_");
                String available = String.join(", ", BotProvider.getInstance().getAvailableBotsList()).replaceAll("_", "\\\\_");
                String queue = String.join(", ", HelpstartExecutor.getInstance().getRequests()).replaceAll("_", "\\\\_");
                event.getTextChannel().sendMessage("connected to server: " + content + "\navailable for use " + available + "\n requests: " + queue).queue((msg) -> event.getTextChannel().deleteMessages(Arrays.asList(msg, event.getMessage())).queueAfter(15L, TimeUnit.SECONDS));
            } else {
                if (event.getMessage().getContentRaw().equals("!cancelAll")) {
                    u = event.getMember();
                    if (!u.getRoles().contains(u.getGuild().getRoleById("361187594324934657")) && u.getIdLong() != 332836587576492033L) {
                        event.getTextChannel().sendMessage("Est-ce que vous avez une rôle Staff ou vous êtes un developer du bot?").queue((msg) -> event.getTextChannel().deleteMessages(Arrays.asList(msg, event.getMessage())).queueAfter(1L, TimeUnit.MINUTES));
                    } else {
                        HelpstartExecutor.getInstance().cancelAll();
                        event.getTextChannel().sendMessage("Allo requestos haveo beeno canceloedo, @herre").queue((msg) -> event.getTextChannel().deleteMessages(Arrays.asList(msg, event.getMessage())).queueAfter(1L, TimeUnit.MINUTES));
                    }
                }

                if (allowedChannels.contains(event.getChannel().getIdLong())) {
                    content = event.getMessage().getContentRaw();
                    Member author = Objects.requireNonNull(event.getMember());
                    if (!content.startsWith("!helpstart")) {
                        return;
                    }

                    if (bans_discord.containsKey(author.getIdLong())) {
                        event.getTextChannel().sendMessage("You're banned from using the bot for reason: " + bans_discord.get(author.getIdLong())).queue((msg) -> event.getTextChannel().deleteMessages(Arrays.asList(msg, event.getMessage())).queueAfter(5L, TimeUnit.SECONDS));
                        return;
                    }

                    if (author.getRoles().stream().anyMatch(this.blacklistRoles::contains)) {
                        event.getTextChannel().sendMessage("You're banned from using the bot for reason: you have been blacklisted from the bot by the moderators of Zombies League").queue((msg) -> event.getTextChannel().deleteMessages(Arrays.asList(msg, event.getMessage())).queueAfter(5L, TimeUnit.SECONDS));
                        return;
                    }

                    String[] params = content.split(" ");
                    String[] realParams = new String[params.length - 1];
                    System.arraycopy(params, 1, realParams, 0, realParams.length);
                    HelpstartArgument hsa = new HelpstartArgument();

                    try {
                        JCommander.newBuilder().addObject(hsa).build().parse(realParams);
                    } catch (Exception var12) {
                        event.getTextChannel().sendMessage(author.getAsMention() + ", " + var12.getMessage() + "\n\n**» How to use the command:**\n\n`!helpstart <-map <de/bb/aa>> [-difficulty <normal/hard/rip>] [-players [IGN_1] [IGN_2] [IGN_3]] [-badchests/-goodchests location_1 [location_2] [location_3] [...]]`\n<argument> = Required\n[argument] = Optional\nIGN = In Game Name, the people that will play in the game 0 (to start a game based on your IGN) or 1 is Solo, 2 names for a Duo and 3 for a Trio)\n\nExamples:\n➦ `!helpstart -map de -difficulty hard -players syeyoung`\n➦ `!helpstart -map de -players syeyoung -goodchests power`\n➦ `!helpstart -map de -difficulty rip` (starts a de rip solo for yourself according to your nickname with gallery chest set by default as bad chest)\n➦ `!helpstart -map aa -difficulty normal -players syeyoung Antek Antimony`\n➦ `!helpstart -map bb -difficulty rip -players syeyoung Antek -badchests mansion library dungeon`\n\n\nI did everything well, why doesn't it work? -> https://pastebin.com/AJX7cQLf\n*System coded by syeyoung and accounts provided by the community and Antek__.*").queue((msg) -> event.getTextChannel().deleteMessages(Arrays.asList(msg, event.getMessage())).queueAfter(60L, TimeUnit.SECONDS));
                        return;
                    }

                    boolean badchests = hsa.badchests != null;
                    boolean goodchests = hsa.goodchests != null;
                    if (badchests && goodchests) {
                        event.getTextChannel().sendMessage(author.getAsMention() + " Please only specify badchest or only specify goodchest option").queue((msg) -> event.getTextChannel().deleteMessages(Arrays.asList(msg, event.getMessage())).queueAfter(10L, TimeUnit.SECONDS));
                        return;
                    }
                    if (!badchests && !goodchests && hsa.map == GameMap.DEAD_END) {
                        badchests = true;
                        hsa.badchests = Collections.singletonList("gallery");
                    }

                    String str;
                    if ((str = this.validateChest(hsa.badchests, hsa.map)) != null || (str = this.validateChest(hsa.goodchests, hsa.map)) != null) {
                        event.getTextChannel().sendMessage(author.getAsMention() + ", " + str + " is not valid chest for " + hsa.map + ".\nValid chest locations: " + String.join(", ", hsa.map.getChests())).queue((msg) -> event.getTextChannel().deleteMessages(Arrays.asList(msg, event.getMessage())).queueAfter(10L, TimeUnit.SECONDS));
                        return;
                    }

                    if (!hsa.map.getAllowedDifficulties().contains(hsa.gameDifficulty)) {
                        event.getTextChannel().sendMessage(author.getAsMention() + ", " + hsa.gameDifficulty + " is not valid difficulty for " + hsa.map + ".\nValid difficulties: " + hsa.map.getAllowedDifficulties().stream().map(Enum::name).collect(Collectors.joining(", "))).queue((msg) -> event.getTextChannel().deleteMessages(Arrays.asList(msg, event.getMessage())).queueAfter(10L, TimeUnit.SECONDS));
                        return;
                    }

                    if (hsa.players == null) {
                        u = event.getMember();
                        hsa.players = Collections.singletonList(u.getEffectiveName().replaceAll("\\W", ""));
                    }
                    for (String pl : hsa.players) {
                        if (bans_mc.containsKey(pl.toLowerCase())) {
                            event.getTextChannel().sendMessage("The player " + pl + " is banned from using the bot for reason:" + bans_mc.get(pl.toLowerCase())).queue((msg) -> event.getTextChannel().deleteMessages(Arrays.asList(msg, event.getMessage())).queueAfter(5L, TimeUnit.SECONDS));
                            return;
                        }

                        if (this.blacklistRoles.size() != 0 && event.getGuild().getMembersWithRoles(this.blacklistRoles).stream().anyMatch((m) -> m.getEffectiveName().toLowerCase().contains(pl.toLowerCase()))) {
                            event.getTextChannel().sendMessage("The player " + pl + " is banned from using the bot for reason: " + pl + " has been blacklisted from the bot by the moderators of Zombies League").queue((msg) -> event.getTextChannel().deleteMessages(Arrays.asList(msg, event.getMessage())).queueAfter(5L, TimeUnit.SECONDS));
                            return;
                        }
                    }

                    boolean finalBadchests = badchests;
                    event.getTextChannel().sendMessage(author.getAsMention() + ", your request has been added to the queue. The bot will ping you when it's going to invite you").queue((msg) -> {
                        msg.addReaction("❌").queue();
                        HelpstartRequest hr = HelpstartRequest.builder().channel(event.getTextChannel()).requester(author).gameDifficulty(hsa.gameDifficulty).gameMap(hsa.map).usernames(hsa.players).chests(goodchests ? hsa.goodchests : (finalBadchests ? hsa.badchests : new ArrayList<>())).bad(!goodchests).messages(new ArrayList<>()).build();
                        hr.getMessages().add(event.getMessage());
                        hr.getMessages().add(msg);
                        HelpstartExecutor.getInstance().addToQueue(hr);
                        requests.put(msg.getIdLong(), hr);
                    });
                }
            }
        } else if (event.getChannel().getIdLong() == 346379892331380740L || event.getChannel().getIdLong() == 819396506331119639L && event.isFromGuild()) {
            /* TextChannel textChannel = (TextChannel) event.getChannel();
            try {*/
                /* CompletableFuture<List<Message>> future = textChannel.getIterableHistory().takeAsync(1000).thenApply(list -> list.stream()
                .filter(m -> m.getAuthor().getIdLong() == event.getAuthor().getIdLong())
                .collect(Collectors.toList()));
                List<Message> messages = future.get();
                for (Message message : messages) {
                    if (message.getIdLong() != event.getMessageIdLong()) {
                        textChannel.deleteMessageById(message.getIdLong()).queue();
                    }
                }*/
                Guild guild = event.getGuild();
                //noinspection ConstantConditions
                guild.addRoleToMember(event.getAuthor().getIdLong(), guild.getRoleById(720572468997390386L)).queue();
            /*} catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }*/
        }
    }

    public void onMessageDelete(@Nonnull MessageDeleteEvent event) {
        requests.remove(event.getMessageIdLong());
    }

    public void onMessageBulkDelete(@Nonnull MessageBulkDeleteEvent event) {
        event.getMessageIds().stream().map(Long::parseLong).forEach(requests::remove);
    }

    public void onMessageReactionAdd(@Nonnull MessageReactionAddEvent event) {
        HelpstartRequest hr = requests.get(event.getMessageIdLong());
        if (hr != null) {
            if (!Objects.requireNonNull(event.getUser()).isBot() && hr.getRequester().getIdLong() != event.getUserIdLong()) {
                event.getReaction().removeReaction(event.getUser()).queue();
            } else {
                if (event.getReaction().getReactionEmote().getEmoji().equals("❌") && hr.getRequester().getIdLong() == event.getUserIdLong()) {
                    hr.setCanceled(true);
                    if (hr.getSession() != null) {
                        hr.getSession().cancel();
                    } else {
                        HelpstartExecutor.getInstance().updateHurry();
                        event.getChannel().sendMessage(hr.getRequester().getAsMention() + ", your helpstart request has been canceled").queue((m) -> {
                            hr.getMessages().add(m);
                            event.getTextChannel().deleteMessages(hr.getMessages()).queueAfter(5L, TimeUnit.SECONDS);
                        });
                        synchronized(BotProvider.getInstance()) {
                            BotProvider.getInstance().notifyAll();
                        }
                    }
                }

            }
        }
    }

    public String validateChest(List<String> chests, GameMap gameMap) {
        if (chests == null) {
            return null;
        } else {
            Iterator<String> var3 = chests.iterator();

            String str;
            do {
                if (!var3.hasNext()) {
                    return null;
                }

                str = var3.next();
            } while(gameMap.getChests().contains(str));

            return str;
        }
    }

    public void notifyThirdBot() {
        /*GuildChannel gc = this.jda.getGuildChannelById(344145510132744192L);

        if (gc instanceof TextChannel) {
            ((TextChannel) gc).sendMessage("We have 3 bots online now, free solos!").addFile(this.getClass().getResourceAsStream(""), "three_bots.png").queue();
        }*/
    }
}
