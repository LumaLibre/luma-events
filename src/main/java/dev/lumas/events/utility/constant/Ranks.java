package dev.lumas.events.utility.constant;

import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public enum Ranks {
    ORIGIN(0, "group.default"),
    GENESIS(3_000),
    QUANTUM(10_000),
    NEBULA(30_000),
    INTERLUDE(100_000),
    REVELATION(300_000),
    STELLARIS(500_000),
    COSMOS(750_000),
    SERENE(2_000_000),
    SINGULARITY(5_000_000),
    EPIPHANY(15_000_000),
    ODYSSEY(35_000_000),
    ECLIPSED(70_000_000),
    MAVEN(90_000_000);

    private final int cost;
    private final String permission;

    Ranks(int cost) {
        this.cost = cost;
        this.permission = "group." + this.name().toLowerCase();
    }

    Ranks(int cost, String permission) {
        this.cost = cost;
        this.permission = permission;
    }

    public double getPaleSideEntryCost() {
        return cost * 0.10;
    }

    public double getPaleSideLifeCost() {
        return cost * 0.01;
    }

    public static Ranks getRank(Player player) {
        List<Ranks> ranks = Arrays.asList(Ranks.values());
        Collections.reverse(ranks);
        for (Ranks rank : ranks) {
            if (player.hasPermission(rank.permission)) {
                return rank;
            }
        }
        return ORIGIN;
    }
}
