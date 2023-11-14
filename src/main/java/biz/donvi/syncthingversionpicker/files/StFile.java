package biz.donvi.syncthingversionpicker.files;

import biz.donvi.syncthingversionpicker.StFolder;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * A {@code StFile} represents either a file with multiple version, or a directory.
 */
public abstract sealed class StFile implements Comparable<StFile> permits StDirectory, StFileGroup {

    /**
     * The SyncThing folder that this file represents.
     */
    protected final StFolder localStFolder;

    /**
     * The relative path of this file. The root file (well, directory)
     * in a syncthing folder will have this path be an empty string ("").
     * <br/>
     * Note: a `.stversion` will never appear in this relative path as those
     * types of locations are handled seperatly.
     */
    protected final Path relativePath;

    /**
     * The name of the file or directory.
     */
    public final String fileName;


    protected StFile(StFolder localStFolder, Path relativePath) {
        this.localStFolder = localStFolder;
        this.relativePath = relativePath;
        this.fileName = relativePath.getFileName().toString();
    }

    static final String STV = ".stversions";
    static final String STF = ".stfolder";

    /**
     * Creates a new root directory from a {@link StFolder}.
     * This method is the only way for a non-{@link StFile} class to make an instance of a {@link StFile} class.
     *
     * @param localStFolder The base Syncthing folder
     * @return A new {@link StDirectory} that represents the root directory of the Syncthing folder.
     */
    public static StDirectory newDirFromStFolder(StFolder localStFolder) {
        LocationLister lister = new LocationLister(
            Paths.get(localStFolder.path()),
            Paths.get(localStFolder.path(), STV)
        );
        return new StDirectory(localStFolder, lister, Paths.get(""), Location.LocalReal);
    }

    /**
     * Gets the primary location of this {@code StFile}. The primary location is the location in the {@code StFile}
     * with the lowest ordinal. (This function requires the {@link Location}s to be written in the proper order to work)
     *
     * @return The primary {@link Location} of this {@code StFile}
     */
    public abstract Location getPrimaryLocation();

    @Override
    public int compareTo(StFile o) {
        return this.relativePath.compareTo(o.relativePath);
    }

    /**
     * All the different possible locations that a {@link StFile} can come from.
     * <br/> Note: The order of the declarations <b>does</b> matter.
     * <br/> Note: This may be expanded upon in the future to allow multiple remotes.
     */
    public enum Location {
        /**
         * Describes a file that comes from the <b>local</b> Syncthing folder.
         */
        LocalReal,
        /**
         * Describes a file that comes from a <b>remote</b> syncthing folder.
         */
        RemoteReal,
        /**
         * Describes a file that comes from the <b>local .stversions</b> folder.
         */
        LocalVersions,
        /**
         * Describes a file that comes from a <b>remote .stversions</b>  folder.
         */
        RemoteVersions
    }

}
