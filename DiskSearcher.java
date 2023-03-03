import java.io.File;
import java.util.ArrayList;

/**
 This is the main class of the application. This class contains a main method that starts the
 search process according to the given command lines.
 */
public class DiskSearcher {
    public static final int AUDITING_QUEUE_CAPACITY = 50; // Capacity of the auditing thread (must be above the number of threads working in total)
    public static final int DIRECTORY_QUEUE_CAPACITY = 50; // Capacity of the queue that holds the directories to be searched
    public static final int RESULTS_QUEUE_CAPACITY = 50; // Capacity of the queue that holds the files found

    public static void main(String[] args){
        // Start measuring the running time of the program
        long startTime = System.currentTimeMillis();

        // Call to function is as follows:
        //> java DiskSearcher <boolean of milestoneQueueFlag> <file-prefix> <root directory> <destination directory> <# of searchers> <# of copiers>
        int id = 0;
        boolean isAudit = Boolean.parseBoolean(args[0]);
        String prefix = args[1];
        File root = new File(args[2]);
        File destination = new File(args[3]);
        int numSearchers = Integer.parseInt(args[4]);
        int numCopiers = Integer.parseInt(args[5]);

        SynchronizedQueue<File> directoryQueue = new SynchronizedQueue<>(DIRECTORY_QUEUE_CAPACITY);
        SynchronizedQueue<File> resultsQueue = new SynchronizedQueue<>(RESULTS_QUEUE_CAPACITY);
        SynchronizedQueue<String> auditingQueue = null;
        if ( isAudit ){
            auditingQueue = new SynchronizedQueue<>(AUDITING_QUEUE_CAPACITY);
            auditingQueue.registerProducer();
            String audit = "General, program has started the search";
            auditingQueue.enqueue(audit);
            auditingQueue.unregisterProducer();
        }

        // Create array of Thread to track completed Threads
        ArrayList<Thread> threadArray = new ArrayList<>();

        // Generate thread that calls the Scouter class
        Scouter scouter = new Scouter(id++, directoryQueue, root, auditingQueue, isAudit);
        Thread scouterThread = new Thread(scouter);
        scouterThread.start();
        threadArray.add(scouterThread);

        // Generate threads that call the Searcher class
        for(int i = 0; i < numSearchers; i++){
            Searcher searcher = new Searcher(id++, prefix, directoryQueue, resultsQueue, auditingQueue, isAudit);
            Thread searcherThread = new Thread(searcher);
            searcherThread.start();
            threadArray.add(searcherThread);
        }

        // Generate threads that call the Copier class
        for(int i = 0; i < numCopiers; i++){
            Copier copier = new Copier(id++, destination, resultsQueue, auditingQueue, isAudit);
            Thread copierThread = new Thread(copier);
            copierThread.start();
            threadArray.add(copierThread);
        }

        // if isAudit flag is up: dequeue and print all elements in auditingQueue iteratively
        if ( isAudit ){
            String s;
            int index = 0;
            while( (s = auditingQueue.dequeue()) != null ){
                String audit = String.format("%d - %s", index, s);
                System.out.println(audit);
                index ++;
            }
        }

        // Wait for all threads to complete
        try {
            for (int i = 0; i < threadArray.size(); i++) {
                threadArray.get(i).join();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Get complete running time of the program in milli-seconds
        long totalRunningTime = System.currentTimeMillis() - startTime;
        System.out.println("Total running time of the program is: " + totalRunningTime);
    }
}
