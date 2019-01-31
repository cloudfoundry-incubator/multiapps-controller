package com.sap.cloud.lm.sl.cf.core.helpers;

import java.util.Map;

import com.sap.cloud.lm.sl.mta.helpers.SimplePropertyVisitor;
import com.sap.cloud.lm.sl.mta.helpers.VisitableObject;
import com.sap.cloud.lm.sl.mta.model.ElementContext;
import com.sap.cloud.lm.sl.mta.model.ParametersContainer;
import com.sap.cloud.lm.sl.mta.model.Visitor;
import com.sap.cloud.lm.sl.mta.model.v2.Module;
import com.sap.cloud.lm.sl.mta.model.v2.ProvidedDependency;
import com.sap.cloud.lm.sl.mta.model.v2.RequiredDependency;
import com.sap.cloud.lm.sl.mta.model.v2.Resource;

public class XsPlaceholderResolverInvoker extends Visitor implements SimplePropertyVisitor {

    private XsPlaceholderResolver resolver;

    public XsPlaceholderResolverInvoker(XsPlaceholderResolver resolver) {
        this.resolver = resolver;
    }

    @Override
    public void visit(ElementContext context, Module module) {
        resolveParameters(module);
    }

    @Override
    public void visit(ElementContext context, ProvidedDependency providedDependency) {
        if (providedDependency instanceof com.sap.cloud.lm.sl.mta.model.v2.ProvidedDependency) {
            return;
        }
        resolveParameters((com.sap.cloud.lm.sl.mta.model.v3.ProvidedDependency) providedDependency);
    }

    @Override
    public void visit(ElementContext context, RequiredDependency requiredDependency) {
        resolveParameters(requiredDependency);
    }

    @Override
    public void visit(ElementContext context, Resource resource) {
        resolveParameters(resource);
    }

    @SuppressWarnings("unchecked")
    private void resolveParameters(ParametersContainer parametersContainer) {
        Map<String, Object> parameters = parametersContainer.getParameters();
        Map<String, Object> resolvedParameters = (Map<String, Object>) new VisitableObject(parameters).accept(this);
        parametersContainer.setParameters(resolvedParameters);
    }

    @Override
    public Object visit(String key, String value) {
        return resolver.resolve(value);
    }

}
