package com.rag.ownermanual.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

/**
 * Datasource configuration: validates required DB settings and creates the DataSource bean.
 */
@Configuration
@EnableConfigurationProperties(DatasourceProperties.class)
public class DatasourceConfig {

    /**
     * Builds the DataSource from validated DatasourceProperties.
     * Called only after properties have been bound and validated (e.g. @NotBlank).
     */
    @Bean
    public DataSource dataSource(DatasourceProperties props) {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(props.getUrl());
        config.setUsername(props.getUsername());
        config.setPassword(props.getPassword());
        return new HikariDataSource(config);
    }
}
