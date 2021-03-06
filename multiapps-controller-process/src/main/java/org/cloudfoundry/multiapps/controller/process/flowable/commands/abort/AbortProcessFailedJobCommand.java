package org.cloudfoundry.multiapps.controller.process.flowable.commands.abort;

import org.apache.commons.lang3.BooleanUtils;
import org.cloudfoundry.multiapps.controller.process.flowable.commands.FlowableCommandExecutor;
import org.cloudfoundry.multiapps.controller.process.util.HistoryUtil;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.flowable.common.engine.impl.interceptor.Command;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.flowable.job.service.impl.persistence.entity.JobEntity;

public class AbortProcessFailedJobCommand implements Command<Object> {

    private final AbortProcessFlowableCommandExecutorFactory abortCommandExecutorFactory;
    private final String jobId;
    private final Command<Object> delegate;

    public AbortProcessFailedJobCommand(AbortProcessFlowableCommandExecutorFactory abortCommandExecutorFactory, String jobId,
                                        Command<Object> delegate) {
        this.abortCommandExecutorFactory = abortCommandExecutorFactory;
        this.jobId = jobId;
        this.delegate = delegate;
    }

    @Override
    public Object execute(CommandContext commandContext) {
        Object result = delegate.execute(commandContext);
        abortProcessIfRequested(commandContext);
        return result;
    }

    private void abortProcessIfRequested(CommandContext commandContext) {
        String processInstanceId = findProcessInstanceId(commandContext);
        if (processInstanceId != null) {
            abortProcessIfRequested(commandContext, processInstanceId);
        }
    }

    private String findProcessInstanceId(CommandContext commandContext) {
        JobEntity job = CommandContextUtil.getJobService(commandContext)
                                          .findJobById(jobId);
        return job != null ? job.getProcessInstanceId() : null;
    }

    private void abortProcessIfRequested(CommandContext commandContext, String processInstanceId) {
        String correlationId = HistoryUtil.getVariableValue(commandContext, processInstanceId, Variables.CORRELATION_ID.getName());
        if (shouldAbortOnError(commandContext, correlationId)) {
            FlowableCommandExecutor abortProcessExecutor = abortCommandExecutorFactory.getExecutor(commandContext, processInstanceId);
            abortProcessExecutor.executeCommand();
        }
    }

    private boolean shouldAbortOnError(CommandContext commandContext, String processInstanceId) {
        Boolean shouldAbortOnError = HistoryUtil.getVariableValue(commandContext, processInstanceId, Variables.ABORT_ON_ERROR.getName());
        return BooleanUtils.toBoolean(shouldAbortOnError);
    }
}
