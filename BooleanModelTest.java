import java.util.ArrayList;
import java.util.HashSet;

/*
 * Catatan:
 * - Nilai Harapan Program di setiap test case disesuaikan dengan dataset yang diberikan (doc_1.txt s/d doc_10.txt) Bukan keseluruhan corpus!
 */
public class BooleanModelTest {

    // Helper: Mencetak isi list ke terminal dan membandingkan dengan harapan
    public static void printResult(String kueri, String harapan, ArrayList<InvertedIndex.Posting> result) {
        System.out.println("Kueri: " + kueri);
        System.out.println("↳ Harapan Program: " + harapan);
        System.out.print("↳ Hasil Eksekusi : [");
        if (result != null) {
            for (int i = 0; i < result.size(); i++) {
                System.out.print(result.get(i).docID);
                if (i < result.size() - 1)
                    System.out.print(", ");
            }
        }
        System.out.println("]\n");
    }

    // Helper untuk memastikan data yang diambil dari Index aman dan TERURUT
    private static ArrayList<InvertedIndex.Posting> getSortedPostings(InvertedIndex index, String term) {
        ArrayList<InvertedIndex.Posting> raw = index.getPostings(term);
        if (raw == null) {
            return new ArrayList<>();
        }
        // Buat salinan agar index asli tidak berubah
        ArrayList<InvertedIndex.Posting> sorted = new ArrayList<>(raw);
        // Wajib diurutkan untuk BooleanModel
        sorted.sort((p1, p2) -> Integer.compare(p1.docID, p2.docID));
        return sorted;
    }

    // Helper: Mengambil seluruh dokumen (Universal Set) secara dinamis dari
    // Inverted Index
    private static ArrayList<InvertedIndex.Posting> getAllDocuments(InvertedIndex index) {
        HashSet<Integer> uniqueDocs = new HashSet<>();
        for (String term : index.getKeySet()) {
            ArrayList<InvertedIndex.Posting> postings = index.getPostings(term);
            if (postings != null) {
                for (InvertedIndex.Posting p : postings) {
                    uniqueDocs.add(p.docID);
                }
            }
        }
        ArrayList<InvertedIndex.Posting> allDocs = new ArrayList<>();
        for (Integer id : uniqueDocs) {
            allDocs.add(new InvertedIndex.Posting(id));
        }
        // Wajib disortir ( Algoritma Two-Pointer )
        allDocs.sort((p1, p2) -> Integer.compare(p1.docID, p2.docID));
        return allDocs;
    }

    public static void main(String[] args) {
        System.out.println("=== PENGUJIAN BOOLEAN MODEL (DATASET AERODINAMIKA) ===\n");

        // 1. INISIALISASI & LOAD INVERTED INDEX
        // Pastikan Anda membuat objek InvertedIndex dan memuat dokumen doc_1.txt s/d
        // doc_10.txt
        InvertedIndex index = new InvertedIndex();
        // Indexer.buildIndex(index); // <-- Asumsikan ini cara program Anda meload 10
        // dokumen teks

        // 2. DAPATKAN SEMUA DOKUMEN & POSTING LIST
        ArrayList<InvertedIndex.Posting> allDocs = getAllDocuments(index);

        // Gunakan fungsi helper getSortedPostings dan gunakan kata yang sudah di-stem!
        ArrayList<InvertedIndex.Posting> flow = getSortedPostings(index, "flow");
        ArrayList<InvertedIndex.Posting> boundary = getSortedPostings(index, "boundari"); // Porter Stemmer mengubah y
                                                                                          // -> i
        ArrayList<InvertedIndex.Posting> heat = getSortedPostings(index, "heat");
        ArrayList<InvertedIndex.Posting> mach = getSortedPostings(index, "mach");

        // ====================================================================

        System.out.println("--- 1. TEST AND ---");
        ArrayList<InvertedIndex.Posting> resAnd = BooleanModel.andOpt(flow, boundary);
        printResult("flow AND boundary", "[1, 2, 3, 4, 7, 9]", resAnd);

        // ====================================================================

        System.out.println("--- 2. TEST OR ---");
        ArrayList<InvertedIndex.Posting> resOr = BooleanModel.orOpt(heat, mach);
        printResult("heat OR mach", "[5, 6, 7, 9, 10]", resOr);

        // ====================================================================

        System.out.println("--- 3. TEST NOT (Unary) ---");
        ArrayList<InvertedIndex.Posting> resNot = BooleanModel.notOpt(allDocs, heat);
        printResult("NOT heat", "[1, 2, 3, 4, 7, 8, 9, 10]", resNot);

        // ====================================================================

        System.out.println("--- 4. TEST AND NOT ---");
        ArrayList<InvertedIndex.Posting> tempNotBoundary = BooleanModel.notOpt(allDocs, boundary);
        ArrayList<InvertedIndex.Posting> resAndNot = BooleanModel.andOpt(flow, tempNotBoundary);
        printResult("flow AND NOT boundary", "[6]", resAndNot);

        // ====================================================================

        System.out.println("--- 5. TEST KURUNG () ---");
        ArrayList<InvertedIndex.Posting> tempKurungOr = BooleanModel.orOpt(heat, mach);
        ArrayList<InvertedIndex.Posting> resKurung = BooleanModel.andOpt(tempKurungOr, flow);
        printResult("(heat OR mach) AND flow", "[6, 7, 9]", resKurung);

        // ====================================================================

        System.out.println("--- 6. TEST GABUNGAN KOMPLEKS ---");
        ArrayList<InvertedIndex.Posting> step1 = BooleanModel.orOpt(flow, heat);
        ArrayList<InvertedIndex.Posting> step2 = BooleanModel.andOpt(boundary, mach);
        ArrayList<InvertedIndex.Posting> step3 = BooleanModel.notOpt(allDocs, step2);
        ArrayList<InvertedIndex.Posting> finalRes = BooleanModel.andOpt(step1, step3);

        printResult("(flow OR heat) AND NOT (boundary AND mach)", "[1, 2, 3, 4, 5, 6]", finalRes);
    }
}