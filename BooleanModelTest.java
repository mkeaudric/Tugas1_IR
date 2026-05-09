import java.util.ArrayList;

public class BooleanModelTest {

    // Helper: Fungsi untuk membuat dummy list dokumen (Posting) dengan cepat
    public static ArrayList<InvertedIndex.Posting> createPostingList(int... docIDs) {
        ArrayList<InvertedIndex.Posting> list = new ArrayList<>();
        for (int id : docIDs) {
            // Memastikan data terurut karena list harus sorted untuk algoritma two-pointer
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
        System.out.println("=== MEMULAI PENGUJIAN BOOLEAN MODEL (UPDATE UNARY NOT) ===\n");

        // 1. SIAPKAN DATA DUMMY
        // Asumsikan total dokumen di korpus kita ada 10 (docID 1 sampai 10)
        ArrayList<InvertedIndex.Posting> allDocs = createPostingList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
        
        // Data Term
        ArrayList<InvertedIndex.Posting> flow = createPostingList(2, 3, 4, 6, 7, 9);
        ArrayList<InvertedIndex.Posting> boundary = createPostingList(2, 3, 4, 7, 8, 9);
        ArrayList<InvertedIndex.Posting> heat = createPostingList(5, 6);
        ArrayList<InvertedIndex.Posting> mach = createPostingList(7, 9, 10);
        
        // ====================================================================

        System.out.println("--- 1. TEST AND ---");
        // Kueri: flow AND boundary
        ArrayList<InvertedIndex.Posting> resAnd = BooleanModel.andOpt(flow, boundary);
        System.out.println("Kueri: flow AND boundary (Harapan: [2, 3, 4, 7, 9])");
        printResult("Eksekusi", resAnd);

        // ====================================================================

        System.out.println("--- 2. TEST OR ---");
        // Kueri: heat OR mach
        ArrayList<InvertedIndex.Posting> resOr = BooleanModel.orOpt(heat, mach);
        System.out.println("Kueri: heat OR mach (Harapan: [5, 6, 7, 9, 10])");
        printResult("Eksekusi", resOr);

        // ====================================================================

        System.out.println("--- 3. TEST NOT (Unary) ---");
        // Kueri: NOT heat
        // Logika: allDocs - heat -> Buang 5 dan 6 dari 1-10
        ArrayList<InvertedIndex.Posting> resNot = BooleanModel.notOpt(allDocs, heat);
        System.out.println("Kueri: NOT heat (Harapan: [1, 2, 3, 4, 7, 8, 9, 10])");
        printResult("Eksekusi", resNot);

        // ====================================================================

        System.out.println("--- 4. TEST AND NOT ---");
        // Kueri: flow AND NOT boundary
        // Logika Stack: NOT boundary -> [1, 5, 6, 10]
        //               flow AND [1, 5, 6, 10] -> [6]
        ArrayList<InvertedIndex.Posting> tempNotBoundary = BooleanModel.notOpt(allDocs, boundary);
        ArrayList<InvertedIndex.Posting> resAndNot = BooleanModel.andOpt(flow, tempNotBoundary);
        
        System.out.println("Kueri: flow AND NOT boundary (Harapan: [6])");
        printResult("Eksekusi", resAndNot);

        // ====================================================================

        System.out.println("--- 5. TEST OR NOT ---");
        // Kueri: heat OR NOT flow
        // Logika Stack: NOT flow -> [1, 5, 8, 10]
        //               heat([5, 6]) OR [1, 5, 8, 10] -> [1, 5, 6, 8, 10]
        ArrayList<InvertedIndex.Posting> tempNotFlow = BooleanModel.notOpt(allDocs, flow);
        ArrayList<InvertedIndex.Posting> resOrNot = BooleanModel.orOpt(heat, tempNotFlow);
        
        System.out.println("Kueri: heat OR NOT flow (Harapan: [1, 5, 6, 8, 10])");
        printResult("Eksekusi", resOrNot);

        // ====================================================================

        System.out.println("--- 6. TEST KURUNG () ---");
        // Kueri: (heat OR mach) AND flow
        // Logika Stack: heat OR mach -> [5, 6, 7, 9, 10]
        //               [5, 6, 7, 9, 10] AND flow([2, 3, 4, 6, 7, 9]) -> [6, 7, 9]
        ArrayList<InvertedIndex.Posting> tempKurungOr = BooleanModel.orOpt(heat, mach);
        ArrayList<InvertedIndex.Posting> resKurung = BooleanModel.andOpt(tempKurungOr, flow);
        
        System.out.println("Kueri: (heat OR mach) AND flow (Harapan: [6, 7, 9])");
        printResult("Eksekusi", resKurung);

        // ====================================================================

        System.out.println("--- 7. TEST GABUNGAN KOMPLEKS ---");
        // Kueri: (flow OR heat) AND NOT (boundary AND mach)
        // Logika: 
        // 1. flow OR heat -> [2, 3, 4, 5, 6, 7, 9]
        // 2. boundary AND mach -> [7, 9]
        // 3. NOT (Hasil 2) -> allDocs - [7, 9] -> [1, 2, 3, 4, 5, 6, 8, 10]
        // 4. (Hasil 1) AND (Hasil 3) -> [2, 3, 4, 5, 6]
        
        ArrayList<InvertedIndex.Posting> step1 = BooleanModel.orOpt(flow, heat);
        ArrayList<InvertedIndex.Posting> step2 = BooleanModel.andOpt(boundary, mach);
        ArrayList<InvertedIndex.Posting> step3 = BooleanModel.notOpt(allDocs, step2); // Unary NOT
        ArrayList<InvertedIndex.Posting> finalRes = BooleanModel.andOpt(step1, step3);
        
        System.out.println("Kueri: (flow OR heat) AND NOT (boundary AND mach) (Harapan: [2, 3, 4, 5, 6])");
        printResult("Eksekusi", finalRes);
    }
}