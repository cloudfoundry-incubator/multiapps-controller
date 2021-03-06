package org.cloudfoundry.multiapps.controller.process.util;

import javax.inject.Inject;
import javax.inject.Named;

import org.cloudfoundry.multiapps.common.SLException;
import org.cloudfoundry.multiapps.controller.core.cf.CloudControllerClientProvider;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.flowable.engine.HistoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
public class ClientReleaser {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClientReleaser.class);

    private final CloudControllerClientProvider clientProvider;

    @Inject
    public ClientReleaser(CloudControllerClientProvider clientProvider) {
        this.clientProvider = clientProvider;
    }

    public void releaseClientFor(HistoryService historyService, String processInstanceId) {
        String user = HistoryUtil.getVariableValue(historyService, processInstanceId, Variables.USER.getName());
        String organizationName = HistoryUtil.getVariableValue(historyService, processInstanceId, Variables.ORGANIZATION_NAME.getName());
        String spaceName = HistoryUtil.getVariableValue(historyService, processInstanceId, Variables.SPACE_NAME.getName());
        String spaceGuid = HistoryUtil.getVariableValue(historyService, processInstanceId, Variables.SPACE_GUID.getName());
        String correlationId = HistoryUtil.getVariableValue(historyService, processInstanceId, Variables.CORRELATION_ID.getName());

        try {
            clientProvider.releaseClient(user, spaceGuid, correlationId);
            clientProvider.releaseClient(user, organizationName, spaceName, correlationId);
            clientProvider.releaseClientWithNoCorrelation(user, spaceGuid);
            clientProvider.releaseClientWithNoCorrelation(user, organizationName, spaceName);
        } catch (SLException e) {
            LOGGER.warn(e.getMessage());
        }
    }
}
