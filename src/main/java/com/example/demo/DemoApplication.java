package com.example.demo;

import org.flowable.engine.ManagementService;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import lombok.extern.slf4j.Slf4j;

@EnableWebMvc
@SpringBootApplication(proxyBeanMethods = false)
@Slf4j
public class DemoApplication {

	public static void main(String[] args) {
		SpringApplication.run(DemoApplication.class, args);
	}

	@Bean
    public CommandLineRunner init(final RepositoryService repositoryService,
                                  final RuntimeService runtimeService,
                                  final ManagementService managementService,
                                  final TaskService taskService) {

        return new CommandLineRunner() {
            @Override
            public void run(String... strings) throws Exception {
                log.info("Number of process definitions: {}", repositoryService.createProcessDefinitionQuery().count());
                log.info("Number of process instances: {}", runtimeService.createProcessInstanceQuery().count());
                log.info("Number of tasks: {}", taskService.createTaskQuery().count());
                log.info("Number of jobs: {}", managementService.createJobQuery().count());
                log.info("Number of timer jobs: {}", managementService.createTimerJobQuery().count());
                log.info("Number of event subscriptions: {}", runtimeService.createEventSubscriptionQuery().count());
                
            }
        };
    }
}
