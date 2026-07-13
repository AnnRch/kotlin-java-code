package com.example.jobs.repository;

import com.example.jobs.entity.KotlinDeveloper;
import java.util.UUID;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DeveloperJavaRepository extends ReactiveCrudRepository<KotlinDeveloper, UUID> {

}
