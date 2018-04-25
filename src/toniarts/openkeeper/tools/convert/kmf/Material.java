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

import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import toniarts.openkeeper.tools.convert.IFlagEnum;

/**
 * KMF Material wrapper
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class Material {

    public enum MaterialFlag implements IFlagEnum {

        HAS_ALPHA(0x0001),
        UNKNOWN2(0x0002), // Some sort of shininess
        ALPHA_ADDITIVE(0x0004), // Also emits "light" / glows..?
        UNKNOWN4(0x0008),
        UNKNOWN5(0x0010),
        UNKNOWN6(0x0020),
        UNKNOWN7(0x0040),
        UNKNOWN8(0x0080), // Some sort of glow?
        UNKNOWN9(0x0100); // Environment mapped, invisible guys have this???
        private final long flagValue;

        private MaterialFlag(long flagValue) {
            this.flagValue = flagValue;
        }

        @Override
        public long getFlagValue() {
            return flagValue;
        }
    };
    private String name;
    private List<String> textures;
    private EnumSet<MaterialFlag> flag;
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

    public EnumSet<MaterialFlag> getFlag() {
        return flag;
    }

    protected void setFlag(EnumSet<MaterialFlag> flag) {
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
        int hash = 5;
        hash = 13 * hash + Objects.hashCode(this.name);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Material other = (Material) obj;
        if (Float.floatToIntBits(this.brightness) != Float.floatToIntBits(other.brightness)) {
            return false;
        }
        if (Float.floatToIntBits(this.gamma) != Float.floatToIntBits(other.gamma)) {
            return false;
        }
        if (!Objects.equals(this.name, other.name)) {
            return false;
        }
        if (!Objects.equals(this.environmentMappingTexture, other.environmentMappingTexture)) {
            return false;
        }
        if (!Objects.equals(this.textures, other.textures)) {
            return false;
        }
        if (!Objects.equals(this.flag, other.flag)) {
            return false;
        }
        return true;
    }

}
