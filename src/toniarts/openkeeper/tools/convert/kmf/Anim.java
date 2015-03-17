/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package toniarts.openkeeper.tools.convert.kmf;

import java.util.List;
import javax.vecmath.Vector3f;

/**
 * KMF Anim wrapper
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class Anim {

    public enum FrameFactorFunction {

        CLAMP, WRAP;

        /**
         * Kmf anim head FrameFactorFunction value to enum
         *
         * @param index the type value
         * @return returns type
         */
        public static FrameFactorFunction toFrameFactorFunction(int index) {
            if (index == 0) {
                return CLAMP;
            }
            if (index == 1) {
                return WRAP;
            }
            throw new RuntimeException("FrameFactorFunction must be 0 - 1! Was " + index + "!");
        }
    }
    //HEAD
    private String name;
    private int frames;
    private int indexes;
    private Vector3f pos;
    private float scale;
    private float cubeScale;
    private FrameFactorFunction frameFactorFunction;
    //CTRL
    private List<AnimControl> controls;
    //SPRS
    private List<AnimSprite> sprites;
    //ITAB
    private int[][] itab;
    //GEOM
    private List<AnimGeom> geometries;
    //VGEO
    /*GEOM Offset for Frame*/
    private short[][] offsets;

    public String getName() {
        return name;
    }

    protected void setName(String name) {
        this.name = name;
    }

    public Vector3f getPos() {
        return pos;
    }

    protected void setPos(Vector3f pos) {
        this.pos = pos;
    }

    /**
     * GEOM/coord Scale Factor
     *
     * @return scale factor
     */
    public float getScale() {
        return scale;
    }

    protected void setScale(float scale) {
        this.scale = scale;
    }

    public List<AnimControl> getControls() {
        return controls;
    }

    protected void setControls(List<AnimControl> controls) {
        this.controls = controls;
    }

    public List<AnimSprite> getSprites() {
        return sprites;
    }

    protected void setSprites(List<AnimSprite> sprites) {
        this.sprites = sprites;
    }

    public List<AnimGeom> getGeometries() {
        return geometries;
    }

    protected void setGeometries(List<AnimGeom> geometries) {
        this.geometries = geometries;
    }

    /**
     * Unit cube scale for culling
     *
     * @return cube scale
     */
    public float getCubeScale() {
        return cubeScale;
    }

    protected void setCubeScale(float cubeScale) {
        this.cubeScale = cubeScale;
    }

    public FrameFactorFunction getFrameFactorFunction() {
        return frameFactorFunction;
    }

    protected void setFrameFactorFunction(FrameFactorFunction frameFactorFunction) {
        this.frameFactorFunction = frameFactorFunction;
    }

    public int[][] getItab() {
        return itab;
    }

    protected void setItab(int[][] itab) {
        this.itab = itab;
    }

    public short[][] getOffsets() {
        return offsets;
    }

    protected void setOffsets(short[][] offsets) {
        this.offsets = offsets;
    }

    public int getFrames() {
        return frames;
    }

    protected void setFrames(int frames) {
        this.frames = frames;
    }

    public int getIndexes() {
        return indexes;
    }

    protected void setIndexes(int indexes) {
        this.indexes = indexes;
    }
}
