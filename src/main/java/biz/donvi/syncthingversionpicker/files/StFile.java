package biz.donvi.syncthingversionpicker.files;

import biz.donvi.syncthingversionpicker.StFolder;
import biz.donvi.syncthingversionpicker.controllers.PickerController;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.function.BiFunction;

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

    /**
     * The extension of the file WITH the dot
     */
    public final String fileExtension;

    public final StDirectory parentDir;


    protected StFile(StFolder localStFolder, Path relativePath, StDirectory parentDir) {
        this.localStFolder = localStFolder;
        this.relativePath = relativePath;
        this.fileName = relativePath.getFileName().toString();
        this.parentDir = parentDir;
        this.fileExtension = fileName.contains(".")
            ? fileName.substring(fileName.lastIndexOf(".")) : "";
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

    /* **************************************************************
     MARK: - Static Stuff
     ************************************************************** */

    public static final String STV = ".stversions";
    public static final String STF = ".stfolder";

    /**
     * Helper method to exclude Syncthing files.
     *
     * @param name The name of the file to check.
     * @return {@code true} if the file should be kept, {@code false} if the file is one of the Syncthing files.
     */
    public static boolean notStPlaceholder(String name) {
        return !(name.equals(STV) || name.equals(STF));
    }

    /**
     * <b>FIXME: OUTDATED</b><br/>
     * Creates a new root directory from a {@link StFolder}.
     * This method is the only way for a non-{@link StFile} class to make an instance of a {@link StFile} class.
     *
     * @return A new {@link StDirectory} that represents the root directory of the Syncthing folder.
     */
    public static StDirectory newDirFromStFolder(
        PickerController.DoubleStFolder folder,
        BiFunction<Path, Path, DirectoryLister> remoteListerProvider
    ) {
        var localRealPath = Optional.ofNullable(folder.local()).map(StFolder::path).map(Path::of);
        var localVersPath = Optional.ofNullable(folder.local()).map(StFolder::versionsPath).map(Path::of);
        var remoteRealPath = Optional.ofNullable(folder.remote()).map(StFolder::path).map(Path::of);
        var remoteVersPath = Optional.ofNullable(folder.remote()).map(StFolder::versionsPath).map(Path::of);

        if (localRealPath.isEmpty() || localVersPath.isEmpty())
            throw new RuntimeException("Must have a valid local path at least!");

        LocalDirectoryLister localLister = new LocalDirectoryLister(localRealPath.get(), localVersPath.get());
        DirectoryLister remoteLister = (remoteRealPath.isPresent() && remoteVersPath.isPresent())
            ? remoteListerProvider.apply(remoteRealPath.get(), remoteVersPath.get())
            : DirectoryLister.emptyLister;

        FullStLister lister = new FullStLister(localLister, remoteLister);

        return new StDirectory(folder.local(), lister, Paths.get(""), Location.LocalCurrent, null);
    }

}
