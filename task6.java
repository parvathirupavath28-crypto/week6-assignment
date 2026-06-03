import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;

public class task6 {
    // Product model with Comparable and several Comparators
    public static class Product implements Comparable<Product> {
        private final String id;
        private final String name;
        private final String category;
        private double price;
        private int quantity;

        public Product(String id, String name, String category, double price, int quantity) {
            this.id = Objects.requireNonNull(id);
            this.name = Objects.requireNonNull(name);
            this.category = Objects.requireNonNull(category);
            this.price = price;
            this.quantity = quantity;
        }

        public String getId() { return id; }
        public String getName() { return name; }
        public String getCategory() { return category; }
        public double getPrice() { return price; }
        public int getQuantity() { return quantity; }

        public void setPrice(double price) { this.price = price; }
        public void setQuantity(int quantity) { this.quantity = quantity; }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Product)) return false;
            Product p = (Product) o;
            return id.equals(p.id);
        }

        @Override
        public int hashCode() { return id.hashCode(); }

        @Override
        public int compareTo(Product other) {
            return String.CASE_INSENSITIVE_ORDER.compare(this.name, other.name);
        }

        public static final Comparator<Product> BY_NAME =
                Comparator.comparing(Product::getName, String.CASE_INSENSITIVE_ORDER);
        public static final Comparator<Product> BY_PRICE =
                Comparator.comparingDouble(Product::getPrice);
        public static final Comparator<Product> BY_QUANTITY =
                Comparator.comparingInt(Product::getQuantity);
        public static final Comparator<Product> BY_CATEGORY_THEN_NAME =
                Comparator.comparing(Product::getCategory, String.CASE_INSENSITIVE_ORDER)
                          .thenComparing(BY_NAME);

        @Override
        public String toString() {
            return String.format("[%s] %s (%s) - $%.2f x %d", id, name, category, price, quantity);
        }
    }

    // Generic Inventory using deliberate collections: LinkedHashMap, HashMap, List, PriorityQueue
    public static class Inventory<T extends Product> {
        private final Map<String, T> productsById = new LinkedHashMap<>(); // preserve insertion order
        private final Map<String, List<T>> productsByCategory = new HashMap<>();
        private final List<T> duplicates = new ArrayList<>();

        // Add returns true if added; false if id already exists (recorded as duplicate)
        public boolean addProduct(T p) {
            if (productsById.containsKey(p.getId())) {
                duplicates.add(p);
                return false;
            }
            productsById.put(p.getId(), p);
            productsByCategory.computeIfAbsent(p.getCategory(), k -> new ArrayList<>()).add(p);
            return true;
        }

        public Optional<T> getById(String id) {
            return Optional.ofNullable(productsById.get(id));
        }

        public List<T> getAllProducts() {
            return new ArrayList<>(productsById.values());
        }

        public List<T> getProductsInCategory(String category) {
            return new ArrayList<>(productsByCategory.getOrDefault(category, Collections.emptyList()));
        }

        public List<T> getSortedAll(Comparator<T> comp) {
            List<T> copy = getAllProducts();
            copy.sort(comp);
            return copy;
        }

        public List<T> getSortedProductsInCategory(String category, Comparator<T> comp) {
            List<T> list = getProductsInCategory(category);
            list.sort(comp);
            return list;
        }

        public List<T> findDuplicates() {
            return Collections.unmodifiableList(duplicates);
        }

        // Low-stock retrieval using PriorityQueue (min-heap by quantity)
        public List<T> getLowStockItems(int threshold) {
            PriorityQueue<T> pq = new PriorityQueue<>(Comparator.comparingInt(Product::getQuantity));
            for (T p : productsById.values()) {
                if (p.getQuantity() <= threshold) pq.add(p);
            }
            List<T> out = new ArrayList<>();
            while (!pq.isEmpty()) out.add(pq.poll());
            return out;
        }

        // Summary reports using Maps
        public Map<String, Integer> totalQuantityPerCategory() {
            Map<String, Integer> map = new HashMap<>();
            for (Map.Entry<String, List<T>> e : productsByCategory.entrySet()) {
                int sum = e.getValue().stream().mapToInt(Product::getQuantity).sum();
                map.put(e.getKey(), sum);
            }
            return map;
        }

        public Map<String, Double> totalValuePerCategory() {
            Map<String, Double> map = new HashMap<>();
            for (Map.Entry<String, List<T>> e : productsByCategory.entrySet()) {
                double sum = e.getValue().stream().mapToDouble(p -> p.getPrice() * p.getQuantity()).sum();
                map.put(e.getKey(), sum);
            }
            return map;
        }

        public List<T> search(Predicate<T> predicate) {
            List<T> out = new ArrayList<>();
            for (T p : productsById.values()) if (predicate.test(p)) out.add(p);
            return out;
        }

        public <K> Map<K, List<T>> groupBy(Function<T, K> classifier) {
            Map<K, List<T>> map = new LinkedHashMap<>();
            for (T p : productsById.values()) {
                K key = classifier.apply(p);
                map.computeIfAbsent(key, kk -> new ArrayList<>()).add(p);
            }
            return map;
        }
    }

    public static class UserPreferences {
        private String sortPreference = "Name";
        private int lowStockThreshold = 5;
        private String lastSearchTerm = "";
        private String lastCategory = "";
        private String lastAddedCategory = "";

        @Override
        public String toString() {
            return String.format("sort=%s, lowStockThreshold=%d, lastCategory=%s",
                    sortPreference, lowStockThreshold,
                    lastCategory.isEmpty() ? "none" : lastCategory);
        }
    }

    // Self-contained demo usage for console output (copy into task6.java)
    public static void main(String[] args) {
        Inventory<Product> inv = new Inventory<>();
        seedInventory(inv);
        UserPreferences prefs = new UserPreferences();

        try (Scanner sc = new Scanner(System.in)) {
            while (true) {
                System.out.println();
                System.out.println("SMART INVENTORY SYSTEM");
                System.out.println("1. Add Product");
                System.out.println("2. Search by Name");
                System.out.println("3. View by Category");
                System.out.println("4. View All (sorted)");
                System.out.println("5. Low Stock Alert");
                System.out.println("6. Generate Report");
                System.out.println("7. Exit");
                System.out.print("Choice: ");
                String line = sc.nextLine().trim();
                int choice;
                try { choice = Integer.parseInt(line); } catch (Exception e) { System.out.println("Invalid choice"); continue; }

                switch (choice) {
                    case 1 -> addProductInteractive(sc, inv, prefs);
                    case 2 -> searchByName(sc, inv, prefs);
                    case 3 -> viewByCategory(sc, inv, prefs);
                    case 4 -> viewAllSorted(sc, inv, prefs);
                    case 5 -> lowStockAlert(sc, inv, prefs);
                    case 6 -> printInventoryReport(inv, prefs);
                    case 7 -> { System.out.println("Exiting."); return; }
                    default -> System.out.println("Invalid choice");
                }
            }
        }
    }

    private static void seedInventory(Inventory<Product> inv) {
        inv.addProduct(new Product("001", "Laptop", "Electronics", 45000.00, 2));
        inv.addProduct(new Product("002", "Mouse", "Electronics", 500.00, 10));
        inv.addProduct(new Product("003", "Keyboard", "Electronics", 1200.00, 5));
        inv.addProduct(new Product("004", "Smartphone", "Electronics", 18000.00, 7));
        inv.addProduct(new Product("005", "Bread", "Food", 40.00, 20));
        inv.addProduct(new Product("006", "Butter", "Food", 60.00, 8));
        inv.addProduct(new Product("007", "Winter Jacket", "Clothing", 3500.00, 12));
        inv.addProduct(new Product("008", "T-Shirt", "Clothing", 400.00, 30));
    }

    private static void addProductInteractive(Scanner sc, Inventory<Product> inv, UserPreferences prefs) {
        System.out.print("Enter id: "); String id = sc.nextLine().trim();
        System.out.print("Enter name: "); String name = sc.nextLine().trim();
        System.out.print("Enter category: "); String cat = sc.nextLine().trim();
        System.out.print("Enter price: "); double price = parseDouble(sc.nextLine().trim(), 0.0);
        System.out.print("Enter quantity: "); int qty = parseInt(sc.nextLine().trim(), 0);
        boolean added = inv.addProduct(new Product(id, name, cat, price, qty));
        if (added) {
            prefs.lastAddedCategory = cat;
            System.out.println("Product added.");
        } else {
            System.out.println("Duplicate id — product not added (recorded). ");
        }
    }

    private static void searchByName(Scanner sc, Inventory<Product> inv, UserPreferences prefs) {
        System.out.print("Search term: "); String term = sc.nextLine().trim();
        prefs.lastSearchTerm = term;
        List<Product> results = inv.search(p -> p.getName().toLowerCase().contains(term.toLowerCase()));
        if (results.isEmpty()) System.out.println("No results."); else results.forEach(System.out::println);
    }

    private static void viewByCategory(Scanner sc, Inventory<Product> inv, UserPreferences prefs) {
        System.out.print("Category: "); String cat = sc.nextLine().trim();
        prefs.lastCategory = cat;
        List<Product> list = inv.getProductsInCategory(cat);
        if (list.isEmpty()) System.out.println("No products in that category."); else list.forEach(System.out::println);
    }

    private static void viewAllSorted(Scanner sc, Inventory<Product> inv, UserPreferences prefs) {
        System.out.println("Sort by: 1.Name 2.Price 3.Quantity 4.Category");
        System.out.print("Choice: "); String c = sc.nextLine().trim();
        Comparator<Product> comp = switch (c) {
            case "1" -> Product.BY_NAME;
            case "2" -> Product.BY_PRICE;
            case "3" -> Product.BY_QUANTITY;
            case "4" -> Product.BY_CATEGORY_THEN_NAME;
            default -> Product.BY_NAME;
        };
        prefs.sortPreference = switch (c) {
            case "1" -> "Name";
            case "2" -> "Price";
            case "3" -> "Quantity";
            case "4" -> "Category";
            default -> "Name";
        };
        System.out.println("Displaying all products sorted by " + prefs.sortPreference + ".");
        inv.getSortedAll(comp).forEach(System.out::println);
    }

    private static void lowStockAlert(Scanner sc, Inventory<Product> inv, UserPreferences prefs) {
        System.out.print("Low stock threshold: "); int t = parseInt(sc.nextLine().trim(), 5);
        prefs.lowStockThreshold = t;
        System.out.println("Using threshold " + t + " for low stock alert.");
        List<Product> low = inv.getLowStockItems(t);
        if (low.isEmpty()) System.out.println("No low-stock items."); else low.forEach(System.out::println);
    }

    private static int parseInt(String s, int def) {
        try { return Integer.parseInt(s); } catch (Exception e) { return def; }
    }

    private static double parseDouble(String s, double def) {
        try { return Double.parseDouble(s); } catch (Exception e) { return def; }
    }

    private static void printInventoryReport(Inventory<Product> inv, UserPreferences prefs) {
        List<Product> all = inv.getAllProducts();
        int totalProducts = all.size();
        double totalValue = inv.totalValuePerCategory().values().stream().mapToDouble(Double::doubleValue).sum();

        Map<String, List<Product>> byCat = inv.groupBy(Product::getCategory);
        List<Product> top3 = inv.getSortedAll(Product.BY_PRICE.reversed());
        if (top3.size() > 3) top3 = top3.subList(0, 3);
        List<Product> low = inv.getLowStockItems(prefs.lowStockThreshold);

        System.out.println("INVENTORY REPORT");
        System.out.println(repeat('-', 40));
        System.out.println("Preferences: " + prefs);
        System.out.println("Last search term: " + (prefs.lastSearchTerm.isEmpty() ? "none" : prefs.lastSearchTerm));
        System.out.println("Last category viewed: " + (prefs.lastCategory.isEmpty() ? "none" : prefs.lastCategory));
        System.out.println("Last added category: " + (prefs.lastAddedCategory.isEmpty() ? "none" : prefs.lastAddedCategory));
        System.out.println(repeat('-', 40));
        System.out.println("Total Products  : " + totalProducts);
        System.out.printf("Total Value     : ₹%.2f%n", totalValue);
        System.out.println();
        System.out.println("Products by Category:");
        for (Map.Entry<String, List<Product>> e : byCat.entrySet()) {
            System.out.printf("  %s : %d products%n", e.getKey().toUpperCase(), e.getValue().size());
        }
        System.out.println();
        System.out.println("Top 3 Most Expensive:");
        for (Product p : top3) {
            System.out.printf("  [%s] %s | ₹%.2f | Qty: %d | %s%n",
                    p.getId(), truncate(p.getName(), 20), p.getPrice(), p.getQuantity(), p.getCategory().toUpperCase());
        }
        System.out.println();
        System.out.println("Low Stock Alert (qty <= " + prefs.lowStockThreshold + "):");
        if (low.isEmpty()) {
            System.out.println("  None");
        } else {
            for (Product p : low) {
                System.out.printf("  [%s] %s | Qty: %d | %s%n",
                        p.getId(), truncate(p.getName(), 20), p.getQuantity(), p.getCategory().toUpperCase());
            }
        }
    }

    private static String repeat(char c, int n) {
        StringBuilder sb = new StringBuilder(n);
        for (int i = 0; i < n; i++) sb.append(c);
        return sb.toString();
    }

    private static String truncate(String s, int max) {
        if (s.length() <= max) return s;
        return s.substring(0, max - 1) + "…";
    }
}