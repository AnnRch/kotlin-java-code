package com.example.jobs.config;

import io.r2dbc.spi.ConnectionFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.r2dbc.connection.R2dbcTransactionManager;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.transaction.ReactiveTransactionManager;

@Configuration
public class DatabaseConfig {
  /**
   * Manually forces the generation of the DatabaseClient bean
   * using the auto-configured R2DBC ConnectionFactory.
   */
  @Bean
  public DatabaseClient databaseClient(ConnectionFactory connectionFactory){
    return DatabaseClient.builder()
        .connectionFactory(connectionFactory)
        .build();
  }

  /**
   * Optional: Registers a reactive transaction manager for r2dbc if you ever use it,
   * preventing it from conflicting with your standard JPA PlatformTransactionManager.
   */
  @Bean
  public ReactiveTransactionManager reactiveTransactionManager(ConnectionFactory connectionFactory) {
    return new R2dbcTransactionManager(connectionFactory);
  }
}
