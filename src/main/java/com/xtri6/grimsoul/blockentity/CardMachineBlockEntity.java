package com.xtri6.grimsoul.blockentity;

import java.util.EnumSet;

import com.xtri6.grimsoul.Registration;
import com.xtri6.grimsoul.item.UpgradeType;
import com.xtri6.grimsoul.machine.MachineControl;
import com.xtri6.grimsoul.machine.RedstoneMode;
import com.xtri6.grimsoul.menu.UpgradeItemHandler;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Base for the Mob Fan and XP Tank: a GUI with one Range card slot (up to 10 cards),
 * an on/off switch and a redstone setting.
 */
public abstract class CardMachineBlockEntity extends BlockEntity implements MenuProvider {
    public static final int MAX_RANGE_CARDS = UpgradeType.RANGE.maxCards();

    protected final UpgradeItemHandler cards = new UpgradeItemHandler(1, EnumSet.of(UpgradeType.RANGE), this::onCardsChanged);
    protected final MachineControl control;

    protected CardMachineBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, RedstoneMode defaultRedstone) {
        super(type, pos, state);
        this.control = new MachineControl(defaultRedstone);
    }

    /** A fresh card slot with the same rules, for the client side of the menu. */
    public static UpgradeItemHandler clientCardHandler() {
        return new UpgradeItemHandler(1, EnumSet.of(UpgradeType.RANGE), () -> {});
    }

    private boolean overfillChecked = false;

    /** Called at the start of each server tick: pops out cards above the max once after loading. */
    protected void checkOverfill(Level level, BlockPos pos) {
        if (!overfillChecked && !level.isClientSide) {
            overfillChecked = true;
            cards.dropExtraCards(level, pos);
        }
    }

    public UpgradeItemHandler getCards() {
        return cards;
    }

    public int getRangeCards() {
        return cards.count(UpgradeType.RANGE);
    }

    public abstract ContainerData getDataAccess();

    public abstract void handleButton(int id, Player player);

    protected void onCardsChanged() {
        setChanged();
        syncToClient();
    }

    protected void syncToClient() {
        if (level != null && !level.isClientSide) {
            BlockState state = getBlockState();
            level.sendBlockUpdated(worldPosition, state, state, 3);
        }
    }

    /** Drops the cards when the block is broken. Subclasses drop anything else they hold. */
    public void dropContents(Level level, BlockPos pos) {
        ItemStack stack = cards.getStackInSlot(0);
        if (!stack.isEmpty()) {
            Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), stack.copy());
            cards.setStackInSlot(0, ItemStack.EMPTY);
        }
    }

    /** Settings the client needs (for outlines and fill level). */
    protected abstract void writeClientData(CompoundTag tag);

    protected abstract void readClientData(CompoundTag tag);

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Cards", cards.serializeNBT(registries));
        control.save(tag);
        writeClientData(tag);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("Cards")) {
            cards.deserializeNBT(registries, tag.getCompound("Cards"));
        } else if (tag.contains("RangeCards")) {
            // Saves from the first version stored Range cards as a plain number.
            int old = tag.getInt("RangeCards");
            if (old > 0) {
                cards.setStackInSlot(0, new ItemStack(Registration.CARDS.get(UpgradeType.RANGE).get(), old));
            }
        }
        control.load(tag);
        readClientData(tag);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        writeClientData(tag);
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
