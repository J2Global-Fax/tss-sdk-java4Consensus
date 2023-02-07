package com.thycotic.secrets.server.spring;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.plugins.ExtraPropertiesExtension;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

public class SecretPlugin implements Plugin<Project>{

    SecretServer secretServer;
    private static final String USERNAME_KEY = "secret_server.oauth2.username";
    private static final String PASSWORD_KEY = "secret_server.oauth2.password";
    private static final String DOMAIN_KEY = "secret_server.oauth2.domain";

    @Override
    public void apply(Project project) {
        extractPropertiesFromParentDirectory(project);

        ExtraPropertiesExtension extraProperties = project.getExtensions().getExtraProperties();
        if (ensureNeededPropertiesExist(extraProperties)) return;

        try (ConfigurableApplicationContext applicationContext =
                     new AnnotationConfigApplicationContext(SecretServerFactoryBean.class)) {
            SecretServerFactoryBean ssfb = applicationContext.getBean(SecretServerFactoryBean.class);
            ssfb.setProject(project);
            secretServer = ssfb.getObject();
            project.getExtensions().getExtraProperties().set("consensus-secret", secretServer);
        }
    }

    private static boolean ensureNeededPropertiesExist(ExtraPropertiesExtension extraProperties) {
        StringBuilder sb = new StringBuilder();
        if (!extraProperties.has("secret_server.oauth2.username"))
        {
            sb.append("Missing property 'secret_server.oauth2.username'").append(System.lineSeparator());
        }
        if (!extraProperties.has("secret_server.oauth2.password"))
        {
            sb.append("Missing property 'secret_server.oauth2.password'").append(System.lineSeparator());
        }

        if (!extraProperties.has("secret_server.oauth2.domain"))
        {
            sb.append("Missing property 'secret_server.oauth2.domain'").append(System.lineSeparator());
        }
        if (sb.length() > 0)
        {
            System.err.println("Unable to interface with Thycotic Secret Server due to the missing properties");
            System.err.println(sb);
            return true;
        }
        return false;
    }

    private static void extractPropertiesFromParentDirectory(Project project) {
        Path path = Paths.get(System.getProperty("user.dir"), "/../gradle.properties");
        if (path.toFile().exists()) {

            Properties propertiesFromFile = new Properties();
            ExtraPropertiesExtension extraProperties = project.getExtensions().getExtraProperties();
            try (Reader reader = Files.newBufferedReader(path)) {
                propertiesFromFile.load(reader);

                for (Object obj : propertiesFromFile.keySet()) {
                    String key = obj.toString();
                    extraProperties.set(key, propertiesFromFile.getProperty(key));
                }
            } catch (IOException ex) {
                throw new RuntimeException("Could not run Secret Plugin", ex);
            }
        }
    }

    public SecretServer getSecretServer()
    {
        return secretServer;
    }

}
