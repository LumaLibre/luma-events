package dev.lumas.events.utility;

import org.bukkit.block.BlockFace;

public class BlockFaces {
    public static final BlockFace[] axis = { BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST };
    public static final BlockFace[] radial = { BlockFace.NORTH, BlockFace.NORTH_EAST, BlockFace.EAST, BlockFace.SOUTH_EAST, BlockFace.SOUTH, BlockFace.SOUTH_WEST, BlockFace.WEST, BlockFace.NORTH_WEST };

    /**
     * Gets the horizontal Block Face from a given yaw angle<br>
     * This includes the NORTH_WEST faces
     *
     * @param yaw angle
     * @return The Block Face of the angle
     */
    public static BlockFace yawToFace(float yaw) {
        return yawToFace(yaw, true);
    }
    /**
     * Gets the horizontal Block Face from a given yaw angle
     *
     * @param yaw angle
     * @param useSubCardinalDirections setting, True to allow NORTH_WEST to be returned
     * @return The Block Face of the angle
     */
    public static BlockFace yawToFace(float yaw, boolean useSubCardinalDirections) {
        if (useSubCardinalDirections) {
            return radial[Math.round(yaw / 45f) & 0x7];
        } else {
            return axis[Math.round(yaw / 90f) & 0x3];
        }
    }

    /**
     * Block faces ordered by the vanilla yaw they correspond to (in 45 degree steps)
     */
    private static final BlockFace[] byYaw = {
            BlockFace.SOUTH, BlockFace.SOUTH_WEST, BlockFace.WEST, BlockFace.NORTH_WEST,
            BlockFace.NORTH, BlockFace.NORTH_EAST, BlockFace.EAST, BlockFace.SOUTH_EAST
    };

    /**
     * Gets the yaw an entity needs to look towards a given horizontal Block Face
     * South is 0, west is 90, north is 180, east is -90.
     * This is not the inverse of #yawToFace(float)!
     *
     * @param face the horizontal Block Face to look towards
     * @return the yaw in the [-180, 180] range, or 0 if the face isn't horizontal
     */
    public static float faceToYaw(BlockFace face) {
        for (int i = 0; i < byYaw.length; i++) {
            if (byYaw[i] == face) {
                float yaw = i * 45f;
                return yaw > 180f ? yaw - 360f : yaw;
            }
        }
        return 0f;
    }

    /**
     * @param face any Block Face
     * @return whether the face is one of the eight horizontal directions
     */
    public static boolean isHorizontal(BlockFace face) {
        for (BlockFace horizontal : byYaw) {
            if (horizontal == face) {
                return true;
            }
        }
        return false;
    }
}
