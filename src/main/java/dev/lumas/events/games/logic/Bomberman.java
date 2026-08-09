package dev.lumas.events.games.logic;

import com.google.common.base.Preconditions;
import dev.lumas.core.util.PluginContextLogger;
import dev.lumas.events.EventMain;
import dev.lumas.events.configurable.sectors.BombermanDefinition;
import dev.lumas.events.games.constants.MinigameConstant;
import dev.lumas.events.games.interfaces.InventoryUnifiedMinigame;
import dev.lumas.events.games.interfaces.Scorer;
import dev.lumas.events.games.interfaces.models.MinigameRole;
import dev.lumas.events.games.interfaces.models.MinigameRoleMap;
import dev.lumas.events.games.models.CountdownBossBar;
import dev.lumas.events.games.models.Scoreboard;
import dev.lumas.events.games.tokenformula.BombermanTokenFormula;
import dev.lumas.events.model.EventPlayer;
import dev.lumas.events.model.WorldTiedBoundingBox;
import dev.lumas.events.utility.Couple;
import dev.lumas.events.utility.Executors;
import dev.lumas.events.utility.Util;
import io.papermc.paper.event.entity.EntityKnockbackEvent;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import lombok.Getter;
import lombok.Setter;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class Bomberman extends InventoryUnifiedMinigame {

    private static final String[] SPLASH = {
        "Super!",
        "Kaboom",
        "Blow it up!",
        "// TODO: Splash Text"
    };

    private static final PluginContextLogger LOGGER = PluginContextLogger.getPluginLogger();

    private static final long ROUND_DURATION = 240000L;
    private static final int TNT_FUSE_TICKS = 30;
    private static final long BOMB_LOCK_MILLIS = (TNT_FUSE_TICKS * 50L) + 1000L;
    private static final int BLAST_RADIUS = 2;
    private static final double ALWAYS_VISIBLE_RADIUS = 5;
    private static final double[] BOX_EDGE_OFFSETS = {-0.3, 0.3};
    private static final int KILL_SCORE = 20;
    private static final int ROUND_WIN_SCORE = 30;
    private static final int ROUNDS = 3;
    private static final float GAME_OVER_SECONDS = 10;
    private static final int RESCUE_HEIGHT = 32;
    private static final long DURATION = (ROUND_DURATION + (long) (GAME_OVER_SECONDS * 1000)) * ROUNDS;

    private static final Color[] BOMB_COLORS = pastelPalette(50);

    private final Arena arena;
    private final Location spawnLocation;
    private final MinigameRoleMap<AbstractBombermanPlayer> roles = new MinigameRoleMap<>(AbstractBombermanPlayer::cleanup);
    private final Scoreboard<EventPlayer> scoreboard = new Scoreboard<>();
    private BombermanTokenFormula tokenFormula;

    private CountdownBossBar timerBar;
    private final Set<UUID> roundWinners = ConcurrentHashMap.newKeySet();
    private volatile int round;
    private volatile int startingPlayers;
    private volatile boolean roundLive;
    private volatile boolean ending;

    public Bomberman(BombermanDefinition def) {
        super("Bomberman", Util.getRandom(SPLASH), DURATION, 5, true, true, false, false);
        this.spawnLocation = def.getSpawnLocation();
        this.arena = new Arena(def.getArenaOrigin());
        this.boundingBox = this.arena.maxBounds();
    }

    @Override
    protected int minimumParticipants() {
        return 1;
    }
    
    private void stripKit(EventPlayer participant) {
        Player target = participant.getPlayer();
        if (target == null) return;
        Executors.sync(target, () -> {
            target.getInventory().remove(Material.TNT);
            target.getInventory().remove(Material.DIAMOND_PICKAXE);
            target.getInventory().remove(Material.DIAMOND_AXE);
            target.getInventory().remove(Material.COOKED_PORKCHOP);
        });
    }

    private void equip(EventPlayer participant) {
        if (this.ending) return;
        Player target = participant.getPlayer();
        if (target == null) return;
        Executors.sync(target, () -> {
            if (this.ending) return;
            Player player = target;
            player.getInventory().clear();
            player.clearActivePotionEffects();
            player.setFlying(false);
            player.setAllowFlight(false);
            player.setFireTicks(0);
            AttributeInstance maxHealth = player.getAttribute(Attribute.MAX_HEALTH);
            player.setHealth(maxHealth == null ? 20.0 : maxHealth.getValue());
            player.getInventory().setItem(0, BombermanPlayer.TNT);
            player.getInventory().setItem(1, ItemStack.of(Material.DIAMOND_PICKAXE));
            player.getInventory().setItem(2, ItemStack.of(Material.DIAMOND_AXE));
            player.getInventory().setItem(3, ItemStack.of(Material.COOKED_PORKCHOP, 64));
            player.getInventory().setHeldItemSlot(0);
            player.addPotionEffect(BombermanPlayer.MINING_FATIGUE);
        });
    }

    @Override
    protected boolean handleParticipantJoin(EventPlayer player) {
        player.teleportAsync(this.spawnLocation);
        return super.handleParticipantJoin(player);
    }

    @Override
    public boolean removeParticipant(EventPlayer participant, boolean doTeleport) {
        AbstractBombermanPlayer role = this.roles.get(participant.getUuid());
        if (role != null) {
            role.cleanup();
            this.roles.remove(participant.getUuid());
        }
        return super.removeParticipant(participant, doTeleport);
    }

    @Override
    protected void handleStart() {
        startRound();
    }

    private void startRound() {
        if (this.ending) return;
        this.round++;
        this.arena.teardown(() -> {
            if (this.ending) return;
            this.arena.generate(Math.max(1, this.participants.size()), () -> {
            if (this.ending) {
                this.arena.teardown();
                return;
            }
            List<Location> spawns = this.arena.spawns();

            for (AbstractBombermanPlayer previous : this.roles.values()) {
                previous.cleanup();
            }
            this.roles.clear();

            int spawnIndex = 0;
            for (EventPlayer eventPlayer : this.participants) {
                this.roles.put(new BombermanPlayer(eventPlayer, this, BOMB_COLORS[spawnIndex % BOMB_COLORS.length]));
                equip(eventPlayer);
                eventPlayer.teleportAsync(spawns.get(spawnIndex % spawns.size()));
                spawnIndex++;
            }

            this.timerBar = CountdownBossBar.builder()
                    .title("<red><b>Round " + this.round + "/" + ROUNDS + "</b></red> <gray>|</gray> <yellow>%ss remaining")
                    .color(BossBar.Color.RED)
                    .miliseconds(ROUND_DURATION)
                    .audience(this.audience)
                    .callback(this::endRound)
                    .build();
            this.timerBar.start();

            this.startingPlayers = this.roles.getMatching(BombermanPlayer.class).size();
            this.scoreboard.addScorers(List.copyOf(this.participants));
            if (this.tokenFormula == null) {
                int fairShare = Math.max(1, this.arena.totalDestructible() / Math.max(1, this.startingPlayers));
                int perRound = (fairShare * 2) + ((this.startingPlayers - 1) * KILL_SCORE);
                this.tokenFormula = new BombermanTokenFormula(perRound * ROUNDS);
            }
            this.sendAudienceTitle("<red><b>Round " + this.round, "<gray>Last one standing wins");
            this.playAudienceSound(Sound.ENTITY_ENDER_DRAGON_GROWL, 0.2f, 1.4f);
            this.roundLive = true;
            });
        });
    }

    @Override
    protected void tokenHandler(EventPlayer participant) {
        int score = this.scoreboard.getScore(participant);
        if (this.tokenFormula != null) {
            this.tokenFormula.giveTokens(participant, Couple.of(score, this.roundWinners.contains(participant.getUuid())));
        } else {
            LOGGER.error("Null token formula");
        }
        participant.addPermanentScore(MinigameConstant.BOMBERMAN, score);
    }

    @Override
    protected void onRunnable(long timeLeft) {
        for (AbstractBombermanPlayer role : this.roles.values()) {
            role.tick();
        }
        updateVisibility();
        if (this.arena.isReady()) {
            int remaining = (int) Math.round(this.arena.remainingFraction() * 100.0);
            Component actionBar = Util.color("<gray>Map remaining: <yellow>" + remaining + "%");
            for (EventPlayer participant : this.participants) {
                participant.operatePlayer(player -> player.sendActionBar(actionBar));
            }
        }
        if (!this.roundLive || this.ending) return;
        int alive = this.roles.getMatching(BombermanPlayer.class).size();
        if (alive == 0 || (this.startingPlayers > 1 && alive == 1)) {
            endRound();
        }
    }

    private void endRound() {
        if (!this.roundLive || this.ending) return;
        this.roundLive = false;

        if (this.timerBar != null) {
            this.timerBar.stop(false);
        }

        List<BombermanPlayer> alive = this.roles.getMatching(BombermanPlayer.class);
        String outcome;
        if (alive.size() == 1) {
            BombermanPlayer winner = alive.getFirst();
            this.roundWinners.add(winner.getUuid());
            this.scoreboard.addScore(winner.getEventPlayer(), ROUND_WIN_SCORE);
            outcome = "<yellow>" + winner.getName() + " wins the round!";
        } else {
            outcome = "<gray>Nobody survived";
        }
        this.playAudienceSound(Sound.ENTITY_LIGHTNING_BOLT_IMPACT, 1.0f, 2f);

        boolean lastRound = this.round >= ROUNDS;
        String header = lastRound ? "<red><b>Game Over" : "<red><b>Round " + this.round + "/" + ROUNDS;

        Runnable interlude = () -> {
            this.timerBar = CountdownBossBar.builder()
                    .title(header + "</b></red> <gray>|</gray> " + outcome)
                    .color(BossBar.Color.WHITE)
                    .seconds(GAME_OVER_SECONDS)
                    .audience(this.audience)
                    .callback(lastRound ? this::stop : this::startRound)
                    .build();
            this.timerBar.start();
        };

        if (lastRound) {
            this.scoreboard.handleGameEnd(this.audience, interlude);
        } else {
            interlude.run();
        }
    }

    @Override
    protected void handleStop() {
        if (this.ending) return;
        this.ending = true;
        this.roundLive = false;
        this.round = ROUNDS;

        if (this.timerBar != null) {
            this.timerBar.stop(false);
            this.timerBar = null;
        }
        for (AbstractBombermanPlayer role : this.roles.values()) {
            role.cleanup();
            role.teleportAsync(this.spawnLocation); // TODO temporary
        }
        this.roles.clear();

        for (EventPlayer participant : this.participants) {
            stripKit(participant);
        }
        this.arena.teardown();
    }

    private void updateVisibility() {
        List<BombermanPlayer> alive = this.roles.getMatching(BombermanPlayer.class);
        int count = alive.size();
        if (count < 2) return;

        Player[] players = new Player[count];
        Location[] positions = new Location[count];
        for (int i = 0; i < count; i++) {
            players[i] = alive.get(i).getEventPlayer().getPlayer();
            positions[i] = players[i] == null ? null : players[i].getLocation();
        }

        for (int i = 0; i < count; i++) {
            if (players[i] == null) continue;
            for (int j = i + 1; j < count; j++) {
                if (players[j] == null) continue;
                boolean visible = canSee(positions[i], positions[j]);
                applyVisibility(alive.get(i), players[i], players[j], visible);
                applyVisibility(alive.get(j), players[j], players[i], visible);
            }
        }
    }

    private boolean canSee(Location from, Location to) {
        if (from.getWorld() != to.getWorld()) return false;
        if (from.distanceSquared(to) <= ALWAYS_VISIBLE_RADIUS * ALWAYS_VISIBLE_RADIUS) return true;
        if (this.arena.hasClearPath(from.getX(), from.getZ(), to.getX(), to.getZ())) return true;
        for (double dx : BOX_EDGE_OFFSETS) {
            for (double dz : BOX_EDGE_OFFSETS) {
                if (this.arena.hasClearPath(from.getX(), from.getZ(), to.getX() + dx, to.getZ() + dz)) return true;
            }
        }
        return false;
    }

    private void applyVisibility(BombermanPlayer viewerRole, Player viewer, Player target, boolean visible) {
        boolean hidden = viewerRole.getHidden().contains(target.getUniqueId());
        if (visible == !hidden) return;
        if (visible) {
            viewerRole.getHidden().remove(target.getUniqueId());
            Executors.runSync(viewer, () -> viewer.showPlayer(EventMain.getInstance(), target));
        } else {
            viewerRole.getHidden().add(target.getUniqueId());
            Executors.runSync(viewer, () -> viewer.hidePlayer(EventMain.getInstance(), target));
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        this.ensureNotIllegal();
        Player player = event.getPlayer();
        AbstractBombermanPlayer role = this.roles.get(player.getUniqueId());
        if (!(role instanceof BombermanPlayer bomber)) {
            if (role != null) event.setCancelled(true);
            return;
        }

        event.setCancelled(true);
        Block block = event.getBlock();
        if (event.getBlockPlaced().getType() != Material.TNT || !this.arena.contains(block.getLocation())) {
            return;
        }

        if (bomber.isBombLive()) {
            player.playSound(player, Sound.BLOCK_NOTE_BLOCK_BASS, 0.7f, 0.6f);
            player.sendActionBar(Util.color("<red>Wait for your bomb to go off!"));
            return;
        }

        TNTPrimed tnt = block.getWorld().spawn(block.getLocation().toCenterLocation(), TNTPrimed.class);
        tnt.setFuseTicks(TNT_FUSE_TICKS);
        tnt.setSource(player);
        bomber.lockBomb(BOMB_LOCK_MILLIS);

        player.playSound(player, Sound.ENTITY_TNT_PRIMED, 1.0f, 1.2f);
        BombermanPlayer.refillTnt(player);
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onExplode(EntityExplodeEvent event) {
        this.ensureNotIllegal();
        if (!(event.getEntity() instanceof TNTPrimed tnt)) return;

        BombermanPlayer owner = tnt.getSource() instanceof Player source
                && this.roles.get(source.getUniqueId()) instanceof BombermanPlayer bomber ? bomber : null;
        if (owner != null) owner.releaseBomb();

        if (!this.arena.contains(event.getLocation())) return;

        event.setYield(0.0f);
        int centerX = event.getLocation().getBlockX();
        int centerZ = event.getLocation().getBlockZ();
        int broken = 0;
        for (Block block : List.copyOf(event.blockList())) {
            int dx = block.getX() - centerX;
            int dz = block.getZ() - centerZ;
            if (dx * dx + dz * dz > BLAST_RADIUS * BLAST_RADIUS) continue;
            if (this.arena.destroy(block.getX(), block.getZ())) broken++;
        }
        event.blockList().clear();
        if (broken > 0 && owner != null) {
            this.scoreboard.addScore(owner.getEventPlayer(), broken);
        }

        burst(event.getLocation(), owner == null ? Color.WHITE : owner.getColor());
    }

    /** Golden-angle hues so consecutive players never get near-identical colors. */
    private static Color[] pastelPalette(int count) {
        Color[] palette = new Color[count];
        for (int i = 0; i < count; i++) {
            palette[i] = pastel((float) ((i * 0.6180339887498949) % 1.0));
        }
        return palette;
    }

    private static Color pastel(float hue) {
        float saturation = 0.45f;
        float sector = hue * 6.0f;
        int index = (int) sector;
        float offset = sector - index;
        float p = 1.0f - saturation;
        float q = 1.0f - saturation * offset;
        float t = 1.0f - saturation * (1.0f - offset);
        float r, g, b;
        switch (index % 6) {
            case 0 -> { r = 1.0f; g = t; b = p; }
            case 1 -> { r = q; g = 1.0f; b = p; }
            case 2 -> { r = p; g = 1.0f; b = t; }
            case 3 -> { r = p; g = q; b = 1.0f; }
            case 4 -> { r = t; g = p; b = 1.0f; }
            default -> { r = 1.0f; g = p; b = q; }
        }
        return Color.fromRGB(Math.round(r * 255), Math.round(g * 255), Math.round(b * 255));
    }

    private void burst(Location location, Color color) {
        Firework firework = location.getWorld().spawn(location, Firework.class, spawned -> {
            FireworkMeta meta = spawned.getFireworkMeta();
            meta.addEffect(FireworkEffect.builder()
                    .withColor(color)
                    .with(FireworkEffect.Type.BALL)
                    .build());
            meta.setPower(0);
            spawned.setFireworkMeta(meta);
        });
        firework.detonate();
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        this.ensureNotIllegal();
        if (!(event.getEntity() instanceof Player player)) return;
        if (this.roles.get(player.getUniqueId()) instanceof BombermanSpectator) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onDamageByEntity(EntityDamageByEntityEvent event) {
        this.ensureNotIllegal();
        if (event.getDamager() instanceof Firework) {
            event.setCancelled(true);
            return;
        }
        if (!(event.getEntity() instanceof Player victim)) return;
        if (!this.roles.containsKey(victim.getUniqueId())) return;

        if (event.getDamager() instanceof Player) {
            event.setCancelled(true);
            return;
        }
        if (event.getDamager() instanceof TNTPrimed tnt
                && tnt.getSource() instanceof Player source
                && source.getUniqueId().equals(victim.getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onDropItem(PlayerDropItemEvent event) {
        this.ensureNotIllegal();
        if (isParticipant(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        this.ensureNotIllegal();
        Player player = event.getPlayer();
        AbstractBombermanPlayer role = this.roles.get(player.getUniqueId());
        if (role == null) return;

        if (!(role instanceof BombermanPlayer)) {
            event.setCancelled(true);
            return;
        }

        Block block = event.getBlock();
        if (this.arena.typeAt(block) == Arena.Cell.DESTRUCTIBLE) {
            if (this.arena.noteMined(block.getX(), block.getZ(), block.getY())) {
                this.scoreboard.addScore(role.getEventPlayer(), 1);
            }
        } else {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onKnockback(EntityKnockbackEvent event) {
        this.ensureNotIllegal();
        if (event.getCause() != EntityKnockbackEvent.Cause.EXPLOSION) return;
        if (event.getEntity() instanceof Player player && this.roles.containsKey(player.getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        this.ensureNotIllegal();
        AbstractBombermanPlayer role = this.roles.get(event.getEntity().getUniqueId());
        if (role == null) return;
        role.death(event);
    }

    private static boolean fellOffMap(Player player) {
        EntityDamageEvent last = player.getLastDamageCause();
        if (last == null) return false;
        EntityDamageEvent.DamageCause cause = last.getCause();
        return cause == EntityDamageEvent.DamageCause.FALL || cause == EntityDamageEvent.DamageCause.VOID;
    }

    private static void survive(PlayerDeathEvent event) {
        Player player = event.getEntity();
        event.setCancelled(true);
        event.getDrops().clear();
        AttributeInstance maxHealth = player.getAttribute(Attribute.MAX_HEALTH);
        player.setHealth(maxHealth == null ? 20.0 : maxHealth.getValue());
        player.setFireTicks(0);
    }

    public abstract static class AbstractBombermanPlayer extends MinigameRole implements Scorer {

        protected final Bomberman context;

        public AbstractBombermanPlayer(EventPlayer eventPlayer, Bomberman context) {
            super(eventPlayer);
            this.context = context;
        }

        public abstract void tick();
        public abstract void cleanup();
        public abstract void death(PlayerDeathEvent event);

        @Override
        public boolean equals(Object other) {
            return other instanceof AbstractBombermanPlayer role && role.getUuid().equals(getUuid());
        }

        @Override
        public int hashCode() {
            return getUuid().hashCode();
        }
    }

    @Getter
    @Setter
    public static class BombermanPlayer extends AbstractBombermanPlayer {

        private static final PotionEffect MINING_FATIGUE = new PotionEffect(PotionEffectType.MINING_FATIGUE, 200, 1, false, false, false);
        private static final Map<Attribute, AttributeModifier> ATTRIBUTES = Map.of(
            Attribute.JUMP_STRENGTH, new AttributeModifier(new NamespacedKey(EventMain.getInstance(), "jump_strength"), -20.0, AttributeModifier.Operation.ADD_NUMBER)
        );
        private static final ItemStack TNT = ItemStack.of(Material.TNT, 64);


        private final Color color;
        private final Set<UUID> hidden = ConcurrentHashMap.newKeySet();
        private volatile long bombLiveUntil;

        public BombermanPlayer(EventPlayer eventPlayer, Bomberman context, Color color) {
            super(eventPlayer, context);
            this.color = color;

            operatePlayer(player -> {
                for (Map.Entry<Attribute, AttributeModifier> entry : ATTRIBUTES.entrySet()) {
                    AttributeInstance attributeInstance = player.getAttribute(entry.getKey());
                    Preconditions.checkNotNull(attributeInstance, "Attribute %s not found for player %s", entry.getKey(), player.getName());
                    attributeInstance.addTransientModifier(entry.getValue());
                }
            });
        }

        public boolean isBombLive() {
            return System.currentTimeMillis() < this.bombLiveUntil;
        }

        public void lockBomb(long millis) {
            this.bombLiveUntil = System.currentTimeMillis() + millis;
        }

        public void releaseBomb() {
            this.bombLiveUntil = 0L;
        }

        static void refillTnt(Player player) {
            player.getInventory().setItemInMainHand(TNT.clone());
        }

        @Override
        public void tick() {
            operatePlayer(player -> {
                player.addPotionEffect(MINING_FATIGUE);
            });
        }

        @Override
        public void death(PlayerDeathEvent event) {
            survive(event);
            Player player = event.getEntity();
            boolean fellOff = fellOffMap(player);
            Location restAt = fellOff ? this.context.arena.centre(RESCUE_HEIGHT) : player.getLocation();

            Player killer = player.getKiller();
            if (fellOff) {
                this.context.sendAudienceMessage("<red>" + getName() + "</red> fell off the map!");
            } else if (killer != null && !killer.getUniqueId().equals(player.getUniqueId())
                    && this.context.roles.get(killer.getUniqueId()) instanceof BombermanPlayer bomber) {
                this.context.scoreboard.addScore(bomber.getEventPlayer(), KILL_SCORE);
                this.context.sendAudienceMessage("<red>" + getName() + "</red> was blown up by <green>" + bomber.getName() + "</green>!");
            } else {
                this.context.sendAudienceMessage("<red>" + getName() + "</red> blew themselves up!");
            }

            player.teleportAsync(restAt);
            this.context.roles.swapRole(this, () -> new BombermanSpectator(getEventPlayer(), this.context, restAt));
            this.context.playAudienceSound(Sound.ENTITY_PLAYER_LEVELUP, 0.5f, 0.5f);
            if (!fellOff) player.getWorld().strikeLightningEffect(restAt);
        }

        @Override
        public void cleanup() {
            operatePlayer(player -> {
                for (Map.Entry<Attribute, AttributeModifier> entry : ATTRIBUTES.entrySet()) {
                    AttributeInstance attributeInstance = player.getAttribute(entry.getKey());
                    Preconditions.checkNotNull(attributeInstance, "Attribute %s not found for player %s", entry.getKey(), player.getName());
                    attributeInstance.removeModifier(entry.getValue());
                }
                player.removePotionEffect(PotionEffectType.MINING_FATIGUE);
                for (UUID id : this.hidden) {
                    Player other = Bukkit.getPlayer(id);
                    if (other != null) player.showPlayer(EventMain.getInstance(), other);
                }
                this.hidden.clear();
            });
        }
    }

    public static class BombermanSpectator extends AbstractBombermanPlayer {

        private static final PotionEffect INVISIBILITY = new PotionEffect(PotionEffectType.INVISIBILITY, 300, 0, false, false, false);

        private final Location deathLocation;
        private volatile boolean released;

        public BombermanSpectator(EventPlayer eventPlayer, Bomberman context, Location deathLocation) {
            super(eventPlayer, context);
            this.deathLocation = deathLocation;
            operatePlayer(player -> {
                player.getInventory().clear();
                player.setAllowFlight(true);
                player.setFlying(true);
                player.addPotionEffect(INVISIBILITY);
                Location loc = player.getLocation().clone().add(0, 1, 0);
                player.teleportAsync(loc);
            });
            setVisibleToOthers(false);
        }

        private void setVisibleToOthers(boolean visible) {
            Player player = this.eventPlayer.getPlayer();
            if (player == null) return;
            for (EventPlayer participant : this.context.getParticipants()) {
                if (participant.getUuid().equals(this.eventPlayer.getUuid())) continue;
                participant.operatePlayer(other -> {
                    if (visible) {
                        other.showPlayer(EventMain.getInstance(), player);
                    } else {
                        other.hidePlayer(EventMain.getInstance(), player);
                    }
                });
            }
        }

        @Override
        public void tick() {
            if (this.released) return;
            operatePlayer(player -> {
                if (this.released) return;
                player.addPotionEffect(INVISIBILITY);
                if (!player.getAllowFlight()) player.setAllowFlight(true);
            });
        }

        @Override
        public void death(PlayerDeathEvent event) {
            survive(event);
            operatePlayer(player -> {
                player.teleportAsync(this.deathLocation);
                player.setAllowFlight(true);
                player.setFlying(true);
            });
        }

        @Override
        public void cleanup() {
            this.released = true;
            setVisibleToOthers(true);
            operatePlayer(player -> {
                player.setFlying(false);
                player.setAllowFlight(false);
                player.removePotionEffect(PotionEffectType.INVISIBILITY);
                player.removePotionEffect(PotionEffectType.MINING_FATIGUE);
            });
        }
    }

    public static final class Arena {

        private static final String[] TEMPLATE = {
                "DDPNPDDPNPDDPNPDD",
                "PONOPODONODOPONOP",
                "PNNPNDDPNPDDNPNNP",
                "NOPOXODONODOXOPON",
                "PPNXPPPPNPPPPXNPP",
                "DODOPOPOXOPOPODOD",
                "DDDDPPDNPNDPPDDDD",
                "POPOPONONONOPOPOP",
                "NNNNNXPNPNPXNNNNN",
                "POPOPONONONOPOPOP",
                "DDDDPPDNPNDPPDDDD",
                "DODOPOPOXOPOPODOD",
                "PPNXPPPPNPPPPXNPP",
                "NOPOXODONODOXOPON",
                "PNNPNDDPNPDDNPNNP",
                "PONOPODONODOPONOP",
                "DDPNPDDPNPDDPNPDD",
        };

        private static final int TEMPLATE_SIZE = TEMPLATE.length;
        private static final int MIRROR_PERIOD = (TEMPLATE_SIZE - 1) * 2;

        private static final char LATTICE_PILLAR = 'O';
        private static final char DECOR_PILLAR = 'X';

        public enum Cell {
            OUTSIDE,
            OPEN,
            DESTRUCTIBLE,
            PILLAR
        }

        // Widths must be 17, 21, 25 ...(multiple of 4) or the pillar lattice lands on the wrong parity.
        private static final int MIN_SIZE = 32;
        private static final int MAX_SIZE = 238;
        private static final double CELLS_PER_PLAYER = 120.0;
        private static final double MIN_SPAWN_SPACING = 18.0;
        private static final int SPAWN_CLEARANCE = 2;
        private static final int PILLAR_FREE_RADIUS = 2;
        private static final int PILLAR_HEIGHT = 8;
        private static final int SOFT_HEIGHT = 7;
        private static final int BLOCKS_PER_TICK = 20000;

        private static final BlockData FLOOR = Material.BEDROCK.createBlockData();
        private static final BlockData PILLAR = Material.OBSIDIAN.createBlockData();

        private static final List<Map<Character, BlockData>> SOFT_PALETTES = List.of(
                softPalette(Material.NETHERRACK, Material.OAK_PLANKS, Material.DIRT),
                softPalette(Material.CRIMSON_NYLIUM, Material.CRIMSON_PLANKS, Material.SOUL_SOIL),
                softPalette(Material.WARPED_NYLIUM, Material.WARPED_PLANKS, Material.SOUL_SAND),
                softPalette(Material.RED_SAND, Material.BIRCH_PLANKS, Material.SANDSTONE),
                softPalette(Material.SNOW_BLOCK, Material.SPRUCE_PLANKS, Material.PACKED_ICE),
                softPalette(Material.MYCELIUM, Material.JUNGLE_PLANKS, Material.PODZOL),
                softPalette(Material.SCULK, Material.DARK_OAK_PLANKS, Material.ROOTED_DIRT)
        );

        private static Map<Character, BlockData> softPalette(Material n, Material p, Material d) {
            return Map.of('N', n.createBlockData(), 'P', p.createBlockData(), 'D', d.createBlockData());
        }

        private final Location origin;

        private Map<Character, BlockData> soft = SOFT_PALETTES.getFirst();
        private boolean[][] pillarRemoved;
        private volatile int totalDestructible;
        private volatile int remainingDestructible;
        private World world;
        private int size;
        private int minX;
        private int minZ;
        private int floorY;
        private boolean[][] opened;
        private List<Location> spawns = List.of();
        @Getter
        private volatile boolean ready;
        private ScheduledTask buildTask;

        public Arena(Location origin) {
            this.origin = origin;
        }

        public void generate(int playerCount, Runnable onReady) {
            if (this.origin == null || this.origin.getWorld() == null) {
                LOGGER.error("arenaOrigin is not configured, cannot generate an arena.");
                return;
            }
            playerCount = Math.max(1, playerCount);

            int chosenSize = sizeForPlayers(playerCount);
            List<int[]> spawnCells = pickSpawnCells(chosenSize, playerCount);
            while (spawnCells.size() < playerCount && chosenSize < MAX_SIZE) {
                chosenSize = Math.min(MAX_SIZE, snapSize(chosenSize + 1));
                spawnCells = pickSpawnCells(chosenSize, playerCount);
            }
            if (spawnCells.size() < playerCount) {
                LOGGER.warning("only " + spawnCells.size() + " spawns fit " + playerCount
                        + " players at " + MIN_SPAWN_SPACING + " blocks apart, even at the maximum arena size; players will share spawns.");
            }

            this.soft = Util.getRandom(SOFT_PALETTES);
            this.world = this.origin.getWorld();
            this.size = chosenSize;
            this.minX = this.origin.getBlockX() - chosenSize / 2;
            this.minZ = this.origin.getBlockZ() - chosenSize / 2;
            this.floorY = this.origin.getBlockY();
            this.opened = new boolean[chosenSize][chosenSize];
            this.pillarRemoved = new boolean[chosenSize][chosenSize];
            this.ready = false;

            for (int[] cell : spawnCells) {
                clearPillarsAround(cell[0], cell[1]);
                for (int d = -SPAWN_CLEARANCE; d <= SPAWN_CLEARANCE; d++) {
                    open(cell[0] + d, cell[1]);
                    open(cell[0], cell[1] + d);
                }
            }

            this.totalDestructible = 0;
            for (int cx = 0; cx < chosenSize; cx++) {
                for (int cz = 0; cz < chosenSize; cz++) {
                    char symbol = symbolAt(cx, cz);
                    if (symbol != LATTICE_PILLAR && symbol != DECOR_PILLAR && !this.opened[cx][cz]) {
                        this.totalDestructible++;
                    }
                }
            }
            this.remainingDestructible = this.totalDestructible;

            List<Location> picked = new ArrayList<>(spawnCells.size());
            for (int[] cell : spawnCells) picked.add(facingCentre(cellCentre(cell[0], cell[1])));
            this.spawns = List.copyOf(picked);

            LOGGER.info("Generating arena for " + playerCount + " players: "
                    + chosenSize + "x" + chosenSize + " blocks, " + this.spawns.size() + " spawns.");

            stream(false, () -> {
                this.ready = true;
                LOGGER.info("Arena ready.");
                if (onReady != null) onReady.run();
            });
        }

        public void teardown() {
            teardown(null);
        }

        public void teardown(Runnable onCleared) {
            cancelBuild();
            if (this.world == null) {
                if (onCleared != null) onCleared.run();
                return;
            }
            this.ready = false;
            stream(true, () -> {
                LOGGER.info("Arena cleared.");
                if (onCleared != null) onCleared.run();
            });
        }

        public List<Location> spawns() {
            return this.spawns;
        }

        public int size() {
            return this.size;
        }

        public int floorY() {
            return this.floorY;
        }

        public boolean contains(Location location) {
            return location != null && location.getWorld() == this.world && contains(location.getBlockX(), location.getBlockZ());
        }

        public boolean contains(int worldX, int worldZ) {
            if (this.world == null) return false;
            int cx = worldX - minX;
            int cz = worldZ - minZ;
            return cx >= 0 && cz >= 0 && cx < size && cz < size;
        }

        public Cell typeAt(Location location) {
            if (location == null || location.getWorld() != this.world) return Cell.OUTSIDE;
            return typeAt(location.getBlockX(), location.getBlockZ());
        }

        public Cell typeAt(Block block) {
            return block == null ? Cell.OUTSIDE : typeAt(block.getX(), block.getZ());
        }

        public Cell typeAt(int worldX, int worldZ) {
            if (!contains(worldX, worldZ)) return Cell.OUTSIDE;
            int cx = worldX - minX;
            int cz = worldZ - minZ;
            if (isPillarCell(cx, cz)) return Cell.PILLAR;
            return this.opened[cx][cz] ? Cell.OPEN : Cell.DESTRUCTIBLE;
        }

        public boolean isPillar(int worldX, int worldZ) {
            return typeAt(worldX, worldZ) == Cell.PILLAR;
        }

        public boolean isPassable(int worldX, int worldZ) {
            return typeAt(worldX, worldZ) == Cell.OPEN;
        }

        public boolean destroy(Location location) {
            return location != null && location.getWorld() == this.world && destroy(location.getBlockX(), location.getBlockZ());
        }

        public boolean destroy(int worldX, int worldZ) {
            if (typeAt(worldX, worldZ) != Cell.DESTRUCTIBLE) return false;
            this.opened[worldX - minX][worldZ - minZ] = true;
            this.remainingDestructible--;
            for (int dy = 1; dy <= SOFT_HEIGHT; dy++) {
                this.world.getBlockAt(worldX, floorY + dy, worldZ).setType(Material.AIR, false);
            }
            return true;
        }

        public boolean hasClearPath(double fromX, double fromZ, double toX, double toZ) {
            if (this.world == null) return true;
            int cx = (int) Math.floor(fromX);
            int cz = (int) Math.floor(fromZ);
            int endX = (int) Math.floor(toX);
            int endZ = (int) Math.floor(toZ);
            if (cx == endX && cz == endZ) return true;

            double dx = toX - fromX;
            double dz = toZ - fromZ;
            int stepX = dx >= 0 ? 1 : -1;
            int stepZ = dz >= 0 ? 1 : -1;
            double deltaX = dx == 0 ? Double.MAX_VALUE : Math.abs(1.0 / dx);
            double deltaZ = dz == 0 ? Double.MAX_VALUE : Math.abs(1.0 / dz);
            double nextX = dx == 0 ? Double.MAX_VALUE
                    : (dx > 0 ? (cx + 1 - fromX) : (fromX - cx)) * Math.abs(1.0 / dx);
            double nextZ = dz == 0 ? Double.MAX_VALUE
                    : (dz > 0 ? (cz + 1 - fromZ) : (fromZ - cz)) * Math.abs(1.0 / dz);

            int guard = size * 4;
            while (guard-- > 0) {
                if (nextX < nextZ) {
                    nextX += deltaX;
                    cx += stepX;
                } else {
                    nextZ += deltaZ;
                    cz += stepZ;
                }
                if (cx == endX && cz == endZ) return true;
                if (typeAt(cx, cz) != Cell.OPEN && typeAt(cx, cz) != Cell.OUTSIDE) return false;
            }
            return false;
        }

        public boolean noteMined(int worldX, int worldZ, int brokenY) {
            if (typeAt(worldX, worldZ) != Cell.DESTRUCTIBLE) return false;
            for (int dy = 1; dy <= SOFT_HEIGHT; dy++) {
                int y = floorY + dy;
                if (y == brokenY) continue;
                if (!this.world.getBlockAt(worldX, y, worldZ).getType().isAir()) return false;
            }
            this.opened[worldX - minX][worldZ - minZ] = true;
            this.remainingDestructible--;
            return true;
        }

        public int totalDestructible() {
            return this.totalDestructible;
        }

        public double remainingFraction() {
            return this.totalDestructible <= 0 ? 1.0 : this.remainingDestructible / (double) this.totalDestructible;
        }

        public Location centre(int heightAboveFloor) {
            return new Location(world, minX + size / 2.0, floorY + heightAboveFloor, minZ + size / 2.0);
        }

        private Location facingCentre(Location location) {
            double dx = (minX + size / 2.0) - location.getX();
            double dz = (minZ + size / 2.0) - location.getZ();
            location.setYaw((float) Math.toDegrees(Math.atan2(-dx, dz)));
            location.setPitch(0.0f);
            return location;
        }

        public Location cellCentre(int cx, int cz) {
            return new Location(world, minX + cx + 0.5, floorY + 1, minZ + cz + 0.5);
        }

        public WorldTiedBoundingBox maxBounds() {
            int half = MAX_SIZE / 2 + 1;
            Location min = new Location(origin.getWorld(),
                    origin.getBlockX() - half, origin.getBlockY() - 1, origin.getBlockZ() - half);
            Location max = new Location(origin.getWorld(),
                    origin.getBlockX() + half, origin.getBlockY() + PILLAR_HEIGHT + 2, origin.getBlockZ() + half);
            return WorldTiedBoundingBox.of(min, max);
        }

        private static int snapSize(int n) {
            if (n < TEMPLATE_SIZE) return TEMPLATE_SIZE;
            int remainder = (n - 1) % 4;
            return remainder == 0 ? n : n + (4 - remainder);
        }

        private static int templateIndex(int i, int size) {
            int shifted = i + (TEMPLATE_SIZE / 2) - (size - 1) / 2;
            int wrapped = ((shifted % MIRROR_PERIOD) + MIRROR_PERIOD) % MIRROR_PERIOD;
            return wrapped >= TEMPLATE_SIZE ? MIRROR_PERIOD - wrapped : wrapped;
        }

        private char symbolAt(int cx, int cz) {
            return symbolAt(cx, cz, this.size);
        }

        private static char symbolAt(int cx, int cz, int size) {
            return TEMPLATE[templateIndex(cz, size)].charAt(templateIndex(cx, size));
        }

        private int sizeForPlayers(int playerCount) {
            double sideNeeded = Math.sqrt((playerCount * CELLS_PER_PLAYER) / 0.75);
            return Math.max(MIN_SIZE, Math.min(MAX_SIZE, snapSize((int) Math.ceil(sideNeeded))));
        }

        private void clearPillarsAround(int spawnX, int spawnZ) {
            for (int dx = -PILLAR_FREE_RADIUS; dx <= PILLAR_FREE_RADIUS; dx++) {
                for (int dz = -PILLAR_FREE_RADIUS; dz <= PILLAR_FREE_RADIUS; dz++) {
                    if (dx * dx + dz * dz > PILLAR_FREE_RADIUS * PILLAR_FREE_RADIUS) continue;
                    int cx = spawnX + dx;
                    int cz = spawnZ + dz;
                    if (cx < 0 || cz < 0 || cx >= size || cz >= size) continue;
                    char symbol = symbolAt(cx, cz);
                    if (symbol != LATTICE_PILLAR && symbol != DECOR_PILLAR) continue;
                    this.pillarRemoved[cx][cz] = true;
                    this.opened[cx][cz] = true;
                }
            }
        }

        private boolean isPillarCell(int cx, int cz) {
            if (this.pillarRemoved != null && this.pillarRemoved[cx][cz]) return false;
            char symbol = symbolAt(cx, cz);
            return symbol == LATTICE_PILLAR || symbol == DECOR_PILLAR;
        }

        private void open(int cx, int cz) {
            if (cx < 0 || cz < 0 || cx >= size || cz >= size) return;
            char symbol = symbolAt(cx, cz);
            if (symbol == LATTICE_PILLAR || symbol == DECOR_PILLAR) return;
            this.opened[cx][cz] = true;
        }

        private List<int[]> pickSpawnCells(int size, int count) {
            List<int[]> candidates = new ArrayList<>();
            for (int cx = 0; cx < size; cx += 2) {
                for (int cz = 0; cz < size; cz += 2) {
                    if (symbolAt(cx, cz, size) == DECOR_PILLAR) continue;
                    candidates.add(new int[]{cx, cz});
                }
            }

            double centre = (size - 1) / 2.0;
            candidates.sort((a, b) -> Double.compare(
                    Math.hypot(b[0] - centre, b[1] - centre),
                    Math.hypot(a[0] - centre, a[1] - centre)));

            List<int[]> chosen = new ArrayList<>(count);
            for (double spacing = size; spacing >= MIN_SPAWN_SPACING && chosen.size() < count; spacing -= 1) {
                for (int[] cell : candidates) {
                    if (chosen.size() >= count) break;
                    boolean farEnough = true;
                    for (int[] taken : chosen) {
                        if (Math.hypot(taken[0] - cell[0], taken[1] - cell[1]) < spacing) { farEnough = false; break; }
                    }
                    if (farEnough) chosen.add(cell);
                }
            }
            return chosen;
        }

        private BlockData blockAt(int x, int y, int z) {
            int cx = x - minX;
            int cz = z - minZ;
            char symbol = symbolAt(cx, cz);
            int above = y - floorY;
            boolean pillar = isPillarCell(cx, cz);

            if (above == 0) {
                return pillar && symbol == LATTICE_PILLAR ? PILLAR : FLOOR;
            }
            if (pillar) {
                return above < PILLAR_HEIGHT ? PILLAR : null;
            }
            if (this.opened[cx][cz]) return null;
            return above <= SOFT_HEIGHT ? this.soft.getOrDefault(symbol, PILLAR) : null;
        }

        private void stream(boolean clearing, Runnable onComplete) {
            int height = PILLAR_HEIGHT;
            int columnsPerTick = Math.max(1, BLOCKS_PER_TICK / height);
            int totalColumns = size * size;
            final int[] cursor = {0};
            Location anchor = new Location(world, minX, floorY, minZ);

            cancelBuild();
            this.buildTask = Executors.repeatingSync(anchor, 1L, task -> {
                int done = 0;
                while (cursor[0] < totalColumns && done < columnsPerTick) {
                    int index = cursor[0]++;
                    int x = minX + (index % size);
                    int z = minZ + (index / size);
                    for (int dy = 0; dy < height; dy++) {
                        int y = floorY + dy;
                        BlockData data = clearing ? null : blockAt(x, y, z);
                        if (data == null) {
                            world.getBlockAt(x, y, z).setType(Material.AIR, false);
                        } else {
                            world.getBlockAt(x, y, z).setBlockData(data, false);
                        }
                    }
                    done++;
                }
                if (cursor[0] >= totalColumns) {
                    task.cancel();
                    this.buildTask = null;
                    if (onComplete != null) onComplete.run();
                }
            });
        }

        private void cancelBuild() {
            ScheduledTask task = this.buildTask;
            if (task != null) {
                task.cancel();
                this.buildTask = null;
            }
        }

    }
}
