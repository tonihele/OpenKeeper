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
package toniarts.openkeeper.tools.convert.map;

/**
 * Container class for *Variables.kld
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class Variable {

//    struct VariableBlock {
//        int x00;
//        int x04;
//        int x08;
//        int x0c;
//        };
    private int x00; // Some kind of ID, but there are duplicates
    private int value;
    private int x08;
    private int x0c;

    public int getX00() {
        return x00;
    }

    protected void setX00(int x00) {
        this.x00 = x00;
    }

    public int getValue() {
        return value;
    }

    protected void setValue(int value) {
        this.value = value;
    }

    public int getX08() {
        return x08;
    }

    protected void setX08(int x08) {
        this.x08 = x08;
    }

    public int getX0c() {
        return x0c;
    }

    protected void setX0c(int x0c) {
        this.x0c = x0c;
    }
}
