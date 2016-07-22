package ua.grab.u2bodio.services;

import org.springframework.stereotype.Service;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import ua.grab.u2bodio.extractors.OdioEx;
import ua.grab.u2bodio.extractors.U2Dl;
import ua.grab.u2bodio.extractors.VideoInfo;

@Service
public class Video2Audio {

    private Map<String, VideoInfo> lastInfos = new HashMap<>();

    public byte[] audioStream(final String url) {
        Path workDir = workDir();
        try {
            Path video = new U2Dl(
                lastInfos.computeIfAbsent(url, VideoInfo::new))
                .download(workDir);
            Path audio = new OdioEx().mp3(video, workDir);
            return Files.readAllBytes(audio);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new byte[0];
    }

    public String title(final String url) {
        VideoInfo vi = lastInfos.computeIfAbsent(url, VideoInfo::new);
        return vi.title();
    }

    private Path workDir() {
        Path workDir = Paths.get("working",
            LocalDateTime.now().toString().replaceAll(":", ".") + "_" +
                UUID.randomUUID().toString());
        try {
            Files.createDirectories(workDir);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return workDir;
    }
}
