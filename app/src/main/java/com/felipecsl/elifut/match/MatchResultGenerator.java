package com.felipecsl.elifut.match;

import android.util.Log;

import com.felipecsl.elifut.models.Club;
import com.felipecsl.elifut.models.ClubSquad;
import com.felipecsl.elifut.models.Goal;
import com.felipecsl.elifut.models.Goals;
import com.felipecsl.elifut.models.MatchResult;

import org.apache.commons.math3.distribution.RealDistribution;
import org.apache.commons.math3.random.RandomGenerator;
import org.apache.commons.math3.random.Well19937c;

import java.util.Collections;
import java.util.List;
import java.util.Locale;

/** Generates random results for matches */
public class MatchResultGenerator {
  private static final String TAG = MatchResultGenerator.class.getSimpleName();
  private final RandomGenerator random;
  private final RealDistribution goalsDistribution;

  public MatchResultGenerator() {
    this(new Well19937c(), MatchResult.GOALS_DISTRIBUTION);
  }

  public MatchResultGenerator(RandomGenerator random, RealDistribution goalsDistribution) {
    this.random = random;
    this.goalsDistribution = goalsDistribution;
  }

  public MatchResult generate(Club home, ClubSquad homeSquad, Club away, ClubSquad awaySquad) {
    float result = random.nextFloat();
    MatchResult.Builder matchResult = MatchResult.builder();
    Club winner;
    List<Goal> winnerGoals;
    List<Goal> loserGoals;

    double ratingDifference = homeSquad.rating() - awaySquad.rating();
    double ratingDiffModifier = (ratingDifference * 0.7) / 100;
    double homeWinProbability = MatchResult.HOME_WIN_PROBABILITY + ratingDiffModifier;
    double drawProbability = homeWinProbability + MatchResult.DRAW_PROBABILITY;

    if (result <= homeWinProbability) {
      winner = home;
    } else if (result <= drawProbability) {
      winner = null;
    } else {
      winner = away;
    }

    Log.d(TAG, String.format(Locale.getDefault(),
        "%s X %s\nHome winning Odds=%.2f, Away winning odds=%.2f\nWinner=%s, Result=%.2f",
        home, away, homeWinProbability * 100, (1 - drawProbability) * 100,
        winner, result * 100));

    boolean isHomeWin = home.equals(winner);
    Club loser = isHomeWin ? away : home;
    int totalGoals = Math.max((int) Math.floor(goalsDistribution.sample()), 0);

    if (winner != null) {
      if (totalGoals <= 2) {
        // 1x0 or 2x0
        winnerGoals = Goals.create(random, Math.max(totalGoals, 1), winner);
        loserGoals = Collections.emptyList();
      } else {
        // 3+ goals (eg.: 3x1, 3x0, 4x0, etc)
        loserGoals = Goals.create(random, random.nextInt(Math.max(1, (totalGoals / 2) + 1)), loser);
        winnerGoals = Goals.create(random, totalGoals - loserGoals.size(), winner);
      }
    } else {
      // draw (0x0, 1x1, 2x2, etc)
      int evenGoals = (totalGoals % 2 == 0) ? totalGoals : totalGoals + 1;
      winnerGoals = Goals.create(random, evenGoals / 2, home);
      loserGoals = Goals.create(random, evenGoals / 2, away);
    }

    List<Goal> homeGoals = isHomeWin ? winnerGoals : loserGoals;
    List<Goal> awayGoals = isHomeWin ? loserGoals : winnerGoals;

    return matchResult
        .homeGoals(homeGoals)
        .awayGoals(awayGoals)
        .build(home, away);
  }
}
