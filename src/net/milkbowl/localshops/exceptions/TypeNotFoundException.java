/**
 * 
 * Copyright 2011 MilkBowl (https://github.com/MilkBowl)
 * 
 * This work is licensed under the Creative Commons
 * Attribution-NonCommercial-ShareAlike 3.0 Unported License. To view a copy of
 * this license, visit http://creativecommons.org/licenses/by-nc-sa/3.0/ or send
 * a letter to Creative Commons, 444 Castro Street, Suite 900, Mountain View,
 * California, 94041, USA.
 * 
 */

package net.milkbowl.localshops.exceptions;

public class TypeNotFoundException extends Exception {

    private static final long serialVersionUID = 23542315L;

    public TypeNotFoundException() {
    }

    public TypeNotFoundException(String message) {
        super(message);
    }

    public TypeNotFoundException(Throwable cause) {
        super(cause);
    }

    public TypeNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}