import java.util.*;

public class QueryEvaluator {

    private InvertedIndex index;
    private BooleanModel BM;
    private Stemmer stemmer;

    // Constructor menerima InvertedIndex dan BooleanModel
    public QueryEvaluator(InvertedIndex index, BooleanModel BM) {
        this.index = index;
        this.BM = BM;
        this.stemmer = new PorterStemmer();
    }

    /* Tokenisasi dan konversi ke Postfix (Reverse Polish Notation)
    Menggunakan algoritma Shunting-yard yang disederhanakan */
    private List<String> infixToPostfix(String query) {
        List<String> postfix = new ArrayList<>();
        Stack<String> operatorStack = new Stack<>();

        // Pre-processing: Beri spasi pada tanda kurung agar mudah di-split
        query = query.replace("(", " ( ").replace(")", " ) ");
        //https://stackoverflow.com/questions/15625629/regex-expressions-in-java-s-vs-s
        String[] tokens = query.split("\\s+");


        //https://www-geeksforgeeks-org.translate.goog/java/java-program-to-implement-shunting-yard-algorithm/?_x_tr_sl=en&_x_tr_tl=id&_x_tr_hl=id&_x_tr_pto=tc&_x_tr_hist=true
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
                operatorStack.push("(");
            } else if (token.equals(")")) {
                // Keluarkan semua operator sampai ketemu kurung buka
                while (!operatorStack.isEmpty() && !operatorStack.peek().equals("(")) {
                    postfix.add(operatorStack.pop());
                }
                if (!operatorStack.isEmpty()) {
                    operatorStack.pop(); // Buang kurung buka "("
                }
            } else {
                // https://stackoverflow.com/questions/1805518/replacing-all-non-alphanumeric-characters-with-empty-strings + lower case
                String str = token.replaceAll("[\\W]|_", "").toLowerCase();
                
                if (!str.isEmpty()) {
                    String stemmedTerm = stemmer.stem(str);
                    postfix.add(stemmedTerm); // Masukkan term yang sudah di-stem ke postfix
                }
            }
        }

        // Habiskan sisa operator di stack
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
                ArrayList<InvertedIndex.Posting> rightOperand = resultStack.isEmpty() ? new ArrayList<>() : resultStack.pop();
                ArrayList<InvertedIndex.Posting> leftOperand = resultStack.isEmpty() ? new ArrayList<>() : resultStack.pop();

                ArrayList<InvertedIndex.Posting> tempResult = new ArrayList<>();

                if (token.equals("AND")) {
                    tempResult = BM.andOpt(leftOperand, rightOperand);
                } else if (token.equals("OR")) {
                    tempResult = BM.orOpt(leftOperand, rightOperand);
                } else if (token.equals("NOT")) {
                    tempResult = BM.notOpt(leftOperand, rightOperand);
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
                }
                
                resultStack.push(postings);
            }
        }

        // Hasil akhir adalah satu-satunya list yang tersisa di dalam stack
        return resultStack.isEmpty() ? new ArrayList<>() : resultStack.pop();
    }
}