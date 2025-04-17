package dev.jsinco.luma.lumaevents.npc.obj;

import dev.jsinco.luma.lumaevents.tokens.TokenExchanging;
import dev.jsinco.luma.lumaevents.tokens.TokenExchanging.TokenType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.bukkit.entity.Player;

import java.time.LocalDate;
import java.time.Month;
import java.util.Calendar;

/**
 * Okay, to make my job a little bit easier, Anais is only going to buy X amount of carrots
 * for 1 Easter Basket. Where X fluctuates based on the day of the week.
 * <br />
 * We're going to go ahead and set the median price at 6, which will get a player about 21 Easter
 * Baskets if they were to trade with Anais on that day.
 */
@Getter
public class StalkMarketDay {

    private static final int YEAR = LocalDate.now().getYear();

    private final LocalDate date;
    private final int price;


    public StalkMarketDay(int day, Month month, int price) {
        this.date = LocalDate.of(YEAR, month.num, day);
        this.price = price;
    }


    // Rounds down to the nearest divisible integer
    public int nearestDivisible(int input) {
        return input - (input % this.price);
    }


    // Trades the specified number of carrots for a basket based on the price
    public boolean trade(Player player, int input) {
        int total = TokenExchanging.getAmount(player, TokenType.CARROT);
        int amount = input - (input % this.price);

        if (total < price || total < amount) {
            return false;
        }

        if (!TokenExchanging.take(player, TokenType.CARROT, amount)) {
            return false;
        }

        int basketCount = amount / this.price;
        TokenExchanging.give(player, TokenType.BASKET, basketCount);
        return true;
    }


    @AllArgsConstructor
    public enum Month {
        JAN(1), FEB(2), MAR(3), APR(4),
        MAY(5), JUN(6), JUL(7), AUG(8),
        SEP(9), OCT(10), NOV(11), DEC(12);

        private final int num;
    }

}
