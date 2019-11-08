package test.job;

public class JobException extends Exception {
    private int failureCount = 0;

    public int getFailureCount() {
        return failureCount;
    }

    public void setFailureCount(int failureCount) {
        this.failureCount = failureCount;
    }
}
