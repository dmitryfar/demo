package com.example.demo.service;

import java.util.EmptyStackException;
import java.util.Map;

import com.example.demo.interceptor.ExecutionContextHolder;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.flowable.common.engine.impl.interceptor.Command;
import org.flowable.common.engine.impl.interceptor.CommandConfig;
import org.flowable.common.engine.impl.interceptor.CommandExecutor;
import org.flowable.engine.ProcessEngine;
import org.flowable.engine.TaskService;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.engine.impl.persistence.entity.ExecutionEntity;
import org.flowable.engine.task.Comment;
import org.slf4j.helpers.FormattingTuple;
import org.slf4j.helpers.MessageFormatter;
import org.springframework.beans.factory.annotation.Autowired;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class AbstractBpmService {
    @Autowired
    ProcessEngine processEngine;

    @Autowired
    ObjectMapper objectMapper;

    protected <T> T getExecutionVariable(String variableName, Class<T> clazz) {
        Object variable = ExecutionContextHolder.getExecution().getVariable(variableName);
        if (variable == null) {
            return null;
        }
        try {
            return clazz.cast(variable);
        } catch (ClassCastException e) {
            log.error(String.format("Can't cast process[%s] variable [%s] to target type [%s]: %s",
                    ExecutionContextHolder.getExecution().getProcessInstanceId(), variableName, clazz.getSimpleName(),
                    e));
            return null;
        }
    }
    
    protected CommandExecutor getCommandExecutor() {
        return ((ProcessEngineConfigurationImpl) processEngine.getProcessEngineConfiguration()).getCommandExecutor();
    }

    protected <T> T executeCommand(Command<T> command) {
        CommandConfig commandConfig = getCommandExecutor().getDefaultConfig().transactionRequiresNew();
        return processEngine.getProcessEngineConfiguration().getManagementService().executeCommand(commandConfig,
                command);
    }

    protected void setExecutionVariable(String variableName, Object value) {
        ExecutionContextHolder.getExecution().setVariable(variableName, value);
    }

    protected void setExecutionVariableLocal(String variableName, Object value) {
        ExecutionContextHolder.getExecution().setVariableLocal(variableName, value);
    }

    protected void addProcessInstanceComment(String message) {
        _addProcessInstanceServiceComment(null, message);
    }

    protected void addProcessInstanceServiceComment(String message, Object... objects) {
        _addProcessInstanceServiceComment(this.getClass().getSimpleName(), message, objects);
    }

    protected void addProcessInstanceServiceComment(Class<?> clazz, String message, Object... objects) {
        _addProcessInstanceServiceComment(clazz.getSimpleName(), message, objects);
    }

    private Comment _addProcessInstanceServiceComment(String className, String message, Object... objects) {
        final FormattingTuple formattingTuple = MessageFormatter.arrayFormat(message, objects);
        
        String formattedMessage = formattingTuple.getMessage();

        Comment comment = null;
        try {
            ExecutionEntity execution = ExecutionContextHolder.getExecution();
            String processInstanceId = execution.getProcessInstanceId();
            TaskService taskService = processEngine.getTaskService();

            Map<String, String> map = Map.ofEntries(
                    Map.entry("message", formattedMessage)
                );
            if (className != null) {
                map.put("className", className);
            }

            String jsonMessage = objectMapper.writeValueAsString(map);

            comment = taskService.addComment(null, processInstanceId, "serviceComment", jsonMessage);
        } catch (EmptyStackException e) {
            log.debug("No execution found");
        } catch (Exception e) {
            log.warn("Can not create process instance comment: " + e.getMessage());
        }
        return comment;
    }
}
