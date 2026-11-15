package toniarts.openkeeper.game.sound;

import java.util.EnumSet;
import toniarts.openkeeper.tools.convert.sound.BankMapFileEntry.BankFlag;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.utils.Point;

class AmbienceTest {

    private static final Set<SoundEventFlag> REPEATING_FLAGS = EnumSet.of(SoundEventFlag.REPEATING);
    private static final Set<SoundEventFlag> SEGMENTED_FLAGS = EnumSet.of(SoundEventFlag.REPEATING, SoundEventFlag.SEGMENTED);

    private final List<SilentAudioBackend> backends = new ArrayList<>();
    private SoundEngine engine;

    @BeforeEach
    void setUp() {
        backends.clear();
        engine = new SoundEngine(spec -> {
            SilentAudioBackend backend = new SilentAudioBackend(spec, 60.0f);
            backends.add(backend);
            return backend;
        });
    }

    @Test
    void repeaterIsLeftToTheEngineToChain() {
        SoundCategory category = category("ATMOS", AmbienceBeds.LAYER_ONE,
                event(AmbienceBeds.LAYER_ONE, REPEATING_FLAGS, timed(1000, 1000, 5000)));
        AmbienceLayer layer = new AmbienceLayer(engine, category, AmbienceBeds.LAYER_ONE, null, 0);

        layer.tick(10_000);
        assertEquals(1, layer.getLiveCount(), "the first clip starts at once");

        layer.tick(10_000 + 250 + 1000);
        layer.tick(10_000 + 60_000);
        assertEquals(1, layer.getLiveCount(),
                "the engine plays the next clips of the voice, the layer must not start voices of its own");
    }

    @Test
    void segmentedLayerIsLeftToTheEngineToChain() {
        SoundCategory category = category("BED", AmbienceBeds.LAYER_ONE,
                event(AmbienceBeds.LAYER_ONE, SEGMENTED_FLAGS, loud(100, 100, 4000)));
        AmbienceLayer layer = new AmbienceLayer(engine, category, AmbienceBeds.LAYER_ONE, null, 0);

        layer.tick(1_000);
        layer.tick(1_000 + 4000 - SoundEngine.FADE_IN_MS);
        layer.tick(1_000 + 60_000);

        assertEquals(1, layer.getLiveCount(), "the engine starts the next segment, not the layer");
    }

    @Test
    void propertyLinkedLayerWaitsWhileNoLinkMatches() {
        SoundArchive archive = new SoundArchive("Global" + File.separator + "Test", EnumSet.of(BankFlag.RESIDENT), null,
                List.of("clip1.mp2"));
        SoundLink link = new SoundLink(1, 0, 39);
        SoundEventGroup group = new SoundEventGroup(100, 100, 0, 0, 0, 0, 0, 0, 0, 0, 100, 0.0f,
                List.of(new SoundSample(1, 100, 4000, 0, archive)), List.of(link));
        link.setTarget(group);
        SoundCategory category = category("AMBIENCE", AmbienceBeds.LAYER_ONE, event(AmbienceBeds.LAYER_ONE,
                EnumSet.of(SoundEventFlag.REPEATING, SoundEventFlag.SEGMENTED, SoundEventFlag.PROPERTY_LINKS), group));
        int[] musicIntensity = {40};
        AmbienceLayer layer = new AmbienceLayer(engine, category, AmbienceBeds.LAYER_ONE, null, () -> musicIntensity[0]);

        layer.tick(1_000);
        assertEquals(0, layer.getLiveCount(), "no link matches the louder music, nothing plays");

        musicIntensity[0] = 20;
        layer.tick(1_000 + 1000);
        assertEquals(1, layer.getLiveCount(), "plays again once a link matches");
    }

    @Test
    void layerStopsItsVoices() {
        SoundCategory category = category("BED", AmbienceBeds.LAYER_ONE,
                event(AmbienceBeds.LAYER_ONE, SEGMENTED_FLAGS, loud(100, 100, 4000)));
        AmbienceLayer layer = new AmbienceLayer(engine, category, AmbienceBeds.LAYER_ONE, null, 0);
        layer.tick(1_000);
        assertEquals(1, engine.getActiveVoiceCount());

        layer.stop();

        assertEquals(0, layer.getLiveCount());
        assertTrue(layer.isStopped());
        layer.tick(100_000);
        assertEquals(0, layer.getLiveCount(), "a stopped layer stays stopped");
    }

    @Test
    void layerMovesItsVoices() {
        SoundCategory category = category("BED", AmbienceBeds.LAYER_ONE,
                event(AmbienceBeds.LAYER_ONE, SEGMENTED_FLAGS, loud(100, 100, 4000)));
        AmbienceLayer layer = new AmbienceLayer(engine, category, AmbienceBeds.LAYER_ONE, new Point(1, 1), 0);
        layer.tick(1_000);
        int handle = firstHandle();

        layer.move(new Point(5, 7));

        assertEquals(new Point(5, 7), engine.getVoice(handle).getPosition());
    }

    @Test
    void volumeScaleFollowsTheGroupVolume() {
        SoundCategory category = category("TORCH", AmbienceBeds.LAYER_ONE,
                event(AmbienceBeds.LAYER_ONE, SEGMENTED_FLAGS, loud(0, 60, 4000)));
        AmbienceLayer layer = new AmbienceLayer(engine, category, AmbienceBeds.LAYER_ONE, new Point(0, 0), 0);
        layer.tick(1_000);

        layer.setVolumeScale(0.5f);

        assertEquals(30, engine.getVoice(firstHandle()).getVolume());
    }

    @Test
    void bedsTakeTheTwoHeaviestCategories() {
        Map<String, SoundCategory> categories = new HashMap<>();
        for (String name : List.of("LAVA", "WATER", "LIBRARY")) {
            categories.put(name, category(name, AmbienceBeds.LAYER_ONE,
                    event(AmbienceBeds.LAYER_ONE, SEGMENTED_FLAGS, loud(100, 100, 4000))));
        }
        AmbienceBeds beds = new AmbienceBeds(engine, categories::get);

        String[] cells = new String[AmbienceBeds.GRID_SIZE * AmbienceBeds.GRID_SIZE];
        fill(cells, 4, 4, 6, 6, "LAVA");   // the middle
        fill(cells, 0, 0, 1, 1, "WATER");  // a corner
        fill(cells, 9, 9, 10, 10, "LIBRARY"); // the other corner, lighter than the middle

        beds.update(1_000, cells, new Point(20, 20));

        assertEquals(2, beds.getBedCount());
        assertNotNull(beds.getBedPosition("LAVA"));
        assertNotNull(beds.getBedPosition("WATER"));
        assertNull(beds.getBedPosition("LIBRARY"), "the lightest category gets no bed");
    }

    @Test
    void bedsSitOnTheListenerWithinTheDeadZone() {
        SoundCategory lava = category("LAVA", AmbienceBeds.LAYER_ONE,
                event(AmbienceBeds.LAYER_ONE, SEGMENTED_FLAGS, loud(100, 100, 4000)));
        AmbienceBeds beds = new AmbienceBeds(engine, name -> lava);

        String[] cells = new String[AmbienceBeds.GRID_SIZE * AmbienceBeds.GRID_SIZE];
        fill(cells, 7, 5, 7, 5, "LAVA"); // two tiles from the listener

        beds.update(1_000, cells, new Point(20, 20));

        assertEquals(new Point(20, 20), beds.getBedPosition("LAVA"));
    }

    @Test
    void bedsLeaveTheDeadZoneBehind() {
        SoundCategory lava = category("LAVA", AmbienceBeds.LAYER_ONE,
                event(AmbienceBeds.LAYER_ONE, SEGMENTED_FLAGS, loud(100, 100, 4000)));
        AmbienceBeds beds = new AmbienceBeds(engine, name -> lava);

        String[] cells = new String[AmbienceBeds.GRID_SIZE * AmbienceBeds.GRID_SIZE];
        fill(cells, 10, 5, 10, 5, "LAVA"); // five tiles east

        beds.update(1_000, cells, new Point(20, 20));

        assertEquals(new Point(23, 20), beds.getBedPosition("LAVA"), "five tiles away, minus the dead zone");
    }

    @Test
    void categoriesWithoutABedAreIgnored() {
        SoundCategory rock = category("ROCK", 0x100,
                event(0x100, EnumSet.noneOf(SoundEventFlag.class), loud(100, 100, 1000)));
        AmbienceBeds beds = new AmbienceBeds(engine, name -> rock);

        String[] cells = new String[AmbienceBeds.GRID_SIZE * AmbienceBeds.GRID_SIZE];
        fill(cells, 4, 4, 6, 6, "ROCK");

        beds.update(1_000, cells, new Point(0, 0));

        assertEquals(0, beds.getBedCount());
        assertEquals(0, engine.getActiveVoiceCount());
    }

    @Test
    void bedsGoAwayWhenTheCategoryDoes() {
        SoundCategory lava = category("LAVA", AmbienceBeds.LAYER_ONE,
                event(AmbienceBeds.LAYER_ONE, SEGMENTED_FLAGS, loud(100, 100, 4000)));
        AmbienceBeds beds = new AmbienceBeds(engine, name -> lava);
        String[] cells = new String[AmbienceBeds.GRID_SIZE * AmbienceBeds.GRID_SIZE];
        fill(cells, 4, 4, 6, 6, "LAVA");
        beds.update(1_000, cells, new Point(0, 0));
        assertEquals(1, beds.getBedCount());

        beds.update(2_000, new String[AmbienceBeds.GRID_SIZE * AmbienceBeds.GRID_SIZE], new Point(0, 0));

        assertEquals(0, beds.getBedCount());
    }

    @Test
    void secondLayerStartsWithTheFirst() {
        Map<Integer, SoundEvent> events = new HashMap<>();
        events.put(AmbienceBeds.LAYER_ONE, event(AmbienceBeds.LAYER_ONE, SEGMENTED_FLAGS, loud(100, 100, 4000)));
        events.put(AmbienceBeds.LAYER_TWO, event(AmbienceBeds.LAYER_TWO, SEGMENTED_FLAGS, loud(100, 100, 4000)));
        SoundCategory library = new SoundCategory("LIBRARY", 0b11, events);
        AmbienceBeds beds = new AmbienceBeds(engine, name -> library);
        String[] cells = new String[AmbienceBeds.GRID_SIZE * AmbienceBeds.GRID_SIZE];
        fill(cells, 4, 4, 6, 6, "LIBRARY");

        beds.update(1_000, cells, new Point(0, 0));

        assertEquals(2, engine.getActiveVoiceCount());
    }

    @Test
    void driverStartsAmbienceAndAtmos() {
        Map<String, SoundCategory> categories = new HashMap<>();
        categories.put("AMBIENCE", category("AMBIENCE", AmbienceDriver.AMBIENCE_EVENT,
                event(AmbienceDriver.AMBIENCE_EVENT, SEGMENTED_FLAGS, loud(100, 100, 4000))));
        categories.put("ONE_SHOT_ATMOS", category("ONE_SHOT_ATMOS", AmbienceDriver.ONE_SHOT_ATMOS_EVENT,
                event(AmbienceDriver.ONE_SHOT_ATMOS_EVENT, REPEATING_FLAGS, timed(8000, 8000, 1000))));
        AmbienceDriver driver = new AmbienceDriver(engine, categories::get);

        driver.update(1_000, null, new Point(0, 0), null);

        assertEquals(2, engine.getActiveVoiceCount());
        driver.stop();
        engine.tick();
    }

    @Test
    void torchIsLoudestAtTheTorchAndSilentBeyondTwoTiles() {
        Map<String, SoundCategory> categories = new HashMap<>();
        categories.put("OBJECT_TORCH", category("OBJECT_TORCH", AmbienceDriver.TORCH_EVENT,
                event(AmbienceDriver.TORCH_EVENT, SEGMENTED_FLAGS, loud(0, 60, 4000))));
        AmbienceDriver driver = new AmbienceDriver(engine, categories::get);
        engine.setListener(new Point(10, 10));

        driver.update(1_000, null, new Point(10, 10), new Point(10, 10));
        assertEquals(1, engine.getActiveVoiceCount());
        assertEquals(60, engine.getVoice(firstHandle()).getVolume());

        driver.update(1_100, null, new Point(10, 10), new Point(11, 10));
        assertEquals(45, engine.getVoice(firstHandle()).getVolume(), "100 - 25 d^2 of 60");

        driver.update(1_200, null, new Point(10, 10), new Point(12, 10));
        engine.tick();
        assertEquals(0, countPlaying(), "silent at two tiles");
    }

    private int firstHandle() {
        for (int slot = 0; slot < SoundEngine.MAX_VOICES; slot++) {
            for (int generation = 1; generation < 64; generation++) {
                int handle = (generation << 16) | slot;
                if (engine.getVoice(handle) != null && !engine.getVoice(handle).isStopped()) {
                    return handle;
                }
            }
        }
        throw new AssertionError("no voice");
    }

    private int countPlaying() {
        int count = 0;
        for (int slot = 0; slot < SoundEngine.MAX_VOICES; slot++) {
            for (int generation = 1; generation < 64; generation++) {
                if (engine.isPlaying((generation << 16) | slot)) {
                    count++;
                }
            }
        }
        return count;
    }

    private static void fill(String[] cells, int fromX, int fromY, int toX, int toY, String name) {
        for (int y = fromY; y <= toY; y++) {
            for (int x = fromX; x <= toX; x++) {
                cells[y * AmbienceBeds.GRID_SIZE + x] = name;
            }
        }
    }

    private static SoundCategory category(String name, int eventId, SoundEvent event) {
        Map<Integer, SoundEvent> events = new HashMap<>();
        events.put(eventId, event);
        return new SoundCategory(name, 0b11, events);
    }

    private static SoundEvent event(int id, Set<SoundEventFlag> flags, SoundEventGroup group) {
        return new SoundEvent(id, 100, flags, List.of(group));
    }

    /**
     * A group with a delay between its clips, at full volume.
     */
    private static SoundEventGroup timed(int delayMin, int delayMax, int durationMs) {
        return group(100, 100, delayMin, delayMax, durationMs);
    }

    /**
     * A group without a delay, with a volume that goes from {@code volumeMin} to {@code volumeMax}.
     */
    private static SoundEventGroup loud(int volumeMin, int volumeMax, int durationMs) {
        return group(volumeMin, volumeMax, 0, 0, durationMs);
    }

    private static SoundEventGroup group(int volumeMin, int volumeMax, int delayMin, int delayMax, int durationMs) {
        SoundArchive archive = new SoundArchive("Global" + File.separator + "Test", EnumSet.of(BankFlag.RESIDENT), null,
                List.of("clip1.mp2"));
        SoundSample sample = new SoundSample(1, 100, durationMs, 0, archive);
        return new SoundEventGroup(volumeMin, volumeMax, 0, 0, delayMin, delayMax, 0, 0, 0, 0, 100, 0.0f,
                List.of(sample), List.of());
    }
}
