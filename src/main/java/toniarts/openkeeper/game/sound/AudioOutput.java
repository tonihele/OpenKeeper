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
package toniarts.openkeeper.game.sound;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.nio.IntBuffer;
import org.lwjgl.openal.ALC10;
import org.lwjgl.openal.SOFTHRTF;
import org.lwjgl.openal.SOFTOutputMode;
import org.lwjgl.system.MemoryStack;

/** Changes the existing OpenAL device without replacing contexts or playing sources. */
public final class AudioOutput {
    private static final Logger logger = System.getLogger(AudioOutput.class.getName());
    private static volatile Result status = new Result(true, "Automatic audio output");

    private AudioOutput() {
    }

    public record Result(boolean applied, String message) {
    }

    interface Device {
        boolean supportsHrtf();
        boolean reset(AudioOutputMode mode);
        boolean hrtfEnabled();
    }

    public static Result getStatus() {
        return status;
    }

    /** Call after jME has initialized its OpenAL renderer, or when the user applies settings. */
    public static Result apply(AudioOutputMode mode) {
        try {
            long context = ALC10.alcGetCurrentContext();
            long device = context == 0 ? 0 : ALC10.alcGetContextsDevice(context);
            status = apply(mode, new Device() {
                public boolean supportsHrtf() {
                    return device != 0 && ALC10.alcIsExtensionPresent(device, "ALC_SOFT_HRTF");
                }
                public boolean reset(AudioOutputMode selected) {
                    boolean outputMode = ALC10.alcIsExtensionPresent(device, "ALC_SOFT_output_mode");
                    try (MemoryStack stack = MemoryStack.stackPush()) {
                        IntBuffer attributes = stack.ints(attributes(selected, outputMode));
                        return SOFTHRTF.alcResetDeviceSOFT(device, attributes);
                    }
                }
                public boolean hrtfEnabled() {
                    return ALC10.alcGetInteger(device, SOFTHRTF.ALC_HRTF_SOFT) == ALC10.ALC_TRUE;
                }
            });
        } catch (IllegalStateException | UnsatisfiedLinkError ex) {
            logger.log(Level.WARNING, "Cannot configure spatial audio output", ex);
            status = unavailable(mode);
        }
        logger.log(status.applied() ? Level.INFO : Level.WARNING, status.message());
        return status;
    }

    static int[] attributes(AudioOutputMode mode, boolean outputMode) {
        int hrtf = switch (mode) {
            case AUTO -> SOFTHRTF.ALC_DONT_CARE_SOFT;
            case HEADPHONES -> ALC10.ALC_TRUE;
            case SPEAKERS -> ALC10.ALC_FALSE;
        };
        if (!outputMode) {
            return new int[]{SOFTHRTF.ALC_HRTF_SOFT, hrtf, 0};
        }
        // Returning to Auto/Speakers restores device-selected channel layout,
        // including surround, instead of retaining a forced stereo headphone mode.
        return new int[]{SOFTHRTF.ALC_HRTF_SOFT, hrtf, SOFTOutputMode.ALC_OUTPUT_MODE_SOFT,
            mode == AudioOutputMode.HEADPHONES ? SOFTOutputMode.ALC_STEREO_HRTF_SOFT
                    : SOFTOutputMode.ALC_ANY_SOFT, 0};
    }

    static Result apply(AudioOutputMode mode, Device device) {
        if (!device.supportsHrtf()) {
            return unavailable(mode);
        }
        if (!device.reset(mode)) {
            return new Result(false, "Audio output could not be changed; previous output remains active.");
        }
        boolean enabled = device.hrtfEnabled();
        if (mode == AudioOutputMode.HEADPHONES && !enabled) {
            return new Result(false, "Headphone spatial audio is unavailable on this device; standard audio remains active.");
        }
        if (mode == AudioOutputMode.SPEAKERS && enabled) {
            return new Result(false, "This device requires headphone spatial audio; it remains active.");
        }
        return new Result(true, enabled ? "Headphone spatial audio (HRTF) is active."
                : "Standard positional audio is active.");
    }

    private static Result unavailable(AudioOutputMode mode) {
        return new Result(mode == AudioOutputMode.AUTO,
                "Audio output selection is unavailable; using the device default.");
    }
}
