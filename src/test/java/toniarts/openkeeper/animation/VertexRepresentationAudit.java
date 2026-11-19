/* Copyright (C) 2026 OpenKeeper, licensed under GPL-3.0-or-later. */
package toniarts.openkeeper.animation;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import toniarts.openkeeper.tools.convert.AssetsConverter;

/** Offline comparison only; no new runtime format, approximate key reduction or GPU promotion. */
public final class VertexRepresentationAudit {
    record Result(int uniqueFrames, long deduplicatedPrimitiveBytes, long halfPositionBytes,
                  double halfComponentError, double halfVelocityError, long halfOutsideSourceBounds) { }

    private static final class Frame {
        final float[] values;
        final int hash;
        Frame(float[] values) { this.values = values.clone(); hash = Arrays.hashCode(values); }
        @Override public int hashCode() { return hash; }
        @Override public boolean equals(Object other) {
            return other instanceof Frame frame && Arrays.equals(values, frame.values);
        }
    }

    static Result inspect(VertexClip clip) {
        int size = Math.multiplyExact(clip.vertexCount(), 3);
        int keys = clip.keyCount();
        float[] times = new float[keys], positions = new float[Math.multiplyExact(size, keys)];
        float[] frame = new float[size];
        float[] minimum = {Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY};
        float[] maximum = {Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY};
        var frames = new HashSet<Frame>();
        for (int key = 0; key < keys; key++) {
            times[key] = clip.keyTime(key); clip.copyFrame(key, frame); frames.add(new Frame(frame));
            for (int i = 0; i < size; i++) {
                minimum[i % 3] = Math.min(minimum[i % 3], frame[i]);
                maximum[i % 3] = Math.max(maximum[i % 3], frame[i]);
                float decoded = Float.float16ToFloat(Float.floatToFloat16(frame[i]));
                if (!Float.isFinite(decoded)) throw new IllegalArgumentException("Binary16 overflows source coordinates");
                positions[key * size + i] = decoded;
            }
        }
        long outside = 0;
        for (int i = 0; i < positions.length; i++)
            if (positions[i] < minimum[i % 3] || positions[i] > maximum[i % 3]) outside++;
        var half = VertexClip.of(clip.vertexCount(), times, positions);
        float[] original = new float[size], decoded = new float[size];
        double error = 0;
        // Linear intervals need endpoint/fraction checks, including first/last clamps.
        for (int key = 0; key < keys; key++) for (int fraction = 0; fraction < 4; fraction++) {
            int next = Math.min(keys - 1, key + 1);
            double time = clip.keyTime(key) - clip.keyTime(0)
                    + ((double) clip.keyTime(next) - clip.keyTime(key)) * fraction / 4;
            clip.sample(time, original); half.sample(time, decoded);
            for (int i = 0; i < size; i++) error = Math.max(error, Math.abs((double) original[i] - decoded[i]));
        }
        for (double time : new double[]{-1, clip.keyTime(keys - 1) - clip.keyTime(0) + 1}) {
            clip.sample(time, original); half.sample(time, decoded);
            for (int i = 0; i < size; i++) error = Math.max(error, Math.abs((double) original[i] - decoded[i]));
        }
        // Flat unique Float32 frames + source times + an int frame lookup, not HashSet/object overhead.
        long deduplicated = 4L * (keys + keys + (long) frames.size() * size);
        return new Result(frames.size(), deduplicated, 2L * positions.length,
                error, VertexClipBake.velocityError(clip, half), outside);
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 2) throw new IllegalArgumentException("Expected source-bound corpus root and output CSV");
        Path root = Path.of(args[0]);
        var manifest = Files.readAllLines(root.resolve("report.csv"));
        var rows = new ArrayList<String>();
        rows.add("resource,part,keys,unique_frames,float32_primitive_bytes,deduplicated_primitive_bytes,half_position_bytes,half_component_error,half_velocity_error,half_outside_source_bounds,source_sha256");
        long original = 0, optionalDeduplicated = 0, repeatedClips = 0, outside = 0, parts = 0;
        long totalKeys = 0, totalUnique = 0;
        double error = 0, velocity = 0;
        for (String line : manifest.subList(1, manifest.size())) {
            String[] fields = line.split(","); String name = fields[0], hash = fields[7]; int part = Integer.parseInt(fields[1]);
            VertexClip clip;
            try (var input = Files.newInputStream(root.resolve(hash + "-" + part + "-FLOAT32.okva"))) {
                clip = VertexClip.read(input, hash, AssetsConverter.ConvertProcess.MODELS.getVersion(), part, VertexClipBake.BUDGET);
            }
            Result result = inspect(clip); parts++; original += clip.decodedBytes();
            optionalDeduplicated += Math.min(clip.decodedBytes(), result.deduplicatedPrimitiveBytes);
            if (result.deduplicatedPrimitiveBytes < clip.decodedBytes()) repeatedClips++;
            totalKeys += clip.keyCount(); totalUnique += result.uniqueFrames;
            error = Math.max(error, result.halfComponentError); velocity = Math.max(velocity, result.halfVelocityError);
            outside += result.halfOutsideSourceBounds;
            rows.add(String.format(Locale.ROOT, "%s,%d,%d,%d,%d,%d,%d,%.12g,%.12g,%d,%s",
                    name, part, clip.keyCount(), result.uniqueFrames, clip.decodedBytes(), result.deduplicatedPrimitiveBytes,
                    result.halfPositionBytes, result.halfComponentError, result.halfVelocityError, result.halfOutsideSourceBounds, hash));
        }
        if (parts == 0) throw new AssertionError("No source-bound clips");
        Path output = Path.of(args[1]); Files.createDirectories(output.getParent()); Files.write(output, rows);
        System.out.printf(Locale.ROOT,
                "VERTEX_REPRESENTATIONS parts=%d keys=%d unique_frames=%d float32_primitive_bytes=%d selective_deduplicated_primitive_bytes=%d benefiting_parts=%d half_max_component_error=%.12g half_max_velocity_error=%.12g half_outside_source_bound_components=%d%n",
                parts, totalKeys, totalUnique, original, optionalDeduplicated, repeatedClips, error, velocity, outside);
    }
}
