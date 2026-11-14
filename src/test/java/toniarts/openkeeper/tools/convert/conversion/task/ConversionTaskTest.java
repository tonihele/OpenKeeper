package toniarts.openkeeper.tools.convert.conversion.task;

import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import toniarts.openkeeper.tools.convert.AssetsConverter;
import toniarts.openkeeper.utils.PathUtils;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ConversionTaskTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    void normalizesTheSourceRootForTaskImplementations() {
        TestTask task = new TestTask(temporaryDirectory.toString());
        assertEquals(PathUtils.fixFilePath(temporaryDirectory.toString()), task.getSourceRoot());
    }

    private static final class TestTask extends ConversionTask {

        private TestTask(String sourceRoot) {
            super(sourceRoot, null, false);
        }

        @Override
        public AssetsConverter.ConvertProcess getConvertProcess() {
            return AssetsConverter.ConvertProcess.TEXTURES;
        }

        @Override
        protected void internalExecuteTask() {
        }

        private String getSourceRoot() {
            return dungeonKeeperFolder;
        }
    }
}
