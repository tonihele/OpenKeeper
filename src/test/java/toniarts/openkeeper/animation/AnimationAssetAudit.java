/* Copyright (C) 2026 OpenKeeper, licensed under GPL-3.0-or-later. */
package toniarts.openkeeper.animation;

import com.jme3.anim.AnimComposer;
import com.jme3.asset.AssetKey;
import com.jme3.asset.DesktopAssetManager;
import com.jme3.asset.plugins.FileLocator;
import com.jme3.bounding.BoundingBox;
import com.jme3.scene.Spatial;
import com.jme3.scene.VertexBuffer;
import java.nio.FloatBuffer;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.TreeSet;
import java.util.TreeMap;
import java.nio.file.Files;
import toniarts.openkeeper.tools.convert.map.ArtResource;
import toniarts.openkeeper.tools.convert.map.KwdFile;
import toniarts.openkeeper.tools.convert.KmfAssetInfo;
import toniarts.openkeeper.tools.convert.KmfModelLoader;
import toniarts.openkeeper.tools.convert.kmf.KmfFile;
import toniarts.openkeeper.tools.convert.wad.WadFile;

/** Read-only, opt-in installed-asset validation. Never invokes ConvertModels. */
public final class AnimationAssetAudit {
    public static void main(String[] args) throws Exception {
        if (args.length < 2) throw new IllegalArgumentException("Expected DK2 root, converted asset root, optional resource names");
        var assets = new DesktopAssetManager(true);
        assets.registerLocator(args[1], FileLocator.class);
        assets.registerLocator("assets", FileLocator.class);
        var wad = new WadFile(Path.of(args[0], "Data", "Meshes.WAD"));
        var options = List.of(args).subList(2,args.length);
        for (String option : options) if (option.startsWith("--")
                && !option.equals("--report-only") && !option.equals("--all-creatures") && !option.equals("--packed"))
            throw new IllegalArgumentException("Unknown audit option: "+option);
        boolean reportOnly = options.contains("--report-only");
        boolean packed = options.contains("--packed");
        List<String> names = options.stream().filter(value->!value.startsWith("--")).toList();
        if (options.contains("--all-creatures")) {
            if (!names.isEmpty()) throw new IllegalArgumentException("Choose all creatures or explicit resources");
            var catalog = KwdFile.load(args[0],Path.of(args[0],"Data","Editor","Maps","Alcatraz.kwd"));
            var resources = new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);
            for (var creature : catalog.getCreatureList()) for (var art : creature.getAnimations().values()) {
                if (art == null || art.getType() != ArtResource.ArtResourceType.ANIMATING_MESH) continue;
                resources.add(art.getName());
                if (art.getFlags().contains(ArtResource.ArtResourceFlag.HAS_START_ANIMATION)) resources.add(art.getName()+"Start");
                if (art.getFlags().contains(ArtResource.ArtResourceFlag.HAS_END_ANIMATION)) resources.add(art.getName()+"End");
            }
            names = List.copyOf(resources);
        } else if (names.isEmpty()) names = List.of("Arc-Walk", "BIL-Walkback", "Electrocute1");
        var sourceNames = new TreeMap<String,String>(String.CASE_INSENSITIVE_ORDER);
        for (String entry : wad.getWadFileEntries()) sourceNames.put(Path.of(entry).getFileName().toString(),entry);
        var convertedNames = new TreeMap<String,String>(String.CASE_INSENSITIVE_ORDER);
        try (var files = Files.list(Path.of(args[1],"Models"))) {
            files.forEach(file->convertedNames.put(file.getFileName().toString(),file.getFileName().toString()));
        }
        long samples = 0, invalid = 0, outside = 0;
        System.out.println("resource,sha256,mode,tracks,samples,nonfinite_samples,outside_bound_samples");
        for (String name : names) {
            byte[] bytes = wad.getFileData(sourceNames.getOrDefault(name+".kmf",name+".kmf"));
            String hash = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
            for (boolean fresh : new boolean[]{false, true}) {
                Spatial model = fresh ? (Spatial) new KmfModelLoader().load(new KmfAssetInfo(assets,
                        new AssetKey<>(name), new KmfFile(bytes), false)) : assets.loadModel("Models/" + convertedNames.getOrDefault(name+".j3o",name+".j3o"));
                if (packed) new AnimationDataCache(64*1024*1024).prepare(model);
                long[] result = inspect(model,packed);
                System.out.printf("%s,%s,%s,%d,%d,%d,%d%n", name, hash, fresh ? "fresh" : "cached",
                        result[0], result[1], result[2], result[3]);
                samples += result[1]; invalid += result[2]; outside += result[3];
            }
        }
        System.out.printf("TOTAL samples=%d nonfinite=%d outside=%d packed=%s%n", samples, invalid, outside,packed);
        validate(invalid,outside,reportOnly);
    }

    static void validate(long invalid, long outside, boolean reportOnly) {
        if (!reportOnly && (invalid != 0 || outside != 0))
            throw new AssertionError("Invalid animation samples: nonfinite="+invalid+" outside="+outside);
    }

    static long[] inspect(Spatial model) {
        return inspect(model,false);
    }

    private static long[] inspect(Spatial model, boolean requirePacked) {
        List<PoseTrack> tracks = new ArrayList<>();
        model.depthFirstTraversal(spatial -> {
            var composer = spatial.getControl(AnimComposer.class);
            if (composer != null) for (var clip : composer.getAnimClips())
                for (var track : clip.getTracks()) if (track instanceof PoseTrack pose) tracks.add(pose);
        });
        long samples = 0, invalid = 0, outside = 0;
        for (PoseTrack track : tracks) {
            if (requirePacked && !track.isPackedSampling()) throw new AssertionError("Packed corpus audit unexpectedly fell back: "+track.getTarget().getName());
            var geometry = track.getTarget();
            var bound = (BoundingBox) geometry.getModelBound();
            // Both traversal directions, exact keys, midpoints, and clamped endpoints.
            int steps = Math.max(1, (track.getTimes().length - 1) * 2);
            for (int step = -1; step <= steps * 2 + 1; step++) {
                double fraction = step <= steps ? (double) step / steps : (double) (steps * 2 - step) / steps;
                track.getDataAtTime(step == steps * 2 + 1 ? track.getTimes()[track.getTimes().length - 1] + 1.0
                        : fraction * track.getLength(), null);
                FloatBuffer data = (FloatBuffer) geometry.getMesh().getBuffer(VertexBuffer.Type.Position).getData();
                boolean bad = false, out = false;
                for (int v = 0; v < data.limit(); v += 3) {
                    float x = data.get(v), y = data.get(v + 1), z = data.get(v + 2);
                    bad |= !Float.isFinite(x) || !Float.isFinite(y) || !Float.isFinite(z);
                    out |= Math.abs(x - bound.getCenter().x) > bound.getXExtent() + 1e-4f
                            || Math.abs(y - bound.getCenter().y) > bound.getYExtent() + 1e-4f
                            || Math.abs(z - bound.getCenter().z) > bound.getZExtent() + 1e-4f;
                }
                samples++; if (bad) invalid++; if (out) outside++;
            }
        }
        return new long[]{tracks.size(), samples, invalid, outside};
    }
}
