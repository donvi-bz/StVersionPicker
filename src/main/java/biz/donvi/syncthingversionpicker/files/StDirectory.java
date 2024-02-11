package biz.donvi.syncthingversionpicker.files;

import biz.donvi.syncthingversionpicker.StFolder;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public final class StDirectory extends StFile {

    /**
     * The location if this {@code StDirectory}. Unlike a {@link StFileGroup}, a {@code StDirectory} only ever has one
     * location. This is because for any given directory, all locations will be checked for additional files.
     */
    public final Location location;

    /**
     * The lister used to list files in a given directory.
     */
    private final FullStLister fullStLister;

    StDirectory(
        StFolder localStFolder, FullStLister fullStLister, Path relativePath, Location location, StDirectory parentDir
    ) {
        super(localStFolder, relativePath, parentDir);
        this.fullStLister = fullStLister;
        this.location = location;
    }

    @Override
    public Location getPrimaryLocation() {
        return location;
    }

    /**
     * <b>FIXME: OUTDATED</b><br/>
     * Gets a list of {@link StFile}s that are within this directory using the provided
     * {@link FullStLister locationLister}. The returned list will contain files from all places listed in the
     * {@link Location Location} enum (currently only 4 locations).
     *
     * @return A list of {@code StFile}s that are in this directory.
     */
    public CompletableFuture<List<StFile>> listFilesAsync() {
        // Listing files for a directory, then collecting them into a map
        return fullStLister
            .listAllFiles(relativePath)
            .thenApplyAsync(files -> {
                Map<String, List<FileWithInfo>> fileGroups = files
                    .stream()
                    .map(FileWithInfo::into)
                    .collect(Collectors.groupingBy(FileWithInfo::sortName));
                // Then turn this into a map of StFileGroups
                List<StFile> children = new ArrayList<>();
                for (List<FileWithInfo> fileList : fileGroups.values()) {
                    var fileZero = fileList.get(0);
                    Path path = relativePath.resolve(fileZero.nameInfo.nameFixed());
                    // The map is sorted both by the name of the file, and if it's a directory or not.
                    // Because of that, we are able to just check if the first file in the list is a dir.
                    if (fileZero.isDir) {
                        // If we have a directory, we only need to show it in the tree once.
                        Location mainLoc = fileList
                            .stream() // Take all the dirs that match this path and stream them
                            .map(FileWithInfo::loc) // Map to the location type.
                            .distinct().sorted() // Sort so the first in the list is the one we want to represent.
                            .toList().get(0); // Take the first option in the list.
                        // Resolve the path of the new directory.
                        // Lastly, all we got to do is put this info into the
                        children.add(new StDirectory(localStFolder, fullStLister, path, mainLoc, parentDir));
                    } else {
                        // To start, make a new file group.
                        StFileGroup fileGroup = new StFileGroup(localStFolder, this, path);
                        // Then, for each file in the list, add a new file to the file group
                        for (FileWithInfo file : fileList)
                            fileGroup.add(fileGroup.new File(file.nameInfo,file.loc));
                        // And lastly, add it to the final result
                        children.add(fileGroup);
                    }
                }
                return children;
            });
    }

    public FullStLister getFullStLister() {
        return fullStLister;
    }

    /**
     * A record purely for moving file info around conveniently.
     *
     * @param loc       The {@link Location Location} that this file came from.
     * @param nameRaw   The name exactly as written in the file system.
     * @param isDir     {@code true} if this is a directory, {@code false} if it is a file.
     * @param nameFixed The name of the file with the syncthing version timestamp removed. Expect there to
     *                  <em>often</em> be multiple files with the same {@code nameFixed}. That's kinda central to the
     *                  point of the whole application.
     * @param sortName  A name specifically for sorting files. This is almost the same as the {@link #nameFixed} except
     *                  that directories have a {@code !} appended to the end to make them sort differently in the map.
     *                  This name is not used for any reasons other than sorting the files.
     * @param timestamp The syncthing timestamp that was in the raw file name.
     */
    private record FileWithInfo(Location loc, ParsedFileName nameInfo, boolean isDir, String sortName) {
        /**
         * Converts a {@link DirectoryLister.FileWithLocation FileWithLocation}
         * into a {@link FileWithInfo FileWithInfo}. This conversion adds additional information that is needed to
         * create {@link StFileGroup}s
         *
         * @param f The {@code FileWithLocation} to convert
         * @return A {@code FileWithInfo with additional information}.
         */
        static FileWithInfo into(DirectoryLister.FileWithLocation f) {
            ParsedFileName parsedFile = new ParsedFileName(f.name());
            String sortName = parsedFile.nameFixed() + (f.isDir() ? "!" : "");
            return new FileWithInfo(f.location(), parsedFile, f.isDir(), sortName);
        }

    }

}
