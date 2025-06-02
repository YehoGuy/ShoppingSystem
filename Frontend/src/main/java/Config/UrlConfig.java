package Config;

import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Component
public class UrlConfig {
    @Value("${url.api}")
    private String api;
    
    
    public String getApi() {
        return api;
    }
    public void setApi(String api) {
        this.api = api;
    }
}