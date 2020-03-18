package com.sap.cloud.lm.sl.cf.process.steps;

import java.text.MessageFormat;
import java.util.Collections;

import javax.inject.Named;

import org.cloudfoundry.client.lib.domain.CloudTask;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;

import com.sap.cloud.lm.sl.cf.process.Messages;
import com.sap.cloud.lm.sl.cf.process.util.TaskHookParser;
import com.sap.cloud.lm.sl.cf.process.variables.Variables;
import com.sap.cloud.lm.sl.mta.model.Hook;

@Named("determineTasksFromHookStep")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class DetermineTasksFromHookStep extends SyncFlowableStep {

    protected final TaskHookParser hookParser = new TaskHookParser();

    @Override
    protected StepPhase executeStep(ExecutionWrapper execution) {
        Hook hook = execution.getVariable(Variables.HOOK_FOR_EXECUTION);

        getStepLogger().info(Messages.EXECUTING_HOOK_0, hook.getName());

        CloudTask task = hookParser.parse(hook);
        StepsUtil.setTasksToExecute(execution.getContext(), Collections.singletonList(task));

        return StepPhase.DONE;
    }

    @Override
    protected String getStepErrorMessage(ExecutionWrapper execution) {
        return MessageFormat.format(Messages.ERROR_EXECUTING_HOOK, execution.getVariable(Variables.HOOK_FOR_EXECUTION)
                                                                            .getName());
    }

}
