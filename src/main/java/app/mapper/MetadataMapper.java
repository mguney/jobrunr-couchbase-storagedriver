package app.mapper;

import app.model.MetadataEntity;
import org.jobrunr.storage.JobRunrMetadata;

public class MetadataMapper {
    
    public MetadataEntity toInsertDocument(JobRunrMetadata metadata) {
        return MetadataEntity.builder().id(metadata.getId()).name(metadata.getName()).owner(metadata.getOwner()).value(metadata.getValue()).build();
    }
    
    public void toUpdateDocument(JobRunrMetadata metadata, MetadataEntity entity) {
        entity.setName(metadata.getName());
        entity.setOwner(metadata.getOwner());
        entity.setValue(metadata.getValue());
        entity.setUpdatedAt(metadata.getUpdatedAt());
    }
    
    public JobRunrMetadata toJobRunrMetadata(MetadataEntity entity) {
        if (entity == null) {
            return null;
        }
        
        return new JobRunrMetadata(entity.getName(), entity.getOwner(), entity.getValue(), entity.getCreatedAt(), entity.getUpdatedAt());
    }
}
