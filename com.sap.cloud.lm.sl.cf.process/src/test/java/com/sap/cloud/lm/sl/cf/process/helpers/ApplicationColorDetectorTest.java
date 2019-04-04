package com.sap.cloud.lm.sl.cf.process.helpers;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.flowable.variable.api.history.HistoricVariableInstance;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.sap.cloud.lm.sl.cf.core.dao.OperationDao;
import com.sap.cloud.lm.sl.cf.core.dao.filters.OperationFilter;
import com.sap.cloud.lm.sl.cf.core.flowable.FlowableFacade;
import com.sap.cloud.lm.sl.cf.core.model.ApplicationColor;
import com.sap.cloud.lm.sl.cf.core.model.DeployedMta;
import com.sap.cloud.lm.sl.cf.core.model.DeployedMtaMetadata;
import com.sap.cloud.lm.sl.cf.core.model.DeployedMtaModule;
import com.sap.cloud.lm.sl.cf.core.model.DeployedMtaResource;
import com.sap.cloud.lm.sl.cf.core.model.Phase;
import com.sap.cloud.lm.sl.cf.process.Constants;
import com.sap.cloud.lm.sl.cf.web.api.model.Operation;
import com.sap.cloud.lm.sl.cf.web.api.model.State;
import com.sap.cloud.lm.sl.common.util.JsonUtil;
import com.sap.cloud.lm.sl.common.util.TestUtil;
import com.sap.cloud.lm.sl.common.util.TestUtil.Expectation;

public class ApplicationColorDetectorTest {

    private static final String FAKE_CORRELATION_ID = "39170ac4-57ad-11e9-8647-d663bd873d93";
    private static final String FAKE_MTA_ID = "mta_id";
    private static final String BLUE = "BLUE";
    private static final String GREEN = "GREEN";
    private static final String EXPECTED_EXCEPTION_MESSAGE = "There are both blue and green applications already deployed for MTA \"com.sap.sample.mta.consumer\"";
    private static final String FAKE_BLUE_GREEN_DEPLOY_HISTORIC_PROCESS_INSTANCE_ID = "123123123";
    private static final String FAKE_PROCESS_ID = "abc";

    // This method is used to initialize method parameters -> @MethodSource
    // @formatter:off
    private static Stream<Arguments> detectLiveApplicationColorMtaColorAndPhase() {
        return Stream.of(
                Arguments.of("deployed-mta-01.json", GREEN, State.FINISHED, State.ABORTED, GREEN),
                Arguments.of("deployed-mta-01.json", BLUE, State.FINISHED, State.ABORTED, BLUE),
                Arguments.of("deployed-mta-02.json", GREEN, State.FINISHED, State.ABORTED, GREEN),
                Arguments.of("deployed-mta-02.json", BLUE, State.FINISHED, State.ABORTED, BLUE),
                Arguments.of("deployed-mta-02.json", GREEN, State.FINISHED, State.FINISHED, BLUE),
                Arguments.of("deployed-mta-03.json", BLUE, State.FINISHED, State.ABORTED, BLUE),
                Arguments.of("deployed-mta-03.json", GREEN, State.FINISHED, State.ABORTED, GREEN),
                Arguments.of("deployed-mta-04.json", BLUE, State.FINISHED, State.ABORTED, BLUE),
                Arguments.of("deployed-mta-04.json", GREEN, State.FINISHED, State.ABORTED, GREEN)
               );
    }
    // @formatter:on

    // This method is used to initialize method parameters -> @MethodSource
    // @formatter:off
    private static Stream<Arguments> detectLiveApplicationColor() {
        return Stream.of(
                Arguments.of("deployed-mta-02.json", new Expectation(GREEN)),
                Arguments.of("deployed-mta-01.json", new Expectation(BLUE)),
                Arguments.of("deployed-mta-03.json", new Expectation(BLUE)),
                Arguments.of("deployed-mta-04.json", new Expectation(GREEN)),
                Arguments.of("deployed-mta-05.json", new Expectation(null)),
                Arguments.of("deployed-mta-06.json", new Expectation(GREEN))
            );
    }
    // @formatter:on

    @Mock
    private OperationDao operationDao;

    @Mock
    private FlowableFacade flowableFacade;

    @InjectMocks
    private ApplicationColorDetector applicationColorDetector;

    public ApplicationColorDetectorTest() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void detectSingularDeployedAppNullDeployedMta() {
        TestUtil.test(() -> applicationColorDetector.detectSingularDeployedApplicationColor(null), new Expectation(null), getClass());
    }

    @Test
    public void detectSingularDeployedApp3ModulesGreenSuffix() {
        DeployedMta deployedMta = createMta("com.sap.sample.mta.consumer", Collections.emptySet(),
                                            Arrays.asList(createMtaModule("app-1", "app-1-green", parseDate("2019-04-04")),
                                                          createMtaModule("app-2", "app-2-green", parseDate("2019-04-05")),
                                                          createMtaModule("app-3", "app-3-green", parseDate("2019-04-06"))));
        Expectation expectation = new Expectation(GREEN);
        TestUtil.test(() -> applicationColorDetector.detectSingularDeployedApplicationColor(deployedMta), expectation, getClass());
    }

    @Test
    public void detectSingularDeployedApp3ModulesBlueSuffix() {
        DeployedMta deployedMta = createMta("com.sap.sample.mta.consumer", Collections.emptySet(),
                                            Arrays.asList(createMtaModule("app-1", "app-1-blue", parseDate("2019-04-04")),
                                                          createMtaModule("app-2", "app-2-blue", parseDate("2019-04-05")),
                                                          createMtaModule("app-3", "app-3-blue", parseDate("2019-04-06"))));
        Expectation expectation = new Expectation(BLUE);
        TestUtil.test(() -> applicationColorDetector.detectSingularDeployedApplicationColor(deployedMta), expectation, getClass());
    }

    @Test
    public void detectSingularDeployedApp3ModulesNoSuffix() {
        DeployedMta deployedMta = createMta("com.sap.sample.mta.consumer", Collections.emptySet(),
                                            Arrays.asList(createMtaModule("app-1", "app-1", parseDate("2019-04-04")),
                                                          createMtaModule("app-2", "app-2", parseDate("2019-04-05")),
                                                          createMtaModule("app-3", "app-3", parseDate("2019-04-06"))));
        Expectation expectation = new Expectation(BLUE);
        TestUtil.test(() -> applicationColorDetector.detectSingularDeployedApplicationColor(deployedMta), expectation, getClass());
    }

    @Test
    public void detectSingularDeployedApp3ModulesGreenBlueSuffix() {
        DeployedMta deployedMta = createMta("com.sap.sample.mta.consumer", Collections.emptySet(),
                                            Arrays.asList(createMtaModule("app-1", "app-1-blue", parseDate("2016-15-10")),
                                                          createMtaModule("app-2", "app-2-green", parseDate("2016-10-10")),
                                                          createMtaModule("app-3", "app-3-blue", parseDate("2016-15-10"))));
        Expectation expectation = new Expectation(Expectation.Type.EXCEPTION, EXPECTED_EXCEPTION_MESSAGE);
        TestUtil.test(() -> applicationColorDetector.detectSingularDeployedApplicationColor(deployedMta), expectation, getClass());
    }

    @Test
    public void detectSingularDeployedAppNoModule() {
        DeployedMta deployedMta = createMta("com.sap.sample.mta.consumer", Collections.emptySet(), Collections.emptyList());
        Expectation expectation = new Expectation(null);
        TestUtil.test(() -> applicationColorDetector.detectSingularDeployedApplicationColor(deployedMta), expectation, getClass());
    }

    @Test
    public void detectSingularDeployedApp2ModuleBlueGreenSuffix() {
        DeployedMta deployedMta = createMta("com.sap.sample.mta.consumer", Collections.emptySet(),
                                            Arrays.asList(createMtaModule("consumer", "consumer-green", parseDate("2016-15-10")),
                                                          createMtaModule("consumer", "consumer-blue", parseDate("2016-10-10"))));
        Expectation expectation = new Expectation(Expectation.Type.EXCEPTION, EXPECTED_EXCEPTION_MESSAGE);
        TestUtil.test(() -> applicationColorDetector.detectSingularDeployedApplicationColor(deployedMta), expectation, getClass());
    }

    @ParameterizedTest
    @MethodSource
    public void detectLiveApplicationColor(String deployedMtaJsonLocation, Expectation expectations) {
        mockOperationDao(createFakeOperation(State.RUNNING), createFakeOperation(State.FINISHED));
        TestUtil.test(() -> detectLiveApplicationColor(readResource(deployedMtaJsonLocation, DeployedMta.class)), expectations, getClass());
    }

    @Test
    public void detectLiveApplicationColorNullDeployedMta() {
        TestUtil.test(() -> detectLiveApplicationColor(null), new Expectation(null), getClass());
    }

    @ParameterizedTest
    @MethodSource
    public void detectLiveApplicationColorMtaColorAndPhase(String deployedMtaJsonLocation, String expectedColor,
        State currentOperationState, State lastOperationState, String lastDeployedColor) {
        Expectation expectation = new Expectation(expectedColor);
        mockOperationDao(createFakeOperation(currentOperationState), createFakeOperation(lastOperationState));
        when(flowableFacade.findHistoricProcessInstanceIdByProcessDefinitionKey(FAKE_PROCESS_ID,
                                                                                Constants.BLUE_GREEN_DEPLOY_SERVICE_ID)).thenReturn(FAKE_BLUE_GREEN_DEPLOY_HISTORIC_PROCESS_INSTANCE_ID);
        mockHistoricVariableInstanceColor(lastDeployedColor);
        mockHistoricVariableInstancePhase();
        TestUtil.test(() -> detectLiveApplicationColor(readResource(deployedMtaJsonLocation, DeployedMta.class)), expectation, getClass());
    }

    @Test
    public void detectLiveApplicationColorPhaseNotFound() {
        Expectation expectation = new Expectation(GREEN);
        mockOperationDao(createFakeOperation(State.RUNNING), createFakeOperation(State.ABORTED));
        mockHistoricVariableInstanceColor(GREEN);
        TestUtil.test(() -> detectLiveApplicationColor(readResource("deployed-mta-02.json", DeployedMta.class)), expectation, getClass());
    }

    @Test
    public void detectLiveApplicationColorMtaColoNotFound() {
        Expectation expectation = new Expectation(GREEN);
        mockOperationDao(createFakeOperation(State.RUNNING), createFakeOperation(State.ABORTED));
        mockHistoricVariableInstancePhase();
        TestUtil.test(() -> detectLiveApplicationColor(readResource("deployed-mta-02.json", DeployedMta.class)), expectation, getClass());
    }

    @Test
    public void detectLiveApplicationColorNoOperations() {
        Expectation expectation = new Expectation(GREEN);
        mockOperationDaoNoOtherOperations(createFakeOperation(State.RUNNING));
        TestUtil.test(() -> detectLiveApplicationColor(readResource("deployed-mta-02.json", DeployedMta.class)), expectation, getClass());
    }

    private DeployedMta createMta(String id, Set<String> services, List<DeployedMtaModule> deployedModules) {
        DeployedMta deployedMta = new DeployedMta();
        deployedMta.setMetadata(new DeployedMtaMetadata(id));
        deployedMta.setModules(deployedModules);
        Set<DeployedMtaResource> deployedServices = services.stream()
                                         .map(s -> DeployedMtaResource.builder().withServiceName(s).build())
                                         .collect(Collectors.toSet());
        deployedMta.setServices(deployedServices);
        return deployedMta;
    }

    private Date parseDate(String date) {
        try {
            return new SimpleDateFormat("yyyy-MM-dd").parse(date);
        } catch (ParseException e) {
            e.printStackTrace();
        }

        return null;
    }

    private DeployedMtaModule createMtaModule(String moduleName, String appName, Date createdOn) {
        return DeployedMtaModule.builder().withModuleName(moduleName).withAppName(appName).withCreatedOn(createdOn).build();
    }

    private void mockOperationDaoNoOtherOperations(Operation currentOperation) {
        when(operationDao.find(anyString())).thenReturn(currentOperation);
        when(operationDao.find(any(OperationFilter.class))).thenReturn(Collections.emptyList());
    }

    private <T> T readResource(String deployMtaJsonLocation, Class<T> clazz) {
        return JsonUtil.fromJson(TestUtil.getResourceAsString(deployMtaJsonLocation, ApplicationColorDetectorTest.class), clazz);
    }

    private ApplicationColor detectLiveApplicationColor(DeployedMta deployedMta) {
        return applicationColorDetector.detectLiveApplicationColor(deployedMta, FAKE_CORRELATION_ID);
    }

    private void mockHistoricVariableInstancePhase() {
        HistoricVariableInstance historicVariableInstancePhase = mock(HistoricVariableInstance.class);
        when(historicVariableInstancePhase.getValue()).thenReturn(Phase.UNDEPLOY.toString());
        when(flowableFacade.getHistoricVariableInstance(FAKE_BLUE_GREEN_DEPLOY_HISTORIC_PROCESS_INSTANCE_ID,
                                                        Constants.VAR_PHASE)).thenReturn(historicVariableInstancePhase);
    }

    private void mockHistoricVariableInstanceColor(String color) {
        HistoricVariableInstance historicVariableInstanceColor = mock(HistoricVariableInstance.class);
        when(historicVariableInstanceColor.getValue()).thenReturn(color);
        when(flowableFacade.getHistoricVariableInstance(FAKE_BLUE_GREEN_DEPLOY_HISTORIC_PROCESS_INSTANCE_ID,
                                                        Constants.VAR_MTA_COLOR)).thenReturn(historicVariableInstanceColor);
    }

    private void mockOperationDao(Operation currentOperation, Operation lastOperation) {
        when(operationDao.find(anyString())).thenReturn(currentOperation);
        when(operationDao.find(any(OperationFilter.class))).thenReturn(Arrays.asList(lastOperation));
    }

    private Operation createFakeOperation(State state) {
        Operation operation = mock(Operation.class);
        when(operation.getState()).thenReturn(state);
        when(operation.getMtaId()).thenReturn(FAKE_MTA_ID);
        when(operation.getProcessId()).thenReturn(FAKE_PROCESS_ID);
        return operation;
    }

}
