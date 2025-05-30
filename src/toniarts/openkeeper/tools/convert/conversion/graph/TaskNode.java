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

import toniarts.openkeeper.tools.convert.conversion.IConversionTaskProvider;

/**
 * Graph node that holds task related data
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public final class TaskNode extends Node<TaskNode> {

    private final String name;
    private final IConversionTaskProvider task;
    private boolean executed = false;

    public TaskNode(int id, String name, IConversionTaskProvider task, boolean executed) {
        super(id);

        this.name = name;
        this.task = task;
        this.executed = executed;
    }

    public void executeTask() throws Exception {
        task.getTask().executeTask();

        executed = true;
    }

    public boolean isExecuted() {
        return executed;
    }

    @Override
    public String toString() {
        return "TaskNode{" + "name=" + name + ", executed=" + executed + '}';
    }

}
