package gitlet;


import java.io.File;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.HashSet;

/** Driver class for Gitlet, the tiny stupid version-control system.
 *  @author Chris Wang , Chen Feng Tsai, Wei Min Chou
 */
public class Main {

    /** Usage: java gitlet.Main ARGS, where ARGS contains
     *  <COMMAND> <OPERAND> .... */
    /** Current Working Directory. */
    static final File CWD = new File(".");
    /** Main metadata folder. */
    static final File MAIN_FOLDER = Utils.join(CWD, ".gitlet");
    /** Staging area. */
    static final File STAGING_AREA = Utils.join(MAIN_FOLDER, "Stage");
    /** adding. */
    static final File STAGING_ADD = Utils.join(STAGING_AREA, "Addition");
    /** Removing. */
    static final File STAGING_REMOVE = Utils.join(STAGING_AREA, "Removal");
    /** Storing branch. */
    static final File BRANCH = Utils.join(MAIN_FOLDER, "branch");

    /** Running all the commands.
     * @param args Array{[command] [parameters]}
     */
    public static void main(String... args) {
        if (args.length == 0) {
            System.out.println(" Please enter a command.");
            System.exit(0);
        }
        switch (args[0]) {
        case "init": {
            init();
            break;
        } case "add": {
            add(args);
            break;
        } case "commit": {
            commit(args);
            break;
        } case "rm": {
            rm(args);
            break;
        } case "log": {
            log();
            break;
        } case "checkout": {
            checkout(args);
            break;
        } case "branch": {
            branch(args);
            break;
        } case "global-log": {
            globalLog();
            break;
        } case "find": {
            find(args);
            break;
        } case "cwd": {
            for (String s : Utils.plainFilenamesIn(CWD)) {
                System.out.println(s);
            }
            break;
        } case "status": {
            status();
            break;
        } case "rm-branch": {
            rmBranch(args);
            break;
        } case "reset": {
            reset(args);
            break;
        } case "merge": {
            merge(args);
            break;

        } case "set":{
            setup();
            break;
            }
            default: {
            System.out.println("No command with that name exists.");
            System.exit(0);
        }
        }
    }
    /** Check if all the required folder exist.*/
    public static void setupPersistence() {
        if (!MAIN_FOLDER.exists()) {
            MAIN_FOLDER.mkdirs();
            STAGING_ADD.mkdirs();
            STAGING_REMOVE.mkdirs();
            Commit.COMMIT_FOLDER.mkdirs();
            Blob.BLOB_FOLDER.mkdirs();
            Commit.CURRENT_FOLDER.mkdirs();
            BRANCH.mkdirs();
        }
    }

    /** Initialize the .gitlet repository.*/
    public static void init() {
        if (MAIN_FOLDER.exists()) {
            System.out.println("A Gitlet version-control system "
                   +  "already exists in the current directory.");
            System.exit(0);
        }
        setupPersistence();
        Commit commit = new Commit("initial commit",
                new HashMap<String, Blob>(), null,
                true, null);
        commit.saveCommit();
        File masterBranch = Utils.join(BRANCH, "master");
        File activeBranch = Utils.join(BRANCH, "active");
        Utils.writeObject(masterBranch, commit);
        Utils.writeContents(activeBranch, "master");
    }

    /** Adding file to the staging area.
     *
     * @param args Array {"add" file}
     */
    public static void add(String[] args) {
        if (args.length != 2) {
            System.out.println("Incorrect operands.");
            System.exit(0);
        }
        String name = args[1];
        File file = Utils.join(CWD, name);
        if (!file.exists()) {
            System.out.println("File does not exist.");
            System.exit(0);
        } else {
            File stagingFile = Utils.join(STAGING_ADD, name);
            Commit current = getCurrent();
            Blob blob = new Blob(Utils.readContents(file));
            List<String> rmFiles = Utils.plainFilenamesIn(STAGING_REMOVE);
            if (rmFiles.contains(name)) {
                File rmFile = Utils.join(STAGING_REMOVE, name);
                rmFile.delete();
            }
            if (!blob.equals(current.getBlob(name))) {
                Utils.writeObject(stagingFile, blob);
                blob.saveBlob();
            }
        }

    }

    /** make a new commit that store all the
     * information and clear the staging area.
     * @param args {commit message}
     */
    public static void commit(String[] args) {
        if (args.length == 1 || args[1].equals("")) {
            System.out.println("Please enter a commit message.");
            System.exit(0);
        }
        String message = args[1];
        List<String> addFiles = Utils.plainFilenamesIn(STAGING_ADD);
        List<String> rmFiles = Utils.plainFilenamesIn(STAGING_REMOVE);
        if (addFiles.isEmpty() && rmFiles.isEmpty()) {
            System.out.println("No changes added to the commit.");
            System.exit(0);
        }
        HashMap<String, Blob> reference = getActive().getReference();
        for (String s : rmFiles) {
            if (reference.containsKey(s)) {
                reference.remove(s);
            }
            File file = Utils.join(STAGING_REMOVE, s);
            file.delete();
        }
        Commit parent = getActive();
        for (String name : addFiles) {
            File file = Utils.join(STAGING_ADD, name);
            Blob blob = Utils.readObject(file, Blob.class);
            reference.put(name, blob);
            file.delete();
        }
        Commit currentCommit = new Commit(message, reference, parent,
                false, null);
        currentCommit.saveCommit();
        File activeBranch = getActiveFile();
        Utils.writeObject(activeBranch, currentCommit);

    }
    /** Unstage the file if it is currently staged. If the file is tracked in
     * current commit, mark it not include in the next commit. And then remove
     * the file working directory.
     * @param args Array in format {rm, name}
     */
    public static void rm(String[] args) {
        if (args.length != 2) {
            System.out.println("Incorrect operands.");
            System.exit(0);
        }
        String name = args[1];
        File file = Utils.join(STAGING_ADD, name);
        HashMap<String, Blob> reference = getActive().getReference();
        if (!file.exists() && !reference.containsKey(name)) {
            System.out.print("No reason to remove the file.");
            System.exit(0);

        }
        if (file.exists()) {
            file.delete();
        }
        if (reference.containsKey(name)) {
            File rmFile = Utils.join(STAGING_REMOVE, name);
            Utils.writeObject(rmFile, reference.get(name));
            File deleteFile = Utils.join(CWD, name);
            deleteFile.delete();
        }
    }
    /** Print all the commits made. */
    public static void log() {
        Commit current = getActive();
        while (current.getParent() != null) {
            System.out.println(current);
            current = current.getParent();
        }
        System.out.print(current);
    }
    /** convert to a given state.
     * @param args Array {checkout ....}
     */

    public static void checkout(String[] args) {
        if (args[1].equals("--")
            && args.length == 3) {
            String name = args[2];
            HashMap<String, Blob> reference = getCurrent().getReference();
            if (!reference.containsKey(name)) {
                System.out.println("File does not exist in that commit.");
                System.exit(0);
            }
            File file = Utils.join(CWD, name);
            Blob blob = reference.get(name);
            blob.write(file);
        } else if (args.length == 4) {
            if (!args[2].equals("--")) {
                System.out.println("Incorrect operands.");
                System.exit(0);
            }
            String id = args[1];
            String name = args[3];
            String target = null;
            List<String> ids = Utils.plainFilenamesIn(Commit.COMMIT_FOLDER);
            for (String s : ids) {
                String i = s.substring(0, id.length());
                if (id.equals(i)) {
                    target = s;
                    break;
                }
            }
            if (target == null) {
                System.out.println("No commit with that id exists.");
                System.exit(0);
            }
            Commit commit = Commit.fromFile(target);
            if (commit.getReference().containsKey(name)) {
                Blob blob = commit.getReference().get(name);
                File file = Utils.join(CWD, name);
                blob.write(file);
            } else {
                System.out.println("File does not exist in that commit.");
                System.exit(0);
            }
        } else if (args.length == 2) {
            checkoutBranch(args);
        }
    }
    /** Put all the files in  the commit of the give branch head
     * into the working directory. Overwriting if they already
     * exist. Change the active branch to this branch.
     * @param args An array {"branch" [branch name]}
     */


    public static void checkoutBranch(String[] args) {
        if (args.length != 2) {
            System.out.println("Incorrect operands.");
            System.exit(0);
        }
        String branchName = args[1];
        if (branchName.equals(getActiveName())) {
            System.out.println("No need to checkout the current branch.");
            System.exit(0);
        }
        List<String> branches = Utils.plainFilenamesIn(BRANCH);
        if (!branches.contains(branchName)) {
            System.out.println("No such branch exists.");
            System.exit(0);
        }
        File branch = Utils.join(BRANCH, branchName);
        Commit activeCommit = getActive();
        Commit branchHead = Utils.readObject(branch, Commit.class);
        List<String> untrackedFiles = new ArrayList<String>();
        for (String s : branchHead.getReference().keySet()) {
            if (!activeCommit.getReference().containsKey(s)) {
                untrackedFiles.add(s);
            }
        }
        for (String s : untrackedFiles) {
            List<String> filesCWD = Utils.plainFilenamesIn(CWD);
            if (filesCWD.contains(s)) {
                System.out.println("There is an untracked file in the way;"
                       +  " delete it or add it first.");
                System.exit(0);
            }
        }
        for (String s : branchHead.getReference().keySet()) {
            File file = Utils.join(CWD, s);
            Blob blob = branchHead.getBlob(s);
            blob.write(file);
        }
        Commit current = getActive();
        Set<String> files = current.getReference().keySet();
        for (String s : files) {
            if (!branchHead.getReference().containsKey(s)) {
                File rmFile = Utils.join(CWD, s);
                rmFile.delete();
            }
        }



        File activeBranch = Utils.join(BRANCH, "active");
        Utils.writeContents(activeBranch, branchName);


    }
    /** Make a new branch.
     * @param args An array {"branch" [branch name]}
     */
    public static void branch(String[] args) {
        if (args.length != 2) {
            System.out.println("Incorrect operands.");
            System.exit(0);
        }
        String branchName = args[1];
        List<String> branches = Utils.plainFilenamesIn(BRANCH);
        if (branches.contains(branchName)) {
            System.out.println("A branch with that name already exists.");
            System.exit(0);
        }
        File newBranch = Utils.join(BRANCH, branchName);
        Utils.writeObject(newBranch, getActive());
    }
    /** Like log, except displays information about all commits ever made.
     * The order of the commits does not matter. */
    public static void globalLog() {
        List<String> ids = Utils.plainFilenamesIn(Commit.COMMIT_FOLDER);
        for (String id : ids) {
            File file = Utils.join(Commit.COMMIT_FOLDER, id);
            Commit commit = Utils.readObject(file, Commit.class);
            System.out.println(commit);
        }
    }
    /**
     * : Prints out the ids of all commits that
     * have the given commit message, one per line.
     * @param args Array {find [message]}
     */
    public static void find(String[] args) {
        if (args.length != 2) {
            System.out.println("Incorrect operands.");
            System.exit(0);
        }
        String message = args[1];
        Boolean found = false;
        List<String> ids = Utils.plainFilenamesIn(Commit.COMMIT_FOLDER);
        for (String id : ids) {
            File file = Utils.join(Commit.COMMIT_FOLDER, id);
            Commit commit = Utils.readObject(file, Commit.class);
            if (commit.getMessage().equals(message)) {
                found = true;
                System.out.println(id);
            }
        }
        if (!found) {
            System.out.println("Found no commit with that message.");
            System.exit(0);
        }
    }
    /**
     * Displays what branches currently exist,
     * and marks the current branch with a *.
     * Also displays what files have been staged or marked for untracking.
     */
    public static void status() {
        if (!MAIN_FOLDER.exists()) {
            System.out.println("Not in an initialized Gitlet directory.");
            System.exit(0);
        }
        File currentBranch = Utils.join(BRANCH, "active");
        String currentBranchName =
                Utils.readContentsAsString(currentBranch);
        System.out.println("=== Branches ===");
        System.out.println("*" + currentBranchName);
        for (String s : Utils.plainFilenamesIn(BRANCH)) {
            if (!s.equals(currentBranchName)
                && !s.equals("active")) {
                System.out.println(s);
            }
        }
        System.out.println("\n=== Staged Files ===");
        for (String s : Utils.plainFilenamesIn(STAGING_ADD)) {
            System.out.println(s);
        }
        System.out.println("\n=== Removed Files ===");
        for (String s : Utils.plainFilenamesIn(STAGING_REMOVE)) {
            System.out.println(s);
        }
        System.out.println("\n=== Modifications Not Staged For Commit ===");
        System.out.println("\n=== Untracked Files ===\n");
    }
    /**
     * Deletes the branch with the given name. This only means to
     * delete the pointer associated with the branch;
     * it does not mean to delete all commits that
     * were created under the branch, or anything like that.
     * @param args Array{"rm-branch" [branch name]}
     */
    public static void rmBranch(String[] args) {
        if (args.length != 2) {
            System.out.println("Incorrect operands.");
            System.exit(0);
        }
        String name = args[1];
        List<String> branches = Utils.plainFilenamesIn(BRANCH);
        if (!branches.contains(name)) {
            System.out.println(" A branch with that name does not exist.");
            System.exit(0);
        }
        String currentBranchName = getActiveName();
        if (currentBranchName.equals(name)) {
            System.out.println("Cannot remove the current branch.");
            System.exit(0);
        }
        File file = Utils.join(BRANCH, name);
        file.delete();
    }
    /**
     * Checks out all the files tracked by the given commit.
     * Removes tracked files that are not present in that commit.
     * Also moves the current branch's head to that commit node.
     * @param args Array {"reset", [commit id]}
     */
    public static void reset(String[] args) {
        String id = args[1];
        String target = null;
        List<String> ids = Utils.plainFilenamesIn(Commit.COMMIT_FOLDER);
        for (String s : ids) {
            String i = s.substring(0, id.length());
            if (id.equals(i)) {
                target = s;
                break;
            }
        }
        if (target == null) {
            System.out.println("No commit with that id exists.");
            System.exit(0);
        }
        Commit commit = Commit.fromFile(target);
        Commit activeCommit = getActive();
        List<String> untrackedFiles = new ArrayList<String>();
        for (String s : commit.getReference().keySet()) {
            if (!activeCommit.getReference().containsKey(s)) {
                untrackedFiles.add(s);
            }
        }
        for (String s : untrackedFiles) {
            List<String> filesCWD = Utils.plainFilenamesIn(CWD);
            if (filesCWD.contains(s)) {
                System.out.println("There is an untracked file in the way;"
                        + " delete it or add it first.");
                System.exit(0);
            }
        }

        for (String s : Utils.plainFilenamesIn(STAGING_REMOVE)) {
            Utils.join(STAGING_REMOVE, s).delete();
        }
        for (String s : Utils.plainFilenamesIn(STAGING_ADD)) {
            Utils.join(STAGING_ADD, s).delete();
        }

        HashMap<String, Blob> reference = commit.getReference();
        for (String name : reference.keySet()) {
            String[] arr = {"checkout", id, "--", name };
            Main.checkout(arr);
        }
        Commit current = getActive();
        Set<String> files = current.getReference().keySet();
        for (String s : files) {
            if (!reference.containsKey(s)) {
                File rmFile = Utils.join(CWD, s);
                rmFile.delete();
            }
        }
        String branchHead = getActiveName();
        File head = Utils.join(BRANCH, branchHead);
        Utils.writeObject(head, commit);

    }
    /**
     * merge.
     * @param args Array {"merge' [branch name]}
     */
    public static void merge(String[] args) {
        if (args.length != 2) {
            System.out.println("Incorrect operands.");
            System.exit(0);
        }
        String branch = args[1];
        Commit current = getActive();
        Commit givenBranch = getBranch(branch);
        Commit lca = lca(branch);
        checkMerge(branch);

        Set<String> lcaFiles = lca.getReference().keySet();
        Set<String> currentFiles = current.getReference().keySet();
        Set<String> branchFiles = givenBranch.getReference().keySet();
        Boolean hasConflict = mergeAddRm(branch);
        for (String name : branchFiles) {
            if (!lcaFiles.contains(name)
                    && branchFiles.contains(name)) {
                File file = Utils.join(CWD, name);
                givenBranch.getBlob(name).write(file);
                String[] arr = {"add", name};
                Main.add(arr);
            }
        }
        String message = "Merged " + branch + " into "
                + getActiveName() + ".";
        if (hasConflict) {
            System.out.println("Encountered a merge conflict.");
        }
        List<String> addFiles = Utils.plainFilenamesIn(STAGING_ADD);
        List<String> rmFiles = Utils.plainFilenamesIn(STAGING_REMOVE);
        HashMap<String, Blob> reference = getActive().getReference();
        for (String s : rmFiles) {
            if (reference.containsKey(s)) {
                reference.remove(s);
            }
        }
        Commit parent = getActive();
        for (String name : addFiles) {
            File file = Utils.join(STAGING_ADD, name);
            Blob blob = Utils.readObject(file, Blob.class);
            reference.put(name, blob);
            file.delete();
        }
        Commit currentCommit = new Commit(message, reference,
                parent, false, givenBranch);
        currentCommit.saveCommit();
        clearStage();
        File activeBranch = getActiveFile();
        Utils.writeObject(activeBranch, currentCommit);
    }
    /**
     * Check if the merge command has error.
     * @param branch Given branch
     */
    public static void checkMerge(String branch) {
        Commit current = getActive();
        Commit givenBranch = getBranch(branch);
        Commit lca = lca(branch);
        List<String> addFiles = Utils.plainFilenamesIn(STAGING_ADD);
        List<String> rmFiles = Utils.plainFilenamesIn(STAGING_REMOVE);
        if (!addFiles.isEmpty() || !rmFiles.isEmpty()) {
            System.out.println("You have uncommitted changes.");
            System.exit(0);
        }
        if (current.equals(givenBranch)) {
            System.out.println("Cannot merge a branch with itself");
            System.exit(0);
        }
        if (lca.equals(givenBranch)) {
            System.out.println("Given branch is an ancestor "
                    + "of the current branch.");
            System.exit(0);
        }
        if (lca.equals(current)) {
            File file = Utils.join(BRANCH, "active");
            Utils.writeObject(file, givenBranch);
            Set<String> currentFiles = current.getReference().keySet();
            Set<String> branchFiles = givenBranch.getReference().keySet();
            for (String name : branchFiles) {
                File f = Utils.join(CWD, name);
                givenBranch.getBlob(name).write(f);
            }
            for (String name : currentFiles) {
                File f = Utils.join(CWD, name);
                if (!branchFiles.contains(name)) {
                    f.delete();
                }
            }
            System.out.println("Current branch fast-forwarded.");
            System.exit(0);
        }
        Set<String> lcaFiles = lca.getReference().keySet();
        Set<String> currentFiles = current.getReference().keySet();
        Set<String> branchFiles = givenBranch.getReference().keySet();
        for (String name : branchFiles) {
            File cwd = Utils.join(CWD, name);
            if (!currentFiles.contains(name)
                    && cwd.exists()) {
                System.out.print("There is an untracked "
                        +  "file in the way; delete it or add it first.");
                System.exit(0);
            }
        }
    }
    /**
     * Add and remove some files to the
     * staging area according to merge rules
     * and Return if there is conflict.
     * @param branch Name of the given branch.
     *
     */
    public static Boolean mergeAddRm(String branch) {
        Commit current = getActive();
        Commit givenBranch = getBranch(branch);
        Commit lca = lca(branch);
        Set<String> lcaFiles = lca.getReference().keySet();
        Set<String> currentFiles = current.getReference().keySet();
        Set<String> branchFiles = givenBranch.getReference().keySet();
        Boolean hasConflict = false;
        for (String name : lcaFiles) {
            if (!sameContent(lca, givenBranch, name)
                    && sameContent(lca, current, name)) {
                if (branchFiles.contains(name)) {
                    File file = Utils.join(CWD, name);
                    givenBranch.getBlob(name).write(file);
                    String[] arr = {"add", name};
                    Main.add(arr);
                }
            }
            if (!lcaFiles.contains(name)
                    && !currentFiles.contains(name)
                    && branchFiles.contains(name)) {
                File file = Utils.join(CWD, name);
                givenBranch.getBlob(name).write(file);
                String[] arr = {"add", name};
                Main.add(arr);
            } else if (lcaFiles.contains(name)
                    && sameContent(lca, current, name)
                    && !branchFiles.contains(name)) {
                String[] arr = {"rm", name};
                Main.rm(arr);
            } else if (!sameContent(lca, givenBranch, name)
                    && !sameContent(lca, current, name)
                    && !sameContent(current, givenBranch, name)) {
                String gbContent = branchFiles.contains(name)
                        ? new String(givenBranch.getBlob(name).getContent())
                        : "";
                String currContent = currentFiles.contains(name)
                        ? new String(current.getBlob(name).getContent())
                        : "";
                String conflictContent = "<<<<<<< HEAD\n"
                        + currContent + "=======\n"
                        + gbContent + ">>>>>>>\n";
                File file = Utils.join(CWD, name);
                Utils.writeContents(file, conflictContent);
                String[] arr = {"add", name};
                Main.add(arr);
                hasConflict = true;
            }
        }
        return hasConflict;
    }

    /**
     * Return the current commit.
     */
    public static Commit getCurrent() {
        File commitFile = Utils.join(Commit.CURRENT_FOLDER, "current");
        if (!commitFile.exists()) {
            throw new IllegalArgumentException(
                    "No commit with that sha1 value found ;(");
        }
        return Utils.readObject(commitFile, Commit.class);
    }
    /**
     * Return the commit of the active branch.
     */
    public static Commit getActive() {
        File activeBranch = getActiveFile();
        return Utils.readObject(activeBranch, Commit.class);
    }
    /**
     * Return the active branch file.
     */
    public static File getActiveFile() {
        String branchName = getActiveName();
        return Utils.join(BRANCH, branchName);
    }
    /**
     * Return the active branch name.
     */
    public static String getActiveName() {
        File activeFile = Utils.join(BRANCH, "active");
        if (!activeFile.exists()) {
            System.out.println("No commit with that sha1 value found ;(");
            System.exit(0);
        }
        return Utils.readContentsAsString(activeFile);
    }
    /**
     * Return the given branch.
     * @param branch Branch name
     */
    public static Commit getBranch(String branch) {
        File file = Utils.join(BRANCH, branch);
        if (!file.exists()) {
            System.out.println("A branch with that name does not exist.");
            System.exit(0);
        }
        return Utils.readObject(file, Commit.class);

    }

    /**
     * Return the latest common ancestor between current branch
     * and the given branch.
     * @param branch Given branch
     */
    public static Commit lca(String branch) {
        Commit branchHead = getBranch(branch);
        Commit current = getActive();
        HashSet<Commit> commits = getCommits(branchHead);
        HashSet<String> commitIDs = new HashSet<String>();
        for (Commit c : commits) {
            commitIDs.add(c.getSha1());
        }
        LinkedBlockingQueue<Commit> queue = new LinkedBlockingQueue<>();
        queue.add(current);
        while (!queue.isEmpty()) {
            Commit commit = queue.poll();
            if (commitIDs.contains(commit.getSha1())) {
                return commit;
            }
            if (commit.getParent() != null) {
                queue.add(commit.getParent());
            }
            if (commit.getMerge() != null) {
                queue.add(commit.getMerge());
            }
        }
        return null;
    }
    /**
     * Return all the commits of a branch from a given head.
     * @param commit head commit
     */
    public static HashSet<Commit> getCommits(Commit commit) {
        HashSet<Commit> commits =  new HashSet<Commit>();
        if (commit == null) {
            return commits;
        } else {
            commits.add(commit);
            commits.addAll(getCommits(commit.getParent()));
            commits.addAll(getCommits(commit.getMerge()));
            return commits;
        }
    }
    /**
     * Return True if two commits have the same content
     * of the given file.
     * @param commit1 first commit
     * @param commit2 second commit
     * @param file file name
     */
    public static Boolean sameContent(
            Commit commit1, Commit commit2, String file) {
        if (commit1.getBlob(file) != null) {
            return commit1.getBlob(file).equals(
                    commit2.getBlob(file));
        }

        return commit2.getBlob(file) == null;

    }
    /** Clear the staging area. */
    public static void clearStage() {
        List<String> addFiles = Utils.plainFilenamesIn(STAGING_ADD);
        List<String> rmFiles = Utils.plainFilenamesIn(STAGING_REMOVE);

        for (String s : rmFiles) {
            File file = Utils.join(STAGING_REMOVE, s);
            file.delete();
        }
        Commit parent = getActive();
        for (String name : addFiles) {
            File file = Utils.join(STAGING_ADD, name);
            file.delete();
        }
    }
    public static void setup() {
        Main.init();
        String[] addargs = {"add", "f.txt"};
        Main.add(addargs);
        addargs[1] = "g.txt";
        Main.add(addargs);
        String[] commitargs = {"commit" , "add f g"};
        Main.commit(commitargs);
        String[] branchargs = {"branch", "other"};
        Main.branch(branchargs);
        addargs[1] = "h.txt";
        String[] rmargs = {"rm", "g.txt"};
        Main.rm(rmargs);
        File file = Utils.join(CWD, "f.txt");
        String wug2 = "this is wug2.";
        Utils.writeContents(file, wug2);
        addargs[1] = "f.txt";
        Main.add(addargs);
        commitargs[1] = "Add h.txt, remove g.txt";
        Main.commit(commitargs);
        String[] check = {"checkout", "other"};
        Main.checkout(check);
        String notwug = "This is not a wug.";
        Utils.writeContents(file, notwug);
        Main.add(addargs);
        rmargs[1] = "f.txt";
        Main.rm(rmargs);
        addargs[1] = "k.txt";
        Main.add(addargs);
        commitargs[1] = "Add k.txt and rm f.txt";
        Main.commit(commitargs);
        check[1] = "master";
        Main.checkout(check);
//        rmargs[1] = "f.txt";


    }


}
