package com.amazonaws.services.neptune.examples.utils;

public class EnvironmentVariableUtils {
    public static String getMandatoryEnv(String name) {

        if (isNullOrEmpty(System.getenv(name))) {

            throw new IllegalStateException(String.format("Missing environment variable: %s", name));
        }
        return System.getenv(name);
    }

    public static String getOptionalEnv(String name, String defaultValue) {
        if (isNullOrEmpty(System.getenv(name))) {
            return defaultValue;
        }
        return System.getenv(name);
    }

    private static boolean isNullOrEmpty(String value) {
        return value == null || value.isEmpty();
    }
}
