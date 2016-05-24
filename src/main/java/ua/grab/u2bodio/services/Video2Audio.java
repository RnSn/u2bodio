package ua.grab.u2bodio.services;

import org.springframework.stereotype.Service;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.UUID;
import ua.grab.u2bodio.extractors.OdioEx;
import ua.grab.u2bodio.extractors.U2Dl;

@Service
public class Video2Audio {

    public byte[] audioStream(final String url) {
        Path workDir = workDir();
        U2Dl u2dl = new U2Dl(url);
        OdioEx ae = new OdioEx();
        try {
            Path video = u2dl.download(workDir);
            Path audio = ae.mp3(video, workDir);
            return Files.readAllBytes(audio);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private Path workDir() {
        Path workDir = Paths.get("working",
            LocalDateTime.now().toString().replaceAll(":", "" + ".") +
                "_" + UUID.randomUUID().toString());
        try {
            Files.createDirectories(workDir);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return workDir;
    }
}
