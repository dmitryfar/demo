package com.example.demo.interceptor;

import org.flowable.common.engine.impl.agenda.AgendaOperationRunner;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.engine.impl.agenda.AbstractOperation;
import org.flowable.engine.impl.interceptor.CommandInvoker;

/**
 * A customized version of the default {@link CommandInvoker} for use with CDI.
 * 
 * The Flowable-CDI integration builds upon the availability of the current execution in a thread local 'execution
 * context'. As this has a (very minimal) impact on performance, this thread local is
 * not set by the default {@link CommandInvoker} and thus this customized version is needed.
 * 
 * @author jbarrez
 */
public class CdiCommandInvoker extends CommandInvoker {

    public CdiCommandInvoker(AgendaOperationRunner agendaOperationRunner) {
        super(agendaOperationRunner);
    }

    @Override
    public void executeOperation(CommandContext commandContext, Runnable runnable) {

        boolean executionContextSet = false;
        if (runnable instanceof AbstractOperation) {
            AbstractOperation operation = (AbstractOperation) runnable;
            if (operation.getExecution() != null) {
                ExecutionContextHolder.setExecutionContext(operation.getExecution());
                executionContextSet = true;
            }
        }

        try {
            super.executeOperation(commandContext, runnable);
        } finally {
            if (executionContextSet) {
                ExecutionContextHolder.removeExecutionContext();
            }
        }

    }

}
