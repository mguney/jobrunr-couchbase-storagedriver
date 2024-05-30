package app;

import app.repository.BackgroundJobServerRepository;
import app.repository.JobRepository;
import app.repository.MetadataRepository;
import app.repository.RecurringJobsRepository;
import com.couchbase.client.java.Cluster;
import org.jobrunr.jobs.mappers.JobMapper;
import org.jobrunr.storage.InMemoryStorageProvider;
import org.jobrunr.storage.StorageProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
public class SchedulerConfiguration {
    
    
    StorageProvider storageProvider2(JobMapper jobMapper) {
        InMemoryStorageProvider storageProvider = new InMemoryStorageProvider();
        storageProvider.setJobMapper(jobMapper);
        return storageProvider;
    }
    
    
    @Bean
    StorageProvider storageProvider(JobMapper jobMapper,
                                    Cluster cluster,
                                    @Value("${org.jobrunr.database.bucket-name}") String bucketName,
                                    @Value("${org.jobrunr.database.scope-name}") String scopeName,
                                    BackgroundJobServerRepository backgroundJobServerRepository,
                                    MetadataRepository metadataRepository,
                                    JobRepository jobRepository,
                                    RecurringJobsRepository recurringJobsRepository) {
        
        return new CouchbaseStorageProvider(jobMapper,
                                            cluster,
                                            bucketName, backgroundJobServerRepository,
                                            jobRepository, metadataRepository,
                                            recurringJobsRepository);
    }
    
}
