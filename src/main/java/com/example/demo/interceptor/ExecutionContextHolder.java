package com.example.demo.interceptor;

import java.util.Stack;

import org.flowable.engine.impl.context.ExecutionContext;
import org.flowable.engine.impl.persistence.entity.ExecutionEntity;

/**
 * @author Joram Barrez
 * @author d.farafonov
 */
public class ExecutionContextHolder {

    protected static ThreadLocal<Stack<ExecutionContext>> executionContextStackThreadLocal = new ThreadLocal<>();

    /**
     * Returns current execution
     */
    public static ExecutionEntity getExecution() {
        ExecutionContext executionContext = ExecutionContextHolder.getExecutionContext();
        return executionContext != null ? executionContext.getExecution() : null;
    }

    public static ExecutionContext getExecutionContext() {
        return getStack(executionContextStackThreadLocal).peek();
    }

    public static boolean isExecutionContextActive() {
        Stack<ExecutionContext> stack = executionContextStackThreadLocal.get();
        return stack != null && !stack.isEmpty();
    }

    public static void setExecutionContext(ExecutionEntity execution) {
        getStack(executionContextStackThreadLocal).push(new ExecutionContext(execution));
    }

    public static void removeExecutionContext() {
        getStack(executionContextStackThreadLocal).pop();
    }

    protected static <T> Stack<T> getStack(ThreadLocal<Stack<T>> threadLocal) {
        Stack<T> stack = threadLocal.get();
        if (stack == null) {
            stack = new Stack<>();
            threadLocal.set(stack);
        }
        return stack;
    }

}
