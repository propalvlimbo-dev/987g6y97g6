import ru.rooyzee.elytrixairdrop.patch.NearbyRewards;

public class NearbyRewardsTest {
    public static void main(String[] args) {
        check(0, 3); check(3, 3); check(4, 5); check(6, 5); check(7, 8); check(100, 8);
        System.out.println("NearbyRewards thresholds OK");
    }
    private static void check(int players, int expected) {
        int actual = NearbyRewards.rewardCount(players);
        if (actual != expected) throw new AssertionError(players + ": " + actual + " != " + expected);
    }
}
