package kr.syeyoung.zombieshelpstart.helpstart;

import kr.syeyoung.zombieshelpstart.bot.Bot;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class State {

    private final List<String> players;

    private final List<Bot> bots;

    private final GameMap map;

    private final HelpstartRequest request;

    private final GameDifficulty difficulty;

    private boolean bugged = false;

    private final List<String> invited = new ArrayList<>();

    private int invCount;

    private int accepts;

    private String chest;

}
