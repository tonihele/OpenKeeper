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
package toniarts.openkeeper.post;

import java.util.List;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.post.PostProcessPipeline.Config;
import toniarts.openkeeper.post.PostProcessPipeline.Stage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Ensures translucent resolve is never gated on ambient occlusion.
 */
class PostProcessPipelineTest {

    @Test
    void translucentStageIsAlwaysRequired() {
        assertTrue(TranslucentBucketStage.isRequired());
    }

    @Test
    void translucentIsPresentWhenAmbientOcclusionDisabled() {
        Config config = new Config(0, false, 0f, 0f, 0f, 0f, false);
        List<Stage> stages = PostProcessPipeline.plan(config);

        assertEquals(List.of(Stage.TRANSLUCENT_BUCKET), stages);
        assertFalse(stages.contains(Stage.AMBIENT_OCCLUSION));
    }

    @Test
    void translucentPrecedsAmbientOcclusionWhenBothEnabled() {
        Config config = new Config(0, true, 5.94f, 3.92f, 0.33f, 0.1f, false);
        List<Stage> stages = PostProcessPipeline.plan(config);

        assertEquals(List.of(Stage.TRANSLUCENT_BUCKET, Stage.AMBIENT_OCCLUSION), stages);
        assertTrue(stages.indexOf(Stage.TRANSLUCENT_BUCKET) < stages.indexOf(Stage.AMBIENT_OCCLUSION));
    }

    @Test
    void fullChainKeepsTranslucentFirstIndependentOfAoAndSmaa() {
        Config config = new Config(4, true, 5.94f, 3.92f, 0.33f, 0.1f, true);
        List<Stage> stages = PostProcessPipeline.plan(config);

        assertEquals(
                List.of(Stage.TRANSLUCENT_BUCKET, Stage.AMBIENT_OCCLUSION, Stage.SMAA),
                stages);
    }

    @Test
    void disablingAoDoesNotDropTranslucentWhenSmaaRemains() {
        Config withAo = new Config(0, true, 1f, 1f, 1f, 1f, true);
        Config withoutAo = new Config(0, false, 1f, 1f, 1f, 1f, true);

        assertTrue(PostProcessPipeline.plan(withAo).contains(Stage.TRANSLUCENT_BUCKET));
        assertTrue(PostProcessPipeline.plan(withoutAo).contains(Stage.TRANSLUCENT_BUCKET));
        assertEquals(
                List.of(Stage.TRANSLUCENT_BUCKET, Stage.SMAA),
                PostProcessPipeline.plan(withoutAo));
    }
}
