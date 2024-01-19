package biz.donvi.syncthingversionpicker.files;

import static biz.donvi.syncthingversionpicker.files.Location.When.Current;
import static biz.donvi.syncthingversionpicker.files.Location.When.Version;
import static biz.donvi.syncthingversionpicker.files.Location.Where.Local;
import static biz.donvi.syncthingversionpicker.files.Location.Where.Remote;

/**
 * All the different possible locations that a {@link StFile} can come from.
 * <br/> Note: The order of the declarations <b>does</b> matter.
 * <br/> Note: This may be expanded upon in the future to allow multiple remotes.
 */
public enum Location {
    /**
     * Describes a file that comes from the <b>local</b> Syncthing folder.
     */
    LocalCurrent(Local, Current),
    /**
     * Describes a file that comes from a <b>remote</b> syncthing folder.
     */
    RemoteCurrent(Remote, Current),
    /**
     * Describes a file that comes from the <b>local .stversions</b> folder.
     */
    LocalVersions(Local, Version),
    /**
     * Describes a file that comes from a <b>remote .stversions</b>  folder.
     */
    RemoteVersions(Remote, Version);

    /**
     * Where the file is located, as in, is it on the <b>{@code Local}</b> or <b>{@code Remote}</b> machine.
     */
    public enum Where {
        Local,
        Remote;

        public <T> T which(T ifLocal, T ifRemote) {
            return switch (this) {
                case Local -> ifLocal;
                case Remote -> ifRemote;
            };
        }
    }

    /**
     * Which type of file is this, it is either a <b>{@code Current}</b> file, or a <b>{@code Version}s</b> file.
     */
    public enum When {
        Current,
        Version;

        public <T> T which(T ifCurrent, T ifVersion) {
            return switch (this) {
                case Current -> ifCurrent;
                case Version -> ifVersion;
            };
        }
    }

    public final Where where;
    public final When  when;

    Location(Where where, When when) {
        this.where = where;
        this.when = when;
    }
}
