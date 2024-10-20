/*
 * Copyright 2024 SpCo
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package top.spco.spcobot.wiki.util.nbt;

import top.spco.spcobot.wiki.util.nbt.tag.builtin.*;
import top.spco.spcobot.wiki.util.nbt.tag.builtin.custom.DoubleArrayTag;
import top.spco.spcobot.wiki.util.nbt.tag.builtin.custom.FloatArrayTag;
import top.spco.spcobot.wiki.util.nbt.tag.builtin.custom.ShortArrayTag;

import java.io.*;
import java.util.regex.Pattern;

/**
 * A class containing methods for reading/writing stringified NBT tags.
 */
public class SNBTIO {
    /**
     * Reads stringified root CompoundTag from the given file.
     *
     * @param path Path of the file.
     * @return The read compound tag.
     * @throws java.io.IOException If an I/O error occurs.
     */
    public static CompoundTag readFile(String path) throws IOException {
        return readFile(new File(path));
    }

    /**
     * Reads the stringified CompoundTag from the given file.
     *
     * @param file File to read from.
     * @return The read compound tag.
     * @throws java.io.IOException If an I/O error occurs.
     */
    public static CompoundTag readFile(File file) throws IOException {
        try (InputStream in = new BufferedInputStream(new FileInputStream(file))) {
            Tag tag = readTag(in);
            if (!(tag instanceof CompoundTag)) {
                throw new IOException("Root tag is not a CompoundTag!");
            }

            return (CompoundTag) tag;
        }
    }

    /**
     * Writes the given root CompoundTag to the given file as stringified NBT.
     *
     * @param tag  Tag to write.
     * @param path Path to write to.
     * @throws java.io.IOException If an I/O error occurs.
     */
    public static void writeFile(CompoundTag tag, String path) throws IOException {
        writeFile(tag, new File(path));
    }

    /**
     * Writes the given root CompoundTag to the given file as stringified NBT.
     *
     * @param tag  Tag to write.
     * @param file File to write to.
     * @throws java.io.IOException If an I/O error occurs.
     */
    public static void writeFile(CompoundTag tag, File file) throws IOException {
        writeFile(tag, file, false);
    }

    /**
     * Writes the given root CompoundTag to the given file as stringified NBT.
     *
     * @param tag       Tag to write.
     * @param path      Path to write to.
     * @param linebreak Whether the SNBT file should be formated with line breaks or as a single line.
     * @throws java.io.IOException If an I/O error occurs.
     */
    public static void writeFile(CompoundTag tag, String path, boolean linebreak) throws IOException {
        writeFile(tag, new File(path), linebreak);
    }

    /**
     * Writes the given root CompoundTag to the given file as stringified NBT.
     *
     * @param tag       Tag to write.
     * @param file      File to write to.
     * @param linebreak Whether the SNBT file should be formated with line breaks or as a single line.
     * @throws java.io.IOException If an I/O error occurs.
     */
    public static void writeFile(CompoundTag tag, File file, boolean linebreak) throws IOException {
        if (!file.exists()) {
            if (file.getParentFile() != null && !file.getParentFile().exists()) {
                file.getParentFile().mkdirs();
            }

            file.createNewFile();
        }

        try (OutputStream out = new FileOutputStream(file)) {
            writeTag(out, tag, linebreak);
        }
    }

    /**
     * Reads a stringified NBT tag.
     *
     * @param in Input stream to read from.
     * @return The read tag, or null if the tag is an end tag.
     * @throws java.io.IOException If an I/O error occurs.
     */
    public static Tag readTag(InputStream in) throws IOException {
        try (StringifiedNBTReader reader = new StringifiedNBTReader(in)) {
            return reader.readNextTag("");
        }
    }

    /**
     * Writes a stringified NBT tag.
     *
     * @param out Output stream to write to.
     * @param tag Tag to write.
     * @throws java.io.IOException If an I/O error occurs.
     */
    public static void writeTag(OutputStream out, Tag tag) throws IOException {
        writeTag(out, tag, false);
    }

    /**
     * Writes a stringified NBT tag.
     *
     * @param out       Output stream to write to.
     * @param tag       Tag to write.
     * @param linebreak Whether the SNBT should be formated with line breaks or as a single line.
     * @throws java.io.IOException If an I/O error occurs.
     */
    public static void writeTag(OutputStream out, Tag tag, boolean linebreak) throws IOException {
        try (StringifiedNBTWriter writer = new StringifiedNBTWriter(out)) {
            writer.writeTag(tag, linebreak);
        }
    }

    public static class StringifiedNBTReader extends PushbackReader {
        public StringifiedNBTReader(InputStream in) {
            super(new InputStreamReader(in), 32);
        }

        public Tag readNextTag(String name) throws IOException {
            skipWhitespace();
            if (lookAhead(0) == '{') {
                return readCompoundTag(name);
            } else if (lookAhead(0) == '[') {
                return readListOrArrayTag(name);
            } else {
                return readPrimitiveTag(name);
            }
        }

        public Tag readCompoundTag(String name) throws IOException {
            return parseTag(new CompoundTag(name));
        }

        private Tag readListOrArrayTag(String name) throws IOException {
            if (lookAhead(2) == ';') {
                switch (lookAhead(1)) {
                    case 'B':
                        // Byte array
                        return parseTag(new ByteArrayTag(name));
                    case 'S':
                        // Short array
                        return parseTag(new ShortArrayTag(name));
                    case 'I':
                        // Integer array
                        return parseTag(new IntArrayTag(name));
                    case 'L':
                        // Long array
                        return parseTag(new LongArrayTag(name));
                    case 'F':
                        // Float array
                        return parseTag(new FloatArrayTag(name));
                    case 'D':
                        // Double array
                        return parseTag(new DoubleArrayTag(name));
                    default:
                        // Treat as list tag
                        break;
                }
            }

            // This is a list tag
            return parseTag(new ListTag(name));
        }

        private Tag readPrimitiveTag(String name) throws IOException {
            String valueString = readNextSingleValueString(32);
            unread(valueString.toCharArray());
            return parseTag(getTagForStringifiedValue(name, valueString));
        }

        public String readNextSingleValueString() throws IOException {
            return readNextSingleValueString(Integer.MAX_VALUE);
        }

        // Used when expecting to unread to limit read to the length of the pushback buffer.
        public String readNextSingleValueString(int maxReadLenght) throws IOException {
            String valueString;
            if (lookAhead(0) == '\'' || lookAhead(0) == '\"') {
                char c = (char) read();
                valueString = c + readUntil(maxReadLenght, true, c);
            } else {
                valueString = readUntil(maxReadLenght, false, ',', '}', ']', '\r', '\n', '\t');
            }
            return valueString;
        }

        static final Pattern byteTagValuePattern = Pattern.compile("[-+]?\\d+[bB]");
        static final Pattern doubleTagValuePattern = Pattern.compile("[-+]?((\\d+(\\.\\d*)?)|(\\.\\d+))[dD]");
        static final Pattern floatTagValuePattern = Pattern.compile("[-+]?((\\d+(\\.\\d*)?)|(\\.\\d+))[fF]");
        static final Pattern intTagValuePattern = Pattern.compile("[-+]?\\d+");
        static final Pattern longTagValuePattern = Pattern.compile("[-+]?\\d+[lL]");
        static final Pattern shortTagValuePattern = Pattern.compile("[-+]?\\d+[sS]");

        private Tag getTagForStringifiedValue(String name, String stringifiedValue) {
            if (byteTagValuePattern.matcher(stringifiedValue).matches()) {
                // Byte
                return new ByteTag(name);
            } else if (doubleTagValuePattern.matcher(stringifiedValue).matches()) {
                // Double
                return new DoubleTag(name);
            } else if (floatTagValuePattern.matcher(stringifiedValue).matches()) {
                // Float
                return new FloatTag(name);
            } else if (intTagValuePattern.matcher(stringifiedValue).matches()) {
                // Integer
                return new IntTag(name);
            } else if (longTagValuePattern.matcher(stringifiedValue).matches()) {
                // Long
                return new LongTag(name);
            } else if (shortTagValuePattern.matcher(stringifiedValue).matches()) {
                // Short
                return new ShortTag(name);
            }
            // String
            return new StringTag(name);
        }

        public Tag parseTag(Tag tag) throws IOException {
            tag.destringify(this);
            return tag;
        }

        public void skipWhitespace() throws IOException {
            char c;
            while ((c = (char) read()) != -1) {
                if (c == '\t' || c == '\r' || c == '\n' || c == ' ') {
                    continue;
                } else {
                    unread(c);
                    return;
                }
            }
        }

        public char readSkipWhitespace() throws IOException {
            skipWhitespace();
            return (char) read();
        }

        public String readUntil(boolean includeEndChar, char... endChar) throws IOException {
            return readUntil(Integer.MAX_VALUE, includeEndChar, endChar);
        }

        // Used when expecting to unread to limit read to the length of the pushback buffer.
        public String readUntil(int maxReadLenght, boolean includeEndChar, char... endChar) throws IOException {
            StringBuilder sb = new StringBuilder();
            boolean escapeEnd = false;
            int reads = 0;
            char c;
            while (++reads < maxReadLenght && (c = (char) read()) != -1) {
                if (c == '\\') {
                    sb.append(c);
                    escapeEnd = true;
                    continue;
                }
                if (!escapeEnd && matchesAny(c, endChar)) {
                    if (includeEndChar) {
                        sb.append(c);
                    } else {
                        unread(c);
                    }
                    break;
                }
                sb.append(c);
                escapeEnd = false;
            }
            return sb.toString();
        }

        public char lookAhead(int offset) throws IOException {
            char[] future = new char[offset + 1];
            read(future);
            unread(future);
            return future[offset];
        }

        public static boolean matchesAny(char c, char[] matchable) {
            for (char m : matchable) {
                if (c == m)
                    return true;
            }
            return false;
        }
    }

    public static class StringifiedNBTWriter extends OutputStreamWriter {

        public StringifiedNBTWriter(OutputStream out) {
            super(out);
        }

        public void writeTag(Tag tag, boolean linebreak) throws IOException {
            writeTag(tag, linebreak, 0);
            flush();
        }

        public void writeTag(Tag tag, boolean linebreak, int depth) throws IOException {
            if (linebreak && depth > 0) {
                append('\n');
                indent(depth);
            }

            if (tag.getName() != null && !tag.getName().isEmpty()) {
                appendTagName(tag.getName());

                append(':');
                append(' ');
            }

            if (tag instanceof CompoundTag) {
                tag.stringify(this, linebreak, depth);
            } else if (tag instanceof ListTag) {
                tag.stringify(this, linebreak, depth);
            } else {
                tag.stringify(this, linebreak, depth);
            }
        }

        public static Pattern nonEscapedTagName = Pattern.compile("(?!\\d+)[\\wd]*");

        public void appendTagName(String tagName) throws IOException {
            if (!nonEscapedTagName.matcher(tagName).matches()) {
                append('"');
                append(tagName.replaceAll("\"", "\""));
                append('"');
            } else {
                append(tagName);
            }
        }

        public void indent(int depth) throws IOException {
            for (int i = 0; i < depth; i++) {
                append('\t');
            }
        }
    }
}