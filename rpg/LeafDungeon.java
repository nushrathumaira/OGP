package rpg;

import rpg.exceptions.*;
import be.kuleuven.cs.som.annotate.*;
import java.util.Map;
import java.util.HashMap;
import java.util.EnumMap;

/**
 * A class representing a 'leaf' dungeon that consists of squares.
 *
 * @invar
 *   .............................
 *
 * @author Roald Frederickx
 */
public abstract class LeafDungeon<S extends Square> extends Dungeon<S> {

    /** 
     * Create a new leaf dungeon with the given coordinate system as its 
     * coordinate system. 
     * 
     * @param coordinateSystem
     * The coordinate system of this new leaf dungeon.
     * @effect
     *   | super(coordinateSystem)
     */
    @Raw @Model
    protected LeafDungeon(CoordinateSystem coordinateSystem)
                                        throws IllegalArgumentException {
        super(coordinateSystem);
    }

    /** 
     * Translate this leaf dungeon over the given offset.
     *
     * @param offset 
     * The offset over which to translate this leaf dungeon.
     */
    public void translate(Coordinate offset) throws IllegalArgumentException,
                                            CoordinateConstraintsException {
        translateCoordSyst(offset);
        for (Map.Entry<Coordinate, S> e : getPositionsAndSquares())
            if (!canHaveAsSquareAt(e.getKey().add(offset), e.getValue())){
                translateCoordSyst(offset.mirror());
                throw new CoordinateConstraintsException(e.getValue(), this);
            }
        Map<Coordinate, S> translatedSquares = new HashMap<Coordinate, S>();
        for (Map.Entry<Coordinate, S> e : getPositionsAndSquares()) {
            translatedSquares.put(e.getKey().add(offset), e.getValue());
        }
        squares = translatedSquares;
    }



    /** 
     * Add the given square to this dungeon at the given coordinate.
     *
     * @param coordinate 
     * The coordinate to add the given square at.
     * @param square 
     * The square to add at the given coordinate.
     * @effect
     * The squares that border the given coordinate in this dungeon get 
     * merged with the given square in the appropriate direction.
     *   | for each e in getDirectionsAndNeighboursOf(coordinate).entrySet() :
     *   |      square.mergeWith(e.getValue()), e.getKey()
     * @post
     *   | new.getSquareAt(coordinate) == square
     * @throws IllegalArgumentException
     *   | !canHaveAsSquareAt(coordinate, square)
     * @throws CoordinateOccupiedException
     *   | isOccupied(coordinate)
     * @throws DungeonConstraintsException
     * Adding the given square at the given coordinate would violate the 
     * constrainst as specified by squaresSatisfyConstraints().
     */
    public void addSquareAt(Coordinate coordinate, S square) 
                                        throws IllegalArgumentException,
                                                CoordinateOccupiedException,
                                                DungeonConstraintsException {
        if (!canHaveAsSquareAt(coordinate, square))
            throw new IllegalArgumentException();
        if (isOccupied(coordinate))
            throw new CoordinateOccupiedException(coordinate, this);

        squares.put(coordinate, square);

        if (!squaresSatisfyConstraints()){
            squares.remove(coordinate);
            throw new DungeonConstraintsException(square, this);
        }

        for (Map.Entry<Direction, S> neighbourEntry :
                        getDirectionsAndNeighboursOf(coordinate).entrySet()){
            S neighbour = neighbourEntry.getValue();
            Direction neighbourDirection = neighbourEntry.getKey();
            square.mergeWith(neighbour, neighbourDirection);
        }
    }


    /** 
     * Return a mapping of directions to squares that represent all 
     * neighbouring squares of the given coordinate in this dungeon. 
     * 
     * @param coordinate 
     * The coordinate whose neighbours to return.
     * @return
     * A mapping of directions to squares that represent all neighbouring 
     * squares of the given coordinate in this dungeon. 
     *   | for each e in result.entrySet() :
     *   |      e.getValue() == getSquareAt(coordinate.moveTo(e.getKey()))
     * @throws IllegalArgumentException
     *   | coordinate == null
     */
    @Raw
    public Map<Direction, S> getDirectionsAndNeighboursOf(
                                                        Coordinate coordinate)
                                            throws IllegalArgumentException {
        if (coordinate == null)
            throw new IllegalArgumentException();

        EnumMap<Direction, S> result =
                    new EnumMap<Direction, S>(Direction.class);

        for (Map.Entry<Direction, Coordinate> neighbourEntry :
                        getCoordSyst().neighboursOf(coordinate).entrySet()) {
            Coordinate neighbourCoordinate = neighbourEntry.getValue();
            Direction neighbourDirection = neighbourEntry.getKey();
            S neighbour = squares.get(neighbourCoordinate);
            if (neighbour != null)
                result.put(neighbourDirection, neighbour);
        }

        return result;
    }


    /** 
     * Returns the square at the given coordinate in this dungeon.
     * 
     * @param coordinate 
     * The coordinate of the square to return.
     * @throws IllegalArgumentException
     *   | !isPossibleSquareCoordinate(coordinate)
     * @throws CoordinateNotOccupiedException
     *   | !isOccupied(coordinate)
     */
    @Basic @Raw
    public S getSquareAt(Coordinate coordinate) 
                                    throws IllegalArgumentException,
                                            CoordinateNotOccupiedException {
        if (!isPossibleSquareCoordinate(coordinate))
            throw new IllegalArgumentException();

        S result = squares.get(coordinate);
        if (result == null)
            throw new CoordinateNotOccupiedException(coordinate, this);

        return result;
    }

    /** 
     * Returns wheter or not this dungeon contains the given square.
     * 
     * @param square 
     * The square to check.
     */
    @Basic @Raw
    public boolean hasSquare(S square) {
        return squares.containsValue(square);
    }

    /** 
     * Deletes the square at the given coordinate and terminates it.
     *
     * @param coordinate 
     * The coordinate to remove the square at.
     * @post
     *   | !isOccupied(coordinate)
     * @effect
     *   | old.getSquareAt(coordinate).terminate()
     */
    public void deleteSquareAt(Coordinate coordinate) 
                                    throws IllegalArgumentException,
                                            CoordinateNotOccupiedException {
        S square = getSquareAt(coordinate);
        square.terminate(); //detatches all neighbouring squares too
        squares.remove(coordinate);
    }

    /** 
     * Returns whether or not the given coordinate is occupied in this 
     * dungeon.
     * 
     * @param coordinate 
     * The coordinate to check.
     * @throws IllegalArgumentException
     *   | !isPossibleSquareCoordinate(coordinate)
     */
    @Basic
    public boolean isOccupied(Coordinate coordinate)
                                        throws IllegalArgumentException {
        if (!isPossibleSquareCoordinate(coordinate))
            throw new IllegalArgumentException();
        return squares.containsKey(coordinate);
    }


    /** 
     * Return the number of squares in this dungeon.
     *
     * @return
     *   result == {square in getSquares() | true : true}.size()
     */
    public int getNbSquares() {
        return squares.size();
    }

    /**
     * Return a mapping of coordinates to squares of this dungeon.
     */
    @Basic @Raw
    public Map<Coordinate, S> getSquareMapping() {
        Map<Coordinate, S> result = new HashMap<Coordinate, S>();
        result.putAll(squares);
        return result;
    }

    /**
     * Return an iterable of the squares in this dungeon.
     *
     * @return
     * An iterable over the elements of getSquareMapping().values().
     */
    public Iterable<S> getSquares() {
        return squares.values();
    }

    /**
     * Return an iterable of the squares and their position in this dungeon.
     *
     * @return
     * An iterable over the elements of getSquareMapping().entrySet().
     */
    public Iterable<Map.Entry<Coordinate, S>> getPositionsAndSquares() {
        return squares.entrySet();
    }


    /**
     * Variable referencing a map of the squares of this dungeon
     */
    private Map<Coordinate, S> squares = new HashMap<Coordinate, S>();

}

// vim: ts=4:sw=4:expandtab:smarttab
