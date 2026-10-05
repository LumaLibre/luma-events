package dev.lumas.events.games.logic.mobarena;

import dev.lumas.events.games.logic.mobarena.MobArenaRewrite.ArenaMob;
import dev.lumas.events.utility.Util;
import lombok.Getter;
import net.kyori.adventure.bossbar.BossBar;
import org.bukkit.Location;
import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.Hoglin;
import org.bukkit.entity.Mob;
import org.bukkit.entity.PiglinAbstract;
import org.bukkit.entity.Player;
import org.bukkit.entity.Warden;
import org.bukkit.entity.Wither;
import org.bukkit.entity.memory.MemoryKey;

import java.util.EnumMap;
import java.util.Map;

// TODO: remove/debug dragon
public final class ArenaBoss extends ArenaMob {
    @Getter
    private final BossBar healthBar;
    private final Map<Ability, Long> cooldowns = new EnumMap<>(Ability.class);
    private long nextPerchAt = System.currentTimeMillis() + 30_000;

    public ArenaBoss(MobArenaRewrite arena, Specification specification, Location anchor) {
        super(arena, specification, anchor, null);
        this.healthBar = BossBar.bossBar(Util.color(specification.coloredName()), 1, BossBar.Color.RED, BossBar.Overlay.PROGRESS);
        for (Ability ability : specification.abilities()) {
            this.cooldowns.put(ability, System.currentTimeMillis() + 3000);
        }
    }

    @Override
    protected Location findSpot() {
        if (this.specification.boss() == BossType.ENDER_DRAGON) {
            return this.anchor.clone().add(0, 20, 0);
        }
        return super.findSpot();
    }

    @Override
    public boolean start() {
        if (!super.start()) {
            return false;
        }
        if (this.ctx.getAudience() != null) {
            this.ctx.getAudience().showBossBar(this.healthBar);
        }
        return true;
    }

    @Override
    public void close(boolean remove) {
        if (this.ctx.getAudience() != null) {
            this.ctx.getAudience().hideBossBar(this.healthBar);
        }
        super.close(remove);
    }

    @Override
    protected void prepare(Mob entity) {
        this.specification.boss().equip(entity);
        super.prepare(entity);
        switch (entity) {
            case PiglinAbstract piglin -> {
                piglin.setImmuneToZombification(true);
            }
            case Hoglin hoglin -> {
                hoglin.setImmuneToZombification(true);
                hoglin.setIsAbleToBeHunted(false);
            }
            case Wither wither -> {
                wither.setInvulnerableTicks(0);
                if (wither.getBossBar() != null) {
                    wither.getBossBar().setVisible(false);
                }
            }
            case EnderDragon dragon -> {
                dragon.setPodium(this.anchor);
                dragon.setPhase(EnderDragon.Phase.CIRCLING);
            }
            default -> {
            }
        }
    }

    @Override
    protected void handleTick(long now) {
        double health = this.entity.getHealth() / Math.min(1024, this.specification.health());
        this.healthBar.progress((float) Math.clamp(health, 0, 1));
        if (this.entity instanceof EnderDragon dragon) {
            this.tickDragon(dragon, now);
            return;
        }
        super.handleTick(now);
    }

    @Override
    protected void attack(Player target, Location targetAt, long now) {
        Mob entity = this.entity;
        if (entity instanceof Warden warden) {
            warden.setAnger(target, 150);
        }
        if (entity instanceof PiglinAbstract piglin) {
            piglin.setMemory(MemoryKey.ANGRY_AT, target.getUniqueId());
        }
        double distance = this.lastLocation.distance(targetAt);
        for (Ability ability : this.specification.abilities()) {
            if (now >= this.cooldowns.getOrDefault(ability, 0L) && ability.use(this, entity, targetAt, distance)) {
                this.cooldowns.put(ability, now + ability.getCooldownMillis());
                break;
            }
        }
    }

    private void tickDragon(EnderDragon dragon, long now) {
        if (Math.hypot(this.lastLocation.getX() - this.anchor.getX(), this.lastLocation.getZ() - this.anchor.getZ()) > 90 || this.lastLocation.getY() < this.ctx.getBoundingBox().worldTied().getMinY()) {
            dragon.teleportAsync(this.anchor.clone().add(0, 20, 0));
        } else if (now >= this.nextPerchAt) {
            this.nextPerchAt = now + 30_000;
            dragon.setPhase(EnderDragon.Phase.FLY_TO_PORTAL);
        }
    }

}
