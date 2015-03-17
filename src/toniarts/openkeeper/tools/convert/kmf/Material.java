/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package toniarts.openkeeper.tools.convert.kmf;

import java.util.Iterator;
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

    public boolean equalsIgnoreCase(List<String> l1, List<String> l2) {

        // Null check
        if (l1 == null && l2 == null) {
            return true;
        }
        if ((l1 == null && l2 != null) || (l2 == null && l1 != null)) {
            return false;
        }

        // Size check
        if (l1.size() != l2.size()) {
            return false;
        }

        // Go through the individual items
        Iterator<String> i1 = l1.iterator();
        Iterator<String> i2 = l2.iterator();
        while (i1.hasNext()) {
            if (!i1.next().equalsIgnoreCase(i2.next())) {
                return false;
            }
        }
        return true;
    }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 41 * hash + Objects.hashCode(this.name.toLowerCase());
        hash = 41 * hash + Objects.hashCode((this.textures != null && this.textures.size() > 0 ? this.textures.get(0).toLowerCase() : this.textures));
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
        if (!Objects.equals((this.name != null ? this.name.toLowerCase() : null), (other.name != null ? other.name.toLowerCase() : null))) {
            return false;
        }
        if (!equalsIgnoreCase(this.textures, other.textures)) {
            return false;
        }
        return true;
    }
}
