import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;

// import EditDistance.editDistanceTable;

public class QueryEvaluator {

    private InvertedIndex index;
    private BooleanModel BM;
    private Stemmer stemmer;
    private SearchEngine engine;

    // Constructor menerima InvertedIndex dan BooleanModel
    public QueryEvaluator(InvertedIndex index, BooleanModel BM, SearchEngine engine) {
        this.index = index;
        this.BM = BM;
        this.stemmer = new PorterStemmer();
        this.engine = engine;
    }

    /* Tokenisasi dan konversi ke Postfix (Reverse Polish Notation)
    Menggunakan algoritma Shunting-yard yang disederhanakan */
    private List<String> infixToPostfix(String query) {
        List<String> postfix = new ArrayList<>();
        Stack<String> operatorStack = new Stack<>();

        // Ganti "AND NOT" dengan "NOT" untuk menyederhanakan parsing
        query = query.replaceAll("(?i)\\bAND\\s+NOT\\b", "NOT"); 

        // Pre-processing: Beri spasi pada tanda kurung agar mudah di-split
        query = query.replace("(", " ( ").replace(")", " ) ");
        //https://stackoverflow.com/questions/15625629/regex-expressions-in-java-s-vs-s
        String[] tokens = query.split("\\s+");


        //https://www-geeksforgeeks-org.translate.goog/java/java-program-to-implement-shunting-yard-algorithm/?_x_tr_sl=en&_x_tr_tl=id&_x_tr_hl=id&_x_tr_pto=tc&_x_tr_hist=true
        //Priority () > NOT > AND > OR
        //Proses secara sederhana jika term maka akan dimasukkan pada list postfix, jika operator maka akan dimasukkan ke stack operator
        //operator akan masuk kedalam list postfix jika operator baru yang masuk memiliki prioritas lebih rendah atau sama dengan operator
        // yang ada di stack, atau jika operator baru adalah tanda kurung tutup ")"
        for (String token : tokens) {
            if (token.isEmpty()) continue;

            String upperToken = token.toUpperCase(); // Standarisasi operator huruf besar

            if (upperToken.equals("AND") || upperToken.equals("OR") || upperToken.equals("NOT")) {
                // Keluarkan operator dari stack yang prioritasnya lebih tinggi atau sama
                while (!operatorStack.isEmpty() && precedence(operatorStack.peek()) >= precedence(upperToken)) {
                    postfix.add(operatorStack.pop());
                }
                operatorStack.push(upperToken);
            } else if (token.equals("(")) {
                while (!operatorStack.isEmpty() && !operatorStack.peek().equals("(")) {
                    postfix.add(operatorStack.pop());
                }
                if (!operatorStack.isEmpty()) {
                    operatorStack.pop(); 
                }
            } else if (token.equals(")")) {
                operatorStack.push("(");
            } else {
                // ini gw ubah
                String[] subTokens = token.split("[\\W_]+");
                int termCt = 0;

                for (String sub : subTokens) {
                    if (sub.isEmpty()) continue;
                    String lowerSub = sub.toLowerCase();
                    if (index.stopWords.contains(lowerSub)) continue;

                    // stemmingnya sebelum spelling correction
                    String stemmedTerm = stemmer.stem(lowerSub);
                    String finalTerm = engine.getCorrectedTerm(stemmedTerm);
                    postfix.add(finalTerm);
                    termCt++;

                    // masukin AND kalo token jd kepisah
                    if (termCt > 1) postfix.add("AND");
                }
            }
        }

        // Habiskan sisa operator di stack
        // masukkan ke list postfix jika masih ada operator yang tersisa di stack
        while (!operatorStack.isEmpty()) {
            postfix.add(operatorStack.pop());
        }

        return postfix;
    }

    // Menentukan prioritas operasi Boolean
    private int precedence(String operator) {
        switch (operator) {
            case "NOT": return 3; // Priority ke 1 NOT
            case "AND": return 2; // Priority ke 2 AND
            case "OR":  return 1; // Priority ke 3 OR
            default:    return 0; // Untuk tanda kurung "("
        }
    }

    public ArrayList<InvertedIndex.Posting> evaluate(String query) {
        List<String> postfix = infixToPostfix(query);
        Stack<ArrayList<InvertedIndex.Posting>> resultStack = new Stack<>();

        for (String token : postfix) {
            if (token.equals("AND") || token.equals("OR") || token.equals("NOT")) {
                // Pop 2 posting list terakhir untuk dievaluasi
                // Urutan pop: yang pertama di-pop adalah operand KANAN
                ArrayList<InvertedIndex.Posting> rightOperand = resultStack.isEmpty() ? new ArrayList<>()
                        : resultStack.pop();
                ArrayList<InvertedIndex.Posting> leftOperand = resultStack.isEmpty() ? new ArrayList<>()
                        : resultStack.pop();

                ArrayList<InvertedIndex.Posting> tempResult = new ArrayList<>();

                if (token.equals("AND")) {
                    tempResult = BooleanModel.andOpt(leftOperand, rightOperand);
                } else if (token.equals("OR")) {
                    tempResult = BM.orOpt(leftOperand, rightOperand);
                } else if (token.equals("NOT")) {
                    tempResult = BM.notOpt(leftOperand, rightOperand);
                    // System.out.println("Pushing to stack, size: " + tempResult.size());
                }

                // Push kembali hasil evaluasi ke dalam stack
                resultStack.push(tempResult);
            } else {
                // Jika token adalah TERM (kata), ambil Posting List-nya dari Inverted Index
                ArrayList<InvertedIndex.Posting> postings = index.getPostings(token);

                // Jika term tidak ada di korpus, getPostings() mungkin return null, 
                // kita standarisasi menjadi ArrayList kosong agar aman dari NullPointerException
                if (postings == null) {
                    postings = new ArrayList<>();
                }else{
                    postings.sort((p1, p2) -> Integer.compare(p1.docID, p2.docID));
                }

                resultStack.push(postings);
            }
        }

        // Hasil akhir adalah satu-satunya list yang tersisa di dalam stack
        ArrayList<InvertedIndex.Posting> finalResult = resultStack.isEmpty() ? new ArrayList<>() : resultStack.pop();
        finalResult.sort((p1, p2) -> Integer.compare(p1.docID, p2.docID));
        
        return finalResult;
    }
    
    // Untuk baca kunci jawaban dari file cranqrel100.txt 
    public HashMap<Integer, HashSet<Integer>> loadGroundTruth(String filePath) {
        // Key: ID Query, Value: Daftar ID Dokumen yang relevan
        HashMap<Integer, HashSet<Integer>> groundTruth = new HashMap<>();

        try (Scanner sc = new Scanner(new File(filePath))) {
            while (sc.hasNextLine()) {
                String line = sc.nextLine().trim();
                // Kalo barisnya kosong, biarin
                if (line.isEmpty()) {
                    continue;
                }

                // Format cranqrel: QueryID DocID Relevance
                // Pisahin berdasarkan spasi
                String[] parts = line.split("\\s+");
                int qID = Integer.parseInt(parts[0]);
                int dID = Integer.parseInt(parts[1]);

                // Kalo QueryID belom ada di map, buat hashSet baru 
                groundTruth.putIfAbsent(qID, new HashSet<>());
                // Masukin DocID ke hashSet sesuai dengan QueryID 
                groundTruth.get(qID).add(dID);
            }
        } catch (FileNotFoundException e) {
            System.out.println("File " + filePath + " tidak ditemukan!");
        }
        return groundTruth;
    }
    
    // Fungsi untuk menghitung metrik dengan nilai precision dan recall berdasarkan ID Query 
    public void calculateMetrics(int qID, ArrayList<InvertedIndex.Posting> results, HashMap<Integer, HashSet<Integer>> groundTruth) {
        // 1. Ambil dokumen yang 'benar' untuk query dari ground truth
        HashSet<Integer> relevantDocs = groundTruth.get(qID);
        
        // Kalo kosong, berarti query gaada yang sesuai kunci jawaban 
        if (relevantDocs == null) {
            System.out.println("Query ID " + qID + " tidak ada di ground truth.");
            return;
        }

        // 2. Hitung ada berapa dokumen yang ketemu (yang relevan) sesuai dengan QueryID
        int countCorrect = 0;
        for (InvertedIndex.Posting p : results) {
            // System.out.println("DEBUG: Memeriksa DocID dari sistem: " + p.docID);
            // Kalo DocID ada di dalam list dokumen yang relevan, maka tambahin countCorrect
            if (relevantDocs.contains(p.docID)) {
                countCorrect++;
            }
        }

        // Hitung metrik (Precision dan Recall)
        // Rumus: (Benar ditemukan / Total yang diberikan sistem)
        double precision = results.isEmpty() ? 0 : (double) countCorrect / results.size();
        // Rumus: (Benar ditemukan / Total yang seharusnya ditemukan)
        double recall = (double) countCorrect / relevantDocs.size();

        System.out.println("\n--- METRIK EVALUASI ---");
        System.out.println("Dokumen Relevan yang Ditemukan: " + countCorrect);
        System.out.println("Total Dokumen Seharusnya: " + relevantDocs.size());
        System.out.printf("Precision: %.2f%%\n", precision * 100);
        System.out.printf("Recall: %.2f%%\n", recall * 100);
    }
}