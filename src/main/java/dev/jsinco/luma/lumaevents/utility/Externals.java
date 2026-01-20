package dev.jsinco.luma.lumaevents.utility;

import org.bukkit.Bukkit;

public final class Externals {

    public static boolean classExists(String className) {
        try {
            Class.forName(className);
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    public static boolean pluginExists(String pluginName) {
        return Bukkit.getPluginManager().getPlugin(pluginName) != null;
    }
}
