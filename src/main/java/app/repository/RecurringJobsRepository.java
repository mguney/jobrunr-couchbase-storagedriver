package app.repository;

import app.CouchbaseStorageProvider;
import app.model.RecurringJobsEntity;
import org.jobrunr.storage.StorageProviderUtils.RecurringJobs;
import org.springframework.data.couchbase.repository.Collection;
import org.springframework.data.couchbase.repository.Query;
import org.springframework.data.couchbase.repository.Scope;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.ListPagingAndSortingRepository;
import org.springframework.stereotype.Repository;

@Collection(RecurringJobs.NAME)
@Scope(CouchbaseStorageProvider.SCOPE)
@Repository
public interface RecurringJobsRepository extends ListPagingAndSortingRepository<RecurringJobsEntity, String>, ListCrudRepository<RecurringJobsEntity, String> {
    
    @Query("select sum(u.createdAt) from RecurringJobsEntity u")
    long findLastModifiedHash();
}
