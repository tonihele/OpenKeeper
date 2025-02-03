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
package toniarts.openkeeper.game.trigger;

import com.jme3.util.SafeArrayList;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import toniarts.openkeeper.tools.convert.map.TriggerGeneric;

/**
 *
 * @author ArchDemon
 */
public final class TriggerGenericData extends TriggerData {
    
    private static final Logger logger = System.getLogger(TriggerGenericData.class.getName());

    private TriggerGeneric.ComparisonType comparison; // Target comparison type
    private TriggerGeneric.TargetType target;
    private short repeatTimes; // Repeat x times, 255 = always
    private TriggerGenericData lastTrigger = null;
    private final SafeArrayList<TriggerData> children = new SafeArrayList<>(TriggerData.class);

    public TriggerGenericData() {
        super();
    }

    public TriggerGenericData(int id) {
        super(id);
    }

    public TriggerGenericData(int id, short repeatTimes) {
        super(id);
        this.repeatTimes = repeatTimes;
    }

    public TriggerGeneric.ComparisonType getComparison() {
        return comparison;
    }

    protected void setComparison(TriggerGeneric.ComparisonType comparison) {
        this.comparison = comparison;
    }

    public TriggerGeneric.TargetType getType() {
        return target;
    }

    protected void setType(TriggerGeneric.TargetType target) {
        this.target = target;
    }

    protected void setRepeatTimes(short repeatTimes) {
        this.repeatTimes = repeatTimes;
    }

    public void subRepeatTimes() {
        if (repeatTimes != 255 && repeatTimes > 0) {
            repeatTimes--;
        }
    }

    public boolean isRepeateable() {
        return (repeatTimes > 0);
    }

    public int getLastTriggerIndex() {
        return children.indexOf(lastTrigger);
    }

    public void setLastTrigger(TriggerGenericData lastTrigger) {
        this.lastTrigger = lastTrigger;
    }

    public int getQuantity() {
        return children.size();
    }

    /**
     * <code>getChild</code> returns the first child found with exactly the
     * given id (case sensitive.) This method does a depth first recursive
     * search of all descendants of this node, it will return the first spatial
     * found with a matching name.
     *
     * @param i the id of the child to retrieve. If null, we'll return null.
     * @return the child if found, or null.
     */
    public TriggerData getChild(int i) {
        return children.get(i);
    }

    /**
     * Determines if the provided TriggerActionData is contained in the children
     * map of this TriggerGenericData.
     *
     * @param trigger the child object to look for.
     * @return true if the object is contained, false otherwise.
     */
    public boolean hasChild(TriggerData trigger) {
        if (children.contains(trigger)) {
            return true;
        }

        for (TriggerData child : children.getArray()) {
            if (child instanceof TriggerGenericData && ((TriggerGenericData) child).hasChild(trigger)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Returns all children to this TriggerGenericData. Note that modifying that
     * given list is not allowed.
     *
     * @return a list containing all children to this node
     */
    public SafeArrayList<TriggerData> getChildren() {
        return children;
    }

    /**
     *
     * <code>attachChildAt</code> attaches a child to this node at an index.
     * This node becomes the child's parent. The current number of children
     * maintained is returned.
     * <br>
     * If the child already had a parent it is detached from that former parent.
     *
     * @param child the child to attach to this node.
     * @param index the index to add to
     * @return the number of children maintained by this node.
     * @throws NullPointerException if child is null.
     */
    public int attachChildAt(TriggerData child, int index) {
        if (child == null) {
            throw new IllegalArgumentException("child cannot be null");
        }

        if (child.getParent() != this && child != this) {
            if (child.getParent() != null) {
                child.getParent().detachChild(child);
            }
            child.setParent(this);
            children.add(index, child);

            if (logger.isLoggable(Level.DEBUG)) {
                logger.log(Level.DEBUG, "Child ({0}) attached to this trigger ({1})",
                        new Object[]{child.getId(), getId()});
            }
        }

        return children.size();
    }

    /**
     *
     * <code>attachChild</code> attaches a child to this TriggerGenericData at
     * an index. This node becomes the child's parent. The current number of
     * children maintained is returned.
     * <br>
     * If the child already had a parent it is detached from that former parent.
     *
     * @param child the child to attach to this TriggerGenericData.
     * @return the number of children maintained by this TriggerGenericData.
     * @throws NullPointerException if child is null.
     */
    public int attachChild(TriggerData child) {
        if (child == null) {
            throw new NullPointerException();
        }

        if (child.getParent() != this && child != this) {
            if (child.getParent() != null) {
                child.getParent().detachChild(child);
            }
            child.setParent(this);
            children.add(child);

            if (logger.isLoggable(Level.DEBUG)) {
                logger.log(Level.DEBUG, "Child ({0}) attached to this TriggerData ({1})",
                        new Object[]{child.getId(), getId()});
            }
        }

        return children.size();
    }

    /**
     *
     * <code>detachAllChildren</code> removes all children attached to this
     * TriggerGenericData.
     */
    public void detachAllChildren() {
        for (int i = children.size() - 1; i >= 0; i--) {
            detachChildAt(i);
        }
        logger.log(Level.DEBUG, "{0}: All children removed.", this.toString());
    }

    public int detachChild(TriggerData child) {
        if (child == null) {
            throw new NullPointerException();
        }

        if (child.getParent() == this) {
            int index = children.indexOf(child);
            if (index != -1) {
                detachChildAt(index);
            }
            return index;
        }

        return -1;
    }

    /**
     * <code>detachChild</code> removes a given child from the
     * TriggerGenericData's map. This child will no longer be maintained. Only
     * the first child with a matching id is removed.
     *
     * @param index the child to remove
     * @return the index the child was at. -1 if the child was not in the list.
     */
    public TriggerData detachChildAt(int index) {
        TriggerData child = children.remove(index);
        if (child != null) {
            child.setParent(null);
            logger.log(Level.DEBUG, "{0}: Child removed.", this.toString());
        }

        return child;
    }

    public int detachFromParent() {
        if (parent != null) {
            return parent.detachChild(this);
        }
        return -1;
    }

    /**
     * <code>getChildIndex</code> returns the index of the given TriggerData in
     * this TriggerGenericData's list of children.
     *
     * @param child The TriggerData to look up
     * @return The index of the TriggerData in the TriggerGenericData's
     * children, or -1 if the TriggerData is not attached to this node
     */
    public int getChildIndex(TriggerData child) {
        return children.indexOf(child);
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 29 * hash + this.id;
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final TriggerGenericData other = (TriggerGenericData) obj;
        if (this.id != other.id) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "TriggerGenericData { id=" + id + ", target=" + target + " }";
    }
}
