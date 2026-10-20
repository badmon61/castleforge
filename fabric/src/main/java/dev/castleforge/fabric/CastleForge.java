package dev.castleforge.fabric;

import dev.castleforge.core.CastlePlanner;
import dev.castleforge.core.CultureMap;
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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.IOException;
import java.util.*;

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
                .executes(c -> say(c.getSource(), "CastleForge alpha: /castleforge region | preview [x z] | build confirm | status | cancel | undo. TEST WORLDS ONLY."))
                .then(Commands.literal("region").executes(c -> safely(c.getSource(), () -> region(c.getSource()))))
                .then(Commands.literal("preview").executes(c -> safely(c.getSource(), () -> preview(c.getSource(), null, null)))
                    .then(Commands.argument("x", IntegerArgumentType.integer(-29_999_800, 29_999_800))
                        .then(Commands.argument("z", IntegerArgumentType.integer(-29_999_800, 29_999_800))
                            .executes(c -> safely(c.getSource(), () -> preview(c.getSource(),
                                IntegerArgumentType.getInteger(c,"x"), IntegerArgumentType.getInteger(c,"z")))))))
                .then(Commands.literal("build").then(Commands.literal("confirm")
                    .executes(c -> safely(c.getSource(), () -> build(c.getSource())))))
                .then(Commands.literal("status").executes(c -> safely(c.getSource(), () -> status(c.getSource()))))
                .then(Commands.literal("cancel").executes(c -> safely(c.getSource(), () -> cancel(c.getSource()))))
                .then(Commands.literal("undo").executes(c -> safely(c.getSource(), () -> undo(c.getSource()))))
        ));
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            Session s = sessions.get(server);
            if (s != null && s.job != null && !s.job.finished) s.job.tick();
        });
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            Session s = sessions.remove(server);
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
    private int preview(CommandSourceStack source, Integer x, Integer z) throws IOException {
        ServerLevel level = source.getLevel(); overworld(level);
        Session s = session(source);
        if (s.job != null && !s.job.finished) throw new IllegalArgumentException("A placement or undo is already running.");
        UUID user = owner(source);
        s.previews.remove(user);
        s.previews.entrySet().removeIf(e -> System.nanoTime() - e.getValue().created > PREVIEW_LIFETIME_NS);
        if (s.previews.size() >= 8) throw new IllegalArgumentException("Too many previews; try after another preview expires.");
        BlockPos p = BlockPos.containing(source.getPosition());
        int ox = x == null ? p.getX() : x;
        int oz = z == null ? p.getZ() - CastlePlanner.RADIUS - 4 : z;
        int radius = CastlePlanner.RADIUS;
        for (int cx = (ox-radius)>>4; cx <= (ox+radius)>>4; cx++)
            for (int cz = (oz-radius)>>4; cz <= (oz+radius)>>4; cz++)
                if (!level.getChunkSource().hasChunk(cx,cz)) throw new IllegalArgumentException("Entire site must already be loaded. Walk nearer; no chunks are force-generated.");
        var region = s.settings.map(level.getSeed()).at(ox,oz);
        var plan = CastlePlanner.plan(level.getSeed(), ox, oz, (px,pz) -> {
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,px,pz);
            BlockState ground = level.getBlockState(new BlockPos(px,y-1,pz));
            if (!ground.getFluidState().isEmpty() || !naturalGround(ground))
                throw new IllegalArgumentException("Unsuitable ground, water or existing structure at " + px + "," + pz);
            return y;
        }, region);
        BlockState ground = level.getBlockState(new BlockPos(ox,level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,ox,oz)-1,oz));
        Palette palette = Palette.choose(region.style(), ground.is(Blocks.SAND) || ground.is(Blocks.RED_SAND) || ground.is(Blocks.SANDSTONE));
        List<Edit> edits = new ArrayList<>();
        for (var voxel : plan.voxels()) {
            var v = voxel.pos(); BlockPos pos = new BlockPos(v.x(),v.y(),v.z());
            if (level.isOutsideBuildHeight(pos) || !level.getWorldBorder().isWithinBounds(pos))
                throw new IllegalArgumentException("Plan exceeds build height or world border.");
            BlockState before = level.getBlockState(pos), after = palette.resolve(voxel.material());
            if (level.getBlockEntity(pos) != null || !before.getFluidState().isEmpty() || !replaceable(before))
                throw new IllegalArgumentException("Protected or non-natural block at " + pos.toShortString() + ". Nothing was changed.");
            if (!before.equals(after)) edits.add(new Edit(pos,before,after));
        }
        s.previews.put(user,new Preview(level,List.copyOf(edits),plan,System.nanoTime()));
        LOGGER.info("CastleForge preview {} at {},{}: {} edits, {}", user,ox,oz,edits.size(),plan.condition());
        return say(source,"PREVIEW only: " + region.style() + " / " + plan.condition() + " at " + ox + "," + plan.floorY() + "," + oz
            + "; " + edits.size() + " edits; relief " + plan.relief() + ". /castleforge build confirm within 120s. Back up first; use a throwaway world.");
    }
    private int build(CommandSourceStack source) throws IOException {
        Session s = session(source);
        if (s.job != null && !s.job.finished) throw new IllegalArgumentException("Another job is running.");
        Preview p = s.previews.remove(owner(source));
        if (p == null || System.nanoTime() - p.created > PREVIEW_LIFETIME_NS || p.level != source.getLevel())
            throw new IllegalArgumentException("Run a fresh preview in this dimension first.");
        for (Edit e : p.edits) {
            if (!loaded(p.level,e.pos) || !p.level.getBlockState(e.pos).equals(e.before) || p.level.getBlockEntity(e.pos)!=null)
                throw new IllegalArgumentException("Site changed since preview; nothing was placed. Preview again.");
        }
        s.job = new Job(owner(source), source, p.level, p.edits, false);
        s.lastBuild = s.job;
        s.previews.clear();
        return say(source,"Test build started (budget: 256 edits / 2 ms per tick). /castleforge cancel stops; /castleforge undo reverts this session's last build.");
    }
    private int status(CommandSourceStack source) throws IOException {
        Job j = session(source).job;
        return say(source,j == null ? "No job." : (j.undo ? "Undo " : "Build ") + j.index + "/" + j.edits.size() + "; " + j.message);
    }
    private int cancel(CommandSourceStack source) throws IOException {
        Job j = session(source).job;
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
    private record Edit(BlockPos pos, BlockState before, BlockState after) {}
    private record Preview(ServerLevel level, List<Edit> edits, CastlePlanner.Plan plan, long created) {}
    private static final class Session {
        final WorldSettings settings; final Map<UUID,Preview> previews = new HashMap<>();
        Job job, lastBuild;
        Session(WorldSettings settings) { this.settings=settings; }
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
                    if (level.getBlockEntity(e.pos)!=null || !level.getBlockState(e.pos).equals(expected)) {
                        if (undo) { conflicts++; index++; work++; continue; }
                        finished=true; message="Stopped: world changed at " + e.pos.toShortString() + ". Partial build remains; use undo."; say(source,message); return;
                    }
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
