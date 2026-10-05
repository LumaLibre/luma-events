package dev.lumas.events.games.logic.mobarena;

import dev.lumas.events.games.logic.mobarena.MobArenaRewrite.ArenaMob;
import dev.lumas.events.utility.Executors;
import dev.lumas.events.utility.Util;
import lombok.Getter;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Hoglin;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Ravager;
import org.bukkit.entity.SmallFireball;
import org.bukkit.entity.SplashPotion;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;
import org.bukkit.util.Vector;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

public enum Ability {

    LEAP_SLAM(8_000L) {
        @Override
        boolean use(ArenaMob caster, Mob mob, Location targetAt, double distance) {
            if (distance < 3 || distance > 16 || !mob.isOnGround()) {
                return false;
            }
            Location from = mob.getLocation();
            Vector leap = targetAt.toVector().subtract(from.toVector()).setY(0);
            if (leap.lengthSquared() < 1.0E-4) {
                return false;
            }

            mob.setVelocity(leap.normalize().multiply(Math.min(1.6, 0.35 + distance * 0.09)).setY(0.85));
            from.getWorld().playSound(from, Sound.ENTITY_RAVAGER_ROAR, 1f, 1.4f);

            AtomicInteger airborne = new AtomicInteger();
            Executors.repeatingSync(mob, 2, task -> {
                int airTicks = airborne.incrementAndGet();
                if (!caster.isActive() || !mob.isValid()) {
                    task.cancel();
                } else if ((airTicks > 3 && mob.isOnGround()) || airTicks > 20) {
                    task.cancel();
                    slam(caster, mob, mob.getLocation(), 4.5, 10.0, 0.9, 0.5);
                }
            });
            return true;
        }
    },

    MINION_SUMMON(18_000L) {
        @Override
        boolean use(ArenaMob caster, Mob mob, Location targetAt, double distance) {
            int count = Math.min(3, MAX_SUMMONS - caster.getSummons().get());
            if (count <= 0) {
                return false;
            }

            Location at = mob.getLocation();
            at.getWorld().playSound(at, Sound.ENTITY_EVOKER_PREPARE_SUMMON, 1.2f, 0.8f);
            at.getWorld().spawnParticle(Particle.SOUL, at.clone().add(0, 1, 0), 30, 1.5, 0.5, 1.5, 0.02);
            caster.getCtx().spawnSummons(caster, at, count);
            return true;
        }
    },

    ARROWS(6_000L) {
        @Override
        boolean use(ArenaMob caster, Mob mob, Location targetAt, double distance) {
            if (distance > 28) {
                return false;
            }
            Location eye = mob.getEyeLocation();
            Vector aim = targetAt.clone().add(0, 1.2, 0).toVector().subtract(eye.toVector());
            aim.setY(aim.getY() + Math.hypot(aim.getX(), aim.getZ()) * 0.12);
            if (aim.lengthSquared() < 1.0E-4) {
                return false;
            }
            aim.normalize();

            for (int i = -3; i <= 3; i++) {
                Arrow arrow = mob.launchProjectile(Arrow.class, aim.clone().rotateAroundY(Math.toRadians(i * 7)).multiply(1.9));
                arrow.setPickupStatus(AbstractArrow.PickupStatus.DISALLOWED);
                arrow.setDamage(3.0);
                arrow.addCustomEffect(new PotionEffect(PotionEffectType.SLOWNESS, 60, 1), true);
            }
            eye.getWorld().playSound(eye, Sound.ENTITY_SKELETON_SHOOT, 1.2f, 0.7f);
            return true;
        }
    },

    FROST_NOVA(12_000L) {
        @Override
        boolean use(ArenaMob caster, Mob mob, Location targetAt, double distance) {
            Location at = mob.getLocation();
            if (caster.getCtx().fightersNear(at, 6).isEmpty()) {
                return false;
            }

            World world = at.getWorld();
            for (int i = 0; i < 36; i++) {
                double angle = Math.PI * 2 * i / 36;
                world.spawnParticle(Particle.SNOWFLAKE, at.clone().add(Math.cos(angle) * 6, 0.3, Math.sin(angle) * 6), 2, 0.1, 0.1, 0.1, 0.01);
            }
            world.spawnParticle(Particle.CLOUD, at.clone().add(0, 0.5, 0), 40, 2, 0.3, 2, 0.05);
            world.playSound(at, Sound.BLOCK_POWDER_SNOW_BREAK, 1.5f, 0.6f);
            world.playSound(at, Sound.ENTITY_PLAYER_HURT_FREEZE, 1.2f, 0.8f);
            caster.getCtx().hurtFightersNear(mob, at, 6, 6.0, 0.4, 0.2, player -> {
                player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 60, 2));
                player.setFreezeTicks(Math.max(player.getFreezeTicks(), 200));
            });
            return true;
        }
    },

    POTION_TOSS(7_000L) {
        @Override
        boolean use(ArenaMob caster, Mob mob, Location targetAt, double distance) {
            Location eye = mob.getEyeLocation();
            List<MobArenaRewrite.Nearby> targets = caster.getCtx().fightersNear(eye, 16).stream().limit(3).toList();
            if (targets.isEmpty()) {
                return false;
            }

            for (MobArenaRewrite.Nearby target : targets) {
                Vector toss = target.at().clone().add(0, 1.1, 0).toVector().subtract(eye.toVector());
                double horizontal = Math.hypot(toss.getX(), toss.getZ());
                toss.setY(toss.getY() + horizontal * 0.2);
                if (toss.lengthSquared() < 1.0E-4) {
                    continue;
                }

                ItemStack potion = ItemStack.of(Material.SPLASH_POTION);
                potion.editMeta(PotionMeta.class, meta -> meta.setBasePotionType(Util.getRandom(HEX_POTIONS)));
                SplashPotion thrown = mob.launchProjectile(SplashPotion.class);
                thrown.setItem(potion);
                thrown.setVelocity(toss.normalize().multiply(0.75 + horizontal * 0.015));
            }
            eye.getWorld().playSound(eye, Sound.ENTITY_WITCH_THROW, 1.2f, 0.8f);
            return true;
        }
    },

    TELEPORT_NEAR(10_000L) {
        @Override
        boolean use(ArenaMob caster, Mob mob, Location targetAt, double distance) {
            if (distance >= 24) {
                return false;
            }
            Location spot = targetAt.clone();
            Executors.runSync(spot, () -> {
                if (!caster.isActive()) {
                    return;
                }
                spot.setDirection(targetAt.toVector().subtract(spot.toVector()));
                Executors.runSync(mob, () -> {
                    if (!caster.isActive() || !mob.isValid()) {
                        return;
                    }
                    Location before = mob.getLocation().add(0, 1, 0);
                    before.getWorld().spawnParticle(Particle.REVERSE_PORTAL, before, 40, 0.4, 0.8, 0.4, 0.05);
                    before.getWorld().playSound(before, Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 0.7f);
                    mob.teleportAsync(spot).thenRun(() -> Executors.runSync(mob, () ->
                            spot.getWorld().spawnParticle(Particle.PORTAL, spot.clone().add(0, 1, 0), 40, 0.4, 0.8, 0.4, 0.3)));
                });
            });
            return true;
        }
    },

    WAR_CRY(15_000L) {
        @Override
        boolean use(ArenaMob caster, Mob mob, Location targetAt, double distance) {
            Location at = mob.getLocation();
            at.getWorld().playSound(at, mob instanceof Ravager ? Sound.ENTITY_RAVAGER_ROAR : Sound.ENTITY_PIGLIN_BRUTE_ANGRY, 1.5f, 0.8f);
            at.getWorld().spawnParticle(Particle.ANGRY_VILLAGER, at.clone().add(0, 2, 0), 12, 1, 0.5, 1, 0);
            for (ArenaMob ally : caster.getCtx().mobs()) {
                Location allyAt = ally.getLastLocation();
                if (allyAt == null || !Objects.equals(allyAt.getWorld(), at.getWorld()) || allyAt.distanceSquared(at) > 144) {
                    continue;
                }
                ally.buff(new PotionEffect(PotionEffectType.SPEED, 160, 1), new PotionEffect(PotionEffectType.STRENGTH, 160, 0));
            }
            return true;
        }
    },

    CHARGE(7_000L) {
        @Override
        boolean use(ArenaMob caster, Mob mob, Location targetAt, double distance) {
            if (distance < 4 || distance > 20 || !mob.isOnGround()) {
                return false;
            }
            Location from = mob.getLocation();
            Vector direction = targetAt.toVector().subtract(from.toVector()).setY(0);
            if (direction.lengthSquared() < 1.0E-4) {
                return false;
            }
            direction.normalize();

            from.setDirection(direction);
            mob.setRotation(from.getYaw(), 0f);
            from.getWorld().playSound(from, mob instanceof Hoglin ? Sound.ENTITY_HOGLIN_ANGRY : Sound.ENTITY_RAVAGER_ROAR, 1.5f, 0.7f);

            Set<UUID> trampled = new HashSet<>();
            AtomicInteger steps = new AtomicInteger();
            Executors.repeatingSync(mob, 2, task -> {
                if (!caster.isActive() || !mob.isValid() || steps.incrementAndGet() > 8) {
                    task.cancel();
                    return;
                }
                Vector velocity = direction.clone().multiply(0.9);
                velocity.setY(mob.getVelocity().getY());
                mob.setVelocity(velocity);

                Location now = mob.getLocation();
                now.getWorld().spawnParticle(Particle.CLOUD, now, 4, 0.3, 0.1, 0.3, 0.01);
                for (MobArenaRewrite.Nearby near : caster.getCtx().fightersNear(now, 2.4 + mob.getWidth() / 2)) {
                    if (trampled.add(near.uuid())) {
                        caster.getCtx().hurtFighter(mob, near.uuid(), 12.0, direction.clone().multiply(1.4).setY(0.5), null);
                    }
                }
            });
            return true;
        }
    },

    GROUND_POUND(10_000L) {
        @Override
        boolean use(ArenaMob caster, Mob mob, Location targetAt, double distance) {
            Location at = mob.getLocation();
            if (caster.getCtx().fightersNear(at, 6).isEmpty()) {
                return false;
            }
            at.getWorld().playSound(at, Sound.ENTITY_RAVAGER_ATTACK, 1.2f, 0.6f);
            at.getWorld().spawnParticle(Particle.LARGE_SMOKE, at, 20, 1.5, 0.1, 1.5, 0.02);
            Executors.delayedSync(mob, 12, () -> {
                if (caster.isActive() && mob.isValid()) {
                    slam(caster, mob, mob.getLocation(), 6, 9.0, 0.6, 0.9);
                }
            });
            return true;
        }
    },

    FIREBALLS(9_000L) {
        @Override
        boolean use(ArenaMob caster, Mob mob, Location targetAt, double distance) {
            if (distance > 24) {
                return false;
            }
            Location eye = mob.getEyeLocation();
            List<MobArenaRewrite.Nearby> targets = caster.getCtx().fightersNear(eye, 24).stream().limit(3).toList();
            if (targets.isEmpty()) {
                return false;
            }

            for (MobArenaRewrite.Nearby target : targets) {
                for (int i = 0; i < 2; i++) {
                    Vector direction = target.at().clone().add(0, 1, 0).toVector().subtract(eye.toVector());
                    if (direction.lengthSquared() < 1.0E-4) {
                        continue;
                    }
                    Vector spread = new Vector(Util.RANDOM.nextGaussian(), Util.RANDOM.nextGaussian(), Util.RANDOM.nextGaussian()).multiply(0.06);
                    direction.normalize().add(spread).normalize();

                    SmallFireball fireball = mob.launchProjectile(SmallFireball.class);
                    fireball.setIsIncendiary(false);
                    fireball.setYield(0f);
                    fireball.setAcceleration(direction.clone().multiply(0.1));
                    fireball.setVelocity(direction.clone().multiply(0.9));
                }
            }
            eye.getWorld().playSound(eye, Sound.ENTITY_BLAZE_SHOOT, 1.2f, 0.8f);
            return true;
        }
    };

    private static final int MAX_SUMMONS = 6;
    private static final List<PotionType> HEX_POTIONS = List.of(PotionType.HARMING, PotionType.POISON, PotionType.SLOWNESS, PotionType.WEAKNESS);

    @Getter
    private final long cooldownMillis;

    Ability(long cooldownMillis) {
        this.cooldownMillis = cooldownMillis;
    }

    abstract boolean use(ArenaMob caster, Mob mob, Location targetAt, double distance);

    final void slam(ArenaMob caster, Mob mob, Location center, double radius, double damage, double push, double lift) {
        World world = center.getWorld();
        Block below = center.clone().subtract(0, 1, 0).getBlock();
        world.spawnParticle(Particle.EXPLOSION, center, 3, 1, 0.2, 1, 0);
        world.spawnParticle(Particle.BLOCK, center, 60, radius / 2, 0.1, radius / 2, 0,
                below.getType().isAir() ? Material.DIRT.createBlockData() : below.getBlockData());
        world.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 0.8f, 1.4f);
        caster.getCtx().hurtFightersNear(mob, center, radius, damage, push, lift, null);
    }
}
