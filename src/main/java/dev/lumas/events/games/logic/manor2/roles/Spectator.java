package dev.lumas.events.games.logic.manor2.roles;

import com.google.common.base.Preconditions;
import dev.lumas.events.EventMain;
import dev.lumas.events.games.logic.manor2.Manor2;
import dev.lumas.events.games.logic.manor2.light.Visibility;
import dev.lumas.events.games.logic.manor2.roles.predicate.IsRolePredicate;
import dev.lumas.events.model.EventPlayer;
import dev.lumas.events.utility.Util;
import dev.lumas.glowapi.model.GlowColorManager;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.Consumable;
import io.papermc.paper.datacomponent.item.ItemLore;
import io.papermc.paper.datacomponent.item.ResolvableProfile;
import io.papermc.paper.persistence.PersistentDataContainerView;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.tag.resolver.Formatter;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.title.Title;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ItemType;
import org.bukkit.persistence.PersistentDataType;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Stream;

public class Spectator extends Manor2Role {

    private static final String SPECTATOR_INIT_MESSAGE = "<red>You are now a spectator";
    private static final String[] EAT_ME = {
        "Eat me!",
        "For eating.",
        "Take a <dark_red>bite"
    };
    private static final String SPECTATOR_MESSAGE = "<gray>Spectating...";
    private static final NamespacedKey SCORE_TARGET = Preconditions.checkNotNull(NamespacedKey.fromString("manor2/score_target", EventMain.getInstance()));
    private static final String PLAYER_SCORE_ITEM = "player_score_item";
    private static final int REFRESH_RATE = 20 * 10;

    private final Manor2 manor2;
    private final Visibility visibility;
    private int tick = 0;
    private volatile boolean retired = false;

    protected Spectator(Manor2 manor2, EventPlayer eventPlayer) {
        super(eventPlayer);
        this.manor2 = manor2;
        this.visibility = new Visibility(manor2, eventPlayer);
    }

    @Override
    public void tick() {
        this.operatePlayer(player -> {
            if (retired) {
                return;
            }
            visibility.setVanished(new IsRolePredicate(Manor2Role.class));
            player.sendActionBar(Util.color(SPECTATOR_MESSAGE));
            if (tick++ % REFRESH_RATE == 0) {
                List<ItemStack> scores = manor2.getScoreboard().getScores()
                    .entrySet()
                    .stream()
                    .filter(entry -> !manor2.getRoleMap().is(entry.getKey().getUuid(), Spectator.class))
                    .sorted(Comparator.<Map.Entry<EventPlayer, Integer>>comparingInt(Map.Entry::getValue).reversed())
                    .flatMap(entry -> toPlayerHead(entry.getKey(), entry.getValue()))
                    .limit(9)
                    .toList();
                Inventory inventory = player.getInventory();
                for (int i = 0; i < scores.size(); i++) {
                    inventory.setItem(i, scores.get(i));
                }
            }
        });
    }

    @Override
    public void die() {
        // NO-OP
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
        List<Location> destinations = manor2.getRoleMap().values()
            .stream()
            .filter(new IsRolePredicate(Spectator.class).negate())
            .map(Manor2Role::getEventPlayer)
            .map(EventPlayer::getPlayer)
            .filter(Objects::nonNull)
            .map(Player::getLocation)
            .toList();
        if (!destinations.isEmpty()) {
            player.teleportAsync(Util.getRandom(destinations));
        }
        GlowColorManager.getInstance().update(player);
        manor2.getPacketGlow().removeTarget(player);
        manor2.getPacketGlow().addViewer(player);
        player.showTitle(Title.title(Component.empty(), Util.color(SPECTATOR_INIT_MESSAGE)));
    }

    @Override
    public void retire(Manor2 manor2) {
        this.retired = true;
        Player player = eventPlayer.getPlayer();
        if (player == null) {
            return;
        }
        super.retire(manor2);
    }

    private Stream<ItemStack> toPlayerHead(EventPlayer eventPlayer, int score) {
        if (!(eventPlayer.getPlayer() instanceof Player player)) {
            return Stream.of();
        }
        ItemStack itemStack = ItemType.STICK.createItemStack();
        itemStack.setData(DataComponentTypes.ITEM_MODEL, NamespacedKey.minecraft("player_head"));
        itemStack.setData(DataComponentTypes.PROFILE, ResolvableProfile.resolvableProfile(player.getPlayerProfile()));
        itemStack.setData(DataComponentTypes.LORE, ItemLore.lore(
            List.of(
                Util.color("<gradient:gold:yellow>" + Util.getRandom(EAT_ME)),
                Util.color(""),
                Util.color(
                    "<player>'s score: <gold><score>",
                    NamedTextColor.GRAY,
                    Formatter.number("score", score),
                    Placeholder.component("player", player.displayName())
                )
            )
        ));
        itemStack.setData(DataComponentTypes.CUSTOM_NAME, player.displayName()
            .decoration(TextDecoration.ITALIC, TextDecoration.State.FALSE)
        );
        itemStack.editPersistentDataContainer(pdc -> {
            pdc.set(Manor2.ITEM_TAG, PersistentDataType.STRING, PLAYER_SCORE_ITEM);
            pdc.set(SCORE_TARGET, PersistentDataType.STRING, player.getUniqueId().toString());
        });
        itemStack.setData(DataComponentTypes.CONSUMABLE, Consumable.consumable()
            .hasConsumeParticles(true)
            .consumeSeconds(1f)
            .build());
        return Stream.of(itemStack);
    }

    public void onConsumeItem(ItemStack item) {
        PersistentDataContainerView pdc = item.getPersistentDataContainer();
        if (!PLAYER_SCORE_ITEM.equalsIgnoreCase(pdc.get(Manor2.ITEM_TAG, PersistentDataType.STRING))) {
            return;
        }
        String uuidString = pdc.get(SCORE_TARGET, PersistentDataType.STRING);
        if (uuidString == null) {
            return;
        }
        try {
            UUID playerUuid = UUID.fromString(uuidString);
            if (manor2.getRoleMap().is(playerUuid, Spectator.class)) {
                operatePlayer(player -> player.sendMessage(Util.color("<red>This player is now also a spectator")));
            }
            Manor2Role role = manor2.getRoleMap().get(playerUuid);
            if (role != null && role.getEventPlayer().getPlayer() instanceof Player toSpectate) {
                operatePlayer(player -> {
                    player.showTitle(Util.title("", "Spectating <player>...", Placeholder.component("player", toSpectate.displayName())));
                    player.teleportAsync(toSpectate.getLocation());
                });
            }
        } catch (IllegalArgumentException ignored) {
        }
    }
}
