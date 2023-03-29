package com.example.demo.api;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.example.demo.helper.FlowableHelper;
import com.example.demo.service.MyService;

import org.flowable.engine.RuntimeService;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.flowable.engine.runtime.Execution;
import org.flowable.engine.runtime.ExecutionQuery;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.eventsubscription.service.EventSubscriptionService;
import org.flowable.eventsubscription.service.impl.persistence.entity.SignalEventSubscriptionEntity;
import org.flowable.task.api.Task;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.Data;

@RestController
@RequestMapping("/demo")
public class DemoController {

    @Autowired
    MyService myService;
    @Autowired
    private RuntimeService runtimeService;
    @Autowired
    private FlowableHelper flowableHelper;
    
    @GetMapping("/test")
    public String test() {
        
        return "test";
    }
    
    @PostMapping(value="/process")
    public void startProcessInstance() {
        myService.startProcess();
    }

    @PostMapping(value="/start/{processDefinitionKey}", produces = "text/plain;charset=UTF-8")
    public String startProcessInstance4(@PathVariable String processDefinitionKey,
            @RequestBody(required = false) Map<String, Object> variables) {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey(processDefinitionKey, variables);
        return flowableHelper.printExecutionTree(processInstance.getId());
    }

    @PostMapping(value="/signal/{signal}/{executionId}", produces = "text/plain;charset=UTF-8")
    public void signalExecution(@PathVariable String signal, @PathVariable String executionId) {
        runtimeService.signalEventReceived(signal, executionId);
    }

    @PostMapping(value="/process/{processInstanceId}/signal/{signal}", produces = "text/plain;charset=UTF-8")
    public void signalRootProcessInstance(@PathVariable String signal, @PathVariable String processInstanceId) {
        List<Execution> executions = runtimeService.createExecutionQuery()
                .signalEventSubscriptionName(signal)
                .rootProcessInstanceId(processInstanceId).list();
        
        for (Execution execution : executions) {
            runtimeService.signalEventReceived(signal, execution.getId());
        }
    }

    @RequestMapping(value="/tasks", method= RequestMethod.GET, produces=MediaType.APPLICATION_JSON_VALUE)
    public List<TaskRepresentation> getTasks(@RequestParam String assignee) {
        List<Task> tasks = myService.getTasks(assignee);
        List<TaskRepresentation> dtos = new ArrayList<TaskRepresentation>();
        for (Task task : tasks) {
            dtos.add(new TaskRepresentation(task.getId(), task.getName()));
        }
        return dtos;
    }

    @Data
    static class TaskRepresentation {

        private String id;
        private String name;

        public TaskRepresentation(String id, String name) {
            this.id = id;
            this.name = name;
        }

    }
}
