package me.barnaby.trial.runnables;

import me.barnaby.trial.MarketPlace;
import org.bukkit.scheduler.BukkitRunnable;

public class BlackMarketRunnable extends BukkitRunnable {

    private final MarketPlace marketPlace;
    public BlackMarketRunnable(MarketPlace marketPlace) {
        this.marketPlace = marketPlace;
    }

    @Override
    public void run() {
        marketPlace.getMongoDBManager().moveItemsToBlackMarket();
    }
}
