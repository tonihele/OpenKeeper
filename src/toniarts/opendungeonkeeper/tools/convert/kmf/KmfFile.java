/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package toniarts.opendungeonkeeper.tools.convert.kmf;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import javax.vecmath.Vector3f;
import toniarts.opendungeonkeeper.tools.convert.Utils;

/**
 * Reads Dungeon Keeper II model file to a data structure<br>
 * The file is LITTLE ENDIAN I might say<br>
 * Uses the Dungeon Keeper 2 File Format Guide by George Gensure
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class KmfFile {

    public enum Type {

        MESH, ANIM, GROP;

        /**
         * Kmf head type value to enum
         *
         * @param index the type value
         * @return returns type
         */
        public static Type toType(int index) {
            if (index == 1) {
                return MESH;
            }
            if (index == 2) {
                return ANIM;
            }
            if (index == 3) {
                return GROP;
            }
            throw new RuntimeException("Type must be 1 -3! Was " + index + "!");
        }
    }
    private int version;
    private Type type;
    private List<Material> materials;
    private List<Mesh> meshes;
    private static final String KMF_HEADER_IDENTIFIER = "KMSH";
    private static final String KMF_HEAD = "HEAD";
    private static final String KMF_MATERIALS = "MATL";
    private static final String KMF_MATERIAL = "MAT2";
    private static final String KMF_MESH = "MESH";
    private static final String KMF_MESH_CONTROL = "CTRL";
    private static final String KMF_MESH_SPRITES = "SPRS";
    private static final String KMF_MESH_SPRITES_HEADER = "SPHD";
    private static final String KMF_MESH_SPRITES_DATA_HEADER = "SPRS";
    private static final String KMF_MESH_GEOM = "GEOM";

    public KmfFile(File file) {

        //Read the file
        try (RandomAccessFile rawKmf = new RandomAccessFile(file, "r")) {

            //Read the identifier
            byte[] buf = new byte[4];
            rawKmf.read(buf);
            String temp = Utils.bytesToString(buf);
            if (!KMF_HEADER_IDENTIFIER.equals(temp)) {
                throw new RuntimeException("Header should be " + KMF_HEADER_IDENTIFIER + " and it was " + temp + "! Cancelling!");
            }


            rawKmf.skipBytes(4);
            version = Utils.readUnsignedInteger(rawKmf);

            //KMSH/HEAD
            rawKmf.read(buf);
            temp = Utils.bytesToString(buf);
            if (!KMF_HEAD.equals(temp)) {
                throw new RuntimeException("Header should be " + KMF_HEAD + " and it was " + temp + "! Cancelling!");
            }
            parseHead(rawKmf);

            //KMSH/MATL
            if (type != Type.GROP) {
                rawKmf.read(buf);
                temp = Utils.bytesToString(buf);
                if (!KMF_MATERIALS.equals(temp)) {
                    throw new RuntimeException("Header should be " + KMF_MATERIALS + " and it was " + temp + "! Cancelling!");
                }
                parseMatl(rawKmf);
            }

            //KMSH/MESH, there are n amount of these
            meshes = new ArrayList();
            do {
                if (rawKmf.read(buf) == -1) {
                    break; // EOF
                }
                temp = Utils.bytesToString(buf);
                if (KMF_MESH.equals(temp)) {
                    meshes.add(parseMesh(rawKmf));
                } else {
                    break;
                }
            } while (true);

        } catch (IOException e) {

            //Fug
            throw new RuntimeException("Failed to open the file " + file + "!", e);
        }
    }

    /**
     * Parses the head section<br>
     * KMSH/HEAD
     *
     * @param rawKmf kmf file starting on HEAD
     */
    private void parseHead(RandomAccessFile rawKmf) throws IOException {
        rawKmf.skipBytes(4);
        int type = Utils.readUnsignedInteger(rawKmf);
        this.type = Type.toType(type);
        int unknown = Utils.readUnsignedInteger(rawKmf);
    }

    /**
     * Parses the materials section<br>
     * KMSH/MATL
     *
     * @param rawKmf kmf file starting on MATL
     */
    private void parseMatl(RandomAccessFile rawKmf) throws IOException {
        rawKmf.skipBytes(4);
        int materialsCount = Utils.readUnsignedInteger(rawKmf);
        byte[] buf = new byte[4];

        //Read the materials
        materials = new ArrayList(materialsCount);
        for (int i = 0; i < materialsCount; i++) {
            rawKmf.read(buf);
            String temp = Utils.bytesToString(buf);
            if (!KMF_MATERIAL.equals(temp)) {
                throw new RuntimeException("Header should be " + KMF_MATERIAL + " and it was " + temp + "! Cancelling!");
            }
            materials.add(parseMat2(rawKmf));
        }
    }

    /**
     * Parses the materials section<br>
     * KMSH/MATL/MAT2
     *
     * @param rawKmf kmf file starting on MATL
     */
    private Material parseMat2(RandomAccessFile rawKmf) throws IOException {
        rawKmf.skipBytes(4);

        //Create the material
        Material m = new Material();

        //Now we should have the name
        m.setName(Utils.readVaryingLengthStrings(rawKmf, 1).get(0));

        //Textures
        int texturesCount = Utils.readUnsignedInteger(rawKmf);
        m.setTextures(Utils.readVaryingLengthStrings(rawKmf, texturesCount));

        m.setFlag(Utils.readUnsignedInteger(rawKmf));
        m.setBrightness(Utils.readFloat(rawKmf));
        m.setGamma(Utils.readFloat(rawKmf));

        //Environment map
        m.setEnvironmentMappingTexture(Utils.readVaryingLengthStrings(rawKmf, 1).get(0));

        return m;
    }

    /**
     * Parses the mesh section<br>
     * KMSH/MESH
     *
     * @param rawKmf kmf file starting on mesh
     */
    private Mesh parseMesh(RandomAccessFile rawKmf) throws IOException {
        rawKmf.skipBytes(4);

        //KMSH/MESH/HEAD
        byte[] buf = new byte[4];
        rawKmf.read(buf);
        String temp = Utils.bytesToString(buf);
        if (!KMF_HEAD.equals(temp)) {
            throw new RuntimeException("Header should be " + KMF_HEAD + " and it was " + temp + "! Cancelling!");
        }
        rawKmf.skipBytes(4);

        //Create the mesh
        Mesh m = new Mesh();

        //Now we should have the name
        m.setName(Utils.readVaryingLengthStrings(rawKmf, 1).get(0));

        int sprsCount = Utils.readUnsignedInteger(rawKmf);
        int geomCount = Utils.readUnsignedInteger(rawKmf);
        m.setPos(new Vector3f(Utils.readFloat(rawKmf), Utils.readFloat(rawKmf), Utils.readFloat(rawKmf)));
        m.setScale(Utils.readFloat(rawKmf));
        int lodCount = Utils.readUnsignedInteger(rawKmf);

        //Controls
        //KMSH/MATL/CTRL
        rawKmf.read(buf);
        temp = Utils.bytesToString(buf);
        if (!KMF_MESH_CONTROL.equals(temp)) {
            throw new RuntimeException("Header should be " + KMF_MESH_CONTROL + " and it was " + temp + "! Cancelling!");
        }
        m.setControls(parseMeshControls(rawKmf));

        //Sprites
        //KMSH/MESH/SPRS
        rawKmf.read(buf);
        temp = Utils.bytesToString(buf);
        if (!KMF_MESH_SPRITES.equals(temp)) {
            throw new RuntimeException("Header should be " + KMF_MESH_SPRITES + " and it was " + temp + "! Cancelling!");
        }
        m.setSprites(parseMeshSprites(rawKmf, sprsCount, lodCount));

        //Geoms
        //KMSH/MESH/GEOM
        rawKmf.read(buf);
        temp = Utils.bytesToString(buf);
        if (!KMF_MESH_GEOM.equals(temp)) {
            throw new RuntimeException("Header should be " + KMF_MESH_GEOM + " and it was " + temp + "! Cancelling!");
        }
        m.setGeometries(parseMeshGeoms(rawKmf, geomCount));

        return m;
    }

    /**
     * Parses the mesh control section<br>
     * KMSH/MESH/CTRL
     *
     * @param rawKmf kmf file starting on mesh
     */
    private List<MeshControl> parseMeshControls(RandomAccessFile rawKmf) throws IOException {
        rawKmf.skipBytes(4);

        int controlCount = Utils.readUnsignedInteger(rawKmf);
        List<MeshControl> controls = new ArrayList<>(controlCount);

        //Read the controls
        for (int i = 0; i < controlCount; i++) {
            MeshControl control = new MeshControl();
            control.setUnknown1(Utils.readUnsignedInteger(rawKmf));
            control.setUnknown2(Utils.readUnsignedInteger(rawKmf));
            controls.add(control);
        }

        return controls;
    }

    /**
     * Parses the mesh sprites section<br>
     * KMSH/MESH/SPRS
     *
     * @param rawKmf kmf file starting on sprite
     */
    private List<MeshSprite> parseMeshSprites(RandomAccessFile rawKmf, int sprsCount, int lodCount) throws IOException {
        rawKmf.skipBytes(4);
        List<MeshSprite> sprites = new ArrayList<>(sprsCount);

        //Headers
        for (int i = 0; i < sprsCount; i++) {

            //Sprite headers
            //KMSH/MESH/SPRS/SPHD
            byte[] buf = new byte[4];
            rawKmf.read(buf);
            String temp = Utils.bytesToString(buf);
            if (!KMF_MESH_SPRITES_HEADER.equals(temp)) {
                throw new RuntimeException("Header should be " + KMF_MESH_SPRITES_HEADER + " and it was " + temp + "! Cancelling!");
            }
            rawKmf.skipBytes(4);

            //Create new sprite
            MeshSprite sprite = new MeshSprite();
            List<Integer> triangleCounts = new ArrayList<>(lodCount);
            for (int j = 0; j < lodCount; j++) {
                triangleCounts.add(Utils.readUnsignedInteger(rawKmf));
            }
            sprite.setTriangleCounts(triangleCounts);
            sprite.setVerticeCount(Utils.readUnsignedInteger(rawKmf));
            sprite.setMmFactor(Utils.readFloat(rawKmf));
            sprites.add(sprite);
        }

        //Sprite data
        for (int i = 0; i < sprsCount; i++) {

            //Sprite data
            //KMSH/MESH/SPRS/SPRS
            byte[] buf = new byte[4];
            rawKmf.read(buf);
            String temp = Utils.bytesToString(buf);
            if (!KMF_MESH_SPRITES_DATA_HEADER.equals(temp)) {
                throw new RuntimeException("Header should be " + KMF_MESH_SPRITES_DATA_HEADER + " and it was " + temp + "! Cancelling!");
            }
            rawKmf.skipBytes(4);

            MeshSprite sprite = sprites.get(i);
            sprite.setMaterialIndex(Utils.readUnsignedInteger(rawKmf));

            //The triangles, for each lod level
            HashMap<Integer, List<Triangle>> trianglesPerLod = new HashMap<>(lodCount);
            for (int j = 0; j < lodCount; j++) {
                List<Triangle> triangles = new ArrayList<>(sprite.getTriangleCounts().get(j));
                for (int k = 0; k < sprite.getTriangleCounts().get(j); k++) {
                    triangles.add(new Triangle(Utils.toUnsignedByte(rawKmf.readByte()), Utils.toUnsignedByte(rawKmf.readByte()), Utils.toUnsignedByte(rawKmf.readByte())));
                }
                trianglesPerLod.put(j, triangles);
            }
            sprite.setTriangles(trianglesPerLod);

            //Mesh vertices
            List<MeshVertex> vertices = new ArrayList<>(sprite.getVerticeCount());
            for (int j = 0; j < sprite.getVerticeCount(); j++) {
                MeshVertex meshVertex = new MeshVertex();
                meshVertex.setGeomIndex(Utils.readUnsignedShort(rawKmf));
                meshVertex.setUv(new Uv(Utils.readUnsignedShort(rawKmf), Utils.readUnsignedShort(rawKmf)));
                meshVertex.setNormal(new Vector3f(Utils.readFloat(rawKmf), Utils.readFloat(rawKmf), Utils.readFloat(rawKmf)));
                vertices.add(meshVertex);
            }
            sprite.setVertices(vertices);
        }

        return sprites;
    }

    /**
     * Parses the mesh geometries section<br>
     * KMSH/MESH/GEOM
     *
     * @param rawKmf kmf file starting on geom
     */
    private List<Vector3f> parseMeshGeoms(RandomAccessFile rawKmf, int geomCount) throws IOException {
        rawKmf.skipBytes(4);
        List<Vector3f> geometries = new ArrayList<>(geomCount);

        //Geometries
        for (int i = 0; i < geomCount; i++) {
            geometries.add(new Vector3f(Utils.readFloat(rawKmf), Utils.readFloat(rawKmf), Utils.readFloat(rawKmf)));
        }

        return geometries;
    }

    public int getVersion() {
        return version;
    }

    public Type getType() {
        return type;
    }

    public List<Material> getMaterials() {
        return materials;
    }

    public List<Mesh> getMeshes() {
        return meshes;
    }
}
