import java.util.ArrayList;

public class SearchEngine {
    private InvertedIndex index;
    private EditDistance levenshtein;

    public SearchEngine(InvertedIndex index) {
        this.index = index;
        this.levenshtein = new EditDistance();
    }
    
    public ArrayList<InvertedIndex.Posting> search(String query) {
        // 1. Lakukan tokenisasi query - ubah query menjadi lowercase dan split berdasarkan spasi
        String[] terms = query.toLowerCase().split("\\s+");

        // 2. Bersikan kata (term)
        // Buat arrayList untuk menyimpan term yang sudah dibersihkan
        ArrayList<String> cleanedTerms = new ArrayList<>();
        // untuk setiap term, periksa apakah ada di vocabulary
        // Jika tidak ada di vocabulary, cari term yang paling mirip dengan menggunakan edit distance 
        for (String term : terms) {
            if (!index.getKeySet().contains(term)) {
                String suggestion = levenshtein.findClosestTerm(term, index.getKeySet());
                cleanedTerms.add(suggestion);
                // Kalo ada di vocabulary, langsung maskukin ke cleanedTerms
            } else {
                cleanedTerms.add(term);
            }
        }
        
        // 3. proses query dengan boolean model (AND) buat ngegabungin semua term yang udah dibersihin
        ArrayList<InvertedIndex.Posting> finalResults = queryProcess(cleanedTerms);
    
        return finalResults;
    }

    public ArrayList<InvertedIndex.Posting> queryProcess(ArrayList<String> cleanedTerms) {
        if (cleanedTerms.isEmpty()) {
            return new ArrayList<>();
        }
        
        // 1. Ambil kata pertama dari cleandTerms
        String firstTerm = cleanedTerms.get(0);
        ArrayList<InvertedIndex.Posting> resultTemp = index.getPostings(firstTerm);

        // 2. Jika ada kata lagi, maka gabungkan dengan boolean model 
        for (int i = 1; i < cleanedTerms.size(); i++) {
            String nextTerm = cleanedTerms.get(i);

            // Ambil posting list berikutnya 
            ArrayList<InvertedIndex.Posting> nextPostings = index.getPostings(nextTerm);

            // Gabungkan resultTemp dengan nextPostings menggunakan boolean model (AND)
            resultTemp = BooleanModel.andOpt(resultTemp, nextPostings);
        }
        return resultTemp;
    }

    public String getCorrectedTerm(String term) {
        // Pakai logika yang sama dengan yang ada di loop search kamu
        if (!index.getKeySet().contains(term)) {
            return levenshtein.findClosestTerm(term, index.getKeySet());
        }
        return term;
    }
}
