package app;

import static java.util.Arrays.stream;
import static org.jobrunr.jobs.Job.ALLOWED_SORT_COLUMNS;
import static org.jobrunr.jobs.states.StateName.DELETED;
import static org.jobrunr.jobs.states.StateName.ENQUEUED;
import static org.jobrunr.jobs.states.StateName.FAILED;
import static org.jobrunr.jobs.states.StateName.PROCESSING;
import static org.jobrunr.jobs.states.StateName.SCHEDULED;
import static org.jobrunr.jobs.states.StateName.SUCCEEDED;
import static org.jobrunr.storage.StorageProviderUtils.DatabaseOptions.CREATE;

import app.mapper.BackgroundJobServerStatusMapper;
import app.mapper.JobEntityMapper;
import app.mapper.MetadataMapper;
import app.model.BackgroundJobServerEntity;
import app.model.JobEntity;
import app.model.MetadataEntity;
import app.model.StateCountsOnly;
import app.repository.BackgroundJobServerRepository;
import app.repository.JobRepository;
import app.repository.MetadataRepository;
import app.repository.RecurringJobsRepository;
import com.couchbase.client.core.error.CouchbaseException;
import com.couchbase.client.java.Cluster;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.jobrunr.jobs.Job;
import org.jobrunr.jobs.JobListVersioner;
import org.jobrunr.jobs.JobVersioner;
import org.jobrunr.jobs.RecurringJob;
import org.jobrunr.jobs.mappers.JobMapper;
import org.jobrunr.jobs.states.StateName;
import org.jobrunr.storage.AbstractStorageProvider;
import org.jobrunr.storage.BackgroundJobServerStatus;
import org.jobrunr.storage.ConcurrentJobModificationException;
import org.jobrunr.storage.JobNotFoundException;
import org.jobrunr.storage.JobRunrMetadata;
import org.jobrunr.storage.JobStats;
import org.jobrunr.storage.RecurringJobsResult;
import org.jobrunr.storage.StorageException;
import org.jobrunr.storage.StorageProviderUtils.BackgroundJobServers;
import org.jobrunr.storage.StorageProviderUtils.DatabaseOptions;
import org.jobrunr.storage.StorageProviderUtils.Metadata;
import org.jobrunr.storage.navigation.AmountRequest;
import org.jobrunr.storage.navigation.OffsetBasedPageRequest;
import org.jobrunr.storage.navigation.OrderTerm;
import org.jobrunr.storage.nosql.NoSqlStorageProvider;
import org.jobrunr.utils.resilience.RateLimiter;
import org.jobrunr.utils.resilience.RateLimiter.Builder;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Order;
import org.springframework.transaction.annotation.Transactional;


public class CouchbaseStorageProvider extends AbstractStorageProvider implements NoSqlStorageProvider {
    
    public static final String SCOPE = "jobrunr";
    private JobMapper jobMapper;
    private final BackgroundJobServerStatusMapper backgroundJobServerStatusMapper;
    private final JobEntityMapper jobEntityMapper;
    private final MetadataMapper metadataDocumentMapper;
    
    private final BackgroundJobServerRepository backgroundJobServerRepository;
    private final JobRepository jobRepository;
    private final MetadataRepository metadataRepository;
    private final RecurringJobsRepository recurringJobsRepository;
    
    private final Cluster cluster;
    private final String bucketName;
    
    public CouchbaseStorageProvider(JobMapper jobMapper,
                                    Cluster cluster,
                                    String bucketName,
                                    BackgroundJobServerRepository backgroundJobServerRepository,
                                    JobRepository jobRepository,
                                    MetadataRepository metadataRepository,
                                    RecurringJobsRepository recurringJobsRepository) {
        this(jobMapper, cluster, bucketName, CREATE, backgroundJobServerRepository, jobRepository, metadataRepository, recurringJobsRepository);
    }
    
    protected CouchbaseStorageProvider(JobMapper jobMapper,
                                       Cluster cluster,
                                       String bucketName,
                                       DatabaseOptions databaseOptions,
                                       BackgroundJobServerRepository backgroundJobServerRepository,
                                       JobRepository jobRepository,
                                       MetadataRepository metadataRepository,
                                       RecurringJobsRepository recurringJobsRepository) {
        super(Builder.rateLimit().at1Request().per(RateLimiter.SECOND));
        this.backgroundJobServerRepository = backgroundJobServerRepository;
        this.jobRepository = jobRepository;
        this.metadataRepository = metadataRepository;
        this.recurringJobsRepository = recurringJobsRepository;
        this.jobEntityMapper = new JobEntityMapper(jobMapper);
        this.backgroundJobServerStatusMapper = new BackgroundJobServerStatusMapper();
        this.metadataDocumentMapper = new MetadataMapper();
        this.jobMapper = jobMapper;
        this.cluster = cluster;
        this.bucketName = bucketName;
        
        setUpStorageProvider(databaseOptions);
        
    }
    
    @Override
    public void setJobMapper(JobMapper jobMapper) {
        this.jobMapper = jobMapper;
    }
    
    @Override
    public void setUpStorageProvider(DatabaseOptions databaseOptions) {
        if (CREATE == databaseOptions) {
            new CouchbaseDBCreator(cluster, bucketName).runMigration();
        } else {
            new CouchbaseDBCreator(cluster, bucketName).validateCollections();
        }
    }
    
    @Override
    public void announceBackgroundJobServer(BackgroundJobServerStatus backgroundJobServerStatus) {
        backgroundJobServerRepository.save(backgroundJobServerStatusMapper.toInsertDocument(backgroundJobServerStatus));
    }
    
    @Override
    public boolean signalBackgroundJobServerAlive(BackgroundJobServerStatus backgroundJobServerStatus) {
        BackgroundJobServerEntity entity = backgroundJobServerRepository.save(backgroundJobServerStatusMapper.toInsertDocument(backgroundJobServerStatus));
        return entity.getRunning();
    }
    
    @Override
    public void signalBackgroundJobServerStopped(BackgroundJobServerStatus backgroundJobServerStatus) {
        backgroundJobServerRepository.findById(backgroundJobServerStatus.getId()).ifPresent(backgroundJobServerRepository::delete);
    }
    
    @Override
    public List<BackgroundJobServerStatus> getBackgroundJobServers() {
        return backgroundJobServerRepository.findAll(Sort.by(Order.asc(BackgroundJobServers.FIELD_FIRST_HEARTBEAT)))
                                            .stream()
                                            .map(backgroundJobServerStatusMapper::toBackgroundJobServerStatus)
                                            .toList();
    }
    
    @Override
    public UUID getLongestRunningBackgroundJobServerId() {
        PageRequest pageRequest = PageRequest.of(0, 1, Sort.by(Order.asc(BackgroundJobServers.FIELD_FIRST_HEARTBEAT)));
        return backgroundJobServerRepository.findAll(pageRequest)
                                            .map(backgroundJobServerStatusMapper::toBackgroundJobServerStatus)
                                            .stream()
                                            .findFirst()
                                            .orElseThrow(() -> new StorageException("No Server Found"))
                                            .getId();
    }
    
    @Transactional
    @Override
    public int removeTimedOutBackgroundJobServers(Instant heartbeatOlderThan) {
        List<BackgroundJobServerEntity> deletedEntities = backgroundJobServerRepository.findAllByLastHeartbeatBefore(heartbeatOlderThan);
        backgroundJobServerRepository.deleteAll(deletedEntities);
        return deletedEntities.size();
    }
    
    @Override
    public void saveMetadata(JobRunrMetadata metadata) {
        metadataRepository.save(metadataDocumentMapper.toInsertDocument(metadata));
        notifyMetadataChangeListeners();
    }
    
    @Override
    public List<JobRunrMetadata> getMetadata(String name) {
        return metadataRepository.findByName(name).map(metadataDocumentMapper::toJobRunrMetadata).toList();
    }
    
    @Override
    public JobRunrMetadata getMetadata(String name, String owner) {
        return metadataRepository.findById(JobRunrMetadata.toId(name, owner))
                                 .map(metadataDocumentMapper::toJobRunrMetadata)
                                 .orElseThrow(() -> new StorageException("No Metadata Found"));
    }
    
    @Override
    public void deleteMetadata(String name) {
        List<MetadataEntity> deletedList = metadataRepository.deleteAllByName(name);
        notifyMetadataChangeListeners(!deletedList.isEmpty());
    }
    
    @Transactional
    @Override
    public Job save(Job job) throws ConcurrentJobModificationException {
        try (JobVersioner jobVersioner = new JobVersioner(job)) {
            if (jobVersioner.isNewJob()) {
                jobRepository.save(jobEntityMapper.toInsertDocument(job));
            } else {
                JobEntity entity = jobRepository.findById(job.getId()).orElseThrow(() -> new ConcurrentJobModificationException(job));
                jobEntityMapper.toUpdateDocument(job, entity);
                jobRepository.save(entity);
            }
            jobVersioner.commitVersion();
        } catch (CouchbaseException e) {
            throw new StorageException(e);
        }
        notifyJobStatsOnChangeListeners();
        return job;
    }
    
    @Transactional
    @Override
    public List<Job> save(List<Job> jobs) throws ConcurrentJobModificationException {
        if (jobs.isEmpty()) {
            return jobs;
        }
        
        try (JobListVersioner jobListVersioner = new JobListVersioner(jobs)) {
            if (jobListVersioner.areNewJobs()) {
                jobRepository.saveAll(jobEntityMapper.toInsertDocument(jobs));
                
            } else {
                List<JobEntity> existingJobs = jobRepository.findAllById(jobs.stream().map(Job::getId).toList());
                
                if (existingJobs.isEmpty() || existingJobs.size() != jobs.size()) {
                    throw new ConcurrentJobModificationException(jobs);
                }
                jobEntityMapper.toUpdateDocument(jobs, existingJobs);
                jobRepository.saveAll(existingJobs);
            }
            jobListVersioner.commitVersions();
        } catch (CouchbaseException e) {
            throw new StorageException(e);
        }
        notifyJobStatsOnChangeListenersIf(!jobs.isEmpty());
        return jobs;
    }
    
    @Override
    public Job getJobById(UUID uuid) throws JobNotFoundException {
        return jobEntityMapper.toJob(jobRepository.findById(uuid).orElseThrow(() -> new JobNotFoundException(uuid)));
    }
    
    @Override
    public long countJobs(StateName stateName) {
        return jobRepository.countByState(stateName.name());
    }
    
    @Override
    public List<Job> getJobList(StateName stateName, Instant updatedBefore, AmountRequest amountRequest) {
        int pageNumber = (amountRequest instanceof OffsetBasedPageRequest) ? (int) ((OffsetBasedPageRequest) amountRequest).getOffset() : 0;
        PageRequest pageRequest = PageRequest.of(pageNumber, amountRequest.getLimit(), mapToSort(amountRequest));
        return jobRepository.findByStateAndUpdatedAtBefore(stateName.name(), updatedBefore, pageRequest).map(jobEntityMapper::toJob).toList();
        
    }
    
    public Sort mapToSort(AmountRequest amountRequest) {
        List<OrderTerm> orderTerms = amountRequest.getAllOrderTerms(ALLOWED_SORT_COLUMNS.keySet());
        List<Order> result = new ArrayList<>();
        for (OrderTerm orderTerm : orderTerms) {
            result.add(OrderTerm.Order.ASC == orderTerm.getOrder() ? Order.asc(orderTerm.getFieldName()) : Order.desc(orderTerm.getFieldName()));
        }
        return Sort.by(result);
    }
    
    @Override
    public List<Job> getJobList(StateName stateName, AmountRequest amountRequest) {
        int pageNumber = (amountRequest instanceof OffsetBasedPageRequest) ? (int) ((OffsetBasedPageRequest) amountRequest).getOffset() : 0;
        PageRequest pageRequest = PageRequest.of(pageNumber, amountRequest.getLimit(), mapToSort(amountRequest));
        return jobRepository.findByState(stateName.name(), pageRequest).map(jobEntityMapper::toJob).toList();
    }
    
    @Override
    public List<Job> getScheduledJobs(Instant scheduledBefore, AmountRequest amountRequest) {
        int pageNumber = (amountRequest instanceof OffsetBasedPageRequest) ? (int) ((OffsetBasedPageRequest) amountRequest).getOffset() : 0;
        PageRequest pageRequest = PageRequest.of(pageNumber, amountRequest.getLimit(), mapToSort(amountRequest));
        return jobRepository.findByStateAndScheduledAtBefore(StateName.SCHEDULED.name(), scheduledBefore, pageRequest).map(jobEntityMapper::toJob).toList();
    }
    
    
    @Override
    public int deletePermanently(UUID uuid) {
        jobRepository.deleteById(uuid);
        notifyJobStatsOnChangeListenersIf(true);
        return 1;
    }
    
    @Override
    public int deleteJobsPermanently(StateName stateName, Instant updatedBefore) {
        List<JobEntity> deletedEntities = jobRepository.deleteAllByStateAndCreatedAtBefore(stateName.name(), updatedBefore);
        notifyJobStatsOnChangeListenersIf(!deletedEntities.isEmpty());
        return deletedEntities.size();
    }
    
    @Override
    public Set<String> getDistinctJobSignatures(StateName... stateNames) {
        return jobRepository.findDistinctJobSignatureByStateIn(stream(stateNames).map(Enum::name).toList());
    }
    
    @Override
    public boolean recurringJobExists(String recurringJobId, StateName... states) {
        if (states.length < 1) {
            return jobRepository.existsByRecurringJobId(recurringJobId);
        }
        return jobRepository.existsByRecurringJobIdAndStateIn(recurringJobId, stream(states).map(Enum::name).toList());
    }
    
    @Override
    public RecurringJob saveRecurringJob(RecurringJob recurringJob) {
        recurringJobsRepository.save(jobEntityMapper.toInsertDocument(recurringJob));
        return recurringJob;
    }
    
    @Override
    public RecurringJobsResult getRecurringJobs() {
        return new RecurringJobsResult(recurringJobsRepository.findAll().stream().map(jobEntityMapper::toRecurringJob).toList());
    }
    
    @Override
    public boolean recurringJobsUpdated(Long recurringJobsUpdatedHash) {
        long lastModifiedHash = recurringJobsRepository.findLastModifiedHash();
        return !recurringJobsUpdatedHash.equals(lastModifiedHash);
    }
    
    @Override
    public int deleteRecurringJob(String id) {
        recurringJobsRepository.deleteById(id);
        return 1;
    }
    
    @Override
    public JobStats getJobStats() {
        Instant instant = Instant.now();
        
        MetadataEntity succeededJobStats = metadataRepository.findById(Metadata.STATS_ID).orElseGet(() -> MetadataEntity.builder().value("0").build());
        final long allTimeSucceededCount = Long.parseLong(succeededJobStats.getValue());
        
        List<StateCountsOnly> statsByStates = jobRepository.findStateCounts();
        Map<String, Long> stateCountMap = statsByStates.stream().collect(Collectors.toMap(StateCountsOnly::state, StateCountsOnly::count));
        
        Long scheduledCount = stateCountMap.getOrDefault(SCHEDULED.name(), 0L);
        Long enqueuedCount = stateCountMap.getOrDefault(ENQUEUED.name(), 0L);
        Long processingCount = stateCountMap.getOrDefault(PROCESSING.name(), 0L);
        Long succeededCount = stateCountMap.getOrDefault(SUCCEEDED.name(), 0L);
        Long failedCount = stateCountMap.getOrDefault(FAILED.name(), 0L);
        Long deletedCount = stateCountMap.getOrDefault(DELETED.name(), 0L);
        
        final long total = scheduledCount + enqueuedCount + processingCount + succeededCount + failedCount;
        final int recurringJobCount = (int) recurringJobsRepository.count();
        final int backgroundJobServerCount = (int) backgroundJobServerRepository.count();
        
        return new JobStats(instant,
                            total,
                            scheduledCount,
                            enqueuedCount,
                            processingCount,
                            failedCount,
                            succeededCount,
                            allTimeSucceededCount,
                            deletedCount,
                            recurringJobCount,
                            backgroundJobServerCount);
    }
    
    
    @Transactional
    @Override
    public void publishTotalAmountOfSucceededJobs(int amount) {
        
        MetadataEntity metadataEntity = metadataRepository.findById(Metadata.STATS_ID)
                                                          .orElseGet(() -> MetadataEntity.builder()
                                                                                         .id(Metadata.STATS_ID)
                                                                                         .createdAt(Instant.now())
                                                                                         .updatedAt(Instant.now())
                                                                                         .value("0")
                                                                                         .build());
        
        metadataEntity.setValue(String.valueOf(Integer.parseInt(metadataEntity.getValue()) + amount));
        metadataRepository.save(metadataEntity);
    }
}
