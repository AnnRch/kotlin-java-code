package com.example.jobs.dto;

import java.util.List;

public record CompanyProfileResponse(
    CompanyDetailsDto company,
    List<JobSyncDto> jobs
) {

}
