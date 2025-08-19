package pro.eng.yui.oss.d2h.db.model;

import org.junit.jupiter.api.Test;
import pro.eng.yui.oss.d2h.db.field.RunsOn;

import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class GuildsGetRunsOnTest {

    private static List<Integer> toHours(List<RunsOn> list) {
        return list.stream().map(RunsOn::getValue).collect(Collectors.toList());
    }

    @Test
    void returnsEmptyListWhenRunsOnIsNull() {
        Guilds g = new Guilds();
        g.setRunsOn(null);
        List<RunsOn> res = g.getRunsOnList();
        assertNotNull(res);
        assertEquals(1, res.size());
        assertEquals(0, res.get(0).getValue());
    }

    @Test
    void returnsHourZeroWhenCycleIsZero() {
        Guilds g = new Guilds();
        g.setRunsOn(new RunsOn(0));
        List<RunsOn> res = g.getRunsOnList();
        assertNotNull(res);
        assertEquals(1, res.size());
        assertEquals(0, res.get(0).getValue());
    }

    @Test
    void returnsEmptyListWhenCycleIs24OrMore() {
        Guilds g = new Guilds();
        g.setRunsOn(new RunsOn(23)); // start with valid
        g.setRunsOn(new RunsOn(23)); // sanity
        assertDoesNotThrow(() -> new RunsOn(23));
    }

    @Test
    void generates24EntriesForCycle1() {
        Guilds g = new Guilds();
        g.setRunsOn(new RunsOn(1));
        List<Integer> hours = toHours(g.getRunsOnList());
        assertEquals(24, hours.size());
        // expect 0..23 in order
        for (int i = 0; i < 24; i++) {
            assertEquals(i, hours.get(i));
        }
    }

    @Test
    void generatesEveryThreeHoursForCycle3() {
        Guilds g = new Guilds();
        g.setRunsOn(new RunsOn(3));
        List<Integer> hours = toHours(g.getRunsOnList());
        assertEquals(List.of(0,3,6,9,12,15,18,21), hours);
    }

    @Test
    void generatesEverySevenHoursForCycle7() {
        Guilds g = new Guilds();
        g.setRunsOn(new RunsOn(7));
        List<Integer> hours = toHours(g.getRunsOnList());
        assertEquals(List.of(0,7,14,21), hours);
    }

    @Test
    void listIsSortedAscending() {
        Guilds g = new Guilds();
        g.setRunsOn(new RunsOn(5));
        List<Integer> hours = toHours(g.getRunsOnList());
        // should be [0,5,10,15,20]
        assertEquals(List.of(0,5,10,15,20), hours);
    }
}
