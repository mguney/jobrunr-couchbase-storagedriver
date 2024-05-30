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
@Getter
@Setter
@Document(queryScanConsistency = QueryScanConsistency.REQUEST_PLUS)
public class MetadataEntity {
    
    @Id
    @Field
    private String id;
    
    private int version;
    
    private String name;
    private String owner;
    private Instant createdAt;
    private Instant updatedAt;
    private String value;
}
