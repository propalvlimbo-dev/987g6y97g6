package ru.elytrix.duelsbridge.shared;
import java.util.*;
public class SlotAllocatorTest {
    static void check(boolean b) { if (!b) throw new AssertionError(); }
    public static void main(String[] args) {
        SlotAllocator slots = new SlotAllocator();
        check(slots.configure(Collections.nCopies(8, true)));
        UUID[] ids = new UUID[5]; for (int i=0;i<ids.length;i++) ids[i]=UUID.randomUUID();
        for (int i=0;i<4;i++) check(slots.reserve(ids[i]).orElse(-1)==i*2);
        check(slots.reserve(ids[0]).orElse(-1)==0);
        check(slots.reserve(ids[4]).isEmpty());
        check(!slots.configure(Collections.nCopies(6,true)));
        slots.release(ids[1]);
        check(slots.reserve(ids[4]).orElse(-1)==2);
        slots.release(ids[1]);
        check(slots.reserve(UUID.randomUUID()).isEmpty());
        for (UUID id:ids) slots.release(id);
        check(slots.configure(Arrays.asList(true,true,true,false,true,true)));
        check(slots.reserve(ids[0]).orElse(-1)==0);
        check(slots.reserve(ids[1]).orElse(-1)==4);
        check(slots.reserve(ids[2]).isEmpty());
        System.out.println("Shared-world slot allocator prototype tests passed");
    }
}
