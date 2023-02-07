package com.thycotic.secrets.spring;

import static java.lang.Integer.parseInt;
import com.thycotic.secrets.server.spring.SecretServer;
import static java.lang.String.format;
import java.util.logging.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan("com.thycotic.secrets.server.spring")
public class Application {
    private final Logger log = Logger.getLogger(Application.class.getName());

    @Value("${secret.id:14888}")
    private String secretId;


    public static void main(final String[] args) {
        SpringApplication.run(Application.class, args);
    }

    @Bean
    public CommandLineRunner runServer(final SecretServer secretServer) {
        return args -> {
            log.info(format("running with args \"%s\"; for secret ID:{}, got Secret({}) -> %s", args, secretId,
                    secretServer.getSecret(parseInt(secretId)).toString()));
        };
    }
}
