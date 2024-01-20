package biz.donvi.syncthingversionpicker.files;

import biz.donvi.syncthingversionpicker.StFolder;

import java.io.InputStream;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public final class StFileGroup extends StFile {

    private final List<File>  files = new ArrayList<>();

    private Location location = null;

    StFileGroup(StFolder localStFolder, StDirectory parentDir, Path relativePath) {
        super(localStFolder, relativePath, parentDir);
    }

    /**
     * Returns a list of files that match this file group. See {@link Location} for more information on what
     * type of file can be found.
     *
     * @return A list of files in this file group.
     */
    public List<File> getFiles() {
        return files;
    }

    /**
     * Are there any files in this group that are NOT {@link Location#LocalCurrent}? True if yes, false otherwise.
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
            .anyMatch(f -> f != Location.LocalCurrent);
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

    public class File implements Comparable<File> {
        private static final DateTimeFormatter dfInput   = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");
        private static final DateTimeFormatter dfDisplay = DateTimeFormatter.ofPattern("yyyy-MM-dd hh:mm a");

        public final  String        nameRaw;
        public final  Location      location;
        private final String        timestamp;
        private final LocalDateTime localDateTime;
        private final String        conflictor;

        File(String nameRaw, Location location, String timestamp, String conflictor) {
            this.nameRaw = nameRaw;
            this.location = location;
            this.timestamp = timestamp;
            this.localDateTime = "".equals(timestamp) ? null : LocalDateTime.parse(timestamp, dfInput);
            this.conflictor = conflictor != null ? conflictor : "";
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


        public Path getRawRelativePath() {
            Path parent = relativePath.getParent();
            return parent != null
                ? parent.resolve(nameRaw)
                : Path.of(nameRaw);
        }

        public Path getRawFullPath() {
            return parentDir.fullStLister.rootDir(location).resolve(getRawRelativePath());
        }


        @Override
        public int compareTo(File that) {
            if (this.localDateTime == null || that.localDateTime == null) {
                return this.location.compareTo(that.location);
            } else if (
                this.localDateTime.truncatedTo(ChronoUnit.SECONDS).equals(
                    that.localDateTime.truncatedTo(ChronoUnit.SECONDS))
            ) {
                return this.location.compareTo(that.location);
            } else {
                return -this.localDateTime.compareTo(that.localDateTime);
            }
        }


        public CompletableFuture<InputStream> getInputStream() {
            return parentDir.fullStLister.readFile(getRawRelativePath(), location);
        }
    }

}
