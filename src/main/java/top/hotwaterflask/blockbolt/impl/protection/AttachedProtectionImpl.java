package top.hotwaterflask.blockbolt.impl.protection;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Bisected;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Openable;
import org.bukkit.block.data.type.Gate;
import org.bukkit.block.data.type.TrapDoor;

import top.hotwaterflask.blockbolt.OpenBlockSound;
import top.hotwaterflask.blockbolt.ProtectionSign;
import top.hotwaterflask.blockbolt.impl.blockfinder.BlockFinder;
import top.hotwaterflask.blockbolt.protection.AttachedProtection;
import top.hotwaterflask.blockbolt.protection.Protection;

/**
 * Implementation of {@link AttachedProtection}. Supports multi-block chaining,
 * opposing trapdoor pair synchronization, and independent single trapdoor toggling.
 */
public final class AttachedProtectionImpl extends AbstractProtection implements AttachedProtection {

    public static Protection fromBlockWithSign(ProtectionSign sign, BlockFinder blockFinder, Collection<Block> protectionBlocks) {
        return new AttachedProtectionImpl(sign, blockFinder, protectionBlocks);
    }

    public static Protection fromBlockWithSigns(Collection<ProtectionSign> signs, BlockFinder blockFinder, Collection<Block> protectionBlocks) {
        return new AttachedProtectionImpl(signs, blockFinder, protectionBlocks);
    }

    private static void setBlockOpen(Block block, boolean open) {
        BlockData blockData = block.getBlockData();
        if (!isFunctionalOpenable(blockData)) {
            return;
        }
        Openable openable = (Openable) blockData;

        if (openable.isOpen() == open) {
            return;
        }

        openable.setOpen(open);
        block.setBlockData(blockData);
    }

    private final BlockFinder blockFinder;
    private final Collection<Block> protectionBlocks;
    private final Block primaryBlock;

    private AttachedProtectionImpl(Collection<ProtectionSign> signs, BlockFinder blockFinder, Collection<Block> protectionBlocks) {
        super(signs);
        this.protectionBlocks = Objects.requireNonNull(protectionBlocks);
        this.blockFinder = blockFinder;
        this.primaryBlock = protectionBlocks.iterator().next();
    }

    private AttachedProtectionImpl(ProtectionSign sign, BlockFinder blockFinder, Collection<Block> protectionBlocks) {
        super(sign);
        this.protectionBlocks = Objects.requireNonNull(protectionBlocks);
        this.blockFinder = blockFinder;
        this.primaryBlock = protectionBlocks.iterator().next();
    }

    @Override
    public boolean canBeOpened() {
        return isFunctionalOpenable(primaryBlock.getBlockData());
    }

    /**
     * Finds the paired opposing trapdoor or paired fence gate for the clicked block.
     * If an opposing trapdoor (facing opposite, same half, neighbor in open direction) exists,
     * returns both blocks. Otherwise, returns only the clicked block.
     *
     * @param clickedBlock
     *            The clicked attachable block.
     * @return The paired blocks or the clicked block itself.
     */
    public List<Block> findPairedAttachables(Block clickedBlock) {
        BlockData blockData = clickedBlock.getBlockData();
        if (blockData instanceof TrapDoor) {
            TrapDoor trapDoor = (TrapDoor) blockData;
            BlockFace facing = trapDoor.getFacing();
            Bisected.Half half = trapDoor.getHalf();

            Block neighbor = clickedBlock.getRelative(facing);
            if (neighbor.getType() == clickedBlock.getType()) {
                BlockData neighborData = neighbor.getBlockData();
                if (neighborData instanceof TrapDoor) {
                    TrapDoor neighborTrapDoor = (TrapDoor) neighborData;
                    if (neighborTrapDoor.getFacing() == facing.getOppositeFace() && neighborTrapDoor.getHalf() == half) {
                        return List.of(clickedBlock, neighbor);
                    }
                }
            }
        } else if (blockData instanceof Gate) {
            Gate gate = (Gate) blockData;
            BlockFace facing = gate.getFacing();
            for (BlockFace face : BlockFinder.CARDINAL_FACES) {
                if (face == facing || face == facing.getOppositeFace()) {
                    continue;
                }
                Block neighbor = clickedBlock.getRelative(face);
                if (neighbor.getType() == clickedBlock.getType()) {
                    BlockData neighborData = neighbor.getBlockData();
                    if (neighborData instanceof Gate) {
                        Gate neighborGate = (Gate) neighborData;
                        if (neighborGate.getFacing() == facing || neighborGate.getFacing() == facing.getOppositeFace()) {
                            return List.of(clickedBlock, neighbor);
                        }
                    }
                }
            }
        }
        return List.of(clickedBlock);
    }

    @Override
    protected Collection<ProtectionSign> fetchSigns() {
        Set<Block> blocksToCheck = new HashSet<>();
        for (Block b : protectionBlocks) {
            blocksToCheck.add(b);
            blocksToCheck.add(blockFinder.findSupportingBlock(b));
            for (BlockFace face : BlockFinder.NORTH_EAST_SOUTH_WEST_UP_DOWN) {
                blocksToCheck.add(b.getRelative(face));
            }
        }
        return blockFinder.findAttachedSigns(blocksToCheck);
    }

    @Override
    public Block getSomeProtectedBlock() {
        return primaryBlock;
    }

    @Override
    public boolean isOpen() {
        for (Block block : protectionBlocks) {
            BlockData materialData = block.getBlockData();
            if (isFunctionalOpenable(materialData) && ((Openable) materialData).isOpen()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean isOpen(Block clickedBlock) {
        List<Block> targets = findPairedAttachables(clickedBlock);
        for (Block target : targets) {
            BlockData materialData = target.getBlockData();
            if (isFunctionalOpenable(materialData) && ((Openable) materialData).isOpen()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void setOpen(boolean open, SoundCondition playSound) {
        for (Block block : protectionBlocks) {
            setBlockOpen(block, open);
            Block supportingBlock = blockFinder.findSupportingBlock(block);
            setBlockOpen(supportingBlock, open);
        }

        if (playSound == SoundCondition.ALWAYS) {
            Sound sound = OpenBlockSound.get(primaryBlock.getType(), open);
            primaryBlock.getWorld().playSound(primaryBlock.getLocation(), sound, 1f, 0.7f);
        }
    }

    @Override
    public void setOpen(Block clickedBlock, boolean open, SoundCondition playSound) {
        List<Block> targets = findPairedAttachables(clickedBlock);
        for (Block target : targets) {
            setBlockOpen(target, open);
            Block supportingBlock = blockFinder.findSupportingBlock(target);
            setBlockOpen(supportingBlock, open);
        }

        if (playSound == SoundCondition.ALWAYS || playSound == SoundCondition.AUTOMATIC) {
            Sound sound = OpenBlockSound.get(clickedBlock.getType(), open);
            clickedBlock.getWorld().playSound(clickedBlock.getLocation(), sound, 1f, 0.7f);
        }
    }

}
