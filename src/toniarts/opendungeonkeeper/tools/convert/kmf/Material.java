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
class Material {

    private String name;
    private List<String> textures;
    private int flag;
    private float brightness;
    private float gamma;
    private String environmentMappingTexture;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<String> getTextures() {
        return textures;
    }

    public void setTextures(List<String> textures) {
        this.textures = textures;
    }

    public int getFlag() {
        return flag;
    }

    public void setFlag(int flag) {
        this.flag = flag;
    }

    public float getBrightness() {
        return brightness;
    }

    public void setBrightness(float brightness) {
        this.brightness = brightness;
    }

    public float getGamma() {
        return gamma;
    }

    public void setGamma(float gamma) {
        this.gamma = gamma;
    }

    public String getEnvironmentMappingTexture() {
        return environmentMappingTexture;
    }

    public void setEnvironmentMappingTexture(String environmentMappingTexture) {
        this.environmentMappingTexture = environmentMappingTexture;
    }
}
