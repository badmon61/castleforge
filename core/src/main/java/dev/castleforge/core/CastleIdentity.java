package dev.castleforge.core;

import java.util.*;
import static dev.castleforge.core.CastlePlanner.Condition;

/**
 * Versioned narrative identity for a hypothetical castle site.
 * Pure data: it neither spawns inhabitants nor edits, loads or samples terrain.
 * Names are project-authored syllable combinations, not imported schematics.
 */
public final class CastleIdentity {
    public static final int VERSION = 1;
    public enum Occupancy { INHABITED, SPARSE, ABANDONED }
    public record Event(int yearsAgo, String key, String description) {}
    public record Identity(String name, String dynasty, String heraldry, String culture,
                           String cultureRegionId, int ageYears, Condition condition,
                           Occupancy expectedOccupancy, List<Event> events) {
        public Identity { events = List.copyOf(events); }
        public String oneLine() {
            return name + " / " + dynasty + " / " + ageYears + " years / " + condition
                + " / planned population: " + expectedOccupancy;
        }
    }
    private record Names(String title, String housePrefix, String[] roots, String[] suffixes) {}
    private static Names names(String style) {
        return switch (style) {
            case "nordic" -> new Names("Hold", "Clan", new String[]{"Frost","Storm","Raven","Skald","Winter","Iron"}, new String[]{"holm","gard","fjell","vik","heim","havn"});
            case "mediterranean" -> new Names("Castello", "House", new String[]{"Monte","Vela","Rosa","Luna","Bel","Porto"}, new String[]{"vento","mar","fiore","castro","verde","alto"});
            case "desert_citadel" -> new Names("Citadel", "House", new String[]{"Qasr","Sahar","Nur","Rimal","Zafir","Miraj"}, new String[]{"ara","edin","amir","sahir","nura","zan"});
            case "east_asian" -> new Names("Fortress", "House", new String[]{"Kiri","Hoshi","Mizu","Kaze","Taka","Yama"}, new String[]{"yama","kawa","mori","shima","sora","tani"});
            case "gothic" -> new Names("Bastion", "House", new String[]{"Schatten","Nebel","Raben","Nacht","Dorn","Aschen"}, new String[]{"fels","wacht","turm","krone","stein","hain"});
            case "high_fantasy" -> new Names("Spire", "House", new String[]{"Aether","Elden","Lumi","Valen","Caeli","Solis"}, new String[]{"dorn","spire","myr","thal","ria","wyn"});
            case "maritime" -> new Names("Fort", "House", new String[]{"Sturm","Wellen","Tiden","Wind","Salz","Falken"}, new String[]{"klippe","hafen","wacht","fels","werft","holm"});
            default -> new Names("Castle", "House", new String[]{"Falken","Hohen","Eichen","Greifen","Rosen","Silber"}, new String[]{"fels","wacht","burg","stein","hain","krone"});
        };
    }
    private static final String[] HERALDRY = {
        "silver falcon on blue", "golden sun on black", "white stag on green", "red tower on ivory",
        "black raven on silver", "golden lion on crimson", "three stars on purple", "silver wave on teal"
    };
    private static int pick(long value, int size) { return Math.floorMod(value, size); }

    public static Identity generate(long worldSeed, int x, int z, CultureMap.Region region, Condition condition) {
        Objects.requireNonNull(region, "region");
        Objects.requireNonNull(condition, "condition");
        Names n = names(region.style());
        long site = CultureMap.hash(worldSeed ^ 0x434153544c454944L, x, z);
        long kingdom = CultureMap.mix(worldSeed ^ 0x4b494e47444f4d31L ^ CultureMap.mix(region.id().hashCode()));
        String name = n.title + " " + n.roots[pick(site,n.roots.length)]
            + n.suffixes[pick(CultureMap.mix(site^17L),n.suffixes.length)];
        String dynasty = n.housePrefix + " " + n.roots[pick(kingdom,n.roots.length)]
            + n.suffixes[pick(CultureMap.mix(kingdom^19L),n.suffixes.length)];
        String heraldry = HERALDRY[pick(CultureMap.mix(kingdom^23L),HERALDRY.length)];
        int age = 120 + pick(CultureMap.mix(site^29L),681);
        Occupancy occupancy = switch(condition) {
            case RESTORED -> Occupancy.INHABITED;
            case WEATHERED -> Occupancy.SPARSE;
            case RUINED -> Occupancy.ABANDONED;
        };
        List<Event> events = new ArrayList<>();
        events.add(new Event(age, "founded", "A fortified watchpost was established."));
        int extension = age * (47 + pick(CultureMap.mix(site^31L),16)) / 100;
        events.add(new Event(extension, "expanded", "The garrison was enlarged with halls and additional defenses."));
        if (condition == Condition.RESTORED) {
            events.add(new Event(Math.max(5, age/9), "restored", "Recent repairs renewed the roofs, gate and inhabited quarters."));
        } else if (condition == Condition.WEATHERED) {
            events.add(new Event(Math.max(10, age/7), "decline", "The household dwindled and maintenance gradually ceased."));
        } else {
            int siege = Math.max(25, age/4);
            events.add(new Event(siege, "siege", "An old siege damaged the defenses and several residential wings."));
            events.add(new Event(Math.max(2, siege/4), "abandoned", "The remaining residents finally left the fortress."));
        }
        for (int i=1;i<events.size();i++)
            if (events.get(i).yearsAgo() >= events.get(i-1).yearsAgo())
                throw new IllegalStateException("History chronology invalid");
        return new Identity(name, dynasty, heraldry, region.style(), region.id(), age, condition, occupancy, events);
    }
    private CastleIdentity() {}
}
