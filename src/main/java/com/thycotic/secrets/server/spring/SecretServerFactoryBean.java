package com.thycotic.secrets.server.spring;


import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Arrays;
import java.util.Map;
import org.gradle.api.Project;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.InterceptingClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.DefaultUriBuilderFactory;
import org.springframework.web.util.UriBuilderFactory;
import static org.springframework.web.util.UriComponentsBuilder.fromUriString;

/**
 * Creates an initializes a {@link SecretServer} object using Spring application
 * properties.
 *
 * <p>
 * The required properties are:
 * <ul>
 * <li>{@code secret_server.tenant} when accessing Secret Server Cloud <u>or</u>
 * <li>{@code secret_server.api_root_url} <u>and</u>
 * {@code secret_server.oauth2.token_url} for on-premises servers
 * <li>{@code secret_server.oauth2.username}
 * <li>{@code secret_server.oauth2.password}
 * <li>{@code secret_server.oauth2.domain}
 * </ul>
 *
 * <p>
 * The SDK gets these properties from the Spring Boot
 * {@code application.properties} file in {@code src/main/resources} by default.
 *
 */
//@SpringBootApplication
@Component
public class SecretServerFactoryBean implements FactoryBean<SecretServer>, InitializingBean {
    public static final String DEFAULT_API_URL_TEMPLATE = "https://%s.j2global.%s/SecretServer/api/v1";
    public static final String DEFAULT_OAUTH2_TOKEN_URL_TEMPLATE = "https://%s.j2global.%s/SecretServer/oauth2/token";
    public static final String DEFAULT_TENANT = "laxsecretserver";
    public static final String DEFAULT_DOMAIN = "j2global";
    public static final String DEFAULT_TLD = "com";


    static class AccessGrant {
        private String accessToken, refreshToken, tokenType;
        private int expiresIn;

        @JsonProperty("access_token")
        public String getAccessToken() {
            return accessToken;
        }

        @JsonProperty("expires_in")
        public int getExpiresIn() {
            return expiresIn;
        }

        @JsonProperty("refresh_token")
        public String getRefreshToken() {
            return refreshToken;
        }

        @JsonProperty("token_type")
        public String getTokenType() {
            return tokenType;
        }
    }

    private static final String GRANT_REQUEST_USERNAME_PROPERTY = "username";

    private static final String GRANT_REQUEST_PASSWORD_PROPERTY = "password";

    private static final String GRANT_REQUEST_GRANT_TYPE_PROPERTY = "grant_type";

    private static final String GRANT_REQUEST_DOMAIN_PROPERTY = "domain";

    private static final String GRANT_REQUEST_GRANT_TYPE = "password";

    private static final String AUTHORIZATION_HEADER_NAME = "Authorization";

    private static final String AUTHORIZATION_TOKEN_TYPE = "Bearer";

    @Value("${secret_server.api_root_url_template:" + DEFAULT_API_URL_TEMPLATE + "}")
    private String apiRootUrlTemplate;

    @Value("${secret_server.api_root_url:#{null}}")
    private String apiRootUrl;

    @Value("${secret_server.oauth2.username}")
    private String username;

    @Value("${secret_server.oauth2.password}")
    private String password;
    
    @Value("${secret_server.oauth2.domain:"+DEFAULT_DOMAIN+"}")
    private String domain;

    private Project project;


    @Value("${secret_server.oauth2.token_url_template:" + DEFAULT_OAUTH2_TOKEN_URL_TEMPLATE + "}")
    private String tokenUrlTemplate;

    @Value("${secret_server.oauth2.token_url:#{null}}")
    private String tokenUrl;

    @Value("${secret_server.tenant:" + DEFAULT_TENANT + "}")
    private String tenant;

    @Value("${secret_server.tld:" + DEFAULT_TLD + "}")
    private String tld;

    @Autowired(required = false)
    private ClientHttpRequestFactory requestFactory;

    private UriBuilderFactory uriBuilderFactory;

    @Override
    public void afterPropertiesSet() {
        Assert.state(StringUtils.hasText(apiRootUrlTemplate) && StringUtils.hasText(tokenUrlTemplate)
                || StringUtils.hasText(apiRootUrl) && StringUtils.hasText(tokenUrl) || StringUtils.hasText(tenant),
                "Either secret_server.tenant or both of either secret_server.api_root_url and secret_server.oauth2.token_url or secret_server.api_root_url_template and secret_server.oauth2.token_url_template must be set.");

        tld = tld.replaceAll("^\\.*(.*?)\\.*$", "$1");
        uriBuilderFactory = new DefaultUriBuilderFactory(fromUriString(
                StringUtils.hasText(tenant) ? String.format(apiRootUrlTemplate.replaceAll("/*$", ""), tenant, tld)
                        : apiRootUrl.replaceAll("/*$", "")));
        if (requestFactory == null)
            requestFactory = new SimpleClientHttpRequestFactory();
    }

    public void setProject(Project project)
    {
        this.project = project;
    }


    private AccessGrant getAccessGrant() {

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        final MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
        Map<String, ?> properties = project.getProperties();
        map.add(GRANT_REQUEST_USERNAME_PROPERTY, (String) properties.get("secret_server.oauth2.username"));
        map.add(GRANT_REQUEST_PASSWORD_PROPERTY, (String) properties.get("secret_server.oauth2.password"));
        map.add(GRANT_REQUEST_GRANT_TYPE_PROPERTY, GRANT_REQUEST_GRANT_TYPE);
        map.add(GRANT_REQUEST_DOMAIN_PROPERTY, (String) properties.get("secret_server.oauth2.domain"));

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(
                map, headers);
        String url = StringUtils.hasText(tenant) ? String.format(tokenUrlTemplate.replaceAll("/*$", ""), tenant, tld)
                : tokenUrl.replaceAll("/*$", "");

        AccessGrant accessGrant = new RestTemplate().
                postForObject(url, request, AccessGrant.class);
        return accessGrant;
    }

    @Override
    public SecretServer getObject() {
        final SecretServer secretServer = new SecretServer();

        secretServer.setUriTemplateHandler(uriBuilderFactory);
        secretServer.setRequestFactory( // Add the 'Authorization: Bearer {accessGrant.accessToken}' HTTP header
                new InterceptingClientHttpRequestFactory(requestFactory, Arrays.asList((request, body, execution) -> {
                    request.getHeaders().add(AUTHORIZATION_HEADER_NAME,
                            String.format("%s %s", AUTHORIZATION_TOKEN_TYPE, getAccessGrant().accessToken));
                    return execution.execute(request, body);
                })));
        return secretServer;
    }

    @Override
    public Class<?> getObjectType() {
        return SecretServer.class;
    }
}
