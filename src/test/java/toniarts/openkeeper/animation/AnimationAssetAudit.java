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
        List<String> names = args.length > 2 ? List.of(args).subList(2, args.length)
                : List.of("Arc-Walk", "BIL-Walkback", "Electrocute1");
        long samples = 0, invalid = 0, outside = 0;
        System.out.println("resource,sha256,mode,tracks,samples,nonfinite_samples,outside_bound_samples");
        for (String name : names) {
            byte[] bytes = wad.getFileData(name + ".kmf");
            String hash = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
            for (boolean fresh : new boolean[]{false, true}) {
                Spatial model = fresh ? (Spatial) new KmfModelLoader().load(new KmfAssetInfo(assets,
                        new AssetKey<>(name), new KmfFile(bytes), false)) : assets.loadModel("Models/" + name + ".j3o");
                long[] result = inspect(model);
                System.out.printf("%s,%s,%s,%d,%d,%d,%d%n", name, hash, fresh ? "fresh" : "cached",
                        result[0], result[1], result[2], result[3]);
                samples += result[1]; invalid += result[2]; outside += result[3];
            }
        }
        System.out.printf("TOTAL samples=%d nonfinite=%d outside=%d%n", samples, invalid, outside);
        if (invalid != 0) throw new AssertionError("Non-finite animation samples: " + invalid);
    }

    static long[] inspect(Spatial model) {
        List<PoseTrack> tracks = new ArrayList<>();
        model.depthFirstTraversal(spatial -> {
            var composer = spatial.getControl(AnimComposer.class);
            if (composer != null) for (var clip : composer.getAnimClips())
                for (var track : clip.getTracks()) if (track instanceof PoseTrack pose) tracks.add(pose);
        });
        long samples = 0, invalid = 0, outside = 0;
        for (PoseTrack track : tracks) {
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
