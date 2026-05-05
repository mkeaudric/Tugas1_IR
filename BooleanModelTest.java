import java.util.ArrayList;

public class BooleanModelTest {

    // Helper: Fungsi untuk membuat dummy list dokumen (Posting) dengan cepat
    public static ArrayList<InvertedIndex.Posting> createPostingList(int... docIDs) {
        ArrayList<InvertedIndex.Posting> list = new ArrayList<>();
        for (int id : docIDs) {
            // Memanggil konstruktor Posting dari dalam class InvertedIndex
            list.add(new InvertedIndex.Posting(id));
        }
        return list;
    }

    // Helper: Fungsi untuk mencetak isi list ke terminal
    public static void printResult(String testName, ArrayList<InvertedIndex.Posting> result) {
        System.out.print(testName + " \n↳ Hasil Program: [");
        for (int i = 0; i < result.size(); i++) {
            System.out.print(result.get(i).docID);
            if (i < result.size() - 1) System.out.print(", ");
        }
        System.out.println("]\n");
    }

    public static void main(String[] args) {
        System.out.println("=== MEMULAI PENGUJIAN BOOLEAN MODEL ===\n");

        // 1. SIAPKAN DATA DUMMY (Berdasarkan index 10 dokumen kita)
        // flow       -> {2, 3, 4, 6, 7, 9}
        // boundary   -> {2, 3, 4, 7, 8, 9}
        // heat       -> {5, 6}
        // mach       -> {7, 9, 10}
        // plate      -> {2, 3, 4, 8, 9}
        // hypersonic -> {2, 9}
        
        ArrayList<InvertedIndex.Posting> flow = createPostingList(2, 3, 4, 6, 7, 9);
        ArrayList<InvertedIndex.Posting> boundary = createPostingList(2, 3, 4, 7, 8, 9);
        ArrayList<InvertedIndex.Posting> heat = createPostingList(5, 6);
        ArrayList<InvertedIndex.Posting> mach = createPostingList(7, 9, 10);
        ArrayList<InvertedIndex.Posting> plate = createPostingList(2, 3, 4, 8, 9);
        ArrayList<InvertedIndex.Posting> hypersonic = createPostingList(2, 9);
        ArrayList<InvertedIndex.Posting> emptyList = new ArrayList<>();

        // ====================================================================

        System.out.println("--- 1. TEST AND (Irisan) ---");
        // Logika: flow AND boundary -> Yang ada di kedua himpunan adalah 2, 3, 4, 7, 9
        ArrayList<InvertedIndex.Posting> resAnd = BooleanModel.andOpt(flow, boundary);
        System.out.println("Kueri: flow AND boundary (Harapan: [2, 3, 4, 7, 9])");
        printResult("Eksekusi", resAnd);

        // ====================================================================

        System.out.println("--- 2. TEST OR (Gabungan) ---");
        // Logika: heat OR mach -> Gabungan tanpa duplikat adalah 5, 6, 7, 9, 10
        ArrayList<InvertedIndex.Posting> resOr = BooleanModel.orOpt(heat, mach);
        System.out.println("Kueri: heat OR mach (Harapan: [5, 6, 7, 9, 10])");
        printResult("Eksekusi", resOr);

        // ====================================================================

        System.out.println("--- 3. TEST NOT (Selisih) ---");
        // Logika: plate NOT hypersonic -> Buang 2 dan 9 dari plate, sisa 3, 4, 8
        ArrayList<InvertedIndex.Posting> resNot = BooleanModel.notOpt(plate, hypersonic);
        System.out.println("Kueri: plate NOT hypersonic (Harapan: [3, 4, 8])");
        printResult("Eksekusi", resNot);

        // ====================================================================

        System.out.println("--- 4. TEST EDGE CASES (Kasus Ekstrem) ---");
        // Jika salah satu operand ternyata kosong (misal usernya typo mencari kata yang tidak ada)
        
        ArrayList<InvertedIndex.Posting> resEmptyAnd = BooleanModel.andOpt(flow, emptyList);
        System.out.println("Kueri: flow AND [KOSONG] (Harapan: [])");
        printResult("Eksekusi", resEmptyAnd);

        ArrayList<InvertedIndex.Posting> resEmptyOr = BooleanModel.orOpt(flow, emptyList);
        System.out.println("Kueri: flow OR [KOSONG] (Harapan: [2, 3, 4, 6, 7, 9])");
        printResult("Eksekusi", resEmptyOr);

        System.out.println("--- 5. TEST > 2 KATA (Berantai) ---");
        // Kueri: flow AND boundary AND plate
        // Logika Eksekusi Stack: (flow AND boundary) lalu di-AND-kan dengan plate
        // flow AND boundary = [2, 3, 4, 7, 9]
        // [2, 3, 4, 7, 9] AND plate([2, 3, 4, 8, 9]) = [2, 3, 4, 9]
        
        ArrayList<InvertedIndex.Posting> tempAnd = BooleanModel.andOpt(flow, boundary);
        ArrayList<InvertedIndex.Posting> resMultiAnd = BooleanModel.andOpt(tempAnd, plate);
        
        System.out.println("Kueri: flow AND boundary AND plate (Harapan: [2, 3, 4, 9])");
        printResult("Eksekusi", resMultiAnd);

        // ====================================================================

        System.out.println("--- 6. TEST KURUNG & NOT ---");
        // Kueri: (heat OR mach) AND NOT hypersonic
        // Logika Eksekusi: Kerjakan dalam kurung dulu (OR), lalu kurangi hasilnya dengan (NOT)
        // Langkah 1: heat OR mach = [5, 6, 7, 9, 10]
        // Langkah 2: Langkah 1 NOT hypersonic([2, 9]) = [5, 6, 7, 10]
        
        ArrayList<InvertedIndex.Posting> tempOr = BooleanModel.orOpt(heat, mach);
        ArrayList<InvertedIndex.Posting> resKurungNot = BooleanModel.notOpt(tempOr, hypersonic);
        
        System.out.println("Kueri: (heat OR mach) AND NOT hypersonic (Harapan: [5, 6, 7, 10])");
        printResult("Eksekusi", resKurungNot);

        // ====================================================================

        System.out.println("--- 7. TEST SANGAT KOMPLEKS (3 Operator + Kurung) ---");
        // Kueri: flow AND (plate OR heat) AND NOT boundary
        // Logika Manual:
        // 1. plate OR heat -> [2, 3, 4, 5, 6, 8, 9]
        // 2. flow AND (Hasil 1) -> [2, 3, 4, 6, 9]
        // 3. (Hasil 2) NOT boundary([2, 3, 4, 7, 8, 9]) -> Hapus 2, 3, 4, dan 9. Sisa [6].
        
        ArrayList<InvertedIndex.Posting> step1 = BooleanModel.orOpt(plate, heat);
        ArrayList<InvertedIndex.Posting> step2 = BooleanModel.andOpt(flow, step1);
        ArrayList<InvertedIndex.Posting> finalRes = BooleanModel.notOpt(step2, boundary);
        
        System.out.println("Kueri: flow AND (plate OR heat) AND NOT boundary (Harapan: [6])");
        printResult("Eksekusi", finalRes);
    }
}