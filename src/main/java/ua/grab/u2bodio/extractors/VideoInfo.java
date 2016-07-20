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
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class VideoInfo {

    private final String videoInfo;

    public VideoInfo(final String url) {
        this.videoInfo = videoInfoString(videoId(url));
    }

    String[] links() {
        return extractedLinks(streamMap(videoInfo));
    }

    private URI toUri(String path, List<NameValuePair> qparams)
        throws URISyntaxException {

        return new URIBuilder().setScheme(Const.SCHEME).setHost(Const.HOST)
            .setPort(-1).setPath("/" + path).setParameters(qparams).build();
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
            return toUri("get_video_info", Arrays
                .asList(new BasicNameValuePair("video_id", value),
                    new BasicNameValuePair("el", "ATV"),
                    new BasicNameValuePair("el", "embedded")));
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    private String streamMap(String videoInfo) {
        return paramVal(videoInfo, "url_encoded_fmt_stream_map");
    }

    private String paramVal(String videoInfo, String param) {
        List<NameValuePair> pairs = URLEncodedUtils
            .parse(videoInfo, Charset.defaultCharset());
        Optional<NameValuePair> result = pairs.stream()
            .filter(pair -> pair.getName().equals(param)).findAny();
        if (result.isPresent()) {
            return result.get().getValue();
        } else {
            return "";
        }
    }

    public String title() {
        return paramVal(videoInfo, "title");
    }

    String encodedTitle() {
        String title = title();
        for (final char c : Const.ILLEGAL_FILENAME_CHARACTERS) {
            title = title.replace(c, '_');
        }
        return title;
    }

    private String[] extractedLinks(String stream) {
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
        private static final String SCHEME = "http";
        private static final String HOST = "www.youtube.com";
        private static final String DEFAULT_USER_AGENT =
            "Mozilla/5.0 " + "(Windows NT 6.3; WOW64) " +
                "AppleWebKit/537.36 (KHTML, like Gecko) " +
                "Chrome/37.0.2062.120 " + "Safari/537.36 " + "OPR/24.0.1558.61";
        private static final String DEFAULT_ENCODING = "UTF-8";
        private static final int BUFFER_CONST = 2048;
        private static final char[] ILLEGAL_FILENAME_CHARACTERS = {'/', '\n',
            '\r', '\t', '\0', '\f', '`', '?', '*', '\\', '<', '>', '|', '\"',
            ':', ' ', '!'};
    }


}
