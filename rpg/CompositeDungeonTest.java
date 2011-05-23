package rpg;

import rpg.util.*;
import rpg.exceptions.*;

import static org.junit.Assert.*;
import org.junit.*;

import java.util.Map;

public class CompositeDungeonTest {

    Level<TransparentSquare> transparentLevel;
    TransparentSquare transSq1;
    TransparentSquare transSq2;
    TransparentSquare transSq3;

    Level<TeleportationSquare> teleportLevel;
    RegularTeleportationSquare regTelSq1;
    RegularTeleportationSquare regTelSq2;
    RegularTeleportationSquare regTelSq3;

    Shaft<RegularSquare> regularShaft;
    RegularSquare regSq1;
    RegularSquare regSq2;
    RegularSquare regSq3;

    CompositeDungeon<Square> dungeon;

    @Before
    public void setUpMutableFixture() {
        transparentLevel = new Level<TransparentSquare>(2, 2);
        transSq1 = new TransparentSquare();
        transSq2 = new TransparentSquare();
        transSq3 = new TransparentSquare();
        transparentLevel.addSquareAt(new Coordinate(0, 1, 0), transSq1);
        transparentLevel.addSquareAt(new Coordinate(1, 0, 0), transSq2);
        transparentLevel.addSquareAt(new Coordinate(1, 1, 0), transSq3);

        teleportLevel = new Level<TeleportationSquare>(2, 2);
        regTelSq1 = new RegularTeleportationSquare(new Teleporter(transSq1));
        regTelSq2 = new RegularTeleportationSquare(new Teleporter(transSq2));
        regTelSq3 = new RegularTeleportationSquare(new Teleporter(transSq3));
        teleportLevel.addSquareAt(new Coordinate(0, 1, 0), regTelSq1);
        teleportLevel.addSquareAt(new Coordinate(1, 0, 0), regTelSq2);
        teleportLevel.addSquareAt(new Coordinate(1, 1, 0), regTelSq3);

        regularShaft = new Shaft<RegularSquare>(new Coordinate(0, 1, 1),
                                                3, Direction.UP);
        regSq1 = new RegularSquare(); 
        regSq2 = new RegularSquare(); 
        regSq3 = new RegularSquare(); 
        regularShaft.addSquareAt(new Coordinate(0,1,1), regSq1);
        regularShaft.addSquareAt(new Coordinate(0,1,2), regSq2);
        regularShaft.addSquareAt(new Coordinate(0,1,3), regSq3);

        CoordinateSystem coordSyst = new CoordinateSystem(
                new Coordinate(0, 0, 0),
                new Coordinate(9, 9, 9));
        dungeon = new CompositeDungeon<Square>(coordSyst);
    }

    private void assertClassInvariants(CompositeDungeon<?> compositeDungeon) {
        assertTrue(compositeDungeon.isNotRaw());
    }

    @Test
    public void constructor_legal() {
        CoordinateSystem coordSyst = new CoordinateSystem(
                new Coordinate(0, 0, 0),
                new Coordinate(9, 9, 9));
        CompositeDungeon<Square> dungeon = 
                                new CompositeDungeon<Square>(coordSyst);
        assertClassInvariants(dungeon);
    }

    @Test
    public void getDirectionsAndNeighboursOf_legal() {
        Map<Direction, Square> map;
        map = dungeon.getDirectionsAndNeighboursOf(new Coordinate(0, 0, 0));
        assertTrue(map.isEmpty());

        dungeon.addSubDungeonAt(teleportLevel, new Coordinate(0,0,0));
        map = dungeon.getDirectionsAndNeighboursOf(new Coordinate(0, 1, 0));
        assertEquals(regTelSq3, map.get(Direction.EAST));
        assertEquals(1, map.size());

        dungeon.addSubDungeonAt(regularShaft, new Coordinate(0,0,0));
        map = dungeon.getDirectionsAndNeighboursOf(new Coordinate(0, 1, 0));
        assertEquals(regTelSq3, map.get(Direction.EAST));
        assertEquals(regSq1, map.get(Direction.UP));
        assertEquals(2, map.size());
    }


}

// vim: ts=4:sw=4:expandtab:smarttab

