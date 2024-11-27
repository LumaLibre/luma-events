package dev.jsinco.luma.config;

import eu.okaeri.configs.OkaeriConfig;
import eu.okaeri.configs.annotation.Comment;
import org.bukkit.Material;

import java.util.LinkedHashMap;
import java.util.Map;

public class Config extends OkaeriConfig {

    @Comment("Materials mapped to the amount of points they give when mined.")
    private Map<Material, Integer> blockValues = Map.of(
            Material.DIAMOND_BLOCK, 2,
            Material.EMERALD_BLOCK, 1,
            Material.GOLD_BLOCK, 1
    );

    @Comment("The mine name and the cost to upgrade from it.")
    private Map<String, Integer> mines = new LinkedHashMap<>(Map.of("A", 2500, "B", 5000, "C", 10000));

    @Comment("The prefix for all messages.")
    private String prefix = "<b><#EFAC42>E<#D19941>v<#B38540>e<#95723F>n<#775E3E>t</b> <dark_gray>»<white> ";

    public Map<Material, Integer> getBlockValues() {
        return this.blockValues;
    }


    public Map<String, Integer> getMines() {
        return this.mines;
    }

    public String getPrefix() {
        return this.prefix;
    }
}
