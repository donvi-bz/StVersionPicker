package biz.donvi.syncthingversionpicker;

public record StFolder(String id, String label, String path) {
    @Override
    public String toString() {
        return label;
    }
}
