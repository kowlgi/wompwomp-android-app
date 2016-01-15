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

package co.wompwomp.sunshine;

import android.util.JsonReader;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

public class FeedParser {
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
        ArrayList<Entry> entries = new ArrayList<Entry>();

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
        String author = null;

        reader.beginObject();
        while (reader.hasNext()) {
            String name = reader.nextName();
            switch (name) {
                case WompWompConstants.WOMPWOMP_ID:
                    id = reader.nextString();
                    break;
                case WompWompConstants.WOMPWOMP_TEXT:
                    quoteText = reader.nextString();
                    break;
                case WompWompConstants.WOMPWOMP_IMAGEURI:
                    imageSourceUri = reader.nextString();
                    break;
                case WompWompConstants.WOMPWOMP_NUMFAVORITES:
                    numFavorites = reader.nextInt();
                    break;
                case WompWompConstants.WOMPWOMP_NUMSHARES:
                    numShares = reader.nextInt();
                    break;
                case WompWompConstants.WOMPWOMP_CREATEDON:
                    createdOn = reader.nextString();
                    break;
                case WompWompConstants.WOMPWOMP_AUTHOR:
                    author = reader.nextString();
                    break;
                default:
                    reader.skipValue();
                    break;
            }
        }
        reader.endObject();
        return new Entry(id, imageSourceUri, quoteText, numFavorites, numShares, createdOn, author);
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
        public final String author;

        Entry(String id, String imageSourceUri, String quoteText, Integer numFavorites, Integer numShares, String createdOn, String author) {
            this.id = id;
            this.imageSourceUri = imageSourceUri;
            this.quoteText = quoteText;
            this.numFavorites = numFavorites;
            this.numShares = numShares;
            this.createdOn = createdOn;
            this.author = author;
        }
    }
}
