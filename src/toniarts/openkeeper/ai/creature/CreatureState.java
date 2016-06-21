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
package toniarts.openkeeper.ai.creature;

import com.badlogic.gdx.ai.fsm.State;
import com.badlogic.gdx.ai.msg.Telegram;
import toniarts.openkeeper.world.creature.CreatureControl;

/**
 * State machine for creature AI. TODO: needs to be hierarchial so that this
 * class doesn't grow to be millions of lines
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public enum CreatureState implements State<CreatureControl> {

    IDLE() {

                @Override
                public void enter(CreatureControl entity) {

                    // Idling is the last resort
                    if (!findStuffToDo(entity)) {
                        entity.idle();
                    }
                }

                private boolean findStuffToDo(CreatureControl entity) {

                    // Find work
                    if (entity.isWorker() && entity.findWork()) {
                        entity.getStateMachine().changeState(CreatureState.WORK);
                        return true; // Found work
                    }

                    // See basic needs
                    if (entity.needsLair() && !entity.hasLair() && entity.findLair()) {
                        entity.getStateMachine().changeState(CreatureState.WORK);
                        return true; // Found work
                    }

                    return false;
                }

                @Override
                public void update(CreatureControl entity) {
                    findStuffToDo(entity);

//                    if (entity.idleTimeExceeded()) {
//                        entity.getStateMachine().changeState(WANDER);
//                    }
                }

                @Override
                public void exit(CreatureControl entity) {

                }

                @Override
                public boolean onMessage(CreatureControl entity, Telegram telegram) {
                    return true;
                }
            },
    WANDER() {

                @Override
                public void enter(CreatureControl entity) {
                    entity.wander();
                }

                @Override
                public void update(CreatureControl entity) {
//                    if (entity.idleTimeExceeded()) {
//                        entity.getStateMachine().changeState(IDLE);
//                    }
                }

                @Override
                public void exit(CreatureControl entity) {

                }

                @Override
                public boolean onMessage(CreatureControl entity, Telegram telegram) {
                    return true;
                }

            },
    DEAD() {
                @Override
                public void enter(CreatureControl entity) {
                    entity.die();
                }

                @Override
                public void update(CreatureControl entity) {
//                    if (entity.idleTimeExceeded()) {
//                        entity.getStateMachine().changeState(IDLE);
//                    }
                }

                @Override
                public void exit(CreatureControl entity) {

                }

                @Override
                public boolean onMessage(CreatureControl entity, Telegram telegram) {
                    return true;
                }
            }, SLAPPED {

                @Override
                public void enter(CreatureControl entity) {

                }

                @Override
                public void update(CreatureControl entity) {

                }

                @Override
                public void exit(CreatureControl entity) {

                }

                @Override
                public boolean onMessage(CreatureControl entity, Telegram telegram) {
                    return true;
                }
            }, WORK {

                @Override
                public void enter(CreatureControl entity) {
                    entity.navigateToAssignedTask();
                }

                @Override
                public void update(CreatureControl entity) {

                    // Check arrival
                    if (entity.isAtAssignedTaskTarget()) {

                        // If we have too much gold, drop it to the treasury
                        if (entity.isTooMuchGold()) {
                            if (!entity.dropGoldToTreasury()) {
                                entity.dropGold();
                            }
                        }
                    }

                    // Check validity
                    // If we have some pocket money left, we should return it to treasury
                    if (!entity.isAssignedTaskValid() && !entity.dropGoldToTreasury()) {
                        entity.getStateMachine().changeState(IDLE);
                    }
                }

                @Override
                public void exit(CreatureControl entity) {

                }

                @Override
                public boolean onMessage(CreatureControl entity, Telegram telegram) {
                    return true;
                }

            }, FIGHT {

                @Override
                public void enter(CreatureControl entity) {

                }

                @Override
                public void update(CreatureControl entity) {

                }

                @Override
                public void exit(CreatureControl entity) {

                }

                @Override
                public boolean onMessage(CreatureControl entity, Telegram telegram) {
                    return true;
                }

            }

}
