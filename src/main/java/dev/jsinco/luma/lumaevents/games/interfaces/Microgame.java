package dev.jsinco.luma.lumaevents.games.interfaces;

import dev.jsinco.luma.lumaevents.EventMain;
import dev.jsinco.luma.lumaevents.configurable.sectors.MicrogameMap;
import dev.jsinco.luma.lumaevents.games.exceptions.GameAlreadyStartedException;
import dev.jsinco.luma.lumaevents.games.exceptions.GameComponentIllegallyActive;
import dev.jsinco.luma.lumaevents.games.interfaces.structures.Structure;
import dev.jsinco.luma.lumaevents.obj.EventPlayer;
import dev.jsinco.luma.lumaevents.obj.WorldTiedBoundingBox;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;

import java.util.List;


// would love to extend Minigame here, but there's too much pollution
public abstract class Microgame implements Listener {

    protected final List<EventPlayer> eventPlayers;
    protected final Structure structure;
    protected final WorldTiedBoundingBox boundingBox;
    private final Runnable delegateOnEnd;
    private final long timeLimit;

    private long startTime = -1;
    private boolean active = false;
    private EventPlayer winner = null;


    public Microgame(List<EventPlayer> eventPlayers, Structure structure, Runnable onEnd, long timeLimit, int padding) {
        this.structure = structure;
        this.eventPlayers = eventPlayers;
        this.delegateOnEnd = onEnd;
        this.timeLimit = timeLimit;
        Location origin = structure.getOrigin();
        this.boundingBox = new WorldTiedBoundingBox(origin.getWorld(), origin.getX() - padding, origin.getY() - padding, origin.getZ() - padding, origin.getX() + padding, origin.getY() + padding, origin.getZ() + padding);
    }

    // child
    protected abstract void heartbeat(long remainder);


    public void begin() {
        if (this.active) {
            throw new GameAlreadyStartedException("Microgame is active!");
        }
        this.active = true;
        this.startTime = System.currentTimeMillis();
        this.registerEvents(this);

        this.structure.paste();

        // should probably delegate but oh well
        for (EventPlayer eventPlayer : this.eventPlayers) {
            eventPlayer.teleportAsync(this.structure.getOrigin().add(0, 1, 0));
        }
    }

    public void end(EventPlayer winner) {
        this.ensureActive();
        this.winner = winner;
        this.active = false;
        this.unregisterEvents(this);
        unsafe(this.delegateOnEnd); // assume this has teleports and such

        this.structure.remove();
    }


    public void doHeartBeat() {
        this.ensureActive();
        long timeLeft = this.timeLimit - (System.currentTimeMillis() - this.startTime);
        if (timeLeft <= 0) {
            this.end(null);
            return;
        }

        unsafe(() -> this.heartbeat(timeLeft));
    }


    // util

    protected void unsafe(Runnable block) {
        try {
            block.run();
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        }
    }

    protected void registerEvents(Listener listener) {
        Bukkit.getPluginManager().registerEvents(listener, EventMain.getInstance());
    }

    protected void unregisterEvents(Listener listener) {
        HandlerList.unregisterAll(listener);
    }

    protected void ensureActive() {
        if (!this.active) {
            throw new GameComponentIllegallyActive("Microgame is not active!");
        }
    }

    public static MicrogameBuilder builder() {
        return new MicrogameBuilder();
    }

    public static class MicrogameBuilder {

        private Location origin;
        private MicrogameMap map;
        private List<EventPlayer> eventPlayers;
        private Runnable onEnd;
        private long timeLimit;


        public MicrogameBuilder origin(Location origin) {
            this.origin = origin;
            return this;
        }

        public MicrogameBuilder map(MicrogameMap map) {
            this.map = map;
            return this;
        }

        public MicrogameBuilder eventPlayers(EventPlayer... eventPlayers) {
            this.eventPlayers = List.of(eventPlayers);
            return this;
        }

        public MicrogameBuilder onEnd(Runnable onEnd) {
            this.onEnd = onEnd;
            return this;
        }

        public MicrogameBuilder timeLimit(long timeLimit) {
            this.timeLimit = timeLimit;
            return this;
        }


        public <T extends Microgame> T create(Class<T> microgameClass) {
            Structure structure = new Structure(origin, map.getStructureName());
            try {
                // using reflection is so dumb here
                return microgameClass.getDeclaredConstructor(
                        List.class,
                        Structure.class,
                        Runnable.class,
                        long.class,
                        int.class
                ).newInstance(eventPlayers, structure, onEnd, timeLimit, map.getPadding());
            } catch (Exception e) {
                throw new RuntimeException("Failed to create microgame instance", e);
            }
        }
    }

}
