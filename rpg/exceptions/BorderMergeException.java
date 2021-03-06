package rpg.exceptions;

import rpg.square.Border;
import rpg.util.Couple;
import be.kuleuven.cs.som.annotate.*;

/**
 * A class for signalling an illegal border merge.
 * 
 * @author Roald Frederickx
 */
public class BorderMergeException extends RuntimeException {
    /** 
     * Initialize this new border constraints exception with the given 
     * offending square, border and direction.
     *
     * @param border1
     * One of the offending borders for this new border merge exception.
     * @param border2
     * The other offending border for this new border merge exception.
     * @post
     * The offending borders for this new border exception exception are 
     * equal to the given offending borders.
     *   | new.getBorders().contains(border1)
     *   |      &amp;&amp; new.getBorders().contains(border2)
     */
    public BorderMergeException(Border border1, Border border2) {
        borders = new Couple<Border>(border1, border2);
    }

    /**
     * Return the offending borders for this border merge exception.
     */
    @Immutable @Raw
    public Couple<Border> getBorders() {
        return borders;
    }
    
    /**
     * Variable registering the offending borders for this border merge 
     * exception.
     */
    private final Couple<Border> borders;

    static final long serialVersionUID = 1;
}

// vim: ts=4:sw=4:expandtab:smarttab

