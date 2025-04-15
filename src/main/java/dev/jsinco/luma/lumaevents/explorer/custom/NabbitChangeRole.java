package dev.jsinco.luma.lumaevents.explorer.custom;

import dev.jsinco.luma.lumaevents.games.obj.NabbitPlayer;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class NabbitChangeRole {
    private final NabbitPlayer.Role role;
}
