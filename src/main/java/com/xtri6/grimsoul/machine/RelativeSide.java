package com.xtri6.grimsoul.machine;

import net.minecraft.core.Direction;

/**
 * A machine side named the way the player sees it when looking at the machine's front,
 * so side settings stay correct no matter which way the machine faces.
 */
public enum RelativeSide {
    FRONT("front"), BACK("back"), LEFT("left"), RIGHT("right"), TOP("top"), BOTTOM("bottom");

    private final String id;

    RelativeSide(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }

    /** The world direction of this side for a machine facing {@code facing}. */
    public Direction toDirection(Direction facing) {
        return switch (this) {
            case FRONT -> facing;
            case BACK -> facing.getOpposite();
            // Looking at the front, your left is the machine's right-hand side.
            case LEFT -> facing.getClockWise();
            case RIGHT -> facing.getCounterClockWise();
            case TOP -> Direction.UP;
            case BOTTOM -> Direction.DOWN;
        };
    }

    public static RelativeSide fromDirection(Direction facing, Direction side) {
        for (RelativeSide relative : values()) {
            if (relative.toDirection(facing) == side) {
                return relative;
            }
        }
        return FRONT;
    }
}
