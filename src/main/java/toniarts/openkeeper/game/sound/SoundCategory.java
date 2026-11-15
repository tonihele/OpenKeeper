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
import java.io.IOException;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.annotation.Nullable;
import toniarts.openkeeper.game.data.IIndexable;
import toniarts.openkeeper.tools.convert.ConversionUtils;
import toniarts.openkeeper.tools.convert.sound.BankMapFile;
import toniarts.openkeeper.tools.convert.sound.BankMapFileEntry;
import toniarts.openkeeper.tools.convert.sound.SdtFile;
import toniarts.openkeeper.utils.PathUtils;

/**
 * A sound category, i.e. one {@code SFX.map} plus its {@code BANK.map}.
 * <p>
 * The link records that follow a group in {@code SFX.map} have to be read as well,
 * otherwise every sample after a group with links points at the wrong clip.
 */
public final class SoundCategory {

    private static final Logger logger = System.getLogger(SoundCategory.class.getName());

    private static final int SFX_HEADER_SIZE = 0x1C;

    /** The entry count is stored in the last word of the header, before the first entry. */
    private static final int SFX_ENTRY_COUNT_OFFSET = 0x18;

    public static final String SPEECH_PREFIX = "SPEECH";
    public static final String SPEECH_MENTOR = "SPEECH_MENTOR";
    public static final String SPEECH_MULTIPLAYER = "SPEECH_MULTIPLAYER";
    public static final String SPEECH_HORNY = "SPEECH_HORNY";
    public static final String SPEECH_MYPETDUNGEON = "SPEECH_MYPETDUNGEON";

    private static final Map<String, SoundCategory> CACHE = new HashMap<>();

    private final String folder;
    private final String name;
    private final Map<Integer, SoundEvent> events = new HashMap<>();
    private final Map<Integer, SoundGroup> groups = new HashMap<>();

    private int channelMask;
    private int eaxEnvironment;
    private float eaxVolume;
    private float eaxDecay;
    private float eaxDamping;

    public SoundCategory(String name, boolean useGlobal) {
        if (name == null || name.isEmpty()) {
            throw new RuntimeException("Sound category is empty");
        }

        if (useGlobal) {
            this.folder = PathUtils.DKII_SFX_GLOBAL_FOLDER;
        } else {
            this.folder = PathUtils.DKII_SFX_FOLDER + name.toLowerCase() + File.separator;
        }

        this.name = name;
        parseEvents();
    }

    /**
     * Creates a category from already parsed events, without touching the disk.
     * Only for tests and for programmatic sound definitions.
     */
    SoundCategory(String name, int channelMask, Map<Integer, SoundEvent> events) {
        if (name == null || name.isEmpty()) {
            throw new RuntimeException("Sound category is empty");
        }

        this.name = name;
        this.folder = "";
        this.channelMask = channelMask;
        this.events.putAll(events);
        for (Map.Entry<Integer, SoundEvent> entry : this.events.entrySet()) {
            groups.put(entry.getKey(), new SoundGroup(this, entry.getValue()));
        }
    }

    /**
     * Loads a category, or returns the cached one. The same category name may
     * exist both in the global and in a non global folder.
     *
     * @return the category, {@code null} if it does not exist or has no events
     */
    @Nullable
    public static synchronized SoundCategory load(String category, boolean useGlobal) {
        if (category == null || category.isEmpty()) {
            return null;
        }

        String key = (useGlobal ? "global:" : "local:") + category.toUpperCase(Locale.ROOT);
        SoundCategory result = CACHE.get(key);
        if (result != null) {
            return result;
        }

        try {
            result = new SoundCategory(category, useGlobal);
            if (result.getEvents().isEmpty()) {
                logger.log(Level.WARNING, "Sound category {0} has no events", category);
                return null;
            }
            CACHE.put(key, result);
        } catch (Exception e) {
            logger.log(Level.WARNING, "Sound category {0} does not exist", category);
            result = null;
        }
        return result;
    }

    public String getName() {
        return name;
    }

    /**
     * Legacy view of the events of this category.
     */
    public Map<Integer, SoundGroup> getGroups() {
        return groups;
    }

    public Map<Integer, SoundEvent> getEvents() {
        return Collections.unmodifiableMap(events);
    }

    @Nullable
    public SoundEvent getEvent(int id) {
        return events.get(id);
    }

    @Nullable
    public SoundEvent getEvent(IIndexable type) {
        return events.get((int) type.getId());
    }

    public boolean hasEvent(int id) {
        return events.containsKey(id);
    }

    @Nullable
    public SoundGroup getGroup(int id) {
        SoundGroup group = groups.get(id);
        if (group != null) {
            return group;
        }
        SoundEvent event = events.get(id);
        if (event == null) {
            return null;
        }
        group = new SoundGroup(this, event);
        groups.put(id, group);
        return group;
    }

    @Nullable
    public SoundGroup getGroup(IIndexable type) {
        return getGroup((int) type.getId());
    }

    public boolean hasGroup(int id) {
        return events.containsKey(id);
    }

    public int getChannelMask() {
        return channelMask;
    }

    public int getEaxEnvironment() {
        return eaxEnvironment;
    }

    public float getEaxVolume() {
        return eaxVolume;
    }

    public float getEaxDecay() {
        return eaxDecay;
    }

    public float getEaxDamping() {
        return eaxDamping;
    }

    /**
     * @return path of the category's {@code SFX.map} file
     */
    @Nullable
    public Path getSfxMapPath() {
        try {
            return Paths.get(PathUtils.getRealFileName(PathUtils.getDKIIFolder(),
                    folder + name.toLowerCase() + "SFX.map"));
        } catch (IOException ex) {
            return null;
        }
    }

    @Nullable
    public BankMapFile getBankMapFile() {
        try {
            Path f = Paths.get(PathUtils.getRealFileName(PathUtils.getDKIIFolder(),
                    folder + name.toLowerCase() + "BANK.map"));

            return new BankMapFile(f);
        } catch (IOException ex) {
            logger.log(Level.ERROR, () -> "Bank file of category " + name + " does not exist", ex);
        }

        return null;
    }

    /**
     * Get SdtFile
     *
     * @param archiveFilename name in BankMapFile
     * @return SdtFile
     */
    @Nullable
    public static SdtFile getSdtFile(String archiveFilename) {
        Path f = findSdtFile(archiveFilename);
        if (f == null) {
            logger.log(Level.WARNING, "SDT File archive {0} not found", archiveFilename);
        }
        return f == null ? null : new SdtFile(f);
    }

    @Nullable
    private static Path findSdtFile(String archiveFilename) {
        for (String part : new String[]{"HD.sdt", "HW.sdt"}) {
            try {
                Path f = Paths.get(PathUtils.getRealFileName(PathUtils.getDKIIFolder(),
                        PathUtils.DKII_SFX_FOLDER + archiveFilename + part));
                if (Files.exists(f)) {
                    return f;
                }
            } catch (IOException ex) {
                // nop
            }
        }
        return null;
    }

    private void parseEvents() {
        Path sfxPath = getSfxMapPath();
        if (sfxPath == null) {
            logger.log(Level.WARNING, "Sfx file of category {0} does not exist", name);
            return;
        }

        byte[] data;
        try {
            data = Files.readAllBytes(sfxPath);
        } catch (IOException ex) {
            logger.log(Level.ERROR, () -> "Failed to read sfx file of category " + name, ex);
            return;
        }
        if (data.length < SFX_HEADER_SIZE) {
            logger.log(Level.WARNING, "Sfx file of category {0} is truncated", name);
            return;
        }

        BankMapFile bank = getBankMapFile();
        Cursor cursor = new Cursor(data);

        try {
cursor.skip(SFX_ENTRY_COUNT_OFFSET);
int entryCount = cursor.readInt();
            for (int i = 0; i < entryCount; i++) {
                int eventCount = cursor.readInt();
                cursor.skipInt();
                channelMask = cursor.readUnsignedByte();
                cursor.skip(1);
                eaxEnvironment = cursor.readUnsignedShort();
                eaxVolume = cursor.readFloat();
                eaxDecay = cursor.readFloat();
                eaxDamping = cursor.readFloat();

                // All event headers of an entry are stored contiguously, before any of its groups
                EventHeader[] headers = new EventHeader[eventCount];
                for (int j = 0; j < eventCount; j++) {
                    headers[j] = new EventHeader(cursor);
                }
                for (EventHeader header : headers) {
                    SoundEvent event = readEvent(cursor, bank, header);
                    events.putIfAbsent(event.getId(), event);
                }
            }
        } catch (RuntimeException ex) {
            logger.log(Level.ERROR, () -> "Failed to parse sfx file of category " + name, ex);
            return;
        }

        if (cursor.position() != data.length) {
            logger.log(Level.WARNING, "Sfx file of category {0} has {1} trailing bytes", name,
                    data.length - cursor.position());
        }

        // eagerly create the legacy group facades, so that getGroups() is complete right after parsing
        for (Map.Entry<Integer, SoundEvent> entry : events.entrySet()) {
            groups.put(entry.getKey(), new SoundGroup(this, entry.getValue()));
        }
    }

    private SoundEvent readEvent(Cursor cursor, @Nullable BankMapFile bank, EventHeader header) {
        // All group headers of the event come first, contiguously
        GroupHeader[] groupHeaders = new GroupHeader[header.groupCount];
        for (int i = 0; i < header.groupCount; i++) {
            groupHeaders[i] = new GroupHeader(cursor);
        }

        // ...and only then, per group in order, that group's samples followed by its links
        List<SoundEventGroup> eventGroups = new ArrayList<>(header.groupCount);
        for (GroupHeader groupHeader : groupHeaders) {
            List<SoundSample> samples = new ArrayList<>(groupHeader.sampleCount);
            for (int i = 0; i < groupHeader.sampleCount; i++) {
                int clip = cursor.readInt();
                int weight = cursor.readInt();
                int duration = cursor.readInt();
                int archiveId = cursor.readUnsignedShort();
                cursor.skip(2);
                samples.add(new SoundSample(clip, weight, duration, archiveId, getArchive(bank, archiveId)));
            }

            List<SoundLink> links = new ArrayList<>(groupHeader.linkCount);
            for (int i = 0; i < groupHeader.linkCount; i++) {
                int toGroup = cursor.readUnsignedShort();
                cursor.skip(4);
                int propertyMin = cursor.readUnsignedByte();
                int propertyMax = cursor.readUnsignedByte();
                links.add(new SoundLink(toGroup, propertyMin, propertyMax));
            }

            eventGroups.add(new SoundEventGroup(groupHeader.volumeMin, groupHeader.volumeMax,
                    groupHeader.pitchMin, groupHeader.pitchMax, groupHeader.delayMin, groupHeader.delayMax,
                    groupHeader.propertyKey1, groupHeader.propertyFlags1, groupHeader.propertyKey2,
                    groupHeader.propertyFlags2, groupHeader.cumulativeWeight, groupHeader.maxDistance, samples,
                    links));
        }

        for (SoundEventGroup eventGroup : eventGroups) {
            for (SoundLink link : eventGroup.getLinks()) {
                int index = link.getGroupIndex() - 1;
                if (index >= 0 && index < eventGroups.size()) {
                    link.setTarget(eventGroups.get(index));
                }
            }
        }

        return new SoundEvent(header.id, header.priority,
                ConversionUtils.parseFlagValue(header.flags, SoundEventFlag.class), eventGroups);
    }

    @Nullable
    private static SoundArchive getArchive(@Nullable BankMapFile bank, int archiveId) {
        if (bank == null || archiveId <= 0 || archiveId > bank.getEntries().length) {
            return null;
        }
        BankMapFileEntry entry = bank.getEntries()[archiveId - 1];
        return SoundArchive.load(entry.getName(), entry.getFlags());
    }

    @Override
    public String toString() {
        return "SoundCategory{" + "folder=" + folder + ", name=" + name + '}';
    }

    /**
     * The 20 byte event header of a {@code SFX.map}. All headers of an entry are
     * stored contiguously, before the groups of any of its events.
     */
    private static final class EventHeader {

        private final int id;
        private final int groupCount;
        private final int priority;
        private final int flags;

        EventHeader(Cursor cursor) {
            id = cursor.readInt();
            groupCount = cursor.readInt();
            cursor.skipInt();
            priority = cursor.readInt();
            flags = cursor.readUnsignedShort();
            // Not flags: the id of the event's first sound parameter, 2 (the intensity) for the music and the ambience
            cursor.skip(2);
        }
    }

    /**
     * The 42 byte group header of a {@code SFX.map}. All group headers of an
     * event are stored contiguously, before the samples and links of any of them.
     */
    private static final class GroupHeader {

        private final int sampleCount;
        private final int linkCount;
        private final int volumeMin;
        private final int volumeMax;
        private final int pitchMin;
        private final int pitchMax;
        private final int delayMin;
        private final int delayMax;
        private final int propertyKey1;
        private final int propertyFlags1;
        private final int propertyKey2;
        private final int propertyFlags2;
        private final int cumulativeWeight;
        private final float maxDistance;

        GroupHeader(Cursor cursor) {
            sampleCount = cursor.readInt();
            linkCount = cursor.readInt();
            cursor.skipInt();
            volumeMin = cursor.readUnsignedByte();
            volumeMax = cursor.readUnsignedByte();
            pitchMin = cursor.readByte();
            pitchMax = cursor.readByte();
            delayMin = cursor.readUnsignedShort();
            delayMax = cursor.readUnsignedShort();
            cursor.skip(2);
            propertyKey1 = cursor.readUnsignedByte();
            cursor.skip(1);
            propertyFlags1 = cursor.readUnsignedByte();
            cursor.skip(1);
            propertyKey2 = cursor.readUnsignedByte();
            cursor.skip(1);
            propertyFlags2 = cursor.readUnsignedByte();
            cursor.skip(1);
            cumulativeWeight = cursor.readUnsignedShort();
            cursor.skip(2);
            maxDistance = cursor.readFloat();
            cursor.skipInt();
        }
    }

    private static final class Cursor {

        private final byte[] data;
        private int position;

        Cursor(byte[] data) {
            this.data = data;
        }

        int position() {
            return position;
        }

        void skip(int bytes) {
            require(bytes);
            position += bytes;
        }

        void skipInt() {
            skip(4);
        }

        int readInt() {
            require(4);
            int value = (data[position] & 0xFF)
                    | ((data[position + 1] & 0xFF) << 8)
                    | ((data[position + 2] & 0xFF) << 16)
                    | ((data[position + 3] & 0xFF) << 24);
            position += 4;
            return value;
        }

        int readUnsignedShort() {
            require(2);
            int value = (data[position] & 0xFF) | ((data[position + 1] & 0xFF) << 8);
            position += 2;
            return value;
        }

        int readUnsignedByte() {
            require(1);
            return data[position++] & 0xFF;
        }

        int readByte() {
            require(1);
            return data[position++];
        }

        float readFloat() {
            return Float.intBitsToFloat(readInt());
        }

        private void require(int bytes) {
            if (position + bytes > data.length) {
                throw new IllegalStateException(
                        String.format("Unexpected end of file at %d, wanted %d more bytes", position, bytes));
            }
        }
    }
}