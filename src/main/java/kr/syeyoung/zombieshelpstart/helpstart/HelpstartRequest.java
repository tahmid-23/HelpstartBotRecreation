package kr.syeyoung.zombieshelpstart.helpstart;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;

@Data
@AllArgsConstructor
@Builder
public class HelpstartRequest {
    private GameDifficulty gameDifficulty;
    private GameMap gameMap;
    private TextChannel channel;
    private Member requester;
    private List<String> usernames;
    private boolean bad;
    private List<String> chests;
    private List<Message> messages;
    private HelpstartSession session;
    private boolean canceled;
}