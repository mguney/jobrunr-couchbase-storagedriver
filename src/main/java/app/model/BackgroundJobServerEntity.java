package app.model;

import com.couchbase.client.java.query.QueryScanConsistency;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.couchbase.core.mapping.Document;

@Builder
@Setter
@Getter
@Document(queryScanConsistency = QueryScanConsistency.REQUEST_PLUS)
public class BackgroundJobServerEntity {
    
    @Id
    private UUID id;
    
    private int version;
    
    private String name;
    private int workerPoolSize;
    private int pollIntervalInSeconds;
    private Duration deleteSucceededJobsAfter;
    private Duration permanentlyDeleteDeletedJobsAfter;
    private Instant firstHeartbeat;
    private Instant lastHeartbeat;
    private Boolean running;
    private Long systemTotalMemory;
    private Long systemFreeMemory;
    private Double systemCpuLoad;
    private Long processMaxMemory;
    private Long processFreeMemory;
    private Long processAllocatedMemory;
    private Double processCpuLoad;
}
