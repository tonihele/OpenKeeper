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

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import toniarts.openkeeper.tools.convert.map.TriggerGeneric;

/**
 *
 * @author ArchDemon
 */


public class TriggerGenericData extends TriggerData {

    private TriggerGeneric.ComparisonType comparison; // Target comparison type
    private TriggerGeneric.TargetType target;
    private short repeatTimes; // Repeat x times, 255 = always
    private LinkedHashMap<Integer, TriggerData> children = new LinkedHashMap<>();
    private static final Logger logger = Logger.getLogger(TriggerGenericData.class.getName());
    private Integer cycle = null;

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

    public boolean subRepeatTimes() {
        if (repeatTimes != 255 && repeatTimes > 0) {
            repeatTimes--;
        }
        return (repeatTimes > 0);
    }

    public int getQuantity() {
        return children.size();
    }

    /**
     * <code>getChild</code> returns the first child found with exactly the given id (case sensitive.) This method does
     * a depth first recursive search of all descendants of this node, it will return the first spatial found with a
     * matching name.
     *
     * @param id the id of the child to retrieve. If null, we'll return null.
     * @return the child if found, or null.
     */
    public <T> T getChild(int id) {

        if (children.containsKey(id)) {
            return (T) children.get(id);
        }

        for (TriggerData child : children.values()) {
            if (child instanceof TriggerGenericData) {
                TriggerData out = ((TriggerGenericData) child).getChild(id);
                if (out != null) {
                    return (T) out;
                }
            }
        }

        return null;
    }

    /**
     * determines if the provided TriggerActionData is contained in the children map of this TriggerGenericData.
     *
     * @param trigger the child object to look for.
     * @return true if the object is contained, false otherwise.
     */
    public boolean hasChild(TriggerData trigger) {
        return hasChildId(trigger.getId());
    }

    /**
     * determines if the provided ID is contained in the children map of this TriggerGenericData.
     *
     * @param id the child ID to look for.
     * @return true if the object is contained, false otherwise.
     */
    public boolean hasChildId(int id) {
        if (children.containsKey(id)) {
            return true;
        }

        for (TriggerData child : children.values()) {
            if (child instanceof TriggerGenericData && ((TriggerGenericData) child).hasChildId(id)) {
                return true;
            }
        }

        return false;
    }

    public boolean isCycleEnd() {
        boolean result = (Integer.valueOf(0).equals(cycle));

        if (cycle == null || cycle == 0) {
            cycle = 0;
            for (Map.Entry<Integer, TriggerData> entry : children.entrySet()) {
                TriggerData t = entry.getValue();

                if (t instanceof TriggerGenericData) {
                    cycle++;
                }
            }
        }

        return result;
    }

    /**
     * Returns all children to this TriggerGenericData. Note that modifying that given list is not allowed.
     *
     * @return a list containing all children to this node
     */
    public LinkedHashMap<Integer, TriggerData> getChildren() {
        return children;
    }

    public int attachChild(int id, TriggerData child) {
        if (child == null) {
            throw new IllegalArgumentException("child cannot be null");
        }

        if (child.getParent() != this && child != this) {
            if (child.getParent() != null) {
                child.getParent().detachChild(child);
            }
            child.setParent(this);
            children.put(id, child);

            if (logger.isLoggable(Level.FINE)) {
                logger.log(Level.FINE, "Child ({0}) attached to this trigger ({1})",
                        new Object[]{child.getId(), getId()});
            }
        }

        return children.size();
    }

    /**
     *
     * <code>attachChildId</code> attaches a child to this TriggerGenericData at an index. This node becomes the child's
     * parent. The current number of children maintained is returned.
     * <br>
     * If the child already had a parent it is detached from that former parent.
     *
     * @param child the child to attach to this TriggerGenericData.
     * @return the number of children maintained by this TriggerGenericData.
     * @throws NullPointerException if child is null.
     */
    public int attachChildId(int id, TriggerData child) {
        if (child == null) {
            throw new NullPointerException();
        }

        if (child.getParent() != this && child != this) {
            if (child.getParent() != null) {
                child.getParent().detachChild(child);
            }
            child.setParent(this);
            children.put(id, child);

            if (logger.isLoggable(Level.FINE)) {
                logger.log(Level.FINE, "Child ({0}) attached to this TriggerData ({1})",
                        new Object[]{child.getId(), getId()});
            }
        }

        return children.size();
    }

    /**
     *
     * <code>detachAllChildren</code> removes all children attached to this TriggerGenericData.
     */
    public void detachAllChildren() {
        for (int i : children.keySet()) {
            detachChildId(i);
        }
        logger.log(Level.FINE, "{0}: All children removed.", this.toString());
    }

    public int detachChild(TriggerData child) {
        if (child == null) {
            throw new NullPointerException();
        }

        if (child.getParent() == this) {
            if (children.containsValue(child)) {
                detachChildId(child.getId());
            }
            return child.getId();
        }

        return -1;
    }

    /**
     * <code>detachChild</code> removes a given child from the TriggerGenericData's map. This child will no longe be
     * maintained. Only the first child with a matching id is removed.
     *
     * @param id the child to remove.
     * @return the index the child was at. -1 if the child was not in the list.
     */
    public TriggerData detachChildId(int index) {
        TriggerData child = children.remove(index);
        if (child != null) {
            child.setParent(null);
            logger.log(Level.FINE, "{0}: Child removed.", this.toString());
        }

        return child;
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
}
