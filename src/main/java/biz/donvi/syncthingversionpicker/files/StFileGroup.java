package biz.donvi.syncthingversionpicker.files;

import biz.donvi.syncthingversionpicker.StFolder;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
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
     *
     * @return A list of files in this file group.
     */
    public List<File> getFiles() {
        return files;
    }

    /**
     * Are there any files in this group that are NOT {@link StFile.Location#LocalReal}? True if yes, false otherwise.
     * <br/> For some added explanation: if this is true then there are extra versions of the main file, if it is false
     * then the only version of the file is the singular one in the real folder.
     *
     * @return {@code true} if there are version files, {@code false} if there is only the original real local file.
     */
    public boolean hasNonRealLocalFiles() {
        return files
            .stream()
            .map(f -> f.location)
            .distinct()
            .anyMatch(f -> f != Location.LocalReal);
    }

    public long countFiles(Location... locations) {
        EnumSet<Location> locs = EnumSet.of(locations[0], locations);
        return files.stream().filter(f -> locs.contains(f.location)).count();
    }

    @Override
    public Location getPrimaryLocation() {
        return location;
    }

    /**
     * Adds a new file to this file group, and updates the primary location of the group.
     *
     * @param file
     */
    void add(File file) {
        files.add(file);
        if (location == null || file.location.ordinal() < location.ordinal()) {
            location = file.location;
        }
        Collections.sort(files);
    }

    public abstract class File implements Comparable<File> {
        private static final DateTimeFormatter dfInput   = DateTimeFormatter.ofPattern("~yyyyMMdd-HHmmss");
        private static final DateTimeFormatter dfDisplay = DateTimeFormatter.ofPattern("yyyy-MM-dd hh:mm a");

        public final  String        nameRaw;
        public final  Location      location;
        private final String        timestamp;
        private final LocalDateTime localDateTime;

        File(String nameRaw, Location location, String timestamp) {
            this.nameRaw = nameRaw;
            this.location = location;
            this.timestamp = timestamp;
            this.localDateTime = "".equals(timestamp) ? null : LocalDateTime.parse(timestamp, dfInput);
        }

        public StFileGroup getParent() {
            return StFileGroup.this;
        }

        public String getTimeStamp() {
            return localDateTime != null
                ? localDateTime.format(dfDisplay)
                : "Current File";
        }

        public String getTimeAgo(LocalDateTime now) {
            return localDateTime != null
                ? durationToHumanString(Duration.between(localDateTime, now)) + " ago"
                : "";
        }

        private static String durationToHumanString(Duration dur) {
            if (dur.toMinutes() == 0)
                return String.format("%s seconds", dur.toSeconds());
            else if (dur.toMinutes() <= 60)
                return String.format("%s minutes, %s seconds", dur.toMinutes(), dur.toSeconds() % 60);
            else if (dur.toHours() < 48)
                return String.format("%s hours, %s minutes", dur.toHours(), dur.toMinutes() % 60);
            else
                return String.format("%s days, %s hours", dur.toDays(), dur.toHours() % 24);
        }


        /**
         * Gets the full path (starting at the system root) of the file's parent directory.
         * @return The full path of this file's directory.
         */
        public Path getFullFolderPath() {
            if (!location.isLocal) throw new UnsupportedOperationException("Not yet implemented.");
            Path path = Path.of(localStFolder.path());
            if (!location.isReal) path = path.resolve(STV);
            return path.resolve(relativePath).getParent();
        }

        /**
         * Gets the full path (starting at the system root) of this file.
         * @return The full path of this file.
         */
        public Path getFullRawPath() {
            return getFullFolderPath().resolve(nameRaw);
        }

        @Override
        public int compareTo(File o) {
            return this.timestamp.compareTo(o.timestamp);
        }


        public abstract InputStream getInputStream() throws FileNotFoundException;
    }

    public class LocalFile extends File {
        LocalFile(String nameRaw, Location location, String timestamp) {
            super(nameRaw, location, timestamp);
        }

        @Override
        public InputStream getInputStream() throws FileNotFoundException {
            return new FileInputStream(getFullRawPath().toFile());
        }
    }

    public class RemoteFile extends File {
        RemoteFile(String nameRaw, Location location, String timestamp) {
            super(nameRaw, location, timestamp);
        }

        @Override
        public InputStream getInputStream() {
            return null; // TODO: Where does this come from?
        }
    }

}
