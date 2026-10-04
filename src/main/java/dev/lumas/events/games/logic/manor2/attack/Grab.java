package dev.lumas.events.games.logic.manor2.attack;

import dev.lumas.events.games.logic.manor2.Manor2;
import dev.lumas.events.games.logic.manor2.light.LightHit;
import dev.lumas.events.games.logic.manor2.roles.Ghost;
import dev.lumas.events.games.logic.manor2.roles.GhostHunter;
import dev.lumas.events.games.logic.manor2.score.ScoreRecord;
import dev.lumas.events.games.logic.manor2.score.ScoreRecordDisplay;
import dev.lumas.events.utility.Executors;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.DeathProtection;
import io.papermc.paper.datacomponent.item.ResolvableProfile;
import io.papermc.paper.entity.LookAnchor;
import lombok.Getter;
import org.bukkit.EntityEffect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Vector;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public final class Grab {
    private static final int GRAB_TICKS = 50;
    private static final int STAGGER_TILL_BREAK = 150;
    private static final double MIN_STAGGER_CENTEREDNESS = 0.5;
    private static final int HEARTBEAT_INTERVAL_TICKS = 12;
    private static final int KILLED_COOLDOWN_TICKS = 100;
    private static final int BROKEN_COOLDOWN_TICKS = 200;
    private static final int LET_GO_COOLDOWN_TICKS = 100;
    private static final int BROKEN_EXPOSE_TICKS = 60;
    private static final int LET_GO_EXPOSE_TICKS = 20;
    private static final int VICTIM_GRACE_TICKS = 60;
    private static final double BROKEN_KNOCKBACK = 1.2;
    private static final String GHOST_FACE_TAG = "ghost_face";

    @Getter
    private final Ghost ghost;
    private final GhostHunter victim;
    private final Location ghostEyes;
    private final Location victimLocation;
    private final AtomicInteger stagger = new AtomicInteger();
    private final AtomicBoolean ended = new AtomicBoolean();
    private int ticksLeft = GRAB_TICKS; // not making this volatile because only touched by `tick()`

    public Grab(Ghost ghost, GhostHunter victim, Location ghostEyes, Location victimLocation) {
        this.ghost = ghost;
        this.victim = victim;
        this.ghostEyes = ghostEyes;
        this.victimLocation = victimLocation;
    }

    public void begin(Player ghostPlayer) {
        NiceRestraint.apply(ghostPlayer);
        ghost.getVisibility().exposeFor(2);
        victim.operatePlayer(victimPlayer -> {
            if (ended.get()) return;
            NiceRestraint.apply(victimPlayer);
            playEffects(victimPlayer, ghostPlayer);

            victimPlayer.getWorld().playSound(victimPlayer.getLocation(), Sound.ENTITY_WARDEN_SONIC_CHARGE, SoundCategory.HOSTILE, 1.5f, 1f);
        });
    }

    @SuppressWarnings("UnstableApiUsage")
    public void tick(Manor2 manor2) {
        if (ended.get()) return;
        if (ghost.getEventPlayer().getPlayer() == null || victim.getEventPlayer().getPlayer() == null
                || !manor2.getParticipants().contains(ghost.getEventPlayer()) || !manor2.getParticipants().contains(victim.getEventPlayer())) {
            abort(manor2);
            return;
        }
        if (stagger.get() >= STAGGER_TILL_BREAK) {
            breakFree(manor2);
            return;
        }
        if (--ticksLeft <= 0) {
            kill(manor2);
            return;
        }

        ghost.getVisibility().exposeFor(2);

        boolean heartbeat = ticksLeft % HEARTBEAT_INTERVAL_TICKS == 0;
        victim.operatePlayer(player -> {
            if (ended.get()) return;
            player.lookAt(ghostEyes, LookAnchor.EYES);
            player.setFreezeTicks(player.getMaxFreezeTicks() - 1);
            if (heartbeat) {
                player.getWorld().playSound(player.getLocation(), Sound.ENTITY_WARDEN_HEARTBEAT, SoundCategory.HOSTILE, 1.5f, 1f);
            }
        });
    }

    public void light(LightHit hit, UUID shooter) {
        // ignoring the victim's light
        if (shooter.equals(victim.getUuid()) || hit.centeredness() < MIN_STAGGER_CENTEREDNESS) {
            return;
        }
        stagger.addAndGet(Math.max(0, hit.lightLevel()));
    }

    private void kill(Manor2 manor2) {
        if (!finish(manor2)) {
            return;
        }
        manor2.getGhostAttacks().cooldown(ghost, KILLED_COOLDOWN_TICKS);
        victim.operatePlayer(player -> {
            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_WARDEN_SONIC_BOOM, SoundCategory.HOSTILE, 1.5f, 1f);
            player.sendHurtAnimation(0);
        });
        victim.die();
        manor2.getScoreManager().registerScore(
                new ScoreRecord(Map.of(ghost.getUuid(), 100D), new ScoreRecordDisplay.HunterKilled(victim.getUuid()))
        );
    }

    private void breakFree(Manor2 manor2) {
        if (!finish(manor2)) {
            return;
        }
        manor2.getGhostAttacks().cooldown(ghost, BROKEN_COOLDOWN_TICKS);
        manor2.getGhostAttacks().cooldown(victim, VICTIM_GRACE_TICKS);
        ghost.getVisibility().exposeFor(BROKEN_EXPOSE_TICKS);
        ghost.operatePlayer(player -> {
            Vector away = player.getLocation().toVector().subtract(victimLocation.toVector()).setY(0);
            if (away.lengthSquared() < 1.0E-4) away = new Vector(1, 0, 0);
            player.setVelocity(away.normalize().multiply(BROKEN_KNOCKBACK).setY(0.4));
            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_GHAST_SCREAM, SoundCategory.HOSTILE, 1.5f, 0.6f);
        });
    }

    public void backout(Manor2 manor2) {
        if (!finish(manor2)) {
            return;
        }
        manor2.getGhostAttacks().cooldown(ghost, LET_GO_COOLDOWN_TICKS);
        manor2.getGhostAttacks().cooldown(victim, VICTIM_GRACE_TICKS);
        ghost.getVisibility().exposeFor(LET_GO_EXPOSE_TICKS);
    }

    public void abort(Manor2 manor2) {
        finish(manor2);
    }

    private boolean finish(Manor2 manor2) {
        if (!ended.compareAndSet(false, true)) return false;
        manor2.getGhostAttacks().releaseGrab(ghost.getUuid(), this);
        manor2.getGhostAttacks().releaseGrab(victim.getUuid(), this);
        ghost.operatePlayer(NiceRestraint::release);
        victim.operatePlayer(NiceRestraint::release);
        return true;
    }

    private void playEffects(Player victimPlayer, Player ghostPlayer) { // TODO: Looking for a better way to do this?
        ItemStack face = ItemStack.of(Material.PLAYER_HEAD);
        face.setData(DataComponentTypes.PROFILE, ResolvableProfile.resolvableProfile(ghostPlayer.getPlayerProfile()));
        face.setData(DataComponentTypes.DEATH_PROTECTION, DeathProtection.deathProtection(List.of()));
        face.editPersistentDataContainer(pdc -> pdc.set(Manor2.ITEM_TAG, PersistentDataType.STRING, GHOST_FACE_TAG)); // retire() cleans it up if all else fails

        ItemStack offhand = victimPlayer.getInventory().getItemInOffHand();
        victimPlayer.getInventory().setItemInOffHand(face);
        victimPlayer.updateInventory(); // TODO
        Executors.delayedSync(victimPlayer, 1, () -> {
            victimPlayer.playEffect(EntityEffect.PROTECTED_FROM_DEATH);
            Executors.delayedSync(victimPlayer, 1, () -> {
                ItemStack held = victimPlayer.getInventory().getItemInOffHand();
                String tag = held.getPersistentDataContainer().get(Manor2.ITEM_TAG, PersistentDataType.STRING);
                if (GHOST_FACE_TAG.equals(tag)) {
                    victimPlayer.getInventory().setItemInOffHand(offhand);
                }
            });
        });
    }
}