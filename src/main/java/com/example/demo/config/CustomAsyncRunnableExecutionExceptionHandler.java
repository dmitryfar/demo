package com.example.demo.config;

import org.flowable.job.api.JobInfo;
import org.flowable.job.service.JobServiceConfiguration;
import org.flowable.job.service.impl.asyncexecutor.DefaultAsyncRunnableExecutionExceptionHandler;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Slf4j
//@Component
public class CustomAsyncRunnableExecutionExceptionHandler extends DefaultAsyncRunnableExecutionExceptionHandler {

    @Override
    public boolean handleException(JobServiceConfiguration jobServiceConfiguration, JobInfo job, Throwable exception) {

        log.error("Error raised: " + job, exception);

        return super.handleException(jobServiceConfiguration, job, exception);
    }
}
