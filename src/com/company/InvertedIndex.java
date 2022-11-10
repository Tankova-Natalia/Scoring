package com.company;

import org.jsoup.Jsoup;

import org.jsoup.nodes.Document;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.Comparator;

class Term {
    LinkedList<TermDocument> list = new LinkedList<>();
    int termFrequencyCollection;

    public Term(int docID) {
        termFrequencyCollection = 1;
        TermDocument termDocument = new TermDocument(docID);
        list.add(termDocument);
    }

    public void addDocument(int docID) {
        if (list.getLast().getDocID() == docID) {
            list.getLast().increaseFrequency();
        } else {
            TermDocument termDocument = new TermDocument(docID);
            list.add(termDocument);
        }
    }

    public void computeTfIdf(double idf) {
        Iterator<TermDocument> iterator = list.iterator();
        while (iterator.hasNext()) {
            iterator.next().computeTfIdf(idf);
        }
    }

    public int getDocumentFrequency() {
        return list.size();
    }

    @Override
    public String toString() {
        return "Term{" +
                "list=" + list +
                ", termFrequencyCollection=" + termFrequencyCollection +
                '}';
    }

    public LinkedList<TermDocument> getList() {
        return list;
    }
}

class TermDocument {
    private int docID;
    private int tf;
    private double tf_idf;

    public TermDocument(int docID) {
        this.docID = docID;
        tf = 1;
    }

    public int getDocID() {
        return docID;
    }

    public void increaseFrequency() {
        tf++;
    }

    public void computeTfIdf(double idf) {
        tf_idf = (1 + Math.log10(tf)) * idf;
    }

    public double getTfIdf() {
        return tf_idf;
    }

    public String toString() {
        return "docID=" + docID +
                ", tf=" + tf +
                ", tf_idf=" + tf_idf;
    }
}

class DocumentRelevance {
    private int docID;
    private double relevance = 0;

    public DocumentRelevance(int docID) {
        this.docID = docID;

    }

    public int getDocID() {
        return docID;
    }

    public double getRelevance() {
        return relevance;
    }

    public void updateRelevance(double tf) {
        relevance += tf;
    }

    public String toString() {
        return "docID=" + docID +
                ", relevance=" + relevance;
    }
}

class DocumentRelevanceComparator implements Comparator<DocumentRelevance> {
    public int compare(DocumentRelevance o1, DocumentRelevance o2) {
        return Double.compare(o2.getRelevance(), o1.getRelevance());
    }
}

public class InvertedIndex {
    int countTokens = 0;
    List<String> documents;
    Map<String, Term> index;

    public InvertedIndex() {
        documents = new ArrayList<>();
        index = new HashMap<>();
    }

    public void indexDocument(String path) throws IOException {
        if (!documents.contains(path)) {
            Integer docId = documents.size();
            documents.add(docId, path);
            //Document doc = (Document) Jsoup.parse(file, "UTF-8");
            File input = new File(path);
            Document doc = (Document) Jsoup.parse(input, "UTF-8");
            String content = doc.body().text().toLowerCase();
            String[] words = content.split("[^a-zA-Z0-9_']+");
            boolean isExist = false;
            Stemmer stemmer;
            for (int i = 0; i < words.length; i++) {
                stemmer = new Stemmer();
                stemmer.add(words[i].toCharArray(), words[i].length());
                stemmer.stem();
                String term = stemmer.toString();
                Term idx = index.get(term);
                isExist = false;
                if (idx == null) {
                    idx = new Term(docId);
                    isExist = true;
                } else
                    idx.addDocument(docId);
                index.put(term, idx);

                //System.out.println(term + " " + index.get(term).getList());
            }
            countTokens += index.size();
            System.out.printf("| %2d | %60s | %5d |%n", docId, path,
                    index.size());
        }
    }

    public void indexCollection(String folder) throws IOException {
        File dir = new File(folder);
        String[] files = dir.list();
        for (int i = 0; i < files.length; i++) {
            this.indexDocument(folder + "\\" + files[i]);
        }
        Iterator<String> iter = index.keySet().iterator();
        double idf;
        String term, word;
        int n = documents.size();
        int df;
        Stemmer stemmer;
        while (iter.hasNext()) {
            word = iter.next();  // считываем очередной терма из индекса
            df = index.get(word).list.size();
            idf = Math.log10(n * 1. / df);
            index.get(word).computeTfIdf(idf);
        }
    }

    public LinkedList<DocumentRelevance> executeQuery(String q) {
        String query = q.toLowerCase();
        LinkedList<DocumentRelevance> answer = new LinkedList<>();
        int i = 0;
        DocumentRelevance documentRelevance;
        while (i < documents.size()) {
            documentRelevance = new DocumentRelevance(i);
            answer.add(documentRelevance);
            i++;
        }
        String s[] = query.split(" ");
        Stemmer stemmer;
        String term;
        for (String sustr : s) {
            stemmer = new Stemmer();
            stemmer.add(sustr.toCharArray(), sustr.length());
            stemmer.stem();
            term = stemmer.toString();
            if (index.containsKey(term)) {
                getIntersection(answer, index.get(term));
            }
        }
        Collections.sort(answer, new DocumentRelevanceComparator());
        int count = 0;
        for (DocumentRelevance relevance : answer) {
            if (relevance.getRelevance() == 0) {
                count++;
            }
        }
        for (int j = 0; j < count; j++){
            answer.removeLast();
        }
        return answer;
    }

    public LinkedList<DocumentRelevance> executeQuery(String query, int count) {
        LinkedList<DocumentRelevance> answer = executeQuery(query);

        int size = answer.size();
        for (int i = 0; i < size - count; i++){
            answer.removeLast();
        }
        return answer;
    }

    public void getIntersection(List<DocumentRelevance> answer, Term term) {
        LinkedList list = term.getList();


        DocumentRelevance a;
        TermDocument t;
        for (Iterator<TermDocument> termIterator = list.iterator();termIterator.hasNext();) {
            t = termIterator.next();
            for (Iterator<DocumentRelevance> answerIterator = answer.iterator();answerIterator.hasNext();) {
                a = answerIterator.next();

                if (a.getDocID() == t.getDocID()) {
                    a.updateRelevance(t.getTfIdf());
                }
            }
        }
    }

    void printResult(LinkedList<DocumentRelevance> relevances){
        int i = 1;
        for (DocumentRelevance relevance : relevances) {
            System.out.printf("%d.\t(%.4f)\t%s\n", i, relevance.getRelevance(), this.documents.get(relevance.getDocID()));
            i++;
        }
    }
    public static void main(String[] args) throws IOException {
        InvertedIndex myIndex = new InvertedIndex();
        myIndex.indexCollection("collection_html");
        System.out.println("Count tokens " + myIndex.countTokens);
        System.out.println("Size " + myIndex.index.size());
        long i = 0;
        /*for (Map.Entry<String, Term> pair : myIndex.index.entrySet()) {
            System.out.println(pair.getKey() + " " + pair.getValue().getList());
        }*/
        String q = "Brutus Caesar Calpurnia is a";
        LinkedList<DocumentRelevance> relevances = myIndex.executeQuery(q, 5);
        System.out.println(q);
        //System.out.println(myIndex.index.get("romeo"));
        myIndex.printResult(relevances);
    }
}
