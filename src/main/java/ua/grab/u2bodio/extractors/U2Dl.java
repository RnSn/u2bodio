package ua.grab.u2bodio.extractors;

import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Locally download a YouTube.com video.
 */
public class U2Dl {

    private static final double _1_MB = 1048576.0;
    private static final int TRANSFTER_COUNT_MB = 10485760 * 2;

    private final VideoInfo videoInfo;

    public U2Dl(final VideoInfo videoInfo) {
        this.videoInfo = videoInfo;
    }

    public Path download(final Path outputDir) throws IOException {
        return retrieve(outputDir);
    }

    private Path retrieve(Path destDir) throws IOException {
        final String[] links = videoInfo.links();
        final Path dest = prepareDest(destDir);

        for (final String directLink : links) {
            try {
                downloadFile(directLink, dest);
                break;
            } catch (IOException e) {
                System.err
                    .println("Could not download " + videoInfo.title());
                System.out.println("-------------");
                e.printStackTrace();
            }
        }
        return dest;
    }

    private Path prepareDest(final Path destDir) throws IOException {
        if (!Files.exists(destDir)) {
            Files.createDirectories(destDir);
        }

        final Path dest = destDir.resolve(videoInfo.encodedTitle());
        if (Files.exists(dest)) {
            Files.delete(dest);
        }

        return dest;
    }

    private void downloadFile(String link, Path outFile) throws IOException {
        URL website = new URL(link);
        try (
            ReadableByteChannel rbc = Channels.newChannel(website.openStream());
            FileOutputStream fos = new FileOutputStream(outFile.toFile())) {

            long transfered;
            long total = 0;
            while ((transfered = fos.getChannel()
                .transferFrom(rbc, total, TRANSFTER_COUNT_MB)) ==
                TRANSFTER_COUNT_MB) {
                total += transfered;
                System.out.printf("Transfered %.2f MB - %s\n", total / _1_MB,
                    outFile.getFileName());
            }
        }
    }
}