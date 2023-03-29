package com.example.demo.service;

import com.example.demo.interceptor.ExecutionContextHolder;

import org.apache.commons.lang3.StringUtils;
import org.flowable.engine.ProcessEngine;
import org.flowable.engine.delegate.DelegateExecution;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Component("bpmLog")
@Slf4j
public class BpmLog extends AbstractBpmService {

    @Autowired
    ProcessEngine processEngine;
    
    public BpmLog() {
        log.info(">>> {} installed", BpmLog.class.getName());
    }
    
    public void info(String msg) {
        if (log.isInfoEnabled()) {
            String procInstId = ExecutionContextHolder.getExecution().getProcessInstanceId();
            String procDefId = ExecutionContextHolder.getExecution().getProcessDefinitionId();
            log.info("{} (procInstId={}, procDefId={})", StringUtils.upperCase(msg), procInstId, procDefId);
        }

        addProcessInstanceComment(msg);
    }
    
    public void info(String msg, DelegateExecution execution) {
        if (log.isInfoEnabled()) {
            log.info("{}: ==> {}", StringUtils.upperCase(msg), getInfo(execution));
        }
    }
    
    private String getInfo(DelegateExecution execution) {
        return "PROCESS EXECUTION [" + execution.getProcessDefinitionId() + "] " + "[id:" + execution.getId()
                + ", piId:" + execution.getProcessInstanceId() + "], "
                + ((execution.getEventName() == null) ? "" : "event:" + StringUtils.upperCase(execution.getEventName()) + ", ")
                + "activity: " + execution.getCurrentActivityId() + "(" + execution.getCurrentFlowElement().getName()
                + ")";
    }
}
