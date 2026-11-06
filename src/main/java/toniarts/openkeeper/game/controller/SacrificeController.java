/*
 * Copyright (C) 2014-2026 OpenKeeper
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

import com.jme3.math.FastMath;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import toniarts.openkeeper.game.component.CreatureComponent;
import toniarts.openkeeper.game.component.CreatureExperience;
import toniarts.openkeeper.game.controller.room.SacrificeHistory;
import toniarts.openkeeper.game.controller.room.TempleController;
import toniarts.openkeeper.tools.convert.map.Variable;
import toniarts.openkeeper.tools.convert.map.Variable.SacrificeType;
import toniarts.openkeeper.utils.Point;
import toniarts.openkeeper.utils.WorldUtils;

/**
 * Sacrifices creatures dropped into a Temple's pool. The creature is gone for
 * good, and when the latest sacrifices match one of the level's recipes the
 * temple's owner gets the recipe's reward, like in DK II:
 * <ul>
 * <li>a creature, at the average level of the sacrificed creatures</li>
 * <li>an object, such as a trap crate or a special</li>
 * <li>gold or mana</li>
 * </ul>
 * Rewards appear on a random floor tile of the temple.
 */
public final class SacrificeController {

    private static final Logger logger = System.getLogger(SacrificeController.class.getName());

    private final EntityData entityData;
    private final List<Variable.Sacrifice> recipes;
    private final ICreaturesController creaturesController;
    private final IObjectsController objectsController;
    private final IGameWorldController gameWorldController;
    private final Map<Short, IPlayerController> playerControllers;

    public SacrificeController(EntityData entityData, List<Variable.Sacrifice> recipes,
            ICreaturesController creaturesController, IObjectsController objectsController,
            IGameWorldController gameWorldController, Map<Short, IPlayerController> playerControllers) {
        this.entityData = entityData;
        this.recipes = recipes;
        this.creaturesController = creaturesController;
        this.objectsController = objectsController;
        this.gameWorldController = gameWorldController;
        this.playerControllers = playerControllers;
    }

    /**
     * Sacrifices a creature in a temple
     *
     * @param temple the temple
     * @param creature the creature dropped into the pool
     * @return the matched recipe, or {@code null} if the sacrifice didn't
     * complete any
     */
    public SacrificeHistory.Match sacrificeCreature(TempleController temple, EntityId creature) {
        CreatureComponent creatureComponent = entityData.getComponent(creature, CreatureComponent.class);
        CreatureExperience experience = entityData.getComponent(creature, CreatureExperience.class);
        int level = experience != null ? experience.level : 1;

        creaturesController.createController(creature).remove();

        SacrificeHistory.Match match = temple.getSacrificeHistory().offer(SacrificeType.CREATURE, creatureComponent.creatureId, level, recipes);
        if (match != null) {
            reward(temple, match);
        }

        return match;
    }

    private void reward(TempleController temple, SacrificeHistory.Match match) {
        short ownerId = temple.getOwnerId();
        int value = match.recipe().getRewardValue();
        switch (match.recipe().getRewardType()) {
            case CREATURE -> {
                creaturesController.spawnCreature((short) value, ownerId, match.level(),
                        WorldUtils.pointToVector2f(getRewardLocation(temple)), ICreaturesController.SpawnType.PLACE);
            }
            case OBJECT -> {
                Point p = getRewardLocation(temple);
                objectsController.loadObject((short) value, ownerId, p.x, p.y);
            }
            case GOLD -> {
                gameWorldController.addGold(ownerId, value);
            }
            case MANA -> {
                IPlayerController playerController = playerControllers.get(ownerId);
                if (playerController != null && playerController.getManaControl() != null) {
                    playerController.getManaControl().addMana(value);
                }
            }
            default -> {

                // The original has no reward for these either
                logger.log(Level.DEBUG, "No sacrifice reward for {0}", match.recipe());
            }
        }
    }

    private static Point getRewardLocation(TempleController temple) {
        List<Point> floor = new ArrayList<>(temple.getRoomInstance().getCoordinates());
        floor.removeIf(temple::isSacrificePool);
        return floor.get(FastMath.rand.nextInt(floor.size()));
    }

}
