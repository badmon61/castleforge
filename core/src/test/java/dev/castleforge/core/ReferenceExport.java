package dev.castleforge.core;

import dev.castleforge.core.architecture.ReferenceCastlePlanner;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.HexFormat;

/** Reproducible actual-block evidence; deliberately no Minecraft assets or concept imagery. */
public final class ReferenceExport {
    public static void main(String[] args) throws Exception {
        Path directory=Path.of(args.length==0 ? "build/reference" : args[0]);
        long seed=args.length>1 ? Long.parseLong(args[1]) : 20261020L;
        Files.createDirectories(directory);
        long start=System.nanoTime();
        var plan=ReferenceCastlePlanner.plan(seed,0,0,(x,z)->64,new CultureMap(seed,1024,7).at(0,0));
        long millis=(System.nanoTime()-start)/1_000_000;
        StringBuilder tsv=new StringBuilder("x\ty\tz\tkind\tfacing\n");
        for(var b:plan.localBlocks()) tsv.append(b.pos().x()).append('\t').append(b.pos().y()).append('\t')
            .append(b.pos().z()).append('\t').append(b.kind()).append('\t').append(b.facing()).append('\n');
        byte[] data=tsv.toString().getBytes(StandardCharsets.UTF_8);
        Files.write(directory.resolve("blocks.tsv"),data);
        String digest=HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(data));
        StringBuilder model=new StringBuilder("Actual generated architecture, canonical local coordinates\n")
            .append("variant=").append(plan.architecture().variant()).append('\n')
            .append("seed=").append(seed).append(" origin=0,0 ground=64 rotation=").append(plan.quarterTurns()).append('\n')
            .append("planMillis=").append(millis).append(" (one cold invocation; not a benchmark)\n")
            .append("blocks.tsv.sha256=").append(digest).append('\n').append(plan.report().compact()).append("\n\nModules\n");
        plan.architecture().modules().forEach(m->model.append(m).append('\n'));
        model.append("\nRooms\n"); plan.architecture().rooms().forEach(r->model.append(r).append('\n'));
        model.append("\nConnections\n"); plan.architecture().connections().forEach(c->model.append(c).append('\n'));
        model.append("\nStair flights\n"); plan.architecture().stairs().forEach(s->model.append(s).append('\n'));
        Files.writeString(directory.resolve("model.txt"),model,StandardCharsets.UTF_8);
        System.out.println(model.substring(0,model.indexOf("\n\nModules")));
        System.out.println("Exported "+directory.toAbsolutePath());
    }
    private ReferenceExport() {}
}
