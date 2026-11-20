package toniarts.openkeeper.view.map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class Dk2EmbmResourcesTest {

    @Test
    void waterUsesAll32OriginalBumpFramesInNumericOrder() {
        List<String> frameNames = Dk2EmbmResources.waterBumpFrameNames();

        assertEquals(32, frameNames.size());
        assertEquals("Textures/BumpWaterBumpmap0.png", frameNames.getFirst());
        assertEquals("Textures/BumpWaterBumpmap31.png", frameNames.getLast());
        assertTrue(frameNames.get(2).endsWith("BumpWaterBumpmap2.png"));
    }
}
