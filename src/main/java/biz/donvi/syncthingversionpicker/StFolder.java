package biz.donvi.syncthingversionpicker;

public record StFolder(String id, String label, String path, String versionsPath) {
    @Override
    public String toString() {
        return label;
    }
}
