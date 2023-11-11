package biz.donvi.syncthingversionpicker;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class SyncFile implements Comparable<SyncFile> {

    private static final String STV = ".stversions";// TODO: Make sure this is spelled right

    private final StFolder   folder;
    private final Path       rootPath;
    private final Path       prevRootPath;
    private final Path       relPath;
    private final File       realFile;
    private final List<File> previousVersions;

    public SyncFile(StFolder folder, Path relPath) {
        this.folder = folder;
        this.rootPath = Path.of(folder.path());
        this.prevRootPath = Path.of(folder.path(), STV);
        this.relPath = relPath;
        this.realFile = rootPath.resolve(relPath).toFile();
        this.previousVersions = new ArrayList<>();
        var f = prevRootPath.resolve(relPath).toFile();
        if (f.exists()) previousVersions.add(f);
    }

    public SyncFile(StFolder folder, File realFile) {
        this(folder, getSubPath(folder, realFile, false));
    }

    private static Path getSubPath(StFolder folder, File realFile, boolean includeStVersion) {
        var fop = Path.of(folder.path());
        var fip = realFile.toPath();
        return fip.subpath(fop.getNameCount() + (includeStVersion ? 1 : 0), fip.getNameCount());
    }

    public File getRealFile() {
        return realFile;
    }

    public Path getRelPath() {
        return relPath;
    }

    private void addPreviousVersion(File prev) {
        previousVersions.add(prev);
    }

    public List<File> getPreviousVersions() {
        return previousVersions;
    }

    @Override
    public int compareTo(SyncFile sf) {
        return this.realFile.compareTo(sf.realFile);
    }

    public void scanPreviousVersions() {

    }

    public List<SyncFile> listFiles() {
        // If we are a directory, just return a blank list
        if (!isDirectory()) return new ArrayList<>();
        // Get the children of the real file.
        File[] realChildren = null;
        if (realFile.exists())
            realChildren = realFile.listFiles();
        if (realChildren == null)
            realChildren = new File[0];
        // If this is a dir, there should only be one in the prev list.
        // Get the children of that too.
        File[] previousChildren = null;
        if (!previousVersions.isEmpty())
            previousChildren = previousVersions.get(0).listFiles();
        if (previousChildren == null)
            previousChildren = new File[0];
        ArrayList<File> unusedChildren = new ArrayList<>(Arrays.asList(previousChildren));
        // Now to turn them into `SyncFile`s
        // This is going to be painful.
        List<SyncFile> children = new ArrayList<>();
        for (File realFile : realChildren) {
            var syncFile = new SyncFile(folder, realFile);
            var relPath = syncFile.getRelPath();
            ArrayList<File> usedChildren = new ArrayList<>();
            for (File pf : unusedChildren) {
                var relPathStr = relPath.toString();
                relPathStr = relPathStr.substring(0, relPathStr.lastIndexOf('.'));
                if (getSubPath(folder, pf, true).toString().startsWith(relPathStr)) {
                    syncFile.addPreviousVersion(pf);
                    usedChildren.add(pf);
                }
            }
            unusedChildren.removeAll(usedChildren);
            children.add(syncFile);
        }
        if (!unusedChildren.isEmpty()) {
            System.out.println(unusedChildren);
        }


        return children;
    }

    public boolean isDirectory() {
        if (realFile.exists())
            return realFile.isDirectory();
        else if (!previousVersions.isEmpty())
            return previousVersions.get(0).isDirectory();
        else return false;
    }
}
