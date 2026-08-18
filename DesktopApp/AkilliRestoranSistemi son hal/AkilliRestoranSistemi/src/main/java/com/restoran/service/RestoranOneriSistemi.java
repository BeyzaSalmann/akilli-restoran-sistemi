package com.restoran.service;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

public class RestoranOneriSistemi {

    private String menuPath;
    private String ordersPath;
    private List<Set<String>> transactions;
    private List<Rule> rules;

    /** Birliktelik kuralı: sepet (antecedent) → önerilen ürün (consequent). */
    private static class Rule {
        Set<String> antecedent; // Sepet (Eğer bunlar varsa...)
        String consequent;      // Öneri (...bunu öner)
        double confidence;      // Güven

        public Rule(Set<String> antecedent, String consequent, double confidence) {
            this.antecedent = antecedent;
            this.consequent = consequent;
            this.confidence = confidence;
        }
    }

    public RestoranOneriSistemi(String menuPath, String ordersPath) {
        this.menuPath = menuPath;
        this.ordersPath = ordersPath;
        this.transactions = new ArrayList<>();
        this.rules = new ArrayList<>();
    }

    public void veriYukleVeHazirla() {
        Map<Integer, String> menuMap = new HashMap<>();

        try {
            try (BufferedReader br = new BufferedReader(new FileReader(menuPath))) {
                String line = br.readLine();
                if (line != null && line.startsWith("\uFEFF")) {
                    line = line.substring(1);
                }
                
                // Sütun indekslerini bul (Dinamik okuma için)
                Map<String, Integer> headers = parseHeaders(line);

                while ((line = br.readLine()) != null) {
                    String[] parts = line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)", -1); // CSV regex split
                    
                    if (headers.containsKey("menu_item_id") && headers.containsKey("item_name")) {
                        try {
                            int id = Integer.parseInt(parts[headers.get("menu_item_id")].trim());
                            String name = parts[headers.get("item_name")].trim();
                            menuMap.put(id, name);
                        } catch (NumberFormatException e) {
                            // Başlık veya hatalı satır atla
                        }
                    }
                }
            }

            // B. Siparişleri Yükle
            Map<String, List<String>> ordersMap = new HashMap<>();
            try (BufferedReader br = new BufferedReader(new FileReader(ordersPath))) {
                String line = br.readLine();
                if (line != null && line.startsWith("\uFEFF")) line = line.substring(1);
                
                Map<String, Integer> headers = parseHeaders(line);

                while ((line = br.readLine()) != null) {
                    String[] parts = line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)", -1);
                    
                    if (headers.containsKey("order_id") && headers.containsKey("item_id")) {
                        String oId = parts[headers.get("order_id")].trim();
                        String iIdStr = parts[headers.get("item_id")].trim();

                        if (!oId.isEmpty() && !iIdStr.isEmpty()) {
                            try {
                                // "109.0" gibi float stringleri int'e çevir
                                double d = Double.parseDouble(iIdStr);
                                int itemId = (int) d;

                                if (menuMap.containsKey(itemId)) {
                                    ordersMap.computeIfAbsent(oId, k -> new ArrayList<>()).add(menuMap.get(itemId));
                                }
                            } catch (NumberFormatException e) {
                                continue;
                            }
                        }
                    }
                }
            }

            for (List<String> orderItems : ordersMap.values()) {
                this.transactions.add(new HashSet<>(orderItems));
            }
        } catch (IOException e) {
            System.err.println("Öneri verisi okunamadı: " + e.getMessage());
        }
    }

    private Map<String, Integer> parseHeaders(String headerLine) {
        Map<String, Integer> headers = new HashMap<>();
        if (headerLine == null) return headers;
        String[] parts = headerLine.split(",");
        for (int i = 0; i < parts.length; i++) {
            headers.put(parts[i].trim(), i);
        }
        return headers;
    }

    /**
     * Apriori ile sık öğe kümelerinden birliktelik kuralları üretir.
     */
    public void modeliEgit(double minSupport, double minConfidence) {
        if (transactions.isEmpty()) return;

        int n = transactions.size();
        double minCount = minSupport * n;

        // Tüm frekans haritası (Itemset -> Count)
        Map<Set<String>, Integer> freqItemsets = new HashMap<>();

        // ADIM A: Tekli ürünleri say
        Map<Set<String>, Integer> counts = new HashMap<>();
        for (Set<String> t : transactions) {
            for (String item : t) {
                Set<String> singleItemSet = new HashSet<>(Collections.singletonList(item));
                counts.put(singleItemSet, counts.getOrDefault(singleItemSet, 0) + 1);
            }
        }

        // Eşiği geçenleri filtrele
        Map<Set<String>, Integer> currentFreq = new HashMap<>();
        for (Map.Entry<Set<String>, Integer> entry : counts.entrySet()) {
            if (entry.getValue() >= minCount) {
                currentFreq.put(entry.getKey(), entry.getValue());
            }
        }
        freqItemsets.putAll(currentFreq);

        // ADIM B: 2'li ve 3'lü kombinasyonlar
        int k = 2;
        while (!currentFreq.isEmpty() && k <= 3) {
            Set<Set<String>> candidates = new HashSet<>();
            List<Set<String>> itemList = new ArrayList<>(currentFreq.keySet());

            // Aday oluştur (Join step)
            for (int i = 0; i < itemList.size(); i++) {
                for (int j = i + 1; j < itemList.size(); j++) {
                    Set<String> union = new HashSet<>(itemList.get(i));
                    union.addAll(itemList.get(j));
                    if (union.size() == k) {
                        candidates.add(union);
                    }
                }
            }

            // Adayları say
            Map<Set<String>, Integer> candCounts = new HashMap<>();
            for (Set<String> t : transactions) {
                for (Set<String> cand : candidates) {
                    if (t.containsAll(cand)) {
                        candCounts.put(cand, candCounts.getOrDefault(cand, 0) + 1);
                    }
                }
            }

            // Filtrele ve güncelle
            currentFreq = new HashMap<>();
            for (Map.Entry<Set<String>, Integer> entry : candCounts.entrySet()) {
                if (entry.getValue() >= minCount) {
                    currentFreq.put(entry.getKey(), entry.getValue());
                }
            }
            freqItemsets.putAll(currentFreq);
            k++;
        }

        // ADIM C: Kuralları oluştur
        this.rules.clear();
        for (Map.Entry<Set<String>, Integer> entry : freqItemsets.entrySet()) {
            Set<String> itemset = entry.getKey();
            int count = entry.getValue();

            if (itemset.size() < 2) continue;

            // Alt kümeleri (antecedent) bul
            List<Set<String>> subsets = getSubsets(itemset);
            for (Set<String> antecedent : subsets) {
                if (antecedent.isEmpty() || antecedent.size() == itemset.size()) continue;

                // Consequent = Itemset - Antecedent
                Set<String> consequentSet = new HashSet<>(itemset);
                consequentSet.removeAll(antecedent);

                if (consequentSet.size() == 1) { // Sadece tek ürün öneriyoruz
                    String consequentItem = consequentSet.iterator().next();
                    Integer antCount = freqItemsets.get(antecedent);
                    
                    if (antCount != null) {
                        double conf = (double) count / antCount;
                        if (conf >= minConfidence) {
                            this.rules.add(new Rule(antecedent, consequentItem, conf));
                        }
                    }
                }
            }
        }

        // Güvene göre sırala (Büyükten küçüğe)
        this.rules.sort((r1, r2) -> Double.compare(r2.confidence, r1.confidence));
    }

    // 3. TAVSİYE ET
    public void tavsiyeEt(List<String> sepetListesi) {
        TavsiyeSonucu sonuc = tavsiyeAl(sepetListesi);
        
        System.out.println("\n--- Sipariş: " + sepetListesi + " ---");
        if (sonuc != null && sonuc.onerilenUrun != null) {
            System.out.printf("✅ TAVSİYE: %s%n", sonuc.onerilenUrun);
            System.out.printf("📊 Güven: %% %.1f%n", sonuc.guven * 100);
        } else {
            System.out.println("❌ Bu kombinasyon için yeterli veri yok.");
        }
    }
    
    // Tavsiye sonucu için iç sınıf
    public static class TavsiyeSonucu {
        public String onerilenUrun;
        public double guven;
        
        public TavsiyeSonucu(String onerilenUrun, double guven) {
            this.onerilenUrun = onerilenUrun;
            this.guven = guven;
        }
    }
    
    // Tavsiye al ve sonucu döndür (UI için)
    public TavsiyeSonucu tavsiyeAl(List<String> sepetListesi) {
        if (sepetListesi == null || sepetListesi.isEmpty()) {
            return null;
        }
        
        // Kurallar yüklenmemişse null döndür
        if (this.rules == null || this.rules.isEmpty()) {
            return null;
        }
        
        Set<String> sepetSeti = new HashSet<>(sepetListesi);
        
        // ADIM 1: Tam eşleşme ara (en iyi sonuç)
        Rule enIyiKural = null;
        for (Rule kural : this.rules) {
            if (kural.antecedent.equals(sepetSeti)) {
                enIyiKural = kural;
                break; // İlk (en yüksek güvenli) kuralı al
            }
        }
        
        // ADIM 2: Eğer tam eşleşme yoksa, sepetin alt kümelerini dene
        if (enIyiKural == null) {
            // Sepetin alt kümelerini oluştur (1 elemanlı, 2 elemanlı, vb.)
            List<Set<String>> sepetAltKumeleri = getSubsets(sepetSeti);
            
            // Her alt küme için kural ara
            for (Set<String> altKume : sepetAltKumeleri) {
                if (altKume.isEmpty() || altKume.size() == sepetSeti.size()) {
                    continue; // Boş küme ve tam küme hariç
                }
                
                for (Rule kural : this.rules) {
                    if (kural.antecedent.equals(altKume)) {
                        // Alt küme eşleşmesi bulundu, ama tam eşleşme olmadığı için güveni düşür
                        double adjustedConfidence = kural.confidence * 0.8; // %20 güven kaybı
                        if (enIyiKural == null || adjustedConfidence > enIyiKural.confidence) {
                            enIyiKural = new Rule(kural.antecedent, kural.consequent, adjustedConfidence);
                        }
                    }
                }
            }
        }
        
        // ADIM 3: Eğer hala bulunamadıysa, kısmi eşleşme dene (en az bir ürün ortak)
        if (enIyiKural == null) {
            for (Rule kural : this.rules) {
                // Sepet ve kural antecedent'i arasında ortak ürün var mı?
                Set<String> ortakUrunler = new HashSet<>(sepetSeti);
                ortakUrunler.retainAll(kural.antecedent);
                
                if (!ortakUrunler.isEmpty()) {
                    // Ortak ürün sayısına göre güven hesapla
                    double ortakOran = (double) ortakUrunler.size() / Math.max(sepetSeti.size(), kural.antecedent.size());
                    double adjustedConfidence = kural.confidence * ortakOran * 0.6; // Kısmi eşleşme için daha düşük güven
                    
                    if (enIyiKural == null || adjustedConfidence > enIyiKural.confidence) {
                        enIyiKural = new Rule(kural.antecedent, kural.consequent, adjustedConfidence);
                    }
                }
            }
        }
        
        if (enIyiKural != null && enIyiKural.confidence > 0.05) { // Minimum %5 güven eşiği
            return new TavsiyeSonucu(enIyiKural.consequent, enIyiKural.confidence);
        }
        
        return null;
    }

    // Yardımcı: Bir kümenin tüm alt kümelerini bulur (Kural üretimi için)
    private List<Set<String>> getSubsets(Set<String> set) {
        List<Set<String>> subsets = new ArrayList<>();
        List<String> list = new ArrayList<>(set);
        int n = list.size();

        for (int i = 0; i < (1 << n); i++) {
            Set<String> subset = new HashSet<>();
            for (int j = 0; j < n; j++) {
                if ((i & (1 << j)) > 0) {
                    subset.add(list.get(j));
                }
            }
            subsets.add(subset);
        }
        return subsets;
    }

    public static void main(String[] args) {
        RestoranOneriSistemi sistem = new RestoranOneriSistemi("menu_items.csv", "order_details.csv");

        sistem.veriYukleVeHazirla();
        sistem.modeliEgit(0.002, 0.1);

        sistem.tavsiyeEt(Arrays.asList("Hamburger"));
        sistem.tavsiyeEt(Arrays.asList("Meat Lasagna", "Chicken Burrito"));
        sistem.tavsiyeEt(Arrays.asList("Veggie Burger", "French Fries"));
    }
}