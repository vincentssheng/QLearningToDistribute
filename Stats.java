import java.util.ArrayList;

class Stats {
    String state;
    int count;
    double[] q_values;
    int[] action_counts;
    ArrayList<Integer> legalActions;

    public Stats(String state, int count, double[] q_values, int[] action_counts, ArrayList<Integer> legalActions) {
        this.state = state;
        this.count = count;
        this.q_values = q_values;
        this.action_counts = action_counts;
        this.legalActions = legalActions;
    }
}