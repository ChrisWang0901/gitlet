package gitlet;
import java.io.File;
import java.io.Serializable;
import java.util.Arrays;
/** Blob object used for storing
 * the contents of files.
 *  @author Chris Wang
 */
public class Blob implements Serializable {
    /** Folder that stores all blobs. */
    static final File BLOB_FOLDER = Utils.join(Main.MAIN_FOLDER, "blobs");

    /** Create blob object with the specified parameters.
     * @param content Content of the file
     */
    public Blob(byte[] content) {
        _content = content;
        _sha1 = Utils.sha1(Utils.serialize(this));
    }

    /**
     * Reads in and deserializes a blob from a file
     * with sha1 SHA1 in BLOB_FOLDER.
     *
     * @param sha1 SHA1 value of the blob
     * @return Blob read from file
     */
    public static Blob fromFile(String sha1) {
        File blobFile = Utils.join(BLOB_FOLDER, sha1);
        if (!blobFile.exists()) {
            throw new IllegalArgumentException(
                    "No commit with that sha1 value found ;(");
        }
        return Utils.readObject(blobFile, Blob.class);
    }
    /**
     * Saves a blob to a file for future use.
     */
    public void saveBlob() {
        Utils.writeObject(Utils.join(BLOB_FOLDER, this._sha1), this);
    }

    /**
     * Write the content of blob to a file.
     * @param file File to output
     */
    public void write(File file) {
        Utils.writeContents(file, _content);
    }

    /** return the content.*/
    public byte[] getContent() {
        return _content;
    }
    @Override
    public boolean equals(Object o) {

        if (o == this) {
            return true;
        }

        if (!(o instanceof Blob)) {
            return false;
        }

        Blob c = (Blob) o;
        return Arrays.equals(c._content, _content);
    }
    @Override
    public int hashCode() {
        return Integer.parseInt(_sha1);
    }
    /** contents of the blob stored in byte array type.*/
    private byte[] _content;
    /** sha1 value of the blob.*/
    private String _sha1;

}
