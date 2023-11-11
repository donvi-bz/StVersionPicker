package biz.donvi.syncthingversionpicker;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

public class SyncthingScraper {

    private static final String ST_LIST_FOLDERS = "/rest/config/folders";

    private final ObjectMapper mapper = new ObjectMapper()
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private final String url;
    private final String apikey;

    private final ObservableList<StFolder> folders = FXCollections.observableArrayList();

    public SyncthingScraper(String url, String apiKey) {
        this.url = url;
        this.apikey = apiKey;
    }

    private <T> T getEndpoint(String endpoint, Class<T> clazz) throws IOException {
        HttpURLConnection con = (HttpURLConnection) new URL(this.url + endpoint).openConnection();
        con.setRequestProperty("X-API-Key", apikey);
        con.connect();
        return mapper.readValue(con.getInputStream(), clazz);
    }

    public void updateFolders() throws IOException {
        this.folders.setAll(List.of(getEndpoint(ST_LIST_FOLDERS, StFolder[].class)));
    }

    public ObservableList<StFolder> getFolders() {
        return folders.sorted((o1, o2) -> o1.label().compareToIgnoreCase(o2.label()));
    }

}
