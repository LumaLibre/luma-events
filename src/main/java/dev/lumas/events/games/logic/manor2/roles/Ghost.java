package dev.lumas.events.games.logic.manor2.roles;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.google.common.base.Preconditions;
import dev.lumas.events.EventMain;
import dev.lumas.events.games.logic.manor2.Manor2;
import dev.lumas.events.games.logic.manor2.ManorTeam;
import dev.lumas.events.games.logic.manor2.light.GhostVisibility;
import dev.lumas.events.games.logic.manor2.light.LightHit;
import dev.lumas.events.model.EventPlayer;
import dev.lumas.events.utility.Util;
import dev.lumas.glowapi.model.GlowColorManager;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.FoodProperties;
import io.papermc.paper.datacomponent.item.ResolvableProfile;
import io.papermc.paper.datacomponent.item.UseCooldown;
import lombok.Getter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.tag.resolver.Formatter;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ItemType;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public class Ghost extends Manor2Role {

    private static final int WHIRL_INTERVAL_TICKS = 80;
    private static final float WHIRL_VOLUME = 0.6f;

    private static final int GHOST_SPEED_BUFF_DELAY = 200;
    private static final PotionEffect NIGHT_VISION = new PotionEffect(PotionEffectType.NIGHT_VISION, 220, 0, true, false, false);
    private static final PotionEffect GHOST_SPEED_BUFF = new PotionEffect(PotionEffectType.SPEED, 30, 0, true, false, false);
    private static final NamespacedKey GHOST_SPEED = Preconditions.checkNotNull(NamespacedKey.fromString("manor2/ghost_speed", EventMain.getInstance()));
    private static final AttributeModifier GHOST_SPEED_MODIFIER = new AttributeModifier(GHOST_SPEED, -0.10, AttributeModifier.Operation.MULTIPLY_SCALAR_1);
    private static final NamespacedKey COOKIE_COOLDOWN = Preconditions.checkNotNull(NamespacedKey.fromString("manor2/cookie_cooldown", EventMain.getInstance()));
    private static final String COOKIE = "cookie";
    private static final Component COOKIE_DISPLAY_NAME = Util.color("<gold>Eat for temporary speed boost");
    private static final String COOKIE_SPEED_BUFF_DELAY = "<red>Wait <delay_seconds> seconds until you can eat more";

    @Getter
    private final GhostVisibility visibility;
    private final Manor2 manor2;
    private int ticksUntilWhirl = 0;
    private volatile boolean isDead = false;
    private int cookieDelay = 0;

    public Ghost(Manor2 manor2, EventPlayer eventPlayer) {
        super(eventPlayer);
        this.visibility = new GhostVisibility(manor2, eventPlayer);
        this.manor2 = manor2;
    }

    @Override
    public void die() {
        if (isDead) {
            return;
        }
        isDead = true;
        if (manor2.getDefinition().isArtifactPlay()) {
            manor2.getRoleMap().swapRole(this, () -> new Artifact(manor2, eventPlayer));
        } else {
            manor2.getRoleMap().swapRole(this, () -> new Spectator(manor2, eventPlayer));
        }
    }

    @Override
    public void initiatePlay() {
        if (manor2.isStopping()) {
            return;
        }
        Player player = eventPlayer.getPlayer();
        if (player == null) {
            return;
        }
        ItemStack starterCookie = cookie();
        starterCookie.setAmount(3);
        player.getInventory().addItem(starterCookie);
        player.showTitle(ManorTeam.GHOSTS.assignmentTitle());
        GlowColorManager.getInstance().setTransientColor(player, NamedTextColor.DARK_GRAY);
        manor2.getPacketGlow().addViewer(player);
        AttributeInstance instance = player.getAttribute(Attribute.MOVEMENT_SPEED);
        if (instance != null && instance.getModifier(GHOST_SPEED) == null) {
            instance.addModifier(GHOST_SPEED_MODIFIER);
        }
    }

    @Override
    public void retire(Manor2 manor2) {
        visibility.stop();
        super.retire(manor2);
        Player player = eventPlayer.getPlayer();
        if (player == null) {
            return;
        }
        AttributeInstance instance = player.getAttribute(Attribute.MOVEMENT_SPEED);
        if (instance != null) {
            instance.removeModifier(GHOST_SPEED);
        }
    }

    @Override
    public void tick() {
        visibility.tick();
        operatePlayer(player -> {
            player.addPotionEffect(NIGHT_VISION);
            if (cookieDelay > 0) {
                cookieDelay--;
            }
        });
        whirl();
    }

    public void light(LightHit hit, UUID playerUuid) {
        visibility.light(hit, playerUuid);
        manor2.getGhostAttacks().onLit(this, hit, playerUuid);
    }

    public void rewardCookie(PlayerProfile profile) {
        operatePlayer(player -> {
            if (!manor2.getRoleMap().is(player.getUniqueId(), Ghost.class)) {
                return;
            }
            ItemStack cookie = cookie();
            cookie.setData(DataComponentTypes.ITEM_MODEL, NamespacedKey.minecraft("player_head"));
            cookie.setData(DataComponentTypes.PROFILE, ResolvableProfile.resolvableProfile(profile));
            player.getInventory().addItem(cookie);
        });
    }

    public boolean onConsumeItem(@NotNull ItemStack item) {
        Player player = eventPlayer.getPlayer();
        if (player == null) {
            return false;
        }
        if (cookieDelay > 0) {
            player.sendActionBar(Util.color(COOKIE_SPEED_BUFF_DELAY, Formatter.number("delay_seconds", Math.ceilDiv(cookieDelay, 20))));
            return false;
        }
        if (COOKIE.equals(item.getPersistentDataContainer().get(Manor2.ITEM_TAG, PersistentDataType.STRING))) {
            if (!manor2.getRoleMap().is(player.getUniqueId(), Ghost.class)) {
                return false;
            }
            player.addPotionEffect(GHOST_SPEED_BUFF);
            cookieDelay = GHOST_SPEED_BUFF_DELAY;
        }
        return true;
    }

    private ItemStack cookie() {
        ItemStack cookie = ItemType.COOKIE.createItemStack(meta -> {
            meta.getPersistentDataContainer().set(Manor2.ITEM_TAG, PersistentDataType.STRING, COOKIE);
            meta.displayName(COOKIE_DISPLAY_NAME);
            meta.lore(List.of(Util.color("<gray>Yummy!")));
        });
        cookie.setData(DataComponentTypes.FOOD, FoodProperties.food()
            .canAlwaysEat(true)
            .build()
        );
        cookie.setData(DataComponentTypes.USE_COOLDOWN, UseCooldown.useCooldown(GHOST_SPEED_BUFF_DELAY / 20F)
            .cooldownGroup(COOKIE_COOLDOWN)
            .build());
        return cookie;
    }

    private void whirl() {
        if (--ticksUntilWhirl > 0 || !manor2.getParticipants().contains(eventPlayer)) return;
        ticksUntilWhirl = WHIRL_INTERVAL_TICKS;

        float pitch = 0.8f + (ThreadLocalRandom.current().nextFloat() * 0.4f);
        operatePlayer(player -> player.getWorld().playSound(player.getLocation(), Sound.ENTITY_BREEZE_WHIRL, SoundCategory.HOSTILE, WHIRL_VOLUME, pitch));
    }
}
