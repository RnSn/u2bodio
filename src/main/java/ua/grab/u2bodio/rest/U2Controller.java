package ua.grab.u2bodio.rest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ua.grab.u2bodio.services.Video2Audio;

@RestController
public class U2Controller {

    @Autowired
    private Video2Audio service;

    @RequestMapping("/direct")
    public final ResponseEntity<String> directVideoUrl(@RequestParam("url")
        String url) {
        return new ResponseEntity<String>(
            "resp : " + this.service.directUrl(url), HttpStatus.OK);
    }

}
