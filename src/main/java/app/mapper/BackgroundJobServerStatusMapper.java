package app.mapper;

import app.model.BackgroundJobServerEntity;
import java.time.ZoneId;
import org.jobrunr.storage.BackgroundJobServerStatus;

public class BackgroundJobServerStatusMapper {
    
    public BackgroundJobServerEntity toInsertDocument(BackgroundJobServerStatus serverStatus) {
        return BackgroundJobServerEntity.builder()
                                        .id(serverStatus.getId())
                                        .name(serverStatus.getName())
                                        .workerPoolSize(serverStatus.getWorkerPoolSize())
                                        .pollIntervalInSeconds(serverStatus.getPollIntervalInSeconds())
                                        .deleteSucceededJobsAfter(serverStatus.getDeleteSucceededJobsAfter())
                                        .permanentlyDeleteDeletedJobsAfter(serverStatus.getPermanentlyDeleteDeletedJobsAfter())
                                        .firstHeartbeat(serverStatus.getFirstHeartbeat())
                                        .lastHeartbeat(serverStatus.getLastHeartbeat())
                                        .running(serverStatus.isRunning())
                                        .systemTotalMemory(serverStatus.getSystemTotalMemory())
                                        .systemFreeMemory(serverStatus.getSystemFreeMemory())
                                        .systemCpuLoad(serverStatus.getSystemCpuLoad())
                                        .processMaxMemory(serverStatus.getProcessMaxMemory())
                                        .processFreeMemory(serverStatus.getProcessFreeMemory())
                                        .processAllocatedMemory(serverStatus.getProcessAllocatedMemory())
                                        .processCpuLoad(serverStatus.getProcessCpuLoad())
                                        .build();
    }
    
    public void toUpdateDocument(BackgroundJobServerStatus serverStatus, BackgroundJobServerEntity backgroundJobServerEntity) {
        backgroundJobServerEntity.setLastHeartbeat(serverStatus.getLastHeartbeat());
        backgroundJobServerEntity.setSystemFreeMemory(serverStatus.getSystemFreeMemory());
        backgroundJobServerEntity.setSystemCpuLoad(serverStatus.getSystemCpuLoad());
        backgroundJobServerEntity.setProcessFreeMemory(serverStatus.getProcessFreeMemory());
        backgroundJobServerEntity.setProcessAllocatedMemory(serverStatus.getProcessAllocatedMemory());
        backgroundJobServerEntity.setProcessCpuLoad(serverStatus.getProcessCpuLoad());
    }
    
    public BackgroundJobServerStatus toBackgroundJobServerStatus(BackgroundJobServerEntity entity) {
        return new BackgroundJobServerStatus(entity.getId(),
                                             entity.getName(),
                                             entity.getWorkerPoolSize(),
                                             entity.getPollIntervalInSeconds(),
                                             entity.getDeleteSucceededJobsAfter(),
                                             entity.getPermanentlyDeleteDeletedJobsAfter(),
                                             entity.getFirstHeartbeat(),
                                             entity.getLastHeartbeat(),
                                             entity.getRunning(),
                                             entity.getSystemTotalMemory(),
                                             entity.getSystemFreeMemory(),
                                             entity.getSystemCpuLoad(),
                                             entity.getProcessMaxMemory(),
                                             entity.getProcessFreeMemory(),
                                             entity.getProcessAllocatedMemory(),
                                             entity.getProcessCpuLoad());
    }
}
