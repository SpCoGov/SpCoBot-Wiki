package top.spco.spcobot.wiki.util.nbt.tag.builtin;

import top.spco.spcobot.wiki.util.nbt.SNBTIO.StringifiedNBTReader;
import top.spco.spcobot.wiki.util.nbt.SNBTIO.StringifiedNBTWriter;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/**
 * A tag containing a float.
 */
public class FloatTag extends Tag {
    private float value;

    /**
     * Creates a tag with the specified name.
     *
     * @param name The name of the tag.
     */
    public FloatTag(String name) {
        this(name, 0);
    }

    /**
     * Creates a tag with the specified name.
     *
     * @param name  The name of the tag.
     * @param value The value of the tag.
     */
    public FloatTag(String name, float value) {
        super(name);
        this.value = value;
    }

    @Override
    public Float getValue() {
        return this.value;
    }

    /**
     * Sets the value of this tag.
     *
     * @param value New value of this tag.
     */
    public void setValue(float value) {
        this.value = value;
    }

    @Override
    public void read(DataInput in) throws IOException {
        this.value = in.readFloat();
    }

    @Override
    public void write(DataOutput out) throws IOException {
        out.writeFloat(this.value);
    }

    @Override
    public void destringify(StringifiedNBTReader in) throws IOException {
        String s = in.readNextSingleValueString();
        s = s.toLowerCase().substring(0, s.length() - 1);
        value = Float.parseFloat(s);
    }

    @Override
    public void stringify(StringifiedNBTWriter out, boolean linebreak, int depth) throws IOException {
        String sb = String.valueOf(value) + 'f';
        out.append(sb);
    }

    @Override
    public FloatTag clone() {
        return new FloatTag(this.getName(), this.getValue());
    }
}
