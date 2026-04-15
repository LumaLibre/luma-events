package dev.lumas.events.games.models;

import dev.lumas.events.model.EventPlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;

public class NabbitPlayerSet extends HashSet<NabbitPlayer> {

    public Set<NabbitPlayer> getNabbits() {
        Set<NabbitPlayer> nabbits = new HashSet<>();
        for (NabbitPlayer element : this) {
            if (element.isNabbit()) {
                nabbits.add(element);
            }
        }
        return nabbits;
    }

    public Set<NabbitPlayer> getRabbits() {
        Set<NabbitPlayer> rabbits = new HashSet<>();
        for (NabbitPlayer element : this) {
            if (element.getRole() == NabbitPlayer.Role.RABBIT) {
                rabbits.add(element);
            }
        }
        return rabbits;
    }

    public Set<NabbitPlayer> getFleeing() {
        Set<NabbitPlayer> fleeing = new HashSet<>();
        for (NabbitPlayer element : this) {
            if (element.getRole() == NabbitPlayer.Role.FLEEING) {
                fleeing.add(element);
            }
        }
        return fleeing;
    }

    public Set<NabbitPlayer> getRoles(NabbitPlayer.Role... roles) {
        Set<NabbitPlayer> players = new HashSet<>();
        for (NabbitPlayer element : this) {
            for (NabbitPlayer.Role role : roles) {
                if (element.getRole() == role) {
                    players.add(element);
                    break; // No need to check other roles
                }
            }
        }
        return players;
    }

    @Nullable
    public NabbitPlayer getNabbitPlayer(EventPlayer eventPlayer) {
        for (NabbitPlayer nabbitPlayer : this) {
            if (nabbitPlayer.getEventPlayer().getUuid().equals(eventPlayer.getUuid())) {
                return nabbitPlayer;
            }
        }
        return null; // Not found
    }

    @Nullable
    public NabbitPlayer getNabbitPlayer(Player bukkitPlayer) {
        for (NabbitPlayer nabbitPlayer : this) {
            if (nabbitPlayer.getEventPlayer().getUuid().equals(bukkitPlayer.getUniqueId())) {
                return nabbitPlayer;
            }
        }
        return null; // Not found
    }
}