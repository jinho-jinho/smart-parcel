package com.capstone.smart_parcel.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.nio.file.Path;
import java.nio.file.Paths;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.storage")
public class StorageProperties {

    /**
     * The root directory on the server where images are stored.
     * Defaults to "${user.home}/smartparcel/images" when not configured.
     */
    private String imageDir = Paths.get(
            System.getProperty("user.home"),
            "smartparcel",
            "images"
    ).toString();

    /**
     * Public URL prefix that will be exposed for serving images.
     * Example: "/media".
     */
    private String publicUrlPrefix = "/media";

    public Path imageDirPath() {
        return Paths.get(imageDir).toAbsolutePath().normalize();
    }

    public String imageDirLocation() {
        return imageDirPath().toUri().toString();
    }

    /**
     * Returns a normalized public URL prefix that always starts with "/" and has no trailing slash.
     */
    public String publicUrlPrefixNormalized() {
        String prefix = (publicUrlPrefix == null || publicUrlPrefix.isBlank())
                ? "/media"
                : publicUrlPrefix.trim();
        if (!prefix.startsWith("/")) {
            prefix = "/" + prefix;
        }
        if (prefix.length() > 1 && prefix.endsWith("/")) {
            prefix = prefix.substring(0, prefix.length() - 1);
        }
        return prefix;
    }

    /**
     * Returns a Spring resource handler pattern, e.g. "/media/**".
     */
    public String publicUrlPattern() {
        return publicUrlPrefixNormalized() + "/**";
    }
}
