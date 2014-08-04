/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package toniarts.opendungeonkeeper.tools.convert.kmf;

import java.util.List;

/**
 * KMF Material wrapper
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class Material {

    private String name;
    private List<String> textures;
    private int flag;
    private float brightness;
    private float gamma;
    private String environmentMappingTexture;

    public String getName() {
        return name;
    }

    protected void setName(String name) {
        this.name = name;
    }

    public List<String> getTextures() {
        return textures;
    }

    protected void setTextures(List<String> textures) {
        this.textures = textures;
    }

    public int getFlag() {
        return flag;
    }

    protected void setFlag(int flag) {
        this.flag = flag;
    }

    public float getBrightness() {
        return brightness;
    }

    protected void setBrightness(float brightness) {
        this.brightness = brightness;
    }

    public float getGamma() {
        return gamma;
    }

    protected void setGamma(float gamma) {
        this.gamma = gamma;
    }

    public String getEnvironmentMappingTexture() {
        return environmentMappingTexture;
    }

    protected void setEnvironmentMappingTexture(String environmentMappingTexture) {
        this.environmentMappingTexture = environmentMappingTexture;
    }
}
