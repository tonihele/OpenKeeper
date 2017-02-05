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

            // Should we flee or attack
            if (entity.shouldFleeOrAttack()) {
                return;
            }

            // Idling is the last resort
            entity.unassingCurrentTask();
            if (!findStuffToDo(entity)) {
                entity.navigateToRandomPoint();
            }
        }

        private boolean findStuffToDo(CreatureControl entity) {

            // See if we should just follow
            if (entity.getParty() != null && !entity.getParty().isPartyLeader(entity)) {
                entity.setFollowTarget(entity.getParty().getPartyLeader());
                entity.getStateMachine().changeState(CreatureState.FOLLOW);
                return true;
            }

            // See if we have an objective
            if (entity.hasObjective() && entity.followObjective()) {
                entity.getStateMachine().changeState(CreatureState.WORK);
                return true;
            }

            // See lair need
            if (entity.needsLair() && !entity.hasLair() && entity.findLair()) {
                entity.getStateMachine().changeState(CreatureState.WORK);
                return true; // Found work
            }

            // See basic needs
            if (entity.hasLair() && (entity.isNeedForSleep() || entity.isHealthAtCriticalLevel()) && entity.goToSleep()) {
                entity.getStateMachine().changeState(CreatureState.WORK);
                return true; // Found work
            }

            // Find work
            if (entity.findWork() || (entity.isWorker() && entity.isTooMuchGold() && entity.dropGoldToTreasury())) {
                entity.getStateMachine().changeState(CreatureState.WORK);
                return true; // Found work
            }

            return false;
        }

        @Override
        public void update(CreatureControl entity) {

            // Should we flee or attack
            if (entity.shouldFleeOrAttack()) {
                return;
            }

            if (!findStuffToDo(entity) && entity.getIdleAnimationPlayCount() > 0 && entity.isStopped()) {
                entity.navigateToRandomPoint();
            }
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
//                    entity.wander();
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

            // Should we flee or attack
            if (entity.shouldFleeOrAttack()) {
                return;
            }

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
            entity.unassingCurrentTask();
            CreatureControl attackTarget = entity.getAttackTarget();
            if (attackTarget != null && !entity.isWithinAttackDistance(attackTarget)) {
                entity.navigateToAttackTarget(attackTarget);
            }
        }

        @Override
        public void update(CreatureControl entity) {
            CreatureControl attackTarget = entity.getAttackTarget();
            if (attackTarget == null) {
                entity.getStateMachine().changeState(IDLE); // Nothing to do
                return;
            }

            // If we have reached the target, stop and fight!
            if (entity.isWithinAttackDistance(attackTarget)) {

                // Attack!!
                entity.stop();
                entity.executeAttack(attackTarget);
            } else {
                entity.navigateToAttackTarget(attackTarget);
            }
        }

        @Override
        public void exit(CreatureControl entity) {

        }

        @Override
        public boolean onMessage(CreatureControl entity, Telegram telegram) {
            return true;
        }

    }, FOLLOW {

        @Override
        public void enter(CreatureControl entity) {
            entity.followTarget(entity.getFollowTarget());
        }

        @Override
        public void update(CreatureControl entity) {

            // See if we should follow
            if (entity.getFollowTarget() == null || entity.getFollowTarget().isIncapacitated()) {
                entity.getStateMachine().changeState(IDLE);
                return;
            }

            // Should we flee or attack
            if (entity.shouldFleeOrAttack()) {
                return;
            }

            // If leader has set a task, perform it
            if (entity.getAssignedTask() != null) {
                entity.getStateMachine().changeState(WORK);
                return;
            }

            // Don't let the target wander too far off
            if (entity.isStopped() && !entity.getFollowTarget().isStopped() && entity.getDistanceToCreature(entity.getFollowTarget()) > 2.5f) {
                entity.followTarget(entity);
            } else if (entity.isStopped()) {
                entity.navigateToRandomPointAroundTarget(entity.getFollowTarget(), 2);
            }
        }

        @Override
        public void exit(CreatureControl entity) {
            entity.resetFollowTarget();
            entity.stop();
        }

        @Override
        public boolean onMessage(CreatureControl entity, Telegram telegram) {
            return true;
        }

    },
    ENTERING_DUNGEON {

        @Override
        public void enter(CreatureControl entity) {

        }

        @Override
        public void update(CreatureControl entity) {
            entity.getStateMachine().changeState(IDLE);
        }

        @Override
        public void exit(CreatureControl entity) {

        }

        @Override
        public boolean onMessage(CreatureControl entity, Telegram telegram) {
            return true;
        }

    },
    PICKED_UP {

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

    }, FLEE {

        @Override
        public void enter(CreatureControl entity) {
            entity.unassingCurrentTask();
            entity.flee();
        }

        @Override
        public void update(CreatureControl entity) {
            if (!entity.shouldFleeOrAttack()) {
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

    }, UNCONSCIOUS {
        @Override
        public void enter(CreatureControl entity) {
            entity.stop();
            entity.unassingCurrentTask();
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

    }, STUNNED {

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
    }, IMPRISONED {

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
    }, TORTURED {

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
    }, SLEEPING {

        @Override
        public void enter(CreatureControl entity) {

        }

        @Override
        public void update(CreatureControl entity) {
            if (entity.isAttacked() || entity.isEnoughSleep()) {
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
    }, RECUPERATING {

        @Override
        public void enter(CreatureControl entity) {

        }

        @Override
        public void update(CreatureControl entity) {
            if (entity.isFullHealth()) {
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
    }, DRAGGED;

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
