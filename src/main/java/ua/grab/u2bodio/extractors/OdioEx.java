package ua.grab.u2bodio.extractors;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

public class OdioEx {

    private static final String TYPE_INFO = "INFO";
    private static final String MP3 = ".mp3";

    public Path mp3(Path videoIn, Path outDir) {
        Path output = outDir.resolve(videoIn.getFileName() + MP3);
        String[] cmd = buildCmd(videoIn, output);
        try {
            System.out.println("Extracting audio from " + videoIn);
            System.out.println("Executing " + Arrays.toString(cmd));

            Process ffmpeg = Runtime.getRuntime()
                .exec(cmd, null, new File("."));
            new StreamGobbler(ffmpeg.getErrorStream(), System.err, TYPE_INFO)
                .start();
            new StreamGobbler(ffmpeg.getInputStream(), System.out, TYPE_INFO)
                .start();
            int code = ffmpeg.waitFor();
            System.out.println("Done! Exit code = " + code);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return output;
    }

    private String[] buildCmd(final Path videoIn, final Path output) {
        return new CmdBuilder()
            .withInput(videoIn.toAbsolutePath().toString())
            .withOutput(output.toAbsolutePath().toString()).cmd();
    }


    private boolean isSupportedFormat(String test) {
        return test.endsWith("mp4") || test.endsWith("avi") ||
            test.endsWith("flv");
    }

    private class CmdBuilder {
        private final String exec = "ffmpeg";
        private final String[] inFlags = {"-i"};
        private final String[] outFlags = {"-vn", "-f", "mp3"};

        private String input;
        private String output;

        public String[] cmd() {
            List<String> args = new ArrayList<>();
            args.add(exec);
            Stream.of(inFlags).forEach(args::add);
            args.add(inQuotes(input));
            Stream.of(outFlags).forEach(args::add);
            args.add(inQuotes(output));
            return args.toArray(new String[args.size()]);
        }

        private CmdBuilder withInput(String input) {
            this.input = input;
            return this;
        }

        private CmdBuilder withOutput(String output) {
            this.output = output;
            return this;
        }

        private String inQuotes(final String arg) {
            return String.format("\"%s\"", arg);
        }
    }

    private class StreamGobbler extends Thread {
        private final OutputStream out;
        private final InputStream is;
        private final String type;

        private StreamGobbler(InputStream is, OutputStream out, String type) {
            this.is = is;
            this.out = out;
            this.type = type;
        }

        @Override
        public void run() {
            try {
                BufferedReader br = new BufferedReader(
                    new InputStreamReader(is));
                String write = "";
                String line;
                while ((line = br.readLine()) != null) {
                    write = type + ">" + line;
                }
                out.write(write.getBytes());
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
        }
    }

}
