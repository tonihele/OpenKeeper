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
package toniarts.openkeeper.tools.convert.material;

import com.jme3.material.MatParam;

/**
 * Forked by Toni Helenius <helenius.toni@gmail.com>
 *
 * @author normenhansen
 */
public class MaterialProperty {

    private String type;
    private String name;
    private String value;

    public MaterialProperty() {
    }

    public MaterialProperty(String type, String name, String value) {
        this.type = type;
        this.name = name;
        this.value = value;
    }

    public MaterialProperty(MatParam param) {
        this.type = param.getVarType().name();
        this.name = param.getName();
        if (param.getValue() != null) {
            try {
                this.value = param.getValueAsString();
            } catch (UnsupportedOperationException e) {
            }
        }
    }

    /**
     * @return the type
     */
    public String getType() {
        return type;
    }

    /**
     * @param type the type to set
     */
    public void setType(String type) {
        this.type = type;
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @param name the name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * @return the value
     */
    public String getValue() {
        return value;
    }

    /**
     * @param value the value to set
     */
    public void setValue(String value) {
        this.value = value;
    }
}
