/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package toniarts.opendungeonkeeper.tools.convert;

import com.jme3.asset.AssetInfo;
import com.jme3.asset.AssetKey;
import com.jme3.asset.AssetLoader;
import com.jme3.export.binary.BinaryExporter;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Node;
import com.jme3.scene.VertexBuffer;
import com.jme3.scene.VertexBuffer.Type;
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
import java.util.List;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;
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

        KmfFile kmfFile = new KmfFile(inputStreamToFile(assetInfo.openStream(), assetInfo.getKey().getName()));

        //Create a root
        Node root = new Node("Root");

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
                int i = 0;
                for (MeshVertex meshVertex : meshSprite.getVertices()) {

                    //Vertice
                    javax.vecmath.Vector3f v = sourceMesh.getGeometries().get(meshVertex.getGeomIndex());
                    vertices[i] = new Vector3f(v.x, v.y, v.z);

                    //Texture coordinate
                    Uv uv = meshVertex.getUv();
                    texCoord[i] = new Vector2f(uv.getUv()[0], uv.getUv()[1]);

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
                        indexes[x * 3] = triangle.getTriangle()[0];
                        indexes[x * 3 + 1] = triangle.getTriangle()[1];
                        indexes[x * 3 + 2] = triangle.getTriangle()[2];
                        x++;
                    }
                    VertexBuffer buf = new VertexBuffer(Type.Index);
                    buf.setupData(VertexBuffer.Usage.Static, 3, VertexBuffer.Format.Int, BufferUtils.createIntBuffer(indexes));
                    lodLevels[triangles.getKey()] = buf;
                }

                //Max LOD level triangles
                List<Integer> faces = new ArrayList<>(meshSprite.getTriangles().get(0).size() * 3);
                for (Triangle tri : meshSprite.getTriangles().get(0)) {
                    faces.add(new Short(tri.getTriangle()[0]).intValue());
                    faces.add(new Short(tri.getTriangle()[1]).intValue());
                    faces.add(new Short(tri.getTriangle()[2]).intValue());
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
//                mesh.setLodLevels(lodLevels); <--- Breaks stuff???
                mesh.setBuffer(Type.TexCoord, 2, BufferUtils.createFloatBuffer(texCoord));
                mesh.setBuffer(Type.Normal, 3, BufferUtils.createFloatBuffer(normals));

                mesh.updateBound();

                //Create geometry
                Geometry geom = new Geometry(index + "", mesh);

                //FIXME: Proper material
//                Material mat = new Material(assetInfo.getManager(), "Common/MatDefs/Misc/Unshaded.j3md");
//                mat.setColor("Color", ColorRGBA.Blue);
//                geom.setMaterial(mat);

                //Attach the geometry to the node
                node.attachChild(geom);
                index++;
            }

            //Attach the node to the root
            root.attachChild(node);
        }

        return root;
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
