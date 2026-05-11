import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;

// import EditDistance.editDistanceTable;

public class QueryEvaluator {

    private InvertedIndex index;
    private BooleanModel BM;
    private Stemmer stemmer;
    private EditDistance levenshtein;

    // Constructor menerima InvertedIndex dan BooleanModel
    public QueryEvaluator(InvertedIndex index, BooleanModel BM, EditDistance levenshtein) {
        this.index = index;
        this.BM = BM;
        this.stemmer = new PorterStemmer();
        this.levenshtein = levenshtein;
    }


    private List<String> infixToPostfix(String query) {
        List<String> postfix = new ArrayList<>();
        Stack<String> operatorStack = new Stack<>();

        // Pre-processing: Beri spasi pada tanda kurung agar mudah di-split
        query = query.replace("(", " ( ").replace(")", " ) ");
        //https://stackoverflow.com/questions/15625629/regex-expressions-in-java-s-vs-s
        String[] tokens = query.split("\\s+");

        /* Tokenisasi dan konversi ke Postfix (Reverse Polish Notation)
        Menggunakan algoritma Shunting-yard yang disederhanakan */
        List<String> cleanTokens = new ArrayList<>();
        for (String token : tokens) {
            if (token.isEmpty()) continue;

            String upperToken = token.toUpperCase();
            if (upperToken.equals("AND") || upperToken.equals("OR") || upperToken.equals("NOT") || token.equals("(") || token.equals(")")) {
                // Simpan operator dan tanda kurung langsung ke list bersih
                cleanTokens.add(upperToken.equals("(") || upperToken.equals(")") ? token : upperToken);
            } else {
                // Jika itu term, pisahkan jika ada karakter non-alfanumerik
                String[] subTokens = token.split("[\\W_]+");
                for (String sub : subTokens) {
                    if (sub.isEmpty()) continue;
                    String lowerSub = sub.toLowerCase();
                    
                    // Cek stopword
                    if (index.stopWords.contains(lowerSub)) continue;

                    // Stemming & Edit distance
                    String stemmedTerm = stemmer.stem(lowerSub);
                    String finalTerm = levenshtein.findClosestTerm(stemmedTerm, index.getKeySet());
                    cleanTokens.add(finalTerm);

                    System.out.println(finalTerm);
                }
            }
        }

        // masukin AND kalo token jd kepisah
        List<String> processedTokens = new ArrayList<>();
        for (int i = 0; i < cleanTokens.size(); i++) {
            String current = cleanTokens.get(i);

            if (i > 0) {
                String prev = cleanTokens.get(i - 1);
                
                // Syarat menyisipkan AND:
                // 1. Token SEBELUMNYA adalah Term atau ")"
                boolean isPrevTermOrCloseParen = !prev.equals("AND") && !prev.equals("OR") && !prev.equals("NOT") && !prev.equals("(");
                
                // 2. Token SAAT INI adalah Term, "(", ATAU "NOT" (Ini diubah agar mengizinkan NOT)
                boolean isCurrTermOrOpenParenOrNot = !current.equals("AND") && !current.equals("OR") && !current.equals(")");

                // Jika kedua syarat terpenuhi, sisipkan "AND"
                if (isPrevTermOrCloseParen && isCurrTermOrOpenParenOrNot) {
                    processedTokens.add("AND");
                }
            }
            processedTokens.add(current);
        }

        //https://www-geeksforgeeks-org.translate.goog/java/java-program-to-implement-shunting-yard-algorithm/?_x_tr_sl=en&_x_tr_tl=id&_x_tr_hl=id&_x_tr_pto=tc&_x_tr_hist=true
        //Priority () > NOT > AND > OR
        //Proses secara sederhana jika term maka akan dimasukkan pada list postfix, jika operator maka akan dimasukkan ke stack operator
        //operator akan masuk kedalam list postfix jika operator baru yang masuk memiliki prioritas lebih rendah atau sama dengan operator
        // yang ada di stack, atau jika operator baru adalah tanda kurung tutup ")"
        for (String token : processedTokens) {
            if (token.equals("AND") || token.equals("OR") || token.equals("NOT")) {
                while (!operatorStack.isEmpty() && precedence(operatorStack.peek()) >= precedence(token)) {
                    postfix.add(operatorStack.pop());
                }
                operatorStack.push(token);
            } else if (token.equals(")")) {
                while (!operatorStack.isEmpty() && !operatorStack.peek().equals("(")) {
                    postfix.add(operatorStack.pop());
                }
                if (!operatorStack.isEmpty()) {
                    operatorStack.pop();
                }
            } else if (token.equals("(")) {
                operatorStack.push("(");
            } else {
                postfix.add(token);
            }
        }

        // Habiskan sisa operator di stack
        while (!operatorStack.isEmpty()) {
            postfix.add(operatorStack.pop());
        }

        return postfix;
    }
    
    // Menentukan prioriti operasi Boolean
    private int precedence(String operator) {
        switch (operator) {
            case "NOT": return 3; // Prioriti ke-1 (Tertinggi)
            case "AND": return 2; // Prioriti ke-2
            case "OR":  return 1; // Prioriti ke-3
            default:    return 0; // Untuk tanda kurung "("
        }
    }

    // Helper untuk mendapatkan seluruh dokumen di corpus (Universal Set)
    private ArrayList<InvertedIndex.Posting> getAllDocuments() {
        HashSet<Integer> uniqueDocs = new HashSet<>();
        
        // Kumpulkan semua docID unik dari seluruh terms yang ada di Inverted Index
        for (String term : index.getKeySet()) {
            ArrayList<InvertedIndex.Posting> postings = index.getPostings(term);
            if (postings != null) {
                for (InvertedIndex.Posting p : postings) {
                    uniqueDocs.add(p.docID);
                }
            }
        }

        // Ubah kembali menjadi format ArrayList<Posting> agar sesuai dengan BooleanModel
        ArrayList<InvertedIndex.Posting> allDocs = new ArrayList<>();
        for (Integer id : uniqueDocs) {
            allDocs.add(new InvertedIndex.Posting(id));
        }
        
        // Pastikan terurut (Sorting sangat penting untuk fungsi andOpt/orOpt/notOpt Anda)
        allDocs.sort((p1, p2) -> Integer.compare(p1.docID, p2.docID));
        return allDocs;
    }

    public ArrayList<InvertedIndex.Posting> evaluate(String query) {
        List<String> postfix = infixToPostfix(query);
        Stack<ArrayList<InvertedIndex.Posting>> resultStack = new Stack<>();

        for (String token : postfix) {
            if (token.equals("AND") || token.equals("OR") || token.equals("NOT")) {
                if (token.equals("NOT")) {
                    // Hanya pop 1 kali untuk operand kanan
                    ArrayList<InvertedIndex.Posting> rightOperand = resultStack.isEmpty() ? new ArrayList<>() : resultStack.pop();
                    
                    // Lakukan operasi: Seluruh Dokumen (Universal Set) - Operand Kanan
                    ArrayList<InvertedIndex.Posting> allDocs = getAllDocuments();
                    resultStack.push(BooleanModel.notOpt(allDocs, rightOperand));
                }else {
                    // AND dan OR MEMBUTUHKAN DUA OPERAND (BINARY)
                    // Pop 2 posting list terakhir untuk dievaluasi
                    // Urutan pop: yang pertama di-pop adalah operand KANAN
                    ArrayList<InvertedIndex.Posting> rightOperand = resultStack.isEmpty() ? new ArrayList<>() : resultStack.pop();
                    ArrayList<InvertedIndex.Posting> leftOperand = resultStack.isEmpty() ? new ArrayList<>() : resultStack.pop();
                    
                    if (token.equals("AND")) {
                        resultStack.push(BooleanModel.andOpt(leftOperand, rightOperand));
                    } else if (token.equals("OR")) {
                        resultStack.push(BooleanModel.orOpt(leftOperand, rightOperand));
                    }
                }
                
            } else {
                // Jika token adalah TERM (kata), ambil Posting List-nya dari Inverted Index
                ArrayList<InvertedIndex.Posting> postings = index.getPostings(token);

                // Jika term tidak ada di korpus, getPostings() mungkin return null, 
                // kita standarisasi menjadi ArrayList kosong agar aman dari NullPointerException
                if (postings == null) {
                    postings = new ArrayList<>();
                }else{
                    // Pastikan posting list terurut berdasarkan docID untuk operasi Boolean yang efisien
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
    // public HashMap<Integer, HashSet<Integer>> loadGroundTruth(String filePath) {
    //     // Key: ID Query, Value: Daftar ID Dokumen yang relevan
    //     HashMap<Integer, HashSet<Integer>> groundTruth = new HashMap<>();

    //     try (Scanner sc = new Scanner(new File(filePath))) {
    //         while (sc.hasNextLine()) {
    //             String line = sc.nextLine().trim();
    //             // Kalo barisnya kosong, biarin
    //             if (line.isEmpty()) {
    //                 continue;
    //             }

    //             // Format cranqrel: QueryID DocID Relevance
    //             // Pisahin berdasarkan spasi
    //             String[] parts = line.split("\\s+");
    //             int qID = Integer.parseInt(parts[0]);
    //             int dID = Integer.parseInt(parts[1]);

    //             // Kalo QueryID belom ada di map, buat hashSet baru 
    //             groundTruth.putIfAbsent(qID, new HashSet<>());
    //             // Masukin DocID ke hashSet sesuai dengan QueryID 
    //             groundTruth.get(qID).add(dID);
    //         }
    //     } catch (FileNotFoundException e) {
    //         System.out.println("File " + filePath + " tidak ditemukan!");
    //     }
    //     return groundTruth;
    // }
    
    // // Fungsi untuk menghitung metrik dengan nilai precision dan recall berdasarkan ID Query 
    // public void calculateMetrics(int qID, ArrayList<InvertedIndex.Posting> results,
    //         HashMap<Integer, HashSet<Integer>> groundTruth) {
    //     // 1. Ambil dokumen yang 'benar' untuk query dari ground truth
    //     HashSet<Integer> relevantDocs = groundTruth.get(qID);

    //     // Kalo kosong, berarti query gaada yang sesuai kunci jawaban 
    //     if (relevantDocs == null) {
    //         System.out.println("Query ID " + qID + " tidak ada di ground truth.");
    //         return;
    //     }

    //     // 2. Hitung ada berapa dokumen yang ketemu (yang relevan) sesuai dengan QueryID
    //     int countCorrect = 0;
    //     for (InvertedIndex.Posting p : results) {
    //         // System.out.println("DEBUG: Memeriksa DocID dari sistem: " + p.docID);
    //         // Kalo DocID ada di dalam list dokumen yang relevan, maka tambahin countCorrect
    //         if (relevantDocs.contains(p.docID)) {
    //             countCorrect++;
    //         }
    //     }

    //     // Hitung metrik (Precision dan Recall)
    //     // Rumus: (Benar ditemukan / Total yang diberikan sistem)
    //     double precision = results.isEmpty() ? 0 : (double) countCorrect / results.size();
    //     // Rumus: (Benar ditemukan / Total yang seharusnya ditemukan)
    //     double recall = (double) countCorrect / relevantDocs.size();

    //     System.out.println("\n--- METRIK EVALUASI ---");
    //     System.out.println("Dokumen Relevan yang Ditemukan: " + countCorrect);
    //     System.out.println("Total Dokumen Seharusnya: " + relevantDocs.size());
    //     System.out.printf("Precision: %.2f%%\n", precision * 100);
    //     System.out.printf("Recall: %.2f%%\n", recall * 100);
    // }
}