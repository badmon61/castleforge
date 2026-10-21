package dev.castleforge.core.architecture;

public record QualityReport(int modules,int rooms,int flights,int windows,int blocks,int reachableRooms,int reservedCells) {
    public String compact() { return modules+" modules, "+rooms+" rooms ("+reachableRooms+" reachable), "+flights+
        " stair flights, "+windows+" windows; "+blocks+" planned cells, "+reservedCells+" protected route cells"; }
}
