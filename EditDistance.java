import java.util.Set;

public class EditDistance {

    // https://leetcode.com/problems/edit-distance/solutions/3230662/clean-codes-full-explanation-dynamic-pro-ytsr/
    // Menggunakan Dynamic Programming karena akan lebih cepat untuk menggitung edit distance 
    public int calculateLevenshtein(String s1, String s2) {
        int m = s1.length();
        int n = s2.length();
        
        // Buat tabel untuk menyimpan hasil perhitungan
        int[][] dp = new int[m + 1][n + 1];

        for (int i = 0; i <= m; i++) {
            for (int j = 0; j <= n; j++) {
                // Kalo s1 kosong, butuh j langkah (insert semua s2)
                if (i == 0) {
                    dp[i][j] = j;
                }
                
                // Kalo s2 kosong, butuh i langkah (hapus semua s1)
                else if (j == 0) {
                    dp[i][j] = i;
                }
                
                // Kalo karakter sama, tidak ada operasi apa pun  
                else if (s1.charAt(i - 1) == s2.charAt(j - 1)) {
                    dp[i][j] = dp[i - 1][j - 1];
                }
                
                // Kalo beda, pilih biaya minimum dari 3 operasi
                else {
                    dp[i][j] = 1 + Math.min(dp[i][j - 1],      // Insertion
                            Math.min(dp[i - 1][j],          // Deletion
                            dp[i - 1][j - 1]));    // Substitution
                }
            }
        }
        return dp[m][n];
    }
    
    // Untuk kalo user input query yang typo
    // https://stevehanov.ca/blog/fast-and-easy-levenshtein-distance-using-a-trie
    public String findClosestTerm(String term, Set<String> vocabulary){
        // Simpan term paling mirip dan jaraknya 
        String bestTerm = term; 
        int minDistance = Integer.MAX_VALUE;

        // Perika semua vocab di vocabulary dari inverted index yang sudah di-stem, untuk mencari yang paling mirip dengan term yang dimasukkan user
        for(String vocab : vocabulary){

            // Hitung edit distance 
            int distanceTable = calculateLevenshtein(term, vocab);

            // Cari yang distancenya paling kecil 
            if (distanceTable < minDistance){
                minDistance = distanceTable;
                bestTerm = vocab;
            }
        }

        if (minDistance > 3) {
            return term; // Jarak terlalu jauh, mungkin emang kata baru
        }

        // Balikin hasil kata yang distancenya paling kecil (paling mirip)
        return bestTerm;
    }
    
}
