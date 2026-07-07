package com.example.jobs

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.persistence.autoconfigure.EntityScan
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication
@EnableScheduling
@ConfigurationPropertiesScan
//@EnableJpaRepositories(basePackages = ["com.example.jobs.repository"])
@EntityScan(basePackages = ["com.example.jobs.entity"])
class JobsApplication

fun main(args: Array<String>) {
	runApplication<JobsApplication>(*args)
}
