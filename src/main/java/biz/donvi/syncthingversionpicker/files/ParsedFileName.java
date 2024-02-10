package biz.donvi.syncthingversionpicker.files;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static biz.donvi.syncthingversionpicker.files.ParsedFileName.Type.*;

public record ParsedFileName(
    String originalName,
    String name,
    String prevMarker,
    String conflictDate, String conflictDevice,
    String syncDate,
    String extension
) {

    private static final Logger logger = LogManager.getLogger(ParsedFileName.class);

    public ParsedFileName(String fileName) {
        this(fileName, matchedMatcher(fileName));
    }

    // Gotta love weird indirection to satisfy the "this must be first" restriction.
    private ParsedFileName(String fileName, Matcher m) {
        this(fileName,
             m.group(Name.names[0]),
             m.group(PrevMarker.names[0]),
             m.group(Conflict.names[0]), m.group(Conflict.names[1]),
             m.group(Date.names[0]),
             m.group(Extension.names[0])
        );
        if (!m.matches()) {
            logger.warn(" - Parsed Value:  `{}`", this);
        }
    }

    private static Matcher matchedMatcher(String fileName) {
        Matcher m = Type.pHasMiddleVal.matcher(fileName);
        if (!m.matches()) {
            logger.warn("No match for file. more information... ");
            logger.warn(" - OriginalName:  `{}`", fileName);
            logger.warn(" - Parser String: `{}`", rHasMiddleVal);
        }
        return m;
    }

    public String nameFixed() {
        return (hasName() ? name : "") +
               (hasExtension() ? extension : "");
    }

    //@formatter:off
    public boolean hasOriginalName()   { return   originalName != null && !originalName.isEmpty();   }
    public boolean hasName()           { return           name != null && !name.isEmpty();           }
    public boolean hasPrevMarker()     { return     prevMarker != null && !prevMarker.isEmpty();     }
    public boolean hasConflictDate()   { return   conflictDate != null && !conflictDate.isEmpty();   }
    public boolean hasConflictDevice() { return conflictDevice != null && !conflictDevice.isEmpty(); }
    public boolean hasConflict()       { return      hasConflictDate() && hasConflictDevice();       }
    public boolean hasSyncDate()       { return       syncDate != null && !syncDate.isEmpty();       }
    public boolean hasExtension()      { return      extension != null && !extension.isEmpty();      }

    public String getBeginning() { return hasName()      ? name      : ""; }
    public String getEnd()       { return hasExtension() ? extension : ""; }
    public String getMiddle()    {
        return (hasPrevMarker() ? PrevMarker.rebuilder.formatted(prevMarker)                   : "") +
               (hasConflict()   ? Conflict  .rebuilder.formatted(conflictDate, conflictDevice) : "") +
               (hasSyncDate()   ? Date      .rebuilder.formatted(syncDate)                     : "");
    }
    public String rebuild()      { return getBeginning() + getMiddle() + getEnd(); }
    //@formatter:on


    enum Type {
        /*language=RegExp*/
        Name(
            "^(?<name>.*?)",
            "%s",
            "name"),
        /*language=RegExp*/
        PrevMarker(
            "~VP-(?<stMarker>PREV)",
            "~VP-%S",
            "stMarker"),
        /*language=RegExp*/
        Conflict(
            "\\.sync-conflict-(?<conflictDate>\\d{8}-\\d{6})-(?<conflictDevice>[A-Z0-9]{7})",
            ".sync-conflict-%s-%s",
            "conflictDate", "conflictDevice"),
        /*language=RegExp*/
        Date(
            "~(?<syncDate>\\d{8}-\\d{6})",
            "~%s",
            "syncDate"),
        /*language=RegExp*/
        Extension(
            "(?<ext>\\.[^.]*)?$",
            "%s",
            "ext");


        final String   regex;
        final String   rebuilder;
        final String[] names;
        final Pattern  pattern;

        Type(String regex, String rebuilder, String... names) {
            this.regex = regex;
            this.rebuilder = rebuilder;
            this.names = names;
            this.pattern = Pattern.compile(regex);
        }

        /**
         * Regex to check whether one of the middle components exists within
         */
        public static final String  rHasMiddleVal;
        public static final Pattern pHasMiddleVal;

        static {
            Type[] values = Type.values();
            List<Type> innerValues = Arrays.asList(values).subList(1, values.length - 1);
            String innerValuesJoined = innerValues.stream().map(t -> t.regex).collect(Collectors.joining("|"));
            //language=RegExp
            rHasMiddleVal = "%s(?<inner>(%s)*)%s".formatted(values[0].regex, innerValuesJoined,
                                                            values[values.length - 1].regex);
            pHasMiddleVal = Pattern.compile(rHasMiddleVal);
            logger.debug("Initialized regex with value `{}`", rHasMiddleVal);
        }
    }
}
