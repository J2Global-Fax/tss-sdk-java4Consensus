package com.thycotic.secrets.spring;


import com.thycotic.secrets.server.spring.Secret;
import com.thycotic.secrets.server.spring.SecretServer;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.BitSet;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import org.gradle.api.Project;
import org.gradle.testfixtures.ProjectBuilder;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Bean;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.OkHttp3ClientHttpRequestFactory;

class ApplicationTests {

    public static class Config {
        @Bean
        public ClientHttpRequestFactory clientRequestFactory() {
            return new OkHttp3ClientHttpRequestFactory();
        }
    }


    /*
    This text expects the following properties to be set in the
     */
    @Test
    public void secretFileTest() {
        // you may want to change the secret below to work with your secret.
        final int DEFAULT_SECRET_ID = 14916;

        String userHome = System.getProperty("user.home");
        String projectDirectory = System.getProperty("user.dir");
        ProjectBuilder projectBuilder = ProjectBuilder.builder().
                withGradleUserHomeDir(new File(userHome)).
                withProjectDir(new File(projectDirectory));
        Project project = projectBuilder.build();
        setupHomeDirectoryProperties(project);
        project.getPluginManager().apply("consensus-secret");
        assertTrue(project.getExtensions().getExtraProperties().has("consensus-secret"));
        SecretServer secretServer = (SecretServer) project.getExtensions().getExtraProperties().get("consensus-secret");
        Secret secret = Optional.ofNullable(secretServer.getSecret(DEFAULT_SECRET_ID)).orElse(new Secret());
        System.out.println(secret);
        assertEquals(14916, secret.getId());
        assertEquals(1163, secret.getFolderId());
        assertEquals("test password", secret.getName());
        assertEquals("Password Safe", secret.getSecretTemplateName());
        List<Secret.Field> fieldList = secret.getFields();
        assertEquals(7, fieldList.size());
        BitSet bitSet = new BitSet(2);
        for (Secret.Field field : fieldList)
        {
            if ("Username".equals( field.getFieldName()))
            {
                if ("test".equals(field.getValue())) {
                    bitSet.set(0);
                }
            }
            if ("Password".equals(field.getFieldName()))
            {
                if ("password".equals(field.getValue())) {
                    bitSet.set(1);
                }
            }
        }
        assertTrue(bitSet.get(0));
        assertTrue(bitSet.get(1));
    }

    private static void setupHomeDirectoryProperties(Project project) {
        try {
            Properties props = new Properties();
            try (FileInputStream is = new FileInputStream(System.getProperty("user.home") + "/.gradle/gradle.properties")) {
                props.load(is);
                for (Object s : props.keySet())
                {
                    Object value = props.get(s);
                    project.getExtensions().getExtraProperties().set((String)s, value);
                }
            }
        } catch (IOException ex)
        {
            throw new RuntimeException("Could not load file: "+System.getProperty("user.home") + "/.gradle/gradle.properties");
        }
    }
}
