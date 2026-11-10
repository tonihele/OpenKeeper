/* Copyright (C) 2003-2014 Michael Scheerer. All Rights Reserved. */

/*
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU General Public
 *  License as published by the Free Software Foundation; either
 *  version 2 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public
 *  License along with this library; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 */
package toniarts.openkeeper.audio.plugins.decoder.tag.id3v1;

import java.io.*;
import toniarts.openkeeper.audio.plugins.decoder.tag.Tag;
import toniarts.openkeeper.audio.plugins.decoder.tag.TagException;

/**
 * The
 * <code>ID3v1</code> class is the main class of this IDv1 system.
 * <p>
 * Note that this ID3v1 system requires an ID3v1 tag starting at the first byte
 * of the inputstream, although the ID3v1 specification doesn't demand this.
 *
 * @author Michael Scheerer
 */
public final class ID3v1 extends Tag {

    private final static int TITLE_LENGTH = 30;
    private final static int ARTIST_LENGTH = 30;
    private final static int ALBUM_LENGTH = 30;
    private final static int YEAR_LENGTH = 4;
    private final static int COMMENT_LENGTH = 30;
    private final static int GENRE_LENGTH = 1;
    public static final String[] GENRE = {
        "Blues", "Classic Rock", "Country", "Dance", "Disco", "Funk", "Grunge", "Hip-Hop", "Jazz", "Metal", "New Age", "Oldies", "Other", "Pop", "Rhythm and Blues", "Rap", "Reggae", "Rock", "Techno", "Industrial", "Alternative", "Ska", "Death Metal", "Pranks", "Soundtrack", "Euro-Techno", "Ambient", "Trip-Hop", "Vocal", "Jazz & Funk", "Fusion", "Trance", "Classical", "Instrumental", "Acid", "House", "Game", "Sound Clip", "Gospel", "Noise", "Alternative Rock", "Bass", "Soul", "Punk", "Space", "Meditative", "Instrumental Pop", "Instrumental Rock", "Ethnic", "Gothic", "Darkwave", "Techno-Industrial", "Electronic", "Pop-Folk", "Eurodance", "Dream", "Southern Rock", "Comedy", "Cult", "Gangsta", "Top 40", "Christian Rap", "Pop/Funk", "Jungle", "Native US", "Cabaret", "New Wave", "Psychedelic", "Rave", "Showtunes", "Trailer", "Lo-Fi", "Tribal", "Acid Punk", "Acid Jazz", "Polka", "Retro", "Musical", "Rock & Roll", "Hard Rock", "Folk", "Folk-Rock", "National Folk", "Swing", "Fast Fusion",
        "Bebop", "Latin", "Revival", "Celtic", "Bluegrass", "Avantgarde", "Gothic Rock", "Progressive Rock", "Psychedelic Rock", "Symphonic Rock", "Slow Rock", "Big Band", "Chorus", "Easy Listening", "Acoustic", "Humour", "Speech", "Chanson", "Opera", "Chamber Music", "Sonata", "Symphony", "Booty Bass", "Primus", "Porn Groove", "Satire", "Slow Jam", "Club", "Tango", "Samba", "Folklore", "Ballad", "Power Ballad", "Rhythmic Soul", "Freestyle", "Duet", "Punk Rock", "Drum Solo", "A capella", "Euro-House", "Dance Hall", "Goa", "Drum & Bass", "Club-House", "Hardcore Techno", "Terror", "Indie", "BritPop", "Negerpunk", "Polsk Punk", "Beat", "Christian Gangsta Rap", "Heavy Metal", "Black Metal", "Crossover", "Contemporary Christian", "Christian Rock", "Merengue", "Salsa", "Thrash Metal", "Anime", "Jpop", "Synthpop", "Abstract", "Art Rock", "Baroque", "Bhangra", "Big Beat", "Breakbeat", "Chillout", "Downtempo", "Dub", "EBM", "Eclectic", "Electro", "Electroclash", "Emo", "Experimental", "Garage",
        "Global", "IDM", "Illbient", "Industro-Goth", "Jam Band", "Krautrock", "Leftfield", "Lounge", "Math Rock", "New Romantic", "Nu-Breakz", "Post-Punk", "Post-Rock", "Psytrance", "Shoegaze", "Space Rock", "Trop Rock", "World Music", "Neoclassical", "Audiobook", "Audio Theatre", "Neue Deutsche Welle", "Podcast", "Indie Rock", "G-Funk", "Dubstep", "Garage Rock", "Psybient"
    };
    private String title;
    private String artist;
    private String album;
    private String year;
    private String comment;
    private byte track;
    private byte genre;
    private int version;
    private byte[] b = new byte[30];

    /**
     * Provides access to ID3v1 tag.
     *
     * @param in Input stream to read from. Stream position must be set to
     * beginning of file (i.e. position of ID3v1 tag)
     * @exception IOException if an I/O errors occur
     */
    public ID3v1(InputStream stream) throws IOException {
        super();

        // open file and read tag (if present)
        try {
            readAll(stream);
        } catch (TagException e) {
            // no tag or tag error
            close();
            throw new IOException(e.getMessage());
        }
        put(S_TAG_FORMAT, ("ID3v1." + version));
        buildKeyMapTable();
    }

    private void buildKeyMapTable() {
        if (!title.trim().isEmpty()) {
            put(S_TITLE, title);
        }
        if (!artist.trim().isEmpty()) {
            put(S_ARTIST, artist);
        }
        if (!album.trim().isEmpty()) {
            put(S_ALBUM, album);
        }
        if (!year.trim().isEmpty()) {
            put(S_YEAR, year);
        }
        if (!comment.trim().isEmpty()) {
            put(S_COMMENT, comment);
        }
        if (genre >= 0 && genre < GENRE.length) {
            put(S_GENRE, GENRE[genre]);
        }
        if (version == 1) {
            byte[] t = new byte[1];

            t[0] = (byte) track;
            String buffer = new String(t);

            if (!buffer.trim().isEmpty()) {
                put(S_TRACK, buffer);
            }
        }
    }

    /* If necessary excludes the tag data and/or skipped data length from the byte position
     * or calculates a variable bitrate correction to determine the
     * actual playtime or seek position of the media. Normally the playtime position
     * calculation is different from the one of the seek position.
     * Inside the Ogg format the method don nothing.
     *
     * @param seeking          seeking flag to distinguish the different calculation of the playtime and seek position
     * @param position         the current byte position
     * @return                 the corrected byte position
     */
    @Override
    public long correctedBytePosition(long position, boolean seeking) {
        return position;
    }

    /**
     * Frees all system resources, which are bounded to this object.
     */
    @Override
    public void close() {
        b = null;
        super.close();
    }

    private void readAll(InputStream stream) throws TagException, IOException {
        stream.read(b, 0, 3);
        if (((char) b[0] == 'T' && (char) b[1] == 'A' && (char) b[2] == 'G') || ((char) b[0] == 't' && (char) b[1] == 'a' && (char) b[2] == 'g')) {
            stream.read(b, 0, 30);
            title = new String(b, 0, getStringLength());
            stream.read(b, 0, 30);
            artist = new String(b, 0, getStringLength());
            stream.read(b, 0, 30);
            album = new String(b, 0, getStringLength());
            stream.read(b, 0, 4);
            year = new String(b, 0, getStringLength());
            stream.read(b, 0, 30);
            if (b[27] == 0 && b[28] > 0) {
                track = b[28];
                version = 1;
            }
            comment = new String(b, 0, getStringLength());
            stream.read(b, 0, 1);
            genre = b[0];
        } else {
            throw new TagException("No ID3v1 header");
        }
    }

    private int getStringLength() {
        int i = 0;

        for (; i < b.length && b[i] != 0; i++) {
        }
        return i;
    }
}
