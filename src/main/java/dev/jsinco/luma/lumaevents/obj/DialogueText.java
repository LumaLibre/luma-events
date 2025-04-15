package dev.jsinco.luma.lumaevents.obj;

import dev.jsinco.luma.lumaevents.EventMain;
import dev.jsinco.luma.lumaevents.utility.MonoUpperFont;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class DialogueText {

    private final EventPlayer eventPlayer;
    private final int rate;
    private final List<String> queue;

    private boolean monoUpperFont;

    public DialogueText(EventPlayer eventPlayer) {
        this.eventPlayer = eventPlayer;
        this.rate = 60;
        this.queue = new ArrayList<>();
    }

    public DialogueText(EventPlayer eventPlayer, boolean monoUpperFont) {
        this(eventPlayer);
        this.monoUpperFont = monoUpperFont;
    }

    public void queueText(String msg) {
        if (monoUpperFont) {
            msg = MonoUpperFont.toMonoupperText(msg);
        }
        this.queue.add(msg);
    }

    public void sendQueuedText(@Nullable TextColor color, @Nullable TextDecoration decoration, @Nullable Runnable callback) {
        Player player = this.eventPlayer.getPlayer();

        if (this.queue.isEmpty() || player == null) {
            return;
        }


        final int[] currentChar = {0};
        StringBuilder text = new StringBuilder(this.queue.getFirst());

        Bukkit.getAsyncScheduler().runAtFixedRate(EventMain.getInstance(), (task) -> {
            currentChar[0]++;

            int totalChars = text.length();

            // If it's a space, let's jump to the next character
            if (currentChar[0] < totalChars && text.charAt(currentChar[0]) == ' ') {
                currentChar[0]++;
            }

            if (currentChar[0] <= totalChars) {
                sendActionBar(color, decoration, player, text.substring(0, currentChar[0]));
            } else {
                this.queue.removeFirst(); // Remove the first element from the queue
                currentChar[0] = 0; // Reset the current character index
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }

                if (!this.queue.isEmpty()) {
                    text.delete(0, text.length());
                    text.append(this.queue.getFirst());
                    currentChar[0] = 0; // Reset the current character index
                    return;
                } else {
                    task.cancel();
                }
                if (callback != null) {
                    callback.run();
                }
            }
        }, 0, this.rate, TimeUnit.MILLISECONDS);
    }

    public void sendMessage(String text, TextColor color) {
        sendMessage(text, color, null, null);
    }


    public void sendMessage(@NotNull String text, @Nullable TextColor color, @Nullable TextDecoration decoration, @Nullable Runnable callback) {
        Player player = this.eventPlayer.getPlayer();
        if (player == null) {
            return;
        }
        final int totalChars = text.length();
        final int[] currentChar = {0}; // Mutable wrapper for the variable

        Bukkit.getAsyncScheduler().runAtFixedRate(EventMain.getInstance(), (task) -> {
            currentChar[0]++; // Increment the value
            // If it's a space, let's jump to the next character
            if (currentChar[0] < totalChars && text.charAt(currentChar[0]) == ' ') {
                currentChar[0]++;
            }

            if (currentChar[0] <= totalChars) {
                sendActionBar(color, decoration, player, text.substring(0, currentChar[0]));
            } else {
                task.cancel();
                if (callback != null) {
                    callback.run();
                }
            }
        }, 0, this.rate, TimeUnit.MILLISECONDS);
    }

    private void sendActionBar(@Nullable TextColor color, @Nullable TextDecoration decoration, Player player, String substring) {
        Component component = Component.text(substring);
        if (color != null) component = component.color(color);
        if (decoration != null) component = component.decorate(decoration);
        player.sendActionBar(component);
    }
}
