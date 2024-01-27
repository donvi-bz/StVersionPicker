package biz.donvi.syncthingversionpicker.files;

import biz.donvi.syncthingversionpicker.StFolder;
import biz.donvi.syncthingversionpicker.files.Location.Where;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

public final class StFileGroup extends StFile {

    private static final Logger logger = LogManager.getLogger(StFileGroup.class);

    private final List<File> files = new ArrayList<>();

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
        private static final Logger            logger    = LogManager.getLogger(File.class);
        private static final Path              tmpdir    = Path.of(System.getProperty("java.io.tmpdir"))
                                                               .resolve("SyncThingVersionPicker.Files");
        private static final DateTimeFormatter dfInput   = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");
        private static final DateTimeFormatter dfDisplay = DateTimeFormatter.ofPattern("yyyy-MM-dd hh:mm a");

        static {
            logger.info("Clearing temporary directory `{}`", tmpdir);
            try (Stream<Path> pathStream = Files.walk(tmpdir)) {
                pathStream.filter(path -> path != tmpdir)
                          .sorted(Comparator.reverseOrder())
                          .map(Path::toFile)
                          .forEach(file -> {
                              logger.trace("Deleting temporary file `{}`", file.getPath());
                              file.delete();
                          });
            } catch (IOException e) {
                logger.warn("Couldn't walk file tree.", e);
            }
        }

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

        /**
         * Gets the raw relative path of this file. For instance, a file located in the root stfolder will have a path
         * resembling {@code fileName.md}. Since this is a relative path, there is no difference between how local
         * and remote
         * paths are returned. This is called the `raw` path because it will include any syncthing text after it, like
         * {@code fileName~20201201-125465.md}
         *
         * @return The relative path of this file.
         */
        public Path getRawRelativePath() {
            Path parent = relativePath.getParent();
            return parent != null
                ? parent.resolve(nameRaw)
                : Path.of(nameRaw);
        }

        /**
         * Gets the raw full path of this file. Just like {@link #getRawRelativePath()}, this will contain the literal
         * file name which may have a syncthing date or similar after it. Unlike that method, the path returned here
         * <b>will</b> be different for local and remote files. For instance, on windows it will look like
         * {@code C:/SomeRootDir/SyncthingRootFolder/fileName.md}
         *
         * @return
         */
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

        @Override
        public String toString() {
            return "File{" +
                   "nameRaw='" + nameRaw + '\'' +
                   ", location=" + location +
//                   ", timestamp='" + timestamp + '\'' +
//                   ", localDateTime=" + localDateTime +
//                   ", conflictor='" + conflictor + '\'' +
                   '}';
        }

        /**
         * Returns a {@link CompletableFuture} of a {@link InputStream} that can be used to read this file.
         * This is useful for any time that we want to read a file and be sure we get the current data. This
         * returns a future type because we may be getting this input stream from a remote connection. </br>
         * If you'd prefer to receive a file AND are fine with the chance that it's just a temporary file, use
         * {@link #getLocalFile()} instead.
         *
         * @return A {@link CompletableFuture} of a {@link InputStream} that can be used to read this file.
         */
        public CompletableFuture<InputStream> getInputStream() {
            logger.debug("Input stream requested for file `{}`", this);
            logger.trace("Raw path: `{}` \t Location: {}", getRawFullPath(), location);
            return parentDir.fullStLister.readFile(getRawRelativePath(), location);
        }

        /**
         * Returns the path that a temporary {@link java.io.File} should exist for this file. <br/>
         * Note: this is <b>only</b> for {@link Where#Local Local} files.
         *
         * @return
         */
        private Path getTempLocation() {
            if (location.where == Where.Local)
                throw new RuntimeException("Local files can't have temporary paths!");
            return tmpdir.resolve(localStFolder.label()).resolve(getRawRelativePath());
        }

        /**
         * Returns a {@link CompletableFuture} of a {@link java.io.File} that holds this file's data. </br>
         * Note: If this is a remote file, a copy will be downloaded and saved to this computer's temporary path. </br>
         * For direct access to an {@link InputStream}, use {@link #getInputStream()} instead.
         *
         * @return A {@link CompletableFuture} of a {@link java.io.File} that holds this file's data. </br>
         * Note: For remote files, this will be a temporary file.
         */
        public CompletableFuture<java.io.File> getLocalFile() {
            // Mostly defining this so that I don't accidentally use `when` when I mean `where`.
            final Where where = location.where;
            // The java.io.File that should exist for this file.
            java.io.File file = where.which(this::getRawFullPath, this::getTempLocation).toFile();
            file.deleteOnExit();
            // And this is an async method, so we got one of these too.
            CompletableFuture<java.io.File> future = new CompletableFuture<>();
            // Now make sure it exists...
            if (!file.exists()) switch (where) {
                case Local -> {
                    logger.error("Local file `{}` somehow doesn't exist.", this);
                    var e = new FileNotFoundException("Somehow file %s does not exist".formatted(this));
                    future.completeExceptionally(e);
                }
                case Remote -> getInputStream().whenCompleteAsync((in, ex) -> {
                    if (ex != null) {
                        logger.warn("Could not get input stream for file `{}`. Forwarding exception.", this);
                        future.completeExceptionally(new Exception(ex));
                    }
                    if (in != null) try {
                        //noinspection ResultOfMethodCallIgnored
                        file.mkdirs();
                        Path path = file.toPath();
                        Files.copy(in, path, StandardCopyOption.REPLACE_EXISTING);
                        logger.debug("Successfully copied remote file `{}` to temp location `{}`", this, path);
                        future.complete(file);
                    } catch (IOException e) {
                        logger.warn("Received input stream, but could not copy file %s".formatted(this));
                        future.completeExceptionally(e);
                    }
                });
            }
            else {
                switch (where) {
                    case Local -> logger.debug(
                        "Local file requested for already local file. " +
                        "Completing future immediately for with `{}`", file);
                    case Remote -> logger.debug(
                        "Remote file has already been copied to the temp directory. " +
                        "Completing future immediately with `{}`", file);
                }
                future.complete(file);
            }
            return future;
        }
    }

}
