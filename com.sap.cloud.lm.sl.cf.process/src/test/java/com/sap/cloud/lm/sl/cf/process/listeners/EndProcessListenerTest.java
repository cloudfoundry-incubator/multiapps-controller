package com.sap.cloud.lm.sl.cf.process.listeners;

import com.sap.cloud.lm.sl.cf.process.Constants;
import org.flowable.engine.delegate.DelegateExecution;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.sap.cloud.lm.sl.cf.process.mock.MockDelegateExecution;
import com.sap.cloud.lm.sl.cf.process.util.OperationInFinalStateHandler;
import com.sap.cloud.lm.sl.cf.web.api.model.Operation;

public class EndProcessListenerTest {

    private final OperationInFinalStateHandler eventHandler = Mockito.mock(OperationInFinalStateHandler.class);
    private final DelegateExecution context = MockDelegateExecution.createSpyInstance();

    @Test
    public void testNotifyInternal() {
        EndProcessListener endProcessListener = new EndProcessListener(eventHandler);
        // set the process as root process
        context.setVariable(Constants.VAR_CORRELATION_ID, context.getProcessInstanceId());
        endProcessListener.notifyInternal(context);
        Mockito.verify(eventHandler)
               .handle(context, Operation.State.FINISHED);
    }

}
