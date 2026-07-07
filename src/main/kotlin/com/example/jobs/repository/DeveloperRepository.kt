package com.example.jobs.repository

import com.example.jobs.entity.Developer
import org.springframework.data.repository.reactive.ReactiveCrudRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface DeveloperRepository : ReactiveCrudRepository<Developer, UUID>