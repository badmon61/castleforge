package dev.castleforge.fabric;

import dev.castleforge.core.CastlePlanner;
import dev.castleforge.core.CultureMap;
import dev.castleforge.core.TerracedCastlePlanner;
import dev.castleforge.core.CastleIdentity;
import dev.castleforge.core.PlanDiagnostics;
import dev.castleforge.core.architecture.ReferenceCastlePlanner;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;

/** Explicit, operator-only test placement. No chunk-load/worldgen hooks and no client rendering mixins. */
public final class CastleForge implements ModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("castleforge");
    private static final int EDITS_PER_TICK = 256;
    private static final long TICK_BUDGET_NS = 2_000_000L;
    private static final long PREVIEW_LIFETIME_NS = 120_000_000_000L;
    private final Map<MinecraftServer, Session> sessions = new WeakHashMap<>();

    @Override public void onInitialize() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registry, environment) -> dispatcher.register(
            Commands.literal("castleforge")
                .requires(s -> s.permissions().hasPermission(Permissions.COMMANDS_MODERATOR))
                .executes(c -> say(c.getSource(), "CastleForge alpha: /castleforge region | preview [terraced|reference] [x z] | build confirm | inspect | status | cancel | undo. TEST WORLDS ONLY."))
                .then(Commands.literal("region").executes(c -> safely(c.getSource(), () -> region(c.getSource()))))
                .then(Commands.literal("preview")
                    .executes(c -> safely(c.getSource(), () -> preview(c.getSource(), null, null, false)))
                    .then(Commands.argument("x", IntegerArgumentType.integer(-29_999_800, 29_999_800))
                        .then(Commands.argument("z", IntegerArgumentType.integer(-29_999_800, 29_999_800))
                            .executes(c -> safely(c.getSource(), () -> preview(c.getSource(),
                                IntegerArgumentType.getInteger(c,"x"), IntegerArgumentType.getInteger(c,"z"), false)))))
                    .then(Commands.literal("terraced")
                        .executes(c -> safely(c.getSource(), () -> preview(c.getSource(), null, null, true)))
                        .then(Commands.argument("x", IntegerArgumentType.integer(-29_999_800, 29_999_800))
                            .then(Commands.argument("z", IntegerArgumentType.integer(-29_999_800, 29_999_800))
                                .executes(c -> safely(c.getSource(), () -> preview(c.getSource(),
                                    IntegerArgumentType.getInteger(c,"x"), IntegerArgumentType.getInteger(c,"z"), true))))))
                    .then(Commands.literal("reference")
                        .executes(c -> safely(c.getSource(), () -> previewReference(c.getSource(), null, null)))
                        .then(Commands.argument("x", IntegerArgumentType.integer(-29_999_800, 29_999_800))
                            .then(Commands.argument("z", IntegerArgumentType.integer(-29_999_800, 29_999_800))
                                .executes(c -> safely(c.getSource(), () -> previewReference(c.getSource(),
                                    IntegerArgumentType.getInteger(c,"x"), IntegerArgumentType.getInteger(c,"z"))))))))
                .then(Commands.literal("build").then(Commands.literal("confirm")
                    .executes(c -> safely(c.getSource(), () -> build(c.getSource())))))
                .then(Commands.literal("inspect").executes(c -> safely(c.getSource(), () -> inspect(c.getSource()))))
                .then(Commands.literal("status").executes(c -> safely(c.getSource(), () -> status(c.getSource()))))
                .then(Commands.literal("cancel").executes(c -> safely(c.getSource(), () -> cancel(c.getSource()))))
                .then(Commands.literal("undo").executes(c -> safely(c.getSource(), () -> undo(c.getSource()))))
        ));
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            Session s = sessions.get(server);
            if (s != null) s.previews.entrySet().removeIf(e -> e.getValue().reference &&
                (System.nanoTime()-e.getValue().created > PREVIEW_LIFETIME_NS || !referenceOwnerPresent(e.getValue())));
            if (s != null && s.reference != null) tickReference(s);
            if (s != null && s.job != null && !s.job.finished) s.job.tick();
        });
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            Session s = sessions.remove(server);
            if (s != null) {
                invalidateReference(s);
                if (s.planner != null) s.planner.shutdownNow();
            }
            if (s != null && s.job != null && !s.job.finished) LOGGER.warn("CastleForge interrupted: partial test build remains; in-memory undo is lost on shutdown.");
        });
        LOGGER.info("CastleForge alpha initialized; automatic world generation is DISABLED.");
    }
    private Session session(CommandSourceStack source) throws IOException {
        MinecraftServer server = source.getServer();
        Session result = sessions.get(server);
        if (result == null) {
            result = new Session(WorldSettings.load(server));
            sessions.put(server, result);
        }
        return result;
    }
    private static UUID owner(CommandSourceStack s) {
        return s.getEntity() == null ? new UUID(0,0) : s.getEntity().getUUID();
    }
    private static void overworld(ServerLevel level) {
        if (!level.dimension().equals(Level.OVERWORLD)) throw new IllegalArgumentException("Prototype supports Overworld only.");
    }
    private int region(CommandSourceStack source) throws IOException {
        overworld(source.getLevel());
        BlockPos p = BlockPos.containing(source.getPosition());
        CultureMap.Region r = session(source).settings.map(source.getLevel().getSeed()).at(p.getX(), p.getZ());
        return say(source, "Culture " + r.style() + " / " + r.id() + "; nominal spacing " + r.nominalSpacing()
            + " blocks; border blend " + Math.round(r.borderBlend()*100) + "%. Scale uses a configured reference, not automatic biome measurement.");
    }
    private int preview(CommandSourceStack source, Integer x, Integer z, boolean terraced) throws IOException {
        ServerLevel level = source.getLevel(); overworld(level);
        Session s = session(source);
        if (s.job != null && !s.job.finished) throw new IllegalArgumentException("A placement or undo is already running.");
        UUID user = owner(source);
        if (s.reference != null && s.reference.user.equals(user)) invalidateReference(s);
        s.previews.remove(user);
        s.previews.entrySet().removeIf(e -> System.nanoTime() - e.getValue().created > PREVIEW_LIFETIME_NS);
        if (s.previews.size() >= 8) throw new IllegalArgumentException("Too many previews; try after another preview expires.");
        BlockPos p = BlockPos.containing(source.getPosition());
        int radius = terraced ? TerracedCastlePlanner.RADIUS : CastlePlanner.RADIUS;
        int ox = x == null ? p.getX() : x;
        int oz = z == null ? p.getZ() - radius - 4 : z;
        if (Math.abs((long)ox) > 29_999_800 || Math.abs((long)oz) > 29_999_800)
            throw new IllegalArgumentException("Site too near world coordinate limit.");
        for (int cx = (ox-radius)>>4; cx <= (ox+radius)>>4; cx++)
            for (int cz = (oz-radius)>>4; cz <= (oz+radius)>>4; cz++)
                if (!level.getChunkSource().hasChunk(cx,cz)) throw new IllegalArgumentException("Entire site must already be loaded. Walk nearer; no chunks are force-generated.");
        var region = s.settings.map(level.getSeed()).at(ox,oz);
        BlockState centerGround = level.getBlockState(new BlockPos(ox,level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,ox,oz)-1,oz));
        Palette palette = Palette.choose(region.style(), centerGround.is(Blocks.SAND) || centerGround.is(Blocks.RED_SAND) || centerGround.is(Blocks.SANDSTONE));
        List<Check> checks = new ArrayList<>();
        TerracedCastlePlanner.Ground ground = (px,pz) -> {
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,px,pz);
            BlockPos pos = new BlockPos(px,y-1,pz);
            BlockState state = level.getBlockState(pos);
            checks.add(new Check(pos,state));
            if (!state.getFluidState().isEmpty() || !naturalGround(state)) {
                if (terraced) return TerracedCastlePlanner.UNUSABLE;
                throw new IllegalArgumentException("Unsuitable ground, water or existing structure at " + px + "," + pz);
            }
            return y;
        };
        List<Desired> desired = new ArrayList<>();
        String summary;
        String details;
        if (terraced) {
            var plan = TerracedCastlePlanner.plan(level.getSeed(),ox,oz,ground,region);
            for (var block : plan.blocks()) {
                var v = block.pos(); desired.add(new Desired(new BlockPos(v.x(),v.y(),v.z()),palette.resolve(block)));
            }
            var report = PlanDiagnostics.analyze(plan);
            var identity = CastleIdentity.generate(level.getSeed(),ox,oz,region,plan.condition());
            details = identity.oneLine() + "; heraldry: " + identity.heraldry()
                + "; " + report.compact() + "; oldest milestone: "
                + identity.events().getFirst().description();
            summary = identity.name() + " / TERRACED v1 / " + plan.condition() + "; " + plan.terraces().size() + " courts, "
                + plan.rooms().size() + " modules, " + plan.targets().size() + " verified access targets; terrace relief "
                + plan.relief() + "; rotation " + (plan.quarterTurns()*90) + " degrees";
        } else {
            var plan = CastlePlanner.plan(level.getSeed(),ox,oz,ground::surfaceY,region);
            for (var voxel : plan.voxels()) {
                var v = voxel.pos(); desired.add(new Desired(new BlockPos(v.x(),v.y(),v.z()),palette.resolve(voxel.material())));
            }
            var identity = CastleIdentity.generate(level.getSeed(),ox,oz,region,plan.condition());
            details = identity.oneLine() + "; heraldry: " + identity.heraldry()
                + "; classic reference plan (no terraced report).";
            summary = identity.name() + " / CLASSIC / " + plan.condition() + "; relief " + plan.relief();
        }
        List<Edit> edits = new ArrayList<>();
        for (Desired cell : desired) {
            BlockPos pos = cell.pos;
            if (level.isOutsideBuildHeight(pos) || !level.getWorldBorder().isWithinBounds(pos))
                throw new IllegalArgumentException("Plan exceeds build height or world border.");
            BlockState before = level.getBlockState(pos), after = cell.state;
            if (level.getBlockEntity(pos) != null || !before.getFluidState().isEmpty() || !replaceable(before))
                throw new IllegalArgumentException("Protected or non-natural block at " + pos.toShortString() + ". Nothing was changed.");
            // Include unchanged air: a newly obstructed doorway must invalidate confirmation too.
            checks.add(new Check(pos,before));
            if (!before.equals(after)) edits.add(new Edit(pos,before,after));
        }
        s.previews.put(user,new Preview(level,List.copyOf(edits),List.copyOf(checks),System.nanoTime(),details,false,null));
        LOGGER.info("CastleForge preview {} at {},{}: {} edits; {}",user,ox,oz,edits.size(),summary);
        return say(source,"PREVIEW only: " + region.style() + " at " + ox + "," + oz + "; " + summary + "; "
            + edits.size() + " edits. /castleforge build confirm within 120s. Disposable test world only; back up first.");
    }
    /** Reference preview has three bounded phases: server snapshot, pure worker, server preflight.
     * No Minecraft world/chunk/block object is read by the worker. Publication happens only while
     * the exact request is still the active session request; cancellation never publishes late work.
     */
    private int previewReference(CommandSourceStack source, Integer x, Integer z) throws IOException {
        ServerLevel level = source.getLevel(); overworld(level);
        Session s = session(source);
        if (s.job != null && !s.job.finished) throw new IllegalArgumentException("A placement or undo is already running.");
        UUID user = owner(source);
        if (s.reference != null) {
            if (!s.reference.user.equals(user)) throw new IllegalArgumentException("A reference preview is already being prepared by another operator.");
            invalidateReference(s);
        }
        s.previews.remove(user);
        s.previews.entrySet().removeIf(e -> System.nanoTime() - e.getValue().created > PREVIEW_LIFETIME_NS);
        if (s.previews.size() >= 8) throw new IllegalArgumentException("Too many previews; try after another preview expires.");
        BlockPos p = BlockPos.containing(source.getPosition());
        int radius = ReferenceCastlePlanner.RADIUS;
        int ox = x == null ? p.getX() : x;
        int oz = z == null ? p.getZ() - radius - 4 : z;
        if (Math.abs((long)ox) > 29_999_800 || Math.abs((long)oz) > 29_999_800)
            throw new IllegalArgumentException("Site too near world coordinate limit.");
        for (int cx = (ox-radius)>>4; cx <= (ox+radius)>>4; cx++)
            for (int cz = (oz-radius)>>4; cz <= (oz+radius)>>4; cz++)
                if (!level.getChunkSource().hasChunk(cx,cz))
                    throw new IllegalArgumentException("Entire reference site must already be loaded. No chunks are force-generated.");
        s.reference = new ReferencePreview(source, user, ox, oz, level.getSeed(), s.settings.map(level.getSeed()).at(ox,oz));
        return say(source,"Preparing REFERENCE preview at " + ox + "," + oz
            + ". Snapshot and checks run over several ticks; planning runs on a worker. Wait for PREVIEW READY before confirming; /castleforge status or cancel.");
    }

    private static void invalidateReference(Session s) {
        ReferencePreview p = s.reference;
        s.reference = null;
        if (p != null && p.future != null) p.future.cancel(true);
        if (s.planner != null) s.planner.purge();
    }

    private void tickReference(Session s) {
        ReferencePreview p = s.reference;
        if (p == null) return;
        if ((s.job != null && !s.job.finished)
            || (p.source.getEntity() != null && (p.source.getEntity().isRemoved() || p.source.getEntity().level() != p.level))) {
            invalidateReference(s); return;
        }
        long tickStart = System.nanoTime();
        try {
            if (tickStart - p.created > PREVIEW_LIFETIME_NS)
                throw new IllegalArgumentException("Reference preparation timed out after 120 seconds. Nothing was changed.");
            if (p.snapshotIndex < p.heights.length) {
                int work = 0;
                while (p.snapshotIndex < p.heights.length && work < EDITS_PER_TICK && System.nanoTime()-tickStart < TICK_BUDGET_NS) {
                    int px = p.ox - p.radius + p.snapshotIndex % p.width;
                    int pz = p.oz - p.radius + p.snapshotIndex / p.width;
                    if (!p.level.getChunkSource().hasChunk(px>>4,pz>>4))
                        throw new IllegalArgumentException("Reference terrain unloaded at " + px + "," + pz + ". Stay nearby and preview again.");
                    int y = p.level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,px,pz);
                    BlockPos pos = new BlockPos(px,y-1,pz);
                    if (p.level.isOutsideBuildHeight(pos) || !p.level.getWorldBorder().isWithinBounds(pos))
                        throw new IllegalArgumentException("Reference terrain exceeds world bounds at " + pos.toShortString());
                    BlockState state = p.level.getBlockState(pos);
                    if (!state.getFluidState().isEmpty())
                        throw new IllegalArgumentException("Reference site contains water/fluid at " + pos.toShortString());
                    if (p.level.getBlockEntity(pos) != null)
                        throw new IllegalArgumentException("Reference site contains a block entity at " + pos.toShortString() + ": " + state);
                    if (!naturalGround(state))
                        throw new IllegalArgumentException("Reference ground block is not in the supported terrain list at " + pos.toShortString()
                            + ": " + state + ". Choose an open flat site");
                    p.checks.add(new Check(pos,state));
                    p.heights[p.snapshotIndex++] = y;
                    work++;
                }
                p.snapshotNs += System.nanoTime()-tickStart;
                return;
            }
            if (p.future == null) {
                if (s.planner == null) s.planner = new ThreadPoolExecutor(1,1,0,TimeUnit.MILLISECONDS,
                    new ArrayBlockingQueue<>(1), task -> {
                        Thread worker = new Thread(task,"CastleForge-reference-planner");
                        worker.setDaemon(true); return worker;
                    },new ThreadPoolExecutor.AbortPolicy());
                // These immutable values and the copied primitive array are the entire world input.
                int[] heights = p.heights.clone();
                int ox = p.ox, oz = p.oz, radius = p.radius, width = p.width;
                long seed = p.seed;
                CultureMap.Region region = p.region;
                CastlePlanner.Ground ground = (px,pz) -> {
                    int dx=px-ox+radius, dz=pz-oz+radius;
                    if (dx<0 || dx>=width || dz<0 || dz>=width)
                        throw new IllegalArgumentException("Reference planner sampled outside its terrain snapshot");
                    return heights[dz*width+dx];
                };
                p.future = s.planner.submit(() -> {
                    long start = System.nanoTime();
                    try { p.result = ReferenceCastlePlanner.plan(seed,ox,oz,ground,region); }
                    catch (RuntimeException e) { p.failure = e; }
                    finally { p.planningNs = System.nanoTime()-start; p.completed = true; }
                });
                return;
            }
            if (!p.completed) return;
            if (p.failure != null) throw p.failure;
            var plan = p.result;
            if (plan == null) throw new IllegalStateException("Reference planner returned no result");
            if (plan.blocks().size() > ReferenceCastlePlanner.MAX_BLOCKS)
                throw new IllegalArgumentException("Reference exceeds its block budget");
            int work = 0;
            while (p.preflightIndex < plan.blocks().size() && work < EDITS_PER_TICK && System.nanoTime()-tickStart < TICK_BUDGET_NS) {
                var block = plan.blocks().get(p.preflightIndex);
                var v = block.pos();
                BlockPos pos = new BlockPos(v.x(),v.y(),v.z());
                if (p.level.isOutsideBuildHeight(pos) || !p.level.getWorldBorder().isWithinBounds(pos))
                    throw new IllegalArgumentException("Reference plan exceeds build height or world border at " + pos.toShortString());
                if (!loaded(p.level,pos))
                    throw new IllegalArgumentException("Reference plan unloaded at " + pos.toShortString() + ". Nothing was changed.");
                BlockState before = p.level.getBlockState(pos), after = ReferencePalette.resolve(block);
                if (p.level.getBlockEntity(pos) != null || !before.getFluidState().isEmpty() || !replaceable(before))
                    throw new IllegalArgumentException("Protected or non-natural block at " + pos.toShortString() + ". Nothing was changed.");
                // Air and unchanged cells remain part of confirmation's conflict contract.
                p.checks.add(new Check(pos,before));
                if (!before.equals(after)) p.edits.add(new Edit(pos,before,after));
                p.minX=Math.min(p.minX,v.x()); p.maxX=Math.max(p.maxX,v.x());
                p.minY=Math.min(p.minY,v.y()); p.maxY=Math.max(p.maxY,v.y());
                p.minZ=Math.min(p.minZ,v.z()); p.maxZ=Math.max(p.maxZ,v.z());
                p.preflightIndex++; work++;
            }
            p.preflightNs += System.nanoTime()-tickStart;
            if (p.preflightIndex != plan.blocks().size()) return;
            if (s.reference != p) return;
            var identity = CastleIdentity.generate(p.seed,p.ox,p.oz,p.region,CastlePlanner.Condition.RESTORED);
            String timing = "snapshot " + TimeUnit.NANOSECONDS.toMillis(p.snapshotNs) + " ms, plan "
                + TimeUnit.NANOSECONDS.toMillis(p.planningNs) + " ms, preflight " + TimeUnit.NANOSECONDS.toMillis(p.preflightNs) + " ms";
            String details = identity.oneLine() + "; " + plan.architecture().variant() + " (Central European material study); "
                + plan.report().compact() + "; bounds " + (p.maxX-p.minX+1) + " x " + (p.maxY-p.minY+1) + " x " + (p.maxZ-p.minZ+1)
                + "; terrain relief " + plan.relief() + "; rotation " + plan.quarterTurns()*90
                + " degrees; " + p.edits.size() + " edits; " + timing
                + "; geometry checks are not Minecraft collision/visual acceptance.";
            // Retain at most one large reference preview per session, alongside legacy previews.
            s.previews.entrySet().removeIf(e -> e.getValue().reference || System.nanoTime()-e.getValue().created > PREVIEW_LIFETIME_NS);
            if (s.previews.size() >= 8) throw new IllegalArgumentException("Preview slots filled while planning; try again later.");
            s.previews.put(p.user,new Preview(p.level,List.copyOf(p.edits),List.copyOf(p.checks),System.nanoTime(),details,true,p.source));
            s.reference = null;
            LOGGER.info("CastleForge reference preview {} at {},{}: {}; {}",p.user,p.ox,p.oz,plan.report().compact(),timing);
            say(p.source,"PREVIEW READY: Falkenwacht reference at " + p.ox + "," + p.oz + "; " + plan.report().compact()
                + "; " + p.edits.size() + " edits. /castleforge inspect; /castleforge build confirm within 120s. Disposable test world only.");
        } catch (RuntimeException e) {
            invalidateReference(s);
            if (!(e instanceof IllegalArgumentException)) LOGGER.error("CastleForge reference preview failed",e);
            p.source.sendFailure(Component.literal("CastleForge reference: " + e.getMessage() + ". Nothing was placed."));
        }
    }

    private int inspect(CommandSourceStack source) throws IOException {
        Preview p = session(source).previews.get(owner(source));
        if (p == null || System.nanoTime() - p.created > PREVIEW_LIFETIME_NS
            || p.level != source.getLevel()) {
            throw new IllegalArgumentException("No fresh preview to inspect. Run /castleforge preview first.");
        }
        return say(source,"CastleForge design report: " + p.details);
    }
    private int build(CommandSourceStack source) throws IOException {
        Session s = session(source);
        if (s.job != null && !s.job.finished) throw new IllegalArgumentException("Another job is running.");
        if (s.reference != null && s.reference.user.equals(owner(source)))
            throw new IllegalArgumentException("Reference preview is still being prepared. Wait for PREVIEW READY.");
        Preview p = s.previews.remove(owner(source));
        if (p == null || System.nanoTime() - p.created > PREVIEW_LIFETIME_NS || p.level != source.getLevel())
            throw new IllegalArgumentException("Run a fresh preview in this dimension first.");
        if (p.reference && (!referenceOwnerPresent(p) || p.creator.getEntity() != source.getEntity()))
            throw new IllegalArgumentException("Reference preview belongs to an earlier player session. Preview again.");
        for (Check check : p.checks) {
            if (!loaded(p.level,check.pos) || !p.level.getBlockState(check.pos).equals(check.expected) || p.level.getBlockEntity(check.pos)!=null)
                throw new IllegalArgumentException("Site changed since preview; nothing was placed. Preview again.");
        }
        invalidateReference(s);
        s.job = new Job(owner(source), source, p.level, p.edits, false);
        s.lastBuild = s.job;
        s.previews.clear();
        return say(source,"Test build started (budget: 256 edits / 2 ms per tick). /castleforge cancel stops; /castleforge undo reverts this session's last build.");
    }
    private int status(CommandSourceStack source) throws IOException {
        Session s = session(source);
        if (s.reference != null) return say(source,"Reference preview: " + s.reference.progress());
        Job j = s.job;
        return say(source,j == null ? "No job." : (j.undo ? "Undo " : "Build ") + j.index + "/" + j.edits.size() + "; " + j.message);
    }
    private int cancel(CommandSourceStack source) throws IOException {
        Session s = session(source);
        if (s.reference != null) {
            if (!s.reference.user.equals(owner(source)))
                throw new IllegalArgumentException("Only the initiating operator can cancel this reference preview.");
            invalidateReference(s);
            return say(source,"Reference preview cancelled. Nothing was placed.");
        }
        Job j = s.job;
        Preview p = s.previews.get(owner(source));
        if ((j == null || j.finished) && p != null && p.reference) {
            s.previews.remove(owner(source));
            return say(source,"Reference preview discarded. Nothing was placed.");
        }
        if (j == null || j.finished) throw new IllegalArgumentException("No active job.");
        if (!j.owner.equals(owner(source))) throw new IllegalArgumentException("Only the initiating operator can cancel this job.");
        if (j.undo) throw new IllegalArgumentException("Let the bounded undo finish; it cannot be cancelled.");
        j.finished = true; j.message = "Cancelled; partial build remains until /castleforge undo.";
        return say(source,j.message);
    }
    private int undo(CommandSourceStack source) throws IOException {
        Session s = session(source);
        if (s.job != null && !s.job.finished) throw new IllegalArgumentException("Cancel the active build first.");
        Job previous = s.lastBuild;
        if (previous == null || !previous.owner.equals(owner(source))) throw new IllegalArgumentException("No undo for this operator in this server session.");
        List<Edit> reversed = new ArrayList<>(previous.applied);
        Collections.reverse(reversed);
        invalidateReference(s);
        s.job = new Job(owner(source),source,previous.level,List.copyOf(reversed),true);
        s.lastBuild = null;
        return say(source,"Undo started. Blocks modified after placement are left untouched. This is not a world-backup replacement.");
    }
    private static boolean loaded(ServerLevel level, BlockPos p) { return level.getChunkSource().hasChunk(p.getX()>>4,p.getZ()>>4); }
    private static boolean replaceable(BlockState s) { return s.isAir() || s.canBeReplaced() || s.is(BlockTags.LEAVES); }
    private static boolean naturalGround(BlockState s) {
        return s.is(Blocks.GRASS_BLOCK) || s.is(Blocks.DIRT) || s.is(Blocks.COARSE_DIRT) || s.is(Blocks.PODZOL)
            || s.is(Blocks.ROOTED_DIRT) || s.is(Blocks.MYCELIUM) || s.is(Blocks.MUD) || s.is(Blocks.STONE)
            || s.is(Blocks.ANDESITE) || s.is(Blocks.DIORITE) || s.is(Blocks.GRANITE) || s.is(Blocks.DEEPSLATE)
            || s.is(Blocks.TUFF) || s.is(Blocks.CALCITE) || s.is(Blocks.SAND) || s.is(Blocks.RED_SAND)
            || s.is(Blocks.SANDSTONE) || s.is(Blocks.RED_SANDSTONE) || s.is(Blocks.GRAVEL)
            || s.is(Blocks.SNOW_BLOCK) || s.is(Blocks.TERRACOTTA);
    }
    private static int say(CommandSourceStack s, String message) { s.sendSuccess(() -> Component.literal(message),false); return 1; }
    @FunctionalInterface private interface Action { int run() throws IOException; }
    private static int safely(CommandSourceStack source, Action action) {
        try { return action.run(); }
        catch (IllegalArgumentException | IOException e) { source.sendFailure(Component.literal("CastleForge: " + e.getMessage())); return 0; }
        catch (RuntimeException e) { LOGGER.error("CastleForge command failed",e); source.sendFailure(Component.literal("CastleForge failed; see server log. No automatic retry.")); return 0; }
    }
    private record Desired(BlockPos pos, BlockState state) {}
    private record Check(BlockPos pos, BlockState expected) {}
    private record Edit(BlockPos pos, BlockState before, BlockState after) {}
    private record Preview(ServerLevel level, List<Edit> edits, List<Check> checks, long created, String details,
                           boolean reference, CommandSourceStack creator) {}
    private static boolean referenceOwnerPresent(Preview p) {
        return p.creator != null && (p.creator.getEntity() == null
            || (!p.creator.getEntity().isRemoved() && p.creator.getEntity().level() == p.level));
    }
    private static boolean matchesUndo(BlockState actual, BlockState expected) {
        // Neighbor updates recompute stair shape; never ignore facing, half, material or waterlogging changes.
        if (expected.getBlock() instanceof StairBlock && actual.is(expected.getBlock()))
            return actual.setValue(StairBlock.SHAPE, expected.getValue(StairBlock.SHAPE)).equals(expected);
        return actual.equals(expected);
    }
    private static final class Session {
        final WorldSettings settings; final Map<UUID,Preview> previews = new HashMap<>();
        Job job, lastBuild;
        ReferencePreview reference;
        ThreadPoolExecutor planner;
        Session(WorldSettings settings) { this.settings=settings; }
    }
    private static final class ReferencePreview {
        final CommandSourceStack source;
        final ServerLevel level;
        final UUID user;
        final int ox, oz, radius = ReferenceCastlePlanner.RADIUS, width = radius*2+1;
        final int[] heights = new int[width*width];
        final long seed, created = System.nanoTime();
        final CultureMap.Region region;
        final List<Check> checks = new ArrayList<>();
        final List<Edit> edits = new ArrayList<>();
        int snapshotIndex, preflightIndex;
        int minX=Integer.MAX_VALUE,minY=Integer.MAX_VALUE,minZ=Integer.MAX_VALUE;
        int maxX=Integer.MIN_VALUE,maxY=Integer.MIN_VALUE,maxZ=Integer.MIN_VALUE;
        long snapshotNs, preflightNs;
        Future<?> future;
        volatile ReferenceCastlePlanner.Plan result;
        volatile RuntimeException failure;
        volatile boolean completed;
        volatile long planningNs;
        ReferencePreview(CommandSourceStack source, UUID user, int ox, int oz, long seed, CultureMap.Region region) {
            this.source=source; this.level=source.getLevel(); this.user=user; this.ox=ox; this.oz=oz;
            this.seed=seed; this.region=region;
        }
        String progress() {
            if (snapshotIndex < heights.length) return "terrain snapshot " + snapshotIndex + "/" + heights.length;
            if (!completed) return "architecture planning";
            return "block preflight " + preflightIndex + "/" + (result == null ? 0 : result.blocks().size());
        }
    }
    private static final class Job {
        final UUID owner; final CommandSourceStack source; final ServerLevel level;
        final List<Edit> edits; final List<Edit> applied = new ArrayList<>(); final boolean undo;
        int index, unloadedTicks, conflicts; boolean finished; String message = "Running";
        Job(UUID owner, CommandSourceStack source, ServerLevel level, List<Edit> edits, boolean undo) {
            this.owner=owner; this.source=source; this.level=level; this.edits=edits; this.undo=undo;
        }
        void tick() {
            long start = System.nanoTime(); int work=0;
            try {
                while (index < edits.size() && work < EDITS_PER_TICK && System.nanoTime()-start < TICK_BUDGET_NS) {
                    Edit e = edits.get(index);
                    if (!loaded(level,e.pos)) {
                        message="Paused: walk back to load the site.";
                        if (++unloadedTicks > 1200 && !undo) { finished=true; message="Stopped after unloaded timeout. Partial build remains; undo is available."; say(source,message); }
                        return;
                    }
                    unloadedTicks=0;
                    BlockState expected = undo ? e.after : e.before, target = undo ? e.before : e.after;
                    BlockState actual = level.getBlockState(e.pos);
                    boolean matches = undo ? matchesUndo(actual,expected) : actual.equals(expected)
                        || (expected.canBeReplaced() && actual.isAir());
                    if (level.getBlockEntity(e.pos)!=null || !matches) {
                        if (undo) { conflicts++; index++; work++; continue; }
                        finished=true; message="Stopped: world changed at " + e.pos.toShortString() + ". Partial build remains; use undo."; say(source,message); return;
                    }
                    // Neighbor updates may already have cleared replaceable vegetation to the target air.
                    // Do not treat a no-op setBlock result as failure or claim it as our own applied edit.
                    if (actual.equals(target)) { index++; work++; continue; }
                    if (!level.setBlock(e.pos,target,Block.UPDATE_ALL)) {
                        if (undo) { conflicts++; index++; work++; continue; }
                        finished=true; message="Stopped: placement rejected. Use undo for the partial build."; say(source,message); return;
                    }
                    if (!undo) applied.add(e);
                    index++; work++;
                }
                if (index==edits.size()) {
                    finished=true; message="Finished" + (undo ? "; skipped conflicts: " + conflicts : "; undo available until next build or server shutdown.");
                    say(source,"CastleForge " + message); LOGGER.info("CastleForge {} complete: {} edits, {} conflicts",undo?"undo":"build",index,conflicts);
                }
            } catch (RuntimeException e) {
                finished=true; message="Stopped after error; see log. Partial changes remain.";
                LOGGER.error("CastleForge placement error",e); say(source,message);
            }
        }
    }
}

