package dev.lumas.events.configurable.sectors.skywars;

import dev.lumas.events.configurable.MaterialCount;
import eu.okaeri.configs.OkaeriConfig;
import eu.okaeri.configs.annotation.Comment;
import lombok.Getter;
import org.bukkit.Material;

import java.util.List;
import java.util.Map;

import static dev.lumas.events.configurable.MaterialCount.of;
import static java.util.Map.entry;

// TODO: move to a new file? // move each game to it's own file?
@Getter
public class SkyWarsLoot extends OkaeriConfig {

    @Comment("Copper chests. Basic gear; every chest guarantees building blocks and a weapon.")
    private LootPool common = new LootPool(
            1,
            Map.ofEntries(
                    entry("blocks", Map.ofEntries(
                            entry("cobblestone", new LootPackage(10, of(Material.COBBLESTONE, 32))),
                            entry("planks", new LootPackage(10, of(Material.OAK_PLANKS, 32))),
                            entry("stone_bricks", new LootPackage(6, of(Material.STONE_BRICKS, 24))),
                            entry("wool", new LootPackage(6, of(Material.WHITE_WOOL, 24)))
                    )),
                    entry("weapon", Map.ofEntries(
                            entry("stone_sword", new LootPackage(10, of(Material.STONE_SWORD))),
                            entry("copper_sword", new LootPackage(8, of(Material.COPPER_SWORD))),
                            entry("stone_axe", new LootPackage(5, of(Material.STONE_AXE))),
                            entry("copper_axe", new LootPackage(4, of(Material.COPPER_AXE))),
                            entry("stone_spear", new LootPackage(4, of(Material.STONE_SPEAR)))
                    ))
            ),
            Map.ofEntries(
                    entry("bread", new LootPackage(8, of(Material.BREAD, 6))),
                    entry("baked_potato", new LootPackage(7, of(Material.BAKED_POTATO, 8))),
                    entry("porkchop", new LootPackage(5, of(Material.COOKED_PORKCHOP, 4))),
                    entry("copper_chestplate", new LootPackage(4, of(Material.COPPER_CHESTPLATE))),
                    entry("copper_leggings", new LootPackage(5, of(Material.COPPER_LEGGINGS))),
                    entry("copper_boots", new LootPackage(6, of(Material.COPPER_BOOTS))),
                    entry("chainmail_boots", new LootPackage(3, of(Material.CHAINMAIL_BOOTS))),
                    entry("stone_pickaxe", new LootPackage(5, of(Material.STONE_PICKAXE))),
                    entry("copper_pickaxe", new LootPackage(3, of(Material.COPPER_PICKAXE))),
                    entry("snowballs", new LootPackage(6, of(Material.SNOWBALL, 16))),
                    entry("eggs", new LootPackage(5, of(Material.EGG, 16))),
                    entry("arrows", new LootPackage(3, of(Material.ARROW, 8))),
                    entry("extra_blocks", new LootPackage(6, of(Material.OAK_PLANKS, 16))),
                    entry("water", new LootPackage(3, of(Material.WATER_BUCKET))),
                    entry("fishing_rod", new LootPackage(3, of(Material.FISHING_ROD))),
                    entry("xp", new LootPackage(2, of(Material.EXPERIENCE_BOTTLE, 4)))
            ),
            3, 5
    );

    @Comment("Exposed copper chests. Iron-tier weapons, chain and iron armor, ranged and utility items.")
    private LootPool uncommon = new LootPool(
            2,
            Map.of(),
            Map.ofEntries(
                    entry("iron_sword", new LootPackage(6, of(Material.IRON_SWORD))),
                    entry("iron_axe", new LootPackage(4, of(Material.IRON_AXE))),
                    entry("iron_spear", new LootPackage(3, of(Material.IRON_SPEAR))),
                    entry("chainmail_chestplate", new LootPackage(4, of(Material.CHAINMAIL_CHESTPLATE))),
                    entry("chainmail_leggings", new LootPackage(4, of(Material.CHAINMAIL_LEGGINGS))),
                    entry("iron_leggings", new LootPackage(3, of(Material.IRON_LEGGINGS))),
                    entry("iron_boots", new LootPackage(4, of(Material.IRON_BOOTS))),
                    entry("bow", new LootPackage(5, of(Material.BOW), of(Material.ARROW, 12))),
                    entry("crossbow", new LootPackage(2, of(Material.CROSSBOW), of(Material.ARROW, 8))),
                    entry("iron_pickaxe", new LootPackage(4, of(Material.IRON_PICKAXE))),
                    entry("golden_apple", new LootPackage(4, of(Material.GOLDEN_APPLE))),
                    entry("steak", new LootPackage(6, of(Material.COOKED_BEEF, 8))),
                    entry("water", new LootPackage(4, of(Material.WATER_BUCKET))),
                    entry("lava", new LootPackage(3, of(Material.LAVA_BUCKET))),
                    entry("ender_pearl", new LootPackage(2, of(Material.ENDER_PEARL))),
                    entry("cobweb", new LootPackage(3, of(Material.COBWEB, 4))),
                    entry("wind_charge", new LootPackage(3, of(Material.WIND_CHARGE, 3))),
                    entry("snowballs", new LootPackage(4, of(Material.SNOWBALL, 16))),
                    entry("blocks", new LootPackage(5, of(Material.STONE_BRICKS, 32))),
                    entry("xp", new LootPackage(3, of(Material.EXPERIENCE_BOTTLE, 8)))
            ),
            4, 6
    );

    @Comment("Weathered copper chests. Always one piece of strong gear, plus pearls, apples and explosives.")
    private LootPool rare = new LootPool(
            3,
            Map.ofEntries(
                    entry("gear", Map.ofEntries(
                            entry("iron_sword", new LootPackage(5, of(Material.IRON_SWORD))),
                            entry("iron_chestplate", new LootPackage(6, of(Material.IRON_CHESTPLATE))),
                            entry("iron_leggings", new LootPackage(5, of(Material.IRON_LEGGINGS))),
                            entry("diamond_boots", new LootPackage(3, of(Material.DIAMOND_BOOTS))),
                            entry("diamond_sword", new LootPackage(2, of(Material.DIAMOND_SWORD)))
                    ))
            ),
            Map.ofEntries(
                    entry("golden_apples", new LootPackage(6, of(Material.GOLDEN_APPLE, 2))),
                    entry("ender_pearls", new LootPackage(4, of(Material.ENDER_PEARL, 2))),
                    entry("bow", new LootPackage(4, of(Material.BOW), of(Material.ARROW, 16))),
                    entry("crossbow", new LootPackage(3, of(Material.CROSSBOW), of(Material.ARROW, 12))),
                    entry("tnt", new LootPackage(3, of(Material.TNT, 4), of(Material.FLINT_AND_STEEL))),
                    entry("cobweb", new LootPackage(4, of(Material.COBWEB, 6))),
                    entry("lava", new LootPackage(4, of(Material.LAVA_BUCKET))),
                    entry("water", new LootPackage(3, of(Material.WATER_BUCKET))),
                    entry("wind_charges", new LootPackage(4, of(Material.WIND_CHARGE, 6))),
                    entry("diamond_pickaxe", new LootPackage(2, of(Material.DIAMOND_PICKAXE))),
                    entry("diamond_axe", new LootPackage(2, of(Material.DIAMOND_AXE))),
                    entry("trident", new LootPackage(1, of(Material.TRIDENT))),
                    entry("steak", new LootPackage(5, of(Material.COOKED_BEEF, 12))),
                    entry("obsidian", new LootPackage(2, of(Material.OBSIDIAN, 8))),
                    entry("xp", new LootPackage(4, of(Material.EXPERIENCE_BOTTLE, 12)))
            ),
            4, 6
    );

    @Comment("Oxidized copper chests. Always one piece of diamond gear, plus the rarest items in the game.")
    private LootPool epic = new LootPool(
            4,
            Map.ofEntries(
                    entry("gear", Map.ofEntries(
                            entry("diamond_sword", new LootPackage(5, of(Material.DIAMOND_SWORD))),
                            entry("diamond_chestplate", new LootPackage(5, of(Material.DIAMOND_CHESTPLATE))),
                            entry("diamond_leggings", new LootPackage(5, of(Material.DIAMOND_LEGGINGS))),
                            entry("diamond_boots", new LootPackage(5, of(Material.DIAMOND_BOOTS))),
                            entry("diamond_spear", new LootPackage(2, of(Material.DIAMOND_SPEAR))),
                            // kb resistance
                            entry("netherite_sword", new LootPackage(1, of(Material.NETHERITE_SWORD))),
                            entry("netherite_chestplate", new LootPackage(1, of(Material.NETHERITE_CHESTPLATE))),
                            entry("netherite_leggings", new LootPackage(1, of(Material.NETHERITE_LEGGINGS))),
                            entry("netherite_boots", new LootPackage(1, of(Material.NETHERITE_BOOTS)))
                    ))
            ),
            Map.ofEntries(
                    entry("golden_apples", new LootPackage(6, of(Material.GOLDEN_APPLE, 3))),
                    entry("enchanted_golden_apple", new LootPackage(1, of(Material.ENCHANTED_GOLDEN_APPLE))),
                    entry("totem", new LootPackage(1, of(Material.TOTEM_OF_UNDYING))),
                    entry("ender_pearls", new LootPackage(5, of(Material.ENDER_PEARL, 3))),
                    entry("mace", new LootPackage(1, of(Material.MACE), of(Material.WIND_CHARGE, 4))),
                    entry("wind_charges", new LootPackage(4, of(Material.WIND_CHARGE, 8))),
                    entry("bow", new LootPackage(3, of(Material.BOW), of(Material.ARROW, 24))),
                    entry("crossbow", new LootPackage(3, of(Material.CROSSBOW), of(Material.ARROW, 16))),
                    entry("tnt", new LootPackage(4, of(Material.TNT, 8), of(Material.FLINT_AND_STEEL))),
                    entry("cobweb", new LootPackage(4, of(Material.COBWEB, 8))),
                    entry("lava", new LootPackage(3, of(Material.LAVA_BUCKET))),
                    entry("diamond_axe", new LootPackage(3, of(Material.DIAMOND_AXE))),
                    entry("obsidian", new LootPackage(2, of(Material.OBSIDIAN, 12))),
                    entry("lapis", new LootPackage(3, of(Material.LAPIS_LAZULI, 12))),
                    entry("xp", new LootPackage(4, of(Material.EXPERIENCE_BOTTLE, 24)))
            ),
            5, 7
    );

    @Getter
    public static class LootPool extends OkaeriConfig {

        @Comment("Points a player earns for being the first to open one of these chests")
        private int points;
        @Comment("Categories where exactly one package is always rolled")
        private Map<String, Map<String, LootPackage>> guaranteed;
        @Comment("Weighted packages rolled randomly after the guaranteed ones")
        private Map<String, LootPackage> random;
        private int minRandomRolls;
        private int maxRandomRolls;

        public LootPool(int points, Map<String, Map<String, LootPackage>> guaranteed, Map<String, LootPackage> random, int minRandomRolls, int maxRandomRolls) {
            this.points = points;
            this.guaranteed = guaranteed;
            this.random = random;
            this.minRandomRolls = minRandomRolls;
            this.maxRandomRolls = maxRandomRolls;
        }
    }

    @Getter
    public static class LootPackage extends OkaeriConfig {

        private int weight;
        private List<MaterialCount> items;

        public LootPackage(int weight, MaterialCount... items) {
            this.weight = weight;
            this.items = List.of(items);
        }
    }
}
