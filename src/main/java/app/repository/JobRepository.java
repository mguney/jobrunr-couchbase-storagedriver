package app.repository;

import app.CouchbaseStorageProvider;
import app.model.JobEntity;
import app.model.StateCountsOnly;
import com.couchbase.client.java.query.QueryScanConsistency;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;
import org.jobrunr.storage.StorageProviderUtils.Jobs;
import org.springframework.data.couchbase.core.query.WithConsistency;
import org.springframework.data.couchbase.repository.Collection;
import org.springframework.data.couchbase.repository.Query;
import org.springframework.data.couchbase.repository.Scope;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.ListPagingAndSortingRepository;
import org.springframework.stereotype.Repository;

@Collection(Jobs.NAME)
@Scope(CouchbaseStorageProvider.SCOPE)
@Repository
public interface JobRepository extends ListPagingAndSortingRepository<JobEntity, UUID>, ListCrudRepository<JobEntity, UUID> {
    
    long countByState(String state);
    
    Stream<JobEntity> findByStateAndUpdatedAtBefore(String name, Instant updatedBefore, Pageable pageable);
    
    Stream<JobEntity> findByStateAndScheduledAtBefore(String name, Instant scheduledBefore, Pageable pageable);
    
    Stream<JobEntity> findByState(String name, Pageable pageable);
    
    List<JobEntity> deleteAllByStateAndCreatedAtBefore(String name, Instant updatedBefore);
    
    @Query("select distinct jobSignature from jobs")
    Set<String> findDistinctJobSignatureByStateIn(List<String> states);
    
    boolean existsByRecurringJobId(String recurringJobId);
    
    boolean existsByRecurringJobIdAndStateIn(String recurringJobId, List<String> states);
    
    @WithConsistency(QueryScanConsistency.REQUEST_PLUS)
    @Query("select state as __id, state as state, count(*) as count from jobs group by state")
    List<StateCountsOnly> findStateCounts();
    
}
