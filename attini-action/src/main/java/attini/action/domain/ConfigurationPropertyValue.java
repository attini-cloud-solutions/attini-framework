/*
 * Copyright (c) 2020 Attini Cloud Solutions AB.
 * All Rights Reserved
 */

package attini.action.domain;

import static java.util.Objects.requireNonNull;

import java.util.Optional;

import attini.action.StackConfigException;

public class ConfigurationPropertyValue {

    private final String value;
    private final boolean fallback;
    private final Type type;
    private final String defaultValue;


    private ConfigurationPropertyValue(String value, boolean fallback, Type type, String defaultValue) {
        this.value = requireNonNull(value, "value");
        this.fallback = fallback;
        this.type = requireNonNull(type, "type");
        this.defaultValue = defaultValue;
    }

    public String getValue() {
        return value;
    }

    public boolean isFallback() {
        return fallback;
    }

    public Type getType() {
        return type;
    }

    public Optional<String> getDefaultValue() {
        return Optional.ofNullable(defaultValue);
    }

    public static ConfigurationPropertyValue create(String value){
        return new ConfigurationPropertyValue(value, false, Type.STRING, null);
    }

    public static ConfigurationPropertyValue create(String value, boolean fallback){
        return new ConfigurationPropertyValue(value, fallback, Type.STRING, null);
    }

    public static ConfigurationPropertyValue create(String value, Type type){
        return new ConfigurationPropertyValue(value, false, type, null);
    }
    public static ConfigurationPropertyValue create(String value, boolean fallback, Type type, String defaultValue){
        return new ConfigurationPropertyValue(value, fallback, type, defaultValue);
    }

    @Override
    public String toString() {
        return "ConfigurationProperty{" +
               "value='" + value + '\'' +
               ", fallback=" + fallback +
               '}';
    }

    public enum Type{
        STRING, SSM_PARAMETER;

        public static Type mapType(String value){
            if (value.equalsIgnoreCase("string")){
                return Type.STRING;
            } else if (value.equalsIgnoreCase("ssm-parameter")){
                return SSM_PARAMETER;
            }
            throw new StackConfigException("unknown config type="+value);
        }
    }
}
