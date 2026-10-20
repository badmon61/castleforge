package dev.castleforge.core;

import java.util.*;
import static dev.castleforge.core.CastlePlanner.Condition;

/** Independent identity/telemetry checks; pure Java 21, no Minecraft runtime. */
public final class IdentityVerification {
    private static int assertions;
    private static void check(boolean pass, String text) {
        assertions++;
        if(!pass) throw new AssertionError(text);
    }
    public static void main(String[] args) {
        String[] styles={"central_european","nordic","mediterranean","desert_citadel","east_asian",
            "gothic","high_fantasy","maritime"};
        Set<String> names=new HashSet<>(), emblems=new HashSet<>(), dynasties=new HashSet<>();
        for(int seed=0;seed<100;seed++) for(String style:styles) {
            var region=new CultureMap.Region("kingdom-"+seed+"-"+style,style,style,0,0,0,7168);
            for(Condition condition:Condition.values()) {
                var a=CastleIdentity.generate(seed,seed*100,-seed*37,region,condition);
                var b=CastleIdentity.generate(seed,seed*100,-seed*37,region,condition);
                check(a.equals(b),"stable site identity");
                check(a.ageYears()>=120 && a.ageYears()<=800,"plausible age range");
                check(a.events().size()>=3,"historical milestones");
                check(a.events().getFirst().yearsAgo()==a.ageYears(),"foundation starts chronology");
                for(int i=1;i<a.events().size();i++)
                    check(a.events().get(i).yearsAgo()<a.events().get(i-1).yearsAgo(),"history sorted oldest first");
                check(a.name().length()>7,"name produced");
                check(a.culture().equals(style),"original culture retained");
                check(a.expectedOccupancy()==switch(condition) {
                    case RESTORED -> CastleIdentity.Occupancy.INHABITED;
                    case WEATHERED -> CastleIdentity.Occupancy.SPARSE;
                    case RUINED -> CastleIdentity.Occupancy.ABANDONED;
                }, "intended population follows maintenance");
                names.add(a.name()); dynasties.add(a.dynasty()); emblems.add(a.heraldry());
            }
            var a=CastleIdentity.generate(seed,0,0,region,Condition.RESTORED);
            var b=CastleIdentity.generate(seed,10000,5000,region,Condition.RUINED);
            check(a.dynasty().equals(b.dynasty()) && a.heraldry().equals(b.heraldry()),
                "same region shares lineage across castle sites");
        }
        check(names.size()>120,"names differ across culture and seed");
        check(dynasties.size()>40,"kingdoms get distinct dynasties");
        check(emblems.size()==8,"all heraldic combinations represented");
        var region=new CultureMap.Region("special","gothic","nordic",0,0,0,7168);
        check(!CastleIdentity.generate(1,0,0,region,Condition.RUINED)
            .equals(CastleIdentity.generate(2,0,0,region,Condition.RUINED)),"seed changes identity");
        var plan=TerracedCastlePlanner.plan(42,0,0,(x,z)->64,region);
        var report=PlanDiagnostics.analyze(plan);
        check(report.width()>55 && report.length()>60 && report.height()>20,"large multi-court bounds");
        check(report.modules()==8 && report.navigationTargets()==plan.targets().size(),"module/target counts");
        check(report.plannedCells()==report.solids()+report.emptyCells(),"every voxel classified");
        check(report.storeys()>=13 && report.foundationCells()>=0,"room floors tracked");
        check(report.terraceRelief()==plan.relief(),"terrace relief unchanged");
        check(report.compact().contains("bridge links"),"plain text debug report");
        System.out.println("PASS identity/diagnostics: "+assertions+" assertions across 100 seeds and 8 cultures.");
    }
    private IdentityVerification() {}
}
