package ua.grab.u2bodio.extractors;

import org.junit.Test;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import static org.junit.Assert.assertTrue;

public class U2dlTest {

    @Test
    public void testU2() throws IOException {
        String url = "https://www.youtube.com/watch?v=pjcXt4v0p2c";
        VideoInfo vi = new VideoInfo(url);
        U2Dl u2dl = new U2Dl(vi);

        Path outputDir = Paths.get("./target").toAbsolutePath()
            .normalize();
        Path video = u2dl.download(outputDir);


        assertTrue(String.format("Ouput file for '%s' should exist in '%s'",
            url, video),
            Files.exists(video));

        assertTrue(String.format("Ouput file '%s' should not be empty",
            video),
            Files.size(video) > 0L);
    }
}
