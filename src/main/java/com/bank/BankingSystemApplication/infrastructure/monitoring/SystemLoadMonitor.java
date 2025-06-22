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
    
    @Value("${app.load.cpu-threshold:70.0}")
    private double cpuThreshold;
    
    @Value("${app.load.connection-threshold:100}")
    private int connectionThreshold;
    
    public SystemLoadMonitor() {
        this.osBean = (com.sun.management.OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
    }
    
    public double getCurrentCpuUsage() {
        return osBean.getProcessCpuLoad() * 100;
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