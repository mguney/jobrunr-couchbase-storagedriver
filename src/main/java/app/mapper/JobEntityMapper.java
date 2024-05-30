package app.mapper;

import app.model.JobEntity;
import app.model.JobEntity.JobEntityBuilder;
import app.model.RecurringJobsEntity;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.jobrunr.jobs.Job;
import org.jobrunr.jobs.RecurringJob;
import org.jobrunr.jobs.mappers.JobMapper;
import org.jobrunr.jobs.states.ScheduledState;
import org.jobrunr.jobs.states.StateName;

public class JobEntityMapper {
    
    private final JobMapper jobMapper;
    
    public JobEntityMapper(org.jobrunr.jobs.mappers.JobMapper jobMapper) {
        this.jobMapper = jobMapper;
    }
    
    public List<JobEntity> toInsertDocument(List<Job> jobs) {
        return jobs.stream().map(this::toInsertDocument).toList();
    }
    
    public JobEntity toInsertDocument(Job job) {
        JobEntityBuilder jobEntityBuilder = JobEntity.builder()
                                                     .id(job.getId())
                                                     .version(job.getVersion())
                                                     .jobAsJson(jobMapper.serializeJob(job))
                                                     .jobSignature(job.getJobSignature())
                                                     .state(job.getState().name())
                                                     .createdAt(job.getCreatedAt())
                                                     .updatedAt(job.getUpdatedAt());
        
        if (job.hasState(StateName.SCHEDULED)) {
            jobEntityBuilder.scheduledAt(job.<ScheduledState>getJobState().getScheduledAt());
        }
        job.getRecurringJobId().ifPresent(jobEntityBuilder::recurringJobId);
        
        return jobEntityBuilder.build();
    }
    
    public void toUpdateDocument(List<Job> jobs, List<? extends JobEntity> entities) {
        if (jobs.isEmpty() || entities.isEmpty()) {
            return;
        }
        
        Map<UUID, ? extends JobEntity> entriesMap = entities.stream().collect(Collectors.toMap(JobEntity::getId, Function.identity()));
        
        jobs.forEach(job -> toUpdateDocument(job, entriesMap.get(job.getId())));
    }
    
    public void toUpdateDocument(Job job, JobEntity entity) {
        if (Objects.isNull(entity) || Objects.isNull(job)) {
            return;
        }
        entity.setVersion(job.getVersion());
        entity.setJobAsJson(jobMapper.serializeJob(job));
        entity.setState(job.getState().name());
        entity.setUpdatedAt(job.getUpdatedAt());
        if (job.hasState(StateName.SCHEDULED)) {
            entity.setUpdatedAt(((ScheduledState) job.getJobState()).getScheduledAt());
        }
        job.getRecurringJobId().ifPresent(entity::setRecurringJobId);
    }
    
    
    public Job toJob(JobEntity entity) {
        return jobMapper.deserializeJob(entity.getJobAsJson());
    }
    
    public RecurringJobsEntity toInsertDocument(RecurringJob recurringJob) {
        return RecurringJobsEntity.builder()
                                  .id(recurringJob.getId())
                                  .version(recurringJob.getVersion())
                                  .createdAt(recurringJob.getCreatedAt())
                                  .jobAsJson(jobMapper.serializeRecurringJob(recurringJob))
                                  .build();
        
    }
    
    public RecurringJob toRecurringJob(RecurringJobsEntity entity) {
        return jobMapper.deserializeRecurringJob(entity.getJobAsJson());
    }
    
}
