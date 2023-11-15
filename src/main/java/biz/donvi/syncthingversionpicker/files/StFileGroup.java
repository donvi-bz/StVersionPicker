package biz.donvi.syncthingversionpicker.files;

import biz.donvi.syncthingversionpicker.StFolder;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class StFileGroup extends StFile {

    private final List<File> files = new ArrayList<>();

    private Location location = null;

    StFileGroup(StFolder localStFolder, Path relativePath) {
        super(localStFolder, relativePath);
    }

    /**
     * Returns a list of files that match this file group. See {@link StFile.Location} for more information on what
     * type of file can be found.
     * @return A list of files in this file group.
     */
    public List<File> getFiles() {
        return files;
    }

    /**
     * Are there any files in this group that are NOT {@link StFile.Location#LocalReal}? True if yes, false otherwise.
     * <br/> For some added explanation: if this is true then there are extra versions of the main file, if it is false
     * then the only version of the file is the singular one in the real folder.
     * @return {@code true} if there are version files, {@code false} if there is only the original real local file.
     */
    public boolean hasNonRealLocalFiles() {
        return files
            .stream()
            .map(f -> f.location)
            .distinct()
            .anyMatch(f -> f != Location.LocalReal);
    }

    @Override
    public Location getPrimaryLocation() {
        return location;
    }

    /**
     * Adds a new file to this file group, and updates the primary location of the group.
     * @param file
     */
    void add(File file) {
        files.add(file);
        if (location == null || file.location.ordinal() < location.ordinal()) {
            location = file.location;
        }
    }

    public class File {
        public final Location location;
        public final String   timestamp;

        File(Location location, String timestamp) {
            this.location = location;
            this.timestamp = timestamp;
        }
    }

}
