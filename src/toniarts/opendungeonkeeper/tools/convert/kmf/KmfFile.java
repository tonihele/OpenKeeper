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
    private Anim anim;
    private List<Grop> grops;
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
    private static final String KMF_ANIM = "ANIM";
    private static final String KMF_ANIM_SPRITES_POLY_HEADER = "POLY";
    private static final String KMF_ANIM_SPRITES_VERT_HEADER = "VERT";
    private static final String KMF_ANIM_SPRITES_ITAB_HEADER = "ITAB";
    private static final String KMF_ANIM_SPRITES_VGEO_HEADER = "VGEO";
    private static final String KMF_GROP = "GROP";
    private static final String KMF_GROP_ELEM = "ELEM";

    public KmfFile(File file) {

        //Read the file
        try (RandomAccessFile rawKmf = new RandomAccessFile(file, "r")) {

            //Read the identifier
            checkHeader(rawKmf, KMF_HEADER_IDENTIFIER);
            rawKmf.skipBytes(4);
            version = Utils.readUnsignedInteger(rawKmf);

            //KMSH/HEAD
            checkHeader(rawKmf, KMF_HEAD);
            parseHead(rawKmf);

            //KMSH/MATL
            if (type != Type.GROP) {
                checkHeader(rawKmf, KMF_MATERIALS);
                parseMatl(rawKmf);
            }

            //KMSH/MESH, there are n amount of these
            meshes = new ArrayList();
            String temp = "";
            byte[] buf = new byte[4];
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

            //KMSH/ANIM
            if (type == Type.ANIM && KMF_ANIM.equals(temp)) {
                anim = parseAnim(rawKmf);
            }

            //KMSH/GROP
            if (type == Type.GROP && KMF_GROP.equals(temp)) {
                grops = parseGrop(rawKmf);
            }

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
        this.type = Type.toType(Utils.readUnsignedInteger(rawKmf));
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
            checkHeader(rawKmf, KMF_MATERIAL);
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
        checkHeader(rawKmf, KMF_HEAD);
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
        checkHeader(rawKmf, KMF_MESH_CONTROL);
        m.setControls(parseMeshControls(rawKmf));

        //Sprites
        //KMSH/MESH/SPRS
        checkHeader(rawKmf, KMF_MESH_SPRITES);
        m.setSprites(parseMeshSprites(rawKmf, sprsCount, lodCount));

        //Geoms
        //KMSH/MESH/GEOM
        checkHeader(rawKmf, KMF_MESH_GEOM);
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
            checkHeader(rawKmf, KMF_MESH_SPRITES_HEADER);
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
            checkHeader(rawKmf, KMF_MESH_SPRITES_DATA_HEADER);
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

    /**
     * Parses the anim section<br>
     * KMSH/ANIM
     *
     * @param rawKmf kmf file starting on ANIM
     */
    private Anim parseAnim(RandomAccessFile rawKmf) throws IOException {
        rawKmf.skipBytes(4);

        //KMSH/ANIM/HEAD
        checkHeader(rawKmf, KMF_HEAD);
        rawKmf.skipBytes(4);

        //Create the anim
        Anim a = new Anim();

        //Now we should have the name
        a.setName(Utils.readVaryingLengthStrings(rawKmf, 1).get(0));

        int sprsCount = Utils.readUnsignedInteger(rawKmf);
        int frameCount = Utils.readUnsignedInteger(rawKmf);
        int indexCount = Utils.readUnsignedInteger(rawKmf);
        int geomCount = Utils.readUnsignedInteger(rawKmf);
        a.setFrameFactorFunction(Anim.FrameFactorFunction.toFrameFactorFunction(Utils.readUnsignedInteger(rawKmf)));
        a.setPos(new Vector3f(Utils.readFloat(rawKmf), Utils.readFloat(rawKmf), Utils.readFloat(rawKmf)));
        a.setCubeScale(Utils.readFloat(rawKmf));
        a.setScale(Utils.readFloat(rawKmf));
        int lodCount = Utils.readUnsignedInteger(rawKmf);
        a.setFrames(frameCount);
        a.setIndexes(indexCount);

        //Controls
        //KMSH/ANIM/CTRL
        checkHeader(rawKmf, KMF_MESH_CONTROL);
        a.setControls(parseAnimControls(rawKmf));

        //Sprites
        //KMSH/ANIM/SPRS
        checkHeader(rawKmf, KMF_MESH_SPRITES);
        a.setSprites(parseAnimSprites(rawKmf, sprsCount, lodCount));

        //ITAB
        //KMSH/ANIM/SPRS/ITAB
        //indexCount sized chunks for each 128 frame block
        checkHeader(rawKmf, KMF_ANIM_SPRITES_ITAB_HEADER);
        rawKmf.skipBytes(4);
        int chunks = (int) Math.floor((frameCount - 1) / 128.0 + 1);
        int[][] itab = new int[indexCount][chunks];
        for (int chunk = 0; chunk < chunks; chunk++) {
            for (int index = 0; index < indexCount; index++) {
                itab[index][chunk] = Utils.readUnsignedInteger(rawKmf);
            }
        }
        a.setItab(itab);

        //Sprite geometries
        //KMSH/ANIM/SPRS/GEOM
        checkHeader(rawKmf, KMF_MESH_GEOM);
        rawKmf.skipBytes(4);
        List<AnimGeom> geometries = new ArrayList<>(geomCount);
        for (int i = 0; i < geomCount; i++) {
            //10 bits, BITS, yes BITS, per coordinate (Z, Y, X) = 30 bits (2 last bits can be thrown away)
            // ^ so read 4 bytes
            // + 1 byte for frame base
            byte[] bytes = new byte[4];
            rawKmf.read(bytes);
            int coordinates = Utils.readUnsignedInteger(bytes); //Read to an integer, the bit order is now reversed to "normal"??
            AnimGeom geom = new AnimGeom();

            //Divide by 1000, seems right scale... If the positions are f* up, here is the bug then...
            geom.setGeometry(new Vector3f(Utils.bits(coordinates, 0, 10) / 1000f * a.getScale(), Utils.bits(coordinates, 10, 10) / 1000f * a.getScale(), Utils.bits(coordinates, 20, 10) / 1000f * a.getScale()));

            geom.setFrameBase(Utils.toUnsignedByte(rawKmf.readByte()));
            geometries.add(geom);
        }
        a.setGeometries(geometries);

        //Sprite offsets
        //KMSH/ANIM/SPRS/VGEO
        checkHeader(rawKmf, KMF_ANIM_SPRITES_VGEO_HEADER);
        rawKmf.skipBytes(4);
        short[][] offsets = new short[frameCount][indexCount];
        for (int index = 0; index < indexCount; index++) {
            for (int frame = 0; frame < frameCount; frame++) {
                offsets[frame][index] = Utils.toUnsignedByte(rawKmf.readByte());
            }
        }
        a.setOffsets(offsets);

        return a;
    }

    /**
     * Parses the anim control section<br>
     * KMSH/ANIM/CTRL
     *
     * @param rawKmf kmf file starting on mesh
     */
    private List<AnimControl> parseAnimControls(RandomAccessFile rawKmf) throws IOException {
        rawKmf.skipBytes(4);

        int controlCount = Utils.readUnsignedInteger(rawKmf);
        List<AnimControl> controls = new ArrayList<>(controlCount);

        //Read the controls
        for (int i = 0; i < controlCount; i++) {
            AnimControl control = new AnimControl();
            control.setUnknown1(Utils.readUnsignedShort(rawKmf));
            control.setUnknown2(Utils.readUnsignedShort(rawKmf));
            control.setUnknown3(Utils.readUnsignedInteger(rawKmf));
            controls.add(control);
        }

        return controls;
    }

    /**
     * Parses the anim sprites section<br>
     * KMSH/ANIM/SPRS
     *
     * @param rawKmf kmf file starting on sprite
     */
    private List<AnimSprite> parseAnimSprites(RandomAccessFile rawKmf, int sprsCount, int lodCount) throws IOException {
        rawKmf.skipBytes(4);
        List<AnimSprite> sprites = new ArrayList<>(sprsCount);

        //Headers
        for (int i = 0; i < sprsCount; i++) {

            //Sprite headers
            //KMSH/ANIM/SPRS/SPHD
            checkHeader(rawKmf, KMF_MESH_SPRITES_HEADER);
            rawKmf.skipBytes(4);

            //Create new sprite
            AnimSprite sprite = new AnimSprite();
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
            //KMSH/ANIM/SPRS/SPRS
            checkHeader(rawKmf, KMF_MESH_SPRITES_DATA_HEADER);
            rawKmf.skipBytes(4);

            AnimSprite sprite = sprites.get(i);
            sprite.setMaterialIndex(Utils.readUnsignedInteger(rawKmf));

            //The triangles, for each lod level
            //KMSH/ANIM/SPRS/SPRS/POLY
            checkHeader(rawKmf, KMF_ANIM_SPRITES_POLY_HEADER);
            rawKmf.skipBytes(4);
            HashMap<Integer, List<Triangle>> trianglesPerLod = new HashMap<>(lodCount);
            for (int j = 0; j < lodCount; j++) {
                List<Triangle> triangles = new ArrayList<>(sprite.getTriangleCounts().get(j));
                for (int k = 0; k < sprite.getTriangleCounts().get(j); k++) {
                    triangles.add(new Triangle(Utils.toUnsignedByte(rawKmf.readByte()), Utils.toUnsignedByte(rawKmf.readByte()), Utils.toUnsignedByte(rawKmf.readByte())));
                }
                trianglesPerLod.put(j, triangles);
            }
            sprite.setTriangles(trianglesPerLod);

            //Anim vertices
            //KMSH/ANIM/SPRS/SPRS/VERT
            checkHeader(rawKmf, KMF_ANIM_SPRITES_VERT_HEADER);
            rawKmf.skipBytes(4);
            List<AnimVertex> vertices = new ArrayList<>(sprite.getVerticeCount());
            for (int j = 0; j < sprite.getVerticeCount(); j++) {
                AnimVertex animVertex = new AnimVertex();
                animVertex.setUv(new Uv(Utils.readUnsignedShort(rawKmf), Utils.readUnsignedShort(rawKmf)));
                animVertex.setNormal(new Vector3f(Utils.readFloat(rawKmf), Utils.readFloat(rawKmf), Utils.readFloat(rawKmf)));
                animVertex.setItabIndex(Utils.readUnsignedShort(rawKmf));
                vertices.add(animVertex);
            }
            sprite.setVertices(vertices);
        }

        return sprites;
    }

    /**
     * Parses the kmf GROP section<br>
     * KMSH/GROP
     *
     * @param rawKmf kmf file starting on grop
     */
    private List<Grop> parseGrop(RandomAccessFile rawKmf) throws IOException {
        rawKmf.skipBytes(4);

        //KMSH/GROP/HEAD
        checkHeader(rawKmf, KMF_HEAD);
        rawKmf.skipBytes(4);
        int elementCount = Utils.readUnsignedInteger(rawKmf);

        //Read the elements
        List<Grop> gs = new ArrayList<>();
        for (int i = 0; i < elementCount; i++) {

            //KMSH/GROP/ELEM
            checkHeader(rawKmf, KMF_GROP_ELEM);
            rawKmf.skipBytes(4);

            //Read it
            Grop grop = new Grop();
            grop.setName(Utils.readVaryingLengthStrings(rawKmf, 1).get(0));
            grop.setPos(new Vector3f(Utils.readFloat(rawKmf), Utils.readFloat(rawKmf), Utils.readFloat(rawKmf)));
            gs.add(grop);
        }

        return gs;
    }

    /**
     * Check the header. If the header is not the expected type, throw an
     * exception
     *
     * @param expectedHeader header that is expected
     * @throws RuntimeException if the extracted header doesn't mach the
     * expected header
     */
    private void checkHeader(RandomAccessFile rawKmf, String expectedHeader) throws RuntimeException, IOException {
        byte[] buf = new byte[4];
        rawKmf.read(buf);
        String extractedHeader = Utils.bytesToString(buf);
        if (!expectedHeader.equals(extractedHeader)) {
            throw new RuntimeException("Header should be " + expectedHeader + " and it was " + extractedHeader + "! Cancelling!");
        }
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

    public Anim getAnim() {
        return anim;
    }

    public List<Grop> getGrops() {
        return grops;
    }
}
