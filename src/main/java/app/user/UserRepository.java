package app.user;

import com.couchbase.client.java.query.QueryScanConsistency;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.couchbase.core.query.WithConsistency;
import org.springframework.data.couchbase.repository.Collection;
import org.springframework.data.couchbase.repository.CouchbaseRepository;
import org.springframework.data.couchbase.repository.Query;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.ListPagingAndSortingRepository;
import org.springframework.stereotype.Repository;

@Collection("User")
@Repository
public interface UserRepository extends CouchbaseRepository<User, String> {
    
    
    List<User> findAllByFirstNameStartsWith(String firstName);
    
    List<UserRecordNameSurname> findAllByLastNameStartsWith(String firstName);
    
    @WithConsistency(QueryScanConsistency.REQUEST_PLUS)
    @Query("SELECT firstName AS __id, firstName, COUNT(*) as count FROM `User` WHERE #{#n1ql.filter} GROUP BY firstName order by count(*) desc")
    List<UserRecord> countByFirstName();
}
