package co.wompwomp.sunshine;

import android.database.Cursor;

import java.util.Locale;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

public class MyListItem {
    public Integer _id; // database id
    public String id; //  globally unique item id
    public String imageSourceUri;
    public String quoteText;
    public Boolean favorite;
    public Integer numFavorites;
    public Integer numShares;
    public String createdOn;
    public Integer cardType;
    public String author;
    public String videoUri;
    public Integer numPlays;
    public Integer fileSize;
    public String annotation;
    public String list_type;
    public Integer featured_priority;

    public static MyListItem fromCursor(Cursor c) {
        MyListItem myItem = new MyListItem();
        myItem._id = c.getInt(WompWompConstants.COLUMN_ID);
        myItem.id = c.getString(WompWompConstants.COLUMN_ENTRY_ID);
        myItem.imageSourceUri = c.getString(WompWompConstants.COLUMN_IMAGE_SOURCE_URI);
        myItem.quoteText = c.getString(WompWompConstants.COLUMN_QUOTE_TEXT);
        myItem.favorite = c.getInt(WompWompConstants.COLUMN_FAVORITE) > 0;
        myItem.numFavorites = c.getInt(WompWompConstants.COLUMN_NUM_FAVORITES);
        myItem.numShares = c.getInt(WompWompConstants.COLUMN_NUM_SHARES);
        myItem.createdOn = c.getString(WompWompConstants.COLUMN_CREATED_ON);
        myItem.cardType = c.getInt(WompWompConstants.COLUMN_CARD_TYPE);
        myItem.author = c.getString(WompWompConstants.COLUMN_AUTHOR);
        myItem.videoUri = c.getString(WompWompConstants.COLUMN_VIDEOURI);
        myItem.numPlays = c.getInt(WompWompConstants.COLUMN_NUM_PLAYS);
        myItem.fileSize = c.getInt(WompWompConstants.COLUMN_FILE_SIZE);
        myItem.annotation = c.getString(WompWompConstants.COLUMN_ANNOTATION);
        myItem.list_type = c.getString(WompWompConstants.COLUMN_LIST_TYPE);
        myItem.featured_priority = c.getInt(WompWompConstants.COLUMN_FEATURED_PRIORITY);
        return myItem;
    }


    // http://stackoverflow.com/questions/4753251/how-to-go-about-formatting-1200-to-1-2k-in-java
    private static final NavigableMap<Long, String> suffixes = new TreeMap<>();
    static {
        suffixes.put(1_000L, "k");
        suffixes.put(1_000_000L, "M");
        suffixes.put(1_000_000_000L, "G");
        suffixes.put(1_000_000_000_000L, "T");
        suffixes.put(1_000_000_000_000_000L, "P");
        suffixes.put(1_000_000_000_000_000_000L, "E");
    }

    public static String format(long value) {
        //Long.MIN_VALUE == -Long.MIN_VALUE so we need an adjustment here
        if (value == Long.MIN_VALUE) return format(Long.MIN_VALUE + 1);
        if (value < 1000) return String.format(Locale.US, "%d", value); //deal with easy case

        Map.Entry<Long, String> e = suffixes.floorEntry(value);
        Long divideBy = e.getKey();
        String suffix = e.getValue();

        long truncated = value / (divideBy / 10); //the number part of the output times 10
        boolean hasDecimal = truncated < 100 && (truncated / 10d) != (truncated / 10);
        return hasDecimal ? (truncated / 10d) + suffix : (truncated / 10) + suffix;
    }

    @Override
    public String toString() {
        return "_id: " + _id +
                ", entryId: " + id +
                ", imageuri: " + imageSourceUri +
                ", quoteText: " + quoteText +
                ", favorite: " + favorite +
                ", numfavorite:" + numFavorites +
                ", numShares: " + numShares +
                ", created_on: " + createdOn +
                ", cardType: " + cardType +
                ", author: " + author +
                ", videouri: " + videoUri +
                ", numPlays: " + numPlays +
                ", fileSize: " + fileSize +
                ", annotation: " + annotation +
                ", list_type: " + list_type +
                ", featured_priority: " + featured_priority;
    }
}
