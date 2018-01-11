package cofh.thermalcultivation.gui.slot;

import cofh.core.gui.slot.ISlotValidator;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

public class SlotBagCreative extends Slot {

    ISlotValidator validator;

    public SlotBagCreative(ISlotValidator validator, IInventory inventory, int index, int x, int y) {

        super(inventory, index, x, y);
        this.validator = validator;
    }

    @Override
    public boolean isItemValid(ItemStack stack) {

        return validator.isItemValid(stack);
    }

    @Override
    public void putStack(ItemStack stack) {

        if (stack.isEmpty()) {
            return;
        }
        stack.setCount(stack.getMaxStackSize());
        this.inventory.setInventorySlotContents(this.getSlotIndex(), stack);
        this.onSlotChanged();
    }

    @Override
    public ItemStack decrStackSize(int amount) {

        return this.inventory.getStackInSlot(this.getSlotIndex()).copy();
    }
}
