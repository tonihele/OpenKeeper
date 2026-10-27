/*
 * Copyright (C) 2014-2026 OpenKeeper
 *
 * OpenKeeper is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * OpenKeeper is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with OpenKeeper.  If not, see <http://www.gnu.org/licenses/>.
 */
package toniarts.openkeeper.tools.hdtextures;

import java.awt.color.ColorSpace;
import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import java.util.zip.CRC32;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import javax.imageio.ImageIO;

/**
 * Makes HD versions of the converted DK II textures with
 * <a href="https://github.com/xinntao/Real-ESRGAN">Real-ESRGAN</a>, on your
 * own machine: {@code assets/Converted/Textures} is upscaled 4x into
 * {@code assets/Textures}, which the game uses before the converted ones.
 * <pre>
 * ./gradlew hdTextures --args="setup"
 * ./gradlew hdTextures --args="upscale --test-batch"
 * ./gradlew hdTextures --args="upscale"
 * ./gradlew hdTextures --args="clean"
 * </pre>
 * {@code setup} downloads Real-ESRGAN to {@code ~/.OpenKeeper/hd-textures}.
 * {@code upscale} does every texture, {@code --test-batch} only the terrain,
 * walls and floors and {@code --gui-test-batch} the HUD's windows, tabs and room
 * icons; up to date ones are skipped ({@code --force} redoes them). Other
 * options: {@code --model}, {@code --tile}, {@code --gpu} ({@code -1} for the
 * CPU), {@code --source}, {@code --dest} and {@code --tools-dir}.
 * {@code clean} deletes only the files this tool wrote, which the manifest in
 * the destination lists. Files listed by the old Python script's manifest
 * ({@code .hd-textures.json}) are taken over as this tool's own.
 * <p>
 * Each texture is padded with its own opposite edges, so tiling ones stay
 * seamless, and cut-out alpha stays cut-out. Soft textures (smoke, flames) get
 * a bicubic resize instead of Real-ESRGAN, and in-between ones a blend. The
 * original's local brightness, colour and opacity are kept.
 *
 * @author OpenKeeper
 */
public class HdTextures {

    private static final int SCALE = 4;
    private static final int PAD = 16;
    private static final String MANIFEST_NAME = ".hd-textures.properties";
    private static final String PYTHON_MANIFEST_NAME = ".hd-textures.json";
    private static final String DEFAULT_MODEL = "realesrgan-x4plus";
    private static final String RELEASE_URL = "https://github.com/xinntao/Real-ESRGAN/releases/download/v0.2.5.0/";
    private static final Map<String, String[]> DOWNLOADS = Map.of(
            "windows", new String[]{"realesrgan-ncnn-vulkan-20220424-windows.zip", "abc02804e17982a3be33675e4d471e91ea374e65b70167abc09e31acb412802d"},
            "ubuntu", new String[]{"realesrgan-ncnn-vulkan-20220424-ubuntu.zip", "e5aa6eb131234b87c0c51f82b89390f5e3e642b7b70f2b9bbe95b6a285a40c96"},
            "macos", new String[]{"realesrgan-ncnn-vulkan-20220424-macos.zip", "e0ad05580abfeb25f8d8fb55aaf7bedf552c375b5b4d9bd3c8d59764d2cc333a"});

    /**
     * Never upscaled: the minimap's colour table and background (read as data),
     * and the cursors, thumbnails and maps (not used)
     */
    static final Pattern EXCLUDED = Pattern.compile("(?i)^(GUI/Map/(MapColours|Map-BG)\\.png|cursor/|Thumbnails/|Maps/)");

    /** The terrain (T_*) and the other wall and floor textures */
    static final Pattern TEST_BATCH = Pattern.compile("(?i)^(T_[^/]*|[^/]*(wall|floor)[^/]*)\\.png$");

    /** The HUD's windows and bars, the minimap frame, the tabs and the room icons */
    static final Pattern GUI_TEST_BATCH = Pattern.compile("(?i)^GUI/(Windows|Map|Tabs|Rooms)/[^/]*\\.png$");

    /**
     * Fine detail is the mean absolute Laplacian over the mean absolute
     * gradient: below SOFT_BELOW a texture is resized smoothly, above
     * DETAILED_ABOVE upscaled with Real-ESRGAN, in between blended
     */
    private static final double SOFT_BELOW = 0.75;
    private static final double DETAILED_ABOVE = 1.05;

    /** Pillow's fixed point precision for 8-bit images, which the bicubic resize copies */
    private static final int PRECISION_BITS = 32 - 8 - 2;

    private static final Set<String> VALUE_OPTIONS = Set.of("--tools-dir", "--source", "--dest", "--model", "--gpu", "--tile");
    private static final Set<String> FLAGS = Set.of("--test-batch", "--gui-test-batch", "--force");

    /** An image as one array per channel (red, green, blue, maybe alpha), 0-255 */
    record Planes(int width, int height, float[][] bands) {

        boolean hasAlpha() {
            return bands.length == 4;
        }
    }

    private HdTextures() {
        // Nope
    }

    /**
     * Runs the tool: {@code setup}, {@code upscale} or {@code clean} and their
     * options, see the class documentation
     *
     * @param args the command and its options
     * @throws IOException if reading or writing fails
     * @throws InterruptedException if interrupted while Real-ESRGAN runs
     */
    public static void main(String[] args) throws IOException, InterruptedException {
        try {
            Map<String, String> options = parseOptions(args);
            Path toolsDir = Paths.get(options.getOrDefault("--tools-dir",
                    Paths.get(System.getProperty("user.home"), ".OpenKeeper", "hd-textures").toString()));
            Path dest = Paths.get(options.getOrDefault("--dest", "assets/Textures")).toAbsolutePath().normalize();
            switch (options.getOrDefault("command", "")) {
                case "setup" -> setup(toolsDir);
                case "upscale" -> upscale(options, toolsDir, dest);
                case "clean" -> System.out.println("Removed " + clean(dest) + " HD textures from " + dest);
                default -> throw new IllegalArgumentException("Usage: hdTextures setup|upscale|clean [options], see "
                        + HdTextures.class.getName());
            }
        } catch (IllegalArgumentException | IllegalStateException e) {
            System.err.println(e.getMessage());
            System.exit(1);
        }
    }

    static Map<String, String> parseOptions(String[] args) {
        Map<String, String> options = new HashMap<>();
        for (int i = 0; i < args.length; i++) {
            if (VALUE_OPTIONS.contains(args[i]) && i + 1 < args.length) {
                options.put(args[i], args[++i]);
            } else if (FLAGS.contains(args[i])) {
                options.put(args[i], "");
            } else if (args[i].startsWith("-") || options.putIfAbsent("command", args[i]) != null) {
                throw new IllegalArgumentException("Unknown option " + args[i]);
            }
        }
        return options;
    }

    // --- Setup ----------------------------------------------------------------------------------
    private static void setup(Path toolsDir) throws IOException, InterruptedException {
        String os = System.getProperty("os.name").toLowerCase(Locale.ROOT);
        String[] download = DOWNLOADS.get(os.contains("win") ? "windows" : os.contains("mac") ? "macos" : "ubuntu");
        if (findExecutable(toolsDir) == null) {
            Files.createDirectories(toolsDir);
            Path archive = toolsDir.resolve(download[0]);
            System.out.println("Downloading " + RELEASE_URL + download[0]);
            HttpResponse<Path> response = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).build()
                    .send(HttpRequest.newBuilder(URI.create(RELEASE_URL + download[0])).build(),
                            HttpResponse.BodyHandlers.ofFile(archive));
            String digest = sha256(archive);
            if (response.statusCode() != 200 || !digest.equals(download[1])) {
                Files.delete(archive);
                throw new IllegalStateException("Download failed for " + download[0] + ": HTTP " + response.statusCode()
                        + ", SHA-256 " + digest + ", expected " + download[1]);
            }
            unzip(archive, toolsDir.resolve(download[0].replace(".zip", "")));
            Files.delete(archive);
            if (findExecutable(toolsDir) == null) {
                throw new IllegalStateException("realesrgan-ncnn-vulkan not found in " + toolsDir);
            }
            findExecutable(toolsDir).toFile().setExecutable(true);
        }
        System.out.println("Real-ESRGAN: " + findExecutable(toolsDir) + "\nNext: ./gradlew hdTextures --args=\"upscale --test-batch\"");
    }

    static String sha256(Path file) throws IOException {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(file)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    static void unzip(Path archive, Path target) throws IOException {
        try (ZipInputStream zip = new ZipInputStream(Files.newInputStream(archive))) {
            for (ZipEntry entry = zip.getNextEntry(); entry != null; entry = zip.getNextEntry()) {
                Path out = target.resolve(entry.getName()).normalize();
                if (!out.startsWith(target)) {
                    throw new IOException("Bad entry in " + archive + ": " + entry.getName());
                }
                if (entry.isDirectory()) {
                    Files.createDirectories(out);
                } else {
                    Files.createDirectories(out.getParent());
                    Files.copy(zip, out, StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }
    }

    private static Path findExecutable(Path toolsDir) throws IOException {
        String name = System.getProperty("os.name").toLowerCase(Locale.ROOT).contains("win")
                ? "realesrgan-ncnn-vulkan.exe" : "realesrgan-ncnn-vulkan";
        try (Stream<Path> files = Files.walk(toolsDir, 2)) {
            return files.filter(file -> file.getFileName().toString().equals(name) && Files.isRegularFile(file)).findFirst().orElse(null);
        } catch (NoSuchFileException e) {
            return null;
        }
    }

    // --- Upscaling ------------------------------------------------------------------------------
    private static void upscale(Map<String, String> options, Path toolsDir, Path dest) throws IOException, InterruptedException {
        Path executable = findExecutable(toolsDir);
        if (executable == null) {
            throw new IllegalStateException("Real-ESRGAN is not installed. Run: ./gradlew hdTextures --args=\"setup\"");
        }
        Path source = Paths.get(options.getOrDefault("--source", "assets/Converted/Textures")).toAbsolutePath().normalize();
        if (!Files.isDirectory(source)) {
            throw new IllegalStateException("No converted textures in " + source + ". Run the game once to convert DK II's assets.");
        }
        String model = options.getOrDefault("--model", DEFAULT_MODEL);
        String settings = model + " pad=" + PAD;
        List<String> all = listTextures(source);
        List<String> textures = select(all, options.containsKey("--test-batch") ? TEST_BATCH
                : options.containsKey("--gui-test-batch") ? GUI_TEST_BATCH : null);
        System.out.println("Measuring detail");
        Map<String, Double> weights = detailWeights(source, all, textures);

        // A file in the destination is ours if the manifest lists it, otherwise it is left alone
        Properties manifest = loadManifest(dest);
        List<String> todo = new ArrayList<>();
        for (String rel : textures) {
            boolean exists = Files.exists(dest.resolve(rel));
            if (exists && manifest.getProperty(rel) == null) {
                System.out.println("Skipping " + rel + ": the destination has a file this tool didn't write");
            } else if (options.containsKey("--force") || !exists
                    || !record(source.resolve(rel), weights.get(rel), settings).equals(manifest.getProperty(rel))) {
                todo.add(rel);
            }
        }
        System.out.println(textures.size() + " textures selected, " + todo.size() + " to upscale");
        if (todo.isEmpty()) {
            return;
        }
        Runtime.getRuntime().addShutdownHook(new Thread(() -> saveManifest(dest, manifest)));

        long started = System.nanoTime();
        Path work = Files.createTempDirectory("hd-textures-");
        List<String> failed = new ArrayList<>();
        try {
            Path workIn = Files.createDirectories(work.resolve("in"));
            Path workOut = Files.createDirectories(work.resolve("out"));
            System.out.println("Preparing");
            for (int i = 0; i < todo.size(); i++) {
                if (weights.get(todo.get(i)) > 0) {
                    Planes[] padded = prepare(read(source.resolve(todo.get(i))));
                    write(padded[0], workIn.resolve(String.format("%05d_c.png", i)));
                    if (padded[1] != null) {
                        float[] alpha = padded[1].bands()[0];
                        write(new Planes(padded[1].width(), padded[1].height(), new float[][]{alpha, alpha, alpha}),
                                workIn.resolve(String.format("%05d_a.png", i)));
                    }
                }
            }
            try (Stream<Path> inputs = Files.list(workIn)) {
                if (inputs.findAny().isPresent()) {
                    System.out.println("Upscaling with " + model);
                    runRealEsrgan(executable, workIn, workOut, model, options);
                }
            }

            System.out.println("Writing");
            for (int i = 0; i < todo.size(); i++) {
                String rel = todo.get(i);
                double weight = weights.get(rel);
                try {
                    Planes original = read(source.resolve(rel));
                    Planes[] padded = prepare(original);
                    Planes[] detailed = null;
                    if (weight > 0) {
                        Path colour = workOut.resolve(String.format("%05d_c.png", i));
                        Path alpha = workOut.resolve(String.format("%05d_a.png", i));
                        if (!Files.exists(colour) || (padded[1] != null && !Files.exists(alpha))) {
                            throw new IOException("Real-ESRGAN wrote no output");
                        }
                        detailed = new Planes[]{read(colour), padded[1] != null ? read(alpha) : null};
                    }
                    Path target = dest.resolve(rel);
                    Files.createDirectories(target.getParent());
                    Path temporary = target.resolveSibling(target.getFileName() + ".tmp");
                    write(finish(original, padded, detailed, weight), temporary);
                    Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException | IllegalStateException e) {
                    failed.add(rel + ": " + e.getMessage());
                    continue;
                }
                manifest.setProperty(rel, record(source.resolve(rel), weight, settings));
            }
        } finally {
            try (Stream<Path> paths = Files.walk(work)) {
                for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
                    Files.delete(path);
                }
            }
        }
        System.out.println("Done: " + (todo.size() - failed.size()) + " textures in "
                + (System.nanoTime() - started) / 1_000_000_000 + " s, written to " + dest);
        if (!failed.isEmpty()) {
            throw new IllegalStateException(failed.size() + " failed:\n  " + String.join("\n  ", failed));
        }
    }

    private static void runRealEsrgan(Path executable, Path in, Path out, String model, Map<String, String> options)
            throws IOException, InterruptedException {
        List<String> command = new ArrayList<>(List.of(executable.toString(), "-i", in.toString(), "-o", out.toString(),
                "-n", model, "-s", String.valueOf(SCALE), "-f", "png", "-m", executable.resolveSibling("models").toString(),
                "-t", options.getOrDefault("--tile", "0")));
        if (options.containsKey("--gpu")) {
            command.addAll(List.of("-g", options.get("--gpu")));
        }
        Process process = new ProcessBuilder(command).redirectOutput(ProcessBuilder.Redirect.DISCARD).start();
        String errors = new String(process.getErrorStream().readAllBytes(), StandardCharsets.UTF_8);
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new IllegalStateException("Real-ESRGAN failed (" + exitCode + "):\n"
                    + errors.substring(Math.max(0, errors.length() - 2000)));
        }
    }

    static List<String> listTextures(Path source) throws IOException {
        try (Stream<Path> files = Files.walk(source)) {
            return files.filter(path -> Files.isRegularFile(path)
                    && path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".png"))
                    .map(path -> source.relativize(path).toString().replace('\\', '/'))
                    .sorted().toList();
        }
    }

    static List<String> select(List<String> textures, Pattern batch) {
        return textures.stream().filter(rel -> !EXCLUDED.matcher(rel).find()
                && (batch == null || batch.matcher(rel).find())).toList();
    }

    /**
     * How much of each texture comes from Real-ESRGAN, the rest from a bicubic
     * resize. The frames of an animation (names that differ only in their
     * trailing number) share one weight, so it doesn't change style from frame
     * to frame.
     */
    static Map<String, Double> detailWeights(Path source, List<String> all, List<String> textures) throws IOException {
        Map<String, List<String>> groups = new HashMap<>();
        all.forEach(rel -> groups.computeIfAbsent(animationKey(rel), key -> new ArrayList<>()).add(rel));
        Map<String, Double> scores = new HashMap<>();
        Map<String, Double> weights = new HashMap<>();
        for (String rel : textures) {
            List<Double> values = new ArrayList<>();
            for (String member : groups.getOrDefault(animationKey(rel), List.of(rel))) {
                if (!scores.containsKey(member)) {
                    scores.put(member, detailScore(read(source.resolve(member))));
                }
                if (scores.get(member) != null) {
                    values.add(scores.get(member));
                }
            }
            if (values.isEmpty()) {
                weights.put(rel, 0.0); // A flat colour, nothing to upscale
                continue;
            }
            values.sort(null);
            int middle = values.size() / 2;
            double median = values.size() % 2 == 1 ? values.get(middle) : (values.get(middle - 1) + values.get(middle)) / 2;
            double weight = Math.clamp((median - SOFT_BELOW) / (DETAILED_ABOVE - SOFT_BELOW), 0.0, 1.0);
            weights.put(rel, Math.round(weight * 100) / 100.0);
        }
        return weights;
    }

    static String animationKey(String rel) {
        int dot = rel.lastIndexOf('.');
        return (dot > rel.lastIndexOf('/') ? rel.substring(0, dot) : rel).replaceAll("\\d+$", "").toLowerCase(Locale.ROOT);
    }

    /**
     * Mean absolute Laplacian over mean absolute gradient, the higher of colour
     * and alpha; null for a flat image
     */
    static Double detailScore(Planes image) {
        int width = image.width();
        int height = image.height();
        float[] grey = new float[width * height];
        for (int i = 0; i < grey.length; i++) {
            grey[i] = (image.bands()[0][i] + image.bands()[1][i] + image.bands()[2][i]) / 3f;
        }
        Double best = null;
        for (float[] channel : image.hasAlpha() ? List.of(grey, image.bands()[3]) : List.of(grey)) {
            double sum = 0;
            double squares = 0;
            for (float value : channel) {
                sum += value;
                squares += (double) value * value;
            }
            double mean = sum / channel.length;
            if (Math.sqrt(squares / channel.length - mean * mean) <= 1 || Math.min(width, height) < 3) {
                continue;
            }
            double down = 0;
            double across = 0;
            double laplacian = 0;
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    int i = y * width + x;
                    if (y > 0) {
                        down += Math.abs(channel[i] - channel[i - width]);
                    }
                    if (x > 0) {
                        across += Math.abs(channel[i] - channel[i - 1]);
                    }
                    if (x > 0 && y > 0 && x < width - 1 && y < height - 1) {
                        laplacian += Math.abs(4 * channel[i] - channel[i - width] - channel[i + width] - channel[i - 1] - channel[i + 1]);
                    }
                }
            }
            double gradient = down / ((height - 1) * width) + across / (height * (width - 1));
            double score = laplacian / ((height - 2) * (width - 2)) / Math.max(gradient, 1e-6);
            best = best == null ? score : Math.max(best, score);
        }
        return best;
    }

    /**
     * Pads the colour and the alpha with the texture's opposite edges. The
     * colour of the visible pixels is spread into the invisible ones first.
     *
     * @return the padded colour, and the padded alpha as one band, or null if
     * there is no alpha or it is constant
     */
    static Planes[] prepare(Planes image) {
        float[][] rgb = Arrays.copyOf(image.bands(), 3);
        Planes alpha = null;
        if (image.hasAlpha()) {
            float[] channel = image.bands()[3];
            if (!isConstant(channel)) {
                alpha = pad(new Planes(image.width(), image.height(), new float[][]{channel}));
            }
            rgb = bleed(rgb, channel, image.width(), image.height());
        }
        return new Planes[]{pad(new Planes(image.width(), image.height(), rgb)), alpha};
    }

    static Planes pad(Planes image) {
        int width = image.width() + 2 * PAD;
        int height = image.height() + 2 * PAD;
        float[][] bands = new float[image.bands().length][width * height];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int from = Math.floorMod(y - PAD, image.height()) * image.width() + Math.floorMod(x - PAD, image.width());
                for (int band = 0; band < bands.length; band++) {
                    bands[band][y * width + x] = image.bands()[band][from];
                }
            }
        }
        return new Planes(width, height, bands);
    }

    private static boolean isConstant(float[] channel) {
        for (float value : channel) {
            if (value != channel[0]) {
                return false;
            }
        }
        return true;
    }

    /**
     * Spreads the colour of the visible pixels into the invisible ones next to
     * them, eight pixels deep, and gives the rest the mean visible colour, so
     * the upscaler doesn't smear the garbage under transparent pixels into the
     * visible edges.
     */
    static float[][] bleed(float[][] rgb, float[] alpha, int width, int height) {
        boolean[] known = new boolean[alpha.length];
        int visible = 0;
        for (int i = 0; i < alpha.length; i++) {
            known[i] = alpha[i] > 0;
            visible += known[i] ? 1 : 0;
        }
        if (visible == 0 || visible == alpha.length) {
            return rgb;
        }
        float[][] colour = new float[3][alpha.length];
        for (int band = 0; band < 3; band++) {
            for (int i = 0; i < alpha.length; i++) {
                colour[band][i] = known[i] ? rgb[band][i] : 0;
            }
        }
        for (int step = 0; step < 8 && visible < alpha.length; step++) {
            boolean[] grown = known.clone();
            float[][] next = new float[][]{colour[0].clone(), colour[1].clone(), colour[2].clone()};
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    int i = y * width + x;
                    float count = 0;
                    float[] total = new float[3];
                    for (int dy = -1; dy <= 1 && !known[i]; dy++) {
                        for (int dx = -1; dx <= 1; dx++) {
                            int neighbour = Math.clamp(y + dy, 0, height - 1) * width + Math.clamp(x + dx, 0, width - 1);
                            if (known[neighbour]) {
                                count++;
                                for (int band = 0; band < 3; band++) {
                                    total[band] += colour[band][neighbour];
                                }
                            }
                        }
                    }
                    if (count > 0) {
                        grown[i] = true;
                        visible++;
                        for (int band = 0; band < 3; band++) {
                            next[band][i] = total[band] / count;
                        }
                    }
                }
            }
            known = grown;
            colour = next;
        }
        for (int band = 0; band < 3; band++) {
            double sum = 0;
            for (int i = 0; i < alpha.length; i++) {
                sum += known[i] ? colour[band][i] : 0;
            }
            for (int i = 0; i < alpha.length; i++) {
                colour[band][i] = (float) Math.clamp(Math.rint(known[i] ? colour[band][i] : sum / visible), 0, 255);
            }
        }
        return colour;
    }

    /**
     * Makes the HD texture: blends Real-ESRGAN's output (null if not used) with
     * a bicubic resize by the weight, keeps the original's levels, and puts the
     * alpha back; cut-out alpha stays cut-out.
     */
    static Planes finish(Planes original, Planes[] padded, Planes[] detailed, double weight) {
        int width = original.width();
        int height = original.height();
        float[][] rgb = blend(padded[0], detailed == null ? null : detailed[0], weight, width, height);
        for (int band = 0; band < 3; band++) {
            rgb[band] = keepLevels(rgb[band], original.bands()[band], width, height);
        }
        if (!original.hasAlpha()) {
            return new Planes(width * SCALE, height * SCALE, rgb);
        }
        float[] alpha = original.bands()[3];
        float[] channel = new float[rgb[0].length];
        if (padded[1] == null) {
            Arrays.fill(channel, alpha[0]);
        } else {
            float[][] blended = blend(padded[1], detailed == null ? null : detailed[1], weight, width, height);
            boolean binary = true;
            for (float value : alpha) {
                binary &= value == 0 || value == 255;
            }
            for (int i = 0; i < channel.length; i++) {
                channel[i] = blended.length == 1 ? blended[0][i] : (blended[0][i] + blended[1][i] + blended[2][i]) / 3f;
                if (binary) {
                    channel[i] = channel[i] >= 128 ? 255 : 0;
                }
            }
            if (!binary) {
                channel = keepLevels(channel, alpha, width, height);
            }
        }
        return new Planes(width * SCALE, height * SCALE, new float[][]{rgb[0], rgb[1], rgb[2], channel});
    }

    /**
     * Real-ESRGAN's output blended with a bicubic resize by the weight, with
     * the padding cropped off
     */
    private static float[][] blend(Planes padded, Planes detailed, double weight, int width, int height) {
        int paddedWidth = padded.width() * SCALE;
        if (detailed != null && (detailed.width() != paddedWidth || detailed.height() != padded.height() * SCALE)) {
            throw new IllegalStateException("unexpected output size " + detailed.width() + "x" + detailed.height());
        }
        float[][] smooth = new float[padded.bands().length][];
        for (int band = 0; band < smooth.length; band++) {
            smooth[band] = resize(padded.bands()[band], padded.width(), padded.height(), true);
        }
        float[][] result = new float[detailed == null ? smooth.length : 3][width * SCALE * height * SCALE];
        for (int band = 0; band < result.length; band++) {
            for (int y = 0; y < height * SCALE; y++) {
                for (int x = 0; x < width * SCALE; x++) {
                    int from = (y + PAD * SCALE) * paddedWidth + x + PAD * SCALE;
                    float value = smooth[band % smooth.length][from];
                    if (detailed != null) {
                        value = detailed.bands()[band][from] * (float) weight + value * (float) (1 - weight);
                    }
                    result[band][y * width * SCALE + x] = value;
                }
            }
        }
        return result;
    }

    /**
     * Brings the local brightness, colour or opacity back to the original's:
     * the blurred difference between the original and the upscaled band shrunk
     * back is spread over the upscaled band.
     */
    static float[] keepLevels(float[] upscaled, float[] original, int width, int height) {
        float[] difference = new float[width * height];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                float sum = 0;
                for (int i = 0; i < SCALE * SCALE; i++) {
                    sum += upscaled[(y * SCALE + i / SCALE) * width * SCALE + x * SCALE + i % SCALE];
                }
                difference[y * width + x] = original[y * width + x] - sum / (SCALE * SCALE);
            }
        }
        for (int pass = 0; pass < 2; pass++) {
            // 3x3 box blur that wraps round the edges, like the tiling textures do
            float[] blurred = new float[difference.length];
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    float sum = 0;
                    for (int i = 0; i < 9; i++) {
                        sum += difference[Math.floorMod(y + i / 3 - 1, height) * width + Math.floorMod(x + i % 3 - 1, width)];
                    }
                    blurred[y * width + x] = sum / 9;
                }
            }
            difference = blurred;
        }
        float[] correction = resize(difference, width, height, false);
        float[] result = new float[upscaled.length];
        for (int i = 0; i < result.length; i++) {
            result[i] = upscaled[i] + correction[i];
        }
        return result;
    }

    /**
     * Resizes a band 4x the way Pillow does (a separable convolution, the
     * kernel cut and renormalised at the edges): bicubic with Pillow's 8-bit
     * fixed point weights and rounding, or bilinear on floating point values.
     */
    static float[] resize(float[] band, int width, int height, boolean bicubic) {
        return resample(resample(band, width, height, true, bicubic), width * SCALE, height, false, bicubic);
    }

    private static float[] resample(float[] band, int width, int height, boolean horizontal, boolean bicubic) {
        int size = horizontal ? width : height;
        int lines = horizontal ? height : width;
        int outWidth = horizontal ? width * SCALE : width;
        float[] result = new float[band.length * SCALE];
        double support = bicubic ? 2 : 1;
        for (int out = 0; out < size * SCALE; out++) {
            double center = (out + 0.5) / SCALE;
            int min = Math.max((int) (center - support + 0.5), 0);
            double[] weights = new double[Math.min((int) (center + support + 0.5), size) - min];
            double total = 0;
            for (int i = 0; i < weights.length; i++) {
                double x = Math.abs(i + min - center + 0.5);
                weights[i] = bicubic ? (x < 1 ? (1.5 * x - 2.5) * x * x + 1 : x < 2 ? (((x - 5) * x + 8) * x - 4) * -0.5 : 0)
                        : Math.max(1 - x, 0);
                total += weights[i];
            }
            for (int i = 0; i < weights.length; i++) {
                weights[i] /= total;
                if (bicubic) {
                    weights[i] = (long) (weights[i] * (1 << PRECISION_BITS) + (weights[i] < 0 ? -0.5 : 0.5));
                }
            }
            for (int line = 0; line < lines; line++) {
                double sum = bicubic ? 1 << (PRECISION_BITS - 1) : 0;
                for (int i = 0; i < weights.length; i++) {
                    sum += band[horizontal ? line * width + min + i : (min + i) * width + line] * weights[i];
                }
                result[horizontal ? line * outWidth + out : out * outWidth + line] = bicubic
                        ? (float) Math.clamp(Math.floor(sum / (1 << PRECISION_BITS)), 0, 255) : (float) sum;
            }
        }
        return result;
    }

    static Planes read(Path path) throws IOException {
        BufferedImage image = ImageIO.read(path.toFile());
        if (image == null) {
            throw new IOException("Not an image: " + path);
        }
        int width = image.getWidth();
        int height = image.getHeight();
        boolean alpha = image.getColorModel().hasAlpha();
        // Grey images are read from the raster: getRGB would treat them as linear and brighten them
        boolean grey = image.getColorModel().getColorSpace().getType() == ColorSpace.TYPE_GRAY;
        Raster raster = image.getRaster();
        float[][] bands = new float[alpha ? 4 : 3][width * height];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int i = y * width + x;
                int argb = grey ? 0 : image.getRGB(x, y);
                for (int band = 0; band < 3; band++) {
                    bands[band][i] = grey ? raster.getSample(x, y, 0) : (argb >> (16 - 8 * band)) & 0xFF;
                }
                if (alpha) {
                    bands[3][i] = grey ? raster.getSample(x, y, 1) : argb >>> 24;
                }
            }
        }
        return new Planes(width, height, bands);
    }

    /**
     * Writes a PNG with Paeth filtering and the strongest deflate. ImageIO's
     * writer does no filtering, which makes the files about 1.5 times bigger.
     */
    static void write(Planes image, Path path) throws IOException {
        int width = image.width();
        int bands = image.bands().length;
        byte[] row = new byte[width * bands];
        byte[] previous = new byte[row.length];
        ByteArrayOutputStream idat = new ByteArrayOutputStream();
        Deflater deflater = new Deflater(Deflater.BEST_COMPRESSION);
        try (DeflaterOutputStream deflated = new DeflaterOutputStream(idat, deflater, 1 << 16)) {
            byte[] filtered = new byte[row.length + 1];
            filtered[0] = 4;
            for (int y = 0; y < image.height(); y++) {
                for (int i = 0; i < row.length; i++) {
                    row[i] = (byte) Math.clamp(Math.rint(image.bands()[i % bands][y * width + i / bands]), 0, 255);
                }
                for (int i = 0; i < row.length; i++) {
                    int a = i >= bands ? row[i - bands] & 0xFF : 0;
                    int b = previous[i] & 0xFF;
                    int c = i >= bands ? previous[i - bands] & 0xFF : 0;
                    int pa = Math.abs(b - c);
                    int pb = Math.abs(a - c);
                    int pc = Math.abs(a + b - 2 * c);
                    filtered[i + 1] = (byte) ((row[i] & 0xFF) - (pa <= pb && pa <= pc ? a : pb <= pc ? b : c));
                }
                deflated.write(filtered);
                System.arraycopy(row, 0, previous, 0, row.length);
            }
        } finally {
            deflater.end();
        }
        try (DataOutputStream out = new DataOutputStream(new BufferedOutputStream(Files.newOutputStream(path)))) {
            out.write(new byte[]{(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n'});
            ByteBuffer header = ByteBuffer.allocate(13).putInt(width).putInt(image.height())
                    .put((byte) 8).put((byte) (image.hasAlpha() ? 6 : 2)).put((byte) 0).put((byte) 0).put((byte) 0);
            writeChunk(out, "IHDR", header.array());
            writeChunk(out, "IDAT", idat.toByteArray());
            writeChunk(out, "IEND", new byte[0]);
        }
    }

    private static void writeChunk(DataOutputStream out, String type, byte[] data) throws IOException {
        byte[] name = type.getBytes(StandardCharsets.US_ASCII);
        CRC32 crc = new CRC32();
        crc.update(name);
        crc.update(data);
        out.writeInt(data.length);
        out.write(name);
        out.write(data);
        out.writeInt((int) crc.getValue());
    }

    // --- Manifest -------------------------------------------------------------------------------
    /**
     * What the manifest keeps for a texture: when it changes, the texture is
     * upscaled again
     */
    private static String record(Path source, double weight, String settings) throws IOException {
        return Files.getLastModifiedTime(source).toInstant() + " " + weight + " " + settings;
    }

    /**
     * Loads the manifest of the files this tool wrote. Files listed by the old
     * Python script's manifest are adopted as ours, with an out of date record,
     * so they get upscaled again and the next save replaces its manifest.
     */
    static Properties loadManifest(Path dest) throws IOException {
        Properties manifest = new Properties();
        if (Files.exists(dest.resolve(MANIFEST_NAME))) {
            try (InputStream in = Files.newInputStream(dest.resolve(MANIFEST_NAME))) {
                manifest.load(in);
            }
        }
        if (Files.exists(dest.resolve(PYTHON_MANIFEST_NAME))) {
            // The files are its top-level keys, indented by two spaces
            Matcher key = Pattern.compile("(?m)^  \"(.+)\": \\{").matcher(Files.readString(dest.resolve(PYTHON_MANIFEST_NAME)));
            while (key.find()) {
                manifest.putIfAbsent(key.group(1), "python");
            }
        }
        return manifest;
    }

    static void saveManifest(Path dest, Properties manifest) {
        try {
            Files.createDirectories(dest);
            try (var out = Files.newOutputStream(dest.resolve(MANIFEST_NAME))) {
                manifest.store(out, "HD textures written by " + HdTextures.class.getName());
            }
            Files.deleteIfExists(dest.resolve(PYTHON_MANIFEST_NAME));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Deletes the HD textures this tool (or the Python script) wrote, the
     * folders left empty and the manifests
     *
     * @return how many textures were deleted
     */
    static int clean(Path dest) throws IOException {
        int removed = 0;
        for (String rel : loadManifest(dest).stringPropertyNames()) {
            Path path = dest.resolve(rel);
            if (Files.deleteIfExists(path)) {
                removed++;
            }
        }
        Files.deleteIfExists(dest.resolve(MANIFEST_NAME));
        Files.deleteIfExists(dest.resolve(PYTHON_MANIFEST_NAME));
        try (Stream<Path> paths = Files.walk(dest)) {
            // Only deletes the folders that are empty now
            paths.sorted(Comparator.reverseOrder()).filter(path -> Files.isDirectory(path) && !path.equals(dest)).forEach(path -> path.toFile().delete());
        }
        return removed;
    }
}
