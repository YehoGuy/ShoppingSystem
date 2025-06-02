package Config;

import org.springframework.stereotype.Component;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Component
@ConfigurationProperties(prefix = "url")
public class UrlConfig {
    
    private String api;
    
    
    public String getApi() {
        return api;
    }
    public void setApi(String api) {
        this.api = api;
    }
}