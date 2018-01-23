package cofh.thermalcultivation.gui.container;

import cofh.core.gui.container.ContainerInventoryItem;
import cofh.core.gui.slot.ISlotValidator;
import cofh.core.gui.slot.SlotLocked;
import cofh.core.gui.slot.SlotValidated;
import cofh.core.util.helpers.MathHelper;
import cofh.core.util.helpers.StringHelper;
import cofh.thermalcultivation.gui.slot.SlotBagCreative;
import cofh.thermalcultivation.item.ItemSeedBag;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Slot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.IPlantable;

public class ContainerSeedBag extends ContainerInventoryItem implements ISlotValidator {

	static final String NAME = "item.thermalcultivation.seed_bag.name";

	boolean isCreative;

	int storageIndex;
	int rowSize;

	public ContainerSeedBag(ItemStack stack, InventoryPlayer inventory) {

		super(stack, inventory);

		isCreative = ItemSeedBag.isCreative(stack);

		storageIndex = ItemSeedBag.getStorageIndex(stack);
		rowSize = MathHelper.clamp(storageIndex, 9, 14);

		int rows = MathHelper.clamp(storageIndex, 2, 9);
		int slots = rowSize * rows;
		int yOffset = 17;

		bindPlayerInventory(inventory);

		switch (storageIndex) {
			case 0:
				addSlotToContainer(new SlotBagCreative(this, containerWrapper, 0, 80, 26));
				rowSize = 1;
				break;
			case 1:
				yOffset += 9;
				for (int i = 0; i < 9; i++) {
					addSlotToContainer(new SlotValidated(this, containerWrapper, i, 8 + i % rowSize * 18, yOffset + i / rowSize * 18));
				}
				break;
			default:
				for (int i = 0; i < slots; i++) {
					addSlotToContainer(new SlotValidated(this, containerWrapper, i, 8 + i % rowSize * 18, yOffset + i / rowSize * 18));
				}
				break;
		}
	}

	@Override
	protected void bindPlayerInventory(InventoryPlayer inventoryPlayer) {

		int xOffset = getPlayerInventoryHorizontalOffset();
		int yOffset = getPlayerInventoryVerticalOffset();

		for (int i = 0; i < 3; i++) {
			for (int j = 0; j < 9; j++) {
				addSlotToContainer(new Slot(inventoryPlayer, j + i * 9 + 9, xOffset + j * 18, yOffset + i * 18));
			}
		}
		for (int i = 0; i < 9; i++) {
			if (i == inventoryPlayer.currentItem) {
				addSlotToContainer(new SlotLocked(inventoryPlayer, i, xOffset + i * 18, yOffset + 58));
			} else {
				addSlotToContainer(new Slot(inventoryPlayer, i, xOffset + i * 18, yOffset + 58));
			}
		}
	}

	@Override
	public String getInventoryName() {

		return containerWrapper.hasCustomName() ? containerWrapper.getName() : StringHelper.localize(NAME);
	}

	@Override
	protected int getPlayerInventoryVerticalOffset() {

		return 30 + 18 * MathHelper.clamp(storageIndex, 2, 9);
	}

	@Override
	protected int getPlayerInventoryHorizontalOffset() {

		return 8 + 9 * (rowSize - 9);
	}

	/* ISlotValidator */
	@Override
	public boolean isItemValid(ItemStack stack) {

		Item current = ItemSeedBag.getCurrentSeed(getContainerStack());
		return containerWrapper.isItemValidForSlot(0, stack) && stack.getItem() instanceof IPlantable && (current == null || current.equals(stack.getItem()));
	}
}
