package com.example.demo.helper;

import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;

import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.FlowElement;
import org.flowable.bpmn.model.FlowNode;
import org.flowable.bpmn.model.SequenceFlow;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.api.FlowableObjectNotFoundException;
import org.flowable.common.engine.impl.interceptor.Command;
import org.flowable.common.engine.impl.interceptor.CommandConfig;
import org.flowable.common.engine.impl.interceptor.CommandExecutor;
import org.flowable.common.engine.impl.runtime.Clock;
import org.flowable.engine.HistoryService;
import org.flowable.engine.ProcessEngine;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.debug.ExecutionTree;
import org.flowable.engine.debug.ExecutionTreeUtil;
import org.flowable.engine.history.HistoricActivityInstance;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.history.HistoricProcessInstanceQuery;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.engine.impl.persistence.entity.ExecutionEntity;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.flowable.engine.runtime.ProcessInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class FlowableHelper {
	private static final Logger LOGGER = LoggerFactory.getLogger(FlowableHelper.class);

	private static final String DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX";
	
	@Autowired
	private ProcessEngine processEngine;
	@Autowired
	private RepositoryService repositoryService;
	@Autowired
	private RuntimeService runtimeService;
	@Autowired
	private HistoryService historyService;

    private static FlowableHelper instance;

    @PostConstruct
    private void init() {
        instance = this;
    }

    public static FlowableHelper getInstance() {
        return instance;
    }

	/**
	 * Represents current process state with highlighted flows and activities.
	 *
	 */
	public static class ProcessState {
		String processInstanceId;
		String processDefinitionId;
		Set<String> completedActivities = new LinkedHashSet<>();
		Set<String> currentActivities = new HashSet<>();
		List<String> completedSequenceFlows = new ArrayList<>();

		public String getProcessInstanceId() {
			return processInstanceId;
		}

		public String getProcessDefinitionId() {
			return processDefinitionId;
		}

		public Set<String> getCompletedActivities() {
			return completedActivities;
		}

		public Set<String> getCurrentActivities() {
			return currentActivities;
		}

		public List<String> getCompletedSequenceFlows() {
			return completedSequenceFlows;
		}

		@Override
		public String toString() {
			return "ProcessState [processInstanceId=" + processInstanceId + ", processDefinitionId="
					+ processDefinitionId + ", completedActivities=" + completedActivities + ", currentActivities="
					+ currentActivities + ", completedSequenceFlows=" + completedSequenceFlows + "]";
		}
	}

	/**
	 * Returns Process Instance state:
	 * <ul>
	 * <li>completed activities</li>
	 * <li>current activities</li>
	 * <li>completed sequence flows</li>
	 * </ul>
	 * 
	 * Process Instance can be completed.
	 * 
	 * @param processInstanceId
	 * @return
	 */
	public ProcessState getProcessState(String processInstanceId) {

		String processDefinitionId = null;
		try {
			ProcessInstance processInstance = getProcessInstanceFromRequest(processInstanceId);
			processDefinitionId = processInstance.getProcessDefinitionId();
		} catch (FlowableObjectNotFoundException e) {
			HistoricProcessInstance historicProcessInstance = getHistoricProcessInstanceFromRequest(processInstanceId);
			processDefinitionId = historicProcessInstance.getProcessDefinitionId();
		}

		BpmnModel bpmnModel = repositoryService.getBpmnModel(processDefinitionId);

		if (bpmnModel == null) {
			throw new FlowableException("Process definition could not be found with id " + processDefinitionId);
		}

		// Fetch process-instance activities
		List<HistoricActivityInstance> activityInstances = historyService.createHistoricActivityInstanceQuery()
				.processInstanceId(processInstanceId).orderByHistoricActivityInstanceStartTime().asc().list();

		// Gather completed flows
		List<HistoricActivityInstanceFlow> historicActivityInstanceFlows = gatherCompletedFlows(bpmnModel,
				activityInstances);
		List<String> completedFlows = historicActivityInstanceFlows.stream()
                        .filter(hf -> Objects.nonNull(hf.getIncomingFlow())).map(hf -> hf.getIncomingFlow().getId())
				.collect(Collectors.toList());

		Set<String> completedActivityInstances = new LinkedHashSet<>();
		Set<String> currentElements = new HashSet<>();
		if (activityInstances != null && !activityInstances.isEmpty()) {
			for (HistoricActivityInstance activityInstance : activityInstances) {
				if (activityInstance.getEndTime() != null) {
					completedActivityInstances.add(activityInstance.getActivityId());
				} else {
					currentElements.add(activityInstance.getActivityId());
				}
			}
		}

		Set<String> completedElements = new HashSet<>(completedActivityInstances);
		completedElements.addAll(completedFlows);

		ProcessState processState = new ProcessState();

		processState.processInstanceId = processInstanceId;
		processState.processDefinitionId = processDefinitionId;

		processState.completedActivities.addAll(completedActivityInstances);
		processState.currentActivities.addAll(currentElements);
		processState.completedSequenceFlows.addAll(completedFlows);

		return processState;
	}

	/**
	 * Returns historic activity flows.
	 * 
	 * @param processInstanceId
	 * @return
	 */
	public List<HistoricActivityInstanceFlow> getProcessFlow(String processInstanceId) {
		String processDefinitionId = null;
		try {
			ProcessInstance processInstance = getProcessInstanceFromRequest(processInstanceId);
			processDefinitionId = processInstance.getProcessDefinitionId();
		} catch (FlowableObjectNotFoundException e) {
			HistoricProcessInstance historicProcessInstance = getHistoricProcessInstanceFromRequest(processInstanceId);
			processDefinitionId = historicProcessInstance.getProcessDefinitionId();
		}

		BpmnModel bpmnModel = repositoryService.getBpmnModel(processDefinitionId);

        if (bpmnModel == null /* || bpmnModel.getLocationMap().isEmpty() */) {
			throw new FlowableException("Process definition could not be found with id " + processDefinitionId);
		}

		// Fetch process-instance activities
		List<HistoricActivityInstance> activityInstances = historyService.createHistoricActivityInstanceQuery()
				.processInstanceId(processInstanceId).orderByHistoricActivityInstanceStartTime().asc().list();

		// Gather completed flows
		return gatherCompletedFlows(bpmnModel, activityInstances);
	}

	/**
	 * Returns completed historic activity flows. Can contain elements with null-incoming flows, i.e. StartEvent
	 * 
	 * @param pojoModel
	 * @param historicActivityInstances
	 * @return
	 */
	protected List<HistoricActivityInstanceFlow> gatherCompletedFlows(BpmnModel pojoModel,
			List<HistoricActivityInstance> historicActivityInstances) {

		List<HistoricActivityInstanceFlow> historicActivityInstanceFlows = new ArrayList<>();

		/*
		// @formatter:off
		for (HistoricActivityInstance historicActivityInstance : historicActivityInstances) {
			String activityId = historicActivityInstance.getActivityId();
			FlowElement activity = pojoModel.getFlowElement(activityId);
			if (activity instanceof FlowNode) {
				String type = historicActivityInstance.getActivityType();
				List<SequenceFlow> incomingFlows = ((FlowNode) activity).getIncomingFlows();
				for (SequenceFlow flow : incomingFlows) {
					String sourceId = flow.getSourceRef();

					HistoricActivityInstance sourceHistoricActivityInstance = null;
					if (incomingFlows.size() > 1) {
						sourceHistoricActivityInstance = getHistoricActivityById(historicActivityInstances, sourceId,
								historicActivityInstance.getExecutionId());
					} else {
						sourceHistoricActivityInstance = getHistoricActivityById(historicActivityInstances, sourceId,
								null);
					}
					if (sourceHistoricActivityInstance != null) {
						// historicActivityInstanceFlow.incomingFlow = flow;
					}
				}
			}
		}
		// @formatter:on
		*/

		for (HistoricActivityInstance historicActivityInstance : historicActivityInstances) {
			String activityId = historicActivityInstance.getActivityId();
			FlowElement activity = pojoModel.getFlowElement(activityId);
			if (activity instanceof FlowNode) {
				HistoricActivityInstanceFlow historicActivityInstanceFlow = new HistoricActivityInstanceFlow();
                historicActivityInstanceFlow.setHistoricActivityInstance(historicActivityInstance);
                historicActivityInstanceFlow.setFlowElement(activity);

				LOGGER.debug("activity: {}", activity);

				String type = historicActivityInstance.getActivityType();
				List<SequenceFlow> incomingFlows = ((FlowNode) activity).getIncomingFlows();
				for (SequenceFlow flow : incomingFlows) {
					String sourceId = flow.getSourceRef();
					LOGGER.debug("\t incoming flow: {}", flow);

					HistoricActivityInstance sourceHistoricActivityInstance = null;
					// Find in history with the same execution id, used for joined parallel gateways.
					// Multiple incoming paths of execution have different executionIds and
					// there is many instances of the Parallel Gateway activity in the
					// history. Because of that we should find activity in the same execution.
					// It is only for joined executions (incoming transitions count > 1)
					if (("parallelGateway".equals(type) || "inclusiveGateway".equals(type))
							&& incomingFlows.size() > 1) {
						sourceHistoricActivityInstance = getHistoricActivityById(historicActivityInstances, sourceId,
								historicActivityInstance.getExecutionId());
					} else {
						sourceHistoricActivityInstance = getHistoricActivityById(historicActivityInstances, sourceId,
								null);
					}
					if (sourceHistoricActivityInstance != null) {
                        historicActivityInstanceFlow.setIncomingFlow(flow);
					}

				}
				historicActivityInstanceFlows.add(historicActivityInstanceFlow);
			}
		}

		// TODO: check subprocess

		return historicActivityInstanceFlows;
	}

	/**
	 * Returns historic activity instance by activity id and specified execution id
	 *
	 * @param historicActivityInstances
	 * @param activityId
	 * @param executionId
	 * @return
	 */
	private HistoricActivityInstance getHistoricActivityById(List<HistoricActivityInstance> historicActivityInstances,
			String activityId, String executionId) {
		for (HistoricActivityInstance historicActivityInstance : historicActivityInstances) {
			if (Objects.equals(historicActivityInstance.getActivityId(), activityId) && (executionId == null
					|| Objects.equals(executionId, historicActivityInstance.getExecutionId()))) {
				return historicActivityInstance;
			}
		}
		return null;
	}

	protected ProcessInstance getProcessInstanceFromRequest(String processInstanceId) {
		ProcessInstance processInstance = runtimeService.createProcessInstanceQuery()
				.processInstanceId(processInstanceId).singleResult();
		if (processInstance == null) {
			throw new FlowableObjectNotFoundException(
					"Could not find a process instance with id '" + processInstanceId + "'.");
		}
		return processInstance;
	}

	protected HistoricProcessInstance getHistoricProcessInstanceFromRequest(String processInstanceId) {
		HistoricProcessInstance processInstance = historyService.createHistoricProcessInstanceQuery()
				.processInstanceId(processInstanceId).singleResult();
		if (processInstance == null) {
			throw new FlowableObjectNotFoundException(
					"Could not find a historic process instance with id '" + processInstanceId + "'.");
		}
		return processInstance;
	}

	public <T> T executeCommand(Command<T> command) {
		CommandExecutor commandExecutor = ((ProcessEngineConfigurationImpl) processEngine
				.getProcessEngineConfiguration()).getCommandExecutor();
		CommandConfig commandConfig = commandExecutor.getDefaultConfig().transactionRequiresNew();
		return processEngine.getProcessEngineConfiguration().getManagementService().executeCommand(commandConfig,
				command);
	}

    public String printExecutionTree(String executionId) {
        String tree = executeCommand((ctx) -> {
            ExecutionEntity executionEntity = CommandContextUtil.getExecutionEntityManager(ctx).findById(executionId);
            if (executionEntity == null) {
                return "Execution " + executionId + " not found";
            }
            ExecutionTree executionTree = ExecutionTreeUtil
                            .buildExecutionTree(executionEntity);
            return executionTree.toString();
        });
        LOGGER.debug("executionTree:\n{}", tree);
        return tree;
    }

	public static List<ExecutionEntity> getExecutionTree(ExecutionEntity execution) {
		List<ExecutionEntity> list = new ArrayList<ExecutionEntity>();
		ExecutionEntity superExecution = execution;
		do {
			list.add(execution);
			superExecution = execution.getSuperExecution();
			ExecutionEntity parentExecution = execution.getParent();
			if (superExecution == null && parentExecution != null) {
				superExecution = parentExecution;
			}
			if (superExecution != null) {
				execution = superExecution;
			}
		} while (superExecution != null);

		return list;
	}

	public List<String> getHistoricProcessInstanceTreeIds(String processInstanceId) {
		// go through subprocesses
		HistoricProcessInstanceQuery query = historyService.createHistoricProcessInstanceQuery();
		query.superProcessInstanceId(processInstanceId);

		List<String> processIds = new ArrayList<String>();
		processIds.add(processInstanceId);

		for (HistoricProcessInstance subProcess : query.list()) {
			List<String> subProcessIds = getHistoricProcessInstanceTreeIds(subProcess.getId());
			processIds.addAll(subProcessIds);
		}

		return processIds;
	}

    public Clock getClock() {
        return processEngine.getProcessEngineConfiguration().getClock();
    }

    /** Move flowable timer forward to time specified in ISO duration format */
    public void clockOffset(String duration) {
        Duration d = Duration.parse(duration);
        Long millis = d.toMillis();
        Clock clock = getClock();
        Date date = clock.getCurrentTime();
        Calendar newTime = Calendar.getInstance();
        newTime.setTimeInMillis(date.getTime());
        newTime.add(Calendar.MILLISECOND, millis.intValue());
        clock.setCurrentCalendar(newTime);
        LOGGER.info("Prev time: {} > Current time: {}", date, clock.getCurrentTime());
    }

    public String getCurrentTimeString() {
        Date currentDate = getClock().getCurrentTime();
        ZonedDateTime zonedDateTime = currentDate.toInstant().atZone(getClock().getCurrentTimeZone().toZoneId());
        return getFormattedDate(zonedDateTime);
    }
    
    public static String getFormattedDate(ZonedDateTime zonedDateTime){
        Date date = Date.from(zonedDateTime.toInstant());
        return new SimpleDateFormat(DATE_FORMAT).format(date);
    }
}
