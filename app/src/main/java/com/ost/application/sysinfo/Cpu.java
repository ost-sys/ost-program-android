package com.ost.application.sysinfo;

public class Cpu {
    private String model;
    private String cores;
    private String architecture;
    private String revision;
    private String process;
    private String clock_speed;
    private String cpu_load;
    private String gpu_vendor;
    private String gpu_renderer;
    private String gpu_clock_speed;

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getCores() {
        return cores;
    }

    public void setCores(String cores) {
        this.cores = cores;
    }

    public String getArchitecture() {
        return architecture;
    }

    public void setArchitecture(String architecture) {
        this.architecture = architecture;
    }

    public String getRevision() {
        return revision;
    }

    public void setRevision(String revision) {
        this.revision = revision;
    }

    public String getProcess() {
        return process;
    }

    public void setProcess(String process) {
        this.process = process;
    }

    public String getClock_speed() {
        return clock_speed;
    }

    public void setClock_speed(String clock_speed) {
        this.clock_speed = clock_speed;
    }

    public String getCpu_load() {
        return cpu_load;
    }

    public void setCpu_load(String cpu_load) {
        this.cpu_load = cpu_load;
    }

    public String getGpu_vendor() {
        return gpu_vendor;
    }

    public void setGpu_vendor(String gpu_vendor) {
        this.gpu_vendor = gpu_vendor;
    }

    public String getGpu_renderer() {
        return gpu_renderer;
    }

    public void setGpu_renderer(String gpu_renderer) {
        this.gpu_renderer = gpu_renderer;
    }

    public String getGpu_clock_speed() {
        return gpu_clock_speed;
    }

    public void setGpu_clock_speed(String gpu_clock_speed) {
        this.gpu_clock_speed = gpu_clock_speed;
    }
}
