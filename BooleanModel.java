import java.util.ArrayList;

public class BooleanModel {
    public ArrayList<InvertedIndex.Posting> andOpt(
        ArrayList<InvertedIndex.Posting> list1, 
        ArrayList<InvertedIndex.Posting> list2){
        
        ArrayList<InvertedIndex.Posting> result = new ArrayList<>();

        if (list1 == null || list2 == null || list1.isEmpty() || list2.isEmpty()) {
            return result;
        }

        int i = 0, j = 0;
        while (i < list1.size() && j < list2.size()) {
            InvertedIndex.Posting posting1 = list1.get(i);
            InvertedIndex.Posting posting2 = list2.get(j);

            if (posting1.docID == posting2.docID) {
                result.add(posting1);
                i++;
                j++;
            } else if (posting1.docID < posting2.docID) {
                i++;
            } else {
                j++;
            }
        }
        return result;
    }

    public ArrayList<InvertedIndex.Posting> orOpt(
        ArrayList<InvertedIndex.Posting> list1, 
        ArrayList<InvertedIndex.Posting> list2){
        
        ArrayList<InvertedIndex.Posting> result = new ArrayList<>();

        if (list1 == null || list1.isEmpty()) {
            return list2 != null ? new ArrayList<>(list2) : result;
        }
        if (list2 == null || list2.isEmpty()) {
            return new ArrayList<>(list1);
        }

        int i = 0, j = 0;
        while (i < list1.size() && j < list2.size()) {
            InvertedIndex.Posting posting1 = list1.get(i);
            InvertedIndex.Posting posting2 = list2.get(j);

            if (posting1.docID == posting2.docID) {
                result.add(posting1);
                i++;
                j++;
            } else if (posting1.docID < posting2.docID) {
                result.add(posting1);
                i++;
            } else {
                result.add(posting2);
                j++;
            }
        }

        // Tambahkan sisa dari list yang belum habis
        while (i < list1.size()) {
            result.add(list1.get(i++));
        }
        while (j < list2.size()) {
            result.add(list2.get(j++));
        }

        return result;
    }

    public ArrayList<InvertedIndex.Posting> notOpt(
        ArrayList<InvertedIndex.Posting> list1, 
        ArrayList<InvertedIndex.Posting> list2){
        
        ArrayList<InvertedIndex.Posting> result = new ArrayList<>();

        if (list1 == null || list1.isEmpty()) {
            return result;
        }
        if (list2 == null || list2.isEmpty()) {
            return new ArrayList<>(list1);
        }

        int i = 0, j = 0;
        while (i < list1.size() && j < list2.size()) {
            InvertedIndex.Posting posting1 = list1.get(i);
            InvertedIndex.Posting posting2 = list2.get(j);

            if (posting1.docID == posting2.docID) {
                i++;
                j++;
            } else if (posting1.docID < posting2.docID) {
                result.add(posting1);
                i++;
            } else {
                j++;
            }
        }

        while (i < list1.size()) {
            result.add(list1.get(i++));
        }

        return result;
    }
}
