package toniarts.openkeeper.game.controller;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.utils.Point;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ConnectedPlotsTest {

    @Test
    void connectedPlotsFormOnePart() {
        List<Point> plots = List.of(new Point(0, 0), new Point(1, 0), new Point(1, 1));

        List<List<Point>> parts = GameWorldController.getConnectedPlots(plots);

        assertEquals(1, parts.size());
        assertEquals(Set.copyOf(plots), Set.copyOf(parts.getFirst()));
    }

    @Test
    void skippedColumnSplitsTheSelection() {

        // A 3x3 selection where the middle column could not be built
        List<Point> plots = List.of(
                new Point(0, 0), new Point(0, 1), new Point(0, 2),
                new Point(2, 0), new Point(2, 1), new Point(2, 2));

        List<List<Point>> parts = GameWorldController.getConnectedPlots(plots);

        assertEquals(2, parts.size());
        assertEquals(Set.of(
                Set.of(new Point(0, 0), new Point(0, 1), new Point(0, 2)),
                Set.of(new Point(2, 0), new Point(2, 1), new Point(2, 2))),
                parts.stream().map(Set::copyOf).collect(Collectors.toSet()));
    }

    @Test
    void diagonalPlotsAreNotConnected() {
        List<List<Point>> parts = GameWorldController.getConnectedPlots(List.of(new Point(0, 0), new Point(1, 1)));

        assertEquals(2, parts.size());
    }

}
