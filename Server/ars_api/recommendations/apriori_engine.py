import csv
import itertools
from collections import defaultdict
from dataclasses import dataclass
from pathlib import Path


@dataclass
class AssociationRule:
    antecedent: frozenset[str]
    consequent: str
    confidence: float


class AprioriEngine:
    def __init__(self, menu_path: Path, orders_path: Path) -> None:
        self.menu_path = menu_path
        self.orders_path = orders_path
        self.transactions: list[list[str]] = []
        self.rules: list[AssociationRule] = []
        self.item_categories: dict[str, str] = {}

    def load_csv_transactions(self) -> None:
        menu_map: dict[int, str] = {}
        with open(self.menu_path, encoding="utf-8-sig") as f:
            reader = csv.DictReader(f)
            for row in reader:
                try:
                    item_id = int(float(row["menu_item_id"]))
                    name = row["item_name"].strip()
                    menu_map[item_id] = name
                    self.item_categories[name] = row.get("category", "").strip()
                except (ValueError, KeyError):
                    continue

        orders_map: dict[str, list[str]] = defaultdict(list)
        with open(self.orders_path, encoding="utf-8-sig") as f:
            reader = csv.DictReader(f)
            for row in reader:
                try:
                    order_id = row["order_id"].strip()
                    item_id = int(float(row["item_id"]))
                    if item_id in menu_map:
                        orders_map[order_id].append(menu_map[item_id])
                except (ValueError, KeyError):
                    continue

        self.transactions = list(orders_map.values())

    def load_db_transactions(self, conn) -> None:
        rows = conn.execute(
            """
            SELECT s.siparis_id, u.urun_adi
            FROM Siparis s
            JOIN SiparisDetay sd ON s.siparis_id = sd.siparis_id
            JOIN Urun u ON sd.urun_id = u.urun_id
            WHERE (s.durum IS NULL OR s.durum != 'İptal Edildi')
            ORDER BY s.siparis_id
            """
        ).fetchall()

        by_order: dict[int, list[str]] = defaultdict(list)
        for row in rows:
            name = row["urun_adi"]
            if name:
                by_order[row["siparis_id"]].append(name)

        self.transactions.extend(by_order.values())

    def train(self, min_support: float = 0.002, min_confidence: float = 0.1) -> None:
        if not self.transactions:
            self.rules = []
            return

        n = len(self.transactions)
        min_count = min_support * n
        counts: dict[frozenset[str], int] = defaultdict(int)

        for transaction in self.transactions:
            for item in set(transaction):
                counts[frozenset([item])] += 1

        freq_itemsets = {k: v for k, v in counts.items() if v >= min_count}
        current_freq = dict(freq_itemsets)

        k = 2
        while current_freq and k <= 3:
            candidates: set[frozenset[str]] = set()
            items = list(current_freq.keys())
            for i in range(len(items)):
                for j in range(i + 1, len(items)):
                    union = items[i] | items[j]
                    if len(union) == k:
                        candidates.add(union)

            cand_counts: dict[frozenset[str], int] = defaultdict(int)
            for transaction in self.transactions:
                t_set = set(transaction)
                for cand in candidates:
                    if cand.issubset(t_set):
                        cand_counts[cand] += 1

            current_freq = {k: v for k, v in cand_counts.items() if v >= min_count}
            freq_itemsets.update(current_freq)
            k += 1

        rules: list[AssociationRule] = []
        for itemset, count in freq_itemsets.items():
            if len(itemset) < 2:
                continue
            for i in range(1, len(itemset)):
                for ant_tuple in itertools.combinations(itemset, i):
                    antecedent = frozenset(ant_tuple)
                    consequent = itemset - antecedent
                    if len(consequent) != 1:
                        continue
                    ant_count = freq_itemsets.get(antecedent)
                    if not ant_count:
                        continue
                    confidence = count / ant_count
                    if confidence >= min_confidence:
                        rules.append(
                            AssociationRule(
                                antecedent=antecedent,
                                consequent=next(iter(consequent)),
                                confidence=confidence,
                            )
                        )

        rules.sort(key=lambda r: r.confidence, reverse=True)
        self.rules = rules

    def recommend(self, cart_csv_names: list[str], limit: int = 5) -> list[tuple[str, float]]:
        cart = set(cart_csv_names)
        if not cart:
            return []

        best: dict[str, float] = {}
        for rule in self.rules:
            if not rule.antecedent.issubset(cart):
                continue
            if rule.consequent in cart:
                continue
            prev = best.get(rule.consequent, 0.0)
            if rule.confidence > prev:
                best[rule.consequent] = rule.confidence

        ranked = sorted(best.items(), key=lambda x: x[1], reverse=True)
        return ranked[:limit]
