/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package toniarts.opendungeonkeeper.tools.convert;

import com.jme3.asset.AssetInfo;
import com.jme3.asset.AssetKey;
import com.jme3.asset.AssetLoader;
import com.jme3.asset.ModelKey;
import com.jme3.export.binary.BinaryExporter;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.scene.AssetLinkNode;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Node;
import com.jme3.scene.VertexBuffer;
import com.jme3.scene.VertexBuffer.Type;
import com.jme3.texture.Texture;
import com.jme3.util.BufferUtils;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;
import toniarts.opendungeonkeeper.tools.convert.kmf.Grop;
import toniarts.opendungeonkeeper.tools.convert.kmf.KmfFile;
import toniarts.opendungeonkeeper.tools.convert.kmf.MeshSprite;
import toniarts.opendungeonkeeper.tools.convert.kmf.MeshVertex;
import toniarts.opendungeonkeeper.tools.convert.kmf.Triangle;
import toniarts.opendungeonkeeper.tools.convert.kmf.Uv;

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
    }

    public static void main(final String[] args) throws IOException {
//        Main main = new Main();
//        main.start();
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
                    Logger.getLogger(KmfModelLoader.class.getName()).log(Level.SEVERE, null, ex);
                }
                return null;
            }
        };
        KmfModelLoader kmfModelLoader = new KmfModelLoader();
        Node n = (Node) kmfModelLoader.load(ai);

        //Export
        BinaryExporter exporter = BinaryExporter.getInstance();
        File file = new File(args[0]);
        String path = args[0].substring(0, args[0].length() - file.toPath().getFileName().toString().length());
        file = new File(path + "converted" + File.separator + file.toPath().getFileName().toString() + ".j3o");
        exporter.save(n, file);

        //Load all KMF files
        File f = new File(path);
        File[] files = f.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.toLowerCase().endsWith(".kmf");
            }
        });
        for (File kmf : files) {
            KmfFile kmfFile = new KmfFile(kmf);
            System.out.println(kmf + " is of type " + kmfFile.getType());
        }
    }

    @Override
    public Object load(AssetInfo assetInfo) throws IOException {

        KmfFile kmfFile = null;
        if (assetInfo instanceof KmfAssetInfo) {
            kmfFile = ((KmfAssetInfo) assetInfo).getKmfFile();
        } else {
            kmfFile = new KmfFile(inputStreamToFile(assetInfo.openStream(), assetInfo.getKey().getName()));
        }

        //Create a root
        Node root = new Node("Root");

        if (kmfFile.getType() == KmfFile.Type.MESH) {

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
                Texture tex = assetInfo.getManager().loadTexture(AssetsConverter.TEXTURES_FOLDER.concat("/").concat(texture).concat(".png"));
                material.setTexture("DiffuseMap", tex);
                material.setColor("Specular", ColorRGBA.Orange); // Dungeons are lit only with fire...? Experimental
                material.setColor("Diffuse", ColorRGBA.White); // Experimental
                material.setFloat("Shininess", 128 * mat.getBrightness()); // Use the brightness as shininess... Experimental
                materials.put(i, material);
                i++;
            }

            //
            // The meshes
            //
            for (toniarts.opendungeonkeeper.tools.convert.kmf.Mesh sourceMesh : kmfFile.getMeshes()) {

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
                    i = 0;
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
                        faces.add(new Short(tri.getTriangle()[2]).intValue());
                        faces.add(new Short(tri.getTriangle()[1]).intValue());
                        faces.add(new Short(tri.getTriangle()[0]).intValue());
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

                    //Create geometry
                    Geometry geom = new Geometry(index + "", mesh);

                    // Material
                    geom.setMaterial(materials.get(meshSprite.getMaterialIndex()));

                    //Attach the geometry to the node
                    node.attachChild(geom);
                    index++;
                }

                //Attach the node to the root
                root.attachChild(node);
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
}
