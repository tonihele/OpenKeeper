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
package toniarts.openkeeper.tools.convert.conversion.task;

import java.util.ArrayList;
import java.util.List;
import toniarts.openkeeper.tools.convert.AssetsConverter;

/**
 * Base class for conversion tasks
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public abstract class ConversionTask implements IConversionTask {

    protected final String dungeonKeeperFolder;
    protected final String destination;
    protected final boolean overwriteData;

    private final List<IConversionTaskUpdate> listeners = new ArrayList<>();

    public ConversionTask(String dungeonKeeperFolder, String destination, boolean overwriteData) {
        this.dungeonKeeperFolder = dungeonKeeperFolder;
        this.destination = destination;
        this.overwriteData = overwriteData;
    }

    public abstract AssetsConverter.ConvertProcess getConvertProcess();

    protected abstract void internalExecuteTask();

    @Override
    public final void executeTask() {
        try {
            internalExecuteTask();

            for (IConversionTaskUpdate listener : listeners) {
                listener.onComplete(getConvertProcess());
            }
        } catch (Exception e) {

            for (IConversionTaskUpdate listener : listeners) {
                listener.onError(e, getConvertProcess());
            }

            // Re-throw
            throw e;
        }
    }

    protected void updateStatus(Integer currentProgress, Integer totalProgress) {
        for (IConversionTaskUpdate listener : listeners) {
            listener.onUpdateStatus(currentProgress, totalProgress, getConvertProcess());
        }
    }

    @Override
    public void addListener(IConversionTaskUpdate listener) {
        listeners.add(listener);
    }

}
