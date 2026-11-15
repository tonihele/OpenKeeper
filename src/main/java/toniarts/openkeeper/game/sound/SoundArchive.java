/*
 * Copyright (C) 2014-2025 OpenKeeper
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
package toniarts.openkeeper.game.sound;

import java.io.File;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nullable;
import toniarts.openkeeper.tools.convert.sound.BankMapFileEntry.BankFlag;
import toniarts.openkeeper.tools.convert.sound.SdtFile;
import toniarts.openkeeper.tools.convert.sound.SdtFileEntry;
import toniarts.openkeeper.utils.PathUtils;

/**
 * A loaded sound archive ({@code .sdt}) with its clip names.
 * <p>
 * Clip indices used by {@code SFX.map} are 1-based; index {@code 0} is reserved
 * for a missing clip and {@code -1} for "no clip at all".
 */
public class SoundArchive {

    private static final Logger logger = System.getLogger(SoundArchive.class.getName());

    private static final Map<String, SoundArchive> CACHE = new ConcurrentHashMap<>();

    private final String name;
    private final Set<BankFlag> flags;
    private final Path sdtFile;
    private final List<String> clipNames;
    private final List<Integer> clipPcmBytes;

    public SoundArchive(String name, Set<BankFlag> flags, Path sdtFile, List<String> clipNames) {
        this(name, flags, sdtFile, clipNames, List.of());
    }

    /**
     * @param clipPcmBytes per clip, the length of the decoded clip as 16 bit
     * PCM in bytes
     */
    public SoundArchive(String name, Set<BankFlag> flags, Path sdtFile, List<String> clipNames,
            List<Integer> clipPcmBytes) {
        this.name = name;
        this.flags = flags;
        this.sdtFile = sdtFile;
        this.clipNames = clipNames;
        this.clipPcmBytes = clipPcmBytes;
    }

    /**
     * Loads, or returns the already loaded, archive with the given
     * {@code BANK.map} name.
     *
     * @param archiveName name as stored in {@code BANK.map}, e.g.
     * {@code Global\MiscCreatures}
     * @param flags archive flags
     */
    public static SoundArchive load(String archiveName, Set<BankFlag> flags) {
        String normalized = stripTrailingSeparator(PathUtils.convertFileSeparators(archiveName));
        return CACHE.computeIfAbsent(normalized.toLowerCase() + '/' + flags, key -> doLoad(normalized, flags));
    }

    public static void flush() {
        CACHE.clear();
    }

    private static SoundArchive doLoad(String archiveName, Set<BankFlag> flags) {
        List<String> clipNames = new ArrayList<>();
        List<Integer> clipPcmBytes = new ArrayList<>();
        Path sdt = findSdtFile(archiveName);
        if (sdt == null) {
            return new SoundArchive(archiveName, flags, null, clipNames);
        }
        try {
            SdtFile sdtFile = new SdtFile(sdt);
            for (SdtFileEntry entry : sdtFile.getEntries()) {
                clipNames.add(entry == null ? null : SdtFile.fixFileExtension(entry));
                clipPcmBytes.add(entry == null ? 0 : entry.getnSamples());
            }
        } catch (Exception ex) {
            logger.log(Level.WARNING, () -> "Failed to read sound archive " + archiveName, ex);
        }
        return new SoundArchive(archiveName, flags, sdt, clipNames, clipPcmBytes);
    }

    private static Path findSdtFile(String archiveName) {
        for (String part : new String[]{"HD.sdt", "HW.sdt"}) {
            try {
                Path f = Paths.get(PathUtils.getRealFileName(PathUtils.getDKIIFolder(),
                        PathUtils.DKII_SFX_FOLDER + archiveName + part));
                if (Files.exists(f)) {
                    return f;
                }
            } catch (Exception ex) {
                // try the next quality level
            }
        }
        return null;
    }

    /**
     * Archive name relative to the {@code Sound/sfx} folder, native separators.
     */
    public String getName() {
        return name;
    }

    public Set<BankFlag> getFlags() {
        return flags;
    }

    @Nullable
    public Path getSdtFile() {
        return sdtFile;
    }

    public List<String> getClipNames() {
        return Collections.unmodifiableList(clipNames);
    }

    public int getClipCount() {
        return clipNames.size();
    }

    /**
     * @return 1-based clip name or {@code null} for an empty slot
     */
    @Nullable
    public String getClipName(int clip) {
        int index = clip - 1;
        if (index < 0 || index >= clipNames.size()) {
            return null;
        }
        return clipNames.get(index);
    }

    /**
     * The MPEG data decodes into whole frames, a little more than the clip
     * really is. Clips played back to back (the music segments) must be cut to
     * this length to join seamlessly.
     *
     * @param clip 1-based clip index
     * @return length of the decoded clip as 16 bit PCM in bytes, 0 if unknown
     */
    public int getClipPcmBytes(int clip) {
        int index = clip - 1;
        if (index < 0 || index >= clipPcmBytes.size()) {
            return 0;
        }
        return clipPcmBytes.get(index);
    }

    /**
     * @param clip 1-based clip index
     * @return asset path relative to the sounds asset root, always using
     * {@code '/'} separators, or {@code null} if the clip does not exist
     */
    @Nullable
    public String getClipAssetPath(int clip) {
        String clipName = getClipName(clip);
        if (clipName == null) {
            return null;
        }
        return toAssetPath(getFolderName()) + "/" + clipName;
    }

    /**
     * @param clip 1-based clip index
     * @return asset path relative to the sounds asset root using native
     * separators, or {@code null} if the clip does not exist
     */
    @Nullable
    public String getClipAssetPathNative(int clip) {
        String clipName = getClipName(clip);
        if (clipName == null) {
            return null;
        }
        return getFolderName() + File.separator + clipName;
    }

    /**
     * The converted sounds live in a folder named after the {@code .sdt} file,
     * i.e. the {@code BANK.map} name plus the quality suffix ({@code HD}/{@code HW}).
     */
    private String getFolderName() {
        if (sdtFile == null || sdtFile.getFileName() == null) {
            return name;
        }
        String file = sdtFile.getFileName().toString();
        String stem = file.substring(0, file.length() - ".sdt".length());
        return stem.length() >= 2 ? name + stem.substring(stem.length() - 2) : name;
    }

    private static String stripTrailingSeparator(String path) {
        int end = path.length();
        while (end > 0 && (path.charAt(end - 1) == '\\' || path.charAt(end - 1) == '/')) {
            end--;
        }
        return path.substring(0, end);
    }

    /**
     * @return 1-based index of the named clip, or {@code -1} if not found
     */
    public int indexOfClip(String clipName) {
        for (int i = 0; i < clipNames.size(); i++) {
            if (clipNames.get(i) != null && clipNames.get(i).equalsIgnoreCase(clipName)) {
                return i + 1;
            }
        }
        return -1;
    }

    private static String toAssetPath(String nativePath) {
        return nativePath.replace('\\', '/');
    }

    @Override
    public String toString() {
        return "SoundArchive[" + name + ']';
    }
}