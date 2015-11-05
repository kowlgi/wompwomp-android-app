/*
 * Copyright 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.agni.sunshine;

import android.util.JsonReader;

import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

/**
 * This class parses generic Atom feeds.
 *
 * <p>Given an InputStream representation of a feed, it returns a List of entries,
 * where each list element represents a single entry (post) in the XML feed.
 *
 * <p>An example of an Atom feed can be found at:
 * http://en.wikipedia.org/w/index.php?title=Atom_(standard)&oldid=560239173#Example_of_an_Atom_1.0_feed
 */
public class FeedParser {

    // Constants indicting XML element names that we're interested in
    private final String AGNI_ID = "id";
    private final String AGNI_TEXT = "text";
    private final String AGNI_IMAGEURI = "imageuri";
    private final String AGNI_CREATEDON = "created_on";
    private final String AGNI_NUMFAVORITES = "numfavorites";
    private final String AGNI_NUMSHARES = "numshares";

    // We don't use XML namespaces
    private static final String ns = null;

    /** Parse an Atom feed, returning a collection of Entry objects.
     *
     * @param in Atom feed, as a stream.
     * @return List of {@link FeedParser.Entry} objects.
     * @throws XmlPullParserException on error parsing feed.
     * @throws IOException on I/O error.
     */
    public List<Entry> parse(InputStream in)
            throws IOException, ParseException {
        JsonReader reader =  new JsonReader(new InputStreamReader(in, "UTF-8"));
        try {
            return readEntriesArray(reader);
        } finally {
            reader.close();
            in.close();
        }
    }

    public List<Entry> readEntriesArray(JsonReader reader) throws IOException {
        List entries = new ArrayList<Entry>();

        reader.beginArray();
        while (reader.hasNext()) {
            entries.add(readEntry(reader));
        }
        reader.endArray();
        return entries;
    }

    public Entry readEntry(JsonReader reader) throws IOException {
        String id = null;
        String imageSourceUri = null;
        String quoteText = null;
        Integer numFavorites = 0;
        Integer numShares = null;
        String createdOn = null;

        reader.beginObject();
        while (reader.hasNext()) {
            String name = reader.nextName();
            if (name.equals(AGNI_ID)) {
                id = reader.nextString();
            } else if (name.equals(AGNI_TEXT)) {
                quoteText = reader.nextString();
            } else if (name.equals(AGNI_IMAGEURI)) {
                imageSourceUri = reader.nextString();
            } else if (name.equals(AGNI_NUMFAVORITES)) {
                numFavorites = reader.nextInt();
            } else if (name.equals(AGNI_NUMSHARES)) {
                numShares = reader.nextInt();
            } else if (name.equals(AGNI_CREATEDON)) {
                createdOn = reader.nextString();
            } else {
                reader.skipValue();
            }
        }
        reader.endObject();
        return new Entry(id, imageSourceUri, quoteText, numFavorites, numShares, createdOn);
    }

    /**
     * This class represents a single entry (post) in the JSON feed.
     */
    public static class Entry {
        public final String id;
        public final String imageSourceUri;
        public final String quoteText;
        public final Integer numFavorites;
        public final Integer numShares;
        public final String createdOn;

        Entry(String id, String imageSourceUri, String quoteText, Integer numFavorites, Integer numShares, String createdOn) {
            this.id = id;
            this.imageSourceUri = imageSourceUri;
            this.quoteText = quoteText;
            this.numFavorites = numFavorites;
            this.numShares = numShares;
            this.createdOn = createdOn;
        }
    }
}
