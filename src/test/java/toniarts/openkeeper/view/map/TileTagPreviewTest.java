package toniarts.openkeeper.view.map;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.utils.Point;
import static org.junit.jupiter.api.Assertions.*;

class TileTagPreviewTest {
    private final TileTagPreview preview = new TileTagPreview();
    private final Point a = new Point(1, 1);
    private final Point b = new Point(2, 1);

    @Test
    void pressingImmediatelyOverridesAnUnselectedTile() {
        assertEquals(Set.of(a), preview.replace(List.of(a), true));
        assertTrue(preview.isTagged(a, false));
        assertFalse(preview.isTagged(b, false));
    }

    @Test
    void extendingAndShrinkingOnlyDirtiesChangedTiles() {
        preview.replace(List.of(a), true);
        assertEquals(Set.of(b), preview.replace(List.of(a, b), true));
        assertEquals(Set.of(), preview.replace(List.of(a, b), true));
        assertEquals(Set.of(a), preview.replace(List.of(b), true));
        assertFalse(preview.isTagged(a, false));
    }

    @Test
    void untaggingRemovesTheTintWhileTheServerStillReportsSelected() {
        preview.replace(List.of(a), false);
        assertFalse(preview.isTagged(a, true));
        assertEquals(Set.of(a), preview.cancel());
        assertTrue(preview.isTagged(a, true));
    }

    @Test
    void cancellingRestoresTheEarlierPendingCommit() {
        preview.replace(List.of(a), true);
        preview.commit();
        preview.replace(List.of(a), false);
        preview.cancel();
        assertTrue(preview.isTagged(a, false));
    }

    @Test
    void releaseKeepsTheTintUntilAcknowledgementWithoutFlicker() {
        preview.replace(List.of(a), true);
        preview.commit();
        assertTrue(preview.isTagged(a, false));
        preview.acknowledge(List.of(a), p -> false, p -> true);
        assertTrue(preview.isTagged(a, false));
        preview.acknowledge(List.of(a), p -> true, p -> true);
        assertTrue(preview.isTagged(a, true));
        assertFalse(preview.isTagged(a, false), "The acknowledged override has been removed");
    }

    @Test
    void olderTagAcknowledgementDoesNotEraseANewerUntag() {
        preview.replace(List.of(a), true);
        preview.commit();
        preview.replace(List.of(a), false);
        preview.commit();
        preview.acknowledge(List.of(a), p -> true, p -> true);
        assertFalse(preview.isTagged(a, true));
        preview.acknowledge(List.of(a), p -> false, p -> true);
        assertTrue(preview.isTagged(a, true), "The untag override is gone after its acknowledgement");
    }

    @Test
    void diggingOrRevealingNonTaggableFloorDiscardsThePrediction() {
        preview.replace(List.of(a), true);
        preview.commit();
        preview.acknowledge(List.of(a), p -> false, p -> false);
        assertFalse(preview.isTagged(a, false));
    }

    @Test
    void activeDragSurvivesAnEarlierServerAcknowledgement() {
        preview.replace(List.of(a), true);
        preview.commit();
        preview.replace(List.of(a), false);
        preview.acknowledge(List.of(a), p -> true, p -> true);
        assertFalse(preview.isTagged(a, true));
        preview.cancel();
        assertTrue(preview.isTagged(a, true));
    }
}
