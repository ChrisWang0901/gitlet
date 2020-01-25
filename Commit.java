package gitlet;
import java.io.File;
import java.io.Serializable;
import java.sql.Timestamp;
import java.util.Date;
import java.util.HashMap;

/** Commit object contains message, reference,
 * parents, and time.
 *  @author Chris Wang
 */

public class Commit implements Serializable {
    /** Folder that stores all commits.*/
    static final File COMMIT_FOLDER = Utils.join(Main.MAIN_FOLDER, "commits");
    /** Floder that store current commit.*/
    static final File CURRENT_FOLDER = Utils.join(Main.MAIN_FOLDER, "current");

    /**
     * Creates a commit object with the specified parameters.
     * @param message Message of the commit
     * @param reference Reference of the commit
     * @param parent Parent of the commit
     * @param isInitial if it's the first commit
     * @param mergeInParent Merge-in parent of the commit
     */
    public Commit(String message, HashMap<String, Blob> reference,
                  Commit parent, Boolean isInitial, Commit mergeInParent) {
        _message = message;
        _reference = reference;
        _parent = parent;
        Timestamp time = new Timestamp(System.currentTimeMillis());

        _isInitial = isInitial;
        if (isInitial) {
            _time = new Date(0);
        } else {
            _time = new Date(time.getTime());
        }
        _mergeInParent = mergeInParent;

        _sha1 = Utils.sha1(Utils.serialize(this));
    }
    /**
     * a.
     *
     * @param sha1 SHA1 value of the commit
     * @return Commit read from file
     */
    public static Commit fromFile(String sha1) {
        File commitFile = Utils.join(COMMIT_FOLDER, sha1);
        if (!commitFile.exists()) {
            throw new IllegalArgumentException(
                    "No commit with that sha1 value found ;(");
        }
        return Utils.readObject(commitFile, Commit.class);
    }
    /**
     * Saves a commit to a file for future use.
     */
    public void saveCommit() {
        Utils.writeObject(Utils.join(COMMIT_FOLDER, this._sha1), this);
        Utils.writeObject(Utils.join(CURRENT_FOLDER, "current"), this);

    }
    @Override
    public String toString() {
        String time = _time.toString().replace(" PST", "");
        time += " -0800";
        if (_mergeInParent != null) {
            String mergeIDs = _parent._sha1.substring(0, 7)
                  + " " + _mergeInParent._sha1.substring(0, 7);
            return String.format("===\ncommit %s\nMerge: %s\nDate: %s\n%s\n",
                    _sha1, mergeIDs, time, _message);
        }
        return String.format("===\ncommit %s\nDate: %s\n%s\n",
                _sha1, time, _message);

    }
    @Override
    public boolean equals(Object o) {

        if (o == this) {
            return true;
        }

        if (!(o instanceof Commit)) {
            return false;
        }
        Commit c = (Commit) o;
        return c._sha1.equals(_sha1);


    }
    @Override
    public int hashCode() {
        int value = 0;
        for (int i = 0; i < 8; i++) {
            value += (int) _sha1.charAt(i);
        }
        return value;
    }

    /**
     * Return the blob of the file.
     * @param name Name of the file.
     */
    public Blob getBlob(String name) {
        return _reference.containsKey(name)
                ? _reference.get(name) : null;
    }
    /**
     * Return the reference of the commit.
     */
    public HashMap<String, Blob> getReference() {
        return _reference;
    }

    /** Return the parent of this commit. */
    public Commit getParent() {
        return _parent;
    }
    /** Return the message of the commit. */
    public String getMessage() {
        return _message;
    }
    /**
     * set merge-in parent to COMMIT.
     */
    public void setMerge(Commit commit) {
        _mergeInParent = commit;
    }
    /** Return the merge-in parent. */
    public Commit getMerge() {
        return _mergeInParent;
    }

    /** Return sha1 of the commit. */
    public String getSha1() {
        return _sha1;
    }

    /** message of the commit. */
    private String _message;
    /** reference of the commit. */
    private HashMap<String, Blob> _reference;
    /** time of this commit. */
    private Date _time;
    /** Parent of this commit. */
    private Commit _parent;
    /** SHA-1 value of this commit .*/
    private String _sha1;
    /** If it's is initial commit. */
    private Boolean _isInitial;
    /** merge-in parent. */
    private Commit _mergeInParent;
}
