import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class RestoranOneriSistemi {

    // Kural yapısını tutacak yardımcı sınıf
    public static class AssociationRule {
        Set<String> sepet; // Antecedent (Eğer bunlar varsa)
        String oneri;      // Consequent (Bunu öner)
        double guven;      // Confidence

        public AssociationRule(Set<String> sepet, String oneri, double guven) {
            this.sepet = sepet;
            this.oneri = oneri;
            this.guven = guven;
        }
    }

    private String menuPath;
    private String ordersPath;
    private List<Set<String>> transactions;
    private List<AssociationRule> rules;

    public RestoranOneriSistemi(String menuPath, String ordersPath) {
        this.menuPath = menuPath;
        this.ordersPath = ordersPath;
        this.transactions = new ArrayList<>();
        this.rules = new ArrayList<>();
    }

    // 1. Dosyaları Okuma ve Hazırlama
    public void veriYukleVeHazirla() {
        System.out.println("1. Dosyalar okunuyor...");
        Map<Integer, String> menuMap = new HashMap<>();

        try {
            // A. Menüyü Yükle
            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(new FileInputStream(menuPath), StandardCharsets.UTF_8))) {
                
                String line = br.readLine();
                
                while ((line = br.readLine()) != null) {
                    String[] parts = splitCsvLine(line);
                    if (parts.length < 2) continue; // Hatalı satır
                    
                    // menu_item_id (genelde 1. sütun), item_name (genelde 2. sütun)
                    // CSV formatınıza göre indeksleri değiştirebilirsiniz.
                    // Python kodundaki dict reader key'lere baktığı için burada indeks tahmini yapıyoruz:
                    // Genelde: menu_item_id, item_name, category...
                    try {
                        String idStr = cleanText(parts[0]);
                        String name = cleanText(parts[1]);
                        
                        if (!idStr.isEmpty() && !name.isEmpty()) {
                            menuMap.put(Integer.parseInt(idStr), name);
                        }
                    } catch (NumberFormatException e) {
                        continue;
                    }
                }
            }

            // B. Siparişleri Yükle
            Map<String, Set<String>> ordersMap = new HashMap<>();
            
            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(new FileInputStream(ordersPath), StandardCharsets.UTF_8))) {
                
                String line = br.readLine(); // Header'ı atla
                
                while ((line = br.readLine()) != null) {
                    String[] parts = splitCsvLine(line);
                    // order_details_id, order_id, item_id...
                    // order_id -> index 1, item_id -> index 2 varsayımı
                    if (parts.length < 3) continue;

                    String orderId = cleanText(parts[1]);
                    String itemIdStr = cleanText(parts[2]);

                    if (!orderId.isEmpty() && !itemIdStr.isEmpty()) {
                        try {
                            // Python'daki float(i_id) mantığına karşılık:
                            double d = Double.parseDouble(itemIdStr);
                            int itemId = (int) d;

                            if (menuMap.containsKey(itemId)) {
                                String yemekIsmi = menuMap.get(itemId);
                                ordersMap.computeIfAbsent(orderId, k -> new HashSet<>()).add(yemekIsmi);
                            }
                        } catch (NumberFormatException e) {
                            continue;
                        }
                    }
                }
            }

            this.transactions = new ArrayList<>(ordersMap.values());
            System.out.println("   -> Başarılı! Toplam " + this.transactions.size() + " sipariş fişi işlendi.");

        } catch (Exception e) {
            System.out.println("   -> HATA: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // 2. Modeli Eğitme (Apriori Benzeri)
    public void modeliEgit(double minSupport, double minConfidence) {
        if (this.transactions.isEmpty()) return;

        System.out.println("2. İlişkiler analiz ediliyor...");
        int n = this.transactions.size();
        int minCount = (int) (minSupport * n);
        
        // Frekansları tutan harita: ItemSet -> Count
        Map<Set<String>, Integer> counts = new HashMap<>();

        // ADIM A: Tekli ürünleri say
        for (Set<String> t : transactions) {
            for (String item : t) {
                Set<String> single = new HashSet<>();
                single.add(item);
                counts.put(single, counts.getOrDefault(single, 0) + 1);
            }
        }

        // Eşiği geçenleri filtrele (L1)
        Map<Set<String>, Integer> freqItemsets = new HashMap<>();
        for (Map.Entry<Set<String>, Integer> entry : counts.entrySet()) {
            if (entry.getValue() >= minCount) {
                freqItemsets.put(entry.getKey(), entry.getValue());
            }
        }
        
        Map<Set<String>, Integer> currentFreq = new HashMap<>(freqItemsets);

        // ADIM B: 2'li ve 3'lü kombinasyonlar
        int k = 2;
        while (!currentFreq.isEmpty() && k <= 3) {
            Set<Set<String>> candidates = new HashSet<>();
            List<Set<String>> items = new ArrayList<>(currentFreq.keySet());

            // Aday oluştur (Join step)
            for (int i = 0; i < items.size(); i++) {
                for (int j = i + 1; j < items.size(); j++) {
                    Set<String> union = new HashSet<>(items.get(i));
                    union.addAll(items.get(j));
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

            // Filtrele
            currentFreq.clear();
            for (Map.Entry<Set<String>, Integer> entry : candCounts.entrySet()) {
                if (entry.getValue() >= minCount) {
                    currentFreq.put(entry.getKey(), entry.getValue());
                    freqItemsets.put(entry.getKey(), entry.getValue());
                }
            }
            k++;
        }

        // ADIM C: Kuralları oluştur
        this.rules.clear();
        for (Map.Entry<Set<String>, Integer> entry : freqItemsets.entrySet()) {
            Set<String> itemset = entry.getKey();
            int count = entry.getValue();

            if (itemset.size() < 2) continue;

            // Kombinasyonun alt kümelerini bul (Antecedent oluşturmak için)
            // Python: for i in range(1, len(itemset)) ... combinations
            List<Set<String>> antecedents = generateSubsets(itemset);
            
            for (Set<String> ant : antecedents) {
                // Consequent = Itemset - Antecedent
                Set<String> consequentSet = new HashSet<>(itemset);
                consequentSet.removeAll(ant);

                // Python kodunda: if len(consequent) == 1
                if (consequentSet.size() == 1) {
                    Integer antCount = freqItemsets.get(ant);
                    // Antecedent frekans listesinde olmayabilir (nadiren), kontrol et
                    if (antCount != null && antCount > 0) {
                        double conf = (double) count / antCount;
                        if (conf >= minConfidence) {
                            String oneriUrun = consequentSet.iterator().next();
                            // Yeni sepet seti oluştur (referans hatası olmaması için)
                            this.rules.add(new AssociationRule(new HashSet<>(ant), oneriUrun, conf));
                        }
                    }
                }
            }
        }

        // Kuralları güvene göre sırala (Büyükten küçüğe)
        this.rules.sort((r1, r2) -> Double.compare(r2.guven, r1.guven));
        System.out.println("   -> Model Hazır! " + this.rules.size() + " kural hafızaya alındı.");
    }

    // 3. Tavsiye Etme
    public void tavsiyeEt(List<String> sepetListesi) {
        Set<String> sepetSeti = new HashSet<>(sepetListesi);
        
        AssociationRule enIyiKural = null;
        
        // Kurallar zaten sıralı, ilk eşleşeni al
        for (AssociationRule kural : rules) {
            if (kural.sepet.equals(sepetSeti)) {
                enIyiKural = kural;
                break;
            }
        }

        System.out.println("\n--- Sipariş: " + sepetListesi + " ---");
        if (enIyiKural != null) {
            System.out.printf("✅ TAVSİYE: %s%n", enIyiKural.oneri);
            System.out.printf("📊 Güven: %% %.1f%n", enIyiKural.guven * 100);
        } else {
            System.out.println("❌ Bu kombinasyon için yeterli veri yok.");
        }
    }

    // --- YARDIMCI METOTLAR ---

    // Set'in tüm alt kümelerini (kendisi ve boş küme hariç) üretir
    private List<Set<String>> generateSubsets(Set<String> set) {
        List<Set<String>> subsets = new ArrayList<>();
        List<String> list = new ArrayList<>(set);
        int n = list.size();
        
        // 1'den 2^n - 1'e kadar (Tam alt küme hariç hepsi için mantık kurulabilir ama
        // burada basitçe antecedent size < set size olduğu sürece çalışır)
        for (int i = 1; i < (1 << n); i++) {
            Set<String> currentSubset = new HashSet<>();
            for (int j = 0; j < n; j++) {
                if ((i & (1 << j)) > 0) {
                    currentSubset.add(list.get(j));
                }
            }
            // Sadece antecedent setin kendisinden küçük olduğu durumları al
            if (currentSubset.size() < n) {
                subsets.add(currentSubset);
            }
        }
        return subsets;
    }

    // Basit CSV satır bölücü (virgül ile)
    private String[] splitCsvLine(String line) {
        // Tırnak içindeki virgülleri yoksaymak için basit regex
        return line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)", -1);
    }

    // BOM (Byte Order Mark) ve tırnakları temizle
    private String cleanText(String text) {
        String s = text.replace("\uFEFF", "").trim(); // BOM temizle
        s = s.replace("\"", ""); // Tırnakları temizle
        return s;
    }

    // --- MAIN ---
    public static void main(String[] args) {
        // Dosya yollarını projenizin yapısına göre ayarlayın
        RestoranOneriSistemi sistem = new RestoranOneriSistemi(
            "menu_items.csv", 
            "order_details.csv"
        );

        sistem.veriYukleVeHazirla();
        sistem.modeliEgit(0.002, 0.1);

        // Testler
        sistem.tavsiyeEt(Arrays.asList("Hamburger"));
        sistem.tavsiyeEt(Arrays.asList("Meat Lasagna", "Chicken Burrito"));
        sistem.tavsiyeEt(Arrays.asList("Veggie Burger", "French Fries"));
    }
}