package app.user;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.springframework.data.annotation.Id;
import org.springframework.data.couchbase.core.mapping.Document;
import org.springframework.data.couchbase.core.mapping.id.GeneratedValue;
import org.springframework.data.couchbase.core.mapping.id.GenerationStrategy;

@ToString
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Document
public class User {
    
    @Id
    @GeneratedValue(strategy = GenerationStrategy.UNIQUE)
    private String id;
    
    private String firstName;
    private String lastName;
    private String email;
}
