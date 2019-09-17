/*
 * Copyright (C) 2014-2016 OpenKeeper
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
package toniarts.openkeeper.game.data;

import com.jme3.export.InputCapsule;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.OutputCapsule;
import com.jme3.export.Savable;
import java.io.IOException;

/**
 * Player's spell (Keeper Spell)
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class PlayerSpell implements Savable {

    private short keeperSpellId;
    private boolean upgraded = false;
    private boolean discovered = false;
    private int research = 0;

    public PlayerSpell() {
    }

    public PlayerSpell(short keeperSpellId) {
        this(keeperSpellId, false);
    }

    public PlayerSpell(short keeperSpellId, boolean discovered) {
        this.keeperSpellId = keeperSpellId;
        this.discovered = discovered;
    }

    public short getKeeperSpellId() {
        return keeperSpellId;
    }

    public boolean isUpgraded() {
        return upgraded;
    }

    public void setUpgraded(boolean upgraded) {
        this.upgraded = upgraded;
    }

    public boolean isDiscovered() {
        return discovered;
    }

    public void setDiscovered(boolean discovered) {
        this.discovered = discovered;
    }

    public int getResearch() {
        return research;
    }

    public void setResearch(int research) {
        this.research = research;
    }

    @Override
    public String toString() {
        return "PlayerSpell{" + "keeperSpellId=" + keeperSpellId + ", upgraded=" + upgraded + ", discovered=" + discovered + ", research=" + research + '}';
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 23 * hash + this.keeperSpellId;
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
        final PlayerSpell other = (PlayerSpell) obj;
        if (this.keeperSpellId != other.keeperSpellId) {
            return false;
        }
        return true;
    }

    @Override
    public void write(JmeExporter ex) throws IOException {
        OutputCapsule out = ex.getCapsule(this);
        out.write(keeperSpellId, "keeperSpellId", Integer.valueOf(0).shortValue());
        out.write(upgraded, "upgraded", false);
        out.write(discovered, "discovered", false);
        out.write(research, "research", 0);
    }

    @Override
    public void read(JmeImporter im) throws IOException {
        InputCapsule in = im.getCapsule(this);
        keeperSpellId = in.readShort("keeperSpellId", keeperSpellId);
        upgraded = in.readBoolean("upgraded", upgraded);
        discovered = in.readBoolean("discovered", discovered);
        research = in.readInt("research", research);
    }

}
