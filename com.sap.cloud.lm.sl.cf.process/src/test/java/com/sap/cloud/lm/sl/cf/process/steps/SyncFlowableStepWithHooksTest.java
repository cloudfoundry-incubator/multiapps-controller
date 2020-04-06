package com.sap.cloud.lm.sl.cf.process.steps;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import org.flowable.engine.delegate.DelegateExecution;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import com.sap.cloud.lm.sl.cf.client.lib.domain.CloudApplicationExtended;
import com.sap.cloud.lm.sl.cf.client.lib.domain.ImmutableCloudApplicationExtended;
import com.sap.cloud.lm.sl.cf.core.model.HookPhase;
import com.sap.cloud.lm.sl.cf.process.mock.MockDelegateExecution;
import com.sap.cloud.lm.sl.cf.process.steps.SyncFlowableStepWithHooks.ModuleHooksAggregator;
import com.sap.cloud.lm.sl.cf.process.util.StepLogger;
import com.sap.cloud.lm.sl.cf.process.variables.Variables;
import com.sap.cloud.lm.sl.common.util.MapUtil;
import com.sap.cloud.lm.sl.mta.model.DeploymentDescriptor;
import com.sap.cloud.lm.sl.mta.model.Hook;
import com.sap.cloud.lm.sl.mta.model.Module;

public class SyncFlowableStepWithHooksTest {

    @Mock
    private ModuleHooksAggregator moduleHooksAggregatorMock;

    private final DelegateExecution execution = MockDelegateExecution.createSpyInstance();
    private final ProcessContext context = new ProcessContext(execution, Mockito.mock(StepLogger.class), null);

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testWithHooksForPreExecuteStepPhase() throws Exception {
        context.setVariable(Variables.STEP_PHASE, StepPhase.EXECUTE);
        Module module = Module.createV3()
                              .setName("test")
                              .setHooks(Collections.singletonList(Hook.createV3()
                                                                      .setType("tasks")
                                                                      .setPhases(Collections.singletonList("application.before-start"))));
        context.setVariable(Variables.MODULE_TO_DEPLOY, module);

        List<Hook> moduleHooks = module.getHooks();

        Mockito.when(moduleHooksAggregatorMock.aggregateHooks(HookPhase.APPLICATION_AFTER_STOP_LIVE))
               .thenReturn(moduleHooks);

        new SyncFlowableStepWithHooksMock().executeStep(context);

        Mockito.verify(moduleHooksAggregatorMock)
               .aggregateHooks(HookPhase.APPLICATION_AFTER_STOP_LIVE);

    }

    @Test
    public void testWithHooksForPostExecuteStepPhase() throws Exception {
        context.setVariable(Variables.STEP_PHASE, StepPhase.DONE);

        Module module = Module.createV3()
                              .setName("test")
                              .setHooks(Collections.singletonList(Hook.createV3()
                                                                      .setType("tasks")
                                                                      .setPhases(Collections.singletonList("application.before-stop"))));
        context.setVariable(Variables.MODULE_TO_DEPLOY, module);

        List<Hook> moduleHooks = module.getHooks();

        Mockito.when(moduleHooksAggregatorMock.aggregateHooks(HookPhase.APPLICATION_BEFORE_STOP_IDLE))
               .thenReturn(moduleHooks);

        new SyncFlowableStepWithHooksMock().executeStep(context);

        Mockito.verify(moduleHooksAggregatorMock)
               .aggregateHooks(HookPhase.APPLICATION_BEFORE_STOP_IDLE);

    }

    @Test
    public void testWithNoHooksForCurrentStep() throws Exception {
        context.setVariable(Variables.STEP_PHASE, StepPhase.EXECUTE);

        Module module = Module.createV3()
                              .setName("test")
                              .setHooks(Collections.emptyList());

        context.setVariable(Variables.MODULE_TO_DEPLOY, module);

        Mockito.when(moduleHooksAggregatorMock.aggregateHooks(HookPhase.APPLICATION_AFTER_STOP_LIVE))
               .thenReturn(Collections.emptyList());

        new SyncFlowableStepWithHooksMock().executeStep(new ProcessContext(execution, Mockito.mock(StepLogger.class), null));

        Mockito.verify(moduleHooksAggregatorMock)
               .aggregateHooks(HookPhase.APPLICATION_AFTER_STOP_LIVE);

    }

    @Test
    public void testWithNoModuleToDeployAndNoDeploymentDescriptor() throws Exception {
        new SyncFlowableStepWithHooksMock().executeStep(new ProcessContext(execution, Mockito.mock(StepLogger.class), null));

        Mockito.verifyNoInteractions(moduleHooksAggregatorMock);
    }

    @Test
    public void testWithDeploymentDescriptorAndNoModuleNameInApplication() throws Exception {
        context.setVariable(Variables.STEP_PHASE, StepPhase.EXECUTE);

        Module module = Module.createV3()
                              .setName("test")
                              .setHooks(Collections.emptyList());

        prepareDeploymentDescriptor(Collections.singletonList(module));
        prepareApplication("test", null);

        Mockito.when(moduleHooksAggregatorMock.aggregateHooks(HookPhase.APPLICATION_BEFORE_STOP_IDLE))
               .thenReturn(Collections.emptyList());

        new SyncFlowableStepWithHooksMock("test").executeStep(new ProcessContext(execution, Mockito.mock(StepLogger.class), null));

        Mockito.verify(moduleHooksAggregatorMock)
               .aggregateHooks(HookPhase.APPLICATION_BEFORE_STOP_IDLE);

    }

    @Test
    public void testWithDeploymentDescriptorAndModuleNameInApplication() throws Exception {
        context.setVariable(Variables.STEP_PHASE, StepPhase.EXECUTE);
        context.setVariable(Variables.MTA_MAJOR_SCHEMA_VERSION, 3);

        Module module = Module.createV3()
                              .setName("test")
                              .setHooks(Collections.emptyList());

        prepareDeploymentDescriptor(Collections.singletonList(module));
        prepareApplication("foo-application", "test");

        Mockito.when(moduleHooksAggregatorMock.aggregateHooks(HookPhase.APPLICATION_BEFORE_STOP_IDLE))
               .thenReturn(Collections.emptyList());

        new SyncFlowableStepWithHooksMock().executeStep(new ProcessContext(execution, Mockito.mock(StepLogger.class), null));

        Mockito.verify(moduleHooksAggregatorMock)
               .aggregateHooks(HookPhase.APPLICATION_BEFORE_STOP_IDLE);

    }

    @Test
    public void testWithDeploymentDescriptorWithNoModulesContainingModuleName() throws Exception {
        context.setVariable(Variables.STEP_PHASE, StepPhase.EXECUTE);
        context.setVariable(Variables.MTA_MAJOR_SCHEMA_VERSION, 3);

        prepareDeploymentDescriptor(Arrays.asList(Module.createV3()
                                                        .setName("foo"),
                                                  Module.createV3()
                                                        .setName("bar")));
        prepareApplication("foo-application", "test");

        Mockito.when(moduleHooksAggregatorMock.aggregateHooks(HookPhase.APPLICATION_BEFORE_STOP_IDLE))
               .thenReturn(Collections.emptyList());

        new SyncFlowableStepWithHooksMock().executeStep(new ProcessContext(execution, Mockito.mock(StepLogger.class), null));

        Mockito.verifyNoInteractions(moduleHooksAggregatorMock);
    }

    @Test
    public void testWithDeploymentDescriptorAndModuleNamesWhichHaveTheSamePrefixForNames() throws Exception {
        context.setVariable(Variables.STEP_PHASE, StepPhase.EXECUTE);
        context.setVariable(Variables.MTA_MAJOR_SCHEMA_VERSION, 3);

        Module moduleWithHooks = Module.createV3()
                                       .setName("test")
                                       .setHooks(Collections.emptyList());

        prepareDeploymentDescriptor(Arrays.asList(moduleWithHooks, Module.createV3()
                                                                         .setName("test-1"),
                                                  Module.createV3()
                                                        .setName("test-bar")));
        prepareApplication("foo-application", null);

        Mockito.when(moduleHooksAggregatorMock.aggregateHooks(HookPhase.APPLICATION_BEFORE_STOP_IDLE))
               .thenReturn(Collections.emptyList());

        new SyncFlowableStepWithHooksMock("test").executeStep(new ProcessContext(execution, Mockito.mock(StepLogger.class), null));

        Mockito.verify(moduleHooksAggregatorMock)
               .aggregateHooks(HookPhase.APPLICATION_BEFORE_STOP_IDLE);

    }

    private void prepareApplication(String name, String moduleName) {
        CloudApplicationExtended cloudApplicationExtended = ImmutableCloudApplicationExtended.builder()
                                                                                             .name(name)
                                                                                             .moduleName(moduleName)
                                                                                             .build();
        context.setVariable(Variables.APP_TO_PROCESS, cloudApplicationExtended);
    }

    private void prepareDeploymentDescriptor(List<Module> modules) {
        DeploymentDescriptor deploymentDescriptorMock = DeploymentDescriptor.createV3()
                                                                            .setModules(modules);
        context.setVariable(Variables.COMPLETE_DEPLOYMENT_DESCRIPTOR, deploymentDescriptorMock);
    }

    private class SyncFlowableStepWithHooksMock extends SyncFlowableStepWithHooks {

        private final String moduleName;

        public SyncFlowableStepWithHooksMock() {
            this(null);
        }

        public SyncFlowableStepWithHooksMock(String moduleName) {
            this.moduleName = moduleName;
        }

        @Override
        protected StepPhase executeStepInternal(ProcessContext context) {
            return StepPhase.DONE;
        }

        @Override
        protected ModuleHooksAggregator getModuleHooksAggregator(DelegateExecution execution, Module moduleToDeploy) {
            return moduleHooksAggregatorMock;
        }

        @Override
        protected HookPhase getHookPhaseBeforeStep(DelegateExecution execution) {
            return HookPhase.APPLICATION_AFTER_STOP_LIVE;
        }

        @Override
        protected HookPhase getHookPhaseAfterStep(DelegateExecution execution) {
            return HookPhase.APPLICATION_BEFORE_STOP_IDLE;
        }

        @Override
        protected String getModuleName(CloudApplicationExtended cloudApplication) {
            return moduleName;
        }

        @Override
        protected String getStepErrorMessage(ProcessContext context) {
            return "generic error message";
        }

    }

    public static class ModuleHooksAggregatorTest {

        private static final String DEFAULT_MODULE_NAME = "testModuleName";

        private final DelegateExecution execution = MockDelegateExecution.createSpyInstance();

        @Test
        public void withNoAlreadyExecutedHooksAndWithHooksForCurrentStepPhase() {
            StepsUtil.setExecutedHooksForModule(execution, DEFAULT_MODULE_NAME, Collections.emptyMap());
            Module module = prepareModule(2, "application.after-stop.live");

            ModuleHooksAggregator aggregator = getModuleHooksAggregator(module);
            List<Hook> aggregatedHooks = aggregator.aggregateHooks(HookPhase.APPLICATION_AFTER_STOP_LIVE);

            Assertions.assertEquals(2, aggregatedHooks.size());
            Assertions.assertEquals(module.getHooks(), aggregatedHooks);

            Map<String, List<String>> executedHooks = StepsUtil.getExecutedHooksForModule(execution, DEFAULT_MODULE_NAME);
            Map<String, List<String>> expectedResult = new HashMap<>();
            buildResultForAggregatedHook(aggregatedHooks, expectedResult);
            Assertions.assertEquals(expectedResult, executedHooks);
        }

        private ModuleHooksAggregator getModuleHooksAggregator(Module module) {
            return new ModuleHooksAggregator(execution, module);
        }

        private Module prepareModule(int numberOfHooks, String phase) {
            return Module.createV3()
                         .setName(DEFAULT_MODULE_NAME)
                         .setHooks(getHooks(numberOfHooks, phase));
        }

        private List<Hook> getHooks(int numberOfHooks, String phase) {
            List<Hook> result = new ArrayList<>();
            IntStream.range(0, numberOfHooks)
                     .forEach(currentHookIndex -> {
                         Hook hook = Hook.createV3()
                                         .setName("hook" + currentHookIndex)
                                         .setPhases(Collections.singletonList(phase));
                         result.add(hook);
                     });
            return result;
        }

        @Test
        public void withAlreadyExecutedHooksAndNoHooksForCurrentStepPhase() {
            Hook executedHook = getExecutedHook();
            StepsUtil.setExecutedHooksForModule(execution, DEFAULT_MODULE_NAME,
                                                MapUtil.asMap(executedHook.getName(), executedHook.getPhases()));

            Module module = prepareModule(0, null);
            ModuleHooksAggregator aggregator = getModuleHooksAggregator(module);
            List<Hook> aggregatedHooks = aggregator.aggregateHooks(HookPhase.APPLICATION_BEFORE_STOP_IDLE);

            Assertions.assertEquals(0, aggregatedHooks.size());
            Assertions.assertEquals(Collections.emptyList(), aggregatedHooks);

            Map<String, List<String>> executedHooks = StepsUtil.getExecutedHooksForModule(execution, DEFAULT_MODULE_NAME);
            Assertions.assertEquals(buildResultWithExecutedHook(Collections.emptyList(), "application.before-start"), executedHooks);
        }

        @Test
        public void withAlreadyExecutedHooksAndOneHookForCurrentStepPhase() {
            Hook executedHook = getExecutedHook();
            StepsUtil.setExecutedHooksForModule(execution, DEFAULT_MODULE_NAME,
                                                MapUtil.asMap(executedHook.getName(), executedHook.getPhases()));

            Module module = prepareModule(1, "application.before-stop.idle");
            ModuleHooksAggregator aggregator = getModuleHooksAggregator(module);
            List<Hook> aggregatedHooks = aggregator.aggregateHooks(HookPhase.APPLICATION_BEFORE_STOP_IDLE);

            Assertions.assertEquals(1, aggregatedHooks.size());
            Assertions.assertEquals(module.getHooks(), aggregatedHooks);

            Map<String, List<String>> executedHooks = StepsUtil.getExecutedHooksForModule(execution, DEFAULT_MODULE_NAME);
            Assertions.assertEquals(buildResultWithExecutedHook(aggregatedHooks, "application.before-start"), executedHooks);
        }

        @Test
        public void withAlreadyExecutedHooksAndTwoHooksForCurrentStepPhase() {
            Hook executedHook = getExecutedHook();
            StepsUtil.setExecutedHooksForModule(execution, DEFAULT_MODULE_NAME,
                                                MapUtil.asMap(executedHook.getName(), executedHook.getPhases()));

            Module module = prepareModule(2, "application.before-stop.idle");
            ModuleHooksAggregator aggregator = getModuleHooksAggregator(module);
            List<Hook> aggregatedHooks = aggregator.aggregateHooks(HookPhase.APPLICATION_BEFORE_STOP_IDLE);

            Assertions.assertEquals(2, aggregatedHooks.size());
            Assertions.assertEquals(module.getHooks(), aggregatedHooks);

            Map<String, List<String>> executedHooksForModule = StepsUtil.getExecutedHooksForModule(execution, DEFAULT_MODULE_NAME);
            Assertions.assertEquals(buildResultWithExecutedHook(aggregatedHooks, "application.before-start"), executedHooksForModule);
        }

        @Test
        public void withAlreadyExecutedHooksAndHooksWhichAreAlreadyExecuted() {
            Hook executedHook = getExecutedHook("hook0", "application.before-stop.idle");
            StepsUtil.setExecutedHooksForModule(execution, DEFAULT_MODULE_NAME,
                                                MapUtil.asMap(executedHook.getName(), executedHook.getPhases()));

            Module module = prepareModule(1, "application.before-stop.idle");
            ModuleHooksAggregator aggregator = getModuleHooksAggregator(module);
            List<Hook> aggregatedHooks = aggregator.aggregateHooks(HookPhase.APPLICATION_BEFORE_STOP_IDLE);

            Assertions.assertEquals(0, aggregatedHooks.size());

            Map<String, List<String>> executedHooksForModule = StepsUtil.getExecutedHooksForModule(execution, DEFAULT_MODULE_NAME);
            Assertions.assertEquals(1, executedHooksForModule.size());
            Assertions.assertTrue(executedHooksForModule.containsKey("hook0"));
            Assertions.assertEquals(Collections.singletonList("application.before-stop.idle"), executedHooksForModule.get("hook0"));
        }

        private Map<String, List<String>> buildResultWithExecutedHook(List<Hook> aggregatedHooks, String executedHooksPhase) {
            Hook executedHook = getExecutedHook(executedHooksPhase);
            Map<String, List<String>> result = MapUtil.asMap(executedHook.getName(), executedHook.getPhases());
            return buildResultForAggregatedHook(aggregatedHooks, result);
        }

        private Map<String, List<String>> buildResultForAggregatedHook(List<Hook> aggregatedHooks, Map<String, List<String>> result) {
            aggregatedHooks.forEach(hook -> result.put(hook.getName(), hook.getPhases()));
            return result;
        }

        private Hook getExecutedHook() {
            return getExecutedHook("application.before-start");
        }

        private Hook getExecutedHook(String phase) {
            return getExecutedHook("testHook", phase);
        }

        private Hook getExecutedHook(String name, String phase) {
            return Hook.createV3()
                       .setName(name)
                       .setPhases(Collections.singletonList(phase));
        }
    }

}
