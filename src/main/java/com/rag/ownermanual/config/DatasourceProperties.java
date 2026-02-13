package com.rag.ownermanual.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Binds and validates datasource settings from application.yml (e.g. Supabase PostgreSQL).
 */
@Validated
@ConfigurationProperties(prefix = "spring.datasource")
public class DatasourceProperties {

    @NotBlank(message = "Database URL is required; set SUPABASE_DB_URL (or SPRING_DATASOURCE_URL) environment variable")
    private String url;

    @NotBlank(message = "Database username is required; set SUPABASE_DB_USERNAME environment variable")
    private String username;

    @NotBlank(message = "Database password is required; set SUPABASE_DB_PASSWORD environment variable")
    private String password;

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
