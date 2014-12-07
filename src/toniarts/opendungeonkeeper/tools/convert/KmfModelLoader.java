/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package toniarts.opendungeonkeeper.tools.convert;

import com.jme3.animation.AnimControl;
import com.jme3.asset.AssetInfo;
import com.jme3.asset.AssetKey;
import com.jme3.asset.AssetLoader;
import com.jme3.asset.ModelKey;
import com.jme3.asset.TextureKey;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
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
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;
import toniarts.opendungeonkeeper.animation.Pose;
import toniarts.opendungeonkeeper.animation.PoseTrack;
import toniarts.opendungeonkeeper.animation.PoseTrack.PoseFrame;
import toniarts.opendungeonkeeper.tools.convert.enginetextures.EngineTextureEntry;
import toniarts.opendungeonkeeper.tools.convert.enginetextures.EngineTexturesFile;
import toniarts.opendungeonkeeper.tools.convert.kmf.Anim;
import toniarts.opendungeonkeeper.tools.convert.kmf.AnimSprite;
import toniarts.opendungeonkeeper.tools.convert.kmf.AnimVertex;
import toniarts.opendungeonkeeper.tools.convert.kmf.Grop;
import toniarts.opendungeonkeeper.tools.convert.kmf.KmfFile;
import toniarts.opendungeonkeeper.tools.convert.kmf.MeshSprite;
import toniarts.opendungeonkeeper.tools.convert.kmf.MeshVertex;
import toniarts.opendungeonkeeper.tools.convert.kmf.Triangle;
import toniarts.opendungeonkeeper.tools.convert.kmf.Uv;
import toniarts.opendungeonkeeper.tools.modelviewer.ModelViewer;

/**
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class KmfModelLoader implements AssetLoader {

    /* Some textures are broken */
    private final static HashMap<String, String> textureFixes;

    static {
        textureFixes = new HashMap<>();
        textureFixes.put("Goblinbak", "GoblinBack");
        textureFixes.put("Goblin2", "GoblinFront");
        textureFixes.put("Knightfrnt", "KnightFrnt");
        textureFixes.put("skeleton", "Skeleton");
        textureFixes.put("3dMap_secretL1+3", "3dMap_SecretL1+3");
        textureFixes.put("Level17_castle", "Level17_Castle");
        textureFixes.put("gui\\Traps\\Alarm", "GUI/Traps/alarm");
        textureFixes.put("3dMap_secretL4+5", "3dMap_SecretL4+5");
        textureFixes.put("ThiefBack", "THIEFback");
        textureFixes.put("ThiefFront", "THIEFfront");
        textureFixes.put("StoneKnightFrnt", "StoneKnightfrnt");
        textureFixes.put("3dMap_secretL2", "3dMap_SecretL2");
        textureFixes.put("WARLOCKback", "WarlockBack");
    }
    private static final Logger logger = Logger.getLogger(KmfModelLoader.class.getName());

    public static void main(final String[] args) throws IOException {

        //Take Dungeon Keeper 2 root folder as parameter
        if (args.length != 2 && !new File(args[0]).exists()) {
            throw new RuntimeException("Please provide Dungeon Keeper II root folder as a first parameter! Second parameter is the actual model file!");
        }

        AssetInfo ai = new AssetInfo(/*main.getAssetManager()*/null, null) {
            @Override
            public InputStream openStream() {
                try {
                    final File file = new File(args[0]);
                    key = new AssetKey() {
                        @Override
                        public String getName() {
                            return file.toPath().getFileName().toString();
                        }
                    };
                    return new FileInputStream(file);
                } catch (FileNotFoundException ex) {
                    logger.log(Level.SEVERE, null, ex);
                }
                return null;
            }
        };

        ModelViewer app = new ModelViewer(new File(args[1]), args[0]);
        app.start();
    }

    @Override
    public Object load(AssetInfo assetInfo) throws IOException {

        KmfFile kmfFile;
        EngineTexturesFile engineTextureFile = null;
        if (assetInfo instanceof KmfAssetInfo) {
            kmfFile = ((KmfAssetInfo) assetInfo).getKmfFile();
            engineTextureFile = ((KmfAssetInfo) assetInfo).getEngineTexturesFile();
        } else {
            kmfFile = new KmfFile(inputStreamToFile(assetInfo.openStream(), assetInfo.getKey().getName()));
        }

        //Create a root
        Node root = new Node("Root");

        if (kmfFile.getType() == KmfFile.Type.MESH || kmfFile.getType() == KmfFile.Type.ANIM) {

            //
            // Create the materials
            //
            HashMap<Integer, Material> materials = new HashMap(kmfFile.getMaterials().size());
            int i = 0;
            for (toniarts.opendungeonkeeper.tools.convert.kmf.Material mat : kmfFile.getMaterials()) {
                Material material = new Material(assetInfo.getManager(), "Common/MatDefs/Light/Lighting.j3md");
                String texture = mat.getTextures().get(0);
                if (textureFixes.containsKey(texture)) {

                    //Fix the texture entry
                    texture = textureFixes.get(texture);
                }

                //Load up the texture and create the material
                TextureKey textureKey = new TextureKey(AssetsConverter.TEXTURES_FOLDER.concat("/").concat(texture).concat(".png"), false);
                Texture tex = assetInfo.getManager().loadTexture(textureKey);
                material.setTexture("DiffuseMap", tex);
                material.setColor("Specular", ColorRGBA.Orange); // Dungeons are lit only with fire...? Experimental
                material.setColor("Diffuse", ColorRGBA.White); // Experimental
                material.setFloat("Shininess", 128 * mat.getBrightness()); // Use the brightness as shininess... Experimental

                // If we have an instance of engine texture file, check the alpha
                if (engineTextureFile != null) {
                    String textureEntry = texture.concat("MM0");
                    EngineTextureEntry engineTextureEntry = engineTextureFile.getEntry(textureEntry);
                    if (engineTextureEntry != null && engineTextureEntry.isAlphaFlag()) {
                        material.setBoolean("UseAlpha", true);
                        material.setFloat("AlphaDiscardThreshold", 0.1f);
                        material.setTransparent(true);
                        material.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);
                        logger.log(Level.INFO, "Texture entry {0} has alpha!", textureEntry);
                    } else if (engineTextureEntry == null) {

                        // Just log
                        logger.log(Level.WARNING, "Texture entry {0} not found from the engine textures!", textureEntry);
                    }
                }

                materials.put(i, material);
                i++;
            }

            //
            // The meshes
            //
            for (toniarts.opendungeonkeeper.tools.convert.kmf.Mesh sourceMesh : kmfFile.getMeshes()) {
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
            String key = AssetsConverter.MODELS_FOLDER.concat("/").concat(grop.getName()).concat(".j3o");
            AssetLinkNode modelLink = new AssetLinkNode(key, new ModelKey(key));
            modelLink.setLocalTranslation(new Vector3f(grop.getPos().x, grop.getPos().y, grop.getPos().z));
            root.attachChild(modelLink);
        }
    }

    /**
     * Converts input stream to a file by writing it to a temp file (yeah...)
     *
     * @param is the InputStream
     * @param prefix temp file prefix
     * @return random access file
     * @throws IOException
     */
    public static File inputStreamToFile(InputStream is, String prefix) throws IOException {
        File tempFile = File.createTempFile(prefix, "kmf");
        tempFile.deleteOnExit();

        //Write the file
        try (BufferedOutputStream output = new BufferedOutputStream(new FileOutputStream(tempFile))) {

            //Write in blocks
            byte[] buffer = new byte[2048];
            int tmp;

            while ((tmp = is.read(buffer)) != -1) {
                output.write(buffer, 0, tmp);
            }
        }


        return tempFile;
    }

    /**
     * Handle mesh creation
     *
     * @param sourceMesh the mesh
     * @param materials materials map
     * @param root the root node
     */
    private void handleMesh(toniarts.opendungeonkeeper.tools.convert.kmf.Mesh sourceMesh, HashMap<Integer, Material> materials, Node root) {

        //Source mesh is node
        Node node = new Node(sourceMesh.getName());
        node.setLocalScale(sourceMesh.getScale());
        node.setLocalTranslation(new Vector3f(sourceMesh.getPos().x, sourceMesh.getPos().y, sourceMesh.getPos().z));

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
                vertices[i] = new Vector3f(v.x, v.y, v.z);

                //Texture coordinate
                Uv uv = meshVertex.getUv();
                texCoord[i] = new Vector2f(uv.getUv()[0] / 32768f, uv.getUv()[1] / 32768f);

                //Normals
                v = meshVertex.getNormal();
                normals[i] = new Vector3f(v.x, v.y, v.z);

                i++;
            }

            // Triangles, we have LODs here
            VertexBuffer[] lodLevels = new VertexBuffer[meshSprite.getTriangles().size()];
            for (Entry<Integer, List<Triangle>> triangles : meshSprite.getTriangles().entrySet()) {
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
            }

            //Max LOD level triangles
            List<Integer> faces = new ArrayList<>(meshSprite.getTriangles().get(0).size() * 3);
            for (Triangle tri : meshSprite.getTriangles().get(0)) {
                faces.add(Short.valueOf(tri.getTriangle()[2]).intValue());
                faces.add(Short.valueOf(tri.getTriangle()[1]).intValue());
                faces.add(Short.valueOf(tri.getTriangle()[0]).intValue());
            }
            int[] indexes = new int[faces.size()];
            int x = 0;
            for (Integer inte : faces) {
                indexes[x] = inte;
                x++;
            }

            //Set the buffers
            mesh.setBuffer(Type.Position, 3, BufferUtils.createFloatBuffer(vertices));
            mesh.setBuffer(Type.Index, 3, BufferUtils.createIntBuffer(indexes));
            mesh.setLodLevels(lodLevels);
            mesh.setBuffer(Type.TexCoord, 2, BufferUtils.createFloatBuffer(texCoord));
            mesh.setBuffer(Type.Normal, 3, BufferUtils.createFloatBuffer(normals));

            mesh.updateBound();

            // Create geometry
            Geometry geom = createGeometry(index, mesh, materials, meshSprite.getMaterialIndex());

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
    private void handleAnim(Anim anim, HashMap<Integer, Material> materials, Node root) {

        //Source mesh is node
        Node node = new Node(anim.getName());
        node.setLocalScale(anim.getCubeScale());
        node.setLocalTranslation(new Vector3f(anim.getPos().x, anim.getPos().y, anim.getPos().z));

        // PoseFrames for each mesh index
        HashMap<Integer, List<PoseFrame>> frames = new HashMap<>(anim.getSprites().size());

        int index = 0;
        for (AnimSprite animSprite : anim.getSprites()) {

            // Animation

            // Poses for each frame
            HashMap<Integer, List<Pose>> poses = new HashMap<>(anim.getFrames() - 1);

            HashMap<Integer, List<Integer>> frameIndices = new HashMap<>(anim.getFrames() - 1);
            HashMap<Integer, List<Vector3f>> frameOffsets = new HashMap<>(anim.getFrames() - 1);

            //

            //Each sprite represents a geometry (+ mesh) since they each have their own material
            Mesh mesh = new Mesh();

            //Vertices, UV (texture coordinates), normals
            Vector3f[] vertices = new Vector3f[animSprite.getVertices().size()];
            Vector2f[] texCoord = new Vector2f[animSprite.getVertices().size()];
            Vector3f[] normals = new Vector3f[animSprite.getVertices().size()];
            int i = 0;
            for (AnimVertex animVertex : animSprite.getVertices()) {

                javax.vecmath.Vector3f baseCoord = null;

                //Go through every frame
//                System.out.println("Indice: " + i);
                for (int frame = 0; frame < anim.getFrames() - 1; frame++) {

//                    System.out.println("Frame: " + frame);

                    //Vertice
                    int geomBase = anim.getItab()[frame >> 7][animVertex.getItabIndex()];
                    short geomOffset = anim.getOffsets()[animVertex.getItabIndex()][frame];
                    int geomIndex = geomBase + geomOffset;

                    short frameBase = anim.getGeometries().get(geomIndex).getFrameBase();
                    short nextFrameBase = anim.getGeometries().get(geomIndex + 1).getFrameBase();
                    float geomFactor = (float) ((frame & 0x7f) - frameBase) / (float) (nextFrameBase - frameBase);
                    javax.vecmath.Vector3f coord = anim.getGeometries().get(geomIndex).getGeometry();

//                    System.out.println(frameBase + " -> " + nextFrameBase);

                    // Store the frame zero coord
                    if (frame == 0) {
                        baseCoord = coord;
                    }

                    // The next coordinate
                    javax.vecmath.Vector3f nextCoord = anim.getGeometries().get(geomIndex + 1).getGeometry();
                    javax.vecmath.Vector3f interpCoord = new javax.vecmath.Vector3f(nextCoord);
                    interpCoord.sub(coord);
                    interpCoord.scale(geomFactor);
                    interpCoord.add(coord);

                    //Add if it has moved
//                    if (!interpCoord.equals(coord)) {
                    if (!frameIndices.containsKey(frame)) {
                        frameIndices.put(frame, new ArrayList<Integer>());
                    }
                    if (!frameOffsets.containsKey(frame)) {
                        frameOffsets.put(frame, new ArrayList<Vector3f>());
                    }
                    frameIndices.get(frame).add(i);
                    frameOffsets.get(frame).add(new Vector3f(interpCoord.x, interpCoord.y, interpCoord.z));
//                    }
                }

//                System.out.println("GEOM index: " + geomIndex);
//                System.out.println("Framebase: " + anim.getGeometries().get(geomIndex).getFrameBase());
//                javax.vecmath.Vector3f v = anim.getGeometries().get(geomIndex).getGeometry();
//                System.out.println(coord);
                vertices[i] = new Vector3f(baseCoord.x, baseCoord.y, baseCoord.z);

                //Texture coordinate
                Uv uv = animVertex.getUv();
                texCoord[i] = new Vector2f(uv.getUv()[0] / 32768f, uv.getUv()[1] / 32768f);

                //Normals
                javax.vecmath.Vector3f v = animVertex.getNormal();
                normals[i] = new Vector3f(v.x, v.y, v.z);

                i++;
            }

            // We have all the animation vertices from a single pose
            for (int frame = 0; frame < anim.getFrames() - 1; frame++) {
                if (!poses.containsKey(frame)) {
                    poses.put(frame, new ArrayList<Pose>());
                }
                if (frameIndices.containsKey(frame)) {
                    List<Integer> list = frameIndices.get(frame);
                    int[] array = new int[list.size()];
                    for (int integer = 0; integer < list.size(); integer++) {
                        array[integer] = list.get(integer);
                    }
                    Pose p = new Pose(index + "", index, frameOffsets.get(frame).toArray(new Vector3f[frameOffsets.get(frame).size()]), array);
                    poses.get(frame).add(p);
                } else {

                    // This mesh is not going to move on this frame, contruct an empty pose
                    Pose p = new Pose(index + "", index, new Vector3f[0], new int[0]);
                    poses.get(frame).add(p);
                }
            }

            // More animation, create the pose frames by the mesh index
            List<PoseFrame> frameList = new ArrayList<>(anim.getFrames() - 1);
            for (int frame = 0; frame < anim.getFrames() - 1; frame++) {
                if (!poses.containsKey(frame)) {
                    continue;
                }
                List<Pose> posesList = poses.get(frame);

                // Create the weight array, I don't know, just make everything 1f
                float[] weights = new float[posesList.size()];
                for (int x = 0; x < posesList.size(); x++) {
                    weights[x] = 1.0f;
                }

                // Create and add the frame
                PoseFrame f = new PoseFrame(posesList.toArray(new Pose[poses.get(frame).size()]), weights);
                frameList.add(f);
            }
            frames.put(index, frameList);

            // Triangles, we have LODs here
            VertexBuffer[] lodLevels = new VertexBuffer[animSprite.getTriangles().size()];
            for (Entry<Integer, List<Triangle>> triangles : animSprite.getTriangles().entrySet()) {
                int[] indexes = new int[triangles.getValue().size() * 3];
                int x = 0;
                for (Triangle triangle : triangles.getValue()) {
                    indexes[x * 3] = triangle.getTriangle()[2];
                    indexes[x * 3 + 1] = triangle.getTriangle()[1];
                    indexes[x * 3 + 2] = triangle.getTriangle()[0];
                    x++;
                }
                VertexBuffer buf = new VertexBuffer(Type.Index);
                buf.setupData(VertexBuffer.Usage.Dynamic, 3, VertexBuffer.Format.UnsignedInt, BufferUtils.createIntBuffer(indexes));
                lodLevels[triangles.getKey()] = buf;
            }

            //Max LOD level triangles
            List<Integer> faces = new ArrayList<>(animSprite.getTriangles().get(0).size() * 3);
            for (Triangle tri : animSprite.getTriangles().get(0)) {
                faces.add(Short.valueOf(tri.getTriangle()[2]).intValue());
                faces.add(Short.valueOf(tri.getTriangle()[1]).intValue());
                faces.add(Short.valueOf(tri.getTriangle()[0]).intValue());
            }
            int[] indexes = new int[faces.size()];
            int x = 0;
            for (Integer inte : faces) {
                indexes[x] = inte;
                x++;
            }

            //Set the buffers
            mesh.setBuffer(Type.Position, 3, BufferUtils.createFloatBuffer(vertices));
            mesh.setBuffer(Type.BindPosePosition, 3, BufferUtils.createFloatBuffer(vertices));
            mesh.setBuffer(Type.Index, 3, BufferUtils.createIntBuffer(indexes));
            mesh.setLodLevels(lodLevels);
            mesh.setBuffer(Type.TexCoord, 2, BufferUtils.createFloatBuffer(texCoord));
            mesh.setBuffer(Type.Normal, 3, BufferUtils.createFloatBuffer(normals));
            mesh.setBuffer(Type.BindPoseNormal, 3, BufferUtils.createFloatBuffer(normals));

            mesh.updateBound();

            // Create geometry
            Geometry geom = createGeometry(index, mesh, materials, animSprite.getMaterialIndex());

            //Attach the geometry to the node
            node.attachChild(geom);
            index++;
        }

        // Create times
        float[] times = new float[anim.getFrames()];
        for (int i = 0; i < anim.getFrames(); i++) {
            times[i] = (i + 1) / 30f;
        }

        // Create pose tracks for each mesh index
        List<PoseTrack> poseTracks = new ArrayList<>(frames.size());
        for (Entry<Integer, List<PoseFrame>> entry : frames.entrySet()) {
            PoseTrack poseTrack = new PoseTrack(entry.getKey(), times, entry.getValue().toArray(new PoseFrame[entry.getValue().size()]));
            poseTracks.add(poseTrack);
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

    /**
     * Creates a geometry from the given mesh, applies material and LOD control
     * to it
     *
     * @param index mesh index (just for naming)
     * @param mesh the mesh
     * @param materials list of materials
     * @param materialIndex the material index
     * @return
     */
    private Geometry createGeometry(int index, Mesh mesh, HashMap<Integer, Material> materials, int materialIndex) {

        //Create geometry
        Geometry geom = new Geometry(index + "", mesh);

        //Add LOD control
        LodControl lc = new LodControl();
        geom.addControl(lc);

        // Material
        geom.setMaterial(materials.get(materialIndex));
        if (geom.getMaterial().isTransparent()) {
            geom.setQueueBucket(RenderQueue.Bucket.Transparent);
        }

        geom.updateModelBound();

        return geom;
    }
}
