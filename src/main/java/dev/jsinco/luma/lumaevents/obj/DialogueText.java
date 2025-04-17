package dev.jsinco.luma.lumaevents.obj;

import dev.jsinco.luma.lumaevents.EventMain;
import dev.jsinco.luma.lumaevents.utility.MonoUpperFont;
import dev.jsinco.luma.lumaevents.utility.Util;
import lombok.Getter;
import lombok.Setter;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class DialogueText {

    private final EventPlayer eventPlayer;
    private final int rate;
    private final List<String> queue;
    @Getter @Setter
    private TextColor ifAbsentColor = NamedTextColor.WHITE;

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

    public void queueText(List<String> msg) {
        for (String s : msg) {
            queueText(s);
        }
    }

    public void queueText(String[] msg) {
        for (String s : msg) {
            queueText(s);
        }
    }

    public void sendQueuedText() {
        sendQueuedText(null);
    }

    public void sendQueuedText(@Nullable Runnable callback) {
        Player player = this.eventPlayer.getPlayer();

        if (this.queue.isEmpty() || player == null) {
            return;
        }

        final int[] currentChar = {0};
        StringBuilder text = new StringBuilder(this.queue.getFirst());

        Bukkit.getAsyncScheduler().runAtFixedRate(EventMain.getInstance(), (task) -> {
            int totalChars = text.length();
            // If it's a < let's increment until we find a >
            if (currentChar[0] < totalChars && text.charAt(currentChar[0]) == '<') {
                while (currentChar[0] < totalChars && text.charAt(currentChar[0]) != '>') {
                    currentChar[0]++;
                }
                currentChar[0]++;
            }


            currentChar[0]++;


            // If it's a space, let's jump to the next character
            if (currentChar[0] < totalChars && text.charAt(currentChar[0]) == ' ') {
                currentChar[0]++;
            }


            if (currentChar[0] <= totalChars) {
                sendActionBar(player, text.substring(0, currentChar[0]));
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


    private void sendActionBar(Player player, String substring) {
        //Component component = Component.text(substring);

        //if (color != null) component = component.color(color);
        //if (decoration != null) component = component.decorate(decoration);
        player.sendActionBar(Util.color(substring, this.ifAbsentColor));
    }
}


//public void sendMessage(String text) {
//        sendMessage(text, null);
//    }
//
//
//    public void sendMessage(String text, @Nullable Runnable callback) {
//        Player player = this.eventPlayer.getPlayer();
//        if (player == null) {
//            return;
//        }
//        final int totalChars = text.length();
//        final int[] currentChar = {0}; // Mutable wrapper for the variable
//
//        Bukkit.getAsyncScheduler().runAtFixedRate(EventMain.getInstance(), (task) -> {
//            currentChar[0]++; // Increment the value
//            // If it's a space, let's jump to the next character
//            if (currentChar[0] < totalChars && text.charAt(currentChar[0]) == ' ') {
//                currentChar[0]++;
//            }
//            // If it's a < let's increment until we find a >
//            if (currentChar[0] < totalChars && text.charAt(currentChar[0]) == '<') {
//                while (currentChar[0] < totalChars && text.charAt(currentChar[0]) != '>') {
//                    currentChar[0]++;
//                }
//                currentChar[0]++;
//            }
//
//            if (currentChar[0] <= totalChars) {
//                sendActionBar(player, text.substring(0, currentChar[0]));
//            } else {
//                task.cancel();
//                if (callback != null) {
//                    callback.run();
//                }
//            }
//        }, 0, this.rate, TimeUnit.MILLISECONDS);
//    }