package dev.jsinco.luma.lumaevents.games.interfaces.structures;

import com.google.common.base.Preconditions;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardReader;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.session.ClipboardHolder;
import dev.jsinco.luma.lumaevents.obj.WorldTiedBoundingBox;
import dev.jsinco.luma.lumaevents.utility.Executors;
import dev.lumas.lumacore.utility.Logging;
import org.bukkit.Location;
import org.bukkit.World;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;

public class WorldEditStructure extends Structure {

    private static final com.sk89q.worldedit.world.block.BlockState AIR = com.sk89q.worldedit.world.block.BlockTypes.AIR.getDefaultState();


    private final BlockVector3 originDelegate;
    private final Clipboard clipboard;
    private final CuboidRegion cuboidRegion;


    public WorldEditStructure(Location origin, String localSchemPath) {
        super(origin, localSchemPath);
        Preconditions.checkArgument(localSchemPath.endsWith(".schem") || localSchemPath.endsWith(".schematic"), "Illegal schematic format: " + localSchemPath);

        File file = SCHEMATIC_DIR.resolve(localSchemPath).toFile();

        Preconditions.checkArgument(file.exists(), "Schematic file does not exist at path: " + file.getAbsolutePath());

        this.originDelegate = BlockVector3.at(origin.getBlockX(), origin.getBlockY(),  origin.getBlockZ());
        this.clipboard = this.loadClipboard(file);
        this.cuboidRegion = this.computePastedRegion(clipboard, origin);
    }

    @Override
    public void paste() {
        this.pasteAsync();
    }

    @Override
    public void remove() {
        this.removeAsync();
    }


    public CompletableFuture<Void> pasteAsync() {
        CompletableFuture<Void> future = new CompletableFuture<>();
        Executors.runAsync(() -> {
            com.sk89q.worldedit.world.World weWorld = BukkitAdapter.adapt(origin.getWorld());

            try (EditSession editSession = WorldEdit.getInstance().newEditSession(weWorld)) {
                Operation op = new ClipboardHolder(clipboard)
                        .createPaste(editSession)
                        .to(originDelegate)
                        .ignoreAirBlocks(true)
                        .copyBiomes(false)
                        .build();

                Operations.complete(op);
                editSession.flushQueue();
                future.complete(null);
            } catch (Throwable throwable) {
                Logging.errorLog("Failed to paste structure schematic at " + origin, throwable);
            }
        });
        return future;
    }

    public CompletableFuture<Void> removeAsync() {
        CompletableFuture<Void> future = new CompletableFuture<>();
        Executors.runAsync(() -> {
            try (EditSession session = WorldEdit.getInstance().newEditSession(cuboidRegion.getWorld())) {
                session.setBlocks((Region) cuboidRegion, AIR);
                session.flushQueue();
                future.complete(null);
            } catch (Throwable throwable) {
                Logging.errorLog("Failed to remove structure schematic at " + origin, throwable);
            }
        });
        return future;
    }

    public WorldTiedBoundingBox getBoundingBox() {
        BlockVector3 clipMin = cuboidRegion.getMinimumPoint();
        BlockVector3 clipMax = cuboidRegion.getMaximumPoint();
        World world = origin.getWorld();


        return new WorldTiedBoundingBox(
                world,
                clipMin.x(),
                clipMin.y(),
                clipMin.z(),
                clipMax.x(),
                clipMax.y(),
                clipMax.z()
        );
    }

    private Clipboard loadClipboard(File file)  {
        ClipboardFormat format = ClipboardFormats.findByFile(file);
        if (format == null) throw new IllegalArgumentException("Unknown schematic format: " + file.getName());

        try (ClipboardReader reader = format.getReader(new FileInputStream(file))) {
            return reader.read();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private CuboidRegion computePastedRegion(Clipboard clipboard, Location pasteAt) {
        World bw = pasteAt.getWorld();
        Preconditions.checkNotNull(bw, "pasteAt world is null");
        com.sk89q.worldedit.world.World weWorld = BukkitAdapter.adapt(bw);

        BlockVector3 clipMin = clipboard.getRegion().getMinimumPoint();
        BlockVector3 clipMax = clipboard.getRegion().getMaximumPoint();
        BlockVector3 clipOrigin = clipboard.getOrigin();

        BlockVector3 base = BlockVector3.at(pasteAt.getBlockX(), pasteAt.getBlockY(), pasteAt.getBlockZ());

        BlockVector3 worldMin = base.add(clipMin.subtract(clipOrigin));
        BlockVector3 worldMax = base.add(clipMax.subtract(clipOrigin));

        return new CuboidRegion(weWorld, worldMin, worldMax);
    }

}
