package me.insidezhou.southernquiet.logging;

import me.insidezhou.southernquiet.util.Pair;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class SouthernQuietLogFormatter {
    @SuppressWarnings("rawtypes")
    public Pair<String, List<?>> formatLogContext(SouthernQuietLogger.LogContext logContext) {
        String format = logContext.getContext().keySet().stream()
            .map(key -> key + "={}")
            .collect(Collectors.joining(", "));

        List<Object> parameters = logContext.getContext().values().stream()
            .map(v -> {
                if (v instanceof Supplier) {
                    return ((Supplier) v).get();
                }
                else {
                    return v;
                }
            })
            .collect(Collectors.toList());

        String msg = logContext.getMessage();
        String result;
        if (null != logContext.getThrowable()) {
            if (StringUtils.isEmpty(msg)) {
                msg = logContext.getThrowable().getMessage();
            }

            result = msg + "\t" + format + "\n" + logContext.getThrowable().toString();
        }
        else {
            result = msg + "\t" + format;
        }

        return new Pair<>(result, parameters);
    }
}
