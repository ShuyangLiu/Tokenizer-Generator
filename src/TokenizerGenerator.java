import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

public class TokenizerGenerator
{

    private class Pair <T1, T2>
    {
        T1 fst;
        T2 snd;

        Pair() { }

        Pair(T1 fst, T2 snd) {
            this.fst = fst;
            this.snd = snd;
        }
    }

    private int state_count;
    private String [] terminals;
    private HashMap <Pair<Integer, Character>, Integer> DFA;
    private HashMap <Pair<Integer, Character>, ArrayList<Integer>> NFA;

    private HashMap <Integer, String> accepting_states;
    private ArrayList <Integer> fake_initial_states;

    TokenizerGenerator(String[] terms) {
        this.state_count = 1; // state 0 is reserved for initial state
        this.terminals = terms;
        accepting_states = new HashMap<>();
        fake_initial_states = new ArrayList<>();
        NFA = new HashMap<>();
        DFA = new HashMap<>();
        build_NFA();
        NFAtoDFA();
    }

    private void build_NFA()
    {
        for (String term:terminals) {
            fake_initial_states.add(state_count);
            for (int i = 0; i < term.length(); i++) {
                char c = term.charAt(i);
                Pair<Integer, Character> pair = new Pair<>();
                pair.fst = state_count;
                pair.snd = c;
                ArrayList<Integer> l = new ArrayList<>();
                l.add(state_count+1);
                NFA.put(pair, l);
                state_count+=1;
            }
            accepting_states.put(state_count, term);
            state_count+=1;
        }

        // use epsilon to union them together
        Pair <Integer, Character> pair = new Pair<>();
        pair.fst = 0;
        pair.snd = Character.MIN_VALUE; // representing epsilon
        ArrayList<Integer> list = new ArrayList<>(fake_initial_states);
        NFA.put(pair, list);
    }

    // FIXME: currently only support one-level merge, need to make it recursive
    private Integer merge(Integer[] toBeMerged)
    {
        // merge a list of states into one
        // then update it to DFA table
        Integer new_state = state_count+1;
        Set <Pair<Integer, Character>> nfa_keys = NFA.keySet();
        for(Pair<Integer, Character> key : nfa_keys){
            for (Integer state:toBeMerged) {
                if(key.fst.equals(state)){
                    Pair <Integer, Character> pair = new Pair<>();
                    pair.fst = new_state;
                    pair.snd = key.snd;
                    DFA.put(pair, NFA.get(key).get(0));
                }
            }
        }
        state_count+=1;
        return new_state;
    }

    // FIXME: Currently only support terminals with at most one common prefix
    private void NFAtoDFA()
    {
        Set <Pair<Integer, Character>> nfa_keys = NFA.keySet();
        Set <Character> input_chars = new HashSet<>();
        for(Integer fake_start : fake_initial_states){
            for(Pair<Integer, Character> key : nfa_keys){
                if(key.fst.equals(fake_start)){
                    Character in = key.snd;
                    input_chars.add(in);
                }
            }
        }
        for (Character c:input_chars) {
            Set <Integer> need_to_be_merged = new HashSet<>();
            for(Pair<Integer, Character> key : nfa_keys){
                if (key.snd == c && fake_initial_states.contains(key.fst)){
                    need_to_be_merged.add(NFA.get(key).get(0));
                }
            }
            Integer new_state = merge(need_to_be_merged.toArray(new Integer[0]));
            Pair <Integer, Character> pair = new Pair<>();
            pair.fst = 0;
            pair.snd = c;
            DFA.put(pair, new_state);
        }
    }

    private void err()
    {
        System.out.println("Parse error");
        System.exit(0);
    }

    private Integer nextState(Character c, Integer current)
    {
        // look up the DFA table and returns the value
        return DFA.get(new Pair<>(current, c));
    }

    public String next_token() throws IOException
    {
        char c = ' ';
        int c_i;

        // Skip white spaces
        while(c == ' '
                || c == '\n'
                || c == '\r'
                || c == '\t'){
            c_i = System.in.read();
            if(c_i == -1){
                // EOF
                return "EOF";
            }
            c = (char) c_i;
        }

        Integer state = nextState(c, 0); // starts at state 0
        while (!accepting_states.containsKey(state)){
            c_i = System.in.read();
            if(c_i == -1){
                // EOF
                err();
            }
            c = (char) c_i;
            state = nextState(c, state);
        }
        return (accepting_states.get(state)); // return null if key does not exist
    }


}
