package dev.lumas.events.games.logic.mobarena;

import dev.lumas.events.games.logic.mobarena.MobArenaRewrite.ArenaMob;
import dev.lumas.events.utility.Util;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.random.RandomGenerator;

public enum MobType {

    BASIC(EntityType.WITHER_SKELETON, 100, 7, 0.15, 3,
            new Gear(Material.STONE_SWORD, Material.CHAINMAIL_HELMET, Material.CHAINMAIL_CHESTPLATE, Material.CHAINMAIL_LEGGINGS, Material.CHAINMAIL_BOOTS),
            70, 40, 20),
    UNCOMMON(EntityType.WITHER_SKELETON, 150, 10, 0.15, 3,
            new Gear(Material.IRON_SWORD, Material.IRON_HELMET, Material.IRON_CHESTPLATE, Material.IRON_LEGGINGS, Material.IRON_BOOTS),
            25, 40, 40),
    RARE(EntityType.WITHER_SKELETON, 200, 16, 0.15, 3,
            new Gear(Material.DIAMOND_SWORD, Material.DIAMOND_HELMET, Material.DIAMOND_CHESTPLATE, Material.DIAMOND_LEGGINGS, Material.DIAMOND_BOOTS),
            5, 20, 30),
    ELITE(EntityType.WITHER_SKELETON, 250, 20, 0.15, 3,
            new Gear(Material.NETHERITE_SWORD, Material.NETHERITE_HELMET, Material.NETHERITE_CHESTPLATE, Material.NETHERITE_LEGGINGS, Material.NETHERITE_BOOTS),
            0, 0, 10),

    BLAST_CREEPER(EntityType.CREEPER, 60, 0, 0.25, 2, null,
            8, 12, 15) {
        @Override
        public boolean explode(MobArenaRewrite ctx, ArenaMob mob) {
            Mob creeper = mob.getEntity();
            Location center = creeper.getLocation();
            World world = center.getWorld();
            world.spawnParticle(Particle.EXPLOSION_EMITTER, center, 1);
            world.spawnParticle(Particle.GUST_EMITTER_LARGE, center, 1);
            world.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 4f, 0.9f);
            world.playSound(center, Sound.ENTITY_WIND_CHARGE_WIND_BURST, 3f, 0.6f);

            for (Entity nearby : creeper.getNearbyEntities(BLAST_RADIUS, BLAST_RADIUS, BLAST_RADIUS)) {
                if (!(nearby instanceof Player player)) {
                    continue;
                }
                Vector away = player.getLocation().toVector().subtract(center.toVector()).setY(0);
                double distance = away.length();
                if (distance > BLAST_RADIUS) {
                    continue;
                }
                if (distance < 1.0E-3) {
                    away = new Vector(Util.RANDOM.nextDouble() - 0.5, 0, Util.RANDOM.nextDouble() - 0.5);
                }

                double closeness = 1 - distance / BLAST_RADIUS;
                ctx.launch(player, away.normalize().multiply(BLAST_LAUNCH_SPEED * (0.6 + 0.4 * closeness)).setY(BLAST_LAUNCH_LIFT));
            }
            ctx.removeMob(mob, true);
            return true;
        }
    };

    private static final double BLAST_RADIUS = 5.0;
    private static final double BLAST_LAUNCH_SPEED = 2.6;
    private static final double BLAST_LAUNCH_LIFT = 0.7;

    private final EntityType type;
    private final double health;
    private final double damage;
    private final double speedBonus;
    private final int height;
    @Nullable
    private final Gear gear;
    private final int[] weights;

    MobType(EntityType type, double health, double damage, double speedBonus, int height, @Nullable Gear gear, int... weights) {
        this.type = type;
        this.health = health;
        this.damage = damage;
        this.speedBonus = speedBonus;
        this.height = height;
        this.gear = gear;
        this.weights = weights;
    }

    public static MobType roll(int wave, RandomGenerator random) {
        int total = 0;
        for (MobType type : values()) {
            total += type.weight(wave);
        }
        int roll = random.nextInt(total);
        for (MobType type : values()) {
            roll -= type.weight(wave);
            if (roll < 0) {
                return type;
            }
        }
        throw new IllegalStateException("No mob type selected");
    }

    public int weight(int wave) {
        return this.weights[Math.clamp(wave - 1, 0, this.weights.length - 1)];
    }

    public ArenaMob.Specification spec(boolean summon) {
        return new ArenaMob.Specification(summon ? ArenaMob.Rank.SUMMON : ArenaMob.Rank.NORMAL,
                this.type, this.name(), "#d6d6d6", this.health * (summon ? 0.75 : 1),
                this.speedBonus, 1, this.damage, this.height, List.of(), null, this);
    }

    public void prepare(Mob mob) {
        if (this.gear != null) {
            this.gear.equip(mob.getEquipment());
        }
    }

    public boolean explode(MobArenaRewrite ctx, ArenaMob mob) {
        return false;
    }

    public record Gear(Material weapon, Material helmet, Material chestplate, Material leggings, Material boots) {

        void equip(EntityEquipment equipment) {
            equipment.clear();
            equipment.setItemInMainHand(ItemStack.of(this.weapon));
            equipment.setHelmet(ItemStack.of(this.helmet));
            equipment.setChestplate(ItemStack.of(this.chestplate));
            equipment.setLeggings(ItemStack.of(this.leggings));
            equipment.setBoots(ItemStack.of(this.boots));
        }
    }
}
