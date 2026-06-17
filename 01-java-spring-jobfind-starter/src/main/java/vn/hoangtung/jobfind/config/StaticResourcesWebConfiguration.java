package vn.hoangtung.jobfind.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class StaticResourcesWebConfiguration implements WebMvcConfigurer {

    @Value("${hoangtung.upload-file.base-uri}")
    private String baseURI;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Map /storage/** URL pattern to the upload directory
        // so that company logos and other public images can be served directly.
        // Example: /storage/company/fpt-logo.png -> file:/D:/Upload/company/fpt-logo.png
        registry.addResourceHandler("/storage/**")
                .addResourceLocations(baseURI);
    }
}
