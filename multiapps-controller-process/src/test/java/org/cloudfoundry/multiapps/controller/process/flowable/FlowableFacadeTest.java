package org.cloudfoundry.multiapps.controller.process.flowable;

import org.flowable.engine.ProcessEngine;
import org.flowable.engine.ProcessEngineConfiguration;
import org.flowable.job.service.impl.asyncexecutor.DefaultAsyncJobExecutor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

class FlowableFacadeTest {

    private FlowableFacade flowableFacade;

    @Mock
    DefaultAsyncJobExecutor mockedAsyncExecutor;
    @Mock
    ProcessEngine mockedProcessEngine;
    @Mock
    ProcessEngineConfiguration mockedProcessEngineConfiguration;

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this)
                          .close();

        Mockito.when(mockedProcessEngineConfiguration.getAsyncExecutor())
               .thenReturn(mockedAsyncExecutor);
        Mockito.when(mockedProcessEngine.getProcessEngineConfiguration())
               .thenReturn(mockedProcessEngineConfiguration);

        flowableFacade = new FlowableFacade(mockedProcessEngine);
    }

    @Test
    void testAsyncExecutorMethodsAreCalled() {
        flowableFacade.shutdownJobExecutor();
        Mockito.verify(mockedAsyncExecutor, Mockito.times(1))
               .shutdown();
    }

}
