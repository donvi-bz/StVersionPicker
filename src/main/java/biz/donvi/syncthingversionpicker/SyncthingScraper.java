package biz.donvi.syncthingversionpicker;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import javax.net.ssl.*;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.GeneralSecurityException;
import java.security.cert.X509Certificate;
import java.util.List;

import static java.net.HttpURLConnection.HTTP_OK;

public class SyncthingScraper {

    public static final String ST_LIST_FOLDERS = "/rest/config/folders";

    private final ObjectMapper mapper = new ObjectMapper()
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private final String url;
    private final String apikey;

    private final ObservableList<StFolder> folders = FXCollections.observableArrayList();

    static {
        // FIXME: Not a good solution
        TrustManager[] trustAllCerts = new TrustManager[]{
            new X509TrustManager() {
                public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                    return new X509Certificate[0];
                }

                public void checkClientTrusted(java.security.cert.X509Certificate[] certs, String authType) {}

                public void checkServerTrusted(java.security.cert.X509Certificate[] certs, String authType) {}
            }
        };

        // Install the all-trusting trust manager
        try {
            SSLContext sc = SSLContext.getInstance("SSL");
            sc.init(null, trustAllCerts, new java.security.SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
        } catch (GeneralSecurityException e) {
        }
    }

    public SyncthingScraper(String url, String apiKey) {
        this.url = url;
        this.apikey = apiKey;
    }

    public record TestResult(boolean valid, String msg, SyncthingScraper self) {}

    public TestResult testConnection() {
        try {
            URL url = new URL(this.url + ST_LIST_FOLDERS);
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            if (con instanceof HttpsURLConnection httpsCon) {
                httpsCon.setHostnameVerifier((hostname, sslSession) -> true);
            }
            con.setRequestProperty("X-API-Key", apikey);
            con.setConnectTimeout(5000);
            con.connect();
            int res = con.getResponseCode();
            return res == HTTP_OK
                ? new TestResult(true, "Connected", this)
                : new TestResult(false, "%d %s".formatted(res, con.getResponseMessage()), this);
        } catch (IOException e) {
            return new TestResult(false, e.getLocalizedMessage(), this);
        }
    }

    private <T> T getEndpoint(String endpoint, Class<T> clazz) throws IOException {
        HttpURLConnection con = (HttpURLConnection) new URL(this.url + endpoint).openConnection();
        if (con instanceof HttpsURLConnection httpsCon) {
            httpsCon.setHostnameVerifier((hostname, sslSession) -> true);
        }
        con.setRequestProperty("X-API-Key", apikey);
        con.setConnectTimeout(5000);
        con.connect();
        return mapper.readValue(con.getInputStream(), clazz);
    }

//    public StFolder getEndpoint(String endpoint, RemoteLister remote) {
//        try {
//            return remote.getRemoteFolder(endpoint, url.substring(url.indexOf("://") + 3), apikey);
//        } catch (JSchException e) {
//            throw new RuntimeException(e);
//        }
//    }

    public void updateFolders() throws IOException {
        this.folders.setAll(List.of(getEndpoint(ST_LIST_FOLDERS, StFolder[].class)));
    }

    public ObservableList<StFolder> getFolders() {
        return folders.sorted((o1, o2) -> o1.label().compareToIgnoreCase(o2.label()));
    }


    public static final SyncthingScraper EmptyScraper = new SyncthingScraper("", "") {
        @Override
        public TestResult testConnection() {
            return new TestResult(true, "This is a fake connection.", this);
        }

        @Override
        public void updateFolders() {
        }

    };

}
