package dev.lumas.events.games.logic.mobarena;

import com.google.common.base.Preconditions;
import dev.lumas.events.EventMain;
import dev.lumas.events.configurable.sectors.MobArenaDefinition;
import dev.lumas.events.games.interfaces.Minigame;
import dev.lumas.events.games.interfaces.models.MinigameRole;
import dev.lumas.events.games.interfaces.models.MinigameRoleMap;
import dev.lumas.events.model.EventPlayer;
import dev.lumas.events.utility.Executors;
import dev.lumas.events.utility.Util;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import lombok.Getter;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.block.Block;
import org.bukkit.damage.DamageSource;
import org.bukkit.damage.DamageType;
import org.bukkit.entity.AbstractSkeleton;
import org.bukkit.entity.ComplexEntityPart;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.AreaEffectCloudApplyEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.entity.ExplosionPrimeEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.PotionSplashEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemDamageEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

// TODO: Extract commons, test, add MM mobs, etc.
public final class MobArenaRewrite extends Minigame {
    public static final int WAVE_COUNT = 3;
    private static final int LAUNCH_TRACK_TICKS = 60;
    private static final double WALL_HIT_MIN_SPEED = 0.6;
    private static final double WALL_HIT_DAMAGE_PER_SPEED = 8.0;
    private static final double WALL_HIT_MAX_DAMAGE = 20.0;
    @Getter
    private final MobArenaDefinition definition;
    private final Location spawnLocation;
    private final Location altar;
    private final MinigameRoleMap<ArenaPlayer> players = new MinigameRoleMap<>();
    private final Map<UUID, Soul> souls = new ConcurrentHashMap<>();
    private final BossBar bar = BossBar.bossBar(Component.empty(), 1, BossBar.Color.RED, BossBar.Overlay.PROGRESS);
    private volatile WaveState wave;
    private volatile long nextWaveAt;

    public MobArenaRewrite(MobArenaDefinition def) {
        super("Mob Arena", "Clear three waves of wither skeletons and bosses!", Long.MAX_VALUE, 10, true, true, false, true);
        Preconditions.checkState(def.getSpawnLocation() != null, "Mob Arena needs a spawn location");
        Preconditions.checkState(def.getHotspots() != null && !def.getHotspots().isEmpty(), "Mob Arena needs mob spawn points");
        Preconditions.checkState(def.getRegion().getLoc1() != null && def.getRegion().getLoc2() != null, "Mob Arena needs a region");
        this.definition = def;
        this.boundingBox = def.getRegion().toWorldTiedBoundingBox();
        this.spawnLocation = def.getSpawnLocation();
        this.altar = Objects.requireNonNullElse(def.getReviveLocation(), this.spawnLocation);
        Preconditions.checkState(this.boundingBox.contains(this.spawnLocation) && this.boundingBox.contains(this.altar), "Spawn and altar must be inside the arena");
        for (Location hotspot : def.getHotspots()) {
            Preconditions.checkState(this.boundingBox.contains(hotspot), "Every mob spawn point must be inside the arena");
        }
    }

    @Override
    protected int minimumParticipants() {
        return 1;
    }

    @Override
    protected boolean handleParticipantJoin(EventPlayer player) {
        this.teleportOnJoin(player, this.spawnLocation);
        return true;
    }

    @Override
    protected void handleStart() {
        for (EventPlayer player : this.participants) {
            this.players.put(new Fighter(player));
        }
        this.nextWaveAt = System.currentTimeMillis() + Math.max(0, this.definition.getStartCountdownSeconds()) * 1000L;
        this.audience.showBossBar(this.bar);
    }

    @Override
    protected void onRunnable(long timeLeft) {
        if (this.stopping) return;

        for (ArenaPlayer role : this.players) {
            role.operatePlayer(player -> this.tickPlayer(role, player));
        }

        if (this.players.getMatching(Fighter.class).isEmpty()) {
            this.finish(false);
            return;
        }
        this.tickSouls();
        long now = System.currentTimeMillis();
        WaveState current = this.wave;
        if (this.nextWaveAt > 0) {
            this.bar.name(Util.color("<red>Next wave in <white>" + Math.max(0, (this.nextWaveAt - now + 999) / 1000) + "s"));
            if (now >= this.nextWaveAt) {
                this.startWave(current == null ? 1 : current.number + 1, now);
            }
        } else if (current != null) {
            boolean cleared;
            synchronized (current) {
                if (now >= current.deadline) {
                    current.remaining = 0;
                }
                if (now >= current.nextBatch) {
                    current.nextBatch = now + Math.max(1, this.definition.getSpawnIntervalSeconds()) * 1000L;
                    for (int i = 0; i < current.batchSize && current.remaining > 0; i++) {
                        List<Location> points = this.definition.getHotspots();
                        Location at = points.get(current.nextHotspot++ % points.size());
                        if (!this.spawn(MobType.roll(current.number, RANDOM).spec(false), at, null)) {
                            break;
                        }
                        current.remaining--;
                    }
                }
                int left = current.remaining + current.pending + current.mobs.size();
                this.bar.name(Util.color("<red>Wave " + current.number + "/" + WAVE_COUNT + " <gray>• <white>" + left + " enemies remaining"));
                this.bar.progress(Math.clamp(1 - left / (float) current.total, 0, 1));
                cleared = left == 0;
            }

            if (cleared) {
                if (current.number == WAVE_COUNT) {
                    this.finish(true);
                } else {
                    this.nextWaveAt = now + Math.max(1, this.definition.getIntermissionSeconds()) * 1000L;
                    this.sendAudienceTitle("<green>Wave cleared!", "<gray>Bring any fallen souls to the altar");
                }
            }
        }
    }

    private void startWave(int number, long now) {
        if (this.stopping) {
            return;
        }
        int count = Math.max(1, this.players.size());
        List<Integer> quotas = this.definition.getBaseQuotaPerWave();
        int base = quotas == null || quotas.isEmpty() ? 30 : quotas.get(Math.min(number - 1, quotas.size() - 1));
        WaveState current = new WaveState(number, count, base, this.definition.getSpawnWindowSeconds(),
                this.definition.getSpawnIntervalSeconds(), now);
        this.wave = current;
        this.nextWaveAt = 0;
        List<BossType> types = new ArrayList<>();
        for (BossType type : BossType.values()) {
            if (type.canSpawn(number, this.definition.isAllowEnderDragon())) {
                types.add(type);
            }
        }
        List<Location> points = new ArrayList<>(this.definition.getHotspots());
        Collections.shuffle(types, RANDOM);
        Collections.shuffle(points, RANDOM);
        for (int i = 0; i < current.bossCount; i++) {
            this.spawn(types.get(i % types.size()).spec(current.bossHealthMultiplier), points.get(i % points.size()), null);
        }
        this.sendAudienceTitle("<red>Wave " + number + "/" + WAVE_COUNT, "");
    }

    private void finish(boolean won) {
        this.sendAudienceTitle(won ? "<gold>Victory!" : "<red>Defeat", won ? "<gray>All waves cleared" : "<gray>Everyone has fallen");
        this.stop();
    }

    @Override
    protected void handleStop() {
        WaveState current = this.wave;
        if (current != null) {
            synchronized (current) {
                current.mobs.values().stream().toList().forEach(it -> this.removeMob(it, true));
            }
        }
        this.souls.clear();
        for (ArenaPlayer role : this.players) {
            role.operatePlayer(player -> {
                role.restore();
                if (this.getGameDropOffLocation() != null) {
                    Executors.teleportSafely(player, this.getGameDropOffLocation());
                }
            });
        }
        this.players.clear();
        if (this.audience != null) {
            this.audience.hideBossBar(this.bar);
        }
    }

    @Override
    public boolean removeParticipant(EventPlayer participant, boolean teleport) {
        ArenaPlayer role = this.players.remove(participant.getUuid());
        this.souls.remove(participant.getUuid());
        if (role != null) {
            Location at = Objects.requireNonNullElse(role.getLastLocation(), this.spawnLocation);
            this.dropCarriedSouls(participant.getUuid(), at);
            role.restore();
        }
        return super.removeParticipant(participant, teleport);
    }

    private boolean spawn(ArenaMob.Specification specification, Location anchor, @Nullable ArenaMob summoner) {
        WaveState current = this.wave;
        if (current == null) {
            return false;
        }
        boolean regular = !specification.rank().isBoss();
        synchronized (current) {
            if (this.stopping || this.nextWaveAt > 0 || current != this.wave) {
                return false;
            }

            long alive = current.mobs.values().stream().filter(mob -> !mob.getSpecification().rank().isBoss()).count();

            if (regular && alive + current.pendingRegular >= Math.max(1, this.definition.getMaxAliveMobs())) {
                return false;
            }
            current.pending++;
            if (regular) {
                current.pendingRegular++;
            }
            if (summoner != null) {
                summoner.getSummons().incrementAndGet();
            }
        }
        Executors.runSync(anchor, () -> {
            boolean spawned = false;
            synchronized (current) {
                try {
                    if (this.stopping || current != this.wave || summoner != null && !summoner.isActive()) {
                        return;
                    }
                    ArenaMob mob;
                    if (specification.rank().isBoss()) {
                        mob = new ArenaBoss(this, specification, anchor);
                    } else {
                        mob = new ArenaMob(this, specification, anchor, summoner);
                    }
                    if (!mob.spawn()) {
                        return;
                    }
                    current.mobs.put(mob.getEntity().getUniqueId(), mob);
                    spawned = mob.start();
                    if (!spawned) {
                        current.mobs.remove(mob.getEntity().getUniqueId());
                        mob.close(true);
                    } else if (specification.rank().isBoss()) {
                        this.sendAudienceMessage(specification.coloredName() + " <red>has appeared!");
                    }
                } finally {
                    if (!spawned) {
                        if (summoner != null) {
                            summoner.getSummons().decrementAndGet();
                        } else if (regular && System.currentTimeMillis() < current.deadline) {
                            current.remaining++;
                        }
                        if (!regular) {
                            EventMain.getInstance().getLogger().warning("[MobArena] Could not spawn " + specification.name() + " at " + anchor);
                        }
                    }
                    if (regular) {
                        current.pendingRegular--;
                    }
                    current.pending--;
                }
            }
        });
        return true;
    }

    public void spawnSummons(ArenaMob summoner, Location at, int count) {
        for (int i = 0; i < count && summoner.getSummons().get() < 6 && this.mob(summoner.getEntity()) == summoner; i++) {
            if (!this.spawn(MobType.BASIC.spec(true), at, summoner)) {
                break;
            }
        }
    }

    public Collection<ArenaMob> mobs() {
        return this.wave == null ? List.of() : this.wave.mobs.values();
    }

    public ArenaMob mob(@Nullable Entity entity) {
        if (entity instanceof ComplexEntityPart part) {
            entity = part.getParent();
        }
        return entity == null || this.wave == null ? null : this.wave.mobs.get(entity.getUniqueId());
    }

    public void removeMob(ArenaMob mob, boolean remove) {
        WaveState current = this.wave;
        if (current == null) {
            return;
        }
        synchronized (current) {
            if (!current.mobs.remove(mob.getEntity().getUniqueId(), mob)) {
                return;
            }
            if (mob.getSummoner() != null) {
                mob.getSummoner().getSummons().decrementAndGet();
            }
            mob.close(remove);
        }
    }

    public boolean cleaningUpWave() {
        WaveState current = this.wave;
        if (current == null) {
            return false;
        }
        synchronized (current) {
            return current.remaining == 0 && current.pending == 0 && current.mobs.size() < this.definition.getStaleMobMaxRemaining();
        }
    }

    private void tickPlayer(ArenaPlayer role, Player player) {
        if (this.stopping || this.players.get(player.getUniqueId()) != role) {
            return;
        }
        role.tick(player);
        if (!this.boundingBox.contains(role.getLastLocation())) {
            Executors.teleportSafely(player, this.spawnLocation);
        }
    }


    private void dropCarriedSouls(UUID carrier, Location at) {
        this.souls.replaceAll((owner, soul) -> {
            if (soul.isCarriedBy(carrier)) {
                return soul.drop(at);
            }
            return soul;
        });
    }

    private void tickSouls() {
        if (this.stopping) {
            return;
        }
        this.altar.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, this.altar, 12, 1.5, 0.2, 1.5, 0);
        for (Soul soul : this.souls.values()) {
            if (soul.isCarried()) {
                this.tickCarriedSoul(soul);
            } else {
                this.tickGroundedSoul(soul);
            }
        }
    }

    private void tickGroundedSoul(Soul soul) {
        Nearby near = this.nearestFighter(soul.getLocation(), this.definition.getSoulPickupRadius());
        if (near == null) {
            soul.showParticles();
            return;
        }
        if (this.souls.replace(soul.getOwner(), soul, soul.pickUp(near.uuid()))) {
            Fighter carrier = this.players.as(near.uuid(), Fighter.class);
            if (carrier != null) {
                carrier.getEventPlayer().sendMessage("<aqua>Soul picked up! <gray>Take it to the green revive altar.");
            }
        }
    }

    private void tickCarriedSoul(Soul soul) {
        ArenaPlayer carrier = this.players.get(soul.getCarrier());
        if (!(carrier instanceof Fighter)) {
            Location at = carrier == null ? this.spawnLocation : carrier.getLastLocation();
            this.souls.replace(soul.getOwner(), soul, soul.drop(Objects.requireNonNullElse(at, this.spawnLocation)));
            return;
        }
        Location at = carrier.getLastLocation();
        double radius = this.definition.getReviveRadius();
        if (at != null && Objects.equals(at.getWorld(), this.altar.getWorld()) && at.distanceSquared(this.altar) <= radius * radius) {
            if (this.souls.remove(soul.getOwner(), soul)) {
                Downed downed = this.players.as(soul.getOwner(), Downed.class);
                if (downed != null) {
                    downed.revive();
                }
            }
        } else {
            carrier.getEventPlayer().sendActionBar("<aqua>Carrying a soul <gray>• Return to the green revive altar");
        }
    }


    public List<Nearby> fightersNear(Location center, double radius) {
        List<Nearby> nearby = new ArrayList<>();
        for (Fighter fighter : this.players.getMatching(Fighter.class)) {
            Location at = fighter.getLastLocation();
            if (at == null || !Objects.equals(at.getWorld(), center.getWorld())) {
                continue;
            }
            double distance = at.distanceSquared(center);
            if (distance <= radius * radius) {
                nearby.add(new Nearby(fighter.getUuid(), at, distance));
            }
        }
        nearby.sort(Comparator.comparingDouble(Nearby::distanceSquared));
        return nearby;
    }

    public Nearby nearestFighter(Location center, double radius) {
        return this.fightersNear(center, radius).stream().findFirst().orElse(null);
    }

    public void hurtFighter(Mob source, UUID uuid, double damage, Vector knockback, @Nullable Consumer<Player> extra) {
        Fighter fighter = this.players.as(uuid, Fighter.class);
        if (fighter == null) {
            return;
        }
        fighter.operatePlayer(player -> {
            if (this.mob(source) == null) {
                return;
            }
            player.damage(damage, DamageSource.builder(DamageType.MOB_ATTACK).withCausingEntity(source).withDirectEntity(source).build());
            player.setVelocity(player.getVelocity().add(knockback));
            if (extra != null) {
                extra.accept(player);
            }
        });
    }

    void launch(Player player, Vector velocity) {
        Fighter fighter = this.players.as(player.getUniqueId(), Fighter.class);
        if (fighter != null) {
            fighter.launch(player, velocity);
        }
    }

    private double damageMultiplier() {
        WaveState current = this.wave;
        return current == null ? 1 : current.damageMultiplier;
    }

    public void hurtFightersNear(Mob source, Location at, double radius, double damage, double push, double lift, @Nullable Consumer<Player> extra) {
        for (Nearby near : this.fightersNear(at, radius)) {
            Vector away = near.at().toVector().subtract(at.toVector()).setY(0);
            if (away.lengthSquared() < 0.001) {
                away = new Vector(1, 0, 0);
            }
            this.hurtFighter(source, near.uuid(), damage, away.normalize().multiply(push).setY(lift), extra);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onDeath(PlayerDeathEvent event) {
        this.ensureNotIllegal();
        Fighter fighter = this.players.as(event.getPlayer().getUniqueId(), Fighter.class);
        if (fighter != null) {
            event.setCancelled(true);
            event.setKeepInventory(true);
            event.setKeepLevel(true);
            event.getDrops().clear();
            event.setDroppedExp(0);
            event.deathMessage(null);
            event.setReviveHealth(Preconditions.checkNotNull(event.getEntity().getAttribute(Attribute.MAX_HEALTH), "max health").getValue());
            fighter.fall();
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onMobDeath(EntityDeathEvent event) {
        this.ensureNotIllegal();
        ArenaMob mob = this.mob(event.getEntity());
        if (mob == null) {
            return;
        }
        event.getDrops().clear();
        event.setDroppedExp(0);
        this.removeMob(mob, false);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        this.ensureNotIllegal();

        Entity victim = event.getEntity();
        Entity attacker = event.getDamageSource().getCausingEntity();

        if (this.players.is(victim, Downed.class) || this.players.is(attacker, Downed.class)) {
            event.setCancelled(true);
            return;
        }

        ArenaMob victimMob = this.mob(victim);
        ArenaMob attackerMob = this.mob(attacker);

        if (victimMob != null && attackerMob != null) {
            event.setCancelled(true);
            return;
        }
        if (victimMob != null) {
            if (this.players.is(attacker, Fighter.class)) {
                victimMob.hit();
            }
            event.setDamage(event.getDamage() * victimMob.getDamageTakenMultiplier());
            return;
        }
        if (attackerMob != null && victim instanceof Player player && this.players.is(player, Fighter.class)) {
            event.setDamage(event.getDamage() * this.wave.damageMultiplier);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onTarget(EntityTargetEvent event) {
        this.ensureNotIllegal();
        ArenaMob mob = this.mob(event.getEntity());
        if (mob != null && event.getTarget() != null && !this.players.is(event.getTarget(), Fighter.class)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onExplosionPrime(ExplosionPrimeEvent event) {
        this.ensureNotIllegal();
        ArenaMob mob = this.mob(event.getEntity());
        MobType type = mob == null ? null : mob.getSpecification().mobType();
        if (type != null && type.explode(this, mob)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInteract(PlayerInteractEvent event) {
        this.ensureNotIllegal();
        if (this.players.is(event.getPlayer(), Downed.class)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onArmorDamage(PlayerItemDamageEvent event) {
        this.ensureNotIllegal();
        if (this.players.containsKey(event.getPlayer().getUniqueId()) && Arrays.asList(event.getPlayer().getInventory().getArmorContents()).contains(event.getItem())) {
            event.setCancelled(true);
        }
    }

    private boolean arenaSourced(Entity entity) {
        return this.mob(entity) != null || entity instanceof Projectile shot && shot.getShooter() instanceof Entity shooter && this.mob(shooter) != null;
    }

    @EventHandler
    public void onPotion(PotionSplashEvent event) {
        this.ensureNotIllegal();
        if (this.arenaSourced(event.getEntity())) {
            event.getAffectedEntities().stream().filter(entity -> this.mob(entity) != null || this.players.is(entity, Downed.class)).forEach(entity -> event.setIntensity(entity, 0));
        }
    }

    @EventHandler
    public void onCloud(AreaEffectCloudApplyEvent event) {
        this.ensureNotIllegal();
        if (event.getEntity().getSource() instanceof Entity source && this.mob(source) != null) {
            event.getAffectedEntities().removeIf(entity -> this.mob(entity) != null || this.players.is(entity, Downed.class));
        }
    }

    public record Nearby(UUID uuid, Location at, double distanceSquared) {
    }

    static final class WaveState {
        final int number;
        final int bossCount;
        final double bossHealthMultiplier;
        final double damageMultiplier;
        final int total;
        final int batchSize;
        final long deadline;
        final Map<UUID, ArenaMob> mobs = new ConcurrentHashMap<>();
        int remaining;
        int pending;
        int pendingRegular;
        int nextHotspot;
        long nextBatch;

        WaveState(int number, int players, int baseQuota, int spawnSeconds, int intervalSeconds, long now) {
            this.number = number;
            int participants = Math.max(1, players);
            this.bossCount = number < WAVE_COUNT ? Math.max(1, participants / 10) : 1;
            this.bossHealthMultiplier = 1 + 0.25 * Math.max(0, participants / (double) this.bossCount - 1);
            this.damageMultiplier = 1 + 0.15 * Math.clamp(number - 1, 0, WAVE_COUNT - 1);
            this.remaining = Math.toIntExact((long) Math.max(1, baseQuota) * participants);
            this.total = Math.addExact(this.remaining, this.bossCount);
            int seconds = Math.max(1, spawnSeconds);
            int batches = Math.max(1, seconds / Math.max(1, intervalSeconds));
            this.deadline = now + seconds * 1000L;
            this.nextBatch = now + 1500;
            this.batchSize = Math.max(1, (int) Math.ceil(this.remaining / (double) batches));
        }
    }

    private abstract class ArenaPlayer extends MinigameRole {

        @Getter
        private volatile Location lastLocation;

        protected ArenaPlayer(EventPlayer eventPlayer) {
            super(eventPlayer);
        }

        protected ArenaPlayer(ArenaPlayer previous) {
            super(previous.eventPlayer);
            this.lastLocation = previous.lastLocation;
        }

        public final void tick(Player player) {
            this.rememberPlayer(player);
            this.handleTick(player);
        }

        public void restore() {
            this.operatePlayer(player -> {
                player.hideBossBar(bar);
                for (ArenaMob mob : mobs()) {
                    if (mob instanceof ArenaBoss boss) {
                        player.hideBossBar(boss.getHealthBar());
                    }
                }
            });
        }

        protected final void rememberPlayer(Player player) {
            this.lastLocation = player.getLocation();
        }

        protected abstract void handleTick(Player player);
    }

    private final class Fighter extends ArenaPlayer {

        @Nullable
        private ScheduledTask launch;

        public Fighter(EventPlayer eventPlayer) {
            super(eventPlayer);
        }

        public void launch(Player player, Vector velocity) {
            Executors.runSync(player, () -> {
                player.setVelocity(velocity);
                if (this.launch != null) {
                    this.launch.cancel();
                }
                this.launch = player.getScheduler().runAtFixedRate(EventMain.getInstance(), new Launch(player), null, 1, 1);
            });
        }

        @Override
        protected void handleTick(Player player) {
            player.setFlying(false);
            player.setAllowFlight(false);
        }

        public void fall() {
            if (stopping) { // TODO: maybe remove
                return;
            }

            Player player = this.getEventPlayer().getPlayer();
            Preconditions.checkState(player != null, "null player");

            this.rememberPlayer(player);
            players.swapRole(this, () -> new Downed(this));

            Location currentlyAt = player.getLocation();
            if (!boundingBox.contains(currentlyAt) || currentlyAt.getBlock().isLiquid()) {
                currentlyAt = spawnLocation;
            }
            dropCarriedSouls(this.getUuid(), currentlyAt);
            souls.put(this.getUuid(), new Soul(this.getUuid(), currentlyAt));

            sendAudienceMessage("<red>" + this.getName() + " fell! <gray>Carry their soul particles to the revive altar.");
        }
    }

    private final class Launch implements Consumer<ScheduledTask> {
        private final Player player;
        private Location previous;
        private Vector heading = new Vector();
        private double previousSpeed;
        private int ticks;

        private Launch(Player player) {
            this.player = player;
            this.previous = player.getLocation();
        }

        @Override
        public void accept(ScheduledTask task) {
            if (stopping || !players.is(this.player, Fighter.class) || ++this.ticks > LAUNCH_TRACK_TICKS) {
                task.cancel();
                return;
            }
            Location at = this.player.getLocation();
            Vector moved = at.toVector().subtract(this.previous.toVector()).setY(0);
            double speed = moved.length();

            if (this.previousSpeed >= WALL_HIT_MIN_SPEED && speed < this.previousSpeed * 0.4) {
                Block wall = this.wallAhead(at);
                if (wall != null) {
                    this.slam(at, wall, this.previousSpeed - speed);
                    task.cancel();
                    return;
                }
            }
            if (this.ticks > 5 && this.player.isOnGround() && speed < 0.1) {
                task.cancel();
                return;
            }
            if (speed > 0.05) {
                this.heading = moved.multiply(1 / speed);
            }
            this.previous = at;
            this.previousSpeed = speed;
        }

        @Nullable
        private Block wallAhead(Location at) {
            for (double height : new double[]{0.2, 1.2}) {
                Block block = this.solid(at, this.heading.getX(), height, this.heading.getZ());
                if (block == null && Math.abs(this.heading.getX()) > 0.2) {
                    block = this.solid(at, Math.signum(this.heading.getX()), height, 0);
                }
                if (block == null && Math.abs(this.heading.getZ()) > 0.2) {
                    block = this.solid(at, 0, height, Math.signum(this.heading.getZ()));
                }
                if (block != null) {
                    return block;
                }
            }
            return null;
        }

        @Nullable
        private Block solid(Location at, double x, double y, double z) {
            Block block = at.clone().add(x * 0.6, y, z * 0.6).getBlock();
            return block.isPassable() ? null : block;
        }

        private void slam(Location at, Block wall, double speedLost) {
            double damage = Math.min(WALL_HIT_MAX_DAMAGE, speedLost * WALL_HIT_DAMAGE_PER_SPEED) * damageMultiplier();
            this.player.damage(damage, DamageSource.builder(DamageType.FLY_INTO_WALL).build());
            Location impact = at.clone().add(this.heading.clone().multiply(0.4)).add(0, 1, 0);
            this.player.getWorld().spawnParticle(Particle.BLOCK, impact, 40, 0.3, 0.6, 0.3, 0, wall.getBlockData());
            this.player.getWorld().playSound(impact, Sound.ENTITY_PLAYER_BIG_FALL, 1.5f, 0.7f);
            this.player.getWorld().playSound(impact, Sound.BLOCK_ANVIL_LAND, 0.6f, 0.6f);
        }
    }

    private final class Downed extends ArenaPlayer {

        private static final PotionEffect RESISTANCE = new PotionEffect(PotionEffectType.RESISTANCE, 60, 4);

        private Downed(Fighter previous) {
            super(previous);
            previous.operatePlayer(player -> {
                player.setFireTicks(0);
                player.setFreezeTicks(0);
                player.clearActivePotionEffects();
            });
        }

        @Override
        protected void handleTick(Player player) {
        }

        public void revive() {
            players.swapRole(this, () -> new Fighter(this.getEventPlayer()));
            this.operatePlayer(player -> {
                player.setHealth(Preconditions.checkNotNull(player.getAttribute(Attribute.MAX_HEALTH), "max health").getValue());
                player.addPotionEffect(RESISTANCE);
                player.teleportAsync(altar);
                sendAudienceMessage("<green>" + player.getName() + " has been revived!");
                //this.tick(player);
            });
        }
    }

    @Getter
    public static final class Soul {

        private final UUID owner;
        private final Location location;
        @Nullable
        private final UUID carrier;

        public Soul(UUID owner, Location location) {
            this(owner, location, null);
        }

        private Soul(UUID owner, Location location, @Nullable UUID carrier) {
            this.owner = Objects.requireNonNull(owner);
            this.location = location.clone();
            this.carrier = carrier;
        }

        public Location getLocation() {
            return this.location.clone();
        }

        public boolean isCarried() {
            return this.carrier != null;
        }

        public boolean isCarriedBy(UUID player) {
            return player.equals(this.carrier);
        }

        public Soul pickUp(UUID carrier) {
            return new Soul(this.owner, this.location, Objects.requireNonNull(carrier));
        }

        public Soul drop(Location location) {
            return new Soul(this.owner, location);
        }

        public void showParticles() {
            if (this.isCarried()) {
                return;
            }
            Location at = this.location.clone().add(0, 1, 0);
            Executors.runSync(at, () -> {
                at.getWorld().spawnParticle(Particle.SOUL, at, 8, 0.3, 0.5, 0.3, 0.02);
            });
        }
    }

    @Getter
    public static class ArenaMob {
        protected final MobArenaRewrite ctx;
        protected final Specification specification;
        protected final Location anchor;
        private final @Nullable ArenaMob summoner;
        private final double damageTakenMultiplier;
        private final AtomicInteger summons = new AtomicInteger();
        protected Mob entity;
        private ScheduledTask task;
        protected volatile Location lastLocation;
        private volatile long lastHitAt = System.currentTimeMillis();

        public ArenaMob(MobArenaRewrite ctx, Specification specification, Location anchor, @Nullable ArenaMob summoner) {
            this.ctx = ctx;
            this.specification = specification;
            this.anchor = anchor;
            this.summoner = summoner;
            this.damageTakenMultiplier = Math.min(1, 1024 / specification.health());
        }

        public boolean spawn() {
            Location at = this.findSpot();
            if (at == null) {
                return false;
            }
            Class<? extends Entity> type = this.specification.type().getEntityClass();
            if (type == null || !Mob.class.isAssignableFrom(type)) {
                return false;
            }
            this.entity = (Mob) at.getWorld().spawn(at, type, spawned -> this.prepare((Mob) spawned));
            this.lastLocation = at;
            if (this.entity.isValid()) {
                return true;
            }
            this.entity.remove();
            return false;
        }

        protected Location findSpot() {
            World world = this.anchor.getWorld();
            double spawnRadius = Math.max(0, this.ctx.definition.getHotspotSpawnRadius());
            for (int attempt = 0; attempt <= 8; attempt++) {
                boolean fallback = attempt == 8;
                double angle = RANDOM.nextDouble() * Math.PI * 2;
                double radius = fallback ? 0 : RANDOM.nextDouble() * spawnRadius;
                int x = (int) Math.floor(this.anchor.getX() + Math.cos(angle) * radius);
                int z = (int) Math.floor(this.anchor.getZ() + Math.sin(angle) * radius);
                if (!world.isChunkLoaded(x >> 4, z >> 4) || !Bukkit.isOwnedByCurrentRegion(world, x >> 4, z >> 4)) {
                    continue;
                }
                for (int y = this.anchor.getBlockY() + 3; y >= this.anchor.getBlockY() - 3; y--) {
                    Location spot = new Location(world, x + 0.5, y, z + 0.5);
                    if (!this.ctx.boundingBox.contains(spot) || y <= world.getMinHeight() || y + this.specification.height() >= world.getMaxHeight()) {
                        continue;
                    }
                    Block floor = world.getBlockAt(x, y - 1, z);
                    if (floor.isPassable() || floor.isLiquid()) {
                        continue;
                    }
                    boolean clear = true;
                    for (int h = 0; h < this.specification.height(); h++) {
                        Block block = world.getBlockAt(x, y + h, z);
                        if (!block.isPassable() || block.isLiquid()) {
                            clear = false;
                            break;
                        }
                    }
                    if (!clear) {
                        continue;
                    }
                    if (!fallback && !this.specification.rank().isBoss() && (!this.ctx.fightersNear(spot, 4).isEmpty() || spot.distanceSquared(this.ctx.altar) <= 16)) {
                        continue;
                    }
                    return spot;
                }
            }
            return null;
        }

        public boolean start() {
            this.task = this.entity.getScheduler().runAtFixedRate(EventMain.getInstance(), ignored -> this.tick(),
                    () -> this.ctx.removeMob(this, false), 1, 10);
            return this.task != null;
        }

        public void close(boolean remove) {
            if (this.task != null) {
                this.task.cancel();
            }
            if (remove) {
                Executors.runSync(this.entity, this.entity::remove);
            }
        }

        public void hit() {
            this.lastHitAt = System.currentTimeMillis();
        }

        public boolean isActive() {
            return !this.ctx.isStopping() && this.ctx.mob(this.entity) == this;
        }

        public void buff(PotionEffect... effects) {
            Executors.runSync(this.entity, () -> {
                if (this.isActive()) {
                    for (PotionEffect effect : effects) {
                        this.entity.addPotionEffect(effect);
                    }
                }
            });
        }

        protected void prepare(Mob mob) {
            mob.setPersistent(false);
            mob.setRemoveWhenFarAway(false);
            mob.setCanPickupItems(false);
            if (this.specification.mobType() != null) {
                this.specification.mobType().prepare(mob);
            }
            for (EquipmentSlot slot : EquipmentSlot.values()) {
                if (slot == EquipmentSlot.SADDLE) {
                    continue;
                }
                mob.getEquipment().setDropChance(slot, 0);
            }
            Map<Attribute, Double> attributes = Map.of(
                    Attribute.MAX_HEALTH, Math.min(1024, this.specification.health()),
                    Attribute.FOLLOW_RANGE, 64.0,
                    Attribute.KNOCKBACK_RESISTANCE, this.specification.rank().isBoss() ? 0.8 : 0.25,
                    Attribute.SCALE, this.specification.scale()
            );

            for (Map.Entry<Attribute, Double> entry : attributes.entrySet()) {
                AttributeInstance attribute = mob.getAttribute(entry.getKey());
                if (attribute != null) {
                    attribute.setBaseValue(entry.getValue());
                }
            }
            mob.setHealth(Math.min(1024, this.specification.health()));
            AttributeInstance attack = mob.getAttribute(Attribute.ATTACK_DAMAGE);
            if (attack != null && this.specification.attackDamage() > 0) {
                attack.setBaseValue(this.specification.attackDamage());
            }
            AttributeInstance speed = mob.getAttribute(Attribute.MOVEMENT_SPEED);
            if (speed != null) {
                speed.setBaseValue(speed.getBaseValue() * (1 + this.specification.speedBonus()));
            }

            if (mob instanceof AbstractSkeleton skeleton) {
                skeleton.setShouldBurnInDay(false);
            }
        }

        private void tick() {
            if (!this.isActive() || !this.entity.isValid() || this.entity.isDead()) {
                return;
            }
            long now = System.currentTimeMillis();
            this.lastLocation = this.entity.getLocation();
            boolean cleanup = this.ctx.cleaningUpWave();
            this.entity.setGlowing(cleanup);
            if (cleanup && now - this.lastHitAt > (this.specification.rank().isBoss() ? 120_000 : 45_000)) {
                this.ctx.removeMob(this, true);
                return;
            }
            this.handleTick(now);
        }

        protected void handleTick(long now) {
            Location at = this.lastLocation;
            if (!this.ctx.boundingBox.contains(at)) {
                this.entity.teleportAsync(this.anchor);
                return;
            }
            Player target = this.entity.getTarget() instanceof Player player && this.ctx.players.is(player, Fighter.class) ? player : null;
            if (target == null) {
                MobArenaRewrite.Nearby near = this.ctx.nearestFighter(at, 64);
                if (near != null) {
                    Fighter fighter = this.ctx.players.as(near.uuid(), Fighter.class);
                    if (fighter != null) {
                        target = fighter.getEventPlayer().getPlayer();
                    }
                }
            }
            if (target == null) {
                this.entity.setTarget(null);
                return;
            }
            this.entity.setTarget(target);
            Location targetAt = target.getLocation();
            if (!at.getWorld().equals(targetAt.getWorld())) {
                return;
            }
            if (this.entity.getTarget() == null && this.specification.type() != EntityType.WITHER) {
                this.entity.getPathfinder().moveTo(targetAt, 1);
            }
            this.attack(target, targetAt, now);
        }

        protected void attack(Player target, Location targetAt, long now) {
        }

        public enum Rank {
            NORMAL,
            SUMMON,
            MINI_BOSS,
            BOSS;

            public boolean isBoss() {
                return this == MINI_BOSS || this == BOSS;
            }
        }

        public record Specification(
            Rank rank,
            EntityType type,
            String name,
            String color,
            double health,
            double speedBonus,
            double scale,
            double attackDamage,
            int height,
            List<Ability> abilities,
            @Nullable BossType boss,
            @Nullable MobType mobType
        ) {
            public String coloredName() {
                return "<" + this.color + "><b>" + this.name + "</b>";
            }
        }
    }
}
