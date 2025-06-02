package Config;

import org.springframework.beans.factory.annotation.Autowired;

public class UrlService {
    private static UrlConfig config;

    @Autowired
    public UrlService(UrlConfig injectedConfig) {
        UrlService.config = injectedConfig;
    }

    public static String getApiUrl() {
        return config.getApi();
    }
}
