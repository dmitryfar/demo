package com.example.demo.config;

import org.apache.commons.lang3.ArrayUtils;
import org.flowable.common.engine.api.delegate.event.FlowableEngineEntityEvent;
import org.flowable.common.engine.api.delegate.event.FlowableEngineEventType;
import org.flowable.common.engine.api.delegate.event.FlowableEntityEvent;
import org.flowable.common.engine.api.delegate.event.FlowableEvent;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.event.AbstractFlowableEngineEventListener;
import org.flowable.engine.delegate.event.FlowableActivityEvent;
import org.flowable.engine.delegate.event.FlowableProcessEngineEvent;
import org.flowable.engine.delegate.event.FlowableSequenceFlowTakenEvent;
import org.flowable.engine.impl.util.CommandContextUtil;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CustomFlowableActivityEventListener extends AbstractFlowableEngineEventListener {

    private static final FlowableEngineEventType[] loggedEvents = {
            FlowableEngineEventType.ACTIVITY_CANCELLED,
            FlowableEngineEventType.ACTIVITY_ERROR_RECEIVED,
            FlowableEngineEventType.ACTIVITY_MESSAGE_RECEIVED,
            FlowableEngineEventType.ACTIVITY_SIGNAL_WAITING,
            FlowableEngineEventType.ACTIVITY_SIGNALED,
            FlowableEngineEventType.CUSTOM,
            FlowableEngineEventType.JOB_CANCELED,
            FlowableEngineEventType.JOB_EXECUTION_FAILURE,
            FlowableEngineEventType.JOB_EXECUTION_SUCCESS,
            FlowableEngineEventType.JOB_RESCHEDULED,
            FlowableEngineEventType.PROCESS_CANCELLED,
            FlowableEngineEventType.PROCESS_COMPLETED,
            FlowableEngineEventType.PROCESS_COMPLETED_WITH_ERROR_END_EVENT,
            FlowableEngineEventType.PROCESS_COMPLETED_WITH_TERMINATE_END_EVENT,
            FlowableEngineEventType.PROCESS_STARTED,
            FlowableEngineEventType.TASK_COMPLETED,
            FlowableEngineEventType.TASK_CREATED,
            FlowableEngineEventType.TIMER_FIRED,
            FlowableEngineEventType.SEQUENCEFLOW_TAKEN,
            FlowableEngineEventType.ACTIVITY_STARTED,
            FlowableEngineEventType.ENGINE_CREATED,
            FlowableEngineEventType.ACTIVITY_COMPLETED,
    };
    
    @Override
    public void onEvent(FlowableEvent event) {

        if (event instanceof FlowableEntityEvent && ArrayUtils.contains(loggedEvents, event.getType())) {
            log.debug("CustomFlowableActivityEventListener: FlowableEntityEvent >>> event type: {}, entity: {}",
                            event.getType(), ((FlowableEntityEvent) event).getEntity());
        }
        if (event instanceof FlowableActivityEvent && ArrayUtils.contains(loggedEvents, event.getType())) {
            DelegateExecution execution = getExecution(((FlowableActivityEvent) event).getExecutionId());
            log.debug("CustomFlowableActivityEventListener: FlowableActivityEvent: onEvent >>> {}, activity name {}, "
                            + "activity id {}, executionId: {}, tenantId: {}",
                    event.getType(),
                    ((FlowableActivityEvent) event).getActivityName(),
                    ((FlowableActivityEvent) event).getActivityId(),
                    ((FlowableActivityEvent) event).getExecutionId(),
                    (execution == null ? "" : execution.getTenantId()));

        }

        super.onEvent(event);
    }

    protected DelegateExecution getExecution(String executionId) {
        if (executionId != null) {
            CommandContext commandContext = CommandContextUtil.getCommandContext();
            if (commandContext != null) {
                return CommandContextUtil.getExecutionEntityManager(commandContext).findById(executionId);
            }
        }
        return null;
    }

    @Override
    protected void sequenceFlowTaken(FlowableSequenceFlowTakenEvent event) {
        DelegateExecution execution = getExecution(event);
        log.debug("FlowableSequenceFlowTakenEvent >>> type: {},"
                        + " sourceAcivityName={}, sourceActivityId={} --> targetActivityId={},"
                        + " executionId={}, on tenantId={}",
                        event.getType(),
                        event.getSourceActivityName(), event.getSourceActivityId(), event.getTargetActivityId(),
                        event.getExecutionId(), (execution == null ? "" : execution.getTenantId()));
    }
    
    @Override
    protected void activityStarted(FlowableActivityEvent event) {
        String activityType = event.getActivityType();
        log.debug("activityStarted: type={}", activityType);
    }

    @Override
    public void timerScheduled(FlowableEngineEntityEvent event) {
        DelegateExecution execution = getExecution(event);
        log.debug("*********************** TIMER SCHEDULED *********************** for event: tenantId: {}",
                        execution.getTenantId());
    }

    @Override
    public void timerFired(FlowableEngineEntityEvent event) {
        DelegateExecution execution = getExecution(event);
        log.debug("*********************** TIMER FIRED *********************** for event: tenantId: {}",
                        execution.getTenantId());
    }

    @Override
    protected void engineCreated(FlowableProcessEngineEvent event) {
        log.debug("engineCreated >>> event: {}", event);
    }

    @Override
    protected void engineClosed(FlowableProcessEngineEvent event) {
        log.debug("engineClosed >>> event: {}", event);
    }

    @Override
    public boolean isFailOnException() {
        return false;
    }
}
