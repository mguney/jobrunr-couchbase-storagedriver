package app;

import static java.util.Arrays.asList;
import static org.apache.naming.ResourceRef.SCOPE;

import com.couchbase.client.java.Bucket;
import com.couchbase.client.java.Cluster;
import com.couchbase.client.java.manager.bucket.BucketSettings;
import com.couchbase.client.java.manager.collection.CollectionManager;
import com.couchbase.client.java.manager.collection.CollectionSpec;
import com.couchbase.client.java.manager.collection.ScopeSpec;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.jobrunr.JobRunrException;
import org.jobrunr.storage.StorageProviderUtils.BackgroundJobServers;
import org.jobrunr.storage.StorageProviderUtils.Jobs;
import org.jobrunr.storage.StorageProviderUtils.Metadata;
import org.jobrunr.storage.StorageProviderUtils.RecurringJobs;

public class CouchbaseDBCreator {
    
    private final Cluster cluster;
    
    private final String bucketName;
    
    protected CouchbaseDBCreator(Cluster cluster, String bucketName) {
        this.cluster = cluster;
        this.bucketName = bucketName;
    }
    
    
    protected void runMigration() {
        Bucket bucket = cluster.bucket(this.bucketName);
        CollectionManager collectionManager = bucket.collections();
        List<ScopeSpec> allScopes = collectionManager.getAllScopes();
        if (allScopes.stream().noneMatch(scopeSpec -> scopeSpec.name().equals(SCOPE))) {
            collectionManager.createScope(SCOPE);
        }
        ScopeSpec scopeSpec = collectionManager.getAllScopes()
                                               .stream()
                                               .filter(spec -> spec.name().equals(SCOPE))
                                               .findFirst()
                                               .orElseThrow(() -> new JobRunrException("Not all required scope are available by JobRunr!"));
        Set<String> collections = scopeSpec.collections().stream().map(CollectionSpec::name).collect(Collectors.toSet());
        final List<String> requiredCollectionNames = asList(Jobs.NAME, RecurringJobs.NAME, BackgroundJobServers.NAME, Metadata.NAME);
        for (String requiredCollectionName : requiredCollectionNames) {
            if (!collections.contains(requiredCollectionName)) {
                collectionManager.createCollection(CollectionSpec.create(requiredCollectionName, SCOPE));
            }
        }
    }
    
    
    public void validateCollections() {
        Map<String, BucketSettings> allBuckets = cluster.buckets().getAllBuckets();
        if (!allBuckets.containsKey(bucketName)) {
            throw new JobRunrException("Not all required bucket are available by JobRunr!");
        }
        Bucket bucket = cluster.bucket(this.bucketName);
        List<ScopeSpec> allScopes = bucket.collections().getAllScopes();
        ScopeSpec scopeSpec = allScopes.stream()
                                       .filter(spec -> spec.name().equals(SCOPE))
                                       .findFirst()
                                       .orElseThrow(() -> new JobRunrException("Not all required scope are available by JobRunr!"));
        
        Set<String> collections = scopeSpec.collections().stream().map(CollectionSpec::name).collect(Collectors.toSet());
        final List<String> requiredCollectionNames = asList(Jobs.NAME, RecurringJobs.NAME, BackgroundJobServers.NAME, Metadata.NAME);
        for (String requiredCollectionName : requiredCollectionNames) {
            if (!collections.contains(requiredCollectionName)) {
                throw new JobRunrException("Not all required collections are available by JobRunr!");
            }
        }
        
        
    }
}
