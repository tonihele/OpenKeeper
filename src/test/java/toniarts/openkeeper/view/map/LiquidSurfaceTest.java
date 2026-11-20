package toniarts.openkeeper.view.map;

import com.jme3.math.Vector3f;
import com.jme3.scene.VertexBuffer;
import java.util.List;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.common.EntityInstance;
import toniarts.openkeeper.tools.convert.map.Terrain;
import toniarts.openkeeper.utils.Point;
import static org.junit.jupiter.api.Assertions.*;

class LiquidSurfaceTest {

    @Test
    void lavaUsesTheNativeThreeByThreePerTileGridAndStableMapCoordinates() {
        var instance = new EntityInstance<>(new Terrain());
        instance.addCoordinate(new Point(1, 2));
        instance.addCoordinate(new Point(2, 2));

        var mesh = Water.createMesh(List.of(instance), true, null);
        var positions = mesh.getFloatBuffer(VertexBuffer.Type.Position);
        var uv = mesh.getFloatBuffer(VertexBuffer.Type.TexCoord);
        var waveUv = mesh.getFloatBuffer(VertexBuffer.Type.TexCoord2);

        assertEquals(18, mesh.getVertexCount());
        assertEquals(16, mesh.getTriangleCount());
        assertEquals(0.5f, positions.get(0));
        assertEquals(1.5f, positions.get(8 * 3));
        assertEquals(1.5f, positions.get(9 * 3));
        assertEquals(1.0f, uv.get(8 * 2));
        assertEquals(0.0f, uv.get(9 * 2));
        assertEquals(2.0f, waveUv.get(8 * 2));
        assertEquals(3.0f, waveUv.get(8 * 2 + 1));
        assertEquals(waveUv.get(8 * 2), waveUv.get(15 * 2));
        assertEquals(waveUv.get(8 * 2 + 1), waveUv.get(15 * 2 + 1));
        assertEquals(LiquidWaves.LEVEL, positions.get(1));
    }

    @Test
    void classicWaterKeepsItsOriginalFourVerticesPerTileAndHasNoWaveCoordinates() {
        var instance = new EntityInstance<>(new Terrain());
        instance.addCoordinate(new Point(1, 2));
        instance.addCoordinate(new Point(2, 2));

        var mesh = Water.createMesh(List.of(instance), false, null);

        assertEquals(8, mesh.getVertexCount());
        assertEquals(4, mesh.getTriangleCount());
        assertNull(mesh.getBuffer(VertexBuffer.Type.TexCoord2));
    }

    @Test
    void enhancedWaterUsesTheWaveGridAndStableTileCornerCoordinates() {
        var instance = new EntityInstance<>(new Terrain());
        instance.addCoordinate(new Point(4, 7));

        var mesh = Water.createMesh(List.of(instance), true, null);
        var positions = mesh.getFloatBuffer(VertexBuffer.Type.Position);
        var waveUv = mesh.getFloatBuffer(VertexBuffer.Type.TexCoord2);

        assertEquals(9, mesh.getVertexCount());
        assertEquals(8, mesh.getTriangleCount());
        assertEquals(5.0f, waveUv.get(8 * 2));
        assertEquals(8.0f, waveUv.get(8 * 2 + 1));
        assertEquals(LiquidWaves.LEVEL, positions.get(1));
    }

    @Test
    void recoveredLavaWaveRepeatsAndNormalRemainsUnitLength() {
        assertEquals(LiquidWaves.height(12.5, 37.5, 321),
                LiquidWaves.height(12.5, 37.5, 321 + LiquidWaves.TICK_PERIOD), 1e-6f);
        assertEquals(LiquidWaves.LEVEL - 0.08f, LiquidWaves.height(0, 0, 32), 1e-6f);
        assertEquals(Math.PI / 4.0, LiquidWaves.phase(1, 0, true), 1e-6);
        assertEquals(Math.PI, LiquidWaves.phase(1, 0), 1e-6);

        Vector3f normal = new Vector3f();
        LiquidWaves.normal(0, 0, 0, normal);
        assertEquals(1.0f, normal.length(), 1e-6f);
        assertEquals(3.0f / (float) Math.sqrt(11), normal.y, 1e-6f);
        LiquidWaves.normal(0, 0, 0, true, normal);
        assertEquals(1.0f, normal.length(), 1e-6f);
    }
}
