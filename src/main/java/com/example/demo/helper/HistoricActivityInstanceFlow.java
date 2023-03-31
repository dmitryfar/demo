package com.example.demo.helper;

import java.io.Serializable;

import org.flowable.bpmn.model.FlowElement;
import org.flowable.bpmn.model.SequenceFlow;
import org.flowable.engine.history.HistoricActivityInstance;

/**
 * Represents historic activity instance with flow
 */
public class HistoricActivityInstanceFlow implements Serializable {

    private static final long serialVersionUID = 1L;

    private HistoricActivityInstance historicActivityInstance;
    private SequenceFlow incomingFlow;
    private FlowElement flowElement;

    public HistoricActivityInstance getHistoricActivityInstance() {
        return historicActivityInstance;
    }

    public void setHistoricActivityInstance(HistoricActivityInstance historicActivityInstance) {
        this.historicActivityInstance = historicActivityInstance;
    }

    public SequenceFlow getIncomingFlow() {
        return incomingFlow;
    }

    public void setIncomingFlow(SequenceFlow incomingFlow) {
        this.incomingFlow = incomingFlow;
    }

    public FlowElement getFlowElement() {
        return flowElement;
    }

    public void setFlowElement(FlowElement flowElement) {
        this.flowElement = flowElement;
    }

    public String getIncomingTransitionId() {
        if (incomingFlow == null) {
            return null;
        }
        return incomingFlow.getId();
    }

    public String getIncomingTransitionName() {
        if (incomingFlow == null) {
            return null;
        }
        return incomingFlow.getName();
    }

    @Override
    public String toString() {
        // (startevent1)--flow1-->
        StringBuilder sb = new StringBuilder();
        if (incomingFlow != null) {
            sb.append(incomingFlow.getSourceRef());
            sb.append("--");
            sb.append(incomingFlow.getId());
            sb.append("-->");
        }
        sb.append(historicActivityInstance.toString());
        return sb.toString();
    }
}
