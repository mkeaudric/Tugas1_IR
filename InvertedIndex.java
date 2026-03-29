// udah ada buat ngasih docID, tokenisasi & stem, sama bikin inverted index

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;

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

    public static void main(String[] args) {
        // map buat docID -> path si file nya
        // HashMap<Integer, String> docPath = new HashMap<>(); 
        // gajadi dipake, cran pas diekstrak di corpus udah bentuk nya doc_{docID}.txt

        // map buat term -> posting
        // tadinya mau pake LinkedList biar kek di ppt
        // https://stackoverflow.com/questions/10656471/performance-differences-between-arraylist-and-linkedlist
        // tapi baca ini keknya bagusan pake ArrayList, krn keknya ga bakal insert delete segala?
        HashMap<String, ArrayList<Posting>> invertedIndex = new HashMap<>();

        Stemmer ps = new PorterStemmer();

        // temp buat 3, 4
        Scanner sc = null;
        String str;

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
                    if(str.isEmpty()) continue; // kalo kosong skip
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

        // debug (nyamain ppt)
        for(String key : invertedIndex.keySet()){
            ArrayList<Posting> curList = invertedIndex.get(key);
            System.out.println(key + " :");
            for(Posting post : curList){
                System.out.print("<" + post.docID + ", " + post.freq + ": <");
                int i = 0;
                for(Integer o : post.offset){
                    System.out.print(o);
                    if(i++ < post.freq - 1) System.out.print(", "); 
                }
                System.out.println(">;");
            }
            System.out.println(">");
        }
    }
    
}