package Config;

import org.springframework.beans.factory.annotation.Autowired;

public class UrlService {
    private static String url;

    @Autowired
    public UrlService(UrlConfig injectedConfig) {
        url = new UrlConfig().getApi();
    }

    public static String getApiUrl() {
        return url;
    }
}
