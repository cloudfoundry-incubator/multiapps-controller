package com.sap.cloud.lm.sl.cf.core.model;

import org.immutables.value.Value;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.sap.cloud.lm.sl.cf.core.cf.HandlerFactory;
import com.sap.cloud.lm.sl.cf.core.dao.ConfigurationEntryDao;
import com.sap.cloud.lm.sl.cf.core.util.ApplicationConfiguration;

@Value.Immutable
@JsonSerialize(as = ImmutableMtaDescriptorPropertiesResolverContext.class)
@JsonDeserialize(as = ImmutableMtaDescriptorPropertiesResolverContext.class)
public interface MtaDescriptorPropertiesResolverContext {

    HandlerFactory getHandlerFactory();

    ConfigurationEntryDao getConfigurationEntryDao();

    CloudTarget getCloudTarget();

    String getCurrentSpaceId();

    ApplicationConfiguration getApplicationConfiguration();

    boolean hasUseNamespaces();

    boolean hasUserNamespacesForServices();

    boolean shouldReserveTemporaryRoute();
}
