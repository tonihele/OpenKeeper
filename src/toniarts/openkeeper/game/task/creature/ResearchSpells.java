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
package toniarts.openkeeper.game.task.creature;

import com.jme3.math.Vector2f;
import toniarts.openkeeper.game.player.PlayerSpell;
import toniarts.openkeeper.game.player.PlayerSpellControl;
import toniarts.openkeeper.game.task.AbstractCapacityCriticalRoomTask;
import toniarts.openkeeper.game.task.TaskManager;
import toniarts.openkeeper.tools.convert.map.ArtResource;
import toniarts.openkeeper.world.WorldState;
import toniarts.openkeeper.world.creature.CreatureControl;
import toniarts.openkeeper.world.room.GenericRoom;

/**
 * Research spells for the keeper
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class ResearchSpells extends AbstractCapacityCriticalRoomTask {

    private final PlayerSpellControl spellControl;

    public ResearchSpells(WorldState worldState, int x, int y, short playerId, GenericRoom room, TaskManager taskManager) {
        super(worldState, x, y, playerId, room, taskManager);
        spellControl = worldState.getGameState().getPlayer(playerId).getSpellControl();
    }

    @Override
    public boolean isValid() {
        return (spellControl.isAnythingToReaseach() && !getRoom().isFullCapacity());
    }

    @Override
    public Vector2f getTarget(CreatureControl creature) {
        return new Vector2f(getTaskLocation().x + 0.5f, getTaskLocation().y + 0.5f);
    }

    @Override
    protected String getStringId() {
        return "2625";
    }

    @Override
    protected GenericRoom.ObjectType getRoomObjectType() {
        return GenericRoom.ObjectType.RESEARCHER;
    }

    @Override
    public void executeTask(CreatureControl creature) {

        // Advance players spell research
        PlayerSpell playerSpell = spellControl.research(creature.getCreature().getResearchPerSecond());
        if (playerSpell != null) {

            // Create a spell book
            getRoom().getObjectControl(GenericRoom.ObjectType.SPELL_BOOK).addItem(playerSpell, null, worldState.getThingLoader(), creature);
        }
    }

    @Override
    public ArtResource getTaskAnimation(CreatureControl creature) {
        return creature.getCreature().getAnimResearchResource();
    }

    @Override
    public String getTaskIcon() {
        return "Textures/GUI/moods/SJ-Library.png";
    }
}
