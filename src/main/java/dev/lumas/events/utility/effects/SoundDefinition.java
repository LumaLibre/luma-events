package dev.lumas.events.utility.effects;


import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.sound.Sound;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public record SoundDefinition(Sound.Type sound, float volume, float pitch, @Nullable Long seed) {

    public Sound toSound() {
        Sound.Builder builder = Sound.sound()
            .type(sound)
            .volume(volume)
            .source(Sound.Source.MASTER)
            .pitch(pitch);
        if (seed != null) {
            builder.seed(seed);
        }
        return builder.build();
    }

    public void playAt(Location location) {
        location.getWorld().playSound(toSound(), location);
    }

    public void playAt(Entity entity) {
        entity.getWorld().playSound(toSound(), entity);
    }

    public void playFor(Audience audience) {
        audience.playSound(toSound());
    }
}
