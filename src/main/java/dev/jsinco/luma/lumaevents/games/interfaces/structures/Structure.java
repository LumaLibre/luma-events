package dev.jsinco.luma.lumaevents.games.interfaces.structures;

import dev.jsinco.luma.lumaevents.EventMain;
import org.bukkit.Location;

import java.nio.file.Path;

public abstract class Structure {

    protected static final Path SCHEMATIC_DIR = EventMain.getInstance().getDataPath().resolve("schematics");

    protected final Location origin;
    protected final String localSchemPath;

    public Structure(Location origin, String localSchemPath) {
        this.origin = origin;
        this.localSchemPath = localSchemPath;
    }

    public abstract void paste();

    public abstract void remove();
}
