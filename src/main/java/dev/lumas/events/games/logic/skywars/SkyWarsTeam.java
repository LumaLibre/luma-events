package dev.lumas.events.games.logic.skywars;

import dev.lumas.events.EventMain;
import dev.lumas.events.utility.Util;
import lombok.AllArgsConstructor;
import lombok.Getter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Color;
import org.bukkit.DyeColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.persistence.PersistentDataType;
import org.jspecify.annotations.Nullable;

public record SkyWarsTeam(TeamColor color, Location spawn) {

    private static final NamespacedKey TEAM_HELMET_KEY = new NamespacedKey(EventMain.getInstance(), "skywars_team_helmet");

    public static boolean isTeamHelmet(@Nullable ItemStack item) {
        return item != null && Util.hasPersistentKey(item, TEAM_HELMET_KEY);
    }

    public Component displayName() {
        return Component.text(color.getDisplayName(), color.getTextColor());
    }

    public ItemStack helmet() {
        ItemStack helmet = ItemStack.of(Material.LEATHER_HELMET);
        helmet.editMeta(LeatherArmorMeta.class, meta -> {
            meta.setColor(color.getArmorColor());
            meta.displayName(displayName().append(Component.text(" Team")).decoration(TextDecoration.ITALIC, false));
            meta.addEnchant(Enchantment.BINDING_CURSE, 1, true);
            meta.addEnchant(Enchantment.VANISHING_CURSE, 1, true);
            meta.setEnchantmentGlintOverride(false);
            meta.setUnbreakable(true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_UNBREAKABLE, ItemFlag.HIDE_DYE, ItemFlag.HIDE_ATTRIBUTES);
            meta.getPersistentDataContainer().set(TEAM_HELMET_KEY, PersistentDataType.BOOLEAN, true);
        });
        return helmet;
    }

    @Getter
    @AllArgsConstructor
    public enum TeamColor {
        RED("Red", NamedTextColor.RED, DyeColor.RED.getColor()),
        BLUE("Blue", NamedTextColor.BLUE, DyeColor.BLUE.getColor()),
        LIME("Lime", NamedTextColor.GREEN, DyeColor.LIME.getColor()),
        YELLOW("Yellow", NamedTextColor.YELLOW, DyeColor.YELLOW.getColor()),
        ORANGE("Orange", NamedTextColor.GOLD, DyeColor.ORANGE.getColor()),
        AQUA("Aqua", NamedTextColor.AQUA, DyeColor.LIGHT_BLUE.getColor()),
        PINK("Pink", NamedTextColor.LIGHT_PURPLE, DyeColor.PINK.getColor()),
        PURPLE("Purple", NamedTextColor.DARK_PURPLE, DyeColor.PURPLE.getColor()),
        CYAN("Cyan", NamedTextColor.DARK_AQUA, DyeColor.CYAN.getColor()),
        GREEN("Green", NamedTextColor.DARK_GREEN, DyeColor.GREEN.getColor()),
        WHITE("White", NamedTextColor.WHITE, DyeColor.WHITE.getColor()),
        GRAY("Gray", NamedTextColor.GRAY, DyeColor.LIGHT_GRAY.getColor()),
        BROWN("Brown", TextColor.color(0xA0673D), DyeColor.BROWN.getColor()),
        MAROON("Maroon", NamedTextColor.DARK_RED, Color.fromRGB(0x6B1419)),
        NAVY("Navy", NamedTextColor.DARK_BLUE, Color.fromRGB(0x1C2459)),
        BLACK("Black", TextColor.color(0x362D24), DyeColor.BLACK.getColor());

        private final String displayName;
        private final TextColor textColor;
        private final Color armorColor;
    }
}
