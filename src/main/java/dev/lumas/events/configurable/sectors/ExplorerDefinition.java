package dev.lumas.events.configurable.sectors;

import eu.okaeri.configs.OkaeriConfig;
import eu.okaeri.configs.annotation.Comment;
import lombok.Getter;

import java.util.List;

@Getter
public class ExplorerDefinition extends OkaeriConfig {

    private boolean explorerMiles = false;

    private boolean explorerOrders = true;

    @Comment("Any worlds not contained in this list a player will not be able to enter while they are suspended.")
    private List<String> suspendedWorlds = List.of(
            "pale_side", "pale_nether", "pale_end"
    );

    @Comment("The world to spawn the player back into after being unsuspended. If this world does not exist, a random world will be chosen.")
    private String suspendRemovalWorld = "spawn";
}
