package com.bank.BankingSystemApplication.infrastructure.monitoring;

import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;

import java.lang.management.ManagementFactory;
import com.sun.management.OperatingSystemMXBean;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class SystemLoadMonitor {
    
    private final OperatingSystemMXBean osBean;
    private final AtomicInteger activeConnections = new AtomicInteger(0);
    
    // CPU usage cache to avoid expensive calls
    private volatile double cachedCpuUsage = 0.0;
    private volatile long lastCpuUpdate = 0;
    private static final long CPU_CACHE_DURATION_MS = 1000; // Cache for 1 second
    
    @Value("${app.load.cpu-threshold:70.0}")
    private double cpuThreshold;
    
    @Value("${app.load.connection-threshold:100}")
    private int connectionThreshold;
    
    public SystemLoadMonitor() {
        this.osBean = (com.sun.management.OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
    }
    
    public double getCurrentCpuUsage() {
        long now = System.currentTimeMillis();
        if (now - lastCpuUpdate > CPU_CACHE_DURATION_MS) {
            cachedCpuUsage = osBean.getProcessCpuLoad() * 100;
            lastCpuUpdate = now;
        }
        return cachedCpuUsage;
    }
    
    public int getActiveConnections() {
        return activeConnections.get();
    }
    
    public void incrementConnections() {
        activeConnections.incrementAndGet();
    }
    
    public void decrementConnections() {
        activeConnections.decrementAndGet();
    }
    
    public boolean shouldUseAsyncProcessing() {
        double currentCpu = getCurrentCpuUsage();
        int currentConnections = getActiveConnections();
        
        return currentCpu > cpuThreshold || currentConnections > connectionThreshold;
    }
    
    public boolean isHighLoad() {
        return shouldUseAsyncProcessing();
    }
}