/*
 * Copyright (C) 2014-2020 OpenKeeper
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
package toniarts.openkeeper.tools.convert.conversion.graph;

import java.util.Objects;
import toniarts.openkeeper.tools.convert.conversion.task.IConversionTask;

/**
 * Graph node that holds task related data
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class TaskNode extends Node<TaskNode> implements IConversionTask {

    private final String name;
    private final IConversionTask task;
    private boolean executed = false;

    public TaskNode(String name, IConversionTask task, boolean executed) {
        this.name = name;
        this.task = task;
        this.executed = executed;
    }

    @Override
    public void executeTask() {
        task.executeTask();

        executed = true;
    }

    public boolean isExecuted() {
        return executed;
    }

    @Override
    public String toString() {
        return "TaskNode{" + "name=" + name + ", executed=" + executed + '}';
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 29 * hash + Objects.hashCode(this.name);
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
        final TaskNode other = (TaskNode) obj;
        if (!Objects.equals(this.name, other.name)) {
            return false;
        }
        return true;
    }

}
