package dev.castleforge.fabric;

import dev.castleforge.core.CultureMap;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;
import java.io.*;
import java.nio.file.*;
import java.util.Properties;

/** Captured once per save. Unknown versions fail closed rather than silently moving culture borders. */
public record WorldSettings(int biomeScaleBlocks, int cultureMultiplier) {
    public CultureMap map(long seed) { return new CultureMap(seed, biomeScaleBlocks, cultureMultiplier); }
    public static WorldSettings load(MinecraftServer server) throws IOException {
        Path file = server.getWorldPath(LevelResource.ROOT).resolve("castleforge/generation-v1.properties");
        Properties p = new Properties();
        if (Files.exists(file)) {
            try (Reader reader = Files.newBufferedReader(file)) { p.load(reader); }
            if (!"1".equals(p.getProperty("algorithmVersion"))) throw new IOException("Unsupported CastleForge algorithm version");
            return parse(p);
        }
        Path defaults = FabricLoader.getInstance().getConfigDir().resolve("castleforge.properties");
        if (Files.exists(defaults)) try (Reader reader = Files.newBufferedReader(defaults)) { p.load(reader); }
        WorldSettings settings = parse(p);
        p.clear();
        p.setProperty("algorithmVersion", "1");
        p.setProperty("biomeScaleBlocks", Integer.toString(settings.biomeScaleBlocks()));
        p.setProperty("cultureMultiplier", Integer.toString(settings.cultureMultiplier()));
        p.setProperty("scaleSource", "configured-reference-not-auto-detected");
        Files.createDirectories(file.getParent());
        Path tmp = Files.createTempFile(file.getParent(), "generation-", ".tmp");
        try {
            try (Writer writer = Files.newBufferedWriter(tmp)) { p.store(writer, "CastleForge immutable generation reference. Back up world before changing."); }
            try { Files.move(tmp, file, StandardCopyOption.ATOMIC_MOVE); }
            catch (AtomicMoveNotSupportedException e) { Files.move(tmp, file); }
        } finally { Files.deleteIfExists(tmp); }
        return settings;
    }
    private static WorldSettings parse(Properties p) throws IOException {
        try {
            int biome = Integer.parseInt(p.getProperty("biomeScaleBlocks", "1024"));
            int multiplier = Integer.parseInt(p.getProperty("cultureMultiplier", "7"));
            new CultureMap(0, biome, multiplier);
            return new WorldSettings(biome, multiplier);
        } catch (IllegalArgumentException e) { throw new IOException("Invalid CastleForge generation settings", e); }
    }
}
