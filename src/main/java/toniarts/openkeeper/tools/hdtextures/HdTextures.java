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
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
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
import java.util.TreeMap;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import javax.imageio.ImageIO;

/**
 * Makes HD versions of the converted DK II textures with
 * <a href="https://github.com/xinntao/Real-ESRGAN">Real-ESRGAN</a>, on your
 * own machine. No textures come with it.<br>
 * It reads the PNGs OpenKeeper converted from your copy of the game
 * ({@code assets/Converted/Textures}), upscales them 4x and writes them under
 * the same names to {@code assets/Textures}. The game finds files under
 * {@code assets/} on the classpath before the converted ones, so it uses the HD
 * textures with no other change, and deleting them brings the originals back.
 * <pre>
 * ./gradlew hdTextures --args="setup"
 * ./gradlew hdTextures --args="upscale --test-batch"
 * ./gradlew hdTextures --args="upscale"
 * ./gradlew hdTextures --args="clean"
 * </pre>
 * <ol>
 * <li>{@code setup} downloads Real-ESRGAN's portable Vulkan build, checked
 * against a SHA-256, to {@code ~/.OpenKeeper/hd-textures} (change it with
 * {@code --tools-dir}). It needs a GPU with Vulkan, or {@code --gpu -1} for the
 * CPU, which is slow.</li>
 * <li>{@code upscale --test-batch} does only the terrain ({@code T_*}) and the
 * wall and floor textures, {@code --gui-test-batch} the HUD's windows and bars,
 * the minimap frame, the tabs and the room icons, and {@code --match REGEX} the
 * textures whose path matches, e.g. {@code --match "(?i)^GUI/Icons/"}.</li>
 * <li>{@code upscale} does all of them, skipping those already up to date.</li>
 * <li>{@code clean} deletes only the files this tool wrote.</li>
 * </ol>
 * What it does to each texture:
 * <ul>
 * <li>Pads it with its own opposite edges before upscaling and cuts the padding
 * off after, so tiling textures stay seamless ({@code --pad},
 * {@code --pad-mode wrap|edge|reflect}).</li>
 * <li>Upscales the alpha separately. Cut-out alpha (only fully transparent or
 * opaque pixels) stays cut-out. The visible colours are spread into the fully
 * transparent pixels first, so no dark fringes appear round cut-outs.</li>
 * <li>Resizes soft textures (smoke, flames, glows) smoothly instead: Real-ESRGAN
 * draws cracks, hairs and hard rims on them. Soft ones get a bicubic resize,
 * detailed ones Real-ESRGAN and those in between a blend of the two; the frames
 * of an animation get the same mix ({@code --no-soft-blend} uses Real-ESRGAN for
 * all).</li>
 * <li>Keeps the original's local brightness, colour and opacity, which
 * Real-ESRGAN tends to shift ({@code --no-level-match} turns this off).</li>
 * </ul>
 * Other options: {@code --model}, {@code --tile} (Real-ESRGAN's {@code -t}),
 * {@code --gpu}, {@code --source}, {@code --dest}, {@code --force},
 * {@code --dry-run}, {@code --all} (also what {@link #EXCLUDED} leaves out),
 * {@code --overwrite-unmanaged} and {@code --no-git-exclude}. The HD textures
 * are added to the repository's {@code .git/info/exclude}, and what was written
 * with which settings is kept in {@code .hd-textures.properties} in the
 * destination. Real-ESRGAN is BSD-3-Clause licensed; the upscaled textures are
 * derived from the game's, so keep them to yourself.
 *
 * @author OpenKeeper
 */
public class HdTextures {

    private static final int SCALE = 4;
    private static final String MANIFEST_NAME = ".hd-textures.properties";
    private static final int MANIFEST_RETRIES = 40;
    private static final String RELEASE_URL = "https://github.com/xinntao/Real-ESRGAN/releases/download/v0.2.5.0/";
    private static final Map<String, String[]> DOWNLOADS = Map.of(
            "windows", new String[]{"realesrgan-ncnn-vulkan-20220424-windows.zip", "abc02804e17982a3be33675e4d471e91ea374e65b70167abc09e31acb412802d"},
            "ubuntu", new String[]{"realesrgan-ncnn-vulkan-20220424-ubuntu.zip", "e5aa6eb131234b87c0c51f82b89390f5e3e642b7b70f2b9bbe95b6a285a40c96"},
            "macos", new String[]{"realesrgan-ncnn-vulkan-20220424-macos.zip", "e0ad05580abfeb25f8d8fb55aaf7bedf552c375b5b4d9bd3c8d59764d2cc333a"});
    private static final List<String> MODELS = List.of("realesrgan-x4plus", "realesrgan-x4plus-anime", "realesr-animevideov3-x4");
    private static final List<String> PAD_MODES = List.of("wrap", "edge", "reflect");

    /**
     * Left out unless --all is given: a path ending in / is a folder, anything
     * else one file. The interface (GUI/) is in: the game draws an HD interface
     * image at its original's size.
     */
    static final Map<String, String> EXCLUDED = new TreeMap<>(Map.of(
            "GUI/Map/MapColours.png", "the minimap's colour table, read as data",
            "GUI/Map/Map-BG.png", "the minimap tiles the converted original as its rock, so an HD copy would only make the panel round the minimap differ from the rock in it",
            "cursor/", "not used: the mouse cursors come from Interface/Cursors, and cursors have their hot spots in pixels",
            "Thumbnails/", "MapColours.png is the colour table of the map thumbnails, and OpenKeeper draws its own thumbnails",
            "Maps/", "not used"));

    /**
     * The terrain (T_*) and the other wall and floor textures
     */
    static final String TEST_BATCH = "(?i)^(T_[^/]*|[^/]*(wall|floor)[^/]*)\\.png$";

    /**
     * The HUD's windows and bars, the minimap frame, the tabs and the room
     * icons
     */
    static final String GUI_TEST_BATCH = "(?i)^GUI/(Windows|Map|Tabs|Rooms)/[^/]*\\.png$";

    /**
     * How much fine detail a texture has is measured as its mean absolute
     * Laplacian over its mean absolute gradient. Below SOFT_BELOW it is resized
     * smoothly, above DETAILED_ABOVE upscaled with Real-ESRGAN, and in between
     * the two are blended.
     */
    private static final double SOFT_BELOW = 0.75;
    private static final double DETAILED_ABOVE = 1.05;

    /**
     * Pillow's fixed point precision for 8-bit images, which the bicubic resize
     * copies
     */
    private static final int PRECISION_BITS = 32 - 8 - 2;

    private static final Set<String> VALUE_OPTIONS = Set.of("--tools-dir", "--source", "--dest", "--match", "--model",
            "--pad", "--pad-mode", "--gpu", "--tile");
    private static final Set<String> FLAGS = Set.of("--test-batch", "--gui-test-batch", "--all", "--no-soft-blend",
            "--no-level-match", "--force", "--overwrite-unmanaged", "--dry-run", "--no-git-exclude");

    /**
     * An image as one array per channel (red, green, blue and maybe alpha), row
     * by row, with 0-255 values
     */
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
                case "clean" -> System.out.println("Removed " + clean(dest) + " HD textures from " + dest
                        + "; the game uses the converted ones again");
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
        if (!MODELS.contains(options.getOrDefault("--model", MODELS.get(0)))
                || !PAD_MODES.contains(options.getOrDefault("--pad-mode", "wrap"))) {
            throw new IllegalArgumentException("--model is one of " + MODELS + ", --pad-mode one of " + PAD_MODES);
        }
        if (Stream.of("--test-batch", "--gui-test-batch", "--match").filter(options::containsKey).count() > 1) {
            throw new IllegalArgumentException("Give only one of --test-batch, --gui-test-batch and --match");
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
            HttpClient client = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).build();
            HttpResponse<Path> response = client.send(HttpRequest.newBuilder(URI.create(RELEASE_URL + download[0])).build(),
                    HttpResponse.BodyHandlers.ofFile(archive));
            String digest = sha256(archive);
            if (response.statusCode() != 200 || !digest.equals(download[1])) {
                Files.delete(archive);
                throw new IllegalStateException("Download failed for " + download[0] + ": HTTP " + response.statusCode()
                        + ", SHA-256 " + digest + ", expected " + download[1]);
            }
            Path target = toolsDir.resolve(download[0].replace(".zip", ""));
            unzip(archive, target);
            Files.delete(archive);
            Path executable = findExecutable(toolsDir);
            if (executable == null) {
                throw new IllegalStateException("realesrgan-ncnn-vulkan not found in " + target);
            }
            executable.toFile().setExecutable(true);
        }
        System.out.println("Real-ESRGAN: " + findExecutable(toolsDir));
        System.out.println("Setup done. Next: ./gradlew hdTextures --args=\"upscale --test-batch\"");
    }

    static String sha256(Path file) throws IOException {
        try (InputStream in = Files.newInputStream(file)) {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[1 << 16];
            for (int read = in.read(buffer); read >= 0; read = in.read(buffer)) {
                digest.update(buffer, 0, read);
            }
            return HexFormat.of().formatHex(digest.digest());
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
        if (!Files.isDirectory(toolsDir)) {
            return null;
        }
        String name = System.getProperty("os.name").toLowerCase(Locale.ROOT).contains("win")
                ? "realesrgan-ncnn-vulkan.exe" : "realesrgan-ncnn-vulkan";
        try (Stream<Path> folders = Files.list(toolsDir)) {
            return folders.map(folder -> folder.resolve(name)).filter(Files::isRegularFile).sorted().findFirst().orElse(null);
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
        String model = options.getOrDefault("--model", MODELS.get(0));
        int pad = Integer.parseInt(options.getOrDefault("--pad", "16"));
        String padMode = options.getOrDefault("--pad-mode", "wrap");
        boolean matchLevels = !options.containsKey("--no-level-match");
        String settings = model + " pad=" + pad + " " + padMode + (matchLevels ? " level-match" : "");
        String pattern = options.containsKey("--test-batch") ? TEST_BATCH
                : options.containsKey("--gui-test-batch") ? GUI_TEST_BATCH : options.get("--match");

        List<String> all = listTextures(source);
        List<String> textures = select(all, options.containsKey("--all"), pattern);
        Map<String, Double> weights = new HashMap<>();
        if (options.containsKey("--no-soft-blend")) {
            textures.forEach(rel -> weights.put(rel, 1.0));
        } else {
            System.out.println("Measuring detail");
            weights.putAll(detailWeights(source, all, textures));
        }
        Properties manifest = loadManifest(dest);
        List<String> todo = new ArrayList<>();
        for (String rel : textures) {
            boolean exists = Files.exists(dest.resolve(rel));
            if (exists && manifest.getProperty(rel) == null && !options.containsKey("--overwrite-unmanaged")) {
                System.out.println("Skipping " + rel + ": the destination has a file this tool didn't write");
            } else if (options.containsKey("--force") || !exists
                    || !record(source.resolve(rel), weights.get(rel), settings).equals(manifest.getProperty(rel))) {
                todo.add(rel);
            }
        }

        System.out.println(textures.size() + " textures selected, " + todo.size() + " to upscale, "
                + (textures.size() - todo.size()) + " up to date");
        if (options.containsKey("--dry-run")) {
            todo.forEach(rel -> System.out.println("  " + rel));
            if (!options.containsKey("--all")) {
                System.out.println("Left out (--all to include them):");
                EXCLUDED.forEach((path, reason) -> System.out.println("  " + path + ": " + reason));
            }
            return;
        }
        if (todo.isEmpty()) {
            return;
        }

        long started = System.nanoTime();
        Path work = Files.createTempDirectory("hd-textures-");
        List<String> failed = new ArrayList<>();
        try {
            Path workIn = Files.createDirectories(work.resolve("in"));
            Path workOut = Files.createDirectories(work.resolve("out"));
            System.out.println("Preparing");
            for (int i = 0; i < todo.size(); i++) {
                if (weights.get(todo.get(i)) > 0) {
                    Planes[] padded = prepare(read(source.resolve(todo.get(i))), pad, padMode);
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
                    Planes[] padded = prepare(original, pad, padMode);
                    Planes[] detailed = null;
                    if (weight > 0) {
                        Path colour = workOut.resolve(String.format("%05d_c.png", i));
                        Path alpha = workOut.resolve(String.format("%05d_a.png", i));
                        if (!Files.exists(colour) || (padded[1] != null && !Files.exists(alpha))) {
                            throw new IOException("Real-ESRGAN wrote no output");
                        }
                        detailed = new Planes[]{read(colour), padded[1] != null ? read(alpha) : null};
                    }
                    writeAtomically(finish(original, padded, detailed, weight, pad, matchLevels), dest.resolve(rel));
                } catch (IOException | IllegalStateException e) {
                    failed.add(rel + ": " + e.getMessage());
                    continue;
                }
                manifest.setProperty(rel, record(source.resolve(rel), weight, settings));
                saveManifest(dest, manifest);
            }
        } finally {
            deleteFolder(work);
        }

        if (!options.containsKey("--no-git-exclude")) {
            addGitExclude(dest);
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
        if (!options.getOrDefault("--gpu", "auto").equals("auto")) {
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

    static List<String> select(List<String> textures, boolean includeExcluded, String pattern) {
        Pattern regex = pattern != null ? Pattern.compile(pattern) : null;
        return textures.stream()
                .filter(rel -> includeExcluded || !isExcluded(rel))
                .filter(rel -> regex == null || regex.matcher(rel).find())
                .toList();
    }

    static boolean isExcluded(String rel) {
        return EXCLUDED.keySet().stream().anyMatch(path -> rel.equalsIgnoreCase(path)
                || (path.endsWith("/") && rel.toLowerCase(Locale.ROOT).startsWith(path.toLowerCase(Locale.ROOT))));
    }

    /**
     * How much of each texture comes from Real-ESRGAN, the rest from a bicubic
     * resize. The frames of an animation (names that differ only in their
     * trailing number) share one weight, so an animation doesn't change style
     * from frame to frame.
     *
     * @param source the source folder
     * @param all all the textures in it, for the animation frames
     * @param textures the textures to weigh
     * @return the weight of each texture, 0-1
     * @throws IOException if reading fails
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
     * and alpha. Crisp, detailed textures score above 1, soft ones (blurred
     * glows, smoke) well below.
     *
     * @param image the texture
     * @return the score, null for a flat image
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
     * Pads the colour and the alpha for upscaling. The colour of the visible
     * pixels is spread into the invisible ones first.
     *
     * @param image the texture
     * @param pad how many pixels to add on each side
     * @param mode wrap, edge or reflect
     * @return the padded colour, and the padded alpha as one band or null if
     * the texture has no alpha or only one alpha value
     */
    static Planes[] prepare(Planes image, int pad, String mode) {
        float[][] rgb = Arrays.copyOf(image.bands(), 3);
        Planes alpha = null;
        if (image.hasAlpha()) {
            float[] channel = image.bands()[3];
            if (!isConstant(channel)) {
                alpha = pad(new Planes(image.width(), image.height(), new float[][]{channel}), pad, mode);
            }
            rgb = bleed(rgb, channel, image.width(), image.height());
        }
        return new Planes[]{pad(new Planes(image.width(), image.height(), rgb), pad, mode), alpha};
    }

    static Planes pad(Planes image, int pad, String mode) {
        int width = image.width() + 2 * pad;
        int height = image.height() + 2 * pad;
        float[][] bands = new float[image.bands().length][width * height];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int from = padIndex(y - pad, image.height(), mode) * image.width() + padIndex(x - pad, image.width(), mode);
                for (int band = 0; band < bands.length; band++) {
                    bands[band][y * width + x] = image.bands()[band][from];
                }
            }
        }
        return new Planes(width, height, bands);
    }

    private static int padIndex(int index, int size, String mode) {
        return switch (mode) {
            case "wrap" ->
                Math.floorMod(index, size);
            case "edge" ->
                Math.clamp(index, 0, size - 1);
            default -> {
                if (size == 1) {
                    yield 0;
                }
                int mirrored = Math.floorMod(index, 2 * size - 2);
                yield mirrored < size ? mirrored : 2 * size - 2 - mirrored;
            }
        };
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
     * them, eight pixels deep, and gives the rest the mean visible colour. The
     * colour under fully transparent pixels is often black or garbage, and the
     * upscaler would otherwise smear it into the visible edges.
     *
     * @param rgb the colour bands
     * @param alpha the alpha band
     * @param width the width
     * @param height the height
     * @return the new colour bands
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
     * Makes the HD texture: crops the padding off, blends Real-ESRGAN's output
     * with a bicubic resize, keeps the original's levels and puts the alpha
     * back.
     *
     * @param original the original texture
     * @param padded the padded colour and alpha, from
     * {@link #prepare(Planes, int, String)}
     * @param detailed Real-ESRGAN's output for the padded colour and alpha,
     * null if not used
     * @param weight how much of the result comes from Real-ESRGAN
     * @param pad the padding on each side
     * @param matchLevels whether to keep the original's brightness and alpha
     * levels
     * @return the HD texture
     */
    static Planes finish(Planes original, Planes[] padded, Planes[] detailed, double weight, int pad, boolean matchLevels) {
        int width = original.width();
        int height = original.height();
        float[][] rgb = blend(padded[0], detailed == null ? null : detailed[0], weight, pad, width, height);
        if (matchLevels) {
            for (int band = 0; band < 3; band++) {
                rgb[band] = keepLevels(rgb[band], original.bands()[band], width, height);
            }
        }
        if (!original.hasAlpha()) {
            return new Planes(width * SCALE, height * SCALE, rgb);
        }
        float[] alpha = original.bands()[3];
        float[] channel = new float[rgb[0].length];
        if (padded[1] == null) {
            Arrays.fill(channel, alpha[0]);
        } else {
            float[][] blended = blend(padded[1], detailed == null ? null : detailed[1], weight, pad, width, height);
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
            if (!binary && matchLevels) {
                channel = keepLevels(channel, alpha, width, height);
            }
        }
        return new Planes(width * SCALE, height * SCALE, new float[][]{rgb[0], rgb[1], rgb[2], channel});
    }

    /**
     * Real-ESRGAN's output blended with a bicubic resize by the weight, with
     * the padding cropped off
     */
    private static float[][] blend(Planes padded, Planes detailed, double weight, int pad, int width, int height) {
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
                    int from = (y + pad * SCALE) * paddedWidth + x + pad * SCALE;
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
     * Brings the local brightness, colour or opacity back to the original's.
     * Shrunk back to the original size, the upscaled band is compared with the
     * original; the difference, blurred so it carries only the broad levels and
     * not the pixel blocks, is spread back over the upscaled band.
     *
     * @param upscaled the upscaled band
     * @param original the original band
     * @param width the original width
     * @param height the original height
     * @return the corrected band
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
     * Resizes a band 4x the way Pillow does, so the result matches its: a
     * separable convolution, horizontal pass first, with the kernel cut and
     * renormalised at the edges.
     *
     * @param band the band
     * @param width its width
     * @param height its height
     * @param bicubic true for bicubic with Pillow's 8-bit fixed point weights
     * and rounding, false for bilinear on floating point values
     * @return the resized band
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

    // --- Files ----------------------------------------------------------------------------------
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

    static void write(Planes image, Path path) throws IOException {
        BufferedImage out = new BufferedImage(image.width(), image.height(),
                image.hasAlpha() ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < image.height(); y++) {
            for (int x = 0; x < image.width(); x++) {
                int argb = image.hasAlpha() ? 0 : 0xFF000000;
                for (int band = 0; band < image.bands().length; band++) {
                    int value = (int) Math.clamp(Math.rint(image.bands()[band][y * image.width() + x]), 0, 255);
                    argb |= value << (band == 3 ? 24 : 16 - 8 * band);
                }
                out.setRGB(x, y, argb);
            }
        }
        if (!ImageIO.write(out, "png", path.toFile())) {
            throw new IOException("No PNG writer");
        }
    }

    private static void writeAtomically(Planes image, Path path) throws IOException {
        Files.createDirectories(path.getParent());
        Path temporary = path.resolveSibling(path.getFileName() + ".tmp");
        write(image, temporary);
        Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING);
    }

    private static void deleteFolder(Path folder) throws IOException {
        try (Stream<Path> paths = Files.walk(folder)) {
            for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
                Files.delete(path);
            }
        }
    }

    // --- Bookkeeping ----------------------------------------------------------------------------
    /**
     * What the manifest keeps for a texture: when it changes, the texture is
     * upscaled again
     */
    private static String record(Path source, double weight, String settings) throws IOException {
        return Files.getLastModifiedTime(source).toInstant() + " " + weight + " " + settings;
    }

    static Properties loadManifest(Path dest) throws IOException {
        Properties manifest = new Properties();
        Path path = dest.resolve(MANIFEST_NAME);
        if (Files.exists(path)) {
            try (InputStream in = Files.newInputStream(path)) {
                manifest.load(in);
            }
        }
        return manifest;
    }

    static void saveManifest(Path dest, Properties manifest) throws IOException, InterruptedException {
        Files.createDirectories(dest);
        Path path = dest.resolve(MANIFEST_NAME);
        Path temporary = dest.resolve(MANIFEST_NAME + ".tmp");
        try (OutputStream out = Files.newOutputStream(temporary)) {
            manifest.store(out, "HD textures written by " + HdTextures.class.getName());
        }

        // On Windows another program (a build copying the assets, a virus
        // scanner) can keep the manifest open for a moment
        for (int attempt = 1;; attempt++) {
            try {
                Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING);
                return;
            } catch (AccessDeniedException e) {
                if (attempt == MANIFEST_RETRIES) {
                    throw e;
                }
                Thread.sleep(250);
            }
        }
    }

    /**
     * Deletes the HD textures this tool wrote, the folders they leave empty and
     * the manifest
     *
     * @param dest the destination folder
     * @return how many textures were deleted
     * @throws IOException if deleting fails
     */
    static int clean(Path dest) throws IOException {
        int removed = 0;
        for (String rel : loadManifest(dest).stringPropertyNames()) {
            Path path = dest.resolve(rel);
            if (Files.deleteIfExists(path)) {
                removed++;
            }
            for (Path folder = path.getParent(); !folder.equals(dest) && folder.startsWith(dest); folder = folder.getParent()) {
                try (Stream<Path> children = Files.list(folder)) {
                    if (children.findAny().isPresent()) {
                        break;
                    }
                }
                Files.delete(folder);
            }
        }
        Files.deleteIfExists(dest.resolve(MANIFEST_NAME));
        return removed;
    }

    /**
     * Keeps the HD textures out of git status, so they don't get committed by
     * accident
     */
    private static void addGitExclude(Path dest) throws IOException, InterruptedException {
        String top = git(dest, "--show-toplevel");
        String exclude = git(dest, "--git-path", "info/exclude");
        if (top == null || exclude == null) {
            return;
        }
        String rel = Paths.get(top).toRealPath().relativize(dest.toRealPath()).toString().replace('\\', '/');
        Path excludePath = dest.resolve(exclude);
        List<String> existing = Files.exists(excludePath) ? Files.readAllLines(excludePath) : List.of();
        List<String> missing = Stream.of("/" + rel + "/**/*.png", "/" + rel + "/**/*.PNG", "/" + rel + "/" + MANIFEST_NAME)
                .filter(line -> !existing.contains(line)).toList();
        if (!missing.isEmpty()) {
            Files.createDirectories(excludePath.getParent());
            Files.writeString(excludePath, "# HD textures from ./gradlew hdTextures\n" + String.join("\n", missing) + "\n",
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        }
    }

    private static String git(Path folder, String... args) throws InterruptedException {
        List<String> command = new ArrayList<>(List.of("git", "rev-parse"));
        command.addAll(List.of(args));
        try {
            Process process = new ProcessBuilder(command).directory(folder.toFile())
                    .redirectError(ProcessBuilder.Redirect.DISCARD).start();
            String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();
            return process.waitFor() == 0 ? output : null;
        } catch (IOException e) {
            return null;
        }
    }
}
