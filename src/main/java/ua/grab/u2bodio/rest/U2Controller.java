package ua.grab.u2bodio.rest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.ModelAndView;
import ua.grab.u2bodio.services.Video2Audio;

@RestController
public class U2Controller {

    private static final String MIME_AUDIO_MP3 = "audio/mpeg";

    @Autowired
    private Video2Audio service;

    @RequestMapping("/direct")
    public final ModelAndView directVideoUrl(
        @RequestParam("url") String url) {

        final String title = service.title(url);

        ModelAndView mav = new ModelAndView("videoinfo");
        mav.addObject("title", title);
        mav.addObject("url", url);
        return mav;
    }

    @RequestMapping("/mp3")
    public final ResponseEntity<ByteArrayResource> mp3(
        @RequestParam("url") String url) {

        final byte[] bytes = service.audioStream(url);
        return ResponseEntity.ok().contentLength(bytes.length)
            .contentType(MediaType.parseMediaType(MIME_AUDIO_MP3))
            .body(new ByteArrayResource(bytes));
    }

}
