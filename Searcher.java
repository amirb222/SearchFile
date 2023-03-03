import java.io.File;
import java.io.FileFilter;
import java.io.FilenameFilter;

/**
 * A searcher thread.
 * Searches for files with a given prefix in all directories listed in a directory queue.
 */
public class Searcher implements Runnable {
    private int id;
    private String prefix;
    private SynchronizedQueue<File> directoryQueue;
    private SynchronizedQueue<File> resultsQueue;
    private SynchronizedQueue<String> auditingQueue;
    private boolean isAudit;

    // Constructor
    Searcher(int id,
             String prefix,
             SynchronizedQueue<File> directoryQueue,
             SynchronizedQueue<File> resultsQueue,
             SynchronizedQueue<String> auditingQueue,
             boolean isAudit){
        this.id = id;
        this.prefix = prefix;
        this.directoryQueue = directoryQueue;
        this.resultsQueue = resultsQueue;
        this.auditingQueue = auditingQueue;
        this.isAudit = isAudit;
    }

    /**
    Runs the searcher thread.
    Thread will fetch a directory to search in from the directory queue, then filter all files inside it (but will not recursively search subdirectories!).
    Files that are found to contain the pattern Pattern and with prefix Prefix are enqueued to the results queue.
    When finishes, this method unregisters from the results queue.
     */
    @Override
    public void run() {
        File directory;
        this.resultsQueue.registerProducer();
        if ( this.isAudit ){ this.auditingQueue.registerProducer(); }
        // Iterate over directoryQueue
        while ( (directory = this.directoryQueue.dequeue()) != null && (directory.isDirectory()) ){
            // Get all files with matching prefixes from current directory
            File[] filesWithPrefix = directory.listFiles( (File file) -> !file.isDirectory() && file.getName().startsWith(this.prefix));
            // Add matching files to resultsQueue
            for ( File fileWithPrefix: filesWithPrefix ){
                this.resultsQueue.enqueue(fileWithPrefix);
                // If isAudit flag is up, update auditingQueue
                if ( this.isAudit ){
                    String audit = String.format("Searcher on thread id %d: file named %s was found", this.id, fileWithPrefix.getName());
                    this.auditingQueue.enqueue(audit);
                }
            }
        }
        this.resultsQueue.unregisterProducer();
        if ( this.isAudit ){ this.auditingQueue.unregisterProducer(); }
    }
}
