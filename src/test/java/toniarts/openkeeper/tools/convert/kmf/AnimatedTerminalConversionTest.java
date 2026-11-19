/* Copyright (C) 2026 OpenKeeper, licensed under GPL-3.0-or-later. */
package toniarts.openkeeper.tools.convert.kmf;

import com.jme3.anim.AnimComposer;
import com.jme3.asset.DesktopAssetManager;
import com.jme3.scene.Node;
import com.jme3.scene.VertexBuffer;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.vecmath.Vector3f;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.animation.PoseTrack;
import toniarts.openkeeper.tools.convert.KmfModelLoader;
import static org.junit.jupiter.api.Assertions.*;

class AnimatedTerminalConversionTest {
    @Test void repeatedTerminalKeyKeepsExactEndpointAndInterpolatesWithoutNaN() throws Exception {
        Anim source = new Anim();
        source.setName("terminal-fixture"); source.setPos(0, 0, 0); source.setFrames(3);
        source.setFrameFactorFunction(Anim.FrameFactorFunction.WRAP);
        source.setItab(new int[][]{{0, 3, 6}});
        source.setOffsets(new short[][]{{0, 0, 1}, {0, 0, 1}, {0, 0, 1}});
        var geometries = new ArrayList<AnimGeom>();
        var vertices = new ArrayList<AnimVertex>();
        for (int v = 0; v < 3; v++) {
            for (int k = 0; k < 3; k++) {
                var key = new AnimGeom(); key.setFrameBase((short)(k == 0 ? 0 : 2));
                key.setGeometry(new Vector3f(k == 0 ? 0 : 2, v, v == 1 ? 1 : 0));
                geometries.add(key);
            }
            var vertex = new AnimVertex(); vertex.setItabIndex(v); vertex.setNormal(0, 0, 1);
            vertex.setUv(new Uv(0, 0)); vertices.add(vertex);
        }
        source.setGeometries(geometries);
        var sprite = new AnimSprite(); sprite.setVertices(vertices); sprite.setMaterialIndex(0);
        sprite.setTriangles(new ArrayList<>(List.of(List.of(new Triangle((short)0, (short)1, (short)2)))));
        source.setSprites(List.of(sprite));
        var assets = new DesktopAssetManager(true);
        var material = new com.jme3.material.Material(assets, "Common/MatDefs/Misc/Unshaded.j3md");
        var convert = KmfModelLoader.class.getDeclaredMethod("handleAnim", Anim.class, Map.class);
        convert.setAccessible(true);
        Node model = (Node) convert.invoke(new KmfModelLoader(), source, Map.of(0, List.of(material)));
        PoseTrack track = (PoseTrack)model.getControl(AnimComposer.class).getAnimClips().iterator().next().getTracks()[0];
        track.getDataAtTime(track.getLength() / 2, null);
        FloatBuffer positions = (FloatBuffer)track.getTarget().getMesh().getBuffer(VertexBuffer.Type.Position).getData();
        assertEquals(1, positions.get(0), 1e-6);
        track.getDataAtTime(100, null);
        assertEquals(2, positions.get(0), 1e-6);
        for (int i = 0; i < positions.limit(); i++) assertTrue(Float.isFinite(positions.get(i)));
    }
}
