package biz.donvi.syncthingversionpicker.files;

import biz.donvi.syncthingversionpicker.StFolder;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public final class StDirectory extends StFile {

    /**
     * A pattern for matching syncthing version files. These come in the format <pre>
     *     Original:    importantDocument.md
     *     Versioned:   importantDocument~20230829-151733.md
     *     Explanation:  ↑original-name↑ ~yyyymmdd-hhmmss ↑extention↑
     * </pre>
     * The timestamp is prefixed with a {@code ~}, and is inserted right before the last {@code .} in the file name.
     * <br/>
     * The pattern is grouped into 3 groups. Group {@code 0} is not one of them, it represents the entire expression.
     * Group {@code 1} matches the original filename before the extention, group {@code 2} matches the string timestamp,
     * and group {@code 3} matches the extension. Thus, to rebuild the original name from the versioned name, just
     * concatinate groups 1 & 3 together like so: {@code m.group(1) + m.group(3)}.
     */
    @SuppressWarnings("SpellCheckingInspection")
    private static final Pattern syncPattern = Pattern.compile("^(.*?)(~\\d{8}-\\d{6})(\\..*)$");

    /**
     * The location if this {@code StDirectory}. Unlike a {@link StFileGroup}, a {@code StDirectory} only ever has one
     * location. This is because for any given directory, all locations will be checked for additional files.
     */
    final Location location;

    /**
     * The lister used to list files in a given directory.
     */
    final LocationLister locationLister;

    StDirectory(StFolder localStFolder, LocationLister locationLister, Path relativePath, Location location) {
        super(localStFolder, relativePath);
        this.locationLister = locationLister;
        this.location = location;
    }

    @Override
    public Location getPrimaryLocation() {
        return location;
    }

    /**
     * Gets a list of {@link StFile}s that are within this directory using the provided
     * {@link LocationLister locationLister}. The returned list will contain files from all places listed in the
     * {@link biz.donvi.syncthingversionpicker.files.StFile.Location Location} enum (currently only 4 locations).
     * @return A list of {@code StFile}s that are in this directory.
     */
    public List<StFile> listFiles() {
        // Listing files for a directory, then collecting them into a map
        Map<String, List<FileWithInfo>> fileGroups = locationLister
            .listAllFiles(relativePath).stream()
            .map(FileWithInfo::into)
            .collect(Collectors.groupingBy(f -> f.sortName));
        // Then turn this into a map of StFileGroups
        List<StFile> children = new ArrayList<>();
        for (List<FileWithInfo> fileList : fileGroups.values()) {
            var fileZero = fileList.get(0);
            Path path = relativePath.resolve(fileZero.nameFixed);
            // The map is sorted both by the name of the file, and if it's a directory or not.
            // Because of that, we are able to just check if the first file in the list is a dir.
            if (fileZero.isDir) {
                // If we have a directory, we only need to show it in the tree once.
                Location mainLoc = fileList
                    .stream() // Take all the dirs that match this path and stream them
                    .map(f -> f.loc) // Map to the location type.
                    .distinct().sorted() // Sort so the first in the list is the one we want to represent.
                    .toList().get(0); // Take the first option in the list.
                // Resolve the path of the new directory.
                // Lastly, all we got to do is put this info into the
                children.add(new StDirectory(localStFolder, locationLister, path, mainLoc));
            } else {
                // To start, make a new file group.
                StFileGroup fileGroup = new StFileGroup(localStFolder, path);
                // Then, for each file in the list, add a new file to the file group
                for (FileWithInfo file : fileList)
                    fileGroup.add(fileGroup.new File(file.loc, file.timestamp));
                // And lastly, add it to the final result
                children.add(fileGroup);
            }
        }
        return children;
    }

    /**
     * A record purely for moving file info around conveniently.
     * @param loc The {@link biz.donvi.syncthingversionpicker.files.StFile.Location Location} that this file came from.
     * @param nameRaw The name exactly as written in the file system.
     * @param isDir {@code true} if this is a directory, {@code false} if it is a file.
     * @param nameFixed The name of the file with the syncthing version timestamp removed. Expect there to
     *                 <em>often</em> be multiple files with the same {@code nameFixed}. That's kinda central to the
     *                  point of the whole application.
     * @param sortName A name specifically for sorting files. This is almost the same as the {@link #nameFixed} except
     *                 that directories have a {@code !} appended to the end to make them sort differently in the map.
     *                 This name is not used for any reasons other than sorting the files.
     * @param timestamp The syncthing timestamp that was in the raw file name.
     */
    private record FileWithInfo(
        Location loc, String nameRaw, boolean isDir,
        String nameFixed, String sortName, String timestamp
    ) {
        /**
         * Converts a {@link biz.donvi.syncthingversionpicker.files.LocationLister.FileWithLocation FileWithLocation}
         * into a {@link FileWithInfo FileWithInfo}. This conversion adds additional information that is needed to
         * create {@link StFileGroup}s
         *
         * @param f The {@code FileWithLocation} to convert
         * @return A {@code FileWithInfo with additional information}.
         */
        static FileWithInfo into(LocationLister.FileWithLocation f) {
            Matcher m = syncPattern.matcher(f.name());
            String nameFixed = f.name();
            String timestamp = "";
            if (m.matches()) {
                nameFixed = m.group(1) + m.group(3);
                timestamp = m.group(2);
            }
            String sortName = nameFixed + (f.isDir() ? "!" : "");
            return new FileWithInfo(f.location(), f.name(), f.isDir(), nameFixed, sortName, timestamp);
        }

        @Override
        public String toString() {
            return "FileWithInfo{" +
                   "loc=" + loc +
                   ", nameRaw='" + nameRaw + '\'' +
                   ", isDir=" + isDir +
                   ", nameFixed='" + nameFixed + '\'' +
                   ", sortName='" + sortName + '\'' +
                   ", timestamp='" + timestamp + '\'' +
                   '}';
        }
    }

}
