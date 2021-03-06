package rpg.util;

import be.kuleuven.cs.som.annotate.*;

/** 
 * A class representing a temperature in a variety of temperature scales.
 *
 * @author Roald Frederickx
 */
@Value
public class Temperature implements Comparable<Temperature> {

    /**
     * Enumeration of all the supported scales for all temperatures.
     */
    public static enum Scale {
        CELCIUS {
            public double toCelcius(double value) {
                return value;
            }
            public double fromCelcius(double value) {
                return value;
            }
        },
        KELVIN {
            public double toCelcius(double value) {
                return value - 273.15;
            }
            public double fromCelcius(double value) {
                return value + 273.15;
            }
        },
        FAHRENHEIT {
            public double toCelcius(double value) {
                return (value - 32) * 5.0/9.0;
            }
            public double fromCelcius(double value) {
                return value * 9.0/5.0 + 32;
            }
        };

        /** 
         * Returns the temperature in degrees Celcius that is equivalent to 
         * the given temperature in this temperature scale.
         * 
         * @param value
         * The temperature value in this temperature scale.
         * @return
         * The temperature in degrees Celcius that is equivalent to the 
         * given temperature in this temperature scale.
         */
        abstract public double toCelcius(double value);

        /** 
         * Returns the temperature value in this temperature scale that is 
         * equivalent to the given temperatue value in degrees Celcius. 
         * 
         * @param value
         * The temperature value in degrees Celcius.
         * @return
         * The temperature value in this temperature scale that is 
         * equivalent to the given temperatue value in degrees Celcius. 
         */
        abstract public double fromCelcius(double value);
    };

    /**
     * Initialize this new temperature to a temperature with the given 
     * temperature value (in degrees Celcius).
     *
     * @param temperature 
     * The value for this new temperature in degrees Celcius.
     * @effect
     * The temperature value for this temperature gets Initialized to the 
     * given temperature value interpreted as degrees Celcius.
     *   | this(temperature, Scale.CELCIUS);
     */
    public Temperature(double temperature) {
        this(temperature, Scale.CELCIUS);
    }

    /**
     * Initialize this new temperature to a temperature with the given 
     * temperature value, in the given temperature scale.
     *
     * @param temperature 
     * The value for this new temperature.
     * @param scale 
     * The temperature scale of the given temperature value.
     * @post
     * The temperature value for this temperature gets Initialized to the 
     * given temperature.
     *   | new.temperature(scale) == temperature
     * @throws IllegalArgumentException
     * The given temperature scale is not effective.
     *   | scale == null
     */
    public Temperature(double temperature, Scale scale) 
                                            throws IllegalArgumentException {
        if (scale == null)
            throw new IllegalArgumentException();
        this.temperature = scale.toCelcius(temperature);
    }

    /**
     * Return the temperature value in degrees Celcius for this temperature.
     */
    @Basic @Raw @Immutable
    public double temperature() {
        return temperature;
    }

    /**
     * Return the temperature value for this temperature in 
     * the given scale.
     * @param scale
     * The temperature scale in which to return the temperature value.
     * @throws IllegalArgumentException
     * The given temperature scale is not effective.
     *   | scale == null
     */
    @Raw @Immutable
    public double temperature(Scale scale) throws IllegalArgumentException {
        if (scale == null)
            throw new IllegalArgumentException();
        return scale.fromCelcius(temperature);
    }
    
    /**
     * Variable registering the temperature (in degrees Celcius) for this 
     * temperature.
     */
    private final double temperature;   


    /** 
     * Compare this temperature with a given temperatures.
     * The result is valid for any temperature scale that is 'monotonically 
     * rising' with the Celcius scale.
     * 
     * @param other
     * The temperature to compare this temperature to.
     * @return
     * -1 if the other temperature is strictly larger (on the Celcius 
     * scale), 0 if they are equal and 1 otherwise.
     *   | if (equals(other))
     *   |      then result == 0
     *   | else if (temperature() &lt; other.temperature())
     *   |     then result == -1
     *   | else result == 1
     * @throws IllegalArgumentException
     * The given other temperature is not effective.
     *   | other == null
     */
    public int compareTo(Temperature other) throws IllegalArgumentException {
        if (other == null)
            throw new IllegalArgumentException();
        if (equals(other))
            return 0;
        if (temperature() < other.temperature())
            return -1;
        return 1;
    }

    /** 
     * Check for equality between this temperature and a given object.
     * Note: this allows a relative error (wrt degrees Celcius) of 
     * EQUALS_EPSILON in order to compensate for rounding errors when 
     * comparing different initial temperature scales.
     * 
     * @param other
     * The object to compare this temperature to.
     * @return
     * True iff the given object is an effective temperature and this 
     * temperature is equal (up to a relative error of EQUALS_EPSILON) 
     * to the given temperature.
     */
    @Override
    public boolean equals(Object other) {
        if (other == null)
            return false;
        if (this.getClass() != other.getClass())
            return false;
        Temperature otherTemp = (Temperature) other;
        double allowedError = Math.max(Math.abs(temperature()),
                                        Math.abs(otherTemp.temperature()))
                                * EQUALS_EPSILON;
        return Math.abs(temperature() - otherTemp.temperature())
                                                            <= allowedError;
    }

    /** 
     * Returns a hashcode for this temperature object.
     * 
     * @return
     * A hashcode equal to the number of milidegrees Celcius.
     *   | result == (int)(1000 * temperature())
     */
    @Override
    public int hashCode() {
        return (int)(1000 * temperature());
    }

    /** 
     * Variable registering the maximum relative error between two 
     * temperatures in order to still consider them equal.
     */
    public static final double EQUALS_EPSILON = 1e-6;

    /** 
     * Returns a string representation of the temperature, in the Celcius 
     * scale.
     *
     * @return
     * A string representation of the temperature, in the Celcius scale.
     *   | result == (temperature() + "C")
     */
    @Immutable
    public String toString() {
        return temperature() + "C";
    }

    /** 
     * Return the result of coercing this temperature to the given limits.
     * 
     * @param low
     * The lower temperature limit.
     * @param high
     * The upper temperature limit.
     * @return
     * The coerced value of this temperature to the given limits.
     */
    public Temperature coerce(Temperature low, Temperature high) {
        if (this.compareTo(low) <= 0)
            return low;
        if (this.compareTo(high) >= 0)
            return high;
        return this;
    }
}

// vim: ts=4:sw=4:expandtab:smarttab

