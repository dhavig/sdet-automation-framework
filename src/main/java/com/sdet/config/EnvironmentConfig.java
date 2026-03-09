package com.sdet.config;

//import com.sdet.config.ConfigReader;

public class EnvironmentConfig {

    public enum Environment {
        ENV1, ENV2
    }

    private final String baseUrl;
    private final String username;
    private final String password;
    private final String name;

    private EnvironmentConfig(String baseUrl, String username, String password, String name) {
        this.baseUrl = baseUrl;
        this.username = username;
        this.password = password;
        this.name = name;
    }

    public static EnvironmentConfig load(Environment env) {
        String prefix = env == Environment.ENV1 ? "env1" : "env2";
        return new EnvironmentConfig(
                ConfigReader.getProperty(prefix + ".base.url"),
                ConfigReader.getProperty(prefix + ".username"),
                ConfigReader.getProperty(prefix + ".password"),
                ConfigReader.getProperty(prefix + ".name")
        );
    }

    public String getBaseUrl()  { return baseUrl; }
    public String getUsername() { return username; }
    public String getPassword() { return password; }
    public String getName()     { return name; }

    @Override
    public String toString() {
        return String.format("Environment[%s | URL: %s | User: %s]", name, baseUrl, username);
    }
}