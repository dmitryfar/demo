package com.example.demo.api;

import java.io.InputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletResponse;

import com.example.demo.helper.FlowableHelper;
import com.example.demo.helper.FlowableHelper.ProcessState;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.ExtensionAttribute;
import org.flowable.cmmn.engine.impl.process.ProcessInstanceService;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.api.FlowableObjectNotFoundException;
import org.flowable.engine.HistoryService;
import org.flowable.engine.ProcessEngineConfiguration;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.image.ProcessDiagramGenerator;
import org.flowable.rest.service.api.runtime.process.BaseProcessInstanceResource;
import org.flowable.rest.service.api.runtime.process.ProcessInstanceResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/pm/process-instance")
@Slf4j
public class PMProcessInstanceHighlightsResource extends BaseProcessInstanceResource {

	@Autowired
	private RepositoryService repositoryService;
	@Autowired
	private RuntimeService runtimeService;
	@Autowired
	private HistoryService historyService;
	@Autowired
	private FlowableHelper flowableHelper;

	@Autowired
	protected ProcessEngineConfiguration processEngineConfiguration;

	@Autowired
	protected ObjectMapper objectMapper;

	@RequestMapping(method= RequestMethod.GET, produces=MediaType.APPLICATION_JSON_VALUE)
    public List<ProcessInstanceResponse> getProcesses() {
	    List<ProcessInstance> list = runtimeService.createProcessInstanceQuery().active().list();
	    return restResponseFactory.createProcessInstanceResponseList(list);
	}
	
	/**
	 * Get process flows and active activities
	 * 
	 * @param processInstanceId
	 * @return
	 */
	@RequestMapping(value = "/{processInstanceId}/highlights", method = RequestMethod.GET, produces = "application/json")
	public ObjectNode getHighlighted(@PathVariable String processInstanceId) {

		// String processInstanceId = String.valueOf(pathVariables.get("processInstanceId"));

		ObjectNode responseJSON = objectMapper.createObjectNode();

		responseJSON.put("processInstanceId", processInstanceId);

		ArrayNode activitiesArray = objectMapper.createArrayNode();
		ArrayNode flowsArray = objectMapper.createArrayNode();

		try {
			ProcessState processState = flowableHelper.getProcessState(processInstanceId);
			responseJSON.put("processDefinitionId", processState.getProcessDefinitionId());

			for (String activityId : processState.getCurrentActivities()) {
				activitiesArray.add(activityId);
			}

			for (String flow : processState.getCompletedSequenceFlows()) {
				flowsArray.add(flow);
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

		responseJSON.set("activities", activitiesArray);
		responseJSON.set("flows", flowsArray);

		return responseJSON;
	}

	@RequestMapping(value = "/{processInstanceId}/diagram", method = RequestMethod.GET)
	public ResponseEntity<byte[]> getProcessInstanceDiagram(@PathVariable String processInstanceId,
			HttpServletResponse response) {
		String processDefinitionId = null;
		try {
			ProcessInstance processInstance = getProcessInstanceFromRequest(processInstanceId);
			processDefinitionId = processInstance.getProcessDefinitionId();
		} catch (FlowableObjectNotFoundException e) {
			HistoricProcessInstance historicProcessInstance = getHistoricProcessInstanceFromRequest(processInstanceId);
			processDefinitionId = historicProcessInstance.getProcessDefinitionId();
		}

		ProcessDefinition pde = repositoryService.getProcessDefinition(processDefinitionId);

		if (pde != null && pde.hasGraphicalNotation()) {
			BpmnModel bpmnModel = repositoryService.getBpmnModel(pde.getId());
			ProcessDiagramGenerator diagramGenerator = processEngineConfiguration.getProcessDiagramGenerator();

			ProcessState processState = flowableHelper.getProcessState(processInstanceId);

			List<String> highLightedActivities = new ArrayList<>(processState.getCurrentActivities());
			List<String> highLightedFlows = processState.getCompletedSequenceFlows();

			String definitionsAttributeValue = bpmnModel.getDefinitionsAttributeValue("meridian",
					"processDefinitionId");
			if (StringUtils.isEmpty(definitionsAttributeValue)) {
				ExtensionAttribute attribute = new ExtensionAttribute("processDefinitionId");
				attribute.setNamespace("meridian");
				attribute.setValue(pde.getId());
				bpmnModel.addDefinitionsAttribute(attribute);
			}

			InputStream resource = diagramGenerator.generateDiagram(bpmnModel, "png", highLightedActivities,
					highLightedFlows, processEngineConfiguration.getActivityFontName(),
					processEngineConfiguration.getLabelFontName(), processEngineConfiguration.getAnnotationFontName(),
					processEngineConfiguration.getClassLoader(), 1.0, false);
			HttpHeaders responseHeaders = new HttpHeaders();
			responseHeaders.set("Content-Type", "image/png");
			// process can
			responseHeaders.set("Cache-Control", "no-cache, no-store, must-revalidate");
			responseHeaders.set("Pragma", "no-cache");
			responseHeaders.set("Expires", "0");
			try {
				return new ResponseEntity<byte[]>(IOUtils.toByteArray(resource), responseHeaders, HttpStatus.OK);
			} catch (Exception e) {
				throw new FlowableIllegalArgumentException("Error exporting diagram", e);
			}

		} else {
			throw new FlowableIllegalArgumentException(
					"Process instance with id '" + processInstanceId + "' has no graphical notation defined.");
		}
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

	@RequestMapping(value = "/{processInstanceId}/model-json", method = RequestMethod.GET, produces = "application/json")
	public ProcessState getModelJSON(@PathVariable String processInstanceId) {
		return flowableHelper.getProcessState(processInstanceId);
	}

	@RequestMapping(value = "/{processInstanceId}/tree", method = RequestMethod.GET, produces = "text/plain;charset=UTF-8")
	public String getTree(@PathVariable String processInstanceId) {
	    return flowableHelper.printExecutionTree(processInstanceId);
	}

}
