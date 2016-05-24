package ua.grab.u2bodio.extractors;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Locally download a YouTube.com video.
 */
public class U2Dl {

    private final String url;

    public U2Dl(final String url) {
        this.url = url;
    }

    public Path download(final Path outputDir) throws IOException {
        return retrieve(videoId(url), outputDir);
    }

    private URI getUri(String path, List<NameValuePair> qparams)
        throws URISyntaxException {

        return new URIBuilder().setScheme(Const.SCHEME).setHost(Const.HOST)
            .setPort(-1).setPath("/" + path).setParameters(qparams).build();
    }

    private Path retrieve(String videoId, Path destDir) throws IOException {
        final String videoInfo = videoInfoString(videoId);
        final String[] links = extractLinks(getStreamMap(videoInfo));
        final Path dest = prepareDest(destDir, getTitle(videoInfo));

        for (final String directLink : links) {
            try {
                downloadFile(directLink, dest);
                break;
            } catch (IOException e) {
                System.err.println("Could not download " + getTitle(videoInfo));
                System.out.println("-------------");
                e.printStackTrace();
            }
        }
        return dest;
    }

    private Path prepareDest(final Path destDir, final String title)
        throws IOException {
        if (!Files.exists(destDir)) {
            Files.createDirectories(destDir);
        }

        final Path dest = destDir.resolve(title);
        if (Files.exists(dest)) {
            Files.delete(dest);
        }

        return dest;
    }

    private String videoInfoString(final String videoId) {
        try (CloseableHttpClient httpclient = HttpClients.createDefault()) {
            HttpResponse response = httpclient
                .execute(httpGet(videoInfoUri(videoId)), httpCtx());

            return toString(response.getEntity().getContent());

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private HttpGet httpGet(final URI uri) {
        HttpGet httpget = new HttpGet(uri);
        httpget.setHeader("User-Agent", Const.DEFAULT_USER_AGENT);
        return httpget;
    }

    private HttpContext httpCtx() {
        HttpContext localContext = new BasicHttpContext();
        localContext.setAttribute(HttpClientContext.COOKIE_STORE,
            new BasicCookieStore());
        return localContext;
    }

    private URI videoInfoUri(final String value) {
        try {
            return getUri("get_video_info", Arrays
                .asList(new BasicNameValuePair("video_id", value),
                    new BasicNameValuePair("el", "ATV"),
                    new BasicNameValuePair("el", "embedded")));
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    private String getStreamMap(String videoInfo) {
        return getNamedParameter(videoInfo, "url_encoded_fmt_stream_map");
    }

    private String getNamedParameter(String videoInfo, String param) {
        List<NameValuePair> pairs = URLEncodedUtils
            .parse(videoInfo, Charset.defaultCharset());
        for (NameValuePair pair : pairs) {
            if (pair.getName().equals(param)) {
                return pair.getValue();
            }
        }
        return "";
    }

    private String getTitle(final String videoInfo) {
        return getNamedParameter(videoInfo, "title");
    }

    private void downloadFile(String link, Path outFile) throws IOException {
        URL website = new URL(link);
        try (
            ReadableByteChannel rbc = Channels.newChannel(website.openStream());
            FileOutputStream fos = new FileOutputStream(outFile.toFile())) {

            long transfered;
            long total = 0;
            while ((transfered = fos.getChannel()
                .transferFrom(rbc, total, Const.TRANSFTER_COUNT_MB)) ==
                Const.TRANSFTER_COUNT_MB) {
                total += transfered;
                System.out
                    .printf("Transfered %.2f MB - %s\n", total / Const._1_MB,
                        outFile.getFileName());
            }
        }
    }

    private String[] extractLinks(String stream) {
        if (stream == null || stream.trim().isEmpty()) {
            return new String[]{};
        }

        List<String> res = Stream.of(stream.split(",")).map(link -> {
            Matcher matcher = Pattern.compile("quality=(.+)&?").matcher(link);
            String quality = "";
            if (matcher.find()) {
                quality = matcher.group();
                System.out.println(quality);
            }

            String[] split = link.split("&?url=");
            String resLink = (split[1] + "&" + split[0]).replaceAll("%3A", ":")
                .replaceAll("%2F", "/").replaceAll("%3F", "\\?")
                .replaceAll("%26", "\\&").replaceAll("%3D", "=")
                .replaceAll("%252C", ",").replaceAll("%252F", "/")
                .replaceFirst("quality=(.+)&?", "")
                .replaceFirst("type=(.+)&?", "")
                .replaceFirst("fallback_host=(.+)&?", "");

            matcher = Pattern.compile("itag=\\d*&?").matcher(resLink);
            if (matcher.find() && matcher.find()) {
                resLink = resLink.replaceFirst("itag=\\d*&?", "");
            }
            if (resLink.endsWith("&")) {
                resLink = resLink.substring(0, resLink.length() - 1);
            }
            if (resLink.endsWith("&pm_")) {
                resLink = resLink.substring(0, resLink.indexOf("&pm_"));
            }
            return resLink;

        }).collect(Collectors.toList());

        return sortedLinks(res);
    }

    private String[] sortedLinks(final List<String> res) {
        Collections.sort(res,
            (l1, l2) -> V_QUALITY.of(l1).compareTo(V_QUALITY.of(l2)));
        return res.toArray(new String[res.size()]);
    }

    private String toString(InputStream instream) throws IOException {
        return toString(Const.DEFAULT_ENCODING, instream);
    }

    private String toString(String encoding, InputStream instream)
        throws IOException {
        final Writer writer = new StringWriter();

        final char[] buffer = new char[Const.BUFFER_CONST];
        try (final Reader reader = new BufferedReader(
            new InputStreamReader(instream, encoding))) {
            int n;
            while ((n = reader.read(buffer)) != -1) {
                writer.write(buffer, 0, n);
            }
        }
        return writer.toString();
    }

    private String videoId(final String url) {
        return url.split("=")[1];
    }

    private enum V_QUALITY {
        WEBM("webm"), _3GPP("3gpp"), X_FLV("x-flv"), MP4("mp4"),
        DEFAULT("none");
        final String mime;

        V_QUALITY(final String type) {
            this.mime = "mime=video/" + type;
        }

        public static V_QUALITY of(final String url) {
            for (V_QUALITY e : values()) {
                if (url.contains(e.mime)) {
                    return e;
                }
            }
            return DEFAULT;
        }
    }

    private static class Const {
        private static final double _1_MB = 1048576.0;
        private static final int TRANSFTER_COUNT_MB = 10485760 * 2;
        private static final String SCHEME = "http";
        private static final String HOST = "www.youtube.com";
        private static final String DEFAULT_USER_AGENT = "Mozilla/5.0 " +
            "(Windows NT 6.3; WOW64) " +
            "AppleWebKit/537.36 (KHTML, like Gecko) " +
            "Chrome/37.0.2062.120 " +
            "Safari/537.36 " +
            "OPR/24.0.1558.61";
        private static final String DEFAULT_ENCODING = "UTF-8";
        private static final int BUFFER_CONST = 2048;
    }
}

/**
 * <pre>
 * Exploded results from get_video_info:
 *
 * fexp=909302
 * allow_embed=1
 * fmt_stream_map=35|http://v9.lscache8.c.youtube.com/videoplayback?ip=174.0.0.0&sparams=id%2Cexpire%2Cip%2Cipbits%2Citag%2Calgorithm%2Cburst%2Cfactor&fexp=909302&algorithm=throttle-factor&itag=35&ipbits=8&burst=40&sver=3&expire=1294549200&key=yt1&signature=9E0A8E67154145BCADEBCF844CC155282548288F.2BBD0B2E125E3E533D07866C7AE91B38DD625D30&factor=1.25&id=4ba2193f7c9127d2||tc.v9.cache8.c.youtube.com,34|http://v6.lscache3.c.youtube.com/videoplayback?ip=174.0.0.0&sparams=id%2Cexpire%2Cip%2Cipbits%2Citag%2Calgorithm%2Cburst%2Cfactor&fexp=909302&algorithm=throttle-factor&itag=34&ipbits=8&burst=40&sver=3&expire=1294549200&key=yt1&signature=6726793A7B041E6456B52C0972596D0D58974141.42B5A0573F62B85AEA7979E5EE1ADDD47EB9E909&factor=1.25&id=4ba2193f7c9127d2||tc.v6.cache3.c.youtube.com,18|http://v12.lscache7.c.youtube.com/videoplayback?ip=174.0.0.0&sparams=id%2Cexpire%2Cip%2Cipbits%2Citag%2Calgorithm%2Cburst%2Cfactor&fexp=909302&algorithm=throttle-factor&itag=18&ipbits=8&burst=40&sver=3&expire=1294549200&key=yt1&signature=AE58398D4CC4D760C682D2A5B670B4047777FFF0.952E4FC4554E786FD937E7A89140E1F79B6DD8B7&factor=1.25&id=4ba2193f7c9127d2||tc.v12.cache7.c.youtube.com,5|http://v1.lscache7.c.youtube.com/videoplayback?ip=174.0.0.0&sparams=id%2Cexpire%2Cip%2Cipbits%2Citag%2Calgorithm%2Cburst%2Cfactor&fexp=909302&algorithm=throttle-factor&itag=5&ipbits=8&burst=40&sver=3&expire=1294549200&key=yt1&signature=43434DCB6CFC463FF4522D9EE7CD019FE47237B1.C60A9522E361130938663AF2DAD83A5C2821AF5C&factor=1.25&id=4ba2193f7c9127d2||tc.v1.cache7.c.youtube.com
 * fmt_url_map=35|http://v9.lscache8.c.youtube.com/videoplayback?ip=174.0.0.0&sparams=id%2Cexpire%2Cip%2Cipbits%2Citag%2Calgorithm%2Cburst%2Cfactor&fexp=909302&algorithm=throttle-factor&itag=35&ipbits=8&burst=40&sver=3&expire=1294549200&key=yt1&signature=9E0A8E67154145BCADEBCF844CC155282548288F.2BBD0B2E125E3E533D07866C7AE91B38DD625D30&factor=1.25&id=4ba2193f7c9127d2,34|http://v6.lscache3.c.youtube.com/videoplayback?ip=174.0.0.0&sparams=id%2Cexpire%2Cip%2Cipbits%2Citag%2Calgorithm%2Cburst%2Cfactor&fexp=909302&algorithm=throttle-factor&itag=34&ipbits=8&burst=40&sver=3&expire=1294549200&key=yt1&signature=6726793A7B041E6456B52C0972596D0D58974141.42B5A0573F62B85AEA7979E5EE1ADDD47EB9E909&factor=1.25&id=4ba2193f7c9127d2,18|http://v12.lscache7.c.youtube.com/videoplayback?ip=174.0.0.0&sparams=id%2Cexpire%2Cip%2Cipbits%2Citag%2Calgorithm%2Cburst%2Cfactor&fexp=909302&algorithm=throttle-factor&itag=18&ipbits=8&burst=40&sver=3&expire=1294549200&key=yt1&signature=AE58398D4CC4D760C682D2A5B670B4047777FFF0.952E4FC4554E786FD937E7A89140E1F79B6DD8B7&factor=1.25&id=4ba2193f7c9127d2,5|http://v1.lscache7.c.youtube.com/videoplayback?ip=174.0.0.0&sparams=id%2Cexpire%2Cip%2Cipbits%2Citag%2Calgorithm%2Cburst%2Cfactor&fexp=909302&algorithm=throttle-factor&itag=5&ipbits=8&burst=40&sver=3&expire=1294549200&key=yt1&signature=43434DCB6CFC463FF4522D9EE7CD019FE47237B1.C60A9522E361130938663AF2DAD83A5C2821AF5C&factor=1.25&id=4ba2193f7c9127d2
 * allow_ratings=1
 * keywords=Stefan Molyneux,Luke Bessey,anarchy,stateless society,giant stone cow,the story of our unenslavement,market anarchy,voluntaryism,anarcho capitalism
 * track_embed=0
 * fmt_list=35/854x480/9/0/115,34/640x360/9/0/115,18/640x360/9/0/115,5/320x240/7/0/0
 * author=lukebessey
 * muted=0
 * length_seconds=390
 * plid=AASZXXGQtTEDKwAw
 * ftoken=null
 * status=ok
 * watermark=http://s.ytimg.com/yt/swf/logo-vfl_bP6ud.swf,http://s.ytimg.com/yt/swf/hdlogo-vfloR6wva.swf
 * timestamp=1294526523
 * has_cc=False
 * fmt_map=35/854x480/9/0/115,34/640x360/9/0/115,18/640x360/9/0/115,5/320x240/7/0/0
 * leanback_module=http://s.ytimg.com/yt/swfbin/leanback_module-vflJYyeZN.swf
 * hl=en_US
 * endscreen_module=http://s.ytimg.com/yt/swfbin/endscreen-vflk19iTq.swf
 * vq=auto
 * avg_rating=5.0
 * video_id=S6IZP3yRJ9I
 * token=vjVQa1PpcFNhI3jvw6hfEHivcKK-XY5gb-iszDMrooA=
 * thumbnail_url=http://i4.ytimg.com/vi/S6IZP3yRJ9I/default.jpg
 * title=The Story of Our Unenslavement - Animated
 * </pre>
 */
