import java.io.*;

/**
 This class reads a file from the results queue (the queue of files that contains the output of
 the searchers), and copies it into the specified destination directory.
 */
public class Copier implements Runnable {
    public static final int COPY_BUFFER_SIZE = 4096;
    private int id;
    private File destination;
    private SynchronizedQueue<File> resultsQueue;
    private SynchronizedQueue<String> auditingQueue;
    private boolean isAudit;

    // Constructor
    Copier(int id,
             File destination,
             SynchronizedQueue<File> resultsQueue,
             SynchronizedQueue<String> auditingQueue,
             boolean isAudit){
        this.id = id;
        this.destination = destination;
        this.resultsQueue = resultsQueue;
        this.auditingQueue = auditingQueue;
        this.isAudit = isAudit;
    }

    @Override
    public void run() {
        if ( this.isAudit ){ this.auditingQueue.registerProducer(); }

        File f;
        while( (f = this.resultsQueue.dequeue()) != null && (f.isFile()) ){
            // Create new output file
            File outFile = new File(this.destination, f.getName());
            byte[] buf = new byte[COPY_BUFFER_SIZE];
            try {
                // Create in/out streams to copy COPY_BUFFER_SIZE at a time to new file
                InputStream inStream = new FileInputStream(f);
                OutputStream outStream = new FileOutputStream(outFile);
                // Write COPY_BUFFER_SIZE bytes to outFile iteratively
                while(inStream.read(buf) > 0){
                    outStream.write(buf);
                }
                // If isAudit flag is up, update auditingQueue
                if (this.isAudit){
                    String audit = String.format("Copier from thread id %d: file named %s was copied", this.id, f.getName());
                    this.auditingQueue.enqueue(audit);
                }
            } catch (Exception e) {
                this.resultsQueue.unregisterProducer();
                if ( this.isAudit ){ this.auditingQueue.unregisterProducer(); }
                e.printStackTrace();
            }
        }
        if ( this.isAudit ){ this.auditingQueue.unregisterProducer(); }
    }
}
