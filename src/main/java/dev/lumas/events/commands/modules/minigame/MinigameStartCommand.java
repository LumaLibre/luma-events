package dev.lumas.events.commands.modules.minigame;

import dev.lumas.core.annotation.Autowire;
import dev.lumas.core.annotation.CommandMeta;
import dev.lumas.core.annotation.Register;
import dev.lumas.events.EventMain;
import dev.lumas.events.commands.CommandManager;
import dev.lumas.events.commands.CommandModule;
import dev.lumas.events.games.MinigameManager;
import dev.lumas.events.games.constants.MinigameConstant;
import dev.lumas.events.utility.Util;
import eu.okaeri.configs.OkaeriConfig;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NullMarked;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Register(Autowire.SUBCOMMAND)
@CommandMeta(
        name = "mgstart",
        permission = "lumaevents.admin",
        description = "Start a minigame",
        parent = CommandManager.class,
        usage = "/<command> mgstart <minigame> [countdown] [map] [tokens]"
)
@NullMarked
public class MinigameStartCommand implements CommandModule {

    private static final int DEFAULT_COUNTDOWN = 90;
    private static final String RANDOM_MAP = "random";
    private static final Set<String> RANDOM_MAP_ALIASES = Set.of(RANDOM_MAP, "*", "-", "any");
    private static final List<String> COUNTDOWN_SUGGESTIONS = List.of("30", "45", "60", "90", "120");
    private static final List<String> TOKEN_SUGGESTIONS = List.of("true", "false", "0.5", "1.5", "2");
    private static final Set<String> TRUTHY = Set.of("true", "yes", "y", "on");
    private static final Set<String> FALSY = Set.of("false", "no", "n", "off");
    private static final double MAX_TOKEN_MULTIPLIER = 10.0;

    @Override
    public boolean execute(EventMain eventMain, CommandSender commandSender, String s, String[] strings) {
        if (strings.length == 0) {
            return false;
        }

        MinigameConstant minigame = MinigameConstant.fromAlias(strings[0]);
        if (minigame == null) {
            Util.sendMsg(commandSender, "Invalid minigame");
            return false;
        }

        int seconds = DEFAULT_COUNTDOWN;
        if (strings.length >= 2) {
            try {
                seconds = Integer.parseInt(strings[1]);
            } catch (NumberFormatException e) {
                Util.sendMsg(commandSender, "Invalid number of seconds");
                return false;
            }
        }

        OkaeriConfig definition;
        if (strings.length >= 3 && !RANDOM_MAP_ALIASES.contains(strings[2].toLowerCase(Locale.ROOT))) {
            definition = resolveDefinition(minigame, strings[2]);
            if (definition == null) {
                Util.sendMsg(commandSender, "Invalid minigame definition: " + strings[2]);
                return false;
            }
        } else {
            definition = minigame.randomEnabledDefinition();
            if (definition == null) {
                Util.sendMsg(commandSender, "Every map of this minigame is disabled. Name one explicitly to start it anyway.");
                return false;
            }
        }

        double tokenMultiplier = 1.0;
        if (strings.length >= 4) {
            Double parsed = parseTokenMultiplier(strings[3]);
            if (parsed == null) {
                Util.sendMsg(commandSender, "Invalid tokens value: " + strings[3]
                        + " (use true, false, or a multiplier between 0 and " + formatMultiplier(MAX_TOKEN_MULTIPLIER) + ")");
                return false;
            }
            tokenMultiplier = parsed;
        }

        if (MinigameManager.getInstance().tryNewMinigameSafely(minigame, definition, true, seconds, tokenMultiplier)) {
            Util.sendMsg(commandSender, describeStart(tokenMultiplier));
        } else {
            Util.sendMsg(commandSender, "Failed to start minigame. Is there another minigame active?");
        }
        return true;
    }

    @Override
    public List<String> tabComplete(EventMain eventMain, CommandSender commandSender, String[] strings) {
        return switch (strings.length) {
            case 1 -> matching(minigameSuggestions(strings[0]), strings[0]);
            case 2 -> matching(COUNTDOWN_SUGGESTIONS, strings[1]);
            case 3 -> matching(mapSuggestions(MinigameConstant.fromAlias(strings[0])), strings[2]);
            case 4 -> matching(TOKEN_SUGGESTIONS, strings[3]);
            default -> List.of();
        };
    }

    private static List<String> minigameSuggestions(String typed) {
        if (typed.isEmpty()) {
            return Arrays.stream(MinigameConstant.values())
                    .map(minigame -> minigame.getAliases()[0])
                    .sorted()
                    .toList();
        }
        return Arrays.stream(MinigameConstant.values())
                .flatMap(minigame -> Arrays.stream(minigame.getAliases()))
                .sorted()
                .toList();
    }

    private static List<String> mapSuggestions(@Nullable MinigameConstant minigame) {
        List<String> suggestions = new ArrayList<>();
        suggestions.add(RANDOM_MAP);
        if (minigame == null) return suggestions;
        Map<String, OkaeriConfig> enabled = minigame.getEnabledDefinitions();
        enabled.keySet().stream().sorted().forEach(suggestions::add);
        minigame.<OkaeriConfig>getDefinitions().keySet().stream()
                .filter(name -> !enabled.containsKey(name))
                .sorted()
                .forEach(suggestions::add);
        return suggestions;
    }

    @Nullable
    private static OkaeriConfig resolveDefinition(MinigameConstant minigame, String name) {
        Map<String, OkaeriConfig> definitions = minigame.getDefinitions();
        OkaeriConfig exact = definitions.get(name);
        if (exact != null) return exact;
        for (Map.Entry<String, OkaeriConfig> entry : definitions.entrySet()) {
            if (entry.getKey().equalsIgnoreCase(name)) {
                return entry.getValue();
            }
        }
        return null;
    }

    @Nullable
    private static Double parseTokenMultiplier(String raw) {
        String value = raw.toLowerCase(Locale.ROOT);
        if (TRUTHY.contains(value)) return 1.0;
        if (FALSY.contains(value)) return 0.0;

        double multiplier;
        try {
            multiplier = Double.parseDouble(value);
        } catch (NumberFormatException e) {
            return null;
        }
        if (!Double.isFinite(multiplier) || multiplier < 0.0 || multiplier > MAX_TOKEN_MULTIPLIER) return null;
        return multiplier;
    }

    private static String describeStart(double tokenMultiplier) {
        if (tokenMultiplier == 1.0) return "Minigame started";
        if (tokenMultiplier == 0.0) return "Minigame started <yellow>without token payouts</yellow>";
        return "Minigame started with <yellow>" + formatMultiplier(tokenMultiplier) + "x</yellow> token payouts";
    }

    private static String formatMultiplier(double multiplier) {
        return multiplier == Math.rint(multiplier)
                ? String.valueOf((long) multiplier)
                : String.valueOf(multiplier);
    }

    private static List<String> matching(List<String> options, String typed) {
        String prefix = typed.toLowerCase(Locale.ROOT);
        return options.stream()
                .filter(option -> option.toLowerCase(Locale.ROOT).startsWith(prefix))
                .toList();
    }
}
