package kr.syeyoung.zombieshelpstart.discord;

import com.beust.jcommander.Parameter;
import java.util.List;
import kr.syeyoung.zombieshelpstart.discord.converter.DifficultyConverter;
import kr.syeyoung.zombieshelpstart.discord.converter.MapConverter;
import kr.syeyoung.zombieshelpstart.discord.converter.PlayerValidator;
import kr.syeyoung.zombieshelpstart.helpstart.GameDifficulty;
import kr.syeyoung.zombieshelpstart.helpstart.GameMap;

public class HelpstartArgument {
    @Parameter(
            names = {"-map"},
            description = "name of map",
            converter = MapConverter.class,
            required = true
    )
    public GameMap map;
    @Parameter(
            names = {"-difficulty"},
            description = "name of map",
            converter = DifficultyConverter.class
    )
    public GameDifficulty gameDifficulty;
    @Parameter(
            names = {"-players"},
            description = "names of player",
            validateValueWith = {PlayerValidator.class},
            variableArity = true
    )
    public List<String> players;
    @Parameter(
            names = {"-badchests"},
            description = "list of bad chests",
            variableArity = true
    )
    public List<String> badchests;
    @Parameter(
            names = {"-goodchests"},
            description = "list of good chests",
            variableArity = true
    )
    public List<String> goodchests;

    public HelpstartArgument() {
        this.gameDifficulty = GameDifficulty.NORMAL;
    }
}
