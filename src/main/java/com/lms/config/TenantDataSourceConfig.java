package com.lms.config;

import com.lms.platform.runtime.TenantRoutingDataSource;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;

/** Keeps the control-plane database as the default and routes tenant requests at runtime. */
@Configuration
public class TenantDataSourceConfig {
    @Bean(name = "controlPlaneDataSource")
    @ConfigurationProperties("spring.datasource.hikari")
    public HikariDataSource controlPlaneDataSource(DataSourceProperties properties) {
        return properties.initializeDataSourceBuilder().type(HikariDataSource.class).build();
    }

    @Bean
    @Primary
    public DataSource dataSource(TenantRoutingDataSource tenantRoutingDataSource) {
        return tenantRoutingDataSource;
    }

    @Bean
    public TenantRoutingDataSource tenantRoutingDataSource(
            @org.springframework.beans.factory.annotation.Qualifier("controlPlaneDataSource") DataSource controlPlaneDataSource) {
        return new TenantRoutingDataSource(controlPlaneDataSource);
    }
}
