import csv
import itertools
from collections import defaultdict

class RestoranOneriSistemi:
    def __init__(self, menu_path, orders_path):
        self.menu_path = menu_path
        self.orders_path = orders_path
        self.transactions = []
        self.rules = []

    def veri_yukle_ve_hazirla(self):
        print("1. Dosyalar okunuyor...")
        menu_map = {}
        
        try:
            # 1. Menüyü Yükle
            with open(self.menu_path, mode='r', encoding='utf-8-sig') as f:
                reader = csv.DictReader(f)
                for row in reader:
                    m_id = row.get('menu_item_id')
                    name = row.get('item_name')
                    if m_id and name:
                        menu_map[int(m_id)] = name

            # 2. Siparişleri Yükle ve Eşleştir
            orders_map = defaultdict(list)
            with open(self.orders_path, mode='r', encoding='utf-8-sig') as f:
                reader = csv.DictReader(f)
                for row in reader:
                    o_id = row.get('order_id')
                    i_id = row.get('item_id')
                    
                    if o_id and i_id:
                        try:
                            item_id_int = int(float(i_id))
                            if item_id_int in menu_map:
                                yemek_ismi = menu_map[item_id_int]
                                orders_map[o_id].append(yemek_ismi)
                        except ValueError:
                            continue
            
            self.transactions = list(orders_map.values())
            print(f"   -> Başarılı! Toplam {len(self.transactions)} sipariş fişi işlendi.")
            
        except FileNotFoundError:
            print("   -> HATA: Dosyalar bulunamadı! Dosya isimlerini kontrol edin.")
            self.transactions = []
        except Exception as e:
            print(f"   -> Beklenmedik hata: {e}")

    def modeli_egit(self, min_support=0.002, min_confidence=0.1):
        if not self.transactions:
            return

        print(f"2. İlişkiler analiz ediliyor...")
        n = len(self.transactions)
        min_count = min_support * n
        counts = defaultdict(int)

        # ADIM A: Tekli ürünlerin sayımı
        for t in self.transactions:
            for item in t:
                counts[frozenset([item])] += 1
        
        # Eşiği geçenleri filtrele
        freq_itemsets = {k: v for k, v in counts.items() if v >= min_count}
        current_freq = freq_itemsets.copy()
        
        # ADIM B: 2'li ve 3'lü kombinasyonları bul
        k = 2
        while current_freq and k <= 3:
            candidates = set()
            items = list(current_freq.keys())
            
            # Aday oluştur
            for i in range(len(items)):
                for j in range(i + 1, len(items)):
                    union = items[i] | items[j]
                    if len(union) == k:
                        candidates.add(union)
            
            # Adayları say
            cand_counts = defaultdict(int)
            for t in self.transactions:
                t_set = set(t)
                for cand in candidates:
                    if cand.issubset(t_set):
                        cand_counts[cand] += 1
            
            current_freq = {k: v for k, v in cand_counts.items() if v >= min_count}
            freq_itemsets.update(current_freq)
            k += 1

        # ADIM C: Kuralları oluştur
        self.rules = []
        for itemset, count in freq_itemsets.items():
            if len(itemset) < 2: continue
            
            for i in range(1, len(itemset)):
                for antecedent in itertools.combinations(itemset, i):
                    antecedent = frozenset(antecedent)
                    consequent = itemset - antecedent
                    
                    if len(consequent) == 1:
                        ant_count = freq_itemsets.get(antecedent)
                        if ant_count:
                            conf = count / ant_count
                            if conf >= min_confidence:
                                self.rules.append({
                                    'sepet': set(antecedent),
                                    'oneri': list(consequent)[0],
                                    'guven': conf
                                })
        
        # Kuralları güvene göre sırala
        self.rules.sort(key=lambda x: x['guven'], reverse=True)
        print(f"   -> Model Hazır! {len(self.rules)} kural hafızaya alındı.")

    def tavsiye_et(self, sepet_listesi):
        sepet_seti = set(sepet_listesi)
        
        # Kurallar zaten sıralı olduğu için ilk eşleşen en iyisidir
        en_iyi_kural = None
        for kural in self.rules:
            if kural['sepet'] == sepet_seti:
                en_iyi_kural = kural
                break
        
        print(f"\n--- Sipariş: {sepet_listesi} ---")
        if en_iyi_kural:
            urun = en_iyi_kural['oneri']
            guven = en_iyi_kural['guven'] * 100
            print(f"✅ TAVSİYE: {urun}")
            print(f"📊 Güven: %{guven:.1f}")
        else:
            print("❌ Bu kombinasyon için yeterli veri yok.")

sistem = RestoranOneriSistemi('menu_items.csv', 'order_details.csv')
sistem.veri_yukle_ve_hazirla()
sistem.modeli_egit(min_support=0.002, min_confidence=0.1)

sistem.tavsiye_et(['Hamburger'])
sistem.tavsiye_et(['Meat Lasagna', 'Chicken Burrito'])
sistem.tavsiye_et(['Veggie Burger', 'French Fries'])