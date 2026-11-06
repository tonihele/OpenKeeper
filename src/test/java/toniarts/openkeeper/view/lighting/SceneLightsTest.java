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
package toniarts.openkeeper.view.lighting;

import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Vertex light loop bounds: CPU upload must never exceed the LightData
 * capacity, and the header count the Vertex shader loops over must match
 * what was actually written (0, 1, max, overflow).
 */
class SceneLightsTest {

    private static final Pattern DK_MAX_LIGHTS = Pattern.compile(
            "#define\\s+DK_MAX_LIGHTS\\s+(\\d+)");

    @Test
    void zeroLightsWritesEmptyHeader() {
        SceneLights sceneLights = new SceneLights();
        sceneLights.setGlobalAmbient(new ColorRGBA(0.1f, 0.2f, 0.3f, 1f));

        sceneLights.update(List.of(), null);

        assertEquals(0, sceneLights.getUploadedLightCount());
        assertEquals(0, sceneLights.getHeaderLightCount());
    }

    @Test
    void oneLightUploadsExactlyOne() {
        SceneLights sceneLights = new SceneLights();
        List<LightSource> lights = List.of(lightAt(0, 0, 0));

        sceneLights.update(lights, null);

        assertEquals(1, sceneLights.getUploadedLightCount());
        assertEquals(1, sceneLights.getHeaderLightCount());
        assertTrue(SceneLights.isLightIndexInBounds(0));
    }

    @Test
    void maximumLightsFillCapacityExactly() {
        SceneLights sceneLights = new SceneLights();
        List<LightSource> lights = nLights(SceneLights.MAX_LIGHTS);

        sceneLights.update(lights, null);

        assertEquals(SceneLights.MAX_LIGHTS, sceneLights.getUploadedLightCount());
        assertEquals(SceneLights.MAX_LIGHTS, sceneLights.getHeaderLightCount());
        assertTrue(SceneLights.isLightIndexInBounds(SceneLights.MAX_LIGHTS - 1));
        assertFalse(SceneLights.isLightIndexInBounds(SceneLights.MAX_LIGHTS));
    }

    @Test
    void overflowingLightsAreCappedAtMax() {
        SceneLights sceneLights = new SceneLights();
        int overflow = SceneLights.MAX_LIGHTS + 17;
        List<LightSource> lights = nLights(overflow);

        sceneLights.update(lights, null);

        assertEquals(SceneLights.MAX_LIGHTS, sceneLights.getUploadedLightCount());
        assertEquals(SceneLights.MAX_LIGHTS, sceneLights.getHeaderLightCount());
        // Last written light's third texel must still address within the row.
        int lastLightBase = 1 + SceneLights.TEXELS_PER_LIGHT * (SceneLights.MAX_LIGHTS - 1);
        assertTrue(lastLightBase + 2 < SceneLights.lightDataTexelWidth());
    }

    @Test
    void disabledLightsDoNotCountTowardUpload() {
        SceneLights sceneLights = new SceneLights();
        LightSource on = lightAt(1, 0, 0);
        LightSource off = lightAt(2, 0, 0);
        off.setEnabled(false);

        sceneLights.update(List.of(on, off), null);

        assertEquals(1, sceneLights.getUploadedLightCount());
        assertEquals(1, sceneLights.getHeaderLightCount());
    }

    @Test
    void lightDataTextureWidthMatchesLayout() {
        assertEquals(1 + SceneLights.TEXELS_PER_LIGHT * SceneLights.MAX_LIGHTS,
                SceneLights.lightDataTexelWidth());
        // Header + full capacity: last fetch index used by the shader is
        // base+2 for light (MAX_LIGHTS-1) = 1 + 3*(MAX-1) + 2 = 3*MAX.
        int lastFetchX = 1 + 3 * (SceneLights.MAX_LIGHTS - 1) + 2;
        assertEquals(SceneLights.lightDataTexelWidth() - 1, lastFetchX);
    }

    @Test
    void shaderDkMaxLightsMatchesSceneLightsCapacity() throws IOException {
        Path shader = resolveVertexShader();
        String source = Files.readString(shader);
        Matcher matcher = DK_MAX_LIGHTS.matcher(source);
        assertTrue(matcher.find(), "DK_MAX_LIGHTS define missing in " + shader);
        assertEquals(SceneLights.MAX_LIGHTS, Integer.parseInt(matcher.group(1)),
                "Vertex.vert DK_MAX_LIGHTS must stay in sync with SceneLights.MAX_LIGHTS");
        assertTrue(source.contains("for (int i = 0; i < numLights; i++)")
                        || source.contains("for (int i = 0; i < numLights; ++i)"),
                "Vertex light loop must bound on clamped numLights, not DK_MAX_LIGHTS");
        assertTrue(source.contains("clamp(int(header.x + 0.5), 0, DK_MAX_LIGHTS)"),
                "numLights must be clamped to DK_MAX_LIGHTS before the loop");
    }

    private static LightSource lightAt(float x, float y, float z) {
        return new LightSource(LightSource.Kind.THING, null, new Vector3f(x, y, z),
                255, 200, 100, 1.0f, 4f);
    }

    private static List<LightSource> nLights(int n) {
        List<LightSource> lights = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            lights.add(lightAt(i * 0.01f, 0f, 0f));
        }
        return lights;
    }

    private static Path resolveVertexShader() {
        Path cwd = Path.of("").toAbsolutePath();
        Path direct = cwd.resolve("assets/Shaders/DkLighting/Vertex.vert");
        if (Files.isRegularFile(direct)) {
            return direct;
        }
        Path nested = cwd.resolve("openkeeper/assets/Shaders/DkLighting/Vertex.vert");
        if (Files.isRegularFile(nested)) {
            return nested;
        }
        throw new IllegalStateException("Vertex.vert not found from " + cwd);
    }
}
