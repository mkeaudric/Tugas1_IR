import java.util.ArrayList;
import java.util.Scanner;

public class main {
    public static void main(String[] args) {
        // 1. Bangun Inverted Index (Proses Indexing) dan load dokumen
        printHeader();

        System.out.println(" [Status] Ready! Please enter your boolean query below.");
        System.out.println("==========================================================");

        // 2. Siapkan index, Boolean Model, evaluaror, dan Search Engine
        InvertedIndex index = new InvertedIndex();
        SearchEngine engine = new SearchEngine(index);
        BooleanModel bm = new BooleanModel();
        QueryEvaluator evaluator = new QueryEvaluator(index, bm, engine);

        // 3. Input Query
        Scanner input = new Scanner(System.in);
        System.out.print("\nInput your Query: ");
        String userQuery = input.nextLine();

        // 4. Eksekusi Pencarian
        // QueryEvaluator akan melakukan tokenisasi, stemming, dan boolean logic
        ArrayList<InvertedIndex.Posting> results = evaluator.evaluate(userQuery);

        if (results != null && !results.isEmpty()) {
            results.sort((p1, p2) -> Integer.compare(p2.freq, p1.freq));
        } else {
            System.out.println("Tidak ada dokumen yang ditemukan.");
        }
        
        // 5. Tampilkan Hasil
        displayResults(results);

        input.close();

        // Kalo mau ngetest precision and recall, pake ini buat load ground truth dari file cranqrel100.txt
        // HashMap<Integer, HashSet<Integer>> groundTruth = evaluator.loadGroundTruth("test/cranqrel100.txt");

        // Gunakan ini jika ingin evaluasi model (presisi, recall) berdasarkan ID query
        // System.out.print("\nMasukkan ID Query untuk evaluasi (misal: 1): ");
        // if (input.hasNextInt()) {
        //     int qID = input.nextInt();
        //     // Panggil fungsi hitung metrik di evaluator
        //     evaluator.calculateMetrics(qID, results, groundTruth);
        // }
    }
    
    private static void displayResults(ArrayList<InvertedIndex.Posting> results) {
        InvertedIndex index = new InvertedIndex();
        index.loadTitles("cran/cran.all.1400");

        if (results == null || results.isEmpty()) {
            System.out.println("\n [!] No documents found.");
            return;
        }

        System.out.println("\nFound " + results.size() + " document(s):");
        System.out.println("--------------------------------------------------------------------------------");
        System.out.printf("%-7s | %-5s | %s\n", "DOC ID", "FREQ", "TITLE");
        System.out.println("--------------------------------------------------------------------------------");

        for (InvertedIndex.Posting p : results) {
            // Ambil judul dari dokumen asli berdasarkan ID
            String title = index.docTitles.getOrDefault(p.docID, "No Title"); 
            System.out.printf("[%03d]   |  %-4d | %s\n", p.docID, p.freq, title);
        }
        System.out.println("--------------------------------------------------------------------------------");
    }
    
    private static void printHeader() {
        System.out.println("==========================================================");
        System.out.println("           Simple Document Search Engine (IR)           ");
        System.out.println("==========================================================");
        System.out.println(" [System] Initializing Inverted Index...                ");
        System.out.println(" [System] Loading Documents...                          ");
        System.out.println("----------------------------------------------------------");
    }
}
