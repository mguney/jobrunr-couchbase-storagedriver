package app.repository;


import app.CouchbaseStorageProvider;
import app.model.BackgroundJobServerEntity;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;
import org.jobrunr.storage.StorageProviderUtils.BackgroundJobServers;
import org.jobrunr.storage.StorageProviderUtils.Metadata;
import org.springframework.data.couchbase.repository.Collection;
import org.springframework.data.couchbase.repository.Scope;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.ListPagingAndSortingRepository;
import org.springframework.stereotype.Repository;

@Collection(BackgroundJobServers.NAME)
@Scope(CouchbaseStorageProvider.SCOPE)
@Repository
public interface BackgroundJobServerRepository extends ListPagingAndSortingRepository<BackgroundJobServerEntity, UUID>, ListCrudRepository<BackgroundJobServerEntity, UUID> {
    
    
    List<BackgroundJobServerEntity> findAllByLastHeartbeatBefore(Instant heartbeatOlderThan);
    
    
}
