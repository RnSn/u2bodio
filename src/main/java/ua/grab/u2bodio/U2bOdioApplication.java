package ua.grab.u2bodio;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.web.SpringBootServletInitializer;

@SpringBootApplication
public class U2bOdioApplication extends SpringBootServletInitializer {

    public static void main(String[] args) {
        SpringApplication.run(U2bOdioApplication.class, args);
    }

    @Override
    protected SpringApplicationBuilder configure(
        SpringApplicationBuilder application) {
        return application.sources(U2bOdioApplication.class);
    }

}
