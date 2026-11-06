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

import com.jme3.asset.AssetManager;
import com.jme3.post.Filter;
import com.jme3.post.FilterPostProcessor;
import com.jme3.post.ssao.SSAOFilter;
import com.jme3.renderer.ViewPort;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import toniarts.openkeeper.game.data.AntiAliasingMode;
import toniarts.openkeeper.game.data.Settings;

/**
 * Assembles the viewport {@link FilterPostProcessor} chain.
 * <p>
 * Fixed order (later stages see earlier stages' colour output):
 * <ol>
 * <li>{@link TranslucentBucketStage} — always (independent of AO)</li>
 * <li>Optional ambient occlusion ({@link SSAOFilter} today; replaceable)</li>
 * <li>Optional {@link SmaaFilter}</li>
 * </ol>
 * SSAO enable/disable only adds or omits the AO filter; it never gates
 * translucent resolve.
 */
public final class PostProcessPipeline {

    /**
     * Named stages in pipeline order. Used for planning / tests without
     * constructing GPU resources.
     */
    public enum Stage {
        TRANSLUCENT_BUCKET,
        AMBIENT_OCCLUSION,
        SMAA
    }

    /**
     * Immutable build inputs. AO parameters are ignored when {@code ambientOcclusion}
     * is false so callers can pass zeros freely.
     */
    public record Config(
            int msaaSamples,
            boolean ambientOcclusion,
            float aoSampleRadius,
            float aoIntensity,
            float aoScale,
            float aoBias,
            boolean smaa) {

        public Config {
            if (msaaSamples < 0) {
                throw new IllegalArgumentException("msaaSamples must be >= 0");
            }
        }

        public static Config fromSettings(Settings settings) {
            Objects.requireNonNull(settings, "settings");
            AntiAliasingMode aa = settings.getAntiAliasingMode();
            return new Config(
                    aa.getSamples(),
                    settings.getBoolean(Settings.Setting.SSAO),
                    settings.getFloat(Settings.Setting.SSAO_SAMPLE_RADIUS),
                    settings.getFloat(Settings.Setting.SSAO_INTENSITY),
                    settings.getFloat(Settings.Setting.SSAO_SCALE),
                    settings.getFloat(Settings.Setting.SSAO_BIAS),
                    aa.isSmaa());
        }
    }

    private PostProcessPipeline() {
    }

    /**
     * Plans the ordered stage list. {@link Stage#TRANSLUCENT_BUCKET} is always
     * first and never depends on {@link Config#ambientOcclusion()}.
     *
     * @param config pipeline toggles
     * @return unmodifiable ordered stages
     */
    public static List<Stage> plan(Config config) {
        Objects.requireNonNull(config, "config");
        List<Stage> stages = new ArrayList<>(3);
        if (TranslucentBucketStage.isRequired()) {
            stages.add(Stage.TRANSLUCENT_BUCKET);
        }
        if (config.ambientOcclusion()) {
            stages.add(Stage.AMBIENT_OCCLUSION);
        }
        if (config.smaa()) {
            stages.add(Stage.SMAA);
        }
        return Collections.unmodifiableList(stages);
    }

    /**
     * Builds a new filter post-processor for the given config. Does not attach
     * it to a viewport.
     *
     * @param assetManager asset manager for filter materials
     * @param config       pipeline toggles
     * @return configured processor
     */
    public static FilterPostProcessor create(AssetManager assetManager, Config config) {
        Objects.requireNonNull(assetManager, "assetManager");
        Objects.requireNonNull(config, "config");

        FilterPostProcessor fpp = new FilterPostProcessor(assetManager);
        if (config.msaaSamples() > 1) {
            fpp.setNumSamples(config.msaaSamples());
        }

        for (Stage stage : plan(config)) {
            fpp.addFilter(createFilter(stage, config));
        }
        return fpp;
    }

    /**
     * Replaces all processors on {@code viewPort} with a freshly built chain.
     *
     * @param viewPort     target viewport
     * @param assetManager asset manager
     * @param config       pipeline toggles
     */
    public static void applyTo(ViewPort viewPort, AssetManager assetManager, Config config) {
        Objects.requireNonNull(viewPort, "viewPort");
        viewPort.clearProcessors();
        viewPort.addProcessor(create(assetManager, config));
    }

    private static Filter createFilter(Stage stage, Config config) {
        return switch (stage) {
            case TRANSLUCENT_BUCKET -> TranslucentBucketStage.createFilter();
            case AMBIENT_OCCLUSION -> new SSAOFilter(
                    config.aoSampleRadius(),
                    config.aoIntensity(),
                    config.aoScale(),
                    config.aoBias());
            case SMAA -> new SmaaFilter();
        };
    }
}
