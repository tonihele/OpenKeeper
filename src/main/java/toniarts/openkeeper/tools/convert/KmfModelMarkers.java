package toniarts.openkeeper.tools.convert;

import com.jme3.anim.AnimComposer;
import com.jme3.anim.AnimTrack;
import com.jme3.anim.TransformTrack;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import toniarts.openkeeper.tools.convert.kmf.Anim;
import toniarts.openkeeper.tools.convert.kmf.KmfFile;
import toniarts.openkeeper.tools.convert.wad.WadFile;
import toniarts.openkeeper.utils.PathUtils;

/** Preserves the native gem-emission marker, including its animated position. */
public final class KmfModelMarkers {
    public static final int PORTAL_GEM = 10001;
    public static final String MARKER_ID = "KmfMarkerId";
    private static final String VERSION = "KmfMarkerVersion";
    private static final Map<String, KmfFile> legacySources = new HashMap<>();
    private static WadFile meshes;

    private KmfModelMarkers() {
    }

    public static void addStatic(Node model, toniarts.openkeeper.tools.convert.kmf.Mesh mesh) {
        model.setUserData(VERSION, 1);
        for (var control : mesh.getControls()) {
            if (control.getUnknown2() == PORTAL_GEM) {
                addMarker(model, convert(mesh.getGeometries().get(control.getUnknown1())));
            }
        }
    }

    public static void addAnimated(Node model, Anim anim) {
        model.setUserData(VERSION, 1);
        var composer = model.getControl(AnimComposer.class);
        var clip = composer.getAnimClip(KmfModelLoader.DUMMY_ANIM_CLIP_NAME);
        var tracks = new ArrayList<AnimTrack>(Arrays.asList(clip.getTracks()));
        for (var control : anim.getControls()) {
            if (control.getUnknown3() != PORTAL_GEM || control.getUnknown1() != 0) {
                continue;
            }
            float[] times = new float[anim.getFrames()];
            Vector3f[] positions = new Vector3f[times.length];
            for (int frame = 0; frame < times.length; frame++) {
                // CTRL's second field addresses the same ITAB/OFFS stream used
                // for mesh vertices. The final field is the native marker ID.
                positions[frame] = sample(anim, control.getUnknown2(), frame);
                times[frame] = (frame + 1) / 30f;
            }
            Node marker = addMarker(model, positions[0]);
            tracks.add(new TransformTrack(marker, times, positions, null, null));
        }
        clip.setTracks(tracks.toArray(AnimTrack[]::new));
    }

    public static Vector3f sample(Anim anim, int index, int frame) {
        int geom = anim.getItab()[frame >> 7][index] + anim.getOffsets()[index][frame];
        var start = anim.getGeometries().get(geom);
        var end = anim.getGeometries().get(geom + 1);
        int span = end.getFrameBase() - start.getFrameBase();
        float weight = span <= 0 ? 0 : ((frame & 127) - start.getFrameBase()) / (float) span;
        return convert(start.getGeometry()).interpolateLocal(convert(end.getGeometry()), weight);
    }

    private static Vector3f convert(javax.vecmath.Vector3f point) {
        return new Vector3f(point.x, -point.z, point.y);
    }

    private static Node addMarker(Node model, Vector3f position) {
        Node marker = new Node("KMF marker " + PORTAL_GEM);
        marker.setUserData(MARKER_ID, PORTAL_GEM);
        marker.setLocalTranslation(position);
        model.attachChild(marker);
        return marker;
    }

    /** Adds markers to installations converted before marker export existed. */
    public static synchronized void ensureLegacyMarkers(Node model, String resourceName) {
        if (model.getUserData(VERSION) != null) {
            return;
        }
        String root = PathUtils.getDKIIFolder();
        if (root == null) {
            return;
        }
        try {
            if (meshes == null) {
                meshes = new WadFile(Path.of(PathUtils.getRealFileName(root, "Data/Meshes.WAD")));
            }
            KmfFile source = legacySources.get(resourceName);
            if (source == null) {
                String name = meshes.getWadFileEntries().stream()
                        .filter(entry -> entry.equalsIgnoreCase(resourceName + ".kmf"))
                        .findFirst().orElse(null);
                if (name == null) {
                    model.setUserData(VERSION, 1);
                    return;
                }
                source = new KmfFile(meshes.getFileData(name));
                legacySources.put(resourceName, source);
            }
            if (source.getMesh() != null) {
                addStatic(model, source.getMesh());
            } else if (source.getAnim() != null && model.getControl(AnimComposer.class) != null) {
                addAnimated(model, source.getAnim());
            }
        } catch (IOException | RuntimeException exception) {
            model.setUserData(VERSION, 1);
            System.getLogger(KmfModelMarkers.class.getName()).log(System.Logger.Level.WARNING,
                    "Cannot load model markers for " + resourceName, exception);
        }
    }
}
