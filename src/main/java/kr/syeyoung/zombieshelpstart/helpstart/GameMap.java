package kr.syeyoung.zombieshelpstart.helpstart;

import java.util.*;

public enum GameMap {
    DEAD_END("/play arcade_zombies_dead_end", Arrays.asList("de", "dead_end", "deadend"), Arrays.asList("office", "gallery", "apartments", "hotel", "power_station"), EnumSet.allOf(GameDifficulty.class), new Location(16, 68, 17)),
    BAD_BLOOD("/play arcade_zombies_bad_blood", Arrays.asList("bb", "bad_blood", "badblood"), Arrays.asList("mansion", "library", "dungeon", "crypts", "balcony"), EnumSet.allOf(GameDifficulty.class), new Location(21, 68, 12)),
    ALIEN_ARCADIUM("/play arcade_zombies_alien_arcadium", Arrays.asList("aa", "alien", "alien_arcadium", "alienarcadium", "arcadium"), Collections.singletonList(""), EnumSet.of(GameDifficulty.NORMAL), new Location(0, 0, 0));

    GameMap(String command, List<String> aliases, List<String> chests, EnumSet<GameDifficulty> allowedDifficulties, Location chestLoc) {
        this.command = command;
        this.aliases = aliases;
        this.chests = chests;
        this.allowedDifficulties = allowedDifficulties;
        this.chestLoc = chestLoc;
    }

    private final String command;

    private final List<String> aliases;

    private final List<String> chests;

    private final EnumSet<GameDifficulty> allowedDifficulties;

    private final Location chestLoc;

    private static final Map<String, GameMap> map;

    public String getCommand() {
        return this.command;
    }

    public List<String> getAliases() {
        return this.aliases;
    }

    public List<String> getChests() {
        return this.chests;
    }

    public EnumSet<GameDifficulty> getAllowedDifficulties() {
        return this.allowedDifficulties;
    }

    public Location getChestLoc() {
        return this.chestLoc;
    }

    static {
        map = new HashMap<>();
        for (GameMap gameMap : values()) {
            for (String aliases : gameMap.aliases)
                map.put(aliases, gameMap);
        }
    }

    public static GameMap getGameMap(String arg) {
        return map.get(arg.toLowerCase());
    }
}