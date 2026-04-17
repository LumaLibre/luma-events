package dev.lumas.events.bunnyarena;


import dev.lumas.events.items.TokenExchanging;
import dev.lumas.events.utility.Util;
import lombok.Getter;
import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Rabbit;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.Nullable;

import java.util.List;

@Getter
public enum BunnyType {


    COMMON("<b><#6CF380>C<#82ED80>o<#98E77F>m<#ADE17F>m<#C3DB7E>o<#D9D57E>n <#E0C977>B<#E7BD6F>u<#EFB268>n<#F6A660>n<#FD9A59>y", false, 100, 0, 2),
    RARE("<b><#6CF380>R<#87EC80>a<#A3E47F>r<#BEDD7F>e <#D9D57E>B<#E2C675>u<#EBB86C>n<#F4A962>n<#FD9A59>y", false, 35, 1, 3),
    SPEED("<b><#6CF380>S<#84EC80>p<#9CE67F>e<#B5DF7F>e<#CDD87E>d <#DDCE7A>B<#E5C172>u<#EDB469>n<#F5A761>n<#FD9A59>y", false, 12, 1,4),
    TINY("<b><#6CF380>T<#87EC80>i<#A3E47F>n<#BEDD7F>y <#D9D57E>B<#E2C675>u<#EBB86C>n<#F4A962>n<#FD9A59>y", true, 20, 1,4),
    GOLDEN("<b><#E7DF71>G<#E7DD6F>o<#E7DA6C>l<#E8D86A>d<#E8D667>e<#E8D465>n <#E8D163>B<#E8CF60>u<#E9CD5E>n<#E9CA5B>n<#E9C859>y", false, 1, 1,13);

    private final List<Rabbit.Type> bunnyTypes = List.of(Rabbit.Type.BROWN, Rabbit.Type.WHITE, Rabbit.Type.BLACK, Rabbit.Type.SALT_AND_PEPPER);
    private final String customName;
    private final boolean baby;
    private final int chance;
    private final int tokenMin;
    private final int tokenMax;
    private final TokenExchanging.TokenType tokenType;

    BunnyType(String customName, boolean baby, int chance, int base, int bound) {
        this.customName = customName;
        this.baby = baby;
        this.chance = chance;
        this.tokenMin = base;
        this.tokenMax = bound;
        this.tokenType = TokenExchanging.TokenType.WAXCAP_SHROOM;
    }

    BunnyType(String customName, boolean baby, int chance, int base, int bound, TokenExchanging.TokenType tokenType) {
        this.customName = customName;
        this.baby = baby;
        this.chance = chance;
        this.tokenMax = base;
        this.tokenMin = bound;
        this.tokenType = tokenType;
    }

    /**
     * Creates a rabbit with the effects for the rarity without spawning it in any worlds
     */
    public Rabbit createBunny(Location location) {
        Rabbit rabbit = location.getWorld().spawn(location, Rabbit.class);
        this.applyEffects(rabbit);
        return rabbit;
    }

    public void applyEffects(Rabbit bunny) {
        bunny.setPersistent(false);
        if (this.isBaby()) {
            bunny.setBaby();
        } else {
            bunny.setAdult();
        }
        bunny.setAgeLock(true);
        bunny.customName(Util.color(this.getCustomName()));
        bunny.setCustomNameVisible(true);
        Util.setPersistentKey(bunny, "bunny", PersistentDataType.STRING, this.name());
        bunny.getAttribute(Attribute.MAX_HEALTH).setBaseValue(40);
        bunny.setHealth(40);

        if (this == GOLDEN) {
            bunny.setRabbitType(Rabbit.Type.GOLD);
            bunny.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, Integer.MAX_VALUE, 0, false, false));
        } else {
            bunny.setRabbitType(Util.getRandom(bunnyTypes));
        }

        if (this == SPEED) {
            bunny.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, Integer.MAX_VALUE, 1, false, false));
        }
    }

    @Nullable
    public static BunnyType getBunnyType(Entity entity) {
        if (!(entity instanceof Rabbit rabbit)) {
            return null;
        }

        String typeAsString = Util.getPersistentKey(rabbit, "bunny", PersistentDataType.STRING);
        if (typeAsString == null) {
            return null;
        }
        return Util.getEnumFromString(BunnyType.class, typeAsString);
    }

    public static BunnyType randomType() {
        BunnyType type = Util.getRandom(BunnyType.values());
        int random = Util.RANDOM.nextInt(100);
        int tries = 0;
        while (random > type.getChance()) {
            type = Util.getRandom(BunnyType.values());
            random = Util.RANDOM.nextInt(100);
            if (tries++ > 100) {
                break; // just in case lol
            }
        }
        return type;
    }
}