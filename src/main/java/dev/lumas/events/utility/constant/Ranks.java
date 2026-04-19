package dev.lumas.events.utility.constant;

import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public enum Ranks {
    ORIGIN(0, CostGroup.A, "group.default"),
    GENESIS(3_000, CostGroup.A),
    QUANTUM(10_000, CostGroup.A),
    NEBULA(30_000, CostGroup.A),
    INTERLUDE(100_000, CostGroup.A),
    REVELATION(300_000, CostGroup.A),
    STELLARIS(500_000, CostGroup.A),
    COSMOS(750_000, CostGroup.A),
    SERENE(2_000_000, CostGroup.B),
    SINGULARITY(5_000_000, CostGroup.B),
    EPIPHANY(15_000_000, CostGroup.B),
    ODYSSEY(35_000_000, CostGroup.B),
    ECLIPSED(70_000_000, CostGroup.B),
    MAVEN(90_000_000, CostGroup.B);

    private final int cost;
    private final CostGroup costGroup;
    private final String permission;

    Ranks(int cost, CostGroup costGroup) {
        this.cost = cost;
        this.costGroup = costGroup;
        this.permission = "group." + this.name().toLowerCase();
    }

    Ranks(int cost, CostGroup costGroup, String permission) {
        this.cost = cost;
        this.costGroup = costGroup;
        this.permission = permission;
    }

    public double getPaleSideEntryCost() {
        return costGroup.getEntranceCost();
    }

    public double getPaleSideLifeCost() {
        return costGroup.getReplayCost();
    }

    public double getLegacyPaleSideEntryCost() {
        return cost * 0.1;
    }

    public double getLegacyPaleSideLifeCost() {
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
