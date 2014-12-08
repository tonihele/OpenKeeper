/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package toniarts.opendungeonkeeper.tools.convert.kmf;

import java.util.List;
import java.util.Objects;

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

    @Override
    public String toString() {
        return name;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 41 * hash + Objects.hashCode(this.name);
        hash = 41 * hash + Objects.hashCode(this.textures);
        hash = 41 * hash + this.flag;
        hash = 41 * hash + Float.floatToIntBits(this.brightness);
        hash = 41 * hash + Float.floatToIntBits(this.gamma);
        hash = 41 * hash + Objects.hashCode(this.environmentMappingTexture);
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
        final Material other = (Material) obj;
        if (!Objects.equals(this.name, other.name)) {
            return false;
        }
        if (!Objects.equals(this.textures, other.textures)) {
            return false;
        }
        if (this.flag != other.flag) {
            return false;
        }
        if (Float.floatToIntBits(this.brightness) != Float.floatToIntBits(other.brightness)) {
            return false;
        }
        if (Float.floatToIntBits(this.gamma) != Float.floatToIntBits(other.gamma)) {
            return false;
        }
        if (!Objects.equals(this.environmentMappingTexture, other.environmentMappingTexture)) {
            return false;
        }
        return true;
    }
}
