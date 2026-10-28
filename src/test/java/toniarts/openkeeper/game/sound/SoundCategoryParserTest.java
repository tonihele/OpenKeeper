package toniarts.openkeeper.game.sound;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.utils.PathUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies the {@code SFX.map} record layout: the header, the entry, the
 * contiguous block of event headers, and the two level nesting of group headers
 * with the per group samples and links. Also parses real {@code SFX.map} files
 * byte for byte as ground truth.
 */
class SoundCategoryParserTest {

    private static final int SFX_HEADER_SIZE = 0x1C;
    private static final int SFX_ENTRY_COUNT_OFFSET = 0x18;
    private static final int EVENT_SIZE = 20;
    private static final int ENTRY_SIZE = 24;
    private static final int GROUP_SIZE = 42;
    private static final int SAMPLE_SIZE = 16;
    private static final int LINK_SIZE = 8;

    /** Real {@code front_endSFX.map}, 248 bytes, 4 events with 1, 1, 0 and 0 groups. */
    private static final String FRONT_END_SFX_MAP_BASE64 = "ACxh6dAx0hG0CQCwyZPyAwAAAAAAAAAAAQAAAAQAAAAAAAAACQABAAAAgD8AAAB"
            + "AAAAAPwoDAAABAAAAAAAAAJ8PAAAAAgAADwMAAAEAAAAAAAAAnw8AAAACAADqAgAAAAAAAAAAAADoAwAABgAAAOsCAAAAAAAAAAAA"
            + "AOgDAAAGAAAAAQAAAAAAAAAAAAAAZGQAAAAAAAAyMgAAAAAAAAAA//8AAAAAoEFHBHTwCAAAAP//AAC/AAAAAQAAAAEAAAAAAAA"
            + "AAAAAAAoKAAAAAAAAMjIAAAAAAAAAAP//AAAAAKBBRwR08AEAAAD//wAAjAQAAAIAAAA=";

    /** Real {@code gui_button_defaultSFX.map}, 130 bytes, a single event and group. */
    private static final String GUI_BUTTON_DEFAULT_SFX_MAP_BASE64 = "ACxh6dAx0hG0CQCwyZPyAwAAAAAAAAAAAQAAAAEAAAAAA"
            + "AAACQABAAAAgD8AAABAAAAAPwQBAAABAAAAAAAAAJ8PAAABAgAAAQAAAAAAAAAAAAAAZGQAAAAAAAAyMgAAAAAAAAAA//8AAAAAoEFHBH"
            + "TwFAAAAP//AAB7AwAAAQAAAA==";

    private String previousDkFolder;
    private Path tempFolder;

    @BeforeEach
    void setUp() throws IOException {
        previousDkFolder = PathUtils.getDKIIFolder();
        tempFolder = Files.createTempDirectory("OpenKeeperSoundTest");
        PathUtils.setDKIIFolder(tempFolder.toString());
    }

    @AfterEach
    void tearDown() {
        PathUtils.setDKIIFolder(previousDkFolder == null ? "" : previousDkFolder);
    }

    @Test
    void parsesEventsGroupsSamplesAndLinks() throws IOException {
        // event 0x1C5 with 2 groups: the first one has a sample and a link to the second group
        Event event = event(0x1C5, 0, 10,
                groupOfWithLinks(100, 100, 0, 0, 0, 0, 3, sample(7, 30, 1234), link(2, 3, 4)),
                groupOf(90, 110, -5, 5, 0, 0, 5, sample(9, 70, 5678)));

        SoundCategory category = parse("speech_mentor", event);

        assertEquals("SPEECH_MENTOR", category.getName());
        assertEquals(1, category.getEvents().size());
        assertEquals(2, category.getChannelMask());

        SoundEvent parsed = category.getEvent(0x1C5);
        assertNotNull(parsed);
        assertEquals(10, parsed.getPriority());
        assertEquals(2, parsed.getGroups().size());

        SoundEventGroup first = parsed.getGroup(1);
        assertEquals(100, first.getVolumeMin());
        assertEquals(3, first.getCumulativeWeight());
        assertEquals(1, first.getSamples().size());
        assertEquals(1, first.getLinks().size());

        // the sample of the first group must not be mixed up by the link record
        assertEquals(7, first.getSamples().get(0).getClip());
        assertEquals(1234, first.getSamples().get(0).getDurationMs());

        // the link must point at the second group of the same event
        assertSame(parsed.getGroup(2), first.getLinks().get(0).getTarget());
        assertTrue(first.getLinks().get(0).matchesProperty(3));
        assertTrue(first.getLinks().get(0).matchesProperty(4));
        assertFalse(first.getLinks().get(0).matchesProperty(5));

        SoundEventGroup second = parsed.getGroup(2);
        assertEquals(90, second.getVolumeMin());
        assertEquals(110, second.getVolumeMax());
        assertEquals(-5, second.getPitchMin());
        assertEquals(9, second.getSamples().get(0).getClip());
        assertEquals(5678, second.getSamples().get(0).getDurationMs());
    }

    /**
     * The samples and links of a group follow all group headers of the event, and
     * each group keeps its own samples and links in order. Reading the children
     * right after their group header, or all samples before all links, shifts
     * every record of an event that has more than one group.
     */
    @Test
    void keepsGroupChildrenInOrderWithinAnEvent() throws IOException {
        Event event = event(7, 0, 0,
                groupOfWithLinks(100, 100, 0, 0, 0, 0, 1, sample(10, 10, 100), link(3, 0, 100)),
                groupOf(100, 100, 0, 0, 0, 0, 2, sample(20, 20, 200)),
                groupOfWithLinks(100, 100, 0, 0, 0, 0, 3, sample(30, 30, 300), link(1, 0, 5)));

        SoundEvent parsed = parse("speech_mentor", event).getEvent(7);

        assertNotNull(parsed);
        assertEquals(3, parsed.getGroups().size());

        assertEquals(10, parsed.getGroup(1).getSamples().get(0).getClip());
        assertEquals(100, parsed.getGroup(1).getSamples().get(0).getDurationMs());
        assertEquals(20, parsed.getGroup(2).getSamples().get(0).getClip());
        assertEquals(200, parsed.getGroup(2).getSamples().get(0).getDurationMs());
        assertEquals(30, parsed.getGroup(3).getSamples().get(0).getClip());
        assertEquals(300, parsed.getGroup(3).getSamples().get(0).getDurationMs());

        assertEquals(1, parsed.getGroup(1).getLinks().size());
        assertEquals(0, parsed.getGroup(2).getLinks().size());
        assertEquals(1, parsed.getGroup(3).getLinks().size());

        assertSame(parsed.getGroup(3), parsed.getGroup(1).getLinks().get(0).getTarget());
        assertSame(parsed.getGroup(1), parsed.getGroup(3).getLinks().get(0).getTarget());
    }

    /** A group may hold several samples with running total weights. */
    @Test
    void parsesSeveralSamplesOfOneGroup() throws IOException {
        Event event = event(11, 0, 0,
                groupOf(100, 100, 0, 0, 0, 0, 0, sample(1, 100, 500), sample(2, 400, 900),
                        sample(3, 900, 1200)));

        SoundEvent parsed = parse("speech_mentor", event).getEvent(11);

        assertNotNull(parsed);
        SoundEventGroup group = parsed.getGroup(1);
        assertEquals(3, group.getSamples().size());
        assertEquals(1, group.getSamples().get(0).getClip());
        assertEquals(2, group.getSamples().get(1).getClip());
        assertEquals(3, group.getSamples().get(2).getClip());
        assertEquals(900, group.getSamples().get(2).getCumulativeWeight());
        assertEquals(900, SoundRng.totalWeightOf(group.getSamples(), SoundSample::getCumulativeWeight));
    }

    /** Several events of one entry are stored as one contiguous block of headers. */
    @Test
    void keepsEventsOfAnEntryContiguous() throws IOException {
        Event first = event(1, 0, 0, groupOf(100, 100, 0, 0, 0, 0, 1, sample(1, 10, 100)));
        Event second = event(2, 0, 0, groupOf(100, 100, 0, 0, 0, 0, 2, sample(2, 20, 200)));
        Event third = event(3, 0, 0);

        SoundCategory category = parse("speech_mentor", first, second, third);

        assertEquals(3, category.getEvents().size());
        assertEquals(1, category.getEvent(1).getGroup(1).getSamples().get(0).getClip());
        assertEquals(2, category.getEvent(2).getGroup(1).getSamples().get(0).getClip());
        assertEquals(0, category.getEvent(3).getGroups().size());
    }

    @Test
    void legacyGroupFacadeIsCompleteAfterParsing() throws IOException {
        SoundCategory category = parse("speech_mentor",
                event(1, 0, 0, groupOf(100, 100, 0, 0, 0, 0, 1, sample(1, 10, 100))),
                event(2, 0, 0, groupOf(100, 100, 0, 0, 0, 0, 1, sample(2, 10, 200))));

        assertEquals(2, category.getGroups().size());
        assertNotNull(category.getGroup(1));
        assertNotNull(category.getGroup(2));
        assertEquals(category.getEvent(1), category.getGroup(1).getEvent());
    }

    @Test
    void unknownEventIsNull() throws IOException {
        SoundCategory category = parse("speech_mentor",
                event(1, 0, 0, groupOf(100, 100, 0, 0, 0, 0, 1, sample(1, 10, 100))));

        assertNull(category.getEvent(4711));
        assertFalse(category.hasEvent(4711));
        assertTrue(category.hasEvent(1));
    }

    @Test
    void parsesRealFrontEndFile() throws IOException {
        SoundCategory category = parse("front_end", Base64.getDecoder().decode(FRONT_END_SFX_MAP_BASE64));

        assertEquals(4, category.getEvents().size());

        assertEquals(1, category.getEvent(778).getGroups().size());
        assertEquals(1, category.getEvent(783).getGroups().size());
        assertEquals(0, category.getEvent(746).getGroups().size());
        assertEquals(0, category.getEvent(747).getGroups().size());

        for (SoundEvent event : category.getEvents().values()) {
            for (SoundEventGroup group : event.getGroups()) {
                String where = "event " + event.getId();
                assertEquals(1, group.getSamples().size(), where);
                assertEquals(0, group.getLinks().size(), where);
                // event 783 is the one odd event of the file: quieter, and played from the second bank
                int volume = event.getId() == 783 ? 10 : 100;
                assertEquals(volume, group.getVolumeMin(), where);
                assertEquals(volume, group.getVolumeMax(), where);
                assertTrue(group.getSamples().get(0).getDurationMs() > 0, where);
                assertEquals(event.getId() == 783 ? 2 : 1, group.getSamples().get(0).getBankRef(), where);
            }
        }
    }

    @Test
    void parsesRealGuiButtonDefaultFile() throws IOException {
        SoundCategory category = parse("gui_button_default",
                Base64.getDecoder().decode(GUI_BUTTON_DEFAULT_SFX_MAP_BASE64));

        assertEquals(1, category.getEvents().size());

        SoundEvent event = category.getEvents().values().iterator().next();
        assertEquals(1, event.getGroups().size());

        SoundEventGroup group = event.getGroups().get(0);
        assertEquals(1, group.getSamples().size());
        assertEquals(0, group.getLinks().size());
        assertEquals(100, group.getVolumeMin());
        assertEquals(100, group.getVolumeMax());
        assertEquals(0, group.getLinks().size());
        assertTrue(group.getSamples().get(0).getClip() >= 0);
        assertTrue(group.getSamples().get(0).getDurationMs() > 0);
    }

    private SoundCategory parse(String name, byte[] data) throws IOException {
        Path sfxFolder = tempFolder.resolve("Data").resolve("Sound").resolve("sfx").resolve(name.toLowerCase());
        Files.createDirectories(sfxFolder);
        Files.write(sfxFolder.resolve(name.toLowerCase() + "SFX.map"), data);
        return new SoundCategory(name.toUpperCase(), false);
    }

    private SoundCategory parse(String name, Event... events) throws IOException {
        return parse(name, serialize(events));
    }

    /**
     * Serializes an {@code SFX.map} the way the game does: header, entry, then
     * every event header of the entry, then per event every group header followed
     * by the samples and links of each group in order.
     */
    private static byte[] serialize(Event... events) {
        int size = SFX_ENTRY_COUNT_OFFSET + Integer.BYTES + ENTRY_SIZE;
        for (Event event : events) {
            size += EVENT_SIZE + event.groups.size() * GROUP_SIZE;
            for (Group group : event.groups) {
                size += group.samples.size() * SAMPLE_SIZE + group.links.size() * LINK_SIZE;
            }
        }

        ByteBuffer buffer = ByteBuffer.allocate(size).order(ByteOrder.LITTLE_ENDIAN);
        // the 16 byte magic and the two unused header words are not interpreted
        buffer.position(SFX_ENTRY_COUNT_OFFSET);
        buffer.putInt(1); // entry count
        buffer.putInt(events.length); // event count of the entry
        buffer.putInt(0); // pointer, not interpreted by the parser
        buffer.put((byte) 2); // channel mask
        buffer.put((byte) 0);
        buffer.putShort((short) 1); // eax environment
        buffer.putFloat(1.0f); // eax volume
        buffer.putFloat(1.0f); // eax decay
        buffer.putFloat(1.0f); // eax damping

        // every event header of the entry, as one contiguous block
        for (Event event : events) {
            buffer.putInt(event.id);
            buffer.putInt(event.groups.size());
            buffer.putInt(0);
            buffer.putInt(event.priority);
            buffer.putInt(event.flags);
        }

        // then the groups of each event, event by event
        for (Event event : events) {
            for (Group group : event.groups) {
                buffer.putInt(group.samples.size());
                buffer.putInt(group.links.size());
                buffer.putInt(0);
                buffer.put((byte) group.volumeMin);
                buffer.put((byte) group.volumeMax);
                buffer.put((byte) group.pitchMin);
                buffer.put((byte) group.pitchMax);
                buffer.putShort((short) group.delayMin);
                buffer.putShort((short) group.delayMax);
                buffer.putShort((short) 0);
                buffer.put((byte) 0); // property key 1
                buffer.put((byte) 0);
                buffer.put((byte) 0); // property flags 1
                buffer.put((byte) 0);
                buffer.put((byte) 0); // property key 2
                buffer.put((byte) 0);
                buffer.put((byte) 0); // property flags 2
                buffer.put((byte) 0);
                buffer.putShort((short) group.cumulativeWeight);
                buffer.putShort((short) 0);
                buffer.putFloat(group.maxDistance);
                buffer.putInt(0);
            }

            for (Group group : event.groups) {
                for (Sample sample : group.samples) {
                    buffer.putInt(sample.clip);
                    buffer.putInt(sample.cumulativeWeight);
                    buffer.putInt(sample.durationMs);
                    buffer.putShort((short) sample.archiveRef);
                    buffer.putShort((short) 0);
                }
                for (Link link : group.links) {
                    buffer.putShort((short) link.toGroup);
                    buffer.putInt(0);
                    buffer.put((byte) link.propertyMin);
                    buffer.put((byte) link.propertyMax);
                }
            }
        }
        return buffer.array();
    }

    private static Event event(int id, int flags, int priority, Group... groups) {
        return new Event(id, flags, priority, groups);
    }

    private static Group groupOf(int volumeMin, int volumeMax, int pitchMin, int pitchMax, int delayMin, int delayMax,
            int cumulativeWeight, Sample... samples) {
        return new Group(volumeMin, volumeMax, pitchMin, pitchMax, delayMin, delayMax, cumulativeWeight,
                java.util.Arrays.asList(samples), new ArrayList<>());
    }

    private static Group groupOfWithLinks(int volumeMin, int volumeMax, int pitchMin, int pitchMax, int delayMin,
            int delayMax, int cumulativeWeight, Sample sample, Link... links) {
        List<Sample> samples = new ArrayList<>();
        samples.add(sample);
        return new Group(volumeMin, volumeMax, pitchMin, pitchMax, delayMin, delayMax, cumulativeWeight, samples,
                java.util.Arrays.asList(links));
    }

    private static Sample sample(int clip, int cumulativeWeight, int durationMs) {
        return new Sample(clip, cumulativeWeight, durationMs);
    }

    private static Link link(int toGroup, int propertyMin, int propertyMax) {
        return new Link(toGroup, propertyMin, propertyMax);
    }

    private static final class Event {

        private final int id;
        private final int flags;
        private final int priority;
        private final List<Group> groups;

        Event(int id, int flags, int priority, Group[] groups) {
            this.id = id;
            this.flags = flags;
            this.priority = priority;
            this.groups = java.util.Arrays.asList(groups);
        }
    }

    private static final class Group {

        private final int volumeMin;
        private final int volumeMax;
        private final int pitchMin;
        private final int pitchMax;
        private final int delayMin;
        private final int delayMax;
        private final int cumulativeWeight;
        private final List<Sample> samples;
        private final List<Link> links;
        private final float maxDistance = 0.0f;

        Group(int volumeMin, int volumeMax, int pitchMin, int pitchMax, int delayMin, int delayMax,
                int cumulativeWeight, List<Sample> samples, List<Link> links) {
            this.volumeMin = volumeMin;
            this.volumeMax = volumeMax;
            this.pitchMin = pitchMin;
            this.pitchMax = pitchMax;
            this.delayMin = delayMin;
            this.delayMax = delayMax;
            this.cumulativeWeight = cumulativeWeight;
            this.samples = samples;
            this.links = links;
        }
    }

    private static final class Sample {

        private final int clip;
        private final int cumulativeWeight;
        private final int durationMs;
        private final int archiveRef;

        Sample(int clip, int cumulativeWeight, int durationMs) {
            this.clip = clip;
            this.cumulativeWeight = cumulativeWeight;
            this.durationMs = durationMs;
            this.archiveRef = 1;
        }
    }

    private static final class Link {

        private final int toGroup;
        private final int propertyMin;
        private final int propertyMax;

        Link(int toGroup, int propertyMin, int propertyMax) {
            this.toGroup = toGroup;
            this.propertyMin = propertyMin;
            this.propertyMax = propertyMax;
        }
    }
}