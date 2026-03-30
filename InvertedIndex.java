// udah ada : 
// - tokenisasi, stopword removal, stemming (porter stemmer)
// - inverted index (hasil outputnya ga disimpen jd harus dijalanin berulang kali)

// list stopword dari : https://github.com/stopwords-iso/stopwords-en/blob/master/raw/snowball-tartarus.txt

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Scanner;
import java.util.Set;

public class InvertedIndex {
    // mau nyimpen docID + frekuensi + indeks kemunculan kata / offset (gaperlu offset sih tp keknya bagus dipake)
    // ppt 3 bagian Positional Index
    private static class Posting {
        int docID;
        int freq;
        ArrayList<Integer> offset;
        
        Posting(int docID) {
            this.docID = docID;
            freq = 0;
            offset = new ArrayList<Integer>();
        }

        void addTerm(int idx){
            offset.add(idx);
            freq++;
        }
    }

    // map buat term -> posting
    // tadinya mau pake LinkedList biar kek di ppt
    // https://stackoverflow.com/questions/10656471/performance-differences-between-arraylist-and-linkedlist
    // tapi baca ini keknya bagusan pake ArrayList, krn keknya ga bakal insert delete segala?
    private HashMap<String, ArrayList<Posting>> invertedIndex;
    
    private Stemmer ps;
    
    // HashSet buat stopwords, pake set biar retrieve O(1)
    // https://stackoverflow.com/questions/30944320/java-most-efficient-structure-for-quick-retrieval
    HashSet<String> stopWords;

    public InvertedIndex(){
        this.invertedIndex = new HashMap<>();
        this.ps = new PorterStemmer();
        this.stopWords  = new HashSet<>();

        // temp buat 3, 4
        Scanner sc = null;
        String str;

        try {
            // sc = new Scanner(new File(docPath.get(i)));
            sc = new Scanner(new File("stopwords.txt"));
            String sw;
            while(sc.hasNext()) {
                sw = sc.next().replaceAll("[\\W]|_", "").toLowerCase(); // bersihin
                if(!sw.isEmpty()) stopWords.add(sw); 
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        File dir = new File("corpus");
        File[] docList = dir.listFiles();
        if(docList != null){
            // int i = 0;
            // loop file corpus : https://stackoverflow.com/questions/4917326/how-to-iterate-over-the-files-of-a-certain-directory-in-java
            // gw pikir biar efisien loop tiap dokumen, tokenisasi + stemming, lgs catet si freq & indeks kemunculan lalu masukin ke map
            for(File doc : docList){
                // 1. ambil docID
                // docPath.putIfAbsent(i, doc.getAbsolutePath());
                String fileName = doc.getName();
                // https://stackoverflow.com/questions/14974033/extract-digits-from-string-stringutils-java
                int docID = Integer.parseInt(fileName.replaceAll("[^0-9]", ""));

                // 2. tokenisasi
                // https://stackoverflow.com/questions/34261466/how-to-extract-only-words-from-a-txt-file-in-java
                try {
                    // sc = new Scanner(new File(docPath.get(i)));
                    sc = new Scanner(doc);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }

                int j = 0; // buat offset kemunculan term
                while(sc.hasNext()){
                    // 3. bersihin & stemming
                    // https://stackoverflow.com/questions/1805518/replacing-all-non-alphanumeric-characters-with-empty-strings + lower case
                    str = sc.next().replaceAll("[\\W]|_", "").toLowerCase();
                    if(str.isEmpty() || stopWords.contains(str)) {
                        j++; // offset tetep naik
                        continue; 
                    }
                    str = ps.stem(str);

                    // 4. masukin ke dictionary terms
                    if(invertedIndex.get(str) == null){ // kalo term baru
                        ArrayList<Posting> newList = new ArrayList<>(); // list baru
                        Posting newPosting = new Posting(docID); // posting (doc i) baru
                        newPosting.addTerm(j); // tambahin term (freq otomatis ++)
                        newList.add(newPosting);
                        invertedIndex.put(str, newList);
                    } else{
                        ArrayList<Posting> curList = invertedIndex.get(str);
                        Posting lastPosting = curList.get(curList.size() - 1); // posting terakhir

                        // kalo lastPosting itu doc skrg (doc i)
                        if(lastPosting.docID == docID){
                            lastPosting.addTerm(j); // tgl tambahain offset
                        } else { // kalo bukan doc skrg
                            Posting newPosting = new Posting(docID);
                            newPosting.addTerm(j);
                            curList.add(newPosting);
                        }
                    }
                    j++;
                }
                // i++;
            }
        }
    }

    public Set<String> getKeySet(){
        return this.invertedIndex.keySet();
    }

    public ArrayList<Posting> getPostings(String key){
        return this.invertedIndex.get(key);
    }

    // public static void main(String[] args) {
    //     InvertedIndex invertedIndex = new InvertedIndex();

    //     // debug (nyamain ppt)
    //     for(String key : invertedIndex.getKeySet()){
    //         ArrayList<Posting> curList = invertedIndex.getPostings(key);
    //         System.out.println(key + " :");
    //         for(Posting post : curList){
    //             System.out.print("<" + post.docID + ", " + post.freq + ": <");
    //             int i = 0;
    //             for(Integer o : post.offset){
    //                 System.out.print(o);
    //                 if(i++ < post.freq - 1) System.out.print(", "); 
    //             }
    //             System.out.println(">;");
    //         }
    //         System.out.println();
    //     }
    // }
}