package me.insidezhou.southernquiet.job.driver;

public class ProcessorNotFoundException extends RuntimeException {
    public ProcessorNotFoundException(String message) {
        super(message);
    }
}
