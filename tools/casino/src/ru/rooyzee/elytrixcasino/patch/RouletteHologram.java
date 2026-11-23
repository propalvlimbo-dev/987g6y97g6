package ru.rooyzee.elytrixcasino.patch;
import java.util.*;
import dev.by1337.virtualentity.api.virtual.display.VirtualTextDisplay;
import ru.rooyzee.elytrixcasino.util.Text;

/** Visual failures must never interrupt payout, animation or NPC return. */
public final class RouletteHologram {
    private static final Map<VirtualTextDisplay,List<String>> lastLines=new WeakHashMap<>();
    private static final Set<VirtualTextDisplay> watching=Collections.newSetFromMap(new WeakHashMap<>());
    private static final Map<VirtualTextDisplay,String> reaction=new WeakHashMap<>();
    public static void watch(VirtualTextDisplay display) { try { if(display!=null) watching.add(display); } catch(Throwable ignored) {} }
    public static void react(VirtualTextDisplay display,String text) { try { if(display!=null) reaction.put(display,text); } catch(Throwable ignored) {} }
    public static void clearReaction(VirtualTextDisplay display) { try { if(display!=null) reaction.remove(display); } catch(Throwable ignored) {} }
    public static List<String> lines(String state,List<String> defaults) {
        if("WALKING".equals(state)) return Collections.singletonList("&f*идёт крутить рулетку*");
        if("SPINNING".equals(state)) return Collections.singletonList("&f*крутит рулетку*");
        if("RETURNING".equals(state)) return Collections.emptyList();
        return defaults;
    }
    public static void update(VirtualTextDisplay display,Enum<?> state,List<String> defaults) {
        if(display==null || state==null) return;
        try {
            String now=state.name();
            if(now.equals("WALKING") || now.equals("IDLE")) watching.remove(display);
            List<String> desired;
            if(now.equals("SPINNING") && watching.contains(display)) desired=Collections.singletonList("&f*следит за рулеткой*");
            else if(now.equals("RETURNING") && reaction.containsKey(display)) desired=Collections.singletonList("&f"+reaction.get(display));
            else desired=lines(now,defaults);
            if(Objects.equals(desired,lastLines.get(display))) return;
            display.setText(Text.linesComponent(desired));
            lastLines.put(display,new ArrayList<>(desired));
        } catch(Throwable ignored) { /* cosmetic error must not stop the game */ }
    }
}
