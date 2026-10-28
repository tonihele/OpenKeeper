package toniarts.openkeeper.game.sound;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SoundRngTest {

    private Random previousRandom;

    @BeforeEach
    void setUp() {
        previousRandom = SoundRng.getRandom();
        SoundRng.setRandom(new Random(1234));
    }

    @AfterEach
    void tearDown() {
        SoundRng.setRandom(previousRandom);
    }

    @Test
    void totalWeightIsTheCumulativeWeightOfTheLastEntry() {
        // the weights are running totals: 10, 30, 60
        List<Item> items = List.of(new Item(10), new Item(30), new Item(60));

        assertEquals(60, SoundRng.totalWeightOf(items, Item::weight));
        assertEquals(0, SoundRng.totalWeightOf(List.of(), Item::weight));
    }

    @Test
    void pickRespectsTheRunningTotals() {
        // running totals 10, 30, 60: the entries cover 10, 20 and 30 of 60 rolls
        List<Item> items = List.of(new Item(10), new Item(30), new Item(60));

        int[] counts = new int[3];
        for (int i = 0; i < 60000; i++) {
            Item picked = SoundRng.pick(items, Item::weight);
            counts[items.indexOf(picked)]++;
        }

        assertBetween(counts[0], 10000, 10000, "the first entry covers 10 of 60");
        assertBetween(counts[1], 20000, 20000, "the second entry covers 20 of 60");
        assertBetween(counts[2], 30000, 30000, "the third entry covers 30 of 60");
    }

    private static void assertBetween(int actual, int expected, int tolerance, String message) {
        assertTrue(Math.abs(actual - expected) <= tolerance,
                message + ": expected about " + expected + " but was " + actual);
    }

    @Test
    void pickFallsBackToTheFirstItemWithoutWeight() {
        List<Item> items = List.of(new Item(0), new Item(0));

        assertEquals(items.get(0), SoundRng.pick(items, Item::weight));
        assertNull(SoundRng.pick(new ArrayList<Item>(), Item::weight));
    }

    @Test
    void volumeStaysInTheGroupRange() {
        for (int i = 0; i < 1000; i++) {
            int volume = SoundRng.randomVolume(80, 120);
            assertTrue(volume >= 80 && volume <= 120, "volume out of range: " + volume);
        }
        assertEquals(100, SoundRng.randomVolume(100, 100));
        assertEquals(0, SoundRng.clampVolume(-5));
        assertEquals(127, SoundRng.clampVolume(500));
    }

    @Test
    void pitchStaysInTheGroupRange() {
        SoundEventGroup group = group(-10, 10, 0, 0);

        for (int i = 0; i < 1000; i++) {
            float pitch = SoundRng.pitch(group);
            assertTrue(pitch >= 90.0f && pitch <= 110.0f, "pitch out of range: " + pitch);
        }
    }

    @Test
    void delayStaysInTheGroupRange() {
        SoundEventGroup group = group(0, 0, 100, 200);

        for (int i = 0; i < 1000; i++) {
            int delay = SoundRng.delay(group);
            assertTrue(delay >= 100 && delay <= 200, "delay out of range: " + delay);
        }

        assertEquals(0, SoundRng.delay(group(0, 0, 0, 0)));
    }

    private static SoundEventGroup group(int pitchMin, int pitchMax, int delayMin, int delayMax) {
        return new SoundEventGroup(100, 100, pitchMin, pitchMax, delayMin, delayMax, 0, 0, 0, 0, 1, 0.0f,
                List.of(), List.of());
    }

    private record Item(int weight) {

    }
}