package com.example.demo.helper.tree;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.flowable.common.engine.api.FlowableException;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.impl.persistence.entity.ExecutionEntity;

/**
 * @author Joram Barrez
 */
public class ExecutionTreeUtil {

    public static ExecutionTree buildExecutionTree(DelegateExecution executionEntity) {

        // Find highest parent
        ExecutionEntity parentExecution = (ExecutionEntity) executionEntity;
        while (parentExecution.getParentId() != null || parentExecution.getSuperExecutionId() != null) {
            if (parentExecution.getParentId() != null) {
                parentExecution = parentExecution.getParent();
            } else {
                parentExecution = parentExecution.getSuperExecution();
            }
        }

        // Collect all child executions now we have the parent
        List<ExecutionEntity> allExecutions = new ArrayList<>();
        allExecutions.add(parentExecution);
        collectChildExecutions(parentExecution, allExecutions);
        return buildExecutionTree(allExecutions);
    }

    public static void collectChildExecutions(ExecutionEntity rootExecutionEntity, List<ExecutionEntity> allExecutions) {
        for (ExecutionEntity childExecutionEntity : rootExecutionEntity.getExecutions()) {
            allExecutions.add(childExecutionEntity);
            collectChildExecutions(childExecutionEntity, allExecutions);
        }

        if (rootExecutionEntity.getSubProcessInstance() != null) {
            allExecutions.add(rootExecutionEntity.getSubProcessInstance());
            collectChildExecutions(rootExecutionEntity.getSubProcessInstance(), allExecutions);
        }
    }

    public static ExecutionTree buildExecutionTree(Collection<ExecutionEntity> executions) {
        ExecutionTree executionTree = new ExecutionTree();

        // Map the executions to their parents. Catch and store the root element (process instance execution) while were at it
        Map<String, List<ExecutionEntity>> parentMapping = new HashMap<>();
        for (ExecutionEntity executionEntity : executions) {
            String parentId = executionEntity.getParentId();

            // Support for call activity
            if (parentId == null) {
                parentId = executionEntity.getSuperExecutionId();
            }

            if (parentId != null) {
                if (!parentMapping.containsKey(parentId)) {
                    parentMapping.put(parentId, new ArrayList<>());
                }
                parentMapping.get(parentId).add(executionEntity);
            } else if (executionEntity.getSuperExecutionId() == null) {
                executionTree.setRoot(new ExecutionTreeNode(executionEntity));
            }
        }

        fillExecutionTree(executionTree, parentMapping);
        return executionTree;
    }

    public static ExecutionTree buildExecutionTreeForProcessInstance(Collection<ExecutionEntity> executions) {
        ExecutionTree executionTree = new ExecutionTree();
        if (executions.size() == 0) {
            return executionTree;
        }

        // Map the executions to their parents. Catch and store the root element (process instance execution) while were at it
        Map<String, List<ExecutionEntity>> parentMapping = new HashMap<>();
        for (ExecutionEntity executionEntity : executions) {
            String parentId = executionEntity.getParentId();

            if (parentId != null) {
                if (!parentMapping.containsKey(parentId)) {
                    parentMapping.put(parentId, new ArrayList<>());
                }
                parentMapping.get(parentId).add(executionEntity);
            } else {
                executionTree.setRoot(new ExecutionTreeNode(executionEntity));
            }
        }

        fillExecutionTree(executionTree, parentMapping);
        return executionTree;
    }

    protected static void fillExecutionTree(ExecutionTree executionTree, Map<String, List<ExecutionEntity>> parentMapping) {
        if (executionTree.getRoot() == null) {
            throw new FlowableException("Programmatic error: the list of passed executions in the argument of the method should contain the process instance execution");
        }

        // Now build the tree, top-down
        LinkedList<ExecutionTreeNode> executionsToHandle = new LinkedList<>();
        executionsToHandle.add(executionTree.getRoot());

        while (!executionsToHandle.isEmpty()) {
            ExecutionTreeNode parentNode = executionsToHandle.pop();
            String parentId = parentNode.getExecutionEntity().getId();
            if (parentMapping.containsKey(parentId)) {
                List<ExecutionEntity> childExecutions = parentMapping.get(parentId);
                List<ExecutionTreeNode> childNodes = new ArrayList<>(childExecutions.size());

                for (ExecutionEntity childExecutionEntity : childExecutions) {

                    ExecutionTreeNode childNode = new ExecutionTreeNode(childExecutionEntity);
                    childNode.setParent(parentNode);

                    childNodes.add(childNode);

                    executionsToHandle.add(childNode);
                }

                parentNode.setChildren(childNodes);

            }
        }
    }
}
