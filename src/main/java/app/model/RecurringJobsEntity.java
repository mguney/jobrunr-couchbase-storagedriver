package app.model;

import com.couchbase.client.java.query.QueryScanConsistency;
import java.time.Instant;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.couchbase.core.mapping.Document;
import org.springframework.data.couchbase.core.mapping.Field;

@Builder
@Setter
@Getter
@Document(queryScanConsistency = QueryScanConsistency.REQUEST_PLUS)
public class RecurringJobsEntity {
    
    @Id
    @Field
    private String id;
    
    private int version;
    private String jobAsJson;
    private Instant createdAt;
}
