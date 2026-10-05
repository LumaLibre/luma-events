package dev.lumas.events.games.logic.mobarena;

import dev.lumas.events.games.logic.mobarena.MobArenaRewrite.ArenaMob;
import lombok.Getter;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Mob;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.Nullable;

import java.util.List;

@Getter
public enum BossType {

    BONE_WARLORD(ArenaMob.Rank.MINI_BOSS, "Bone Warlord", "#e8e8e8", EntityType.WITHER_SKELETON, 350, 1.35, 0.15, 0, 4,
            Material.NETHERITE_SWORD, Material.NETHERITE_HELMET, List.of(Ability.LEAP_SLAM, Ability.MINION_SUMMON)),
    FROSTBINDER(ArenaMob.Rank.MINI_BOSS, "Frostbinder", "#9fe7ff", EntityType.STRAY, 280, 1.3, 0.15, 0, 3,
            Material.BOW, null, List.of(Ability.ARROWS, Ability.FROST_NOVA, Ability.TELEPORT_NEAR)),
    HEXWITCH(ArenaMob.Rank.MINI_BOSS, "Hexwitch", "#b36bff", EntityType.WITCH, 260, 1.3, 0.15, 0, 3,
            null, null, List.of(Ability.POTION_TOSS, Ability.TELEPORT_NEAR, Ability.MINION_SUMMON)),
    BRUTE_WARLORD(ArenaMob.Rank.MINI_BOSS, "Brute Warlord", "#f0a830", EntityType.PIGLIN_BRUTE, 380, 1.3, 0.15, 0, 3,
            Material.NETHERITE_AXE, Material.GOLDEN_HELMET, List.of(Ability.LEAP_SLAM, Ability.WAR_CRY)),
    BEHEMOTH(ArenaMob.Rank.MINI_BOSS, "Behemoth", "#c46a4a", EntityType.HOGLIN, 400, 1.7, 0.15, 0, 3,
            null, null, List.of(Ability.CHARGE, Ability.GROUND_POUND, Ability.MINION_SUMMON)),
    INFERNAL_RAVAGER(ArenaMob.Rank.MINI_BOSS, "Infernal Ravager", "#ff5a2a", EntityType.RAVAGER, 400, 1.15, 0.15, 0, 4,
            null, null, List.of(Ability.CHARGE, Ability.FIREBALLS, Ability.WAR_CRY)) {
        @Override
        public void equip(Mob mob) {
            super.equip(mob);
            mob.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, PotionEffect.INFINITE_DURATION, 0, false, false, false));
        }
    },

    WARDEN(ArenaMob.Rank.BOSS, "The Warden", "#2fb3b3", EntityType.WARDEN, 500, 1.0, 0, 20, 3,
            null, null, List.of()),
    WITHER(ArenaMob.Rank.BOSS, "The Wither", "#9a9a9a", EntityType.WITHER, 500, 1.0, 0, 0, 4,
            null, null, List.of(Ability.MINION_SUMMON)),
    ENDER_DRAGON(ArenaMob.Rank.BOSS, "Ender Dragon", "#c77dff", EntityType.ENDER_DRAGON, 500, 1.0, 0, 0, 0,
            null, null, List.of());

    private final ArenaMob.Rank rank;
    private final String displayName;
    private final String color;
    private final EntityType type;
    private final double baseHealth;
    private final double scale;
    private final double speedBonus;
    private final double attackDamage;
    private final int height;
    @Nullable
    private final Material mainHand;
    @Nullable
    private final Material helmet;
    private final List<Ability> abilities;

    BossType(ArenaMob.Rank rank, String displayName, String color, EntityType type, double baseHealth, double scale,
             double speedBonus, double attackDamage, int height, @Nullable Material mainHand, @Nullable Material helmet,
             List<Ability> abilities) {
        this.rank = rank;
        this.displayName = displayName;
        this.color = color;
        this.type = type;
        this.baseHealth = baseHealth;
        this.scale = scale;
        this.speedBonus = speedBonus;
        this.attackDamage = attackDamage;
        this.height = height;
        this.mainHand = mainHand;
        this.helmet = helmet;
        this.abilities = abilities;
    }

    public boolean canSpawn(int wave, boolean allowEnderDragon) {
        if (wave < MobArenaRewrite.WAVE_COUNT) {
            return this.rank == ArenaMob.Rank.MINI_BOSS;
        }
        return this.rank == ArenaMob.Rank.BOSS && (this != ENDER_DRAGON || allowEnderDragon);
    }

    public ArenaMob.Specification spec(double healthMultiplier) {
        return new ArenaMob.Specification(this.rank, this.type, this.displayName, this.color, this.baseHealth * healthMultiplier,
                this.speedBonus, this.scale, this.attackDamage, this.height, this.abilities, this, null);
    }

    public void equip(Mob mob) {
        if (this.mainHand == null && this.helmet == null) {
            return;
        }
        EntityEquipment equipment = mob.getEquipment();
        equipment.clear();
        if (this.mainHand != null) {
            equipment.setItemInMainHand(ItemStack.of(this.mainHand));
        }
        if (this.helmet != null) {
            equipment.setHelmet(ItemStack.of(this.helmet));
        }
    }
}
