/*
 * Copyright (C) 2014-2017 OpenKeeper
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
package toniarts.openkeeper.game.controller;

import com.simsilica.es.EntityData;
import java.util.Map;
import toniarts.openkeeper.game.controller.player.PlayerCreatureControl;
import toniarts.openkeeper.game.controller.player.PlayerDoorControl;
import toniarts.openkeeper.game.controller.player.PlayerGoldControl;
import toniarts.openkeeper.game.controller.player.PlayerHandControl;
import toniarts.openkeeper.game.controller.player.PlayerManaControl;
import toniarts.openkeeper.game.controller.player.PlayerResearchControl;
import toniarts.openkeeper.game.controller.player.PlayerRoomControl;
import toniarts.openkeeper.game.controller.player.PlayerSpellControl;
import toniarts.openkeeper.game.controller.player.PlayerStatsControl;
import toniarts.openkeeper.game.controller.player.PlayerTrapControl;
import toniarts.openkeeper.game.data.Keeper;
import toniarts.openkeeper.game.listener.PlayerListener;
import toniarts.openkeeper.game.listener.PlayerResearchableEntityListener;
import toniarts.openkeeper.game.listener.PlayerSpellListener;
import toniarts.openkeeper.tools.convert.map.Creature;
import toniarts.openkeeper.tools.convert.map.IKwdFile;
import toniarts.openkeeper.tools.convert.map.Player;
import toniarts.openkeeper.tools.convert.map.Variable;

/**
 * Player controller, hosts and provides player related methods
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public final class PlayerController implements IPlayerController {

    private final Keeper keeper;
    private final PlayerGoldControl goldControl;
    private final PlayerCreatureControl creatureControl;
    private final PlayerRoomControl roomControl;
    private final PlayerSpellControl spellControl;
    private final PlayerManaControl manaControl;
    private final PlayerHandControl handControl;
    private final PlayerStatsControl statsControl;
    private final PlayerDoorControl doorControl;
    private final PlayerTrapControl trapControl;
    private final PlayerResearchControl researchControl;

    public PlayerController(IKwdFile kwdFile, Keeper keeper, Creature imp, EntityData entityData, Map<Variable.MiscVariable.MiscType, Variable.MiscVariable> gameSettings) {
        this.keeper = keeper;

        // Create the actual controllers
        goldControl = new PlayerGoldControl(keeper);
        creatureControl = new PlayerCreatureControl(keeper, imp, kwdFile.getCreatureList());
        roomControl = new PlayerRoomControl(keeper, kwdFile.getRooms());
        spellControl = new PlayerSpellControl(keeper, kwdFile.getKeeperSpells());
        statsControl = new PlayerStatsControl();
        doorControl = new PlayerDoorControl(keeper, kwdFile.getDoors());
        trapControl = new PlayerTrapControl(keeper, kwdFile.getTraps());

        // Don't create certain controls for neutral nor good player
        if (keeper.getId() != Player.GOOD_PLAYER_ID && keeper.getId() != Player.NEUTRAL_PLAYER_ID) {
            manaControl = new PlayerManaControl(keeper, gameSettings);
            handControl = new PlayerHandControl(keeper, (int) gameSettings.get(Variable.MiscVariable.MiscType.MAX_NUMBER_OF_THINGS_IN_HAND).getValue(), entityData);
            researchControl = new PlayerResearchControl(keeper, kwdFile);
        } else {
            manaControl = null;
            handControl = null;
            researchControl = null;
        }
    }

    @Override
    public Keeper getKeeper() {
        return keeper;
    }

    @Override
    public void addListener(PlayerListener listener) {
        goldControl.addListener(listener);
        if (manaControl != null) {
            manaControl.addListener(listener);
        }
        if (roomControl != null) {
            roomControl.addListener(listener);
        }
        if (spellControl != null) {
            spellControl.addListener(listener);
        }
        if (doorControl != null) {
            doorControl.addListener(listener);
        }
        if (trapControl != null) {
            trapControl.addListener(listener);
        }
        if (researchControl != null) {
            researchControl.addListener((PlayerResearchableEntityListener) listener);
            researchControl.addListener((PlayerSpellListener) listener);
        }
    }

    @Override
    public void removeListener(PlayerListener listener) {
        goldControl.removeListener(listener);
        if (manaControl != null) {
            manaControl.removeListener(listener);
        }
        if (roomControl != null) {
            roomControl.removeListener(listener);
        }
        if (spellControl != null) {
            spellControl.removeListener(listener);
        }
        if (doorControl != null) {
            doorControl.removeListener(listener);
        }
        if (trapControl != null) {
            trapControl.removeListener(listener);
        }
        if (researchControl != null) {
            researchControl.removeListener((PlayerResearchableEntityListener) listener);
            researchControl.removeListener((PlayerSpellListener) listener);
        }
    }

    @Override
    public PlayerGoldControl getGoldControl() {
        return goldControl;
    }

    @Override
    public PlayerManaControl getManaControl() {
        return manaControl;
    }

    @Override
    public PlayerSpellControl getSpellControl() {
        return spellControl;
    }

    @Override
    public PlayerCreatureControl getCreatureControl() {
        return creatureControl;
    }

    @Override
    public PlayerRoomControl getRoomControl() {
        return roomControl;
    }

    @Override
    public PlayerHandControl getHandControl() {
        return handControl;
    }

    @Override
    public PlayerStatsControl getStatsControl() {
        return statsControl;
    }

    @Override
    public PlayerDoorControl getDoorControl() {
        return doorControl;
    }

    @Override
    public PlayerTrapControl getTrapControl() {
        return trapControl;
    }

    @Override
    public PlayerResearchControl getResearchControl() {
        return researchControl;
    }

}
