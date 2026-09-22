package top.hotwaterflask.blockbolt.impl;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;

import javax.annotation.Nullable;

import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Bisected.Half;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Door;

import com.google.common.collect.ImmutableSet;

import top.hotwaterflask.blockbolt.OpenBlockSound;
import top.hotwaterflask.blockbolt.impl.blockfinder.BlockFinder;
import top.hotwaterflask.blockbolt.protection.Protection.SoundCondition;

/**
 * Represents a single or multi-connected door structure. Used to find all blocks of a door group,
 * as well to open or close doors individually, in double-door pairs, or as an entire group.
 *
 */
public final class CompleteDoor {

    private static Door asDoorMaterialOrNull(@Nullable Block nullableBlock) {
        if (nullableBlock == null) {
            return null;
        }
        BlockData materialData = nullableBlock.getBlockData();
        if (materialData instanceof Door) {
            return (Door) materialData;
        }
        return null;
    }

    private static boolean isTopHalf(Block doorBlock) {
        Door door = asDoorMaterialOrNull(doorBlock);
        if (door != null) {
            return door.getHalf() == Half.TOP;
        }
        return false;
    }

    private static Door.Hinge getDoorHinge(Block bottomBlock) {
        Block topBlock = bottomBlock.getRelative(BlockFace.UP);
        Door topDoor = asDoorMaterialOrNull(topBlock);
        if (topDoor != null) {
            return topDoor.getHinge();
        }
        Door bottomDoor = asDoorMaterialOrNull(bottomBlock);
        if (bottomDoor != null) {
            return bottomDoor.getHinge();
        }
        return Door.Hinge.LEFT;
    }

    @Nullable
    private static BlockFace getFaceToRightDoorOrNull(BlockFace facing) {
        switch (facing) {
            case EAST:
                return BlockFace.SOUTH;
            case SOUTH:
                return BlockFace.WEST;
            case WEST:
                return BlockFace.NORTH;
            case NORTH:
                return BlockFace.EAST;
            default:
                return null;
        }
    }

    private final Material doorMaterial;
    private final Block someDoorBlock;
    private final Set<Block> bottomDoorBlocks;

    /**
     * Creates a new door group. The given block must be part of the door.
     *
     * @param doorBlock
     *            A block that is part of the door.
     * @param chainDoors
     *            Whether to chain multiple adjacent doors together.
     */
    public CompleteDoor(Block doorBlock, boolean chainDoors) {
        Objects.requireNonNull(doorBlock, "doorBlock");
        this.doorMaterial = doorBlock.getType();
        this.someDoorBlock = doorBlock;

        Block initialBottom = isTopHalf(doorBlock) ? doorBlock.getRelative(BlockFace.DOWN) : doorBlock;

        Set<Block> bottoms = new LinkedHashSet<>();

        if (initialBottom.getType() == doorMaterial) {
            bottoms.add(initialBottom);
        }

        if (chainDoors) {
            Queue<Block> queue = new ArrayDeque<>();
            if (initialBottom.getType() == doorMaterial) {
                queue.add(initialBottom);
            }

            while (!queue.isEmpty()) {
                Block curr = queue.poll();
                for (BlockFace face : BlockFinder.CARDINAL_FACES) {
                    Block neighbor = curr.getRelative(face);
                    if (neighbor.getType() == doorMaterial) {
                        Block neighborBottom = isTopHalf(neighbor) ? neighbor.getRelative(BlockFace.DOWN) : neighbor;
                        if (neighborBottom.getType() == doorMaterial && bottoms.add(neighborBottom)) {
                            queue.add(neighborBottom);
                        }
                    }
                }
            }
        } else {
            // When chaining is disabled, only include double door pair if present
            List<Block> paired = findPairedDoors(initialBottom);
            bottoms.addAll(paired);
        }

        this.bottomDoorBlocks = bottoms;
    }

    /**
     * Creates a new door group with chaining enabled.
     *
     * @param doorBlock
     *            A block that is part of the door.
     */
    public CompleteDoor(Block doorBlock) {
        this(doorBlock, true);
    }

    /**
     * Finds the paired double door for a given clicked door block.
     * If the clicked door is part of a complementary double door pair (same facing, opposite hinge),
     * both doors are returned. If it is a single door, only the clicked door is returned.
     *
     * @param clickedDoorBlock
     *            The door block clicked by the player.
     * @return List of bottom blocks for the double door pair or single door.
     */
    public List<Block> findPairedDoors(Block clickedDoorBlock) {
        Block bottom = isTopHalf(clickedDoorBlock) ? clickedDoorBlock.getRelative(BlockFace.DOWN) : clickedDoorBlock;
        Door doorData = asDoorMaterialOrNull(bottom);
        if (doorData == null) {
            return List.of(bottom);
        }

        BlockFace facing = doorData.getFacing();
        Door.Hinge hinge = getDoorHinge(bottom);

        BlockFace faceToRight = getFaceToRightDoorOrNull(facing);
        if (faceToRight == null) {
            return List.of(bottom);
        }

        BlockFace faceToPartner = (hinge == Door.Hinge.LEFT) ? faceToRight : faceToRight.getOppositeFace();
        Door.Hinge expectedPartnerHinge = (hinge == Door.Hinge.LEFT) ? Door.Hinge.RIGHT : Door.Hinge.LEFT;

        Block partnerBottom = bottom.getRelative(faceToPartner);
        if (partnerBottom.getType() == doorMaterial) {
            Door partnerData = asDoorMaterialOrNull(partnerBottom);
            if (partnerData != null && partnerData.getFacing() == facing && getDoorHinge(partnerBottom) == expectedPartnerHinge) {
                return List.of(bottom, partnerBottom);
            }
        }

        return List.of(bottom);
    }

    /**
     * Gets a collection of all blocks where attached protection signs are used
     * for this door structure. This includes:
     * 1. All door blocks (top and bottom halves)
     * 2. All supporting blocks directly below the doors
     * 3. All blocks directly above the doors
     * 4. Surrounding adjacent horizontal blocks (door frames / walls)
     *
     * @return All blocks that can have protection signs attached or support the door.
     */
    public Collection<Block> getBlocksForSigns() {
        ImmutableSet.Builder<Block> blocks = ImmutableSet.builder();

        for (Block bottom : bottomDoorBlocks) {
            Block top = bottom.getRelative(BlockFace.UP);

            // Door blocks themselves
            blocks.add(bottom);
            blocks.add(top);

            // Supporting ground block below
            blocks.add(bottom.getRelative(BlockFace.DOWN));

            // Block directly above
            blocks.add(top.getRelative(BlockFace.UP));

            // Surrounding horizontal blocks for bottom and top halves
            for (BlockFace face : BlockFinder.CARDINAL_FACES) {
                blocks.add(bottom.getRelative(face));
                blocks.add(top.getRelative(face));
            }
        }

        return blocks.build();
    }

    /**
     * Returns one of the door blocks in this group.
     *
     * @return One of the door blocks.
     */
    public Block getSomeDoorBlock() {
        return this.someDoorBlock;
    }

    /**
     * Gets whether any door in this group is currently open.
     *
     * @return True if any door is currently open, false otherwise.
     */
    public boolean isOpen() {
        for (Block bottom : bottomDoorBlocks) {
            Door doorData = asDoorMaterialOrNull(bottom);
            if (doorData != null && doorData.isOpen()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Gets whether the clicked door or its double-door pair is open.
     *
     * @param clickedBlock
     *            The clicked door block.
     * @return True if open, false otherwise.
     */
    public boolean isOpen(Block clickedBlock) {
        List<Block> targets = findPairedDoors(clickedBlock);
        for (Block target : targets) {
            Door doorData = asDoorMaterialOrNull(target);
            if (doorData != null && doorData.isOpen()) {
                return true;
            }
        }
        return false;
    }

    private void playSound(Block bottomBlock, boolean open, SoundCondition condition) {
        if (condition == SoundCondition.NEVER) {
            return;
        }
        if (open == isOpen()) {
            return;
        }
        boolean ironDoor = bottomBlock.getType() == Material.IRON_DOOR;
        if (condition == SoundCondition.AUTOMATIC && !ironDoor) {
            return;
        }

        Sound sound = OpenBlockSound.get(bottomBlock.getType(), open);
        bottomBlock.getWorld().playSound(bottomBlock.getLocation(), sound, 1f, 0.7f);
    }

    /**
     * Opens or closes the clicked door (and its symmetrical double-door partner if present).
     *
     * @param clickedBlock
     *            The clicked door block.
     * @param open
     *            Whether to open or close.
     * @param soundAction
     *            Sound condition.
     */
    public void setOpen(Block clickedBlock, boolean open, SoundCondition soundAction) {
        List<Block> targets = findPairedDoors(clickedBlock);
        boolean playedSound = false;

        for (Block bottom : targets) {
            Door doorData = asDoorMaterialOrNull(bottom);
            if (doorData != null) {
                if (!playedSound) {
                    playSound(bottom, open, soundAction);
                    playedSound = true;
                }

                doorData.setOpen(open);
                bottom.setBlockData(doorData);
            }
        }
    }

    /**
     * Opens or closes all doors in this connected door group.
     *
     * @param open
     *            Whether the doors must be opened (true) or closed (false).
     * @param soundAction
     *            Whether a sound must be played.
     */
    public void setOpen(boolean open, SoundCondition soundAction) {
        boolean playedSound = false;

        for (Block bottom : bottomDoorBlocks) {
            Door doorData = asDoorMaterialOrNull(bottom);
            if (doorData != null) {
                if (!playedSound) {
                    playSound(bottom, open, soundAction);
                    playedSound = true;
                }

                doorData.setOpen(open);
                bottom.setBlockData(doorData);
            }
        }
    }

}
