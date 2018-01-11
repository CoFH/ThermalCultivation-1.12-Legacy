package cofh.thermalcultivation.gui.client;

import cofh.core.gui.GuiContainerCore;
import cofh.core.gui.element.tab.TabInfo;
import cofh.core.init.CoreProps;
import cofh.core.util.helpers.MathHelper;
import cofh.core.util.helpers.StringHelper;
import cofh.thermalcultivation.gui.container.ContainerSeedBag;
import cofh.thermalcultivation.item.ItemSeedBag;
import net.minecraft.entity.player.InventoryPlayer;

public class GuiSeedBag extends GuiContainerCore {

    boolean isCreative;
    int storageIndex;

    public GuiSeedBag(InventoryPlayer inventory, ContainerSeedBag container) {

        super(container);

        isCreative = ItemSeedBag.isCreative(container.getContainerStack());

        storageIndex = ItemSeedBag.getStorageIndex(container.getContainerStack());
        texture = CoreProps.TEXTURE_STORAGE[storageIndex];
        name = container.getInventoryName();

        allowUserInput = false;

        xSize = 14 + 18 * MathHelper.clamp(storageIndex, 9, 14);
        ySize = 112 + 18 * MathHelper.clamp(storageIndex, 2, 9);

        if (isCreative) {
            generateInfo("tab.thermalcultivation.storage.seed_bag_c");
        } else {
            generateInfo("tab.thermalcultivation.storage.seed_bag");
        }
        if (container.getContainerStack().isItemEnchantable() && !ItemSeedBag.hasHoldingEnchant(container.getContainerStack())) {
            myInfo += "\n\n" + StringHelper.localize("tab.thermalcultivation.storage.enchant");
        }
    }

    @Override
    public void initGui() {

        super.initGui();

        if (!myInfo.isEmpty()) {
            addTab(new TabInfo(this, myInfo));
        }
    }
}
