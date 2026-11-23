# ElytrixCasino 2.2.1-fix1

Patch for the original `ElytrixCasino-2.2.1.jar` (SHA-256 `f21984324b15c1a34d866a66facd2845e5724562467245b03287161c285038d9`). It retains the original plugin and patches its bytecode because upstream sources are not present.

* Bets are locked per player and per game, not per player globally. Automate GUI, result and animation are per player even if the physical machine is shared. A player can have only one outstanding spin at a time.
* Spin sounds only play while that particular GUI is being viewed. Closing the GUI does not cancel the result. On ordinary plugin disable or plugin reload, pending spins are completed once before teardown, including offline pending payouts. Sudden process crashes cannot be made transactionally safe with the original Vault/data storage.
* Roulette uses independent weighted outcomes x0/x2/x3/x5 with probabilities 63/24/9/4 percent: expected payout 95% before rounding. The wheel is weighted, not uniform across visual segments.
* Slots have a 50% chance of one line, otherwise none. Conditional winning line payouts x1.2/x1.5/x2/x3/x6 have probabilities 30/34/20/11/5 percent respectively. Expected payout is 95% before rounding. No player-history adjustment. The first five distinct configured symbol names/icons are used; their configured odds and payouts are not used.
* Miner has 14 mines, 20 coins, 11 empty cells; each coin adds 1.4 times the bet. The maximum expected payout under optimal stop/continue decisions is approximately 94.83% before rounding. Normal shutdown cashes out collected coins rather than granting the higher of stake and collected coins, preventing repeated reload refunds. A miner game with no collected coins therefore pays nothing on shutdown.
* Balance-critical `roulette.sectors`, slot weights/pays/maximum and `miner.field` settings are overridden by the above rules even in an existing config. Appearance, bets, animation timing and other configuration still apply. Payouts are rounded down to prevent small-bet rounding exploits. No finite batch of games guarantees a casino profit.

## Build and check

Use Java 21 and an Eclipse JDT compiler jar, for example:

```
JAVA=/path/to/java ECJ=/path/to/ctxo-jdt-analyzer-17.jar \
  tools/casino/build.sh /path/to/ElytrixCasino-2.2.1.jar
```

The build checks the input hash and patch match counts, statically verifies bytecode, runs isolated concurrency, game math, GUI visibility and spin shutdown tests, and writes `finalbuild/ElytrixCasino.jar`. The stubs are compile/test-only and never packed into the plugin. These are not a substitute for testing against the actual Paper/Leaf server, Vault economy, VirtualEntityApi and BLib. Back up the server and economy data before deploying.
