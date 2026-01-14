package dev.jsinco.luma.lumaevents.utility;

import org.bukkit.Bukkit;

import java.util.logging.Level;

/**
 * @deprecated Use {@link dev.lumas.lumacore.utility.Logging} instead.
 */
@Deprecated(forRemoval = true)
public class Logger {

    public static void log(String message) {
        StackTraceElement caller = Thread.currentThread().getStackTrace()[2];
        String className = caller.getClassName().substring(caller.getClassName().lastIndexOf('.') + 1);
        String prefixedMessage = "[LumaEvents - " + className + ":" + caller.getLineNumber() + "] " + message;
        Bukkit.getLogger().log(Level.INFO, prefixedMessage);
    }

    public static void logWrn(String message) {
        StackTraceElement caller = Thread.currentThread().getStackTrace()[2];
        String className = caller.getClassName().substring(caller.getClassName().lastIndexOf('.') + 1);
        String prefixedMessage = "[LumaEvents - " + className + ":" + caller.getLineNumber() + "] " + message;
        Bukkit.getLogger().log(Level.WARNING, prefixedMessage);
    }

    public static void logErr(String message) {
        StackTraceElement caller = Thread.currentThread().getStackTrace()[2];
        String className = caller.getClassName().substring(caller.getClassName().lastIndexOf('.') + 1);
        String prefixedMessage = "[LumaEvents - " + className + ":" + caller.getLineNumber() + "] " + message;
        Bukkit.getLogger().log(Level.SEVERE, prefixedMessage);
    }

    public static void logErr(Throwable throwable) {
        StackTraceElement caller = Thread.currentThread().getStackTrace()[2];
        String className = caller.getClassName().substring(caller.getClassName().lastIndexOf('.') + 1);
        String prefix = "[LumaEvents - " + className + ":" + caller.getLineNumber() + "] ";
        Bukkit.getLogger().log(Level.SEVERE, prefix + throwable.getMessage(), throwable);
    }

}
