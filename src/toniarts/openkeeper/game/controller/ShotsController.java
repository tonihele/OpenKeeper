/*
 * Copyright (C) 2014-2024 OpenKeeper
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

import com.jme3.math.Vector3f;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import java.lang.System.Logger;
import java.util.Map;
import toniarts.openkeeper.game.controller.entity.EntityController;
import toniarts.openkeeper.tools.convert.map.KwdFile;
import toniarts.openkeeper.tools.convert.map.Shot;
import static toniarts.openkeeper.tools.convert.map.Shot.ProcessType.CREATE_CREATURE;
import static toniarts.openkeeper.tools.convert.map.Shot.ProcessType.CREATE_OBJECT;
import static toniarts.openkeeper.tools.convert.map.Shot.ProcessType.MODIFY_HEALTH;
import static toniarts.openkeeper.tools.convert.map.Shot.ProcessType.POSSESS_CREATURE;
import toniarts.openkeeper.tools.convert.map.Variable;
import toniarts.openkeeper.utils.WorldUtils;

/**
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class ShotsController implements IShotsController {

    private static final Logger logger = System.getLogger(ShotsController.class.getName());

    private final KwdFile kwdFile;
    private final EntityData entityData;
    private final Map<Variable.MiscVariable.MiscType, Variable.MiscVariable> gameSettings;
    private final IGameTimer gameTimer;
    private final IGameController gameController;
    private final IMapController mapController;
    private final ILevelInfo levelInfo;
    private final IObjectsController objectsController;
    private final ICreaturesController creaturesController;

    public ShotsController(KwdFile kwdFile, EntityData entityData, Map<Variable.MiscVariable.MiscType, Variable.MiscVariable> gameSettings, IGameTimer gameTimer,
            IGameController gameController, IMapController mapController, ILevelInfo levelInfo, IObjectsController objectsController, ICreaturesController creaturesController) {
        this.kwdFile = kwdFile;
        this.entityData = entityData;
        this.gameSettings = gameSettings;
        this.gameTimer = gameTimer;
        this.gameController = gameController;
        this.mapController = mapController;
        this.levelInfo = levelInfo;
        this.objectsController = objectsController;
        this.creaturesController = creaturesController;
    }

    @Override
    public void createShot(short shotTypeId, int shotData1, int shotData2, short playerId, Vector3f position, EntityId target) {
        Shot shot = kwdFile.getShotById(shotTypeId);
        switch (shot.getProcessType()) {
            case CREATE_CREATURE -> {
                creaturesController.spawnCreature((short) shotData1, playerId, shotData2, WorldUtils.vector3fToVector2f(position), ICreaturesController.SpawnType.CONJURE);
            }
            case CREATE_OBJECT -> {
                objectsController.loadObject((short) shotData1, playerId, position, 0);
            }
            case POSSESS_CREATURE -> {
                creaturesController.createController(target).setPossession(true);
            }
            case MODIFY_HEALTH -> {
                EntityController.setDamage(entityData, target, -shotData1);
            }
            default ->
                logger.log(Logger.Level.WARNING, "Shot type {0} not implemented", shot.getProcessType());
        }
    }

}
