package biz.donvi.syncthingversionpicker.files;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ParsedFileNameTest {

    @Test
    public void testRegularName() {
        String name = "importantDocument.md";
        var p = new ParsedFileName(name);
        Assertions.assertTrue(p.hasName());
        Assertions.assertFalse(p.hasPrevMarker());
        Assertions.assertFalse(p.hasConflict());
        Assertions.assertFalse(p.hasSyncDate());
        Assertions.assertTrue(p.hasExtension());
    }

    @Test
    public void testRegularNameWithoutExtension() {
        String name = "importantDocument";
        var p = new ParsedFileName(name);
        Assertions.assertTrue(p.hasName());
        Assertions.assertFalse(p.hasPrevMarker());
        Assertions.assertFalse(p.hasConflict());
        Assertions.assertFalse(p.hasSyncDate());
        Assertions.assertFalse(p.hasExtension());
    }

    @Test
    public void testRegularVersion() {
        String name = "Tests.test.doc~20231113-151554.md";
        var p = new ParsedFileName(name);
        Assertions.assertTrue(p.hasName());
        Assertions.assertFalse(p.hasPrevMarker());
        Assertions.assertFalse(p.hasConflict());
        Assertions.assertTrue(p.hasSyncDate());
        Assertions.assertTrue(p.hasExtension());
    }
    @Test
    public void testRegularVersionWithoutExtension() {
        String name = "importantDocument~20230829-151733";
        var p = new ParsedFileName(name);
        Assertions.assertTrue(p.hasName());
        Assertions.assertFalse(p.hasPrevMarker());
        Assertions.assertFalse(p.hasConflict());
        Assertions.assertTrue(p.hasSyncDate());
        Assertions.assertFalse(p.hasExtension());
    }

    @Test
    public void testConflictVersion() {
        String name = "STVP Ideas.sync-conflict-20240120-075404-XZUUN4E~20240122-180332.md";
        var p = new ParsedFileName(name);
        Assertions.assertTrue(p.hasName());
        Assertions.assertFalse(p.hasPrevMarker());
        Assertions.assertTrue(p.hasConflict());
        Assertions.assertTrue(p.hasSyncDate());
        Assertions.assertTrue(p.hasExtension());
    }

    @Test
    public void tertConflictVersionWithoutExtension() {
        String name = "STVP Ideas.sync-conflict-20240120-075404-XZUUN4E~20240122-180332";
        var p = new ParsedFileName(name);
        Assertions.assertTrue(p.hasName());
        Assertions.assertFalse(p.hasPrevMarker());
        Assertions.assertTrue(p.hasConflict());
        Assertions.assertTrue(p.hasSyncDate());
        Assertions.assertFalse(p.hasExtension());
    }
}
