package ua.grab.u2bodio.extractors;

import org.junit.Test;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class OdioExTest {

    @Test
    public void testOdioEx() throws IOException {
        final Path outDir = Paths.get("./target");
        final Path videoIn = outDir.resolve("Labrador. One Day");
        OdioEx ae = new OdioEx();
        Path output = ae.mp3(videoIn, outDir);

        assertNotNull("OdioEx.mp3() should return a result", output);

        assertTrue(String.format("Ouput file for '%s' should exist in '%s'",
            videoIn.toAbsolutePath(), output), Files.exists(output));

        assertTrue(String.format("Ouput file '%s' should not be empty", output),
            Files.size(output) > 0L);


    }
}
