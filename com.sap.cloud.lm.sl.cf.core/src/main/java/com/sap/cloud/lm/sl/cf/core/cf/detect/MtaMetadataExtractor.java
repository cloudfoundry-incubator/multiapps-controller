package com.sap.cloud.lm.sl.cf.core.cf.detect;

import java.util.ArrayList;
import java.util.HashSet;

import com.sap.cloud.lm.sl.cf.core.cf.detect.entity.MtaMetadataEntity;
import com.sap.cloud.lm.sl.cf.core.model.DeployedMta;

public interface MtaMetadataExtractor<T extends MtaMetadataEntity> {

    public void extract(T entity, DeployedMta metadata);
    
    default void initMetadata(T entity, DeployedMta metadata) {
        if(metadata.getServices() == null) {
            metadata.setServices(new HashSet<>());
        }
        if(metadata.getModules() == null) {
            metadata.setModules(new ArrayList<>());
        }
        if(metadata.getMetadata() == null) {
            metadata.setMetadata(entity.getMtaMetadata());
        }
    }
    
}
