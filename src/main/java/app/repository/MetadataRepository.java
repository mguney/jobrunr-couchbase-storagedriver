package app.repository;

import app.CouchbaseStorageProvider;
import app.model.MetadataEntity;
import java.util.List;
import java.util.stream.Stream;
import org.jobrunr.storage.StorageProviderUtils.Metadata;
import org.springframework.data.couchbase.repository.Collection;
import org.springframework.data.couchbase.repository.Scope;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.ListPagingAndSortingRepository;
import org.springframework.stereotype.Repository;

@Collection(Metadata.NAME)
@Scope(CouchbaseStorageProvider.SCOPE)
@Repository
public interface MetadataRepository extends ListPagingAndSortingRepository<MetadataEntity, String>, ListCrudRepository<MetadataEntity, String> {
    
    Stream<MetadataEntity> findByName(String name);
    
    List<MetadataEntity> deleteAllByName(String name);
}
