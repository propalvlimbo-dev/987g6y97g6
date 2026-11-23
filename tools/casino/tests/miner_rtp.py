from functools import lru_cache

@lru_cache(None)
def value(coins, empty, collected):
    if not coins:
        return 1.4 * collected
    remaining = coins + empty + 14
    continuation = coins / remaining * value(coins - 1, empty, collected + 1)
    if empty:
        continuation += empty / remaining * value(coins, empty - 1, collected)
    # Losing on a mine contributes zero. At zero collected, cashout is disallowed,
    # but max(0, continuation) is equivalent because continuation is nonnegative.
    return max(1.4 * collected, continuation)

rtp = value(20, 11, 0)
assert abs(rtp - 0.9483065953654186) < 1e-12
print(f'Optimal miner RTP before rounding: {rtp:.8%}')
