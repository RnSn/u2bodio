package ua.grab.u2bodio.rest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import javax.servlet.http.HttpServletResponse;
import ua.grab.u2bodio.services.Video2Audio;

@RestController
public class U2Controller {

    private static final String MIME_AUDIO_MP3 = "audio/mpeg";

    @Autowired
    private Video2Audio service;

    @RequestMapping("/direct")
    public final ResponseEntity<ByteArrayResource> directVideoUrl(
        @RequestParam("url") String url, HttpServletResponse response) {

        final byte[] bytes = service.audioStream(url);
        response.setContentType(MIME_AUDIO_MP3);
        response.setContentLength(bytes.length);
        response.setStatus(HttpStatus.OK.value());

        return ResponseEntity.ok().contentLength(bytes.length)
            .contentType(MediaType.parseMediaType(MIME_AUDIO_MP3))
            .body(new ByteArrayResource(bytes));
    }

}
