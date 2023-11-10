package biz.donvi.syncthingversionpicker;

import java.io.File;
import java.util.List;

public class SyncFile implements Comparable<SyncFile>{
    private File       file;
    private List<File> previousVersions;

    public SyncFile() {}

    public SyncFile(File file) {
        this.file = file;
    }
    public File getFile() {
        return file;
    }

    public void setFile(File file) {
        this.file = file;
    }

    public List<File> getPreviousVersions() {
        return previousVersions;
    }

    public void setPreviousVersions(List<File> previousVersions) {
        this.previousVersions = previousVersions;
    }

    @Override
    public int compareTo(SyncFile sf) {
        return this.file.compareTo(sf.file);
    }
}
