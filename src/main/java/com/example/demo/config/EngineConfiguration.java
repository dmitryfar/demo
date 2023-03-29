package com.example.demo.config;

import java.sql.Driver;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import javax.sql.DataSource;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.flowable.bpmn.model.CallActivity;
import org.flowable.common.engine.api.delegate.event.FlowableEventListener;
import org.flowable.common.engine.impl.AbstractEngineConfiguration;
import org.flowable.common.engine.impl.history.HistoryLevel;
import org.flowable.engine.ProcessEngine;
import org.flowable.engine.ProcessEngineLifecycleListener;
import org.flowable.engine.impl.bpmn.behavior.CallActivityBehavior;
import org.flowable.engine.impl.bpmn.parser.factory.ActivityBehaviorFactory;
import org.flowable.engine.impl.bpmn.parser.factory.DefaultActivityBehaviorFactory;
import org.flowable.engine.impl.db.DbIdGenerator;
import org.flowable.rest.service.api.RestResponseFactory;
import org.flowable.spring.SpringProcessEngineConfiguration;
import org.flowable.spring.boot.EngineConfigurationConfigurer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.SimpleDriverDataSource;
import org.springframework.transaction.PlatformTransactionManager;

import lombok.extern.slf4j.Slf4j;

@Configuration
@Slf4j
//@EnableJpaRepositories(basePackages = "com.example.database")
public class EngineConfiguration implements ProcessEngineLifecycleListener {

    @Value("${spring.datasource.driver-class-name}")
    private Class<? extends Driver> driverClass;
    @Value("${spring.datasource.url}")
    private String dbUrl;
    @Value("${spring.datasource.username}")
    private String dbUser;
    @Value("${spring.datasource.password}")
    private String dbPassword;

    @Autowired
    protected ObjectMapper objectMapper;
    
    @Bean
    public DataSource dataSource() {
        SimpleDriverDataSource dataSource = new SimpleDriverDataSource();
        dataSource.setDriverClass(driverClass);
        dataSource.setUrl(dbUrl);
        dataSource.setUsername(dbUser);
        dataSource.setPassword(dbPassword);

        return dataSource;
    }
    
    @Bean(name = "transactionManager")
    public PlatformTransactionManager annotationDrivenTransactionManager() {
        DataSourceTransactionManager transactionManager = new DataSourceTransactionManager();
        transactionManager.setDataSource(dataSource());
        return transactionManager;
    }

    @Bean
    @ConditionalOnProperty(prefix = "flowable", name = "simpleDbIdGenerator", havingValue = "true")
    public EngineConfigurationConfigurer<SpringProcessEngineConfiguration> customIdGeneratorConfigurer() {
        return engineConfiguration -> engineConfiguration.setIdGenerator(new DbIdGenerator());
    }

    @Bean
    public CustomAsyncRunnableExecutionExceptionHandler customAsyncRunnableExecutionExceptionHandler() {
        return new CustomAsyncRunnableExecutionExceptionHandler();
    }
    
    @Bean
    public RestResponseFactory restResponseFactory() {
        return new RestResponseFactory(objectMapper);
    }
    
    @Bean
    public EngineConfigurationConfigurer<SpringProcessEngineConfiguration> processEngineConfiguration() {
        return springProcessEngineConfiguration -> {
            springProcessEngineConfiguration
                .setCustomAsyncRunnableExecutionExceptionHandlers(Collections.singletonList(customAsyncRunnableExecutionExceptionHandler()))
//                .setActivityBehaviorFactory(new CustomActivityBehaviourFactory())
                // --------------------------------
//                .setActivityBehaviorFactory(activityBehaviorFactory())
//                .setAsyncExecutorActivate(activateDefaultExecutor)
                .setAsyncExecutorNumberOfRetries(2)
                .setAsyncHistoryExecutorNumberOfRetries(0)
                .setDataSource(dataSource())
                .setDatabaseSchemaUpdate(AbstractEngineConfiguration.DB_SCHEMA_UPDATE_TRUE)
//                .setHttpClientConfig(getHttpClientConfig())
                .setHistoryLevel(HistoryLevel.ACTIVITY)
                ;
            
            List<FlowableEventListener> flowableEventListeners = Collections.singletonList(new CustomFlowableActivityEventListener());
            springProcessEngineConfiguration.setEventListeners(flowableEventListeners);
    
            springProcessEngineConfiguration.setEngineLifecycleListeners(Collections.singletonList(this));
            
            // custom mapping
            // springProcessEngineConfiguration.setCustomMybatisXMLMappers(Collections.singleton("custom-mappers/CustomSignalMapper.xml"));

        };
    }
    
    /**
     * Override the factory behavior to set the inheritVariable property as true.
     */
    @Bean
    public ActivityBehaviorFactory activityBehaviorFactory() {
        return new DefaultActivityBehaviorFactory() {
            @Override
            public CallActivityBehavior createCallActivityBehavior(CallActivity callActivity) {
                callActivity.setInheritVariables(true);
                return super.createCallActivityBehavior(callActivity);
            }
        };
    }
    
    @Override
    public void onProcessEngineBuilt(ProcessEngine processEngine) {
        log.debug("Process Engine Built: {}", processEngine.getName());
    }

    @Override
    public void onProcessEngineClosed(ProcessEngine processEngine) {
        log.debug("Process Engine Close: {}", processEngine.getName());
    }
}
