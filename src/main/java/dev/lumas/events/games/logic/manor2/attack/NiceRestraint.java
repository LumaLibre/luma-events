package dev.lumas.events.games.logic.manor2.attack;

import dev.lumas.events.EventMain;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.Map;

/**
 * A restraint that is 'nice' to clients compared to other methods such as cancelling
 * the player move event.
 */
public final class NiceRestraint {

    private static final NamespacedKey KEY = new NamespacedKey(EventMain.getInstance(), "manor2/restraint");
    private static final AttributeModifier STOPPED = new AttributeModifier(KEY, -1, AttributeModifier.Operation.MULTIPLY_SCALAR_1); // x0, whatever else applies
    private static final AttributeModifier UNMOVABLE = new AttributeModifier(KEY, 1, AttributeModifier.Operation.ADD_NUMBER);
    private static final Map<Attribute, AttributeModifier> MODIFIERS = Map.of(
            Attribute.MOVEMENT_SPEED, STOPPED,
            Attribute.SNEAKING_SPEED, STOPPED,
            Attribute.JUMP_STRENGTH, STOPPED,
            Attribute.KNOCKBACK_RESISTANCE, UNMOVABLE,
            Attribute.EXPLOSION_KNOCKBACK_RESISTANCE, UNMOVABLE
    );

    public static void apply(Player player) {
        MODIFIERS.forEach((attribute, modifier) -> {
            AttributeInstance instance = player.getAttribute(attribute);
            if (instance == null || instance.getModifier(KEY) != null) return;
            instance.addTransientModifier(modifier);
        });
        player.setSprinting(false);
        player.setVelocity(new Vector());
    }

    public static void release(Player player) {
        for (Attribute attribute : MODIFIERS.keySet()) {
            AttributeInstance instance = player.getAttribute(attribute);
            if (instance != null) {
                instance.removeModifier(KEY);
            }
        }
    }
}
