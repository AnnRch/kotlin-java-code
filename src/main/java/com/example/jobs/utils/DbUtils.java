package com.example.jobs.utils;

import org.springframework.r2dbc.core.DatabaseClient;

public class DbUtils {
  public static DatabaseClient.GenericExecuteSpec bindNullable(
      DatabaseClient.GenericExecuteSpec spec,
      String name,
      Object value,
      Class<?> type
  ) {
    return (value != null) ? spec.bind(name, value) : spec.bindNull(name, type);
  }
}
