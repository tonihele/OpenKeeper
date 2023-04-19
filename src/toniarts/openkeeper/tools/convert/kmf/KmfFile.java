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
package toniarts.openkeeper.tools.convert.kmf;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.vecmath.Vector3f;
import toniarts.openkeeper.tools.convert.BufferedResourceReader;
import toniarts.openkeeper.tools.convert.ByteArrayResourceReader;
import toniarts.openkeeper.tools.convert.IResourceChunkReader;
import toniarts.openkeeper.tools.convert.IResourceReader;

/**
 * Reads Dungeon Keeper II model file to a data structure<br>
 * The file is LITTLE ENDIAN I might say<br>
 * Uses the Dungeon Keeper 2 File Format Guide by George Gensure
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public final class KmfFile {

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
    private Mesh mesh;
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

    public KmfFile(Path file) {

        // Read the file
        try (IResourceReader rawKmf = new BufferedResourceReader(file)) {
            parseKmfFile(rawKmf);
        } catch (IOException e) {

            // Fug
            throw new RuntimeException("Failed to open the file " + file + "!", e);
        }
    }
    
    public KmfFile(InputStream inputStream) {

        // Read the file
        try (IResourceReader rawKmf = new BufferedResourceReader(inputStream)) {
            parseKmfFile(rawKmf);
        } catch (IOException e) {

            // Fug
            throw new RuntimeException("Failed to handle the stream!", e);
        }
    }

    public KmfFile(byte[] data) {

        // Read the file
        try (IResourceReader rawKmf = new ByteArrayResourceReader(data)) {
            parseKmfFile(rawKmf);
        } catch (Exception e) {

            // Fug
            throw new RuntimeException("Failed to parse KMF data!", e);
        }
    }

    private void parseKmfFile(final IResourceReader rawKmf) throws RuntimeException, IOException {
        IResourceChunkReader rawKmfReader = rawKmf.readChunk(28);

        // Read the identifier
        checkHeader(rawKmfReader, KMF_HEADER_IDENTIFIER);
        rawKmfReader.skipBytes(4); // skip section size
        version = rawKmfReader.readUnsignedInteger();

        // KMSH/HEAD
        checkHeader(rawKmfReader, KMF_HEAD);
        parseHead(rawKmfReader);

        rawKmfReader = rawKmf.readAll();

        // KMSH/MATL
        if (type != Type.GROP) {
            checkHeader(rawKmfReader, KMF_MATERIALS);
            parseMatl(rawKmfReader);
        }

        // KMSH/MESH, there are n amount of these
        String fourCC = rawKmfReader.readString(4);
        if (KMF_MESH.equals(fourCC))
            mesh = parseMesh(rawKmfReader);

        // KMSH/ANIM
        if (type == Type.ANIM && KMF_ANIM.equals(fourCC)) {
            anim = parseAnim(rawKmfReader);
        }

        // KMSH/GROP
        if (type == Type.GROP && KMF_GROP.equals(fourCC)) {
            grops = parseGrop(rawKmfReader);
        }
    }

    /**
     * Parses the head section<br>
     * KMSH/HEAD
     *
     * @param rawKmf kmf file starting on HEAD
     */
    private void parseHead(IResourceChunkReader rawKmf) throws IOException {
        rawKmf.skipBytes(4); // skip section size
        this.type = Type.toType(rawKmf.readUnsignedInteger());
        int unknown = rawKmf.readUnsignedInteger();
    }

    /**
     * Parses the materials section<br>
     * KMSH/MATL
     *
     * @param rawKmf kmf file starting on MATL
     */
    private void parseMatl(IResourceChunkReader rawKmf) throws IOException {
        rawKmf.skipBytes(4); // skip section size
        int materialsCount = rawKmf.readUnsignedInteger();

        // Read the materials
        materials = new ArrayList<>(materialsCount);
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
    private Material parseMat2(IResourceChunkReader rawKmf) throws IOException {
        rawKmf.skipBytes(4); // skip section size

        // Create the material
        Material m = new Material();

        // Now we should have the name
        m.setName(rawKmf.readVaryingLengthStrings(1).get(0));

        // Textures
        int texturesCount = rawKmf.readUnsignedInteger();
        m.setTextures(rawKmf.readVaryingLengthStrings(texturesCount));

        m.setFlag(rawKmf.readIntegerAsFlag(Material.MaterialFlag.class));
        m.setBrightness(rawKmf.readFloat());
        m.setGamma(rawKmf.readFloat());

        // Environment map
        m.setEnvironmentMappingTexture(rawKmf.readVaryingLengthStrings(1).get(0));

        return m;
    }

    /**
     * Parses the mesh section<br>
     * KMSH/MESH
     *
     * @param rawKmf kmf file starting on mesh
     */
    private Mesh parseMesh(IResourceChunkReader rawKmf) throws IOException {
        rawKmf.skipBytes(4); // skip section size

        //KMSH/MESH/HEAD
        checkHeader(rawKmf, KMF_HEAD);
        rawKmf.skipBytes(4); // skip section size

        //Create the mesh
        Mesh m = new Mesh();

        //Now we should have the name
        m.setName(rawKmf.readVaryingLengthStrings(1).get(0));

        int sprsCount = rawKmf.readUnsignedInteger();
        int geomCount = rawKmf.readUnsignedInteger();
        m.setPos(rawKmf.readFloat(), rawKmf.readFloat(), rawKmf.readFloat());
        m.setScale(rawKmf.readFloat());
        int lodCount = rawKmf.readUnsignedInteger();

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
    private List<MeshControl> parseMeshControls(IResourceChunkReader rawKmf) throws IOException {
        rawKmf.skipBytes(4); // skip section size

        int controlCount = rawKmf.readUnsignedInteger();
        List<MeshControl> controls = new ArrayList<>(controlCount);

        //Read the controls
        for (int i = 0; i < controlCount; i++) {
            MeshControl control = new MeshControl();
            control.setUnknown1(rawKmf.readUnsignedInteger());
            control.setUnknown2(rawKmf.readUnsignedInteger());
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
    private List<MeshSprite> parseMeshSprites(IResourceChunkReader rawKmf, int sprsCount, int lodCount) throws IOException {
        rawKmf.skipBytes(4);
        List<MeshSprite> sprites = new ArrayList<>(sprsCount);

        //Headers
        for (int i = 0; i < sprsCount; i++) {

            //Sprite headers
            //KMSH/MESH/SPRS/SPHD
            checkHeader(rawKmf, KMF_MESH_SPRITES_HEADER);
            rawKmf.skipBytes(4); // skip section size

            //Create new sprite
            MeshSprite sprite = new MeshSprite();
            List<Integer> triangleCounts = new ArrayList<>(lodCount);
            for (int j = 0; j < lodCount; j++) {
                triangleCounts.add(rawKmf.readUnsignedInteger());
            }
            sprite.setTriangleCounts(triangleCounts);
            sprite.setVerticeCount(rawKmf.readUnsignedInteger());
            sprite.setMmFactor(rawKmf.readFloat());
            sprites.add(sprite);
        }

        //Sprite data
        for (int i = 0; i < sprsCount; i++) {

            //Sprite data
            //KMSH/MESH/SPRS/SPRS
            checkHeader(rawKmf, KMF_MESH_SPRITES_DATA_HEADER);
            rawKmf.skipBytes(4); // skip section size

            MeshSprite sprite = sprites.get(i);
            sprite.setMaterialIndex(rawKmf.readUnsignedInteger());

            //The triangles, for each lod level
            List<List<Triangle>> trianglesPerLod = new ArrayList<>(lodCount);
            for (int j = 0; j < lodCount; j++) {
                int numTris = sprite.getTriangleCounts().get(j);
                var triangles = new Triangle[numTris];
                for (int k = 0; k < numTris; ++k) {
                    triangles[k] = new Triangle(rawKmf.readUnsignedByte(),
                            rawKmf.readUnsignedByte(),
                            rawKmf.readUnsignedByte());
                }
                trianglesPerLod.add(Arrays.asList(triangles));
            }

            sprite.setTriangleCounts(null); // not needed anymore
            sprite.setTriangles(trianglesPerLod);

            //Mesh vertices
            List<MeshVertex> vertices = new ArrayList<>(sprite.getVerticeCount());
            for (int j = 0; j < sprite.getVerticeCount(); j++) {
                MeshVertex meshVertex = new MeshVertex();
                meshVertex.setGeomIndex(rawKmf.readUnsignedShort());
                meshVertex.setUv(new Uv(rawKmf.readUnsignedShort(),
                        rawKmf.readUnsignedShort()));
                meshVertex.setNormal(rawKmf.readFloat(),
                        rawKmf.readFloat(),
                        rawKmf.readFloat());
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
    private List<Vector3f> parseMeshGeoms(IResourceChunkReader rawKmf, int geomCount) throws IOException {
        rawKmf.skipBytes(4); // skip section size
        List<Vector3f> geometries = new ArrayList<>(geomCount);

        //Geometries
        for (int i = 0; i < geomCount; i++) {
            geometries.add(new Vector3f(rawKmf.readFloat(),
                    rawKmf.readFloat(),
                    rawKmf.readFloat()));
        }

        return geometries;
    }

    /**
     * Parses the anim section<br>
     * KMSH/ANIM
     *
     * @param rawKmf kmf file starting on ANIM
     */
    private Anim parseAnim(IResourceChunkReader rawKmf) throws IOException {
        rawKmf.skipBytes(4); // skip section size

        //KMSH/ANIM/HEAD
        checkHeader(rawKmf, KMF_HEAD);
        rawKmf.skipBytes(4); // skip section size

        //Create the anim
        Anim a = new Anim();

        //Now we should have the name
        a.setName(rawKmf.readVaryingLengthStrings(1).get(0));

        int sprsCount = rawKmf.readUnsignedInteger();
        int frameCount = rawKmf.readUnsignedInteger();
        int indexCount = rawKmf.readUnsignedInteger();
        int geomCount = rawKmf.readUnsignedInteger();
        a.setFrameFactorFunction(Anim.FrameFactorFunction.toFrameFactorFunction(rawKmf.readUnsignedInteger()));
        a.setPos(rawKmf.readFloat(), rawKmf.readFloat(), rawKmf.readFloat());
        a.setCubeScale(rawKmf.readFloat());
        a.setScale(rawKmf.readFloat());
        int lodCount = rawKmf.readUnsignedInteger();
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
        rawKmf.skipBytes(4); // skip section size
        int chunks = (int) Math.floor((frameCount - 1) / 128.0 + 1);
        int[][] itab = new int[chunks][indexCount];
        for (int chunk = 0; chunk < chunks; chunk++) {
            for (int i = 0; i < indexCount; i++) {
                itab[chunk][i] = rawKmf.readUnsignedInteger();
            }
        }
        a.setItab(itab);

        //Sprite geometries
        //KMSH/ANIM/SPRS/GEOM
        checkHeader(rawKmf, KMF_MESH_GEOM);
        rawKmf.skipBytes(4); // skip section size
        List<AnimGeom> geometries = new ArrayList<>(geomCount + 1);
        AnimGeom geom = null;
        for (int i = 0; i < geomCount; i++) {

            //10 bits, BITS, yes BITS, per coordinate (Z, Y, X) = 30 bits (2 last bits can be thrown away)
            // ^ so read 4 bytes
            // + 1 byte for frame base
            int coordinates = rawKmf.readUnsignedInteger();
            geom = new AnimGeom();

            float x = (((coordinates >> 20) & 0x3ff) - 0x200) / 511.0f;
            float y = (((coordinates >> 10) & 0x3ff) - 0x200) / 511.0f;
            float z = (((coordinates >> 0) & 0x3ff) - 0x200) / 511.0f;

            Vector3f v = new Vector3f(x, y, z);
            v.scale(a.getScale()); // Scale
            geom.setGeometry(v);

            geom.setFrameBase(rawKmf.readUnsignedByte());
            geometries.add(geom);
        }

        // For interpolation purposes add the last one again
        geometries.add(geom);

        a.setGeometries(geometries);

        //Sprite offsets
        //KMSH/ANIM/SPRS/VGEO
        checkHeader(rawKmf, KMF_ANIM_SPRITES_VGEO_HEADER);
        rawKmf.skipBytes(4); // skip section size
        short[][] offsets = new short[indexCount][frameCount];
        for (int i = 0; i < indexCount; i++) {
            for (int frame = 0; frame < frameCount; frame++) {
                offsets[i][frame] = rawKmf.readUnsignedByte();
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
    private List<AnimControl> parseAnimControls(IResourceChunkReader rawKmf) throws IOException {
        rawKmf.skipBytes(4); // skip section size

        int controlCount = rawKmf.readUnsignedInteger();
        List<AnimControl> controls = new ArrayList<>(controlCount);

        //Read the controls
        for (int i = 0; i < controlCount; i++) {
            AnimControl control = new AnimControl();
            control.setUnknown1(rawKmf.readUnsignedShort());
            control.setUnknown2(rawKmf.readUnsignedShort());
            control.setUnknown3(rawKmf.readUnsignedInteger());
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
    private List<AnimSprite> parseAnimSprites(IResourceChunkReader rawKmf, int sprsCount, int lodCount) throws IOException {
        rawKmf.skipBytes(4); // skip section size
        List<AnimSprite> sprites = new ArrayList<>(sprsCount);

        //Headers
        for (int i = 0; i < sprsCount; i++) {

            //Sprite headers
            //KMSH/ANIM/SPRS/SPHD
            checkHeader(rawKmf, KMF_MESH_SPRITES_HEADER);
            rawKmf.skipBytes(4); // skip section size

            //Create new sprite
            AnimSprite sprite = new AnimSprite();
            List<Integer> triangleCounts = new ArrayList<>(lodCount);
            for (int j = 0; j < lodCount; j++) {
                triangleCounts.add(rawKmf.readUnsignedInteger());
            }
            sprite.setTriangleCounts(triangleCounts);
            sprite.setVerticeCount(rawKmf.readUnsignedInteger());
            sprite.setMmFactor(rawKmf.readFloat());
            sprites.add(sprite);
        }

        //Sprite data
        for (int i = 0; i < sprsCount; i++) {

            //Sprite data
            //KMSH/ANIM/SPRS/SPRS
            checkHeader(rawKmf, KMF_MESH_SPRITES_DATA_HEADER);
            rawKmf.skipBytes(4); // skip section size

            AnimSprite sprite = sprites.get(i);
            sprite.setMaterialIndex(rawKmf.readUnsignedInteger());

            //The triangles, for each lod level
            //KMSH/ANIM/SPRS/SPRS/POLY
            checkHeader(rawKmf, KMF_ANIM_SPRITES_POLY_HEADER);
            rawKmf.skipBytes(4); // skip section size
            List<List<Triangle>> trianglesPerLod = new ArrayList<>(lodCount);
            for (int j = 0; j < lodCount; j++) {
                int numTris = sprite.getTriangleCounts().get(j);
                var triangles = new Triangle[numTris];
                for (int k = 0; k < numTris; ++k) {
                    triangles[k] = new Triangle(rawKmf.readUnsignedByte(),
                            rawKmf.readUnsignedByte(),
                            rawKmf.readUnsignedByte());
                }
                trianglesPerLod.add(Arrays.asList(triangles));
            }

            sprite.setTriangleCounts(null); // not needed anymore
            sprite.setTriangles(trianglesPerLod);

            //Anim vertices
            //KMSH/ANIM/SPRS/SPRS/VERT
            checkHeader(rawKmf, KMF_ANIM_SPRITES_VERT_HEADER);
            rawKmf.skipBytes(4); // skip section size
            List<AnimVertex> vertices = new ArrayList<>(sprite.getVerticeCount());
            for (int j = 0; j < sprite.getVerticeCount(); j++) {
                AnimVertex animVertex = new AnimVertex();
                animVertex.setUv(new Uv(rawKmf.readUnsignedShort(), rawKmf.readUnsignedShort()));
                animVertex.setNormal(rawKmf.readFloat(), rawKmf.readFloat(), rawKmf.readFloat());
                animVertex.setItabIndex(rawKmf.readUnsignedShort());
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
    private List<Grop> parseGrop(IResourceChunkReader rawKmf) throws IOException {
        rawKmf.skipBytes(4); // skip section size

        //KMSH/GROP/HEAD
        checkHeader(rawKmf, KMF_HEAD);
        rawKmf.skipBytes(4); // skip section size
        int elementCount = rawKmf.readUnsignedInteger();

        //Read the elements
        List<Grop> gs = new ArrayList<>();
        for (int i = 0; i < elementCount; i++) {

            //KMSH/GROP/ELEM
            checkHeader(rawKmf, KMF_GROP_ELEM);
            rawKmf.skipBytes(4); // skip section size

            //Read it
            Grop grop = new Grop();
            grop.setName(rawKmf.readVaryingLengthStrings(1).get(0));
            grop.setPos(rawKmf.readFloat(), rawKmf.readFloat(), rawKmf.readFloat());
            gs.add(grop);
        }

        return gs;
    }

    /**
     * Check the header. If the header is not the expected type, throw an exception
     *
     * @param expectedHeader header that is expected
     * @throws RuntimeException if the extracted header doesn't mach the expected header
     */
    private void checkHeader(IResourceChunkReader rawKmf, String expectedHeader) throws RuntimeException, IOException {
        String extractedHeader = rawKmf.readString(4);
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

    public Mesh getMesh() {
        return mesh;
    }

    public Anim getAnim() {
        return anim;
    }

    public List<Grop> getGrops() {
        return grops;
    }
}
