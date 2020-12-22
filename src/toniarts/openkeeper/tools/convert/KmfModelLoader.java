/*
 * Copyright (C) 2014-2015 OpenKeeper
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
package toniarts.openkeeper.tools.convert;

import com.jme3.animation.AnimControl;
import com.jme3.asset.AssetInfo;
import com.jme3.asset.AssetLoader;
import com.jme3.asset.MaterialKey;
import com.jme3.asset.ModelKey;
import com.jme3.asset.TextureKey;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.material.plugin.export.material.J3MExporter;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.AssetLinkNode;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Node;
import com.jme3.scene.VertexBuffer;
import com.jme3.scene.VertexBuffer.Type;
import com.jme3.scene.control.LodControl;
import com.jme3.texture.Texture;
import com.jme3.util.BufferUtils;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;
import toniarts.openkeeper.animation.Pose;
import toniarts.openkeeper.animation.PoseTrack;
import toniarts.openkeeper.animation.PoseTrack.PoseFrame;
import toniarts.openkeeper.tools.convert.kmf.Anim;
import toniarts.openkeeper.tools.convert.kmf.AnimSprite;
import toniarts.openkeeper.tools.convert.kmf.AnimVertex;
import toniarts.openkeeper.tools.convert.kmf.Grop;
import toniarts.openkeeper.tools.convert.kmf.KmfFile;
import toniarts.openkeeper.tools.convert.kmf.MeshSprite;
import toniarts.openkeeper.tools.convert.kmf.MeshVertex;
import toniarts.openkeeper.tools.convert.kmf.Triangle;
import toniarts.openkeeper.tools.convert.kmf.Uv;
import toniarts.openkeeper.tools.modelviewer.ModelViewer;
import toniarts.openkeeper.utils.PathUtils;
import toniarts.openkeeper.utils.TangentBinormalGenerator;

/**
 * Loads up and converts a Dungeon Keeper II model to JME model<br>
 * The coordinate system is a bit different, so switching Z & Y is intentional,
 * JME uses right-handed coordinate system
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class KmfModelLoader implements AssetLoader {

    /* Some textures are broken */
    private final static Map<String, String> textureFixes;
    private static String dkIIFolder;

    static {
        textureFixes = new HashMap<>(2);
        textureFixes.put("Goblinbak", "GoblinBack");
        textureFixes.put("Goblin2", "GoblinFront");
    }
    /**
     * If the material has multiple texture options, the material is named
     * &lt;material&gt;&lt;this suffix&gt;&lt;texture index&gt;. Texture index
     * is 0-based
     */
    public static final String MATERIAL_ALTERNATIVE_TEXTURE_SUFFIX_SEPARATOR = "_";
    /**
     * If this user meta data is found from the geometry, it has alternative
     * material possibilities
     */
    public static final String MATERIAL_ALTERNATIVE_TEXTURES_COUNT = "AlternativeTextureCount";
    public static final String FRAME_FACTOR_FUNCTION = "FrameFactorFunction";
    private static final Logger logger = Logger.getLogger(KmfModelLoader.class.getName());
    /* Already saved materials are stored here */
    private static final Map<toniarts.openkeeper.tools.convert.kmf.Material, String> materialCache = new HashMap<>();

    public static void main(final String[] args) throws IOException {

        //Take Dungeon Keeper 2 root folder as parameter
        if (args.length != 2 || !new File(args[1]).exists()) {
            dkIIFolder = PathUtils.getDKIIFolder();
            if (dkIIFolder == null) {
                throw new RuntimeException("Please provide file path to the model as a first parameter! Second parameter is the Dungeon Keeper II main folder (optional)");
            }
        } else {
            dkIIFolder = PathUtils.fixFilePath(args[1]);
        }

        ModelViewer app = new ModelViewer(Paths.get(args[0]), dkIIFolder);
        app.start();
    }

    @Override
    public Object load(AssetInfo assetInfo) throws IOException {

        KmfFile kmfFile;
        boolean generateMaterialFile = false;
        if (assetInfo instanceof KmfAssetInfo) {
            kmfFile = ((KmfAssetInfo) assetInfo).getKmfFile();
            generateMaterialFile = ((KmfAssetInfo) assetInfo).isGenerateMaterialFile();
        } else {
            kmfFile = new KmfFile(readAssetStream(assetInfo));
        }

        // Create a root
        Node root = new Node("Root");

        if (kmfFile.getType() == KmfFile.Type.MESH || kmfFile.getType() == KmfFile.Type.ANIM) {

            // Get the materials first
            Map<Integer, List<Material>> materials = getMaterials(kmfFile, generateMaterialFile, assetInfo);

            //
            // The meshes
            //
            for (toniarts.openkeeper.tools.convert.kmf.Mesh sourceMesh : kmfFile.getMeshes()) {
                handleMesh(sourceMesh, materials, root);
            }
            if (kmfFile.getType() == KmfFile.Type.ANIM) {
                handleAnim(kmfFile.getAnim(), materials, root);
            }
        } else if (kmfFile.getType() == KmfFile.Type.GROP) {
            createGroup(root, kmfFile);
        }

        return root;
    }

    /**
     * Creates a grop, a.k.a. a group, links existing models to one scene?
     *
     * @param root root node
     * @param kmfFile the KMF file
     */
    private void createGroup(Node root, KmfFile kmfFile) {

        //Go trough the models and add them
        for (Grop grop : kmfFile.getGrops()) {
            String key = AssetsConverter.MODELS_FOLDER + File.separator + grop.getName() + ".j3o";
            AssetLinkNode modelLink = new AssetLinkNode(key, new ModelKey(key));
            modelLink.setLocalTranslation(new Vector3f(grop.getPos().x, -grop.getPos().z, grop.getPos().y));
            root.attachChild(modelLink);
        }
    }

    private byte[] readAssetStream(AssetInfo assetInfo) throws IOException {
        try (InputStream is = assetInfo.openStream();
                BufferedInputStream bis = new BufferedInputStream(is);
                ByteArrayOutputStream output = new ByteArrayOutputStream()) {

            // TODO: Java 9 has readAll, if it is buffered use it
            int read;
            final int bufLen = 8192;
            byte[] buf = new byte[bufLen];
            while ((read = bis.read(buf, 0, bufLen)) != -1) {
                output.write(buf, 0, read);
            }

            return output.toByteArray();
        }
    }

    /**
     * Handle mesh creation
     *
     * @param sourceMesh the mesh
     * @param materials materials map
     * @param root the root node
     */
    private void handleMesh(toniarts.openkeeper.tools.convert.kmf.Mesh sourceMesh, Map<Integer, List<Material>> materials, Node root) {

        //Source mesh is node
        Node node = new Node(sourceMesh.getName());
        node.setLocalTranslation(new Vector3f(sourceMesh.getPos().x, -sourceMesh.getPos().z, sourceMesh.getPos().y));

        int index = 0;
        for (MeshSprite meshSprite : sourceMesh.getSprites()) {

            //Each sprite represents a geometry (+ mesh) since they each have their own material
            Mesh mesh = new Mesh();

            //Vertices, UV (texture coordinates), normals
            Vector3f[] vertices = new Vector3f[meshSprite.getVertices().size()];
            Vector2f[] texCoord = new Vector2f[meshSprite.getVertices().size()];
            Vector3f[] normals = new Vector3f[meshSprite.getVertices().size()];
            int i = 0;
            for (MeshVertex meshVertex : meshSprite.getVertices()) {

                //Vertice
                javax.vecmath.Vector3f v = sourceMesh.getGeometries().get(meshVertex.getGeomIndex());
                vertices[i] = new Vector3f(v.x, -v.z, v.y);

                //Texture coordinate
                Uv uv = meshVertex.getUv();
                texCoord[i] = new Vector2f(uv.getUv()[0] / 32768f, uv.getUv()[1] / 32768f);

                //Normals
                v = meshVertex.getNormal();
                normals[i] = new Vector3f(v.x, -v.z, v.y);

                i++;
            }

            // Triangles, we have LODs here
            VertexBuffer[] lodLevels = createIndices(meshSprite.getTriangles());

            //Set the buffers
            mesh.setBuffer(Type.Position, 3, BufferUtils.createFloatBuffer(vertices));
            mesh.setBuffer(lodLevels[0]);
            mesh.setLodLevels(lodLevels);
            mesh.setBuffer(Type.TexCoord, 2, BufferUtils.createFloatBuffer(texCoord));
            mesh.setBuffer(Type.Normal, 3, BufferUtils.createFloatBuffer(normals));
            mesh.setStatic();

            // Create geometry
            Geometry geom = createGeometry(index, sourceMesh.getName(), mesh, materials, meshSprite.getMaterialIndex());

            //Attach the geometry to the node
            node.attachChild(geom);
            index++;
        }

        //Attach the node to the root
        root.attachChild(node);
    }

    /**
     * Handle mesh creation
     *
     * @param anim the anim
     * @param materials materials map
     * @param root the root node
     */
    private void handleAnim(Anim anim, Map<Integer, List<Material>> materials, Node root) {

        //Source mesh is node
        Node node = new Node(anim.getName());
        node.setUserData(FRAME_FACTOR_FUNCTION, anim.getFrameFactorFunction().name());
        node.setLocalTranslation(new Vector3f(anim.getPos().x, -anim.getPos().z, anim.getPos().y));

        // Create pose tracks for each mesh index
        List<PoseTrack> poseTracks = new ArrayList<>(anim.getSprites().size());

        // Create times (same for each pose track)
        float[] times = new float[anim.getFrames()];
        for (int i = 0; i < anim.getFrames(); i++) {
            times[i] = (i + 1) / 30f;
        }

        int index = 0;
        for (AnimSprite animSprite : anim.getSprites()) {

            // Animation
            // Poses for each key frame (aproximate that every 1/3 is a key frame, pessimistic)
            // Note that a key frame may not have all the vertices
            Map<Integer, Map<KmfModelLoader.FrameInfo, Pose>> poses = new HashMap<>(anim.getFrames() / 3);

            // Pose indices and indice offsets for each pose
            Map<Integer, Map<KmfModelLoader.FrameInfo, List<Integer>>> frameIndices = new HashMap<>(anim.getFrames() / 3);
            Map<Integer, Map<KmfModelLoader.FrameInfo, List<Vector3f>>> frameOffsets = new HashMap<>(anim.getFrames() / 3);

            // For each frame, we need the previous key frame (pose) and the next, and the weights, for the pose frames
            Map<Integer, List<KmfModelLoader.FrameInfo>> frameInfos = new HashMap<>(anim.getFrames());

            //
            //Each sprite represents a geometry (+ mesh) since they each have their own material
            Mesh mesh = new Mesh();

            //Vertices, UV (texture coordinates), normals
            Vector3f[] vertices = new Vector3f[animSprite.getVertices().size()];
            Vector2f[] texCoord = new Vector2f[animSprite.getVertices().size()];
            Vector3f[] normals = new Vector3f[animSprite.getVertices().size()];
            int i = 0;
            for (AnimVertex animVertex : animSprite.getVertices()) {

                // Bind Pose
                javax.vecmath.Vector3f baseCoord = null;

                // Keep the last frame for creating the target pose
                KmfModelLoader.FrameInfo previousFrame = null;

                //Go through every frame
                for (int frame = 0; frame < anim.getFrames(); frame++) {

                    //Vertice
                    int geomBase = anim.getItab()[frame >> 7][animVertex.getItabIndex()];
                    short geomOffset = anim.getOffsets()[animVertex.getItabIndex()][frame];
                    int geomIndex = geomBase + geomOffset;

                    short frameBase = anim.getGeometries().get(geomIndex).getFrameBase();
                    short nextFrameBase = anim.getGeometries().get(geomIndex + 1).getFrameBase();
                    float geomFactor = (float) ((frame & 0x7f) - frameBase) / (float) (nextFrameBase - frameBase);
                    javax.vecmath.Vector3f coord = anim.getGeometries().get(geomIndex).getGeometry();

                    // Store the frame zero coord
                    if (frame == 0) {
                        baseCoord = coord;
                    }

                    // Create frame info
                    int lastPose = ((frame >> 7) * 128 + frameBase);
                    int nextPose = ((frame >> 7) * 128 + nextFrameBase);
                    if (!frameInfos.containsKey(frame)) {
                        frameInfos.put(frame, new ArrayList<>());
                    }
                    KmfModelLoader.FrameInfo frameInfo = new KmfModelLoader.FrameInfo(lastPose, nextPose, geomFactor);
                    int x = Collections.binarySearch(frameInfos.get(frame), frameInfo);
                    if (x < 0) {
                        frameInfos.get(frame).add(~x, frameInfo);
                    }

                    // Only make poses from key frames
                    if (frame == lastPose || frame == nextPose) {
                        if (!frameIndices.containsKey(frame)) {
                            frameIndices.put(frame, new HashMap<>());
                        }
                        if (!frameOffsets.containsKey(frame)) {
                            frameOffsets.put(frame, new HashMap<>());
                        }

                        if (frame == nextPose) { // Last frame

                            // Create a new frameInfo with weight as 1
                            frameInfo = new KmfModelLoader.FrameInfo(frameInfo.previousPoseFrame, frameInfo.nextPoseFrame, 1f);
                        }

                        if (frameIndices.get(frame).get(frameInfo) == null) {
                            frameIndices.get(frame).put(frameInfo, new ArrayList<>());
                        }
                        if (frameOffsets.get(frame).get(frameInfo) == null) {
                            frameOffsets.get(frame).put(frameInfo, new ArrayList<>());
                        }
                        frameIndices.get(frame).get(frameInfo).add(i);
                        frameOffsets.get(frame).get(frameInfo).add(new Vector3f(coord.x, -coord.z, coord.y));
                    }

                    // Add the pose target, we are in the last frame of the frame target
                    if (previousFrame != null && !frameInfo.equals(previousFrame)) {
                        if (!frameIndices.containsKey(frame)) {
                            frameIndices.put(frame, new HashMap<>());
                        }
                        if (!frameOffsets.containsKey(frame)) {
                            frameOffsets.put(frame, new HashMap<>());
                        }

                        // Create a new frameInfo with weight as 1
                        KmfModelLoader.FrameInfo fi = new KmfModelLoader.FrameInfo(previousFrame.previousPoseFrame, previousFrame.nextPoseFrame, 1f);

                        if (frameIndices.get(frame).get(fi) == null) {
                            frameIndices.get(frame).put(fi, new ArrayList<>());
                        }
                        if (frameOffsets.get(frame).get(fi) == null) {
                            frameOffsets.get(frame).put(fi, new ArrayList<>());
                        }
                        frameIndices.get(frame).get(fi).add(i);
                        frameOffsets.get(frame).get(fi).add(new Vector3f(coord.x, -coord.z, coord.y));

                        // Also add to the frame infos (otherwise it will never be applied fully with weight 1.0)
                        x = Collections.binarySearch(frameInfos.get(frame), fi);
                        if (x < 0) {
                            frameInfos.get(frame).add(~x, fi);
                        }
                    }

                    // Set the last frame
                    previousFrame = frameInfo;
                }
                vertices[i] = new Vector3f(baseCoord.x, -baseCoord.z, baseCoord.y);

                //Texture coordinate
                Uv uv = animVertex.getUv();
                texCoord[i] = new Vector2f(uv.getUv()[0] / 32768f, uv.getUv()[1] / 32768f);

                //Normals
                javax.vecmath.Vector3f v = animVertex.getNormal();
                normals[i] = new Vector3f(v.x, -v.z, v.y);

                i++;
            }

            // We have all the animation vertices from a single pose
            for (int frame = 0; frame < anim.getFrames(); frame++) {
                if (frameIndices.containsKey(frame) && !poses.containsKey(frame)) {
                    poses.put(frame, new HashMap<>());
                }
                if (frameIndices.containsKey(frame)) {
                    for (Entry<KmfModelLoader.FrameInfo, List<Integer>> entry : frameIndices.get(frame).entrySet()) {

                        if (entry.getKey().nextPoseFrame < entry.getKey().previousPoseFrame) {
                            continue; // Huh?, last frame effect
                        }

                        List<Integer> list = entry.getValue();
                        int[] array = new int[list.size()];
                        for (int integer = 0; integer < list.size(); integer++) {
                            array[integer] = list.get(integer);
                        }
                        Pose p = new Pose(index + ": " + frame + ", " + entry.getKey().previousPoseFrame + " - " + entry.getKey().nextPoseFrame, frameOffsets.get(frame).get(entry.getKey()).toArray(new Vector3f[frameOffsets.get(frame).get(entry.getKey()).size()]), array);
                        poses.get(frame).put(entry.getKey(), p);
                    }
                }
            }

            // More animation, create the pose frames by the frame, mesh index specific
            List<PoseFrame> frameList = new ArrayList<>(anim.getFrames());
            for (int frame = 0; frame < anim.getFrames(); frame++) {

                // Loop through all the frame infos here
                Pose[] p = new Pose[frameInfos.get(frame).size() * 2];
                float[] weights = new float[frameInfos.get(frame).size()];
                int x = 0;
                for (KmfModelLoader.FrameInfo frameInfo : frameInfos.get(frame)) {

                    if (frameInfo.nextPoseFrame < frameInfo.previousPoseFrame) {
                        continue; // Huh?, last frame effect
                    }

                    // The poses, always the start and the end
                    p[x * 2] = poses.get(frameInfo.previousPoseFrame).get(frameInfo);
                    p[x * 2 + 1] = poses.get(frameInfo.nextPoseFrame).get(frameInfo);

                    // Weights
                    weights[x] = frameInfo.weight;
                    x++;
                }
                PoseFrame f = new PoseFrame(p, weights);
                frameList.add(f);
            }

            // Create a pose track for this mesh
            PoseTrack poseTrack = new PoseTrack(index, times, frameList.toArray(new PoseFrame[frameList.size()]));
            poseTracks.add(poseTrack);

            // Create lod levels
            VertexBuffer[] lodLevels = createIndices(animSprite.getTriangles());

            //Set the buffers
            mesh.setBuffer(Type.Position, 3, BufferUtils.createFloatBuffer(vertices));
            mesh.setBuffer(Type.BindPosePosition, 3, BufferUtils.createFloatBuffer(vertices));
            mesh.setBuffer(lodLevels[0]);
            mesh.setLodLevels(lodLevels);
            mesh.setBuffer(Type.TexCoord, 2, BufferUtils.createFloatBuffer(texCoord));
            mesh.setBuffer(Type.Normal, 3, BufferUtils.createFloatBuffer(normals));
            mesh.setBuffer(Type.BindPoseNormal, 3, BufferUtils.createFloatBuffer(normals));
            mesh.setStreamed();

            // Create geometry
            Geometry geom = createGeometry(index, anim.getName(), mesh, materials, animSprite.getMaterialIndex());

            //Attach the geometry to the node
            node.attachChild(geom);
            index++;
        }

        // Create the animation itself and attach the animation
        com.jme3.animation.Animation animation = new com.jme3.animation.Animation("anim", (anim.getFrames() - 1) / 30f);
        animation.setTracks(poseTracks.toArray(new PoseTrack[poseTracks.size()]));
        AnimControl control = new AnimControl();
        control.addAnim(animation);
        node.addControl(control);

        //Attach the node to the root
        root.attachChild(node);
    }

    private VertexBuffer[] createIndices(final Map<Integer, List<Triangle>> trianglesMap) {

        // Triangles are not in order, sometimes they are very random, many missing etc.
        // For JME 3.0 this was somehow ok, but JME 3.1 doesn't do some automatic organizing etc.
        // Assume that if first LOD level is null, there are no LOD levels, instead the mesh should be just point mesh
        // I tried ordering them etc. but it went worse (3DFE_GemHolder.kmf is a good example)
        //
        // Ultimately all failed, now just instead off completely empty buffers, put one 0 there, seems to have done the trick
        //
        // Triangles, we have LODs here
        VertexBuffer[] lodLevels = new VertexBuffer[trianglesMap.size()];
        for (Entry<Integer, List<Triangle>> triangles : trianglesMap.entrySet()) {
            if (!triangles.getValue().isEmpty()) {
                int[] indexes = new int[triangles.getValue().size() * 3];
                int x = 0;
                for (Triangle triangle : triangles.getValue()) {
                    indexes[x * 3] = triangle.getTriangle()[2];
                    indexes[x * 3 + 1] = triangle.getTriangle()[1];
                    indexes[x * 3 + 2] = triangle.getTriangle()[0];
                    x++;
                }
                VertexBuffer buf = new VertexBuffer(Type.Index);
                buf.setupData(VertexBuffer.Usage.Static, 3, VertexBuffer.Format.UnsignedInt, BufferUtils.createIntBuffer(indexes));
                lodLevels[triangles.getKey()] = buf;
            } else {

                // Need to create this seemingly empty buffer
                VertexBuffer buf = new VertexBuffer(Type.Index);
                buf.setupData(VertexBuffer.Usage.Static, 3, VertexBuffer.Format.UnsignedInt, BufferUtils.createIntBuffer(new int[]{0}));
                lodLevels[triangles.getKey()] = buf;

            }
        }
        return lodLevels;
    }

    /**
     * Creates a geometry from the given mesh, applies material and LOD control
     * to it
     *
     * @param index mesh index (just for naming)
     * @param name the name, just for logging
     * @param mesh the mesh
     * @param materials list of materials
     * @param materialIndex the material index
     * @return
     */
    private Geometry createGeometry(int index, String name, Mesh mesh, Map<Integer, List<Material>> materials, int materialIndex) {

        //Create geometry
        Geometry geom = new Geometry(index + "", mesh);

        //Add LOD control
        LodControl lc = new LodControl();
        geom.addControl(lc);

        // Material, set the first
        geom.setMaterial(materials.get(materialIndex).get(0));
        if (geom.getMaterial().isTransparent()) {
            geom.setQueueBucket(RenderQueue.Bucket.Transparent);
        }

        // The receive shadows flag is used to turn the shadows completely off
        if (!geom.getMaterial().isReceivesShadows()) {
            geom.setShadowMode(RenderQueue.ShadowMode.Off);
        }

        // If we have multiple materials to choose from, tag them to the geometry
        if (materials.get(materialIndex).size() > 1) {
            geom.setUserData(MATERIAL_ALTERNATIVE_TEXTURES_COUNT, materials.get(materialIndex).size());
        }

        // Update bounds
        geom.updateModelBound();

        // Try to generate tangents
        try {
            TangentBinormalGenerator.generate(mesh);
        } catch (Exception e) {
            logger.log(Level.WARNING, "Failed to generate tangent binormals for " + name + "! ", e);
        }

        return geom;
    }

    /**
     * Set some flags on the material that do not get saved
     *
     * @param material material to modify
     * @param kmfMaterial the KMF material entry
     */
    private void setMaterialFlags(Material material, toniarts.openkeeper.tools.convert.kmf.Material kmfMaterial) {

        // Shadows thing is just a guess, like, they seem to be small light sources
        material.setReceivesShadows(!kmfMaterial.getFlag().contains(toniarts.openkeeper.tools.convert.kmf.Material.MaterialFlag.ALPHA_ADDITIVE));
        material.setTransparent(kmfMaterial.getFlag().contains(toniarts.openkeeper.tools.convert.kmf.Material.MaterialFlag.HAS_ALPHA) || kmfMaterial.getFlag().contains(toniarts.openkeeper.tools.convert.kmf.Material.MaterialFlag.ALPHA_ADDITIVE));
    }

    /**
     * <i>Extracts</i> the materials from the KMF file
     *
     * @param kmfFile the KMF file
     * @param generateMaterialFile should we create J3M material file (in total
     * conversion always yes)
     * @param assetInfo the asset info
     * @param engineTextureFile instance of engine textures file
     * @return returns materials by the material index
     * @throws IOException may fail
     */
    private Map<Integer, List<Material>> getMaterials(KmfFile kmfFile, boolean generateMaterialFile, AssetInfo assetInfo) throws IOException {

        //
        // Create the materials
        //
        Map<Integer, List<Material>> materials = new HashMap(kmfFile.getMaterials().size());
        int i = 0;
        for (toniarts.openkeeper.tools.convert.kmf.Material mat : kmfFile.getMaterials()) {
            Material material = null;

            // Get the texture, the first one
            // There is a list of possible alternative textures
            String texture = mat.getTextures().get(0);
            if (textureFixes.containsKey(texture)) {

                //Fix the texture entry
                texture = textureFixes.get(texture);
            }

            // See if the material is found already on the cache
            String materialLocation = null;
            String materialKey = null;
            String fileName;
            if (generateMaterialFile) {
                materialKey = materialCache.get(mat);
                if (materialKey != null) {
                    material = assetInfo.getManager().loadMaterial(materialKey);
                    setMaterialFlags(material, mat);
                    List<Material> materialList = new ArrayList<>(mat.getTextures().size());
                    materialList.add(material);

                    // If we have multiple textures, we can just fake them, we just need the count really
                    if (mat.getTextures().size() > 1) {
                        materialList.add(material); // Fake it
                    }

                    materials.put(i, materialList);
                    i++;
                    continue;
                } else {

                    // Ok, it it not in the cache yet, but maybe it has been already generated, so use it and update the defaults in it
                    fileName = ConversionUtils.stripFileName(mat.getName());

                    // If there are multiple texture options, add a suffix to the material file name
                    if (mat.getTextures().size() > 1) {
                        fileName = fileName.concat(MATERIAL_ALTERNATIVE_TEXTURE_SUFFIX_SEPARATOR).concat("0");
                    }

                    materialKey = AssetsConverter.MATERIALS_FOLDER.concat("/").concat(fileName).concat(".j3m");
                    materialLocation = AssetsConverter.getAssetsFolder().concat(AssetsConverter.MATERIALS_FOLDER.concat(File.separator).concat(fileName).concat(".j3m"));

                    // See if it exists
                    File file = new File(materialLocation).getCanonicalFile();
                    if (file.exists()) {
                        if (!file.getName().equals(fileName.concat(".j3m"))) {

                            // Case sensitivity issue
                            materialKey = AssetsConverter.MATERIALS_FOLDER.concat("/").concat(file.getName());
                            materialLocation = AssetsConverter.getAssetsFolder().concat(AssetsConverter.MATERIALS_FOLDER.concat(File.separator).concat(file.getName()));
                        }
                        material = assetInfo.getManager().loadMaterial(materialKey);
                    }
                }
            }

            // Create the material
            if (material == null) {
                material = new Material(assetInfo.getManager(), "Common/MatDefs/Light/Lighting.j3md");
            }

            //Load up the texture and create the material
            Texture tex = loadTexture(texture, assetInfo);
            material.setTexture("DiffuseMap", tex);
            material.setColor("Specular", ColorRGBA.Orange); // Dungeons are lit only with fire...? Experimental
            material.setColor("Diffuse", ColorRGBA.White); // Experimental
            material.setFloat("Shininess", 128 * mat.getBrightness()); // Use the brightness as shininess... Experimental

            // Set some flags
            setMaterialFlags(material, mat);

            // Read the flags & stuff
            if (mat.getFlag().contains(toniarts.openkeeper.tools.convert.kmf.Material.MaterialFlag.HAS_ALPHA)) {
                material.setFloat("AlphaDiscardThreshold", 0.1f);
                material.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);
            }
            if (mat.getFlag().contains(toniarts.openkeeper.tools.convert.kmf.Material.MaterialFlag.ALPHA_ADDITIVE)) {
                material.setFloat("AlphaDiscardThreshold", 0.1f);
                material.getAdditionalRenderState().setDepthWrite(false);
                material.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.AlphaAdditive);
            }

            // Add material to list and create the possible alternatives
            List<Material> materialList = new ArrayList<>(mat.getTextures().size());
            materialList.add(material);
            for (int k = 1; k < mat.getTextures().size(); k++) {

                // Get the texture
                String alternativeTexture = mat.getTextures().get(k);
                if (textureFixes.containsKey(alternativeTexture)) {

                    //Fix the texture entry
                    alternativeTexture = textureFixes.get(alternativeTexture);
                }
                Texture alternativeTex = loadTexture(alternativeTexture, assetInfo);

                // Clone the original material, set texture and add to list
                Material alternativeMaterial = material.clone();
                alternativeMaterial.setTexture("DiffuseMap", alternativeTex);
                materialList.add(alternativeMaterial);
            }

            // See if we should save the materials
            if (generateMaterialFile) {
                for (int k = 0; k < materialList.size(); k++) {

                    Material m = materialList.get(k);

                    // If there are multiple textures / material options, alter the key and location
                    if (materialList.size() > 1) {
                        materialKey = materialKey.substring(0, materialKey.lastIndexOf(MATERIAL_ALTERNATIVE_TEXTURE_SUFFIX_SEPARATOR) + 1).concat(k + "").concat(materialKey.substring(materialKey.lastIndexOf(".")));
                        materialLocation = materialLocation.substring(0, materialLocation.lastIndexOf(MATERIAL_ALTERNATIVE_TEXTURE_SUFFIX_SEPARATOR) + 1).concat(k + "").concat(materialLocation.substring(materialLocation.lastIndexOf(".")));
                    }

                    // Set the material so that it realizes that it is a J3M file
                    m.setName(mat.getName());
                    m.setKey(new MaterialKey(materialKey));

                    // Save
                    J3MExporter exporter = new J3MExporter();
                    try (OutputStream out = Files.newOutputStream(Paths.get(materialLocation));
                            BufferedOutputStream bout = new BufferedOutputStream(out)) {
                        exporter.save(m, bout);
                    }

                    // Put the first one to the cache
                    if (k == 0) {
                        materialCache.put(mat, materialKey);
                    }
                }
            }

            materials.put(i, materialList);
            i++;
        }
        return materials;
    }

    /**
     * Loads a JME texture of the texture name
     *
     * @param texture the texture name
     * @param assetInfo the assetInfo
     * @return texture file
     */
    private Texture loadTexture(String texture, AssetInfo assetInfo) {

        // Load the texture
        TextureKey textureKey = new TextureKey(ConversionUtils.getCanonicalAssetKey(AssetsConverter.TEXTURES_FOLDER.concat("/").concat(texture).concat(".png")), false);
        Texture tex = assetInfo.getManager().loadTexture(textureKey);
        return tex;
    }

    /**
     * A frame info identifies an vertex transition, it has the start and the
     * end pose frame indexes (key frames) which will identify it
     */
    private class FrameInfo implements Comparable<KmfModelLoader.FrameInfo> {

        private final int previousPoseFrame;
        private final int nextPoseFrame;
        private final float weight;

        public FrameInfo(int previousPoseFrame, int nextPoseFrame, float weight) {
            this.previousPoseFrame = previousPoseFrame;
            this.nextPoseFrame = nextPoseFrame;
            this.weight = weight;
        }

        public int getPreviousPoseFrame() {
            return previousPoseFrame;
        }

        public int getNextPoseFrame() {
            return nextPoseFrame;
        }

        public float getWeight() {
            return weight;
        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 79 * hash + this.previousPoseFrame;
            hash = 79 * hash + this.nextPoseFrame;
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final KmfModelLoader.FrameInfo other = (KmfModelLoader.FrameInfo) obj;
            if (this.previousPoseFrame != other.previousPoseFrame) {
                return false;
            }
            if (this.nextPoseFrame != other.nextPoseFrame) {
                return false;
            }
            return true;
        }

        @Override
        public int compareTo(KmfModelLoader.FrameInfo o) {
            int result = Integer.compare(previousPoseFrame, o.previousPoseFrame);
            if (result == 0) {

                // It is up to the second key then
                result = Integer.compare(nextPoseFrame, o.nextPoseFrame);
            }
            return result;
        }
    }
}
