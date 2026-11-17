package toniarts.openkeeper.game.data;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class EnhancedLiquidsSettingsTest {

    @Test
    void newSettingsDefaultToClassicLiquids() {
        assertEquals(Boolean.FALSE, Settings.Setting.ENHANCED_LIQUIDS.getDefaultValue());
    }
}
