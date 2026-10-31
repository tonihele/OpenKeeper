package toniarts.openkeeper.tools.convert.kmf;

import com.jme3.anim.AnimClip;
import com.jme3.anim.AnimComposer;
import com.jme3.anim.AnimTrack;
import com.jme3.anim.TransformTrack;
import com.jme3.export.binary.BinaryExporter;
import com.jme3.export.binary.BinaryImporter;
import com.jme3.math.Transform;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.List;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.tools.convert.KmfModelLoader;
import toniarts.openkeeper.tools.convert.KmfModelMarkers;
import static org.junit.jupiter.api.Assertions.*;

class KmfModelMarkersTest {
    @Test
    void preservesStaticMarkerAndCoordinateConversion() {
        Mesh mesh = new Mesh();
        mesh.setGeometries(List.of(new javax.vecmath.Vector3f(1, 2, -3)));
        MeshControl control = new MeshControl();
        control.setUnknown1(0);
        control.setUnknown2(10001);
        mesh.setControls(List.of(control));
        Node model = new Node();
        KmfModelMarkers.addStatic(model, mesh);
        var marker = model.getChild("KMF marker 10001");
        assertEquals(new Vector3f(1, 3, 2), marker.getLocalTranslation());
        assertEquals(Integer.valueOf(10001), marker.getUserData(KmfModelMarkers.MARKER_ID));
    }

    @Test
    void interpolatesAnimatedMarkerAndSerializesItsTrack() throws Exception {
        Anim anim = new Anim();
        anim.setFrames(2);
        anim.setItab(new int[][]{{0}});
        anim.setOffsets(new short[][]{{0, 0}});
        AnimGeom first = geom(0, 1, 2, -3);
        AnimGeom last = geom(2, 3, 4, -5);
        anim.setGeometries(List.of(first, last));
        AnimControl control = new AnimControl();
        control.setUnknown1(0);
        control.setUnknown2(0);
        control.setUnknown3(10001);
        anim.setControls(List.of(control));
        Node model = new Node();
        var composer = new AnimComposer();
        var clip = new AnimClip(KmfModelLoader.DUMMY_ANIM_CLIP_NAME);
        clip.setTracks(new AnimTrack[0]);
        composer.addAnimClip(clip);
        model.addControl(composer);
        KmfModelMarkers.addAnimated(model, anim);
        assertEquals(new Vector3f(2, 4, 3), KmfModelMarkers.sample(anim, 0, 1));
        var bytes = new ByteArrayOutputStream();
        BinaryExporter.getInstance().save(model, bytes);
        Node loaded = (Node) BinaryImporter.getInstance().load(new ByteArrayInputStream(bytes.toByteArray()));
        var track = (TransformTrack) loaded.getControl(AnimComposer.class)
                .getAnimClip(KmfModelLoader.DUMMY_ANIM_CLIP_NAME).getTracks()[0];
        assertSame(loaded.getChild("KMF marker 10001"), track.getTarget());
        var transform = new Transform();
        track.getDataAtTime(2 / 30f, transform);
        assertEquals(new Vector3f(2, 4, 3), transform.getTranslation());
        Node cloned = (Node) loaded.clone();
        var clonedTrack = (TransformTrack) cloned.getControl(AnimComposer.class)
                .getAnimClip(KmfModelLoader.DUMMY_ANIM_CLIP_NAME).getTracks()[0];
        assertSame(cloned.getChild("KMF marker 10001"), clonedTrack.getTarget());
    }

    private static AnimGeom geom(int frame, float x, float y, float z) {
        AnimGeom geom = new AnimGeom();
        geom.setFrameBase((short) frame);
        geom.setGeometry(new javax.vecmath.Vector3f(x, y, z));
        return geom;
    }
}
