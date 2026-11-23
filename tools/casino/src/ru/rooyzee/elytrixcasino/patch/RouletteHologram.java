package ru.rooyzee.elytrixcasino.patch;
import java.util.*;
import dev.by1337.virtualentity.api.virtual.display.VirtualTextDisplay;
import ru.rooyzee.elytrixcasino.util.Text;

/** The display is already tracked by VirtualEntityApi; only update text on a state transition. */
public final class RouletteHologram {
    private static final Map<VirtualTextDisplay,String> lastState=new WeakHashMap<>();
    public static List<String> lines(String state,List<String> defaults) {
        if("WALKING".equals(state)) return Collections.singletonList("&f*идёт крутить рулетку*");
        if("SPINNING".equals(state)) return Collections.singletonList("&f*крутит рулетку*");
        if("RETURNING".equals(state)) return Collections.emptyList();
        return defaults;
    }
    public static void update(VirtualTextDisplay display,Enum<?> state,List<String> defaults) {
        if(display==null || state==null) return;
        String now=state.name();
        if(now.equals(lastState.get(display))) return;
        display.setText(Text.linesComponent(lines(now,defaults)));
        lastState.put(display,now);
    }
}
