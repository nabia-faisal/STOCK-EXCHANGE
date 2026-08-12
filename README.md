# NUST Exchange — Stock Exchange Trading (Full)

**CS-212 Object Oriented Programming (Summer 2026) — Final Project**

A JavaFX desktop **stock-exchange simulator**. Traders place market and limit
orders; a price-time-priority **matching engine** pairs them into executed
trades; background threads run a live price feed and automated bot traders; and
all state persists to disk between runs.

---

## 1. Features

- Login / registration with three account types (Retail, Institutional, Admin)
- Live market table with moving prices and a price-history chart
- Market & Limit orders (Buy/Sell) with validation
- Real matching engine: price-time priority, partial fills, order book depth
- Portfolio with cash, holdings, market value and unrealized P&L
- Concurrency: matching-engine thread + price simulator + bot traders (race-free)
- Persistence: CSV (stocks, trades), binary serialization (accounts), append-only transaction log
- Admin panel to list new stocks and view all traders

---

## 2. Tech stack

| Area | Choice |
|------|--------|
| Language | Java 17+ (built/run on Java 21 runtime) |
| GUI | JavaFX 21 (LTS) |
| Build | Maven (`pom.xml`) |
| Testing | JUnit 5 |
| Storage | CSV text files + Java object serialization |

---

## 3. What to install

**You need a JDK (Java 17 or newer).** Then pick ONE of the two run methods below.

### Option A — VS Code (recommended, no Maven install needed)
1. Install **VS Code**.
2. In VS Code, open the **Extensions** panel (`Ctrl+Shift+X`) and install
   **"Extension Pack for Java"** by Microsoft. This bundles everything needed to
   build and run Maven projects (it even ships its own Java 21 runtime).

### Option B — Command line
1. Install a **JDK 17+** (e.g. [Temurin](https://adoptium.net/)).
2. Install **Apache Maven**: [install guide](https://maven.apache.org/install.html),
   or on Windows `winget install Apache.Maven` (then open a fresh terminal).

---

## 4. How to run

### Option A — VS Code
1. `File → Open Folder…` and select this `StockExchange` folder.
2. Wait for the bottom status bar to finish importing the Maven project and show
   **Java: Ready** (first time it downloads JavaFX + JUnit — needs internet).
   - If a popup offers to "upgrade to the latest Java runtime", click **Not Now**
     (this project intentionally targets Java 17 so JavaFX 21 matches the runtime).
3. Open `src/main/java/com/nust/exchange/app/Main.java` and click **▶ Run**
   above the `main` method.

### Option B — command line
From this folder:
```bash
mvn clean javafx:run
```

---

## 5. Login (default accounts)

On first run a default market and these accounts are seeded automatically:

| Username | Password | Role | Notes |
|----------|----------|------|-------|
| `admin` | `admin123` | Admin | Can list new stocks; sees the Admin tab |
| `alice` | `pass` | Retail | |
| `bilal` | `pass` | Retail | |
| `megafund` | `pass` | Institutional | Larger order limits |

You can also create a new account from the **Register** button on the login screen.

---

## 6. Running the tests

```bash
mvn test
```
Covers the domain model, the matching engine (priority, partial fills,
conservation), the multi-threaded concurrency invariant, and the persistence
round-trip. In VS Code you can also use the **Testing** (flask) icon in the
sidebar to run them with the Extension Pack for Java.

---

## 7. Project structure

```
StockExchange/
├── pom.xml                     Maven build (JavaFX + JUnit)
├── README.md

├── data/                       Runtime saved state (created on first save)
└── src/
    ├── main/java/com/nust/exchange/
    │   ├── app/          Main (launcher), App (JavaFX lifecycle)
    │   ├── model/        Stock, Order/Market/Limit, Trader/Retail/Institutional/Admin, Portfolio, Holding, Trade
    │   ├── engine/       Exchange (singleton), OrderBook, MatchingEngine, PriceSimulator, BotTrader
    │   ├── enums/        OrderSide, OrderType, OrderStatus, AssetType
    │   ├── persistence/  Repository, CsvRepository, BinaryRepository, mappers, DataStore, TransactionLogger
    │   ├── patterns/     MarketListener (Observer), OrderFactory (Factory)
    │   ├── exceptions/   TradingException + Insufficient*/InvalidOrder
    │   ├── ui/           LoginView, DashboardView
    │   └── util/         IdGenerator, Money
    ├── main/resources/   styles.css
    └── test/java/...     JUnit tests
```

---

## 8. UML Architecture

The complete class diagram showing the exact architecture (including models, engines, persistence mapping, and JavaFX controllers) is available in.


## 8. Data files (created at runtime in `data/`)

| File | Format | Contents |
|------|--------|----------|
| `stocks.csv` | CSV | listed instruments + prices |
| `trades.csv` | CSV | full trade history |
| `traders.dat` | binary | trader accounts + portfolios + holdings |
| `transactions.log` | text (append) | live audit trail of every trade |

State saves automatically on exit and loads on startup. Delete the `data/`
folder to reset to the default seeded market.

---

## 9. Troubleshooting

- **`UnsupportedClassVersionError … javafx/application/Application`** — the
  runtime is older than the JavaFX build. This project uses JavaFX 21 to match a
  Java 21 runtime; make sure your JDK is 17+ and let VS Code re-import
  (Command Palette → *Java: Clean Java Language Server Workspace*).
- **`mvn` not recognized** — Maven isn't installed/on PATH. Use Option A (VS Code)
  or install Maven (Section 3).
- **Cannot resolve `org.openjfx:javafx-controls`** — no internet on first build;
  connect and re-import/re-run.
- **UI won't launch from a plain `java` command** — run via `Main` (not `App`),
  or use `mvn javafx:run`. The `Main` launcher keeps JavaFX on the classpath.

---