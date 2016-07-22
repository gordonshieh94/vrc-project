package ca.sfu.teambeta.logic;

import java.util.*;
import java.util.stream.Collectors;

import ca.sfu.teambeta.core.Pair;
import ca.sfu.teambeta.core.Penalty;
import ca.sfu.teambeta.core.Scorecard;

/**
 * Created by Gordon Shieh on 25/06/16.
 */
public class VrcLadderReorderer implements LadderReorderer {
    @Override
    public List<Pair> reorder(List<Pair> originalPairs, List<Scorecard> scorecards,
                              Set<Pair> activePairs, Map<Pair, Penalty> penalties) {

        List<Pair> intermediateOrdering = swapBetweenGroups(scorecards);

        intermediateOrdering = mergeActivePairs(
                originalPairs, intermediateOrdering);

        intermediateOrdering = applyPassivePenalty(
                originalPairs, intermediateOrdering, activePairs);

        return applyPenalties(intermediateOrdering, penalties);
    }

    private List<Pair> swapBetweenGroups(List<Scorecard> scorecards) {
        List<Pair> completedPairs = new ArrayList<>();
        List<Pair> previousGroup = scorecards.get(0).getReorderedPairs();

        for (int i = 1; i < scorecards.size(); i++) {
            // Swap the player's in the first and last position of subsequent groups
            List<Pair> currentGroup = scorecards.get(i).getReorderedPairs();
            int lastIndexOfFirstGroup = previousGroup.size() - 1;

            Pair temp = previousGroup.get(lastIndexOfFirstGroup);

            previousGroup.set(lastIndexOfFirstGroup, currentGroup.get(0));
            currentGroup.set(0, temp);

            completedPairs.addAll(previousGroup);
            previousGroup = currentGroup;
        }

        // The for loop omits the last group, thus add it now:
        completedPairs.addAll(previousGroup);

        return completedPairs;
    }

    private List<Pair> mergeActivePairs(List<Pair> originalPairs, List<Pair> activeReorderedPairs) {
        int activePairIndex = 0;
        for (int i = 0; i < originalPairs.size(); i++) {
            Pair pair = originalPairs.get(i);
            if (activeReorderedPairs.contains(pair)) {
                originalPairs.set(i, activeReorderedPairs.get(activePairIndex));
                activePairIndex++;
            }
        }
        return originalPairs;
    }

    private List<Pair> applyPassivePenalty(
            List<Pair> originalPairs, List<Pair> activeReorderedPairs,
            Set<Pair> activePairs) {

        Set<Pair> passivePairs = originalPairs.stream()
                .filter(pair -> !activePairs.contains(pair))
                .collect(Collectors.toCollection(LinkedHashSet::new));
        for (Pair pair : passivePairs) {
            int index = activeReorderedPairs.indexOf(pair);
            int newIndex = index + 2;
            if (newIndex > activeReorderedPairs.size() - 1) {
                newIndex = activeReorderedPairs.size() - 1;
            }

            activeReorderedPairs.remove(index);
            activeReorderedPairs.add(newIndex, pair);
        }
        return activeReorderedPairs;
    }

    private List<Pair> applyPenalties(
            List<Pair> activeReorderedPairs, Map<Pair, Penalty> penalties) {

        List<Penalty> availablePenalties = penalties.entrySet().stream()
                .map(Map.Entry::getValue)
                .sorted((penalty1, penalty2) -> penalty1.getPenalty() - penalty2.getPenalty())
                .collect(Collectors.toList());

        for (Penalty penalty : availablePenalties) {
            Set<Pair> penaltyPairs = penalties.entrySet().stream()
                    .filter(pairPenaltyEntry -> pairPenaltyEntry.getValue() == penalty)
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toSet());

            for (Pair pair : penaltyPairs) {
                int index = activeReorderedPairs.indexOf(pair);
                int newIndex = index + penalty.getPenalty();
                if (newIndex > activeReorderedPairs.size() - 1) {
                    newIndex = activeReorderedPairs.size() - 1;
                }

                activeReorderedPairs.remove(index);
                activeReorderedPairs.add(newIndex, pair);
            }
        }

        return activeReorderedPairs;
    }
}
