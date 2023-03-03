import java.io.File;

/**
    A scouter thread - This thread lists all sub-directories from a given root path.
    Each sub-directory is enqueued to be searched for files by Searcher threads.
 */
public class Scouter implements Runnable {
    private int id;
    private SynchronizedQueue<File> directoryQueue;
    private File root;
    private SynchronizedQueue<String> auditingQueue;
    private boolean isAudit;

    Scouter(int id,
            SynchronizedQueue<File> directoryQueue,
            File root,
            SynchronizedQueue<String> auditingQueue,
            boolean isAudit){
        this.id = id;
        this.directoryQueue = directoryQueue;
        this.root = root;
        this.auditingQueue = auditingQueue;
        this.isAudit = isAudit;
    }

    /**
     * Starts the scouter thread.
     * Lists directories under root directory and adds them to queue, then lists directories in the next level and enqueues them and so on.
     * When finishes, this method unregisters from the directory queue.
     */
    @Override
    public void run() {
        this.directoryQueue.registerProducer();
        if ( this.isAudit ){ this.auditingQueue.registerProducer(); }
        treeWalk(root);
        if ( this.isAudit ){ this.auditingQueue.unregisterProducer(); }
        this.directoryQueue.unregisterProducer();
    }


    /**
     * Recursively add all directories in root to directoryQueue
     * @param node
     */
    public void treeWalk( File node ) {
        File[] list = node.listFiles();
        if ( list == null ){ return; }
        for ( File f : list ) {
            if ( f.isDirectory() ) {
                this.directoryQueue.enqueue(f);
                // If isAudit flag is up, update auditingQueue
                if (this.isAudit){
                    String audit = String.format("Scouter on thread id %d: directory named %s was scouted", this.id, f.getName());
                    this.auditingQueue.enqueue(audit);
                }
                this.treeWalk(f);
            }
        }
    }
}
