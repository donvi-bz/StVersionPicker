package biz.donvi.syncthingversionpicker;

import biz.donvi.syncthingversionpicker.files.StFile;

public record StFolder(String id, String label, String path) {
    @Override
    public String toString() {
        return label;
    }

    public String versionsPath() {
        return path + "\\" + StFile.STV; // FIXME: That's... got to change.
    }
}
